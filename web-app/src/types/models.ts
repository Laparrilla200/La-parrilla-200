export type AppScreen =
  | 'POS'
  | 'KITCHEN'
  | 'INVENTORY'
  | 'CLOSURES'
  | 'MENU_ADMIN'
  | 'AI_ASSISTANT'
  | 'SETTINGS';

export type PaperWidth = 58 | 80;

export type OrderStatus = 'EN_COCINA' | 'LISTO' | 'ENTREGADO' | 'CANCELADO';

export type PaymentMethod =
  | 'USD_EFECTIVO'
  | 'PAGO_MOVIL'
  | 'PUNTO_VENTA'
  | 'MIXTO'
  | 'PENDIENTE';

export interface SystemUserDoc {
  id: string;
  username: string;
  password: string;
  displayName: string;
  role: 'ADMIN' | 'STAFF' | string;
  createdAt: number;
  isActive: boolean;
  // Permisos App Móvil
  canUsePosApp: boolean;
  canUseKitchenApp: boolean;
  canUseBillingApp: boolean;
  canUseInventoryApp: boolean;
  canUseClosuresApp: boolean;
  canUseMenuApp: boolean;
  canUseSettingsApp: boolean;
  // Permisos PC / Web
  canAccessPc: boolean;
  canUsePosPc: boolean;
  canUseKitchenPc: boolean;
  canUseBillingPc: boolean;
  canUseInventoryPc: boolean;
  canUseReportsPc: boolean;
}

export interface SupplyDoc {
  id: string;
  name: string;
  currentStock: number;
  minStockThreshold: number;
  unit: string;
  costUsd: number;
  recommendedRestock: number;
}

export interface IngredientRequirement {
  supplyId: string;
  supplyName: string;
  quantityRequired: number;
  unit: string;
  isRemovable: boolean;
}

export interface MenuItemExtra {
  name: string;
  priceUsd: number;
}

export interface MenuItemDoc {
  id: string;
  name: string;
  description: string;
  category: string;
  priceUsd: number;
  isAvailable: boolean;
  ingredientsJson: string;
  extrasJson: string;
}

export interface CartOrderItem {
  menuItemId: string;
  dishName: string;
  quantity: number;
  priceUsd: number;
  excludedIngredientNames: string[];
  specialInstructions: string;
}

export interface OrderDoc {
  id: string;
  orderNumber: number;
  customerName: string;
  tableOrDestination: string;
  timestamp: number;
  status: OrderStatus | string;
  totalUsd: number;
  exchangeRate: number;
  totalBs: number;
  paymentMethod: PaymentMethod | string;
  amountUsdPaid: number;
  amountPagoMovilBs: number;
  amountPuntoVentaBs: number;
  referenceCode: string;
  notes: string;
  isPaid: boolean;
  createdBy: string;
  itemsJson: string;
  closureId?: string | null;
}

export interface ClosureDoc {
  id: string;
  closureType: 'DIARIO' | 'SEMANAL' | 'MENSUAL' | string;
  closureNumber: number;
  timestamp: number;
  startDate: number;
  endDate: number;
  totalSalesUsd: number;
  totalSalesBs: number;
  totalUsdCash: number;
  totalPagoMovilBs: number;
  totalPuntoVentaBs: number;
  orderCount: number;
  closedBy: string;
  notes: string;
}

export interface BusinessConfigDoc {
  id: string;
  businessName: string;
  rif: string;
  phone: string;
  address: string;
  ticketHeader: string;
  ticketFooter: string;
  printerWidthMm: number;
  exchangeRateUsdToBs: number;
}

export interface RestockItem {
  supply: SupplyDoc;
  shortageAmount: number;
  toBuyAmount: number;
  estimatedCostUsd: number;
  estimatedCostBs: number;
}

export const DEFAULT_CONFIG: BusinessConfigDoc = {
  id: 'config',
  businessName: 'LA PARRILLA 200',
  rif: 'J-40982314-0',
  phone: '+58 412-5552000',
  address: 'Sector Parrillera 200',
  ticketHeader: '¡Los mejores cortes y sabor a la brasa!',
  ticketFooter: '¡Gracias por su compra! Vuelva pronto.\nTicket de control para despacho.',
  printerWidthMm: 48,
  exchangeRateUsdToBs: 65.0
};

export function parseIngredients(json?: string | null): IngredientRequirement[] {
  if (!json) return [];
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function parseExtras(json?: string | null): MenuItemExtra[] {
  if (!json) return [];
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function parseCartItems(json?: string | null): CartOrderItem[] {
  if (!json) return [];
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function isUserAdmin(user: SystemUserDoc | null): boolean {
  return Boolean(user && user.role?.toUpperCase() === 'ADMIN');
}

export function canUserAccessWebScreen(
  user: SystemUserDoc | null,
  screen: AppScreen,
  simulateStaff: boolean
): boolean {
  if (!user) return false;
  if (isUserAdmin(user) && !simulateStaff) return true;
  if (isUserAdmin(user) && simulateStaff) {
    return screen === 'POS' || screen === 'KITCHEN' || screen === 'AI_ASSISTANT';
  }
  if (user.canAccessPc === false) return false;
  switch (screen) {
    case 'POS':
      return user.canUsePosPc ?? true;
    case 'KITCHEN':
      return user.canUseKitchenPc ?? true;
    case 'INVENTORY':
      return user.canUseInventoryPc ?? false;
    case 'CLOSURES':
      return user.canUseReportsPc ?? false;
    case 'MENU_ADMIN':
      return user.canUseMenuApp ?? false;
    case 'SETTINGS':
      return user.canUseSettingsApp ?? false;
    case 'AI_ASSISTANT':
      return true;
    default:
      return false;
  }
}
