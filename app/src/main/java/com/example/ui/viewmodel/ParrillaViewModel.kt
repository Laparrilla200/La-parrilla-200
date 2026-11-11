package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.ClosureDoc
import com.example.data.model.IngredientRequirement
import com.example.data.model.MenuItemDoc
import com.example.data.model.OrderDoc
import com.example.data.model.SupplyDoc
import com.example.data.model.SystemUserDoc
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.printer.ThermalTicketHelper
import com.example.printer.PaperWidth
import com.example.printer.ThermalPrinterManager
import android.content.Context
import com.example.data.notification.InventoryNotificationHelper
import com.example.data.notification.OrderReadyNotificationHelper
import com.example.data.repository.ParrillaRepository
import com.example.data.session.SessionManager
import com.example.data.web.LocalWebServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppScreen {
  POS,
  KITCHEN,
  INVENTORY,
  CLOSURES,
  MENU_ADMIN,
  SETTINGS,
  PROFILE
}

data class RestockItem(
  val supply: SupplyDoc,
  val shortageAmount: Double,
  val toBuyAmount: Double,
  val estimatedCostUsd: Double,
  val estimatedCostBs: Double
)

class ParrillaViewModel(
  private val repository: ParrillaRepository,
  context: Context? = null
) : ViewModel() {

  private val sessionManager: SessionManager? = context?.let { SessionManager(it.applicationContext) }

  private val _currentScreen = MutableStateFlow(AppScreen.POS)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  // Inicializa con la sesión persistida para que NUNCA se cierre de golpe
  private val _currentLoggedUser = MutableStateFlow<SystemUserDoc?>(sessionManager?.getSession())
  val currentLoggedUser: StateFlow<SystemUserDoc?> = _currentLoggedUser.asStateFlow()

  val systemUsers: StateFlow<List<SystemUserDoc>> = repository.observeSystemUsers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val userProfile: StateFlow<UserProfile?> = _currentLoggedUser.map { user ->
    if (user != null) {
      UserProfile(
        id = user.id,
        userId = user.username,
        email = "@${user.username}",
        displayName = user.displayName,
        role = user.role,
        createdAt = user.createdAt
      )
    } else null
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  // Permite al Administrador simular la vista del Personal de Cocina
  private val _roleViewMode = MutableStateFlow<UserRole?>(null)
  val roleViewMode: StateFlow<UserRole?> = _roleViewMode.asStateFlow()

  val currentEffectiveRole: UserRole
    get() {
      _roleViewMode.value?.let { return it }
      val profileRole = _currentLoggedUser.value?.role
      return if (profileRole.equals("ADMIN", ignoreCase = true)) UserRole.ADMIN else UserRole.STAFF
    }

  val isCurrentUserAdmin: Boolean
    get() = _currentLoggedUser.value?.role.equals("ADMIN", ignoreCase = true)

  fun canUserAccessScreen(screen: AppScreen): Boolean {
    val user = _currentLoggedUser.value ?: return false
    if (user.role.equals("ADMIN", ignoreCase = true)) {
      if (_roleViewMode.value == UserRole.STAFF) {
        return screen in listOf(AppScreen.POS, AppScreen.KITCHEN, AppScreen.PROFILE)
      }
      return true
    }
    return when (screen) {
      AppScreen.POS -> user.canUsePosApp
      AppScreen.KITCHEN -> user.canUseKitchenApp
      AppScreen.INVENTORY -> user.canUseInventoryApp
      AppScreen.CLOSURES -> user.canUseClosuresApp
      AppScreen.MENU_ADMIN -> user.canUseMenuApp
      AppScreen.SETTINGS -> user.canUseSettingsApp
      AppScreen.PROFILE -> true
    }
  }

  fun canUserBill(): Boolean {
    val user = _currentLoggedUser.value ?: return false
    if (_roleViewMode.value == UserRole.STAFF && !user.canUseBillingApp) return false
    return user.hasBillingApp()
  }

  // --- Datos en tiempo real de Firestore ---
  val supplies: StateFlow<List<SupplyDoc>> = repository.observeSupplies()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val menuItems: StateFlow<List<MenuItemDoc>> = repository.observeMenu()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // --- Órdenes con soporte para actualización optimista instantánea ---
  private val _orders = MutableStateFlow<List<OrderDoc>>(emptyList())
  val orders: StateFlow<List<OrderDoc>> = _orders.asStateFlow()

  // Cuentas activas pendientes de cobro (se quitan al primer toque inmediatamente)
  val unpaidOrders: StateFlow<List<OrderDoc>> = _orders.map { list ->
    list.filter { !it.isPaid && it.status != "CANCELADO" }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // Cuentas ya liquidadas / pagadas para control y evitar volver a cobrar
  val paidOrders: StateFlow<List<OrderDoc>> = _orders.map { list ->
    list.filter { it.isPaid && it.status != "CANCELADO" }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // Servidor Web para gestión por Computadora / PC en la red local
  val localWebServer = LocalWebServer(repository)
  private val _webServerUrl = MutableStateFlow(localWebServer.getServerUrl())
  val webServerUrl: StateFlow<String> = _webServerUrl.asStateFlow()

  val closures: StateFlow<List<ClosureDoc>> = repository.observeClosures()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val config: StateFlow<BusinessConfigDoc> = repository.observeConfig()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessConfigDoc())

  val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // --- Alertas de Inventario Crítico y Recomendaciones de Reabastecimiento ---
  val criticalSupplies: StateFlow<List<SupplyDoc>> = supplies.combine(config) { list, _ ->
    list.filter { it.currentStock <= it.minStockThreshold }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val restockRecommendations: StateFlow<List<RestockItem>> = supplies.combine(config) { list, cfg ->
    list.filter { it.currentStock <= it.minStockThreshold }
      .map { sup ->
        val shortage = (sup.minStockThreshold - sup.currentStock).coerceAtLeast(0.0)
        val toBuy = if (sup.recommendedRestock > 0) sup.recommendedRestock else (shortage + 5.0)
        val costUsd = toBuy * sup.costUsd
        val costBs = costUsd * cfg.exchangeRateUsdToBs
        RestockItem(
          supply = sup,
          shortageAmount = shortage,
          toBuyAmount = toBuy,
          estimatedCostUsd = costUsd,
          estimatedCostBs = costBs
        )
      }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // --- Carrito de Compras en Punto de Venta ---
  private val _cart = MutableStateFlow<List<CartOrderItem>>(emptyList())
  val cart: StateFlow<List<CartOrderItem>> = _cart.asStateFlow()

  private val _customerName = MutableStateFlow("Cliente")
  val customerName: StateFlow<String> = _customerName.asStateFlow()

  private val _tableOrDestination = MutableStateFlow("Mesa 1")
  val tableOrDestination: StateFlow<String> = _tableOrDestination.asStateFlow()

  // --- Diálogo de Ticket Térmico de 48mm ---
  private val _showTicketDialog = MutableStateFlow(false)
  val showTicketDialog: StateFlow<Boolean> = _showTicketDialog.asStateFlow()

  private val _currentTicketTitle = MutableStateFlow("")
  val currentTicketTitle: StateFlow<String> = _currentTicketTitle.asStateFlow()

  private val _currentTicketText = MutableStateFlow("")
  val currentTicketText: StateFlow<String> = _currentTicketText.asStateFlow()

  // Mensaje de éxito / error (Snackbar)
  private val _userMessage = MutableStateFlow<String?>(null)
  val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

  init {
    viewModelScope.launch {
      // 0. Sincronizar órdenes en tiempo real de Firestore con _orders
      launch {
        repository.observeOrders().collect { updatedList ->
          _orders.value = updatedList
        }
      }

      // 1. Pre-cargar catálogo de platos e insumos y usuarios del sistema si la base está nueva
      repository.seedInitialDataIfEmpty()
      repository.seedDefaultSystemUsersIfEmpty()

      // 2. Iniciar Servidor Web local para Computadora / PC
      localWebServer.start(viewModelScope)
      _webServerUrl.value = localWebServer.getServerUrl()

      // 3. Notificaciones push al Administrador por insumos faltantes o críticos
      if (context != null) {
        launch {
          criticalSupplies.collect { critList ->
            val isAdmin = _currentLoggedUser.value?.role?.equals("ADMIN", ignoreCase = true) == true ||
              currentEffectiveRole == UserRole.ADMIN
            if (isAdmin && critList.isNotEmpty()) {
              for (sup in critList) {
                InventoryNotificationHelper.notifyCriticalSupply(
                  context = context,
                  supplyId = sup.id,
                  supplyName = sup.name,
                  currentStock = sup.currentStock,
                  minThreshold = sup.minStockThreshold,
                  unit = sup.unit
                )
              }
            }
          }
        }

        // 4. Notificaciones push a Cajero y Mesoneros cuando un pedido está LISTO en cocina
        launch {
          var previousStatuses = emptyMap<String, String>()
          orders.collect { orderList ->
            val currentRole = _currentLoggedUser.value?.role?.uppercase() ?: ""
            val isEligibleToReceive = currentRole in listOf("CAJERO", "MESONERO", "WAITER", "STAFF", "ADMIN") ||
              currentEffectiveRole in listOf(UserRole.ADMIN, UserRole.STAFF)

            if (previousStatuses.isNotEmpty()) {
              for (order in orderList) {
                val prevStatus = previousStatuses[order.id]
                // Se disparó cuando cocinero marcó LISTO desde cocina o tablet
                if (order.status == "LISTO" && prevStatus != null && prevStatus != "LISTO") {
                  if (isEligibleToReceive) {
                    OrderReadyNotificationHelper.notifyOrderReady(context, order)
                    _userMessage.value = "🔔 ¡Pedido #${order.orderNumber} (${order.tableOrDestination}) LISTO para servir!"
                  }
                }
              }
            }
            previousStatuses = orderList.associate { it.id to it.status }
          }
        }
      }
    }
  }

  fun login(
    username: String,
    pass: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val cleanUser = username.trim().lowercase()
      val cleanPass = pass.trim()
      if (cleanUser.isBlank() || cleanPass.isBlank()) {
        onResult(false, "Por favor ingresa usuario y contraseña")
        return@launch
      }

      val user = repository.authenticateSystemUser(cleanUser, cleanPass)
      if (user != null) {
        _currentLoggedUser.value = user
        sessionManager?.saveSession(user)
        _roleViewMode.value = null
        _userMessage.value = "Bienvenido, ${user.displayName}"
        onResult(true, null)
      } else {
        onResult(false, "Usuario o contraseña incorrectos")
      }
    }
  }

  fun logout() {
    sessionManager?.clearSession()
    _currentLoggedUser.value = null
    _roleViewMode.value = null
    _cart.value = emptyList()
  }

  fun saveSystemUser(
    user: SystemUserDoc,
    previousUsername: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = repository.saveOrUpdateSystemUser(user, previousUsername)
      res.fold(
        onSuccess = {
          val current = _currentLoggedUser.value
          if (current != null && (current.username.equals(previousUsername, ignoreCase = true) || current.username.equals(user.username, ignoreCase = true))) {
            val updated = user.copy(username = user.username.trim().lowercase())
            _currentLoggedUser.value = updated
            sessionManager?.saveSession(updated)
          }
          _userMessage.value = "Usuario '${user.username}' guardado exitosamente"
          onResult(true, null)
        },
        onFailure = { e ->
          _userMessage.value = "Error: ${e.message}"
          onResult(false, e.message ?: "Error al guardar usuario")
        }
      )
    }
  }

  fun deleteSystemUser(
    username: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val res = repository.deleteSystemUser(username)
      res.fold(
        onSuccess = {
          _userMessage.value = "Usuario '$username' eliminado"
          onResult(true, null)
        },
        onFailure = { e ->
          _userMessage.value = "Error: ${e.message}"
          onResult(false, e.message ?: "Error al eliminar usuario")
        }
      )
    }
  }

  fun setScreen(screen: AppScreen) {
    _currentScreen.value = screen
  }

  fun toggleRoleViewMode(targetRole: UserRole?) {
    _roleViewMode.value = targetRole
  }

  fun clearUserMessage() {
    _userMessage.value = null
  }

  fun setCustomerName(name: String) {
    _customerName.value = name
  }

  fun setTableOrDestination(table: String) {
    _tableOrDestination.value = table
  }

  // --- Gestión de Carrito y Personalización de Ingredientes ---

  fun addToCart(
    menuItem: MenuItemDoc,
    quantity: Int = 1,
    excludedIngredients: List<String> = emptyList(),
    specialInstructions: String = ""
  ) {
    val currentList = _cart.value.toMutableList()
    // Buscar si ya existe el mismo plato con exactamente las mismas personalizaciones
    val existingIndex = currentList.indexOfFirst {
      it.menuItemId == menuItem.id &&
        it.excludedIngredientNames == excludedIngredients &&
        it.specialInstructions.equals(specialInstructions, ignoreCase = true)
    }

    if (existingIndex >= 0) {
      val existing = currentList[existingIndex]
      currentList[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
    } else {
      currentList.add(
        CartOrderItem(
          menuItemId = menuItem.id,
          dishName = menuItem.name,
          quantity = quantity,
          priceUsd = menuItem.priceUsd,
          excludedIngredientNames = excludedIngredients,
          specialInstructions = specialInstructions
        )
      )
    }
    _cart.value = currentList
    _userMessage.value = "Agregado: ${menuItem.name}"
  }

  fun updateCartItemQuantity(index: Int, newQuantity: Int) {
    if (index !in _cart.value.indices) return
    val currentList = _cart.value.toMutableList()
    if (newQuantity <= 0) {
      currentList.removeAt(index)
    } else {
      currentList[index] = currentList[index].copy(quantity = newQuantity)
    }
    _cart.value = currentList
  }

  fun clearCart() {
    _cart.value = emptyList()
  }

  fun parseIngredients(json: String): List<IngredientRequirement> =
    repository.parseIngredients(json)

  fun serializeIngredients(list: List<IngredientRequirement>): String =
    repository.serializeIngredients(list)

  fun parseCartItems(json: String): List<CartOrderItem> =
    repository.parseCartItems(json)

  // --- Procesamiento de Pedidos con Descuento Automático en Tiempo Real ---

  fun submitOrderAndCheckout(
    paymentMethod: String,
    amountUsdPaid: Double,
    amountPagoMovilBs: Double,
    amountPuntoVentaBs: Double,
    referenceCode: String,
    isPaid: Boolean,
    notes: String = "",
    context: Context? = null,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ) {
    val currentCartItems = _cart.value
    if (currentCartItems.isEmpty()) {
      _userMessage.value = "El carrito está vacío"
      return
    }

    viewModelScope.launch {
      try {
        val nextOrderNumber = (orders.value.maxOfOrNull { it.orderNumber } ?: 0) + 1
        val rate = config.value.exchangeRateUsdToBs
        val totalUsd = currentCartItems.sumOf { it.priceUsd * it.quantity }
        val totalBs = totalUsd * rate

        val order = OrderDoc(
          orderNumber = nextOrderNumber,
          customerName = _customerName.value.ifBlank { "Cliente" },
          tableOrDestination = _tableOrDestination.value.ifBlank { "Mesa 1" },
          timestamp = System.currentTimeMillis(),
          status = "EN_COCINA",
          totalUsd = totalUsd,
          exchangeRate = rate,
          totalBs = totalBs,
          paymentMethod = paymentMethod,
          amountUsdPaid = amountUsdPaid,
          amountPagoMovilBs = amountPagoMovilBs,
          amountPuntoVentaBs = amountPuntoVentaBs,
          referenceCode = referenceCode,
          notes = notes,
          isPaid = isPaid,
          createdBy = _currentLoggedUser.value?.displayName ?: "Personal"
        )

        val newOrderId = repository.createOrderAndDeductStock(
          order = order,
          cartItems = currentCartItems,
          menuList = menuItems.value
        )

        // Limpiar carrito
        clearCart()
        _customerName.value = "Cliente"

        // Mostrar ticket térmico listo para imprimir en 58mm/80mm
        val savedOrder = order.copy(id = newOrderId)
        val ticketText = ThermalTicketHelper.generateOrderReceiptText(
          order = savedOrder,
          items = currentCartItems,
          config = config.value,
          paperWidth = paperWidth
        )
        if (context != null) {
          ThermalTicketHelper.printTicketViaPrintManager(
            context = context,
            title = "Ticket Pedido #${savedOrder.orderNumber}",
            ticketText = ticketText,
            paperWidth = paperWidth
          )
        }
        showThermalTicket("Ticket Pedido #${savedOrder.orderNumber}", ticketText)
        _userMessage.value = "¡Pedido #${savedOrder.orderNumber} enviado a cocina y cobrado!"
      } catch (e: Exception) {
        _userMessage.value = "Error al procesar pedido: ${e.localizedMessage}"
      }
    }
  }

  fun updateOrderStatus(orderId: String, newStatus: String) {
    _orders.update { list ->
      list.map { if (it.id == orderId) it.copy(status = newStatus) else it }
    }
    viewModelScope.launch {
      try {
        val res = repository.updateOrderStatus(orderId, newStatus)
        res.fold(
          onSuccess = { _userMessage.value = "Estado actualizado a $newStatus" },
          onFailure = { _userMessage.value = "Error al actualizar estado: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  fun markOrderPaid(
    order: OrderDoc,
    paymentMethod: String,
    usd: Double,
    pagoMovilBs: Double,
    puntoVentaBs: Double,
    ref: String
  ) {
    _orders.update { list ->
      list.map {
        if (it.id == order.id) {
          it.copy(
            isPaid = true,
            paymentMethod = paymentMethod,
            amountUsdPaid = usd,
            amountPagoMovilBs = pagoMovilBs,
            amountPuntoVentaBs = puntoVentaBs,
            referenceCode = ref
          )
        } else it
      }
    }
    viewModelScope.launch {
      try {
        val res = repository.markOrderAsPaid(
          orderId = order.id,
          paymentMethod = paymentMethod,
          amountUsd = usd,
          amountPagoMovilBs = pagoMovilBs,
          amountPuntoVentaBs = puntoVentaBs,
          refCode = ref
        )
        res.fold(
          onSuccess = { _userMessage.value = "¡Pedido #${order.orderNumber} marcado como PAGADO!" },
          onFailure = { _userMessage.value = "Error al registrar pago: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  fun cancelOrder(orderId: String, restoreStock: Boolean = true, onComplete: ((Boolean) -> Unit)? = null) {
    _orders.update { list ->
      list.map { if (it.id == orderId) it.copy(status = "CANCELADO") else it }
    }
    viewModelScope.launch {
      try {
        val res = repository.cancelOrder(orderId, restoreStock)
        res.fold(
          onSuccess = {
            _userMessage.value = "Pedido cancelado y productos restaurados"
            onComplete?.invoke(true)
          },
          onFailure = {
            _userMessage.value = "Error al cancelar pedido: ${it.localizedMessage}"
            onComplete?.invoke(false)
          }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
        onComplete?.invoke(false)
      }
    }
  }

  fun deleteOrder(orderId: String, onComplete: ((Boolean) -> Unit)? = null) {
    // Eliminación optimista instantánea: desaparece de inmediato de pantalla sin esperar a la nube
    _orders.update { list -> list.filterNot { it.id == orderId } }
    viewModelScope.launch {
      try {
        val res = repository.deleteOrder(orderId)
        res.fold(
          onSuccess = {
            _userMessage.value = "Cuenta eliminada del sistema definitivamente"
            onComplete?.invoke(true)
          },
          onFailure = {
            _userMessage.value = "Error al eliminar: ${it.localizedMessage}"
            onComplete?.invoke(false)
          }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
        onComplete?.invoke(false)
      }
    }
  }

  fun editOpenOrder(
    orderId: String,
    updatedItems: List<CartOrderItem>,
    newlyAddedItems: List<CartOrderItem> = emptyList(),
    notes: String? = null,
    onComplete: ((Boolean) -> Unit)? = null
  ) {
    viewModelScope.launch {
      try {
        val rate = config.value.exchangeRateUsdToBs
        val newTotalUsd = updatedItems.sumOf { it.priceUsd * it.quantity }
        val newTotalBs = newTotalUsd * rate

        val res = repository.updateOpenOrder(
          orderId = orderId,
          updatedItems = updatedItems,
          newTotalUsd = newTotalUsd,
          newTotalBs = newTotalBs,
          newlyAddedItems = newlyAddedItems,
          menuList = menuItems.value,
          notes = notes
        )
        res.fold(
          onSuccess = {
            _userMessage.value = "¡Cuenta abierta actualizada exitosamente!"
            onComplete?.invoke(true)
          },
          onFailure = {
            _userMessage.value = "Error al actualizar cuenta: ${it.localizedMessage}"
            onComplete?.invoke(false)
          }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
        onComplete?.invoke(false)
      }
    }
  }

  // --- Cierres de Caja (Diario, Semanal, Mensual) ---

  fun generateCashClosure(
    closureType: String, // "DIARIO", "SEMANAL", "MENSUAL"
    notes: String = ""
  ) {
    viewModelScope.launch {
      try {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now

        val startDate = when (closureType) {
          "SEMANAL" -> {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.timeInMillis
          }
          "MENSUAL" -> {
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            calendar.timeInMillis
          }
          else -> { // DIARIO: desde las 00:00 de hoy
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
          }
        }

        // Filtrar pedidos que correspondan a este período
        val relevantOrders = orders.value.filter {
          it.timestamp in startDate..now && it.status != "CANCELADO"
        }

        val totalSalesUsd = relevantOrders.sumOf { it.totalUsd }
        val totalSalesBs = relevantOrders.sumOf { it.totalBs }
        val totalUsdCash = relevantOrders.sumOf { it.amountUsdPaid }
        val totalPagoMovilBs = relevantOrders.sumOf { it.amountPagoMovilBs }
        val totalPuntoVentaBs = relevantOrders.sumOf { it.amountPuntoVentaBs }
        val nextClosureNumber = (closures.value.maxOfOrNull { it.closureNumber } ?: 0) + 1

        val closure = ClosureDoc(
          closureType = closureType,
          closureNumber = nextClosureNumber,
          timestamp = now,
          startDate = startDate,
          endDate = now,
          totalSalesUsd = totalSalesUsd,
          totalSalesBs = totalSalesBs,
          totalUsdCash = totalUsdCash,
          totalPagoMovilBs = totalPagoMovilBs,
          totalPuntoVentaBs = totalPuntoVentaBs,
          orderCount = relevantOrders.size,
          closedBy = _currentLoggedUser.value?.displayName ?: "Administrador",
          notes = notes
        )

        val closureId = repository.saveCashClosure(closure)
        val savedClosure = closure.copy(id = closureId)

        // Generar e imprimir automáticamente el ticket de cierre de 48mm/58mm/80mm
        val closureTicket = ThermalTicketHelper.generateClosureReceiptText(savedClosure, config.value)
        showThermalTicket("Cierre $closureType #${savedClosure.closureNumber}", closureTicket)
        _userMessage.value = "¡Cierre $closureType generado con éxito!"
      } catch (e: Exception) {
        _userMessage.value = "Error al generar cierre: ${e.localizedMessage}"
      }
    }
  }

  // --- Gestión de Inventario y Reabastecimiento ---

  fun restockSupply(supplyId: String, addedAmount: Double) {
    viewModelScope.launch {
      try {
        val res = repository.restockSupply(supplyId, addedAmount)
        res.fold(
          onSuccess = { _userMessage.value = "Inventario actualizado exitosamente" },
          onFailure = { _userMessage.value = "Error al actualizar inventario: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  fun saveSupply(supply: SupplyDoc) {
    viewModelScope.launch {
      try {
        val res = repository.addOrUpdateSupply(supply)
        res.fold(
          onSuccess = { _userMessage.value = "Insumo guardado" },
          onFailure = { _userMessage.value = "Error al guardar insumo: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  fun saveMenuItem(item: MenuItemDoc) {
    viewModelScope.launch {
      try {
        val res = repository.addOrUpdateMenuItem(item)
        res.fold(
          onSuccess = { _userMessage.value = "Plato guardado" },
          onFailure = { _userMessage.value = "Error al guardar plato: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  fun updateExchangeRate(newRate: Double) {
    viewModelScope.launch {
      try {
        val updated = config.value.copy(exchangeRateUsdToBs = newRate)
        val res = repository.saveConfig(updated)
        res.fold(
          onSuccess = { _userMessage.value = "Tasa de cambio actualizada a $newRate Bs/$" },
          onFailure = { _userMessage.value = "Error al actualizar tasa: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  fun saveConfig(newConfig: BusinessConfigDoc) {
    viewModelScope.launch {
      try {
        val res = repository.saveConfig(newConfig)
        res.fold(
          onSuccess = { _userMessage.value = "Configuración guardada" },
          onFailure = { _userMessage.value = "Error al guardar configuración: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  fun updateUserRole(userId: String, newRole: String) {
    viewModelScope.launch {
      try {
        val res = repository.updateUserRole(userId, newRole)
        res.fold(
          onSuccess = { _userMessage.value = "Rol de usuario actualizado a $newRole" },
          onFailure = { _userMessage.value = "Error al actualizar rol: ${it.localizedMessage}" }
        )
      } catch (e: Exception) {
        _userMessage.value = "Error: ${e.localizedMessage}"
      }
    }
  }

  // --- Diálogo de Tickets Térmicos ---

  fun showThermalTicket(title: String, ticketText: String) {
    _currentTicketTitle.value = title
    _currentTicketText.value = ticketText
    _showTicketDialog.value = true
  }

  fun closeTicketDialog() {
    _showTicketDialog.value = false
  }

  fun showOrderKitchenTicket(order: OrderDoc, paperWidth: PaperWidth = PaperWidth.WIDTH_58MM) {
    val items = parseCartItems(order.itemsJson)
    val ticket = ThermalTicketHelper.generateKitchenTicketText(order, items, paperWidth)
    showThermalTicket("Comanda Cocina #${order.orderNumber}", ticket)
  }

  fun showOrderReceiptTicket(order: OrderDoc, paperWidth: PaperWidth = PaperWidth.WIDTH_58MM) {
    val items = parseCartItems(order.itemsJson)
    val ticket = ThermalTicketHelper.generateOrderReceiptText(order, items, config.value, paperWidth)
    showThermalTicket("Ticket Pedido #${order.orderNumber}", ticket)
  }

  fun showClosureTicket(closure: ClosureDoc, paperWidth: PaperWidth = PaperWidth.WIDTH_58MM) {
    val ticket = ThermalTicketHelper.generateClosureReceiptText(closure, config.value, paperWidth)
    showThermalTicket("Ticket Cierre #${closure.closureNumber}", ticket)
  }

  /**
   * Toma Rápida de Pedido: Envía directo a cocina e imprime automáticamente la comanda térmica en 58mm o 80mm.
   * Deja la comanda abierta para ingresar el pago con el método utilizado posteriormente.
   */
  fun submitFastOrderToKitchen(
    context: Context,
    customerName: String = _customerName.value,
    tableOrDestination: String = _tableOrDestination.value,
    notes: String = "",
    paperWidth: PaperWidth = ThermalPrinterManager.getPaperWidth(context)
  ) {
    val currentCartItems = _cart.value
    if (currentCartItems.isEmpty()) {
      _userMessage.value = "Agregue al menos un plato a la comanda"
      return
    }

    viewModelScope.launch {
      try {
        val nextOrderNumber = (orders.value.maxOfOrNull { it.orderNumber } ?: 0) + 1
        val rate = config.value.exchangeRateUsdToBs
        val totalUsd = currentCartItems.sumOf { it.priceUsd * it.quantity }
        val totalBs = totalUsd * rate

        val order = OrderDoc(
          orderNumber = nextOrderNumber,
          customerName = customerName.ifBlank { "Cliente" },
          tableOrDestination = tableOrDestination.ifBlank { "Mesa 1" },
          timestamp = System.currentTimeMillis(),
          status = "EN_COCINA",
          totalUsd = totalUsd,
          exchangeRate = rate,
          totalBs = totalBs,
          paymentMethod = "PENDIENTE",
          isPaid = false,
          notes = notes,
          createdBy = _currentLoggedUser.value?.displayName ?: "Personal"
        )

        val newOrderId = repository.createOrderAndDeductStock(
          order = order,
          cartItems = currentCartItems,
          menuList = menuItems.value
        )

        // Limpiar comanda inmediatamente para atender al siguiente cliente al instante
        clearCart()
        _customerName.value = "Cliente"

        val savedOrder = order.copy(id = newOrderId)
        val kitchenTicketText = ThermalTicketHelper.generateKitchenTicketText(savedOrder, currentCartItems, paperWidth)

        // Impresión automática de la comanda de cocina en máquina térmica (58mm / 80mm)
        ThermalTicketHelper.printTicketViaPrintManager(
          context = context,
          title = "Comanda Cocina #${savedOrder.orderNumber}",
          ticketText = kitchenTicketText,
          paperWidth = paperWidth
        )

        showThermalTicket("Comanda Cocina #${savedOrder.orderNumber}", kitchenTicketText)
        _userMessage.value = "⚡ ¡Comanda #${savedOrder.orderNumber} enviada e impresa automáticamente en cocina!"
      } catch (e: Exception) {
        _userMessage.value = "Error al enviar comanda: ${e.localizedMessage}"
      }
    }
  }

  /**
   * Cobro Posterior de Comanda: Permite ingresar el pago con el método utilizado (o mixto)
   * y vuelve a imprimir el ticket con los montos y métodos de pago aplicados en 58mm o 80mm.
   * Evita expresamente volver a cobrar una cuenta ya pagada.
   */
  fun settlePaymentAndPrintReceipt(
    context: Context,
    order: OrderDoc,
    paymentMethod: String,
    usdPaid: Double,
    pagoMovilBs: Double,
    puntoVentaBs: Double,
    refCode: String,
    notes: String = "",
    paperWidth: PaperWidth = ThermalPrinterManager.getPaperWidth(context)
  ) {
    if (order.isPaid) {
      _userMessage.value = "⚠️ La orden #${order.orderNumber} ya fue pagada con ${order.paymentMethod}. No se puede volver a cobrar."
      return
    }

    val updatedOrder = order.copy(
      isPaid = true,
      paymentMethod = paymentMethod,
      amountUsdPaid = usdPaid,
      amountPagoMovilBs = pagoMovilBs,
      amountPuntoVentaBs = puntoVentaBs,
      referenceCode = refCode,
      notes = if (notes.isNotBlank()) "${order.notes} | $notes" else order.notes
    )

    // Actualización optimista instantánea: desaparece de "Por Cobrar" en el primer toque exacto
    _orders.update { list -> list.map { if (it.id == order.id) updatedOrder else it } }

    viewModelScope.launch {
      try {
        repository.markOrderAsPaid(
          orderId = order.id,
          paymentMethod = paymentMethod,
          amountUsd = usdPaid,
          amountPagoMovilBs = pagoMovilBs,
          amountPuntoVentaBs = puntoVentaBs,
          refCode = refCode
        )

        val items = parseCartItems(order.itemsJson)

        val receiptText = ThermalTicketHelper.generateOrderReceiptText(
          order = updatedOrder,
          items = items,
          config = config.value,
          paperWidth = paperWidth
        )

        // Impresión automática del comprobante de venta final para el cliente
        ThermalTicketHelper.printTicketViaPrintManager(
          context = context,
          title = "Factura Cliente #${updatedOrder.orderNumber}",
          ticketText = receiptText,
          paperWidth = paperWidth
        )

        showThermalTicket("Factura Cliente #${updatedOrder.orderNumber}", receiptText)
        _userMessage.value = "💳 ¡Pago de Orden #${updatedOrder.orderNumber} registrado y factura impresa!"
      } catch (e: Exception) {
        _userMessage.value = "Error al registrar pago: ${e.localizedMessage}"
      }
    }
  }

  /**
   * Cobro Rápido en Efectivo con un solo toque (marca como pagado al instante y emite recibo)
   */
  fun quickMarkOrderAsPaidCash(
    context: Context,
    order: OrderDoc,
    paperWidth: PaperWidth = ThermalPrinterManager.getPaperWidth(context)
  ) {
    if (order.isPaid) {
      _userMessage.value = "⚠️ La orden #${order.orderNumber} ya está pagada. No se puede volver a cobrar."
      return
    }
    settlePaymentAndPrintReceipt(
      context = context,
      order = order,
      paymentMethod = "USD_EFECTIVO",
      usdPaid = order.totalUsd,
      pagoMovilBs = 0.0,
      puntoVentaBs = 0.0,
      refCode = "EFECTIVO",
      notes = "Cobro Rápido en Efectivo",
      paperWidth = paperWidth
    )
  }

  /**
   * Re-impresión de Factura de Cuenta ya Pagada (sin alterar el estado ni volver a cobrar)
   */
  fun reprintPaidOrderReceipt(
    context: Context,
    order: OrderDoc,
    paperWidth: PaperWidth = ThermalPrinterManager.getPaperWidth(context)
  ) {
    val items = parseCartItems(order.itemsJson)
    val receiptText = ThermalTicketHelper.generateOrderReceiptText(
      order = order,
      items = items,
      config = config.value,
      paperWidth = paperWidth
    )
    ThermalTicketHelper.printTicketViaPrintManager(
      context = context,
      title = "Copia Factura #${order.orderNumber}",
      ticketText = receiptText,
      paperWidth = paperWidth
    )
    showThermalTicket("Copia Factura #${order.orderNumber}", receiptText)
    _userMessage.value = "🖨️ Re-imprimiendo factura de Orden #${order.orderNumber} (PAGADA)"
  }

  override fun onCleared() {
    super.onCleared()
    localWebServer.stop()
  }
}
