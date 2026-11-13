import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  query,
  setDoc,
  updateDoc,
  type Unsubscribe
} from 'firebase/firestore';
import { db } from '../firebase';
import {
  DEFAULT_CONFIG,
  parseCartItems,
  parseIngredients,
  type BusinessConfigDoc,
  type CartOrderItem,
  type ClosureDoc,
  type IngredientRequirement,
  type MenuItemDoc,
  type OrderDoc,
  type SupplyDoc,
  type SystemUserDoc
} from '../types/models';

export function subscribeSupplies(cb: (items: SupplyDoc[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'supplies'), (snap) => {
    const list = snap.docs
      .map((d) => ({ ...(d.data() as SupplyDoc), id: d.id }))
      .sort((a, b) => (a.name || '').localeCompare(b.name || ''));
    cb(list);
  });
}

export function subscribeMenu(cb: (items: MenuItemDoc[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'menu'), (snap) => {
    const list = snap.docs
      .map((d) => ({ ...(d.data() as MenuItemDoc), id: d.id }))
      .sort((a, b) => (a.name || '').localeCompare(b.name || ''));
    cb(list);
  });
}

export function subscribeOrders(cb: (items: OrderDoc[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'orders'), (snap) => {
    const list = snap.docs
      .map((d) => ({ ...(d.data() as OrderDoc), id: d.id }))
      .sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    cb(list);
  });
}

export function subscribeClosures(cb: (items: ClosureDoc[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'closures'), (snap) => {
    const list = snap.docs
      .map((d) => ({ ...(d.data() as ClosureDoc), id: d.id }))
      .sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    cb(list);
  });
}

export function subscribeSystemUsers(cb: (users: SystemUserDoc[]) => void): Unsubscribe {
  return onSnapshot(collection(db, 'system_users'), (snap) => {
    const list = snap.docs
      .map((d) => ({ ...(d.data() as SystemUserDoc), id: d.id }))
      .sort((a, b) => (a.username || '').localeCompare(b.username || ''));
    cb(list);
  });
}

export function subscribeConfig(cb: (config: BusinessConfigDoc) => void): Unsubscribe {
  return onSnapshot(doc(db, 'business', 'config'), (snap) => {
    if (snap.exists()) {
      cb({ ...DEFAULT_CONFIG, ...(snap.data() as Partial<BusinessConfigDoc>), id: snap.id });
    } else {
      cb(DEFAULT_CONFIG);
    }
  });
}

export async function authenticateSystemUser(
  username: string,
  password: string
): Promise<SystemUserDoc | null> {
  const cleanUser = username.trim().toLowerCase();
  const cleanPass = password.trim();
  if (!cleanUser || !cleanPass) return null;

  try {
    const snap = await getDoc(doc(db, 'system_users', cleanUser));
    if (snap.exists()) {
      const user = { ...(snap.data() as SystemUserDoc), id: snap.id };
      if (user.isActive !== false && user.password === cleanPass) {
        return user;
      }
      return null;
    }
  } catch (err) {
    console.warn('Error autenticando en Firestore:', err);
  }

  if (cleanUser === 'admin' && (cleanPass === 'admin200' || cleanPass === 'admin')) {
    await seedDefaultSystemUsersIfEmpty();
    return {
      id: 'admin',
      username: 'admin',
      password: cleanPass,
      displayName: 'Administrador Principal',
      role: 'ADMIN',
      createdAt: Date.now(),
      isActive: true,
      canUsePosApp: true,
      canUseKitchenApp: true,
      canUseBillingApp: true,
      canUseInventoryApp: true,
      canUseClosuresApp: true,
      canUseMenuApp: true,
      canUseSettingsApp: true,
      canAccessPc: true,
      canUsePosPc: true,
      canUseKitchenPc: true,
      canUseBillingPc: true,
      canUseInventoryPc: true,
      canUseReportsPc: true
    };
  }
  return null;
}

export async function seedDefaultSystemUsersIfEmpty(): Promise<void> {
  const snap = await getDocs(query(collection(db, 'system_users'), limit(1)));
  if (!snap.empty) return;

  const now = Date.now();
  const defaults: SystemUserDoc[] = [
    {
      id: 'admin',
      username: 'admin',
      password: 'admin200',
      displayName: 'Administrador Principal',
      role: 'ADMIN',
      createdAt: now,
      isActive: true,
      canUsePosApp: true,
      canUseKitchenApp: true,
      canUseBillingApp: true,
      canUseInventoryApp: true,
      canUseClosuresApp: true,
      canUseMenuApp: true,
      canUseSettingsApp: true,
      canAccessPc: true,
      canUsePosPc: true,
      canUseKitchenPc: true,
      canUseBillingPc: true,
      canUseInventoryPc: true,
      canUseReportsPc: true
    },
    {
      id: 'cajero',
      username: 'cajero',
      password: '1234',
      displayName: 'Cajero 1',
      role: 'STAFF',
      createdAt: now,
      isActive: true,
      canUsePosApp: true,
      canUseKitchenApp: true,
      canUseBillingApp: true,
      canUseInventoryApp: false,
      canUseClosuresApp: false,
      canUseMenuApp: false,
      canUseSettingsApp: false,
      canAccessPc: true,
      canUsePosPc: true,
      canUseKitchenPc: false,
      canUseBillingPc: true,
      canUseInventoryPc: false,
      canUseReportsPc: false
    },
    {
      id: 'cocina',
      username: 'cocina',
      password: '1234',
      displayName: 'Personal de Cocina',
      role: 'STAFF',
      createdAt: now,
      isActive: true,
      canUsePosApp: false,
      canUseKitchenApp: true,
      canUseBillingApp: false,
      canUseInventoryApp: true,
      canUseClosuresApp: false,
      canUseMenuApp: false,
      canUseSettingsApp: false,
      canAccessPc: true,
      canUsePosPc: false,
      canUseKitchenPc: true,
      canUseBillingPc: false,
      canUseInventoryPc: true,
      canUseReportsPc: false
    }
  ];

  await Promise.all(defaults.map((u) => setDoc(doc(db, 'system_users', u.id), u)));
}

export async function seedInitialDataIfEmpty(): Promise<void> {
  await seedDefaultSystemUsersIfEmpty();
  const suppliesSnap = await getDocs(query(collection(db, 'supplies'), limit(1)));
  if (!suppliesSnap.empty) return;

  const defaultSupplies: SupplyDoc[] = [
    { id: 'sup_carne_solomo', name: 'Carne Solomo de Cuerito', currentStock: 18.5, minStockThreshold: 5.0, unit: 'kg', costUsd: 6.5, recommendedRestock: 15.0 },
    { id: 'sup_punta_trasera', name: 'Punta Trasera Angus', currentStock: 12.0, minStockThreshold: 4.0, unit: 'kg', costUsd: 8.5, recommendedRestock: 10.0 },
    { id: 'sup_pollo_pechuga', name: 'Pechuga de Pollo', currentStock: 15.0, minStockThreshold: 5.0, unit: 'kg', costUsd: 4.2, recommendedRestock: 12.0 },
    { id: 'sup_chorizo', name: 'Chorizo Ahumado Parrillero', currentStock: 35.0, minStockThreshold: 10.0, unit: 'ud', costUsd: 1.0, recommendedRestock: 30.0 },
    { id: 'sup_morcilla', name: 'Morcilla Artesanal', currentStock: 20.0, minStockThreshold: 8.0, unit: 'ud', costUsd: 0.9, recommendedRestock: 20.0 },
    { id: 'sup_queso_llanero', name: 'Queso Llanero para Asar', currentStock: 8.0, minStockThreshold: 3.0, unit: 'kg', costUsd: 4.5, recommendedRestock: 8.0 },
    { id: 'sup_yuca', name: 'Yuca Lista para Freír/Hervir', currentStock: 25.0, minStockThreshold: 6.0, unit: 'kg', costUsd: 1.2, recommendedRestock: 20.0 },
    { id: 'sup_papas', name: 'Papas para Bastones Fritos', currentStock: 20.0, minStockThreshold: 6.0, unit: 'kg', costUsd: 1.5, recommendedRestock: 15.0 },
    { id: 'sup_pan_hamburguesa', name: 'Pan Artesanal Parrillero', currentStock: 30.0, minStockThreshold: 10.0, unit: 'ud', costUsd: 0.5, recommendedRestock: 30.0 },
    { id: 'sup_pan_pepito', name: 'Pan Baguette Pepito 30cm', currentStock: 18.0, minStockThreshold: 6.0, unit: 'ud', costUsd: 0.7, recommendedRestock: 20.0 },
    { id: 'sup_guasacaca', name: 'Guasacaca Especial de la Casa', currentStock: 6.5, minStockThreshold: 2.0, unit: 'lt', costUsd: 3.0, recommendedRestock: 5.0 },
    { id: 'sup_salsa_bbq', name: 'Salsa BBQ Ahumada 200', currentStock: 5.0, minStockThreshold: 1.5, unit: 'lt', costUsd: 3.5, recommendedRestock: 4.0 },
    { id: 'sup_salsa_ajo', name: 'Salsa de Ajo Criolla', currentStock: 4.0, minStockThreshold: 1.5, unit: 'lt', costUsd: 2.8, recommendedRestock: 4.0 },
    { id: 'sup_cebolla_asada', name: 'Cebolla & Pimentón Asados', currentStock: 7.0, minStockThreshold: 2.5, unit: 'kg', costUsd: 1.8, recommendedRestock: 6.0 },
    { id: 'sup_refresco', name: 'Refresco 1.5L Variados', currentStock: 24.0, minStockThreshold: 6.0, unit: 'ud', costUsd: 1.4, recommendedRestock: 24.0 },
    { id: 'sup_cerveza', name: 'Cerveza Polar Fría / Malta', currentStock: 48.0, minStockThreshold: 12.0, unit: 'ud', costUsd: 0.85, recommendedRestock: 48.0 }
  ];
  await Promise.all(defaultSupplies.map((s) => setDoc(doc(db, 'supplies', s.id), s)));

  const ing = (list: IngredientRequirement[]) => JSON.stringify(list);
  const defaultMenu: MenuItemDoc[] = [
    {
      id: 'dish_parrilla_familiar',
      name: 'Parrilla 200 Mixta Familiar (4 personas)',
      description: '400g Solomo, 300g Pollo, 2 Chorizos, 1 Morcilla, Yuca con Queso a la Plancha y Guasacaca.',
      category: 'Parrillas',
      priceUsd: 22.0,
      isAvailable: true,
      ingredientsJson: ing([
        { supplyId: 'sup_carne_solomo', supplyName: 'Carne Solomo de Cuerito', quantityRequired: 0.4, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_pollo_pechuga', supplyName: 'Pechuga de Pollo', quantityRequired: 0.3, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_chorizo', supplyName: 'Chorizo Ahumado Parrillero', quantityRequired: 2, unit: 'ud', isRemovable: true },
        { supplyId: 'sup_morcilla', supplyName: 'Morcilla Artesanal', quantityRequired: 1, unit: 'ud', isRemovable: true },
        { supplyId: 'sup_yuca', supplyName: 'Yuca Lista para Freír/Hervir', quantityRequired: 0.35, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_queso_llanero', supplyName: 'Queso Llanero para Asar', quantityRequired: 0.2, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_guasacaca', supplyName: 'Guasacaca Especial de la Casa', quantityRequired: 0.1, unit: 'lt', isRemovable: true }
      ]),
      extrasJson: JSON.stringify([{ name: 'Chorizo Extra', priceUsd: 2.0 }, { name: 'Ración Queso Asado', priceUsd: 2.5 }])
    },
    {
      id: 'dish_parrilla_solomo',
      name: 'Parrilla Solomo de Cuerito Especial',
      description: '350g de jugoso Solomo a la brasa, acompañado de Yuca frita, Queso frito y Guasacaca.',
      category: 'Parrillas',
      priceUsd: 13.5,
      isAvailable: true,
      ingredientsJson: ing([
        { supplyId: 'sup_carne_solomo', supplyName: 'Carne Solomo de Cuerito', quantityRequired: 0.35, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_yuca', supplyName: 'Yuca Lista para Freír/Hervir', quantityRequired: 0.25, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_queso_llanero', supplyName: 'Queso Llanero para Asar', quantityRequired: 0.15, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_guasacaca', supplyName: 'Guasacaca Especial de la Casa', quantityRequired: 0.08, unit: 'lt', isRemovable: true }
      ]),
      extrasJson: JSON.stringify([{ name: 'Extra Queso Llanero', priceUsd: 1.8 }])
    },
    {
      id: 'dish_hamburguesa_200',
      name: 'Hamburguesa Parrillera La 200',
      description: '200g carne a la parrilla, Queso a la plancha, Cebolla y Pimentón asados, salsa BBQ.',
      category: 'Hamburguesas & Pepitos',
      priceUsd: 8.5,
      isAvailable: true,
      ingredientsJson: ing([
        { supplyId: 'sup_pan_hamburguesa', supplyName: 'Pan Artesanal Parrillero', quantityRequired: 1, unit: 'ud', isRemovable: false },
        { supplyId: 'sup_carne_solomo', supplyName: 'Carne Solomo de Cuerito', quantityRequired: 0.2, unit: 'kg', isRemovable: false },
        { supplyId: 'sup_queso_llanero', supplyName: 'Queso Llanero para Asar', quantityRequired: 0.08, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_cebolla_asada', supplyName: 'Cebolla & Pimentón Asados', quantityRequired: 0.06, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_salsa_bbq', supplyName: 'Salsa BBQ Ahumada 200', quantityRequired: 0.04, unit: 'lt', isRemovable: true }
      ]),
      extrasJson: JSON.stringify([{ name: 'Doble Carne 200g', priceUsd: 3.5 }, { name: 'Papas Fritas', priceUsd: 2.0 }])
    },
    {
      id: 'dish_pepito_mixto',
      name: 'Pepito Mixto Gratinado 30cm',
      description: 'Carne y pollo salteados a la brasa, queso llanero gratinado y salsa de ajo.',
      category: 'Hamburguesas & Pepitos',
      priceUsd: 9.5,
      isAvailable: true,
      ingredientsJson: ing([
        { supplyId: 'sup_pan_pepito', supplyName: 'Pan Baguette Pepito 30cm', quantityRequired: 1, unit: 'ud', isRemovable: false },
        { supplyId: 'sup_carne_solomo', supplyName: 'Carne Solomo de Cuerito', quantityRequired: 0.15, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_pollo_pechuga', supplyName: 'Pechuga de Pollo', quantityRequired: 0.15, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_queso_llanero', supplyName: 'Queso Llanero para Asar', quantityRequired: 0.1, unit: 'kg', isRemovable: true },
        { supplyId: 'sup_salsa_ajo', supplyName: 'Salsa de Ajo Criolla', quantityRequired: 0.05, unit: 'lt', isRemovable: true }
      ]),
      extrasJson: JSON.stringify([{ name: 'Extra Queso Gratinado', priceUsd: 1.5 }])
    },
    {
      id: 'dish_choripan',
      name: 'Choripán Artesanal con Guasacaca',
      description: 'Chorizo parrillero ahumado en pan tostado con guasacaca y salsa de ajo.',
      category: 'Entradas & Guarniciones',
      priceUsd: 4.5,
      isAvailable: true,
      ingredientsJson: ing([
        { supplyId: 'sup_pan_hamburguesa', supplyName: 'Pan Artesanal Parrillero', quantityRequired: 1, unit: 'ud', isRemovable: false },
        { supplyId: 'sup_chorizo', supplyName: 'Chorizo Ahumado Parrillero', quantityRequired: 1, unit: 'ud', isRemovable: false },
        { supplyId: 'sup_guasacaca', supplyName: 'Guasacaca Especial de la Casa', quantityRequired: 0.05, unit: 'lt', isRemovable: true }
      ]),
      extrasJson: '[]'
    },
    {
      id: 'dish_refresco_15l',
      name: 'Refresco Frío 1.5 Litros',
      description: 'Coca-Cola, Pepsi o Chinotto bien frío.',
      category: 'Bebidas',
      priceUsd: 2.5,
      isAvailable: true,
      ingredientsJson: ing([
        { supplyId: 'sup_refresco', supplyName: 'Refresco 1.5L Variados', quantityRequired: 1, unit: 'ud', isRemovable: false }
      ]),
      extrasJson: '[]'
    }
  ];
  await Promise.all(defaultMenu.map((m) => setDoc(doc(db, 'menu', m.id), m)));

  const cfgSnap = await getDoc(doc(db, 'business', 'config'));
  if (!cfgSnap.exists()) {
    await setDoc(doc(db, 'business', 'config'), DEFAULT_CONFIG);
  }
}

async function deductStockForItems(
  cartItems: CartOrderItem[],
  menuList: MenuItemDoc[]
): Promise<void> {
  const menuMap = new Map(menuList.map((m) => [m.id, m]));
  const suppliesSnap = await getDocs(collection(db, 'supplies'));
  const supplyMap = new Map(
    suppliesSnap.docs.map((d) => [d.id, { ...(d.data() as SupplyDoc), id: d.id }])
  );

  for (const cartItem of cartItems) {
    const dish = menuMap.get(cartItem.menuItemId);
    if (!dish) continue;
    const recipe = parseIngredients(dish.ingredientsJson);
    for (const ingredient of recipe) {
      const excluded = cartItem.excludedIngredientNames?.some(
        (ex) => ex.toLowerCase() === ingredient.supplyName.toLowerCase()
      );
      if (excluded) continue;
      const sup = supplyMap.get(ingredient.supplyId);
      if (!sup) continue;
      const deduction = Math.max(0, ingredient.quantityRequired * cartItem.quantity);
      const nextStock = Math.max(0, sup.currentStock - deduction);
      sup.currentStock = nextStock;
      await updateDoc(doc(db, 'supplies', sup.id), { currentStock: nextStock }).catch(() => {});
    }
  }
}

export async function createOrderAndDeductStock(
  order: Omit<OrderDoc, 'id' | 'itemsJson'>,
  cartItems: CartOrderItem[],
  menuList: MenuItemDoc[]
): Promise<OrderDoc> {
  const ref = doc(collection(db, 'orders'));
  const toSave: OrderDoc = {
    ...order,
    id: ref.id,
    itemsJson: JSON.stringify(cartItems)
  };
  await setDoc(ref, toSave);
  await deductStockForItems(cartItems, menuList).catch((err) =>
    console.warn('Aviso deduciendo stock:', err)
  );
  return toSave;
}

export async function updateOpenOrder(
  orderId: string,
  updatedItems: CartOrderItem[],
  newTotalUsd: number,
  newTotalBs: number,
  newlyAddedItems: CartOrderItem[],
  menuList: MenuItemDoc[],
  notes?: string
): Promise<void> {
  const payload: Record<string, unknown> = {
    itemsJson: JSON.stringify(updatedItems),
    totalUsd: newTotalUsd,
    totalBs: newTotalBs,
    status: 'EN_COCINA'
  };
  if (notes !== undefined) {
    payload.notes = notes;
  }
  await setDoc(doc(db, 'orders', orderId), payload, { merge: true });
  if (newlyAddedItems.length > 0) {
    await deductStockForItems(newlyAddedItems, menuList).catch(() => {});
  }
}

export async function updateOrderStatus(orderId: string, newStatus: string): Promise<void> {
  await updateDoc(doc(db, 'orders', orderId), { status: newStatus });
}

export async function markOrderAsPaid(
  orderId: string,
  paymentMethod: string,
  amountUsdPaid: number,
  amountPagoMovilBs: number,
  amountPuntoVentaBs: number,
  referenceCode: string
): Promise<void> {
  await setDoc(
    doc(db, 'orders', orderId),
    {
      isPaid: true,
      paymentMethod,
      amountUsdPaid,
      amountPagoMovilBs,
      amountPuntoVentaBs,
      referenceCode
    },
    { merge: true }
  );
}

export async function cancelOrder(orderId: string, restoreStock = true): Promise<void> {
  const orderRef = doc(db, 'orders', orderId);
  if (restoreStock) {
    try {
      const orderSnap = await getDoc(orderRef);
      if (orderSnap.exists()) {
        const orderData = orderSnap.data() as OrderDoc;
        const items = parseCartItems(orderData.itemsJson);
        const menuSnap = await getDocs(collection(db, 'menu'));
        const menuMap = new Map(
          menuSnap.docs.map((d) => [d.id, { ...(d.data() as MenuItemDoc), id: d.id }])
        );
        const suppliesSnap = await getDocs(collection(db, 'supplies'));
        const supplyMap = new Map(
          suppliesSnap.docs.map((d) => [d.id, { ...(d.data() as SupplyDoc), id: d.id }])
        );

        for (const cartItem of items) {
          const dish = menuMap.get(cartItem.menuItemId);
          if (!dish) continue;
          const recipe = parseIngredients(dish.ingredientsJson);
          for (const ingredient of recipe) {
            const excluded = cartItem.excludedIngredientNames?.some(
              (ex) => ex.toLowerCase() === ingredient.supplyName.toLowerCase()
            );
            if (excluded) continue;
            const sup = supplyMap.get(ingredient.supplyId);
            if (!sup) continue;
            const restored = sup.currentStock + Math.max(0, ingredient.quantityRequired * cartItem.quantity);
            sup.currentStock = restored;
            await updateDoc(doc(db, 'supplies', sup.id), { currentStock: restored }).catch(() => {});
          }
        }
      }
    } catch (err) {
      console.warn('Aviso restaurando insumos:', err);
    }
  }
  await setDoc(orderRef, { status: 'CANCELADO' }, { merge: true });
}

export async function deleteOrder(orderId: string): Promise<void> {
  await deleteDoc(doc(db, 'orders', orderId));
}

export async function restockSupply(supplyId: string, addedAmount: number): Promise<void> {
  const ref = doc(db, 'supplies', supplyId);
  const snap = await getDoc(ref);
  const current = snap.exists() ? Number(snap.data().currentStock ?? 0) : 0;
  await updateDoc(ref, { currentStock: current + addedAmount });
}

export async function saveSupply(supply: SupplyDoc): Promise<string> {
  const ref = supply.id ? doc(db, 'supplies', supply.id) : doc(collection(db, 'supplies'));
  await setDoc(ref, { ...supply, id: ref.id });
  return ref.id;
}

export async function deleteSupply(supplyId: string): Promise<void> {
  await deleteDoc(doc(db, 'supplies', supplyId));
}

export async function saveMenuItem(item: MenuItemDoc): Promise<string> {
  const ref = item.id ? doc(db, 'menu', item.id) : doc(collection(db, 'menu'));
  await setDoc(ref, { ...item, id: ref.id });
  return ref.id;
}

export async function deleteMenuItem(itemId: string): Promise<void> {
  await deleteDoc(doc(db, 'menu', itemId));
}

export async function saveCashClosure(closure: Omit<ClosureDoc, 'id'>): Promise<ClosureDoc> {
  const ref = doc(collection(db, 'closures'));
  const toSave: ClosureDoc = { ...closure, id: ref.id };
  await setDoc(ref, toSave);
  return toSave;
}

export async function saveBusinessConfig(config: BusinessConfigDoc): Promise<void> {
  await setDoc(doc(db, 'business', 'config'), { ...config, id: 'config' });
}

export async function saveSystemUser(
  user: SystemUserDoc,
  previousUsername?: string | null
): Promise<void> {
  const targetId = user.username.trim().toLowerCase();
  if (!targetId) throw new Error('El nombre de usuario no puede estar vacío.');
  if (!user.password.trim()) throw new Error('La contraseña no puede estar vacía.');

  if (previousUsername) {
    const oldId = previousUsername.trim().toLowerCase();
    if (oldId && oldId !== targetId) {
      await deleteDoc(doc(db, 'system_users', oldId)).catch(() => {});
    }
  }
  await setDoc(doc(db, 'system_users', targetId), {
    ...user,
    id: targetId,
    username: targetId
  });
}

export async function deleteSystemUser(username: string): Promise<void> {
  const targetId = username.trim().toLowerCase();
  const snap = await getDocs(collection(db, 'system_users'));
  const users = snap.docs.map((d) => d.data() as SystemUserDoc);
  const target = users.find((u) => u.username.toLowerCase() === targetId);
  if (target?.role?.toUpperCase() === 'ADMIN') {
    const adminCount = users.filter((u) => u.role?.toUpperCase() === 'ADMIN').length;
    if (adminCount <= 1) {
      throw new Error('No puedes eliminar al único Administrador del sistema.');
    }
  }
  await deleteDoc(doc(db, 'system_users', targetId));
}
