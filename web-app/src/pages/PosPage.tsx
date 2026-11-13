import React, { useMemo, useState } from 'react';
import {
  CheckCircle,
  CreditCard,
  Edit3,
  Flame,
  Minus,
  Plus,
  Printer,
  Receipt,
  ShoppingCart,
  SlidersHorizontal,
  Trash2,
  X,
  Zap
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import {
  isUserAdmin,
  parseCartItems,
  parseExtras,
  parseIngredients,
  type CartOrderItem,
  type MenuItemDoc,
  type OrderDoc
} from '../types/models';

const CATEGORIES = ['Todas', 'Parrillas', 'Hamburguesas & Pepitos', 'Entradas & Guarniciones', 'Bebidas'];
const TABLE_PRESETS = ['Mesa 1', 'Mesa 2', 'Mesa 3', 'Mesa 4', 'Mesa 5', 'Mesa 6', 'Barra', 'Para Llevar'];

export const PosPage: React.FC = () => {
  const {
    loggedUser,
    simulateStaff,
    menuItems,
    cart,
    config,
    customerName,
    setCustomerName,
    tableOrDestination,
    setTableOrDestination,
    unpaidOrders,
    paidOrders,
    paperWidth,
    setPaperWidth,
    addToCart,
    updateCartQuantity,
    clearCart,
    submitFastOrderToKitchen,
    submitOrderAndCheckout,
    markOrderPaid,
    quickPayOrderCash,
    editOpenOrder,
    deleteOrder,
    reprintOrderReceipt,
    showToast
  } = useApp();

  const canBill = useMemo(() => {
    if (!loggedUser) return false;
    if (isUserAdmin(loggedUser) && !simulateStaff) return true;
    return loggedUser.canUseBillingPc ?? loggedUser.canUseBillingApp ?? false;
  }, [loggedUser, simulateStaff]);

  const [accountsTab, setAccountsTab] = useState<'PENDING' | 'PAID'>('PENDING');
  const [selectedCategory, setSelectedCategory] = useState('Todas');
  const [orderNote, setOrderNote] = useState('');
  const [customizingDish, setCustomizingDish] = useState<MenuItemDoc | null>(null);
  const [checkoutTargetOrder, setCheckoutTargetOrder] = useState<OrderDoc | 'CART' | null>(null);
  const [editingOpenOrder, setEditingOpenOrder] = useState<OrderDoc | null>(null);
  const [orderToDelete, setOrderToDelete] = useState<OrderDoc | null>(null);
  const [quickPayConfirmOrder, setQuickPayConfirmOrder] = useState<OrderDoc | null>(null);

  const filteredDishes = useMemo(() => {
    if (selectedCategory === 'Todas') return menuItems;
    return menuItems.filter((m) => m.category.toLowerCase() === selectedCategory.toLowerCase());
  }, [menuItems, selectedCategory]);

  const totalCartUsd = cart.reduce((sum, i) => sum + i.priceUsd * i.quantity, 0);
  const totalCartBs = totalCartUsd * (config.exchangeRateUsdToBs || 65);
  const cartCount = cart.reduce((sum, i) => sum + i.quantity, 0);

  return (
    <div className="grid-pos">
      {/* Columna Izquierda: Cliente/Mesa, Cuentas y Catálogo */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {/* Barra Cliente / Mesa / Impresora */}
        <div className="card">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 10 }}>
            <div>
              <label className="label">Cliente</label>
              <input
                className="input"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                placeholder="Nombre del cliente"
              />
            </div>
            <div>
              <label className="label">Mesa / Destino</label>
              <input
                className="input"
                value={tableOrDestination}
                onChange={(e) => setTableOrDestination(e.target.value)}
                placeholder="Mesa 1 / Para Llevar"
              />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 10 }}>
            {TABLE_PRESETS.map((preset) => (
              <button
                key={preset}
                type="button"
                className={`chip ${tableOrDestination.toLowerCase() === preset.toLowerCase() ? 'active' : ''}`}
                onClick={() => setTableOrDestination(preset)}
              >
                {preset}
              </button>
            ))}
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Printer size={15} color="#9ca3af" />
              <span style={{ fontSize: 12, color: '#9ca3af', fontWeight: 600 }}>Impresora SAT 15T2U:</span>
              <button
                type="button"
                className={`chip ${paperWidth === 58 ? 'active' : ''}`}
                onClick={() => setPaperWidth(58)}
              >
                58mm (48mm útil)
              </button>
              <button
                type="button"
                className={`chip ${paperWidth === 80 ? 'active' : ''}`}
                onClick={() => setPaperWidth(80)}
              >
                80mm (72mm útil)
              </button>
            </div>
            <div style={{ fontSize: 12.5, fontWeight: 800, color: '#00e676' }}>
              Tasa del Día: {(config.exchangeRateUsdToBs || 65).toFixed(2)} Bs/$
            </div>
          </div>
        </div>

        {/* Cuentas Abiertas (Por Cobrar vs Pagadas) */}
        <div className={`card ${accountsTab === 'PENDING' && unpaidOrders.length > 0 ? 'card-crimson' : ''}`}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8, marginBottom: 10 }}>
            <div style={{ display: 'flex', gap: 8 }}>
              <button
                type="button"
                className={`btn btn-sm ${accountsTab === 'PENDING' ? 'btn-primary' : 'btn-surface'}`}
                onClick={() => setAccountsTab('PENDING')}
              >
                <Receipt size={14} /> ⏳ Por Cobrar ({unpaidOrders.length})
              </button>
              <button
                type="button"
                className={`btn btn-sm ${accountsTab === 'PAID' ? 'btn-green' : 'btn-surface'}`}
                onClick={() => setAccountsTab('PAID')}
              >
                <CheckCircle size={14} /> ✅ Pagadas ({paidOrders.length})
              </button>
            </div>
            <span style={{ fontSize: 11.5, color: '#9ca3af' }}>
              Un clic en «PAGADO» liquida la cuenta al instante
            </span>
          </div>

          {accountsTab === 'PENDING' ? (
            unpaidOrders.length === 0 ? (
              <div style={{ fontSize: 12.5, color: '#9ca3af', padding: '6px 0' }}>
                ✅ Todas las cuentas del turno están cobradas y al día.
              </div>
            ) : (
              <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 4 }}>
                {unpaidOrders.map((order) => (
                  <div
                    key={order.id}
                    style={{
                      minWidth: 265,
                      background: '#25252c',
                      border: '1px solid #3f3f4e',
                      borderRadius: 10,
                      padding: 10,
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 8
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div>
                        <div style={{ fontWeight: 800, fontSize: 13 }}>
                          #{order.orderNumber} • {order.tableOrDestination}
                        </div>
                        <div style={{ fontSize: 11.5, color: '#9ca3af' }}>{order.customerName}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontWeight: 900, fontSize: 14, color: '#ffb300' }}>
                          ${order.totalUsd.toFixed(2)}
                        </div>
                        <div style={{ fontSize: 11, color: '#d4d4d8' }}>{order.totalBs.toFixed(0)} Bs</div>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                      <button
                        type="button"
                        className="btn btn-sm btn-surface"
                        onClick={() => setEditingOpenOrder(order)}
                        title="Modificar o agregar platos/bebidas"
                      >
                        <Edit3 size={13} color="#38bdf8" /> Editar
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-green"
                        disabled={!canBill}
                        onClick={() => setQuickPayConfirmOrder(order)}
                      >
                        <Zap size={13} /> PAGADO
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-primary"
                        disabled={!canBill}
                        onClick={() => setCheckoutTargetOrder(order)}
                      >
                        COBRAR
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-danger-outline"
                        onClick={() => setOrderToDelete(order)}
                        title="Eliminar cuenta"
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )
          ) : paidOrders.length === 0 ? (
            <div style={{ fontSize: 12.5, color: '#9ca3af', padding: '6px 0' }}>
              Aún no se han registrado cobros en el historial reciente.
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 4 }}>
              {paidOrders.map((order) => (
                <div
                  key={order.id}
                  style={{
                    minWidth: 250,
                    background: '#1c271e',
                    border: '1px solid #2e7d32',
                    borderRadius: 10,
                    padding: 10,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 8
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: 13, color: '#81c784' }}>
                        ✅ #{order.orderNumber} • {order.tableOrDestination}
                      </div>
                      <div style={{ fontSize: 11, color: '#d1d5db' }}>
                        ${order.totalUsd.toFixed(2)} ({order.totalBs.toFixed(0)} Bs) • {order.paymentMethod.replace('_', ' ')}
                      </div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: 6 }}>
                    <button
                      type="button"
                      className="btn btn-sm btn-surface"
                      style={{ flex: 1 }}
                      onClick={() => reprintOrderReceipt(order)}
                    >
                      <Printer size={13} color="#81d4fa" /> Reimprimir Factura
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm btn-danger-outline"
                      onClick={() => setOrderToDelete(order)}
                      title="Eliminar cuenta"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Categorías y Catálogo */}
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              type="button"
              className={`chip ${selectedCategory === cat ? 'active' : ''}`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat}
            </button>
          ))}
        </div>

        <div className="grid-cards">
          {filteredDishes.map((dish) => {
            const qtyInCart = cart
              .filter((c) => c.menuItemId === dish.id)
              .reduce((s, c) => s + c.quantity, 0);
            const priceBs = dish.priceUsd * (config.exchangeRateUsdToBs || 65);
            return (
              <div
                key={dish.id}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  opacity: dish.isAvailable === false ? 0.5 : 1
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                    <span style={{ fontSize: 11, fontWeight: 700, color: '#ffb300', textTransform: 'uppercase' }}>
                      {dish.category}
                    </span>
                    {qtyInCart > 0 && (
                      <span className="badge-pill" style={{ background: '#d32f2f', color: '#fff' }}>
                        {qtyInCart} en comanda
                      </span>
                    )}
                  </div>
                  <h3 style={{ margin: '4px 0', fontSize: 15.5, fontWeight: 800 }}>{dish.name}</h3>
                  {dish.description && (
                    <p style={{ margin: '0 0 10px', fontSize: 12, color: '#9ca3af', lineHeight: 1.4 }}>
                      {dish.description}
                    </p>
                  )}
                </div>

                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 }}>
                    <span style={{ fontSize: 18, fontWeight: 900, color: '#fff' }}>
                      ${dish.priceUsd.toFixed(2)}
                    </span>
                    <span style={{ fontSize: 12, fontWeight: 700, color: '#ffb300' }}>
                      {priceBs.toFixed(2)} Bs
                    </span>
                  </div>

                  <div style={{ display: 'flex', gap: 8 }}>
                    <button
                      type="button"
                      className="btn btn-surface btn-sm"
                      style={{ flex: 1 }}
                      disabled={dish.isAvailable === false}
                      onClick={() => setCustomizingDish(dish)}
                    >
                      <SlidersHorizontal size={14} /> Personalizar / Extras
                    </button>
                    {qtyInCart > 0 && (
                      <button
                        type="button"
                        className="btn btn-surface btn-sm"
                        onClick={() => {
                          const idx = cart.findIndex((c) => c.menuItemId === dish.id);
                          if (idx >= 0) updateCartQuantity(idx, cart[idx].quantity - 1);
                        }}
                      >
                        <Minus size={14} />
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      disabled={dish.isAvailable === false}
                      onClick={() =>
                        addToCart({
                          menuItemId: dish.id,
                          dishName: dish.name,
                          quantity: 1,
                          priceUsd: dish.priceUsd,
                          excludedIngredientNames: [],
                          specialInstructions: ''
                        })
                      }
                    >
                      <Plus size={15} /> Agregar
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Columna Derecha: Comanda Actual (Sticky) */}
      <div className="card card-crimson" style={{ position: 'sticky', top: 76 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <ShoppingCart size={20} color="#e53935" />
            <div>
              <div style={{ fontWeight: 800, fontSize: 16 }}>Comanda Actual ({cartCount})</div>
              <div style={{ fontSize: 11.5, color: '#9ca3af' }}>
                {tableOrDestination} • {customerName}
              </div>
            </div>
          </div>
          {cart.length > 0 && (
            <button type="button" className="btn btn-sm btn-danger-outline" onClick={clearCart}>
              Vaciar
            </button>
          )}
        </div>

        {cart.length === 0 ? (
          <div style={{ padding: '28px 12px', textAlign: 'center', color: '#9ca3af', fontSize: 13 }}>
            Agrega platos del menú para enviar la comanda a cocina o cobrar directamente.
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: '42vh', overflowY: 'auto', marginBottom: 12 }}>
              {cart.map((item, idx) => (
                <div
                  key={`${item.menuItemId}-${idx}`}
                  style={{
                    background: '#222228',
                    borderRadius: 10,
                    padding: 10,
                    border: '1px solid #353540'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8 }}>
                    <div style={{ fontWeight: 700, fontSize: 13.5 }}>{item.dishName}</div>
                    <div style={{ fontWeight: 800, color: '#ffb300', fontSize: 13.5 }}>
                      ${(item.priceUsd * item.quantity).toFixed(2)}
                    </div>
                  </div>
                  {item.excludedIngredientNames.length > 0 && (
                    <div style={{ fontSize: 11.5, color: '#fca5a5', fontWeight: 700, marginTop: 3 }}>
                      ⚠️ SIN: {item.excludedIngredientNames.join(', ').toUpperCase()}
                    </div>
                  )}
                  {item.specialInstructions && (
                    <div style={{ fontSize: 11.5, color: '#fde047', marginTop: 2 }}>
                      👉 Nota: {item.specialInstructions}
                    </div>
                  )}
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                    <span style={{ fontSize: 11.5, color: '#9ca3af' }}>
                      ${item.priceUsd.toFixed(2)} c/u
                    </span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <button
                        type="button"
                        className="btn btn-sm btn-surface"
                        onClick={() => updateCartQuantity(idx, item.quantity - 1)}
                      >
                        <Minus size={13} />
                      </button>
                      <span style={{ fontWeight: 800, minWidth: 20, textAlign: 'center' }}>{item.quantity}</span>
                      <button
                        type="button"
                        className="btn btn-sm btn-surface"
                        onClick={() => updateCartQuantity(idx, item.quantity + 1)}
                      >
                        <Plus size={13} />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div style={{ marginBottom: 12 }}>
              <label className="label">Observación general de la comanda (opcional)</label>
              <input
                className="input"
                value={orderNote}
                onChange={(e) => setOrderNote(e.target.value)}
                placeholder="Ej. Para llevar, salsa aparte, término medio..."
              />
            </div>

            <div style={{ borderTop: '1px dashed #40404a', paddingTop: 10, marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 17, fontWeight: 900 }}>
                <span>TOTAL USD:</span>
                <span>${totalCartUsd.toFixed(2)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14, fontWeight: 800, color: '#ffb300' }}>
                <span>TOTAL Bs:</span>
                <span>{totalCartBs.toFixed(2)} Bs</span>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <button
                type="button"
                className="btn btn-orange"
                style={{ width: '100%', height: 46 }}
                onClick={() => {
                  submitFastOrderToKitchen(orderNote);
                  setOrderNote('');
                }}
              >
                <Flame size={18} /> ⚡ Enviar a Cocina e Imprimir ({paperWidth}mm)
              </button>
              <button
                type="button"
                className="btn btn-primary"
                style={{ width: '100%', height: 46 }}
                disabled={!canBill}
                onClick={() => setCheckoutTargetOrder('CART')}
              >
                <CreditCard size={18} /> 💳 Cobrar Directo e Imprimir Factura
              </button>
            </div>
          </>
        )}
      </div>

      {/* Modal Personalizar Plato & Extras */}
      {customizingDish && (
        <CustomizeDishModal
          dish={customizingDish}
          exchangeRate={config.exchangeRateUsdToBs || 65}
          onClose={() => setCustomizingDish(null)}
          onConfirm={(itemsToAdd) => {
            itemsToAdd.forEach((it) => addToCart(it));
            setCustomizingDish(null);
          }}
        />
      )}

      {/* Modal Cobro Multi-Moneda */}
      {checkoutTargetOrder && (
        <CheckoutModal
          totalUsd={checkoutTargetOrder === 'CART' ? totalCartUsd : checkoutTargetOrder.totalUsd}
          exchangeRate={config.exchangeRateUsdToBs || 65}
          title={
            checkoutTargetOrder === 'CART'
              ? `Cobrar Pedido (${tableOrDestination})`
              : `Cobrar Cuenta #${checkoutTargetOrder.orderNumber} (${checkoutTargetOrder.tableOrDestination})`
          }
          onClose={() => setCheckoutTargetOrder(null)}
          onConfirm={async (pay) => {
            if (checkoutTargetOrder === 'CART') {
              await submitOrderAndCheckout({ ...pay, isPaid: true, notes: orderNote });
              setOrderNote('');
            } else {
              await markOrderPaid(
                checkoutTargetOrder,
                pay.paymentMethod,
                pay.amountUsdPaid,
                pay.amountPagoMovilBs,
                pay.amountPuntoVentaBs,
                pay.referenceCode
              );
            }
            setCheckoutTargetOrder(null);
          }}
        />
      )}

      {/* Modal Editar Cuenta Abierta */}
      {editingOpenOrder && (
        <EditOpenOrderModal
          order={editingOpenOrder}
          menuItems={menuItems}
          exchangeRate={config.exchangeRateUsdToBs || 65}
          onClose={() => setEditingOpenOrder(null)}
          onSave={async (updatedItems, newlyAdded, notes) => {
            await editOpenOrder(editingOpenOrder.id, updatedItems, newlyAdded, notes);
            setEditingOpenOrder(null);
          }}
        />
      )}

      {/* Confirmación Pago Rápido Efectivo */}
      {quickPayConfirmOrder && (
        <div className="modal-backdrop" onClick={() => setQuickPayConfirmOrder(null)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <h3 style={{ marginTop: 0 }}>¿Confirmar Cobro Rápido en Efectivo?</h3>
            <p style={{ color: '#d4d4d8', fontSize: 13.5 }}>
              Orden <strong>#{quickPayConfirmOrder.orderNumber}</strong> ({quickPayConfirmOrder.tableOrDestination}) por{' '}
              <strong style={{ color: '#ffb300' }}>${quickPayConfirmOrder.totalUsd.toFixed(2)}</strong>. Se marcará como pagada al instante y saldrá de «Por Cobrar».
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
              <button className="btn btn-outline" onClick={() => setQuickPayConfirmOrder(null)}>
                Cancelar
              </button>
              <button
                className="btn btn-green"
                onClick={async () => {
                  const target = quickPayConfirmOrder;
                  setQuickPayConfirmOrder(null);
                  await quickPayOrderCash(target);
                }}
              >
                ✓ Sí, Marcar PAGADO
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmación Eliminar Cuenta */}
      {orderToDelete && (
        <div className="modal-backdrop" onClick={() => setOrderToDelete(null)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <h3 style={{ marginTop: 0, color: '#ef4444' }}>
              ¿Eliminar Cuenta #{orderToDelete.orderNumber}?
            </h3>
            <p style={{ color: '#d4d4d8', fontSize: 13.5 }}>
              Esta acción borrará la cuenta ({orderToDelete.tableOrDestination}) de forma definitiva en la nube. Ideal para limpiar pedidos de prueba.
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
              <button className="btn btn-outline" onClick={() => setOrderToDelete(null)}>
                Volver
              </button>
              <button
                className="btn btn-primary"
                onClick={async () => {
                  const id = orderToDelete.id;
                  setOrderToDelete(null);
                  await deleteOrder(id);
                  showToast('Cuenta eliminada');
                }}
              >
                <Trash2 size={15} /> Sí, Eliminar Definitivamente
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const CustomizeDishModal: React.FC<{
  dish: MenuItemDoc;
  exchangeRate: number;
  onClose: () => void;
  onConfirm: (items: CartOrderItem[]) => void;
}> = ({ dish, exchangeRate, onClose, onConfirm }) => {
  const recipe = useMemo(() => parseIngredients(dish.ingredientsJson), [dish]);
  const extras = useMemo(() => parseExtras(dish.extrasJson), [dish]);

  const [qty, setQty] = useState(1);
  const [excluded, setExcluded] = useState<string[]>([]);
  const [selectedExtras, setSelectedExtras] = useState<number[]>([]);
  const [notes, setNotes] = useState('');

  const extrasTotal = selectedExtras.reduce((sum, idx) => sum + (extras[idx]?.priceUsd || 0), 0);
  const totalUnitUsd = dish.priceUsd + extrasTotal;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <h3 style={{ margin: 0 }}>{dish.name}</h3>
            <div style={{ fontSize: 13, color: '#ffb300', fontWeight: 800 }}>
              ${totalUnitUsd.toFixed(2)} USD ({(totalUnitUsd * exchangeRate).toFixed(2)} Bs)
            </div>
          </div>
          <button className="btn btn-surface btn-sm" onClick={onClose}><X size={16} /></button>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14 }}>
          <span style={{ fontWeight: 700, fontSize: 13 }}>Cantidad:</span>
          <button className="btn btn-surface btn-sm" onClick={() => setQty(Math.max(1, qty - 1))}><Minus size={14} /></button>
          <span style={{ fontWeight: 900, fontSize: 16 }}>{qty}</span>
          <button className="btn btn-surface btn-sm" onClick={() => setQty(qty + 1)}><Plus size={14} /></button>
        </div>

        {recipe.filter((r) => r.isRemovable !== false).length > 0 && (
          <div style={{ marginBottom: 14 }}>
            <div className="label">Quitar ingredientes (no se descuentan del inventario):</div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {recipe.filter((r) => r.isRemovable !== false).map((ing) => {
                const isOff = excluded.includes(ing.supplyName);
                return (
                  <button
                    key={ing.supplyId || ing.supplyName}
                    type="button"
                    className={`chip ${isOff ? 'active' : ''}`}
                    onClick={() =>
                      setExcluded((prev) =>
                        isOff ? prev.filter((x) => x !== ing.supplyName) : [...prev, ing.supplyName]
                      )
                    }
                  >
                    {isOff ? `SIN ${ing.supplyName}` : ing.supplyName}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {extras.length > 0 && (
          <div style={{ marginBottom: 14 }}>
            <div className="label">Extras disponibles:</div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {extras.map((ex, idx) => {
                const active = selectedExtras.includes(idx);
                return (
                  <button
                    key={`${ex.name}-${idx}`}
                    type="button"
                    className={`chip ${active ? 'active' : ''}`}
                    onClick={() =>
                      setSelectedExtras((prev) =>
                        active ? prev.filter((x) => x !== idx) : [...prev, idx]
                      )
                    }
                  >
                    + {ex.name} (${ex.priceUsd.toFixed(2)})
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <div style={{ marginBottom: 16 }}>
          <label className="label">Instrucciones para cocina</label>
          <input
            className="input"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Ej. Carne término 3/4, bien cocida..."
          />
        </div>

        <button
          className="btn btn-primary"
          style={{ width: '100%', height: 46 }}
          onClick={() => {
            const chosenExtraNames = selectedExtras.map((i) => extras[i].name);
            const displayName =
              chosenExtraNames.length > 0 ? `${dish.name} (+ ${chosenExtraNames.join(', ')})` : dish.name;
            onConfirm([
              {
                menuItemId: dish.id,
                dishName: displayName,
                quantity: qty,
                priceUsd: totalUnitUsd,
                excludedIngredientNames: excluded,
                specialInstructions: notes.trim()
              }
            ]);
          }}
        >
          Agregar a la Comanda (${(totalUnitUsd * qty).toFixed(2)})
        </button>
      </div>
    </div>
  );
};

const CheckoutModal: React.FC<{
  title: string;
  totalUsd: number;
  exchangeRate: number;
  onClose: () => void;
  onConfirm: (params: {
    paymentMethod: string;
    amountUsdPaid: number;
    amountPagoMovilBs: number;
    amountPuntoVentaBs: number;
    referenceCode: string;
  }) => Promise<void>;
}> = ({ title, totalUsd, exchangeRate, onClose, onConfirm }) => {
  const totalBs = totalUsd * exchangeRate;
  const [method, setMethod] = useState<'USD_EFECTIVO' | 'PAGO_MOVIL' | 'PUNTO_VENTA' | 'MIXTO'>('USD_EFECTIVO');
  const [usdText, setUsdText] = useState(totalUsd.toFixed(2));
  const [pmText, setPmText] = useState('0');
  const [pvText, setPvText] = useState('0');
  const [refCode, setRefCode] = useState('');
  const [saving, setSaving] = useState(false);

  const selectMethod = (m: typeof method) => {
    setMethod(m);
    if (m === 'USD_EFECTIVO') {
      setUsdText(totalUsd.toFixed(2));
      setPmText('0');
      setPvText('0');
    } else if (m === 'PAGO_MOVIL') {
      setUsdText('0');
      setPmText(totalBs.toFixed(2));
      setPvText('0');
    } else if (m === 'PUNTO_VENTA') {
      setUsdText('0');
      setPmText('0');
      setPvText(totalBs.toFixed(2));
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>{title}</h3>
          <button className="btn btn-surface btn-sm" onClick={onClose}><X size={16} /></button>
        </div>

        <div className="card" style={{ background: '#25252d', marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 900, fontSize: 18 }}>
            <span>Total a Cobrar:</span>
            <span>${totalUsd.toFixed(2)} USD</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', color: '#ffb300', fontWeight: 800 }}>
            <span>Equivalente en Bolívares:</span>
            <span>{totalBs.toFixed(2)} Bs</span>
          </div>
        </div>

        <div className="label">Método de Pago</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 14 }}>
          {(['USD_EFECTIVO', 'PAGO_MOVIL', 'PUNTO_VENTA', 'MIXTO'] as const).map((m) => (
            <button
              key={m}
              type="button"
              className={`btn btn-sm ${method === m ? 'btn-primary' : 'btn-surface'}`}
              onClick={() => selectMethod(m)}
            >
              {m.replace('_', ' ')}
            </button>
          ))}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 12 }}>
          <div>
            <label className="label">Efectivo ($)</label>
            <input className="input" type="number" step="0.01" value={usdText} onChange={(e) => setUsdText(e.target.value)} />
          </div>
          <div>
            <label className="label">Pago Móvil (Bs)</label>
            <input className="input" type="number" step="0.01" value={pmText} onChange={(e) => setPmText(e.target.value)} />
          </div>
          <div>
            <label className="label">Punto Venta (Bs)</label>
            <input className="input" type="number" step="0.01" value={pvText} onChange={(e) => setPvText(e.target.value)} />
          </div>
        </div>

        <div style={{ marginBottom: 16 }}>
          <label className="label">Referencia / Lote (opcional)</label>
          <input className="input" value={refCode} onChange={(e) => setRefCode(e.target.value)} placeholder="Ej. 482910" />
        </div>

        <button
          className="btn btn-green"
          style={{ width: '100%', height: 46 }}
          disabled={saving}
          onClick={async () => {
            setSaving(true);
            await onConfirm({
              paymentMethod: method,
              amountUsdPaid: parseFloat(usdText) || 0,
              amountPagoMovilBs: parseFloat(pmText) || 0,
              amountPuntoVentaBs: parseFloat(pvText) || 0,
              referenceCode: refCode.trim()
            });
            setSaving(false);
          }}
        >
          ✓ Confirmar Cobro e Imprimir Factura
        </button>
      </div>
    </div>
  );
};

const EditOpenOrderModal: React.FC<{
  order: OrderDoc;
  menuItems: MenuItemDoc[];
  exchangeRate: number;
  onClose: () => void;
  onSave: (updatedItems: CartOrderItem[], newlyAdded: CartOrderItem[], notes: string) => Promise<void>;
}> = ({ order, menuItems, exchangeRate, onClose, onSave }) => {
  const [items, setItems] = useState<CartOrderItem[]>(() => parseCartItems(order.itemsJson));
  const [newlyAdded, setNewlyAdded] = useState<CartOrderItem[]>([]);
  const [notes, setNotes] = useState(order.notes || '');
  const [selectedDishId, setSelectedDishId] = useState(menuItems[0]?.id || '');

  const newTotalUsd = items.reduce((s, i) => s + i.priceUsd * i.quantity, 0);

  const handleAddExtraDish = () => {
    const dish = menuItems.find((m) => m.id === selectedDishId);
    if (!dish) return;
    const item: CartOrderItem = {
      menuItemId: dish.id,
      dishName: dish.name,
      quantity: 1,
      priceUsd: dish.priceUsd,
      excludedIngredientNames: [],
      specialInstructions: 'Adición a cuenta abierta'
    };
    setItems((prev) => [...prev, item]);
    setNewlyAdded((prev) => [...prev, item]);
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>Editar Cuenta Abierta #{order.orderNumber}</h3>
          <button className="btn btn-surface btn-sm" onClick={onClose}><X size={16} /></button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 220, overflowY: 'auto', marginBottom: 12 }}>
          {items.map((it, idx) => (
            <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#25252d', padding: 8, borderRadius: 8 }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 13 }}>{it.quantity}x {it.dishName}</div>
                <div style={{ fontSize: 11.5, color: '#ffb300' }}>${(it.priceUsd * it.quantity).toFixed(2)}</div>
              </div>
              <button
                className="btn btn-sm btn-danger-outline"
                onClick={() => setItems((prev) => prev.filter((_, i) => i !== idx))}
              >
                <Trash2 size={13} />
              </button>
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
          <select className="select" value={selectedDishId} onChange={(e) => setSelectedDishId(e.target.value)}>
            {menuItems.map((m) => (
              <option key={m.id} value={m.id}>{m.name} (${m.priceUsd.toFixed(2)})</option>
            ))}
          </select>
          <button className="btn btn-primary" onClick={handleAddExtraDish}>
            <Plus size={16} /> Añadir
          </button>
        </div>

        <div style={{ marginBottom: 14 }}>
          <label className="label">Observaciones de la cuenta</label>
          <input className="input" value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 900, marginBottom: 14 }}>
          <span>Nuevo Total:</span>
          <span style={{ color: '#ffb300' }}>${newTotalUsd.toFixed(2)} ({(newTotalUsd * exchangeRate).toFixed(2)} Bs)</span>
        </div>

        <button
          className="btn btn-green"
          style={{ width: '100%', height: 46 }}
          onClick={() => onSave(items, newlyAdded, notes)}
        >
          Guardar Cambios y Notificar a Cocina
        </button>
      </div>
    </div>
  );
};
