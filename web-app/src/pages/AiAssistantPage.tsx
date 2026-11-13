import React, { useMemo, useRef, useState } from 'react';
import { getGenerativeModel, Schema } from 'firebase/ai';
import {
  AlertCircle,
  ChefHat,
  PlusCircle,
  Send,
  Sparkles,
  Trash2
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { getFirebaseAiInstance } from '../firebase';
import {
  isUserAdmin,
  type IngredientRequirement,
  type MenuItemExtra
} from '../types/models';

const MODEL_NAME = 'gemini-3.8-flash';
const CATEGORIES = ['Parrillas', 'Hamburguesas & Pepitos', 'Entradas & Guarniciones', 'Bebidas'];

interface ChatMessage {
  id: string;
  isUser: boolean;
  text: string;
}

interface GeneratedDishSuggestion {
  name: string;
  category: string;
  description: string;
  priceUsd: number;
  ingredients: { supplyName: string; quantityRequired: number; unit: string }[];
  extras: MenuItemExtra[];
}

export const AiAssistantPage: React.FC = () => {
  const {
    config,
    orders,
    supplies,
    menuItems,
    restockRecommendations,
    loggedUser,
    simulateStaff,
    saveMenuItem,
    showToast
  } = useApp();

  const canEditMenu = (isUserAdmin(loggedUser) && !simulateStaff) || Boolean(loggedUser?.canUseMenuApp);

  const [tab, setTab] = useState<'CHAT' | 'DISH'>('CHAT');

  // Chat state
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [promptInput, setPromptInput] = useState('');
  const [streamingText, setStreamingText] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [chatError, setChatError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  // Dish generator state
  const [dishCategory, setDishCategory] = useState(CATEGORIES[0]);
  const [dishIdea, setDishIdea] = useState('');
  const [dishLoading, setDishLoading] = useState(false);
  const [dishError, setDishError] = useState<string | null>(null);
  const [generatedDish, setGeneratedDish] = useState<GeneratedDishSuggestion | null>(null);

  const activeOrderCount = useMemo(
    () => orders.filter((o) => o.status !== 'CANCELADO').length,
    [orders]
  );

  const quickPrompts = useMemo(
    () => [
      restockRecommendations.length > 0
        ? `Plan de compras urgente (${restockRecommendations.length} insumos bajos)`
        : 'Resumen operativo de hoy',
      'Análisis de cuentas por cobrar y ventas',
      'Idea de combo rentable con el stock alto',
      'Consejos rápidos para agilizar cocina'
    ],
    [restockRecommendations.length]
  );

  const buildOperationalContext = (userPrompt: string) => {
    const activeOrders = orders.filter((o) => o.status !== 'CANCELADO');
    const paid = activeOrders.filter((o) => o.isPaid);
    const unpaid = activeOrders.filter((o) => !o.isPaid);
    const totalPaidUsd = paid.reduce((s, o) => s + (o.totalUsd || 0), 0);
    const totalUnpaidUsd = unpaid.reduce((s, o) => s + (o.totalUsd || 0), 0);
    const inKitchen = activeOrders.filter((o) => o.status === 'EN_COCINA').length;
    const ready = activeOrders.filter((o) => o.status === 'LISTO').length;
    const rate = config.exchangeRateUsdToBs || 65;

    const lowStockLines =
      restockRecommendations.length === 0
        ? 'Todos los insumos están por encima del mínimo.'
        : restockRecommendations
            .map(
              (r) =>
                `- ${r.supply.name}: queda ${r.supply.currentStock.toFixed(1)} ${r.supply.unit} ` +
                `(mínimo ${r.supply.minStockThreshold}), comprar ~${r.toBuyAmount.toFixed(1)} ${r.supply.unit} ` +
                `(~$${r.estimatedCostUsd.toFixed(2)} / ${r.estimatedCostBs.toFixed(0)} Bs)`
            )
            .join('\n');

    const topStockLines = [...supplies]
      .sort((a, b) => b.currentStock - a.currentStock)
      .slice(0, 6)
      .map((s) => `${s.name} (${s.currentStock.toFixed(1)} ${s.unit})`)
      .join(', ');

    const menuSummary = menuItems
      .slice(0, 15)
      .map((m) => `${m.name} ($${m.priceUsd.toFixed(2)})`)
      .join(', ');

    const historySnippet = messages
      .slice(-5)
      .map((m) => `${m.isUser ? 'Usuario' : 'Asistente'}: ${m.text.slice(0, 350)}`)
      .join('\n');

    return `Eres el Asistente Inteligente de Operaciones y Cocina de "${config.businessName}".
Responde siempre en español claro, directo, práctico y con viñetas cortas cuando sea útil. Usa datos reales del restaurante proporcionados abajo y nunca inventes cifras que contradigan los datos.

DATOS EN TIEMPO REAL DEL RESTAURANTE:
- Tasa de cambio actual: ${rate.toFixed(2)} Bs/$
- Órdenes activas (no canceladas): ${activeOrders.length} (En cocina: ${inKitchen}, Listas: ${ready})
- Cuentas cobradas: ${paid.length} ($${totalPaidUsd.toFixed(2)} / ${(totalPaidUsd * rate).toFixed(0)} Bs)
- Cuentas por cobrar: ${unpaid.length} ($${totalUnpaidUsd.toFixed(2)} / ${(totalUnpaidUsd * rate).toFixed(0)} Bs)
- Inventario con stock crítico o bajo (${restockRecommendations.length} insumos):
${lowStockLines}
- Insumos con mayor disponibilidad: ${topStockLines}
- Muestra del menú (${menuItems.length} platos totales): ${menuSummary}
${historySnippet ? `\nCONVERSACIÓN RECIENTE:\n${historySnippet}\n` : ''}
CONSULTA ACTUAL DEL USUARIO:
${userPrompt}`;
  };

  const handleSendChat = async (textToSend?: string) => {
    const clean = (textToSend ?? promptInput).trim();
    if (!clean || chatLoading) return;
    if (!textToSend) setPromptInput('');
    setChatError(null);
    setChatLoading(true);
    setStreamingText('');
    setMessages((prev) => [...prev, { id: crypto.randomUUID(), isUser: true, text: clean }]);

    try {
      const ai = getFirebaseAiInstance();
      const model = getGenerativeModel(ai, { model: MODEL_NAME });
      const prompt = buildOperationalContext(clean);
      const streamResult = await model.generateContentStream(prompt);

      let accumulated = '';
      for await (const chunk of streamResult.stream) {
        const piece = chunk.text();
        if (piece) {
          accumulated += piece;
          setStreamingText(accumulated);
          messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
        }
      }

      const finalText = accumulated.trim();
      if (!finalText) {
        setChatError('Gemini devolvió una respuesta vacía. Intenta nuevamente.');
      } else {
        setMessages((prev) => [...prev, { id: crypto.randomUUID(), isUser: false, text: finalText }]);
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setChatError(
        `No se pudo consultar Gemini AI en Web: ${msg}. Si acabas de desplegar en Netlify, verifica que VITE_FIREBASE_APP_ID corresponda a una Web App registrada en Firebase Console.`
      );
    } finally {
      setStreamingText('');
      setChatLoading(false);
    }
  };

  const handleGenerateDish = async () => {
    if (dishLoading) return;
    setDishLoading(true);
    setDishError(null);
    setGeneratedDish(null);

    const idea =
      dishIdea.trim() ||
      `Crea una especialidad nueva y rentable en la categoría ${dishCategory} usando insumos disponibles.`;

    const suppliesContext =
      supplies.length > 0
        ? supplies
            .map(
              (s) =>
                `- ${s.name} (Stock: ${s.currentStock.toFixed(1)} ${s.unit}, Costo: $${s.costUsd.toFixed(2)}/${s.unit})`
            )
            .join('\n')
        : '- Carne de res, Pechuga de pollo, Queso llanero, Chorizo, Papas';

    const existingDishes = menuItems
      .slice(0, 12)
      .map((m) => `${m.name} ($${m.priceUsd})`)
      .join(', ');

    const prompt = `Eres el chef y gerente gastronómico de "La Parrilla 200" (parrillas, hamburguesas, pepitos y bebidas en Venezuela).
Diseña UN plato nuevo original que NO repita exactamente los ya existentes (${existingDishes}).
Categoría preferida: ${dishCategory}.
Solicitud del usuario: "${idea}"
Insumos reales en inventario (prioriza usar nombres exactos de esta lista cuando apliquen):
${suppliesContext}`;

    try {
      const ai = getFirebaseAiInstance();
      const dishSchema = Schema.object({
        properties: {
          name: Schema.string({ description: 'Nombre comercial atractivo del plato para La Parrilla 200' }),
          category: Schema.enumString({
            enum: CATEGORIES,
            description: 'Categoría exacta del menú'
          }),
          description: Schema.string({ description: 'Descripción breve y apetitosa en español' }),
          priceUsd: Schema.number({ description: 'Precio sugerido en dólares USD realista' }),
          ingredients: Schema.array({
            items: Schema.object({
              properties: {
                supplyName: Schema.string({ description: 'Nombre exacto del insumo' }),
                quantityRequired: Schema.number({ description: 'Cantidad por plato' }),
                unit: Schema.string({ description: 'Unidad: kg, g, ud, porc, lt' })
              }
            }),
            description: 'Insumos necesarios para preparar 1 porción'
          }),
          extras: Schema.array({
            items: Schema.object({
              properties: {
                name: Schema.string({ description: 'Nombre del extra o adicional' }),
                priceUsd: Schema.number({ description: 'Precio del extra en USD' })
              }
            }),
            description: 'Extras opcionales sugeridos para el cliente'
          })
        }
      });

      const model = getGenerativeModel(ai, {
        model: MODEL_NAME,
        generationConfig: {
          responseMimeType: 'application/json',
          responseSchema: dishSchema
        }
      });

      const result = await model.generateContent(prompt);
      const rawJson = result.response.text();
      const parsed = JSON.parse(rawJson) as GeneratedDishSuggestion;
      setGeneratedDish({
        name: parsed.name || 'Nuevo Plato Parrillero',
        category: parsed.category || dishCategory,
        description: parsed.description || '',
        priceUsd: Number(parsed.priceUsd) || 8.0,
        ingredients: Array.isArray(parsed.ingredients) ? parsed.ingredients : [],
        extras: Array.isArray(parsed.extras) ? parsed.extras : []
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setDishError(
        `No se pudo generar el plato con Gemini: ${msg}. Verifica que Firebase App ID Web esté configurado si estás en Netlify.`
      );
    } finally {
      setDishLoading(false);
    }
  };

  const handleSaveGeneratedDish = async () => {
    if (!generatedDish) return;
    const mappedIngredients: IngredientRequirement[] = generatedDish.ingredients
      .map((ing) => {
        const lower = ing.supplyName.toLowerCase();
        const match = supplies.find(
          (s) =>
            s.name.toLowerCase() === lower ||
            s.name.toLowerCase().includes(lower) ||
            lower.includes(s.name.toLowerCase())
        );
        if (!match) return null;
        return {
          supplyId: match.id,
          supplyName: match.name,
          quantityRequired: Math.max(0.05, Number(ing.quantityRequired) || 0.1),
          unit: match.unit,
          isRemovable: true
        };
      })
      .filter((x): x is IngredientRequirement => x !== null);

    await saveMenuItem({
      id: '',
      name: generatedDish.name,
      description: generatedDish.description,
      category: generatedDish.category,
      priceUsd: generatedDish.priceUsd,
      isAvailable: true,
      ingredientsJson: JSON.stringify(mappedIngredients),
      extrasJson: JSON.stringify(generatedDish.extras)
    });
    setGeneratedDish(null);
    setDishIdea('');
    showToast(`¡Plato "${generatedDish.name}" guardado en el menú!`);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* Header + Tabs */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ background: 'rgba(211,47,47,0.2)', padding: 8, borderRadius: 10 }}>
              <Sparkles size={22} color="#ffb300" />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: 19, fontWeight: 900 }}>Asistente Gemini AI</h2>
              <div style={{ fontSize: 12, color: '#9ca3af' }}>
                {supplies.length} insumos • {activeOrderCount} órdenes • {menuItems.length} platos en vivo
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button
              className={`chip ${tab === 'CHAT' ? 'active' : ''}`}
              onClick={() => setTab('CHAT')}
            >
              Consultor del Negocio
            </button>
            <button
              className={`chip ${tab === 'DISH' ? 'active' : ''}`}
              onClick={() => setTab('DISH')}
            >
              Crear Platos & Extras
            </button>
            {tab === 'CHAT' && messages.length > 0 && (
              <button
                className="btn btn-surface btn-sm"
                onClick={() => {
                  setMessages([]);
                  setChatError(null);
                }}
                title="Limpiar chat"
              >
                <Trash2 size={14} /> Limpiar
              </button>
            )}
          </div>
        </div>
      </div>

      {tab === 'CHAT' ? (
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12, minHeight: 520 }}>
          {/* Quick Prompts */}
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {quickPrompts.map((qp) => (
              <button
                key={qp}
                className="chip"
                disabled={chatLoading}
                onClick={() => handleSendChat(qp)}
              >
                <Sparkles size={13} color="#ffb300" /> {qp}
              </button>
            ))}
          </div>

          {chatError && (
            <div
              style={{
                background: 'rgba(229,57,53,0.16)',
                border: '1px solid #ef4444',
                borderRadius: 10,
                padding: 12,
                display: 'flex',
                gap: 10,
                alignItems: 'center',
                fontSize: 13
              }}
            >
              <AlertCircle size={18} color="#ef4444" style={{ flexShrink: 0 }} />
              <span>{chatError}</span>
            </div>
          )}

          {/* Messages */}
          <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 10, maxHeight: 420, paddingRight: 4 }}>
            {messages.length === 0 && !chatLoading && (
              <div style={{ background: '#222226', borderRadius: 12, padding: 16 }}>
                <div style={{ fontWeight: 800, fontSize: 15, marginBottom: 6 }}>
                  ¿En qué te ayudo hoy en La Parrilla 200?
                </div>
                <div style={{ fontSize: 13, color: '#b3b3b7', lineHeight: 1.5 }}>
                  Toca una consulta rápida arriba o escribe tu pregunta. Analizo tus ventas, cuentas pendientes,
                  inventario crítico y menú en tiempo real con Gemini.
                </div>
              </div>
            )}

            {messages.map((m) => (
              <div
                key={m.id}
                style={{
                  alignSelf: m.isUser ? 'flex-end' : 'flex-start',
                  maxWidth: '82%',
                  background: m.isUser ? '#d32f2f' : '#222226',
                  border: m.isUser ? 'none' : '1px solid #383842',
                  borderRadius: 14,
                  padding: '10px 14px'
                }}
              >
                <div style={{ fontSize: 11, fontWeight: 800, color: m.isUser ? '#fee2e2' : '#ffb300', marginBottom: 4 }}>
                  {m.isUser ? 'Tú' : 'Gemini AI'}
                </div>
                <div style={{ fontSize: 13.5, lineHeight: 1.5, whiteSpace: 'pre-wrap' }}>{m.text}</div>
              </div>
            ))}

            {chatLoading && (
              <div
                style={{
                  alignSelf: 'flex-start',
                  maxWidth: '82%',
                  background: '#222226',
                  border: '1px solid #383842',
                  borderRadius: 14,
                  padding: '10px 14px'
                }}
              >
                <div style={{ fontSize: 11, fontWeight: 800, color: '#ffb300', marginBottom: 4 }}>
                  Gemini AI {streamingText ? '(escribiendo...)' : '(analizando...)'}
                </div>
                <div style={{ fontSize: 13.5, lineHeight: 1.5, whiteSpace: 'pre-wrap' }}>
                  {streamingText || 'Consultando datos de La Parrilla 200...'}
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Bar */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendChat();
            }}
            style={{ display: 'flex', gap: 8 }}
          >
            <input
              className="input"
              placeholder="Pregunta sobre ventas, compras, stock o cocina..."
              value={promptInput}
              onChange={(e) => setPromptInput(e.target.value)}
              disabled={chatLoading}
            />
            <button type="submit" className="btn btn-primary" disabled={chatLoading || !promptInput.trim()}>
              <Send size={16} /> Enviar
            </button>
          </form>
        </div>
      ) : (
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div>
            <h3 style={{ margin: '0 0 4px', fontSize: 16, fontWeight: 800 }}>
              Diseñador de Platos, Recetas y Extras con IA
            </h3>
            <div style={{ fontSize: 12.5, color: '#9ca3af' }}>
              Gemini analiza tus insumos reales y genera un plato estructurado con ingredientes y extras listo para guardar
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {CATEGORIES.map((c) => (
              <button
                key={c}
                className={`chip ${dishCategory === c ? 'active' : ''}`}
                onClick={() => setDishCategory(c)}
              >
                {c}
              </button>
            ))}
          </div>

          <div>
            <label className="label">Idea o ingrediente estrella (opcional)</label>
            <textarea
              className="textarea"
              rows={2}
              placeholder="Ej: Hamburguesa doble con queso ahumado, tocineta crujiente y salsa BBQ"
              value={dishIdea}
              onChange={(e) => setDishIdea(e.target.value)}
              disabled={dishLoading}
            />
          </div>

          <button className="btn btn-primary" onClick={handleGenerateDish} disabled={dishLoading}>
            <Sparkles size={16} /> {dishLoading ? 'Creando receta con Gemini...' : 'Generar Plato con IA'}
          </button>

          {dishError && (
            <div
              style={{
                background: 'rgba(229,57,53,0.16)',
                border: '1px solid #ef4444',
                borderRadius: 10,
                padding: 12,
                display: 'flex',
                gap: 10,
                alignItems: 'center',
                fontSize: 13
              }}
            >
              <AlertCircle size={18} color="#ef4444" style={{ flexShrink: 0 }} />
              <span>{dishError}</span>
            </div>
          )}

          {generatedDish && (
            <div
              style={{
                background: '#222226',
                border: '1px solid rgba(255,179,0,0.55)',
                borderRadius: 14,
                padding: 16,
                display: 'flex',
                flexDirection: 'column',
                gap: 10
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 10 }}>
                <div>
                  <div style={{ fontSize: 11, fontWeight: 800, color: '#ffb300', textTransform: 'uppercase' }}>
                    {generatedDish.category}
                  </div>
                  <div style={{ fontSize: 19, fontWeight: 900 }}>{generatedDish.name}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 19, fontWeight: 900, color: '#4ade80' }}>
                    ${generatedDish.priceUsd.toFixed(2)}
                  </div>
                  <div style={{ fontSize: 12, color: '#9ca3af' }}>
                    {(generatedDish.priceUsd * config.exchangeRateUsdToBs).toFixed(0)} Bs
                  </div>
                </div>
              </div>

              {generatedDish.description && (
                <p style={{ margin: 0, fontSize: 13, color: '#d1d5db' }}>{generatedDish.description}</p>
              )}

              <div style={{ borderTop: '1px solid #383842', paddingTop: 10 }}>
                <div style={{ fontWeight: 800, fontSize: 13, marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <ChefHat size={15} color="#ffb300" /> Receta / Insumos ({generatedDish.ingredients.length})
                </div>
                {generatedDish.ingredients.map((ing, idx) => (
                  <div key={idx} style={{ fontSize: 12.5, color: '#d1d5db' }}>
                    • {ing.supplyName}: {ing.quantityRequired} {ing.unit}
                  </div>
                ))}
              </div>

              {generatedDish.extras.length > 0 && (
                <div>
                  <div style={{ fontWeight: 800, fontSize: 13, marginBottom: 4 }}>
                    Extras Sugeridos ({generatedDish.extras.length})
                  </div>
                  {generatedDish.extras.map((ex, idx) => (
                    <div key={idx} style={{ fontSize: 12.5, color: '#d1d5db' }}>
                      • {ex.name} (+${Number(ex.priceUsd).toFixed(2)})
                    </div>
                  ))}
                </div>
              )}

              {canEditMenu ? (
                <button className="btn btn-green" onClick={handleSaveGeneratedDish}>
                  <PlusCircle size={16} /> Guardar Plato en el Menú
                </button>
              ) : (
                <div style={{ fontSize: 12, color: '#9ca3af' }}>
                  Solo usuarios con permiso de Menú o Administrador pueden guardar nuevos platos.
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
