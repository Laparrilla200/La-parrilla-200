import React, { useState } from 'react';
import { AlertTriangle, Edit3, Package, Plus, ShoppingCart, Trash2, X } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { isUserAdmin, type SupplyDoc } from '../types/models';

export const InventoryPage: React.FC = () => {
  const {
    loggedUser,
    simulateStaff,
    supplies,
    criticalSupplies,
    restockRecommendations,
    config,
    restockSupply,
    saveSupply,
    deleteSupply
  } = useApp();

  const isAdmin = isUserAdmin(loggedUser) && !simulateStaff;
  const [tab, setTab] = useState<'ALL' | 'RESTOCK'>('ALL');
  const [supplyToRestock, setSupplyToRestock] = useState<SupplyDoc | null>(null);
  const [editingSupply, setEditingSupply] = useState<SupplyDoc | 'NEW' | null>(null);

  const totalBudgetUsd = restockRecommendations.reduce((s, r) => s + r.estimatedCostUsd, 0);
  const totalBudgetBs = restockRecommendations.reduce((s, r) => s + r.estimatedCostBs, 0);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Package size={24} color="#e53935" />
          <div>
            <h2 style={{ margin: 0, fontSize: 19, fontWeight: 900 }}>Inventario e Insumos</h2>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>Descuento automático en cada plato vendido y alertas de stock crítico</div>
          </div>
        </div>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => setEditingSupply('NEW')}>
            <Plus size={16} /> Nuevo Insumo
          </button>
        )}
      </div>

      {/* KPIs */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 10 }}>
        <div className="card">
          <div style={{ fontSize: 12, color: '#9ca3af' }}>Total Insumos</div>
          <div style={{ fontSize: 24, fontWeight: 900 }}>{supplies.length}</div>
        </div>
        <div
          className="card"
          style={{
            borderColor: criticalSupplies.length > 0 ? '#d32f2f' : undefined,
            background: criticalSupplies.length > 0 ? 'rgba(211,47,47,0.16)' : undefined
          }}
        >
          <div style={{ fontSize: 12, color: criticalSupplies.length > 0 ? '#fca5a5' : '#9ca3af' }}>En Punto Crítico</div>
          <div style={{ fontSize: 24, fontWeight: 900, color: criticalSupplies.length > 0 ? '#ff5252' : '#43a047' }}>
            {criticalSupplies.length}
          </div>
        </div>
        <div className="card">
          <div style={{ fontSize: 12, color: '#9ca3af' }}>Presupuesto Reabastecimiento</div>
          <div style={{ fontSize: 22, fontWeight: 900, color: '#ffb300' }}>
            ${totalBudgetUsd.toFixed(2)} <span style={{ fontSize: 13 }}>({totalBudgetBs.toFixed(0)} Bs)</span>
          </div>
        </div>
      </div>

      {criticalSupplies.length > 0 && (
        <div
          className="card"
          style={{ background: 'rgba(211,47,47,0.18)', borderColor: '#e53935', display: 'flex', alignItems: 'center', gap: 12 }}
        >
          <AlertTriangle size={24} color="#ef4444" />
          <div>
            <div style={{ fontWeight: 800, fontSize: 14 }}>¡Alerta de Reabastecimiento Crítico!</div>
            <div style={{ fontSize: 12.5, color: '#fecaca' }}>
              {criticalSupplies.length} insumo(s) han llegado a su punto mínimo. Revisa las sugerencias de compra.
            </div>
          </div>
        </div>
      )}

      <div style={{ display: 'flex', gap: 8 }}>
        <button className={`chip ${tab === 'ALL' ? 'active' : ''}`} onClick={() => setTab('ALL')}>
          Todos los Insumos ({supplies.length})
        </button>
        <button className={`chip ${tab === 'RESTOCK' ? 'active' : ''}`} onClick={() => setTab('RESTOCK')}>
          <ShoppingCart size={13} /> Sugerencias de Compra ({restockRecommendations.length})
        </button>
      </div>

      {tab === 'ALL' ? (
        <div className="grid-cards">
          {supplies.map((sup) => {
            const isCrit = sup.currentStock <= sup.minStockThreshold;
            const pct = sup.minStockThreshold > 0 ? Math.min(100, Math.max(0, (sup.currentStock / (sup.minStockThreshold * 2)) * 100)) : 100;
            return (
              <div key={sup.id} className="card" style={{ borderColor: isCrit ? '#d32f2f' : undefined }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                  <div>
                    <div style={{ fontWeight: 800, fontSize: 15 }}>
                      {sup.name}{' '}
                      {isCrit && (
                        <span className="badge-pill" style={{ background: '#d32f2f', color: '#fff', marginLeft: 6 }}>
                          CRÍTICO
                        </span>
                      )}
                    </div>
                    <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 2 }}>
                      Mínimo: {sup.minStockThreshold} {sup.unit} • Costo: ${sup.costUsd.toFixed(2)}/{sup.unit}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', fontWeight: 900, fontSize: 17, color: isCrit ? '#ff5252' : '#fff' }}>
                    {sup.currentStock.toFixed(2)} {sup.unit}
                  </div>
                </div>

                <div style={{ height: 6, background: '#2e2e36', borderRadius: 4, overflow: 'hidden', margin: '10px 0' }}>
                  <div style={{ width: `${pct}%`, height: '100%', background: isCrit ? '#d32f2f' : '#43a047' }} />
                </div>

                <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                  <button className="btn btn-sm btn-primary" onClick={() => setSupplyToRestock(sup)}>
                    <Plus size={14} /> Reponer
                  </button>
                  {isAdmin && (
                    <button className="btn btn-sm btn-surface" onClick={() => setEditingSupply(sup)}>
                      <Edit3 size={14} /> Editar
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : restockRecommendations.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 36, color: '#81c784', fontWeight: 700 }}>
          ¡Excelente! Todos los insumos tienen stock saludable.
        </div>
      ) : (
        <div className="grid-cards">
          {restockRecommendations.map((item) => (
            <div key={item.supply.id} className="card" style={{ borderColor: '#ffb300' }}>
              <div style={{ fontWeight: 800, fontSize: 15.5 }}>{item.supply.name}</div>
              <div style={{ fontSize: 12.5, color: '#fca5a5', marginTop: 4 }}>
                Stock actual: {item.supply.currentStock.toFixed(2)} {item.supply.unit} (Faltan {item.shortageAmount.toFixed(1)} {item.supply.unit})
              </div>
              <div style={{ fontSize: 13.5, fontWeight: 800, color: '#fde047', margin: '8px 0' }}>
                Comprar +{item.toBuyAmount} {item.supply.unit} • Est: ${item.estimatedCostUsd.toFixed(2)} ({item.estimatedCostBs.toFixed(0)} Bs)
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  className="btn btn-sm btn-primary"
                  style={{ flex: 1 }}
                  onClick={() => restockSupply(item.supply.id, item.toBuyAmount)}
                >
                  Registrar Compra (+{item.toBuyAmount} {item.supply.unit})
                </button>
                <button className="btn btn-sm btn-surface" onClick={() => setSupplyToRestock(item.supply)}>
                  Otra Cant.
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Reponer Stock */}
      {supplyToRestock && (
        <RestockModal
          supply={supplyToRestock}
          exchangeRate={config.exchangeRateUsdToBs || 65}
          onClose={() => setSupplyToRestock(null)}
          onConfirm={async (amount) => {
            await restockSupply(supplyToRestock.id, amount);
            setSupplyToRestock(null);
          }}
        />
      )}

      {/* Modal Crear / Editar Insumo */}
      {editingSupply && (
        <EditSupplyModal
          initial={editingSupply === 'NEW' ? null : editingSupply}
          onClose={() => setEditingSupply(null)}
          onSave={async (sup) => {
            await saveSupply(sup);
            setEditingSupply(null);
          }}
          onDelete={
            editingSupply !== 'NEW'
              ? async () => {
                  await deleteSupply(editingSupply.id);
                  setEditingSupply(null);
                }
              : undefined
          }
        />
      )}
    </div>
  );
};

const RestockModal: React.FC<{
  supply: SupplyDoc;
  exchangeRate: number;
  onClose: () => void;
  onConfirm: (amount: number) => Promise<void>;
}> = ({ supply, exchangeRate, onClose, onConfirm }) => {
  const [val, setVal] = useState(String(supply.recommendedRestock || 10));
  const amount = parseFloat(val) || 0;
  const costUsd = amount * supply.costUsd;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3 style={{ margin: 0 }}>Reabastecer: {supply.name}</h3>
          <button className="btn btn-surface btn-sm" onClick={onClose}><X size={16} /></button>
        </div>
        <p style={{ fontSize: 12.5, color: '#9ca3af', marginTop: 0 }}>
          Stock actual: {supply.currentStock.toFixed(2)} {supply.unit}
        </p>
        <label className="label">Cantidad a ingresar ({supply.unit})</label>
        <input className="input" type="number" step="0.1" value={val} onChange={(e) => setVal(e.target.value)} />
        <div style={{ margin: '10px 0 16px', color: '#ffb300', fontWeight: 800, fontSize: 13 }}>
          Costo estimado: ${costUsd.toFixed(2)} USD • {(costUsd * exchangeRate).toFixed(2)} Bs
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-outline" style={{ flex: 1 }} onClick={onClose}>Cancelar</button>
          <button
            className="btn btn-primary"
            style={{ flex: 1 }}
            disabled={amount <= 0}
            onClick={() => onConfirm(amount)}
          >
            Registrar Ingreso
          </button>
        </div>
      </div>
    </div>
  );
};

const EditSupplyModal: React.FC<{
  initial: SupplyDoc | null;
  onClose: () => void;
  onSave: (sup: SupplyDoc) => Promise<void>;
  onDelete?: () => Promise<void>;
}> = ({ initial, onClose, onSave, onDelete }) => {
  const [name, setName] = useState(initial?.name || '');
  const [stock, setStock] = useState(String(initial?.currentStock ?? 10));
  const [threshold, setThreshold] = useState(String(initial?.minStockThreshold ?? 5));
  const [cost, setCost] = useState(String(initial?.costUsd ?? 2));
  const [rec, setRec] = useState(String(initial?.recommendedRestock ?? 10));
  const [unit, setUnit] = useState(initial?.unit || 'kg');

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>{initial ? 'Editar Insumo' : 'Nuevo Insumo'}</h3>
          <button className="btn btn-surface btn-sm" onClick={onClose}><X size={16} /></button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div>
            <label className="label">Nombre del Insumo</label>
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <div>
              <label className="label">Stock Actual</label>
              <input className="input" type="number" step="0.1" value={stock} onChange={(e) => setStock(e.target.value)} />
            </div>
            <div>
              <label className="label">Punto Crítico</label>
              <input className="input" type="number" step="0.1" value={threshold} onChange={(e) => setThreshold(e.target.value)} />
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <div>
              <label className="label">Costo USD ($)</label>
              <input className="input" type="number" step="0.01" value={cost} onChange={(e) => setCost(e.target.value)} />
            </div>
            <div>
              <label className="label">Compra Sugerida</label>
              <input className="input" type="number" step="0.1" value={rec} onChange={(e) => setRec(e.target.value)} />
            </div>
          </div>
          <div>
            <label className="label">Unidad de Medida</label>
            <div style={{ display: 'flex', gap: 6 }}>
              {['kg', 'g', 'ud', 'lt', 'porc'].map((u) => (
                <button key={u} type="button" className={`chip ${unit === u ? 'active' : ''}`} onClick={() => setUnit(u)}>
                  {u}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
          {onDelete && (
            <button className="btn btn-danger-outline" onClick={onDelete}>
              <Trash2 size={15} />
            </button>
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
                currentStock: parseFloat(stock) || 0,
                minStockThreshold: parseFloat(threshold) || 5,
                unit,
                costUsd: parseFloat(cost) || 0,
                recommendedRestock: parseFloat(rec) || 10
              })
            }
          >
            Guardar
          </button>
        </div>
      </div>
    </div>
  );
};
