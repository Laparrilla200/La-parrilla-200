import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import {
  authenticateSystemUser,
  cancelOrder as fsCancelOrder,
  createOrderAndDeductStock,
  deleteMenuItem as fsDeleteMenuItem,
  deleteOrder as fsDeleteOrder,
  deleteSupply as fsDeleteSupply,
  deleteSystemUser as fsDeleteSystemUser,
  markOrderAsPaid as fsMarkOrderAsPaid,
  restockSupply as fsRestockSupply,
  saveBusinessConfig as fsSaveBusinessConfig,
  saveCashClosure as fsSaveCashClosure,
  saveMenuItem as fsSaveMenuItem,
  saveSupply as fsSaveSupply,
  saveSystemUser as fsSaveSystemUser,
  seedInitialDataIfEmpty,
  subscribeClosures,
  subscribeConfig,
  subscribeMenu,
  subscribeOrders,
  subscribeSupplies,
  subscribeSystemUsers,
  updateOpenOrder as fsUpdateOpenOrder,
  updateOrderStatus as fsUpdateOrderStatus
} from '../services/firestoreService';
import {
  DEFAULT_CONFIG,
  isUserAdmin,
  parseCartItems,
  type AppScreen,
  type BusinessConfigDoc,
  type CartOrderItem,
  type ClosureDoc,
  type MenuItemDoc,
  type OrderDoc,
  type PaperWidth,
  type RestockItem,
  type SupplyDoc,
  type SystemUserDoc
} from '../types/models';
import {
  generateClosureReceiptHtml,
  generateKitchenTicketHtml,
  generateOrderReceiptHtml,
  printThermalHtml
} from '../utils/thermalPrinter';

const SESSION_KEY = 'parrilla200_web_session_v1';
const PAPER_WIDTH_KEY = 'parrilla200_paper_width';

function playAlertChime() {
  try {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, ctx.currentTime);
    osc.frequency.setValueAtTime(1174.66, ctx.currentTime + 0.12);
    gain.gain.setValueAtTime(0.12, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.35);
  } catch {
    // Ignore autoplay blocks
  }
}

function showBrowserNotification(title: string, body: string) {
  if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
    try {
      new Notification(title, { body });
    } catch {
      // Ignore if blocked by browser context
    }
  }
}

interface AppContextValue {
  loggedUser: SystemUserDoc | null;
  simulateStaff: boolean;
  setSimulateStaff: (v: boolean) => void;
  currentScreen: AppScreen;
  setCurrentScreen: (s: AppScreen) => void;
  paperWidth: PaperWidth;
  setPaperWidth: (w: PaperWidth) => void;
  supplies: SupplyDoc[];
  menuItems: MenuItemDoc[];
  orders: OrderDoc[];
  unpaidOrders: OrderDoc[];
  paidOrders: OrderDoc[];
  closures: ClosureDoc[];
  config: BusinessConfigDoc;
  systemUsers: SystemUserDoc[];
  criticalSupplies: SupplyDoc[];
  restockRecommendations: RestockItem[];
  cart: CartOrderItem[];
  customerName: string;
  setCustomerName: (v: string) => void;
  tableOrDestination: string;
  setTableOrDestination: (v: string) => void;
  toast: string | null;
  showToast: (msg: string) => void;
  ticketModal: { title: string; html: string } | null;
  closeTicketModal: () => void;
  openTicketModal: (title: string, html: string) => void;
  login: (username: string, password: string) => Promise<{ ok: boolean; error?: string }>;
  logout: () => void;
  addToCart: (item: CartOrderItem) => void;
  updateCartQuantity: (index: number, qty: number) => void;
  clearCart: () => void;
  submitFastOrderToKitchen: (notes?: string) => Promise<void>;
  submitOrderAndCheckout: (params: {
    paymentMethod: string;
    amountUsdPaid: number;
    amountPagoMovilBs: number;
    amountPuntoVentaBs: number;
    referenceCode: string;
    isPaid: boolean;
    notes?: string;
  }) => Promise<void>;
  markOrderPaid: (
    order: OrderDoc,
    paymentMethod: string,
    usdPaid: number,
    pagoMovilBs: number,
    puntoVentaBs: number,
    refCode: string
  ) => Promise<void>;
  quickPayOrderCash: (order: OrderDoc) => Promise<void>;
  editOpenOrder: (
    orderId: string,
    updatedItems: CartOrderItem[],
    newlyAddedItems: CartOrderItem[],
    notes?: string
  ) => Promise<void>;
  updateOrderStatus: (orderId: string, status: string) => Promise<void>;
  cancelOrder: (orderId: string, restoreStock?: boolean) => Promise<void>;
  deleteOrder: (orderId: string) => Promise<void>;
  reprintOrderReceipt: (order: OrderDoc) => void;
  printKitchenComanda: (order: OrderDoc) => void;
  restockSupply: (supplyId: string, amount: number) => Promise<void>;
  saveSupply: (supply: SupplyDoc) => Promise<void>;
  deleteSupply: (supplyId: string) => Promise<void>;
  saveMenuItem: (item: MenuItemDoc) => Promise<void>;
  deleteMenuItem: (itemId: string) => Promise<void>;
  generateClosure: (closureType: 'DIARIO' | 'SEMANAL' | 'MENSUAL', notes?: string) => Promise<void>;
  reprintClosure: (closure: ClosureDoc) => void;
  saveConfig: (cfg: BusinessConfigDoc) => Promise<void>;
  saveUser: (user: SystemUserDoc, previousUsername?: string | null) => Promise<void>;
  deleteUser: (username: string) => Promise<void>;
}

const AppContext = createContext<AppContextValue | null>(null);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [loggedUser, setLoggedUser] = useState<SystemUserDoc | null>(() => {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      return raw ? (JSON.parse(raw) as SystemUserDoc) : null;
    } catch {
      return null;
    }
  });
  const [simulateStaff, setSimulateStaff] = useState(false);
  const [currentScreen, setCurrentScreen] = useState<AppScreen>('POS');
  const [paperWidth, setPaperWidthState] = useState<PaperWidth>(() => {
    const saved = localStorage.getItem(PAPER_WIDTH_KEY);
    return saved === '80' ? 80 : 58;
  });

  const [supplies, setSupplies] = useState<SupplyDoc[]>([]);
  const [menuItems, setMenuItems] = useState<MenuItemDoc[]>([]);
  const [orders, setOrders] = useState<OrderDoc[]>([]);
  const [closures, setClosures] = useState<ClosureDoc[]>([]);
  const [config, setConfig] = useState<BusinessConfigDoc>(DEFAULT_CONFIG);
  const [systemUsers, setSystemUsers] = useState<SystemUserDoc[]>([]);

  const [cart, setCart] = useState<CartOrderItem[]>([]);
  const [customerName, setCustomerName] = useState('Cliente');
  const [tableOrDestination, setTableOrDestination] = useState('Mesa 1');
  const [toast, setToast] = useState<string | null>(null);
  const [ticketModal, setTicketModal] = useState<{ title: string; html: string } | null>(null);

  const prevOrderStatusesRef = useRef<Record<string, string>>({});
  const notifiedCriticalIdsRef = useRef<Set<string>>(new Set());

  const showToast = useCallback((msg: string) => {
    setToast(msg);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(t);
  }, [toast]);

  const setPaperWidth = useCallback((w: PaperWidth) => {
    setPaperWidthState(w);
    localStorage.setItem(PAPER_WIDTH_KEY, String(w));
  }, []);

  // Firestore real-time subscriptions
  useEffect(() => {
    seedInitialDataIfEmpty().catch((err) => console.warn('Aviso seed inicial:', err));
    const unsubs = [
      subscribeSupplies(setSupplies),
      subscribeMenu(setMenuItems),
      subscribeOrders(setOrders),
      subscribeClosures(setClosures),
      subscribeConfig(setConfig),
      subscribeSystemUsers(setSystemUsers)
    ];
    return () => unsubs.forEach((u) => u());
  }, []);

  // Keep logged user synced with live systemUsers updates (permissions/role)
  useEffect(() => {
    if (!loggedUser || systemUsers.length === 0) return;
    const latest = systemUsers.find(
      (u) => u.username.toLowerCase() === loggedUser.username.toLowerCase()
    );
    if (latest && JSON.stringify(latest) !== JSON.stringify(loggedUser)) {
      setLoggedUser(latest);
      localStorage.setItem(SESSION_KEY, JSON.stringify(latest));
    }
  }, [systemUsers, loggedUser]);

  const criticalSupplies = useMemo(
    () => supplies.filter((s) => s.currentStock <= s.minStockThreshold),
    [supplies]
  );

  const restockRecommendations = useMemo<RestockItem[]>(
    () =>
      criticalSupplies.map((supply) => {
        const shortageAmount = Math.max(0, supply.minStockThreshold - supply.currentStock);
        const toBuyAmount =
          supply.recommendedRestock > 0 ? supply.recommendedRestock : shortageAmount + 5;
        const estimatedCostUsd = toBuyAmount * supply.costUsd;
        const estimatedCostBs = estimatedCostUsd * config.exchangeRateUsdToBs;
        return { supply, shortageAmount, toBuyAmount, estimatedCostUsd, estimatedCostBs };
      }),
    [criticalSupplies, config.exchangeRateUsdToBs]
  );

  // Admin-only low stock notifications
  useEffect(() => {
    if (!loggedUser || !isUserAdmin(loggedUser)) return;
    for (const sup of criticalSupplies) {
      if (!notifiedCriticalIdsRef.current.has(sup.id)) {
        notifiedCriticalIdsRef.current.add(sup.id);
        showBrowserNotification(
          '⚠️ Stock Crítico — La Parrilla 200',
          `${sup.name}: queda ${sup.currentStock.toFixed(1)} ${sup.unit} (mín. ${sup.minStockThreshold})`
        );
      }
    }
  }, [criticalSupplies, loggedUser]);

  // Kitchen READY -> Notification for Cashier / Waiters / Admin
  useEffect(() => {
    const prevMap = prevOrderStatusesRef.current;
    const nextMap: Record<string, string> = {};
    const hasPrev = Object.keys(prevMap).length > 0;

    for (const order of orders) {
      nextMap[order.id] = order.status;
      if (hasPrev && order.status === 'LISTO' && prevMap[order.id] && prevMap[order.id] !== 'LISTO') {
        if (loggedUser) {
          playAlertChime();
          const msg = `🔔 ¡Pedido #${order.orderNumber} (${order.tableOrDestination}) LISTO para servir!`;
          showToast(msg);
          showBrowserNotification('¡Comanda Lista en Cocina!', msg);
        }
      }
    }
    prevOrderStatusesRef.current = nextMap;
  }, [orders, loggedUser, showToast]);

  const unpaidOrders = useMemo(
    () => orders.filter((o) => !o.isPaid && o.status !== 'CANCELADO'),
    [orders]
  );

  const paidOrders = useMemo(
    () => orders.filter((o) => o.isPaid && o.status !== 'CANCELADO'),
    [orders]
  );

  const login = useCallback(async (username: string, password: string) => {
    const user = await authenticateSystemUser(username, password);
    if (!user) {
      return { ok: false, error: 'Usuario o contraseña incorrectos.' };
    }
    if (!isUserAdmin(user) && user.canAccessPc === false) {
      return { ok: false, error: 'Este usuario no tiene permiso para acceder desde Computadora / Web.' };
    }
    setLoggedUser(user);
    setSimulateStaff(false);
    localStorage.setItem(SESSION_KEY, JSON.stringify(user));
    if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
      Notification.requestPermission().catch(() => {});
    }
    showToast(`Bienvenido, ${user.displayName}`);
    return { ok: true };
  }, [showToast]);

  const logout = useCallback(() => {
    localStorage.removeItem(SESSION_KEY);
    setLoggedUser(null);
    setSimulateStaff(false);
    setCart([]);
  }, []);

  const addToCart = useCallback(
    (item: CartOrderItem) => {
      setCart((prev) => {
        const idx = prev.findIndex(
          (c) =>
            c.menuItemId === item.menuItemId &&
            c.dishName === item.dishName &&
            c.priceUsd === item.priceUsd &&
            JSON.stringify(c.excludedIngredientNames || []) === JSON.stringify(item.excludedIngredientNames || []) &&
            (c.specialInstructions || '').toLowerCase() === (item.specialInstructions || '').toLowerCase()
        );
        if (idx >= 0) {
          const copy = [...prev];
          copy[idx] = { ...copy[idx], quantity: copy[idx].quantity + item.quantity };
          return copy;
        }
        return [...prev, item];
      });
      showToast(`Agregado: ${item.dishName}`);
    },
    [showToast]
  );

  const updateCartQuantity = useCallback((index: number, qty: number) => {
    setCart((prev) => {
      if (index < 0 || index >= prev.length) return prev;
      if (qty <= 0) return prev.filter((_, i) => i !== index);
      const copy = [...prev];
      copy[index] = { ...copy[index], quantity: qty };
      return copy;
    });
  }, []);

  const clearCart = useCallback(() => setCart([]), []);

  const submitFastOrderToKitchen = useCallback(
    async (notes = '') => {
      if (cart.length === 0) {
        showToast('Agrega al menos un plato a la comanda');
        return;
      }
      const nextOrderNumber = orders.reduce((max, o) => Math.max(max, o.orderNumber || 0), 0) + 1;
      const rate = config.exchangeRateUsdToBs || 65;
      const totalUsd = cart.reduce((s, i) => s + i.priceUsd * i.quantity, 0);
      const totalBs = totalUsd * rate;

      const saved = await createOrderAndDeductStock(
        {
          orderNumber: nextOrderNumber,
          customerName: customerName.trim() || 'Cliente',
          tableOrDestination: tableOrDestination.trim() || 'Mesa 1',
          timestamp: Date.now(),
          status: 'EN_COCINA',
          totalUsd,
          exchangeRate: rate,
          totalBs,
          paymentMethod: 'PENDIENTE',
          amountUsdPaid: 0,
          amountPagoMovilBs: 0,
          amountPuntoVentaBs: 0,
          referenceCode: '',
          notes,
          isPaid: false,
          createdBy: loggedUser?.displayName || 'Web POS'
        },
        cart,
        menuItems
      );
      const itemsSnapshot = [...cart];
      setCart([]);
      setCustomerName('Cliente');
      const html = generateKitchenTicketHtml(saved, itemsSnapshot, paperWidth);
      printThermalHtml(html);
      setTicketModal({ title: `Comanda Cocina #${saved.orderNumber}`, html });
      showToast(`⚡ ¡Comanda #${saved.orderNumber} enviada a cocina!`);
    },
    [cart, orders, config.exchangeRateUsdToBs, customerName, tableOrDestination, loggedUser, menuItems, paperWidth, showToast]
  );

  const submitOrderAndCheckout = useCallback(
    async (params: {
      paymentMethod: string;
      amountUsdPaid: number;
      amountPagoMovilBs: number;
      amountPuntoVentaBs: number;
      referenceCode: string;
      isPaid: boolean;
      notes?: string;
    }) => {
      if (cart.length === 0) {
        showToast('El carrito está vacío');
        return;
      }
      const nextOrderNumber = orders.reduce((max, o) => Math.max(max, o.orderNumber || 0), 0) + 1;
      const rate = config.exchangeRateUsdToBs || 65;
      const totalUsd = cart.reduce((s, i) => s + i.priceUsd * i.quantity, 0);
      const totalBs = totalUsd * rate;

      const saved = await createOrderAndDeductStock(
        {
          orderNumber: nextOrderNumber,
          customerName: customerName.trim() || 'Cliente',
          tableOrDestination: tableOrDestination.trim() || 'Mesa 1',
          timestamp: Date.now(),
          status: 'EN_COCINA',
          totalUsd,
          exchangeRate: rate,
          totalBs,
          paymentMethod: params.paymentMethod,
          amountUsdPaid: params.amountUsdPaid,
          amountPagoMovilBs: params.amountPagoMovilBs,
          amountPuntoVentaBs: params.amountPuntoVentaBs,
          referenceCode: params.referenceCode,
          notes: params.notes || '',
          isPaid: params.isPaid,
          createdBy: loggedUser?.displayName || 'Web POS'
        },
        cart,
        menuItems
      );
      const itemsSnapshot = [...cart];
      setCart([]);
      setCustomerName('Cliente');
      const html = generateOrderReceiptHtml(saved, itemsSnapshot, config, paperWidth);
      printThermalHtml(html);
      setTicketModal({ title: `Factura #${saved.orderNumber}`, html });
      showToast(`✅ ¡Pedido #${saved.orderNumber} enviado a cocina y registrado!`);
    },
    [cart, orders, config, customerName, tableOrDestination, loggedUser, menuItems, paperWidth, showToast]
  );

  const markOrderPaid = useCallback(
    async (
      order: OrderDoc,
      paymentMethod: string,
      usdPaid: number,
      pagoMovilBs: number,
      puntoVentaBs: number,
      refCode: string
    ) => {
      if (order.isPaid) {
        showToast(`⚠️ La orden #${order.orderNumber} ya fue pagada.`);
        return;
      }
      const updatedOrder: OrderDoc = {
        ...order,
        isPaid: true,
        paymentMethod,
        amountUsdPaid: usdPaid,
        amountPagoMovilBs: pagoMovilBs,
        amountPuntoVentaBs: puntoVentaBs,
        referenceCode: refCode
      };
      // Actualización optimista instantánea al primer toque
      setOrders((prev) => prev.map((o) => (o.id === order.id ? updatedOrder : o)));

      await fsMarkOrderAsPaid(order.id, paymentMethod, usdPaid, pagoMovilBs, puntoVentaBs, refCode);
      const items = parseCartItems(order.itemsJson);
      const html = generateOrderReceiptHtml(updatedOrder, items, config, paperWidth);
      printThermalHtml(html);
      setTicketModal({ title: `Factura Cliente #${order.orderNumber}`, html });
      showToast(`💳 ¡Orden #${order.orderNumber} marcada como PAGADA!`);
    },
    [config, paperWidth, showToast]
  );

  const quickPayOrderCash = useCallback(
    async (order: OrderDoc) => {
      await markOrderPaid(order, 'USD_EFECTIVO', order.totalUsd, 0, 0, 'EFECTIVO');
    },
    [markOrderPaid]
  );

  const editOpenOrder = useCallback(
    async (
      orderId: string,
      updatedItems: CartOrderItem[],
      newlyAddedItems: CartOrderItem[],
      notes?: string
    ) => {
      const rate = config.exchangeRateUsdToBs || 65;
      const newTotalUsd = updatedItems.reduce((s, i) => s + i.priceUsd * i.quantity, 0);
      const newTotalBs = newTotalUsd * rate;
      await fsUpdateOpenOrder(orderId, updatedItems, newTotalUsd, newTotalBs, newlyAddedItems, menuItems, notes);
      showToast('¡Cuenta abierta actualizada y enviada a cocina!');
    },
    [config.exchangeRateUsdToBs, menuItems, showToast]
  );

  const updateOrderStatus = useCallback(
    async (orderId: string, status: string) => {
      setOrders((prev) => prev.map((o) => (o.id === orderId ? { ...o, status } : o)));
      await fsUpdateOrderStatus(orderId, status);
      showToast(`Estado actualizado a ${status}`);
    },
    [showToast]
  );

  const cancelOrder = useCallback(
    async (orderId: string, restoreStock = true) => {
      setOrders((prev) => prev.map((o) => (o.id === orderId ? { ...o, status: 'CANCELADO' } : o)));
      await fsCancelOrder(orderId, restoreStock);
      showToast('Pedido cancelado e insumos restaurados al inventario');
    },
    [showToast]
  );

  const deleteOrder = useCallback(
    async (orderId: string) => {
      setOrders((prev) => prev.filter((o) => o.id !== orderId));
      await fsDeleteOrder(orderId);
      showToast('Cuenta eliminada definitivamente');
    },
    [showToast]
  );

  const reprintOrderReceipt = useCallback(
    (order: OrderDoc) => {
      const items = parseCartItems(order.itemsJson);
      const html = generateOrderReceiptHtml(order, items, config, paperWidth);
      printThermalHtml(html);
      setTicketModal({ title: `Copia Factura #${order.orderNumber}`, html });
    },
    [config, paperWidth]
  );

  const printKitchenComanda = useCallback(
    (order: OrderDoc) => {
      const items = parseCartItems(order.itemsJson);
      const html = generateKitchenTicketHtml(order, items, paperWidth);
      printThermalHtml(html);
      setTicketModal({ title: `Comanda Cocina #${order.orderNumber}`, html });
    },
    [paperWidth]
  );

  const restockSupply = useCallback(
    async (supplyId: string, amount: number) => {
      await fsRestockSupply(supplyId, amount);
      showToast('Stock actualizado');
    },
    [showToast]
  );

  const saveSupply = useCallback(
    async (supply: SupplyDoc) => {
      await fsSaveSupply(supply);
      showToast('Insumo guardado');
    },
    [showToast]
  );

  const deleteSupply = useCallback(
    async (supplyId: string) => {
      await fsDeleteSupply(supplyId);
      showToast('Insumo eliminado');
    },
    [showToast]
  );

  const saveMenuItem = useCallback(
    async (item: MenuItemDoc) => {
      await fsSaveMenuItem(item);
      showToast('Plato guardado en el menú');
    },
    [showToast]
  );

  const deleteMenuItem = useCallback(
    async (itemId: string) => {
      await fsDeleteMenuItem(itemId);
      showToast('Plato eliminado del menú');
    },
    [showToast]
  );

  const generateClosure = useCallback(
    async (closureType: 'DIARIO' | 'SEMANAL' | 'MENSUAL', notes = '') => {
      const now = Date.now();
      const start = new Date(now);
      if (closureType === 'SEMANAL') {
        start.setDate(start.getDate() - 7);
      } else if (closureType === 'MENSUAL') {
        start.setDate(start.getDate() - 30);
      } else {
        start.setHours(0, 0, 0, 0);
      }
      const startDate = start.getTime();
      const relevant = orders.filter(
        (o) => o.timestamp >= startDate && o.timestamp <= now && o.status !== 'CANCELADO'
      );
      const nextNum = closures.reduce((m, c) => Math.max(m, c.closureNumber || 0), 0) + 1;
      const saved = await fsSaveCashClosure({
        closureType,
        closureNumber: nextNum,
        timestamp: now,
        startDate,
        endDate: now,
        totalSalesUsd: relevant.reduce((s, o) => s + (o.totalUsd || 0), 0),
        totalSalesBs: relevant.reduce((s, o) => s + (o.totalBs || 0), 0),
        totalUsdCash: relevant.reduce((s, o) => s + (o.amountUsdPaid || 0), 0),
        totalPagoMovilBs: relevant.reduce((s, o) => s + (o.amountPagoMovilBs || 0), 0),
        totalPuntoVentaBs: relevant.reduce((s, o) => s + (o.amountPuntoVentaBs || 0), 0),
        orderCount: relevant.length,
        closedBy: loggedUser?.displayName || 'Administrador',
        notes
      });
      const html = generateClosureReceiptHtml(saved, config, paperWidth);
      printThermalHtml(html);
      setTicketModal({ title: `Cierre ${closureType} #${saved.closureNumber}`, html });
      showToast(`¡Cierre ${closureType} #${saved.closureNumber} generado!`);
    },
    [orders, closures, loggedUser, config, paperWidth, showToast]
  );

  const reprintClosure = useCallback(
    (closure: ClosureDoc) => {
      const html = generateClosureReceiptHtml(closure, config, paperWidth);
      printThermalHtml(html);
      setTicketModal({ title: `Cierre ${closure.closureType} #${closure.closureNumber}`, html });
    },
    [config, paperWidth]
  );

  const saveConfig = useCallback(
    async (cfg: BusinessConfigDoc) => {
      await fsSaveBusinessConfig(cfg);
      showToast('Configuración guardada');
    },
    [showToast]
  );

  const saveUser = useCallback(
    async (user: SystemUserDoc, previousUsername?: string | null) => {
      await fsSaveSystemUser(user, previousUsername);
      showToast(`Usuario '${user.username}' guardado`);
    },
    [showToast]
  );

  const deleteUser = useCallback(
    async (username: string) => {
      await fsDeleteSystemUser(username);
      showToast(`Usuario '${username}' eliminado`);
    },
    [showToast]
  );

  return (
    <AppContext.Provider
      value={{
        loggedUser,
        simulateStaff,
        setSimulateStaff,
        currentScreen,
        setCurrentScreen,
        paperWidth,
        setPaperWidth,
        supplies,
        menuItems,
        orders,
        unpaidOrders,
        paidOrders,
        closures,
        config,
        systemUsers,
        criticalSupplies,
        restockRecommendations,
        cart,
        customerName,
        setCustomerName,
        tableOrDestination,
        setTableOrDestination,
        toast,
        showToast,
        ticketModal,
        closeTicketModal: () => setTicketModal(null),
        openTicketModal: (title, html) => setTicketModal({ title, html }),
        login,
        logout,
        addToCart,
        updateCartQuantity,
        clearCart,
        submitFastOrderToKitchen,
        submitOrderAndCheckout,
        markOrderPaid,
        quickPayOrderCash,
        editOpenOrder,
        updateOrderStatus,
        cancelOrder,
        deleteOrder,
        reprintOrderReceipt,
        printKitchenComanda,
        restockSupply,
        saveSupply,
        deleteSupply,
        saveMenuItem,
        deleteMenuItem,
        generateClosure,
        reprintClosure,
        saveConfig,
        saveUser,
        deleteUser
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used inside AppProvider');
  return ctx;
}
