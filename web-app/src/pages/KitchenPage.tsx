import React, { useMemo, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle,
  Clock,
  Maximize2,
  Printer,
  Smartphone,
  Tablet,
  Trash2,
  UtensilsCrossed,
  X
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { parseCartItems, type OrderDoc } from '../types/models';

export const KitchenPage: React.FC = () => {
  const {
    orders,
    paperWidth,
    setPaperWidth,
    updateOrderStatus,
    cancelOrder,
    printKitchenComanda,
    reprintOrderReceipt
  } = useApp();

  const [filterStatus, setFilterStatus] = useState<'PENDIENTES' | 'LISTOS' | 'TODOS'>('PENDIENTES');
  const [isTabletMode, setIsTabletMode] = useState(true);
  const [detailOrder, setDetailOrder] = useState<OrderDoc | null>(null);
  const [orderToCancel, setOrderToCancel] = useState<OrderDoc | null>(null);

  const pendingCount = orders.filter((o) => o.status === 'EN_COCINA').length;
  const readyCount = orders.filter((o) => o.status === 'LISTO').length;

  const filteredOrders = useMemo(() => {
    if (filterStatus === 'PENDIENTES') return orders.filter((o) => o.status === 'EN_COCINA');
    if (filterStatus === 'LISTOS') return orders.filter((o) => o.status === 'LISTO');
    return orders.filter((o) => o.status !== 'CANCELADO');
  }, [orders, filterStatus]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* Cabecera Cocina KDS */}
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <UtensilsCrossed size={24} color="#e53935" />
          <div>
            <h2 style={{ margin: 0, fontSize: 19, fontWeight: 900 }}>Monitor de Cocina (KDS)</h2>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>
              Haz clic en cualquier comanda para abrirla en pantalla gigante de tablet y marcar platos listos
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          <button
            className={`btn btn-sm ${!isTabletMode ? 'btn-primary' : 'btn-surface'}`}
            onClick={() => setIsTabletMode(false)}
          >
            <Smartphone size={14} /> Vista Teléfono
          </button>
          <button
            className={`btn btn-sm ${isTabletMode ? 'btn-blue' : 'btn-surface'}`}
            onClick={() => setIsTabletMode(true)}
          >
            <Tablet size={14} /> Vista Tablet KDS
          </button>
          <span
            className="badge-pill"
            style={{ background: pendingCount > 0 ? '#d32f2f' : '#2e7d32', color: '#fff', padding: '6px 12px' }}
          >
            {pendingCount} por preparar
          </span>
        </div>
      </div>

      {/* Filtros + Selector Impresora Cocina */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className={`chip ${filterStatus === 'PENDIENTES' ? 'active' : ''}`}
            onClick={() => setFilterStatus('PENDIENTES')}
          >
            Pendientes ({pendingCount})
          </button>
          <button
            className={`chip ${filterStatus === 'LISTOS' ? 'active' : ''}`}
            onClick={() => setFilterStatus('LISTOS')}
          >
            Listos ({readyCount})
          </button>
          <button
            className={`chip ${filterStatus === 'TODOS' ? 'active' : ''}`}
            onClick={() => setFilterStatus('TODOS')}
          >
            Todos ({orders.filter((o) => o.status !== 'CANCELADO').length})
          </button>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Printer size={14} color="#9ca3af" />
          <button className={`chip ${paperWidth === 58 ? 'active' : ''}`} onClick={() => setPaperWidth(58)}>58mm</button>
          <button className={`chip ${paperWidth === 80 ? 'active' : ''}`} onClick={() => setPaperWidth(80)}>80mm</button>
        </div>
      </div>

      {/* Comandas */}
      {filteredOrders.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: 48 }}>
          <CheckCircle size={48} color="#43a047" style={{ marginBottom: 10 }} />
          <div style={{ fontSize: 18, fontWeight: 800 }}>¡Todo al día en cocina!</div>
          <div style={{ fontSize: 13, color: '#9ca3af' }}>No hay comandas pendientes en esta vista.</div>
        </div>
      ) : (
        <div className={isTabletMode ? 'grid-kds-tablet' : ''} style={!isTabletMode ? { display: 'flex', flexDirection: 'column', gap: 12 } : undefined}>
          {filteredOrders.map((order) => {
            const items = parseCartItems(order.itemsJson);
            const elapsedMin = Math.max(0, Math.floor((Date.now() - order.timestamp) / 60000));
            const isUrgent = elapsedMin >= 15 && order.status === 'EN_COCINA';
            const borderColor =
              order.status === 'EN_COCINA'
                ? isUrgent
                  ? '#d32f2f'
                  : '#ffb300'
                : order.status === 'LISTO'
                ? '#43a047'
                : '#4b5563';

            return (
              <div
                key={order.id}
                className="card"
                style={{ borderColor, borderWidth: isUrgent ? 2 : 1, cursor: 'pointer' }}
                onClick={() => setDetailOrder(order)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span
                      style={{
                        background: '#d32f2f',
                        color: '#fff',
                        fontWeight: 900,
                        fontSize: 17,
                        padding: '4px 10px',
                        borderRadius: 8
                      }}
                    >
                      #{String(order.orderNumber).padStart(3, '0')}
                    </span>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: 16 }}>{order.tableOrDestination}</div>
                      <div style={{ fontSize: 12, color: '#9ca3af' }}>{order.customerName}</div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: 12, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 4 }}>
                        <Clock size={13} /> {elapsedMin} min
                      </div>
                      <span
                        style={{
                          fontSize: 11,
                          fontWeight: 800,
                          color: borderColor
                        }}
                      >
                        {order.status === 'EN_COCINA' ? 'EN PREPARACIÓN' : order.status === 'LISTO' ? '¡LISTO!' : order.status}
                      </span>
                    </div>
                    <Maximize2 size={18} color="#38bdf8" />
                  </div>
                </div>

                <div style={{ borderTop: '1px solid #2e2e38', paddingTop: 10, display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {items.map((item, idx) => (
                    <div key={idx}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <span
                          style={{
                            width: isTabletMode ? 30 : 24,
                            height: isTabletMode ? 30 : 24,
                            borderRadius: '50%',
                            background: '#e53935',
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 900,
                            fontSize: isTabletMode ? 15 : 12
                          }}
                        >
                          {item.quantity}
                        </span>
                        <span style={{ fontWeight: 800, fontSize: isTabletMode ? 16 : 14 }}>
                          {item.dishName.toUpperCase()}
                        </span>
                      </div>
                      {item.excludedIngredientNames?.length > 0 && (
                        <div style={{ marginLeft: 38, marginTop: 3, color: '#fca5a5', fontWeight: 800, fontSize: 12.5, display: 'flex', alignItems: 'center', gap: 4 }}>
                          <AlertTriangle size={14} /> MODIFICADO: {item.excludedIngredientNames.map((x) => `SIN ${x.toUpperCase()}`).join(', ')}
                        </div>
                      )}
                      {item.specialInstructions && (
                        <div style={{ marginLeft: 38, marginTop: 2, color: '#fde047', fontWeight: 700, fontSize: 12.5 }}>
                          👉 NOTA: {item.specialInstructions}
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                {order.notes && (
                  <div style={{ marginTop: 10, background: '#26262e', padding: 8, borderRadius: 8, fontSize: 12.5, color: '#fef08a' }}>
                    Observación: {order.notes}
                  </div>
                )}

                <div
                  style={{ display: 'flex', gap: 8, marginTop: 12 }}
                  onClick={(e) => e.stopPropagation()}
                >
                  <button
                    className="btn btn-sm btn-outline"
                    onClick={() => printKitchenComanda(order)}
                  >
                    <Printer size={14} /> ({paperWidth}mm)
                  </button>
                  {order.status === 'EN_COCINA' ? (
                    <button
                      className="btn btn-sm btn-green"
                      style={{ flex: 1 }}
                      onClick={() => updateOrderStatus(order.id, 'LISTO')}
                    >
                      ¡Listo para Servir!
                    </button>
                  ) : order.status === 'LISTO' ? (
                    <button
                      className="btn btn-sm btn-surface"
                      style={{ flex: 1 }}
                      onClick={() => updateOrderStatus(order.id, 'ENTREGADO')}
                    >
                      Despachado / Entregado
                    </button>
                  ) : (
                    <button
                      className="btn btn-sm btn-surface"
                      style={{ flex: 1 }}
                      onClick={() => reprintOrderReceipt(order)}
                    >
                      Ver Factura
                    </button>
                  )}
                  <button
                    className="btn btn-sm btn-danger-outline"
                    onClick={() => setOrderToCancel(order)}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Vista Tablet Pantalla Completa */}
      {detailOrder && (
        <TabletDetailModal
          order={detailOrder}
          paperWidth={paperWidth}
          onClose={() => setDetailOrder(null)}
          onPrint={() => printKitchenComanda(detailOrder)}
          onMarkReady={async () => {
            await updateOrderStatus(detailOrder.id, 'LISTO');
            setDetailOrder(null);
          }}
          onCancelRequest={() => {
            setOrderToCancel(detailOrder);
            setDetailOrder(null);
          }}
        />
      )}

      {/* Confirmación Cancelar Pedido (Restaura Inventario) */}
      {orderToCancel && (
        <div className="modal-backdrop" onClick={() => setOrderToCancel(null)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <h3 style={{ marginTop: 0 }}>¿Cancelar Pedido #{orderToCancel.orderNumber}?</h3>
            <p style={{ color: '#d4d4d8', fontSize: 13.5 }}>
              Al cancelar este pedido, los insumos descontados se devolverán automáticamente al inventario.
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
              <button className="btn btn-outline" onClick={() => setOrderToCancel(null)}>Volver</button>
              <button
                className="btn btn-primary"
                onClick={async () => {
                  const id = orderToCancel.id;
                  setOrderToCancel(null);
                  await cancelOrder(id, true);
                }}
              >
                Sí, Cancelar y Restaurar Stock
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const TabletDetailModal: React.FC<{
  order: OrderDoc;
  paperWidth: number;
  onClose: () => void;
  onPrint: () => void;
  onMarkReady: () => void;
  onCancelRequest: () => void;
}> = ({ order, paperWidth, onClose, onPrint, onMarkReady, onCancelRequest }) => {
  const items = useMemo(() => parseCartItems(order.itemsJson), [order.itemsJson]);
  const [checked, setChecked] = useState<number[]>([]);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-panel modal-panel-xl"
        style={{ border: '2px solid #e53935' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14, flexWrap: 'wrap', gap: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <span style={{ background: '#d32f2f', padding: '8px 16px', borderRadius: 10, fontSize: 24, fontWeight: 900 }}>
              COMANDA #{String(order.orderNumber).padStart(3, '0')}
            </span>
            <div>
              <div style={{ fontSize: 22, fontWeight: 900, color: '#fde047' }}>📍 {order.tableOrDestination}</div>
              <div style={{ fontSize: 15, color: '#e5e7eb' }}>Cliente: {order.customerName}</div>
            </div>
          </div>
          <button className="btn btn-surface" onClick={onClose}><X size={20} /></button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxHeight: '55vh', overflowY: 'auto', marginBottom: 16 }}>
          {items.map((item, idx) => {
            const isDone = checked.includes(idx);
            return (
              <div
                key={idx}
                onClick={() =>
                  setChecked((prev) => (isDone ? prev.filter((x) => x !== idx) : [...prev, idx]))
                }
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 16,
                  padding: 16,
                  borderRadius: 14,
                  background: isDone ? '#1b2b1e' : '#25252e',
                  border: `2px solid ${isDone ? '#43a047' : '#3d3d4a'}`,
                  cursor: 'pointer'
                }}
              >
                <input
                  type="checkbox"
                  checked={isDone}
                  onChange={() => {}}
                  style={{ width: 24, height: 24, accentColor: '#43a047' }}
                />
                <span
                  style={{
                    width: 44,
                    height: 44,
                    borderRadius: '50%',
                    background: isDone ? '#43a047' : '#e53935',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 22,
                    fontWeight: 900,
                    flexShrink: 0
                  }}
                >
                  {item.quantity}
                </span>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 21, fontWeight: 900, color: isDone ? '#a5d6a7' : '#fff' }}>
                    {item.dishName.toUpperCase()}
                  </div>
                  {item.excludedIngredientNames?.length > 0 && (
                    <div style={{ marginTop: 6, background: '#7f1d1d', border: '1px solid #ef4444', padding: '5px 10px', borderRadius: 8, fontWeight: 900, fontSize: 15 }}>
                      ⚠️ ATENCIÓN: {item.excludedIngredientNames.map((x) => `SIN ${x.toUpperCase()}`).join(', ')}
                    </div>
                  )}
                  {item.specialInstructions && (
                    <div style={{ marginTop: 4, color: '#fde047', fontWeight: 800, fontSize: 16 }}>
                      👉 NOTA: {item.specialInstructions}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {order.notes && (
          <div style={{ background: '#2c2c36', padding: 12, borderRadius: 10, color: '#fef9c3', fontSize: 15, marginBottom: 16 }}>
            Observación General: {order.notes}
          </div>
        )}

        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button className="btn btn-surface" style={{ height: 52 }} onClick={onPrint}>
            <Printer size={20} /> Imprimir Comanda ({paperWidth}mm)
          </button>
          <button className="btn btn-danger-outline" style={{ height: 52 }} onClick={onCancelRequest}>
            <Trash2 size={18} /> Cancelar Pedido
          </button>
          <button className="btn btn-green" style={{ flex: 1, height: 52, fontSize: 16 }} onClick={onMarkReady}>
            <CheckCircle size={22} /> ✓ ¡Comanda Lista para Servir!
          </button>
        </div>
      </div>
    </div>
  );
};
