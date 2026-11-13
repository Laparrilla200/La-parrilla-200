import React, { useMemo, useState } from 'react';
import { BookOpen, Edit3, Plus, Trash2, X } from 'lucide-react';
import { useApp } from '../context/AppContext';
import {
  isUserAdmin,
  parseExtras,
  parseIngredients,
  type IngredientRequirement,
  type MenuItemDoc,
  type MenuItemExtra,
  type SupplyDoc
} from '../types/models';

const CATEGORIES = ['Todas', 'Parrillas', 'Hamburguesas & Pepitos', 'Entradas & Guarniciones', 'Bebidas'];

export const MenuAdminPage: React.FC = () => {
  const { loggedUser, simulateStaff, menuItems, supplies, config, saveMenuItem, deleteMenuItem } = useApp();
  const canEdit = (isUserAdmin(loggedUser) && !simulateStaff) || Boolean(loggedUser?.canUseMenuApp);

  const [category, setCategory] = useState('Todas');
  const [editingDish, setEditingDish] = useState<MenuItemDoc | 'NEW' | null>(null);

  const filtered = useMemo(() => {
    if (category === 'Todas') return menuItems;
    return menuItems.filter((m) => m.category.toLowerCase() === category.toLowerCase());
  }, [menuItems, category]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BookOpen size={24} color="#e53935" />
          <div>
            <h2 style={{ margin: 0, fontSize: 19, fontWeight: 900 }}>Menú, Extras y Recetas</h2>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>
              Configura platos, precios, extras opcionales y los insumos a descontar automáticamente
            </div>
          </div>
        </div>
        {canEdit && (
          <button className="btn btn-primary" onClick={() => setEditingDish('NEW')}>
            <Plus size={16} /> Nuevo Plato
          </button>
        )}
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {CATEGORIES.map((c) => (
          <button key={c} className={`chip ${category === c ? 'active' : ''}`} onClick={() => setCategory(c)}>
            {c}
          </button>
        ))}
      </div>

      <div className="grid-cards">
        {filtered.map((dish) => {
          const ingredients = parseIngredients(dish.ingredientsJson);
          const extras = parseExtras(dish.extrasJson);
          return (
            <div key={dish.id} className="card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: 10 }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <span style={{ fontSize: 11, fontWeight: 700, color: '#ffb300', textTransform: 'uppercase' }}>
                    {dish.category}
                  </span>
                  <span
                    className="badge-pill"
                    style={{ background: dish.isAvailable !== false ? '#1b5e20' : '#4b5563', color: '#fff' }}
                  >
                    {dish.isAvailable !== false ? 'Disponible' : 'Pausado'}
                  </span>
                </div>
                <h3 style={{ margin: '4px 0', fontSize: 16, fontWeight: 800 }}>{dish.name}</h3>
                {dish.description && (
                  <p style={{ margin: '0 0 8px', fontSize: 12, color: '#9ca3af' }}>{dish.description}</p>
                )}

                <div style={{ fontSize: 17, fontWeight: 900, color: '#fff', marginBottom: 8 }}>
                  ${dish.priceUsd.toFixed(2)} USD{' '}
                  <span style={{ fontSize: 12, color: '#ffb300' }}>
                    ({(dish.priceUsd * (config.exchangeRateUsdToBs || 65)).toFixed(2)} Bs)
                  </span>
                </div>

                {ingredients.length > 0 && (
                  <div style={{ background: '#24242c', padding: 8, borderRadius: 8, marginBottom: 6 }}>
                    <div style={{ fontSize: 11, fontWeight: 700, color: '#9ca3af', marginBottom: 4 }}>
                      Insumos vinculados ({ingredients.length}):
                    </div>
                    {ingredients.map((ing, i) => (
                      <div key={i} style={{ fontSize: 11.5, color: '#d4d4d8' }}>
                        • {ing.supplyName}: {ing.quantityRequired} {ing.unit}
                      </div>
                    ))}
                  </div>
                )}

                {extras.length > 0 && (
                  <div style={{ background: '#262020', padding: 8, borderRadius: 8 }}>
                    <div style={{ fontSize: 11, fontWeight: 700, color: '#fca5a5', marginBottom: 4 }}>
                      Extras disponibles ({extras.length}):
                    </div>
                    {extras.map((ex, i) => (
                      <div key={i} style={{ fontSize: 11.5, color: '#e5e7eb' }}>
                        + {ex.name} (${ex.priceUsd.toFixed(2)})
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {canEdit && (
                <button className="btn btn-sm btn-surface" onClick={() => setEditingDish(dish)}>
                  <Edit3 size={14} /> Editar Plato, Extras y Receta
                </button>
              )}
            </div>
          );
        })}
      </div>

      {editingDish && (
        <DishEditorModal
          initial={editingDish === 'NEW' ? null : editingDish}
          supplies={supplies}
          onClose={() => setEditingDish(null)}
          onSave={async (doc) => {
            await saveMenuItem(doc);
            setEditingDish(null);
          }}
          onDelete={
            editingDish !== 'NEW'
              ? async () => {
                  await deleteMenuItem(editingDish.id);
                  setEditingDish(null);
                }
              : undefined
          }
        />
      )}
    </div>
  );
};

const DishEditorModal: React.FC<{
  initial: MenuItemDoc | null;
  supplies: SupplyDoc[];
  onClose: () => void;
  onSave: (dish: MenuItemDoc) => Promise<void>;
  onDelete?: () => Promise<void>;
}> = ({ initial, supplies, onClose, onSave, onDelete }) => {
  const [name, setName] = useState(initial?.name || '');
  const [description, setDescription] = useState(initial?.description || '');
  const [category, setCategory] = useState(initial?.category || 'Parrillas');
  const [priceUsd, setPriceUsd] = useState(String(initial?.priceUsd ?? 10));
  const [isAvailable, setIsAvailable] = useState(initial?.isAvailable !== false);
  const [ingredients, setIngredients] = useState<IngredientRequirement[]>(() =>
    parseIngredients(initial?.ingredientsJson)
  );
  const [extras, setExtras] = useState<MenuItemExtra[]>(() => parseExtras(initial?.extrasJson));

  const [selectedSupplyId, setSelectedSupplyId] = useState(supplies[0]?.id || '');
  const [supplyQty, setSupplyQty] = useState('0.2');
  const [extraName, setExtraName] = useState('');
  const [extraPrice, setExtraPrice] = useState('1.50');

  const addIngredient = () => {
    const sup = supplies.find((s) => s.id === selectedSupplyId);
    if (!sup) return;
    setIngredients((prev) => [
      ...prev,
      {
        supplyId: sup.id,
        supplyName: sup.name,
        quantityRequired: parseFloat(supplyQty) || 0.1,
        unit: sup.unit,
        isRemovable: true
      }
    ]);
  };

  const addExtra = () => {
    if (!extraName.trim()) return;
    setExtras((prev) => [
      ...prev,
      { name: extraName.trim(), priceUsd: parseFloat(extraPrice) || 1.0 }
    ]);
    setExtraName('');
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel modal-panel-xl" style={{ maxWidth: 680 }} onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>{initial ? 'Editar Plato del Menú' : 'Crear Nuevo Plato'}</h3>
          <button className="btn btn-surface btn-sm" onClick={onClose}><X size={16} /></button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 10, marginBottom: 10 }}>
          <div>
            <label className="label">Nombre del Plato</label>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div>
            <label className="label">Precio USD ($)</label>
            <input className="input" type="number" step="0.1" value={priceUsd} onChange={(e) => setPriceUsd(e.target.value)} />
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: 10, alignItems: 'end', marginBottom: 10 }}>
          <div>
            <label className="label">Categoría</label>
            <select className="select" value={category} onChange={(e) => setCategory(e.target.value)}>
              {CATEGORIES.filter((c) => c !== 'Todas').map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, paddingBottom: 10, cursor: 'pointer', fontSize: 13 }}>
            <input type="checkbox" checked={isAvailable} onChange={(e) => setIsAvailable(e.target.checked)} />
            Disponible para venta
          </label>
        </div>

        <div style={{ marginBottom: 14 }}>
          <label className="label">Descripción</label>
          <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>

        {/* Sección Insumos */}
        <div className="card" style={{ background: '#23232a', marginBottom: 12 }}>
          <div style={{ fontWeight: 800, fontSize: 13.5, marginBottom: 8 }}>
            1. Insumos de la Receta (Se descuentan solos al vender)
          </div>
          {ingredients.map((ing, idx) => (
            <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6, fontSize: 13 }}>
              <span>• {ing.supplyName}: <strong>{ing.quantityRequired} {ing.unit}</strong></span>
              <button className="btn btn-sm btn-danger-outline" onClick={() => setIngredients((p) => p.filter((_, i) => i !== idx))}>
                <Trash2 size={12} />
              </button>
            </div>
          ))}
          {supplies.length > 0 && (
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <select className="select" value={selectedSupplyId} onChange={(e) => setSelectedSupplyId(e.target.value)}>
                {supplies.map((s) => (
                  <option key={s.id} value={s.id}>{s.name} ({s.unit})</option>
                ))}
              </select>
              <input className="input" style={{ width: 90 }} type="number" step="0.05" value={supplyQty} onChange={(e) => setSupplyQty(e.target.value)} />
              <button type="button" className="btn btn-surface" onClick={addIngredient}>+ Añadir</button>
            </div>
          )}
        </div>

        {/* Sección Extras */}
        <div className="card" style={{ background: '#23232a', marginBottom: 16 }}>
          <div style={{ fontWeight: 800, fontSize: 13.5, marginBottom: 8 }}>
            2. Extras Opcionales (Ej. Queso extra, Papas fritas)
          </div>
          {extras.map((ex, idx) => (
            <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6, fontSize: 13 }}>
              <span>+ {ex.name} (<strong>${ex.priceUsd.toFixed(2)}</strong>)</span>
              <button className="btn btn-sm btn-danger-outline" onClick={() => setExtras((p) => p.filter((_, i) => i !== idx))}>
                <Trash2 size={12} />
              </button>
            </div>
          ))}
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <input className="input" placeholder="Nombre del extra" value={extraName} onChange={(e) => setExtraName(e.target.value)} />
            <input className="input" style={{ width: 100 }} type="number" step="0.1" value={extraPrice} onChange={(e) => setExtraPrice(e.target.value)} />
            <button type="button" className="btn btn-surface" onClick={addExtra}>+ Extra</button>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          {onDelete && (
            <button className="btn btn-danger-outline" onClick={onDelete}><Trash2 size={15} /> Eliminar</button>
          )}
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onClose}>Cancelar</button>
          <button
            className="btn btn-primary"
            style={{ flex: 1 }}
            disabled={!name.trim()}
            onClick={() =>
              onSave({
                id: initial?.id || '',
                name: name.trim(),
                description: description.trim(),
                category,
                priceUsd: parseFloat(priceUsd) || 10,
                isAvailable,
                ingredientsJson: JSON.stringify(ingredients),
                extrasJson: JSON.stringify(extras)
              })
            }
          >
            Guardar Plato
          </button>
        </div>
      </div>
    </div>
  );
};
