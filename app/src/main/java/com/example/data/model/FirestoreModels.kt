package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Roles de usuario en el sistema
 */
enum class UserRole {
  ADMIN,
  STAFF
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val displayName: String = "",
  val role: String = "STAFF", // "ADMIN" o "STAFF"
  val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class SystemUserDoc(
  @DocumentId val id: String = "", // username in lowercase
  val username: String = "admin",
  val password: String = "admin200",
  val displayName: String = "Administrador",
  val role: String = "ADMIN", // "ADMIN" o "STAFF"
  val createdAt: Long = System.currentTimeMillis(),
  val isActive: Boolean = true,

  // --- Permisos de Acceso a la Aplicación Android (Tablet / Teléfono) ---
  val canUsePosApp: Boolean = true,        // Tomar pedidos y crear comandas
  val canUseKitchenApp: Boolean = true,    // Monitor de Cocina KDS
  val canUseBillingApp: Boolean = true,    // Cobrar cuentas y emitir recibos
  val canUseInventoryApp: Boolean = false, // Inventario e Insumos
  val canUseClosuresApp: Boolean = false,  // Cierres de Caja y Reportes Financieros
  val canUseMenuApp: Boolean = false,      // Gestión de Menú y Precios
  val canUseSettingsApp: Boolean = false,  // Ajustes del Sistema y Usuarios

  // --- Permisos de Acceso a la Computadora (PC / Web) ---
  val canAccessPc: Boolean = true,         // Permiso general para entrar desde PC
  val canUsePosPc: Boolean = true,         // Tomar pedidos desde PC
  val canUseKitchenPc: Boolean = true,     // Monitor de Cocina en PC
  val canUseBillingPc: Boolean = true,     // Cobrar y ver cuentas en PC
  val canUseInventoryPc: Boolean = false,  // Control de Stock e Insumos en PC
  val canUseReportsPc: Boolean = false     // Cierres y Reportes Financieros en PC
) {
  val isAdmin: Boolean get() = role.equals("ADMIN", ignoreCase = true)

  fun hasPosApp(): Boolean = isAdmin || canUsePosApp
  fun hasKitchenApp(): Boolean = isAdmin || canUseKitchenApp
  fun hasBillingApp(): Boolean = isAdmin || canUseBillingApp
  fun hasInventoryApp(): Boolean = isAdmin || canUseInventoryApp
  fun hasClosuresApp(): Boolean = isAdmin || canUseClosuresApp
  fun hasMenuApp(): Boolean = isAdmin || canUseMenuApp
  fun hasSettingsApp(): Boolean = isAdmin || canUseSettingsApp

  fun hasAccessPc(): Boolean = isAdmin || canAccessPc
  fun hasPosPc(): Boolean = isAdmin || (canAccessPc && canUsePosPc)
  fun hasKitchenPc(): Boolean = isAdmin || (canAccessPc && canUseKitchenPc)
  fun hasBillingPc(): Boolean = isAdmin || (canAccessPc && canUseBillingPc)
  fun hasInventoryPc(): Boolean = isAdmin || (canAccessPc && canUseInventoryPc)
  fun hasReportsPc(): Boolean = isAdmin || (canAccessPc && canUseReportsPc)
}

@IgnoreExtraProperties
data class SupplyDoc(
  @DocumentId val id: String = "",
  val name: String = "",
  val currentStock: Double = 0.0,
  val minStockThreshold: Double = 5.0,
  val unit: String = "kg", // "kg", "g", "ud", "porc", "lt"
  val costUsd: Double = 0.0,
  val recommendedRestock: Double = 10.0
)

@IgnoreExtraProperties
data class IngredientRequirement(
  val supplyId: String = "",
  val supplyName: String = "",
  val quantityRequired: Double = 0.0,
  val unit: String = "kg",
  val isRemovable: Boolean = true
)

@IgnoreExtraProperties
data class MenuItemExtra(
  val name: String = "",
  val priceUsd: Double = 0.0
)

@IgnoreExtraProperties
data class MenuItemDoc(
  @DocumentId val id: String = "",
  val name: String = "",
  val description: String = "",
  val category: String = "Parrillas",
  val priceUsd: Double = 0.0,
  val isAvailable: Boolean = true,
  val ingredientsJson: String = "", // Lista de IngredientRequirement en JSON
  val extrasJson: String = ""       // Lista de MenuItemExtra en JSON
)

@IgnoreExtraProperties
data class CartOrderItem(
  val menuItemId: String = "",
  val dishName: String = "",
  val quantity: Int = 1,
  val priceUsd: Double = 0.0,
  val excludedIngredientNames: List<String> = emptyList(),
  val specialInstructions: String = ""
)

@IgnoreExtraProperties
data class OrderDoc(
  @DocumentId val id: String = "",
  val orderNumber: Int = 1,
  val customerName: String = "Cliente",
  val tableOrDestination: String = "Mesa 1",
  val timestamp: Long = System.currentTimeMillis(),
  val status: String = "EN_COCINA", // "EN_COCINA", "LISTO", "ENTREGADO", "CANCELADO"
  val totalUsd: Double = 0.0,
  val exchangeRate: Double = 65.0,
  val totalBs: Double = 0.0,
  val paymentMethod: String = "PENDIENTE", // "USD_EFECTIVO", "PAGO_MOVIL", "PUNTO_VENTA", "MIXTO", "PENDIENTE"
  val amountUsdPaid: Double = 0.0,
  val amountPagoMovilBs: Double = 0.0,
  val amountPuntoVentaBs: Double = 0.0,
  val referenceCode: String = "",
  val notes: String = "",
  val isPaid: Boolean = false,
  val createdBy: String = "",
  val itemsJson: String = "", // Lista de CartOrderItem
  val closureId: String? = null
)

@IgnoreExtraProperties
data class ClosureDoc(
  @DocumentId val id: String = "",
  val closureType: String = "DIARIO", // "DIARIO", "SEMANAL", "MENSUAL"
  val closureNumber: Int = 1,
  val timestamp: Long = System.currentTimeMillis(),
  val startDate: Long = 0L,
  val endDate: Long = System.currentTimeMillis(),
  val totalSalesUsd: Double = 0.0,
  val totalSalesBs: Double = 0.0,
  val totalUsdCash: Double = 0.0,
  val totalPagoMovilBs: Double = 0.0,
  val totalPuntoVentaBs: Double = 0.0,
  val orderCount: Int = 0,
  val closedBy: String = "",
  val notes: String = ""
)

@IgnoreExtraProperties
data class BusinessConfigDoc(
  @DocumentId val id: String = "config",
  val businessName: String = "LA PARRILLA 200",
  val rif: String = "J-40982314-0",
  val phone: String = "+58 412-5552000",
  val address: String = "Sector Parrillera 200",
  val ticketHeader: String = "¡Los mejores cortes y sabor a la brasa!",
  val ticketFooter: String = "¡Gracias por su compra! Vuelva pronto.\nTicket de control para despacho.",
  val printerWidthMm: Int = 48,
  val exchangeRateUsdToBs: Double = 65.0
)
