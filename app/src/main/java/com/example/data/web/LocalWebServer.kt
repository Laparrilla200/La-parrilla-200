package com.example.data.web

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.ClosureDoc
import com.example.data.model.MenuItemDoc
import com.example.data.model.OrderDoc
import com.example.data.model.SupplyDoc
import com.example.data.printer.ThermalTicketHelper
import com.example.data.repository.ParrillaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "LocalWebServer"

class LocalWebServer(
  private val repository: ParrillaRepository,
  private val port: Int = 8080
) {

  private var serverJob: Job? = null
  private var serverSocket: ServerSocket? = null
  var isRunning = false
    private set

  fun start(scope: CoroutineScope) {
    if (isRunning) return
    serverJob = scope.launch(Dispatchers.IO) {
      try {
        serverSocket = ServerSocket(port)
        isRunning = true
        Log.i(TAG, "Servidor Web para PC iniciado en puerto $port")

        while (isActive && !serverSocket!!.isClosed) {
          val client = serverSocket!!.accept()
          launch(Dispatchers.IO) {
            handleClient(client)
          }
        }
      } catch (e: Exception) {
        if (isActive) {
          Log.e(TAG, "Error en servidor web", e)
        }
      } finally {
        isRunning = false
      }
    }
  }

  fun stop() {
    isRunning = false
    try {
      serverSocket?.close()
      serverJob?.cancel()
    } catch (_: Exception) {}
  }

  fun getLocalIpAddress(): String {
    try {
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val networkInterface = interfaces.nextElement()
        if (networkInterface.isLoopback || !networkInterface.isUp) continue
        val addresses = networkInterface.inetAddresses
        while (addresses.hasMoreElements()) {
          val addr = addresses.nextElement()
          if (addr is Inet4Address && !addr.isLoopbackAddress) {
            val ip = addr.hostAddress ?: ""
            if (ip.startsWith("192.") || ip.startsWith("10.") || ip.startsWith("172.")) {
              return ip
            }
          }
        }
      }
    } catch (_: Exception) {}
    return "localhost"
  }

  fun getServerUrl(): String {
    return "http://${getLocalIpAddress()}:$port"
  }

  private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
    try {
      socket.soTimeout = 10000
      val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
      val writer = PrintWriter(socket.getOutputStream(), true)

      val requestLine = reader.readLine() ?: return@withContext
      val parts = requestLine.split(" ")
      if (parts.size < 2) return@withContext

      val method = parts[0]
      val path = parts[1]

      // Leer cabeceras
      var contentLength = 0
      var line = reader.readLine()
      while (!line.isNullOrEmpty()) {
        if (line.lowercase().startsWith("content-length:")) {
          contentLength = line.substring(15).trim().toIntOrNull() ?: 0
        }
        line = reader.readLine()
      }

      // Leer cuerpo si es POST
      val body = if (contentLength > 0) {
        val charArray = CharArray(contentLength)
        var totalRead = 0
        while (totalRead < contentLength) {
          val read = reader.read(charArray, totalRead, contentLength - totalRead)
          if (read == -1) break
          totalRead += read
        }
        String(charArray, 0, totalRead)
      } else ""

      when {
        method == "GET" && (path == "/" || path.startsWith("/index.html")) -> {
          val html = generateWebDashboardHtml()
          sendResponse(writer, 200, "text/html; charset=utf-8", html)
        }
        method == "GET" && path == "/api/data" -> {
          val json = getSystemDataJson()
          sendResponse(writer, 200, "application/json", json)
        }
        method == "POST" && path == "/api/order" -> {
          val result = handleCreateOrder(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/pay" -> {
          val result = handlePayOrder(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/order/status" -> {
          val result = handleUpdateOrderStatus(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/order/cancel" -> {
          val result = handleCancelOrder(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/order/delete" -> {
          val result = handleDeleteOrder(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/menu/save" -> {
          val result = handleSaveMenuItem(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/menu/delete" -> {
          val result = handleDeleteMenuItem(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/order/update" -> {
          val result = handleUpdateOpenOrder(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/restock" -> {
          val result = handleRestock(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/rate" -> {
          val result = handleUpdateRate(body)
          sendResponse(writer, 200, "application/json", result)
        }
        method == "POST" && path == "/api/closure" -> {
          val result = handleCreateClosure(body)
          sendResponse(writer, 200, "application/json", result)
        }
        else -> {
          sendResponse(writer, 404, "text/plain", "No encontrado")
        }
      }
    } catch (_: Exception) {
    } finally {
      try {
        socket.close()
      } catch (_: Exception) {}
    }
  }

  private fun sendResponse(writer: PrintWriter, status: Int, contentType: String, content: String) {
    val bytes = content.toByteArray(Charsets.UTF_8)
    writer.print("HTTP/1.1 $status OK\r\n")
    writer.print("Content-Type: $contentType\r\n")
    writer.print("Content-Length: ${bytes.size}\r\n")
    writer.print("Access-Control-Allow-Origin: *\r\n")
    writer.print("Connection: close\r\n\r\n")
    writer.print(content)
    writer.flush()
  }

  private suspend fun getSystemDataJson(): String {
    val config = repository.observeConfig().first()
    val supplies = repository.observeSupplies().first()
    val menu = repository.observeMenu().first()
    val orders = repository.observeOrders().first()
    val closures = repository.observeClosures().first()
    val users = repository.observeSystemUsers().first()

    val root = JSONObject()

    val cfgObj = JSONObject().apply {
      put("businessName", config.businessName)
      put("rif", config.rif)
      put("phone", config.phone)
      put("address", config.address)
      put("exchangeRateUsdToBs", config.exchangeRateUsdToBs)
      put("ticketHeader", config.ticketHeader)
      put("ticketFooter", config.ticketFooter)
    }
    root.put("config", cfgObj)

    val usrArr = JSONArray()
    for (u in users) {
      usrArr.put(JSONObject().apply {
        put("id", u.id)
        put("name", u.displayName)
        put("role", u.role)
        put("pin", u.password)
        put("canAccessPc", u.hasAccessPc())
        put("canUsePosPc", u.hasPosPc())
        put("canUseKitchenPc", u.hasKitchenPc())
        put("canUseBillingPc", u.hasBillingPc())
        put("canUseInventoryPc", u.hasInventoryPc())
        put("canUseReportsPc", u.hasReportsPc())
        put("canUsePosApp", u.hasPosApp())
        put("canUseKitchenApp", u.hasKitchenApp())
        put("canUseBillingApp", u.hasBillingApp())
        put("canUseInventoryApp", u.hasInventoryApp())
        put("canUseClosuresApp", u.hasClosuresApp())
        put("canUseMenuApp", u.hasMenuApp())
        put("canUseSettingsApp", u.hasSettingsApp())
      })
    }
    root.put("users", usrArr)

    val supArr = JSONArray()
    for (s in supplies) {
      supArr.put(JSONObject().apply {
        put("id", s.id)
        put("name", s.name)
        put("currentStock", s.currentStock)
        put("minStockThreshold", s.minStockThreshold)
        put("unit", s.unit)
        put("costUsd", s.costUsd)
        put("recommendedRestock", s.recommendedRestock)
      })
    }
    root.put("supplies", supArr)

    val menuArr = JSONArray()
    for (m in menu) {
      menuArr.put(JSONObject().apply {
        put("id", m.id)
        put("name", m.name)
        put("description", m.description)
        put("category", m.category)
        put("priceUsd", m.priceUsd)
        put("isAvailable", m.isAvailable)
        put("ingredientsJson", m.ingredientsJson)
        put("extrasJson", m.extrasJson)
      })
    }
    root.put("menu", menuArr)

    val ordArr = JSONArray()
    for (o in orders) {
      ordArr.put(JSONObject().apply {
        put("id", o.id)
        put("orderNumber", o.orderNumber)
        put("customerName", o.customerName)
        put("tableOrDestination", o.tableOrDestination)
        put("timestamp", o.timestamp)
        put("status", o.status)
        put("totalUsd", o.totalUsd)
        put("totalBs", o.totalBs)
        put("paymentMethod", o.paymentMethod)
        put("amountUsdPaid", o.amountUsdPaid)
        put("amountPagoMovilBs", o.amountPagoMovilBs)
        put("amountPuntoVentaBs", o.amountPuntoVentaBs)
        put("referenceCode", o.referenceCode)
        put("isPaid", o.isPaid)
        put("paidAtTimestamp", o.timestamp)
        put("notes", o.notes)
        put("itemsJson", o.itemsJson)
      })
    }
    root.put("orders", ordArr)

    val cloArr = JSONArray()
    for (c in closures) {
      cloArr.put(JSONObject().apply {
        put("id", c.id)
        put("closureType", c.closureType)
        put("closureNumber", c.closureNumber)
        put("timestamp", c.timestamp)
        put("totalSalesUsd", c.totalSalesUsd)
        put("totalSalesBs", c.totalSalesBs)
        put("totalUsdCash", c.totalUsdCash)
        put("totalPagoMovilBs", c.totalPagoMovilBs)
        put("totalPuntoVentaBs", c.totalPuntoVentaBs)
        put("orderCount", c.orderCount)
        put("closedBy", c.closedBy)
      })
    }
    root.put("closures", cloArr)

    return root.toString()
  }

  private suspend fun handleCreateOrder(body: String): String {
    return try {
      val json = JSONObject(body)
      val customerName = json.optString("customerName", "Cliente PC")
      val table = json.optString("tableOrDestination", "Mesa 1")
      val notes = json.optString("notes", "")

      val itemsJsonArray = json.getJSONArray("items")
      val cartItems = mutableListOf<CartOrderItem>()
      for (i in 0 until itemsJsonArray.length()) {
        val itemObj = itemsJsonArray.getJSONObject(i)
        val excludedArr = itemObj.optJSONArray("excludedIngredients") ?: JSONArray()
        val excludedList = mutableListOf<String>()
        for (j in 0 until excludedArr.length()) {
          excludedList.add(excludedArr.getString(j))
        }

        cartItems.add(
          CartOrderItem(
            menuItemId = itemObj.getString("menuItemId"),
            dishName = itemObj.getString("dishName"),
            quantity = itemObj.getInt("quantity"),
            priceUsd = itemObj.getDouble("priceUsd"),
            excludedIngredientNames = excludedList,
            specialInstructions = itemObj.optString("specialInstructions", "")
          )
        )
      }

      val config = repository.observeConfig().first()
      val menuList = repository.observeMenu().first()
      val orders = repository.observeOrders().first()
      val nextOrderNumber = (orders.maxOfOrNull { it.orderNumber } ?: 0) + 1

      val totalUsd = cartItems.sumOf { it.priceUsd * it.quantity }
      val totalBs = totalUsd * config.exchangeRateUsdToBs

      val order = OrderDoc(
        orderNumber = nextOrderNumber,
        customerName = customerName,
        tableOrDestination = table,
        timestamp = System.currentTimeMillis(),
        status = "EN_COCINA",
        totalUsd = totalUsd,
        exchangeRate = config.exchangeRateUsdToBs,
        totalBs = totalBs,
        paymentMethod = "PENDIENTE",
        isPaid = false,
        notes = notes,
        createdBy = "Portal Web PC"
      )

      val newOrderId = repository.createOrderAndDeductStock(order, cartItems, menuList)
      val kitchenTicketText = ThermalTicketHelper.generateKitchenTicketText(order.copy(id = newOrderId), cartItems)

      JSONObject().apply {
        put("success", true)
        put("orderId", newOrderId)
        put("orderNumber", nextOrderNumber)
        put("kitchenTicket", kitchenTicketText)
      }.toString()
    } catch (e: Exception) {
      JSONObject().apply {
        put("success", false)
        put("error", e.localizedMessage)
      }.toString()
    }
  }

  private suspend fun handlePayOrder(body: String): String {
    return try {
      val json = JSONObject(body)
      val orderId = json.getString("orderId")
      val method = json.getString("paymentMethod")
      val usdPaid = json.optDouble("amountUsdPaid", 0.0)
      val pmBs = json.optDouble("amountPagoMovilBs", 0.0)
      val pvBs = json.optDouble("amountPuntoVentaBs", 0.0)
      val ref = json.optString("referenceCode", "")

      val orders = repository.observeOrders().first()
      val existing = orders.firstOrNull { it.id == orderId }
      if (existing == null) {
        return JSONObject().apply { put("success", false); put("error", "Pedido no encontrado") }.toString()
      }
      if (existing.isPaid) {
        return JSONObject().apply {
          put("success", false)
          put("error", "Esta cuenta ya fue liquidada y pagada previamente. No se permite cobrar de nuevo.")
        }.toString()
      }

      repository.markOrderAsPaid(
        orderId = orderId,
        paymentMethod = method,
        amountUsd = usdPaid,
        amountPagoMovilBs = pmBs,
        amountPuntoVentaBs = pvBs,
        refCode = ref
      )

      val refreshedOrders = repository.observeOrders().first()
      val updatedOrder = refreshedOrders.firstOrNull { it.id == orderId }
      val config = repository.observeConfig().first()
      val items = if (updatedOrder != null) repository.parseCartItems(updatedOrder.itemsJson) else emptyList()

      val clientTicket = if (updatedOrder != null) {
        ThermalTicketHelper.generateOrderReceiptText(updatedOrder, items, config)
      } else ""

      JSONObject().apply {
        put("success", true)
        put("clientTicket", clientTicket)
      }.toString()
    } catch (e: Exception) {
      JSONObject().apply {
        put("success", false)
        put("error", e.localizedMessage)
      }.toString()
    }
  }

  private suspend fun handleUpdateOrderStatus(body: String): String {
    return try {
      val json = JSONObject(body)
      val orderId = json.getString("orderId")
      val newStatus = json.getString("newStatus")
      repository.updateOrderStatus(orderId, newStatus)
      JSONObject().apply { put("success", true) }.toString()
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleCancelOrder(body: String): String {
    return try {
      val json = JSONObject(body)
      val orderId = json.getString("orderId")
      val restoreStock = json.optBoolean("restoreStock", true)
      repository.cancelOrder(orderId, restoreStock)
      JSONObject().apply { put("success", true) }.toString()
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleDeleteOrder(body: String): String {
    return try {
      val json = JSONObject(body)
      val orderId = json.getString("orderId")
      val res = repository.deleteOrder(orderId)
      res.fold(
        onSuccess = { JSONObject().apply { put("success", true) }.toString() },
        onFailure = { JSONObject().apply { put("success", false); put("error", it.localizedMessage) }.toString() }
      )
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleSaveMenuItem(body: String): String {
    return try {
      val json = JSONObject(body)
      val id = json.optString("id", "")
      val name = json.getString("name").trim()
      val category = json.optString("category", "Parrillas")
      val priceUsd = json.getDouble("priceUsd")
      val description = json.optString("description", "")
      val isAvailable = json.optBoolean("isAvailable", true)
      val ingredientsJson = json.optString("ingredientsJson", "[]")
      val extrasJson = json.optString("extrasJson", "[]")

      val item = MenuItemDoc(
        id = id,
        name = name,
        category = category,
        priceUsd = priceUsd,
        description = description,
        isAvailable = isAvailable,
        ingredientsJson = ingredientsJson,
        extrasJson = extrasJson
      )
      val res = repository.addOrUpdateMenuItem(item)
      res.fold(
        onSuccess = { savedId -> JSONObject().apply { put("success", true); put("id", savedId) }.toString() },
        onFailure = { JSONObject().apply { put("success", false); put("error", it.localizedMessage) }.toString() }
      )
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleDeleteMenuItem(body: String): String {
    return try {
      val json = JSONObject(body)
      val itemId = json.getString("id")
      val res = repository.deleteMenuItem(itemId)
      res.fold(
        onSuccess = { JSONObject().apply { put("success", true) }.toString() },
        onFailure = { JSONObject().apply { put("success", false); put("error", it.localizedMessage) }.toString() }
      )
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleUpdateOpenOrder(body: String): String {
    return try {
      val json = JSONObject(body)
      val orderId = json.getString("orderId")
      val itemsArr = json.getJSONArray("items")
      val updatedItems = mutableListOf<CartOrderItem>()
      for (i in 0 until itemsArr.length()) {
        val o = itemsArr.getJSONObject(i)
        val exArr = o.optJSONArray("excludedIngredients") ?: JSONArray()
        val exList = mutableListOf<String>()
        for (j in 0 until exArr.length()) exList.add(exArr.getString(j))
        updatedItems.add(
          CartOrderItem(
            menuItemId = o.optString("menuItemId", ""),
            dishName = o.getString("dishName"),
            quantity = o.getInt("quantity"),
            priceUsd = o.getDouble("priceUsd"),
            excludedIngredientNames = exList,
            specialInstructions = o.optString("specialInstructions", "")
          )
        )
      }

      val config = repository.observeConfig().first()
      val menuList = repository.observeMenu().first()
      val totalUsd = updatedItems.sumOf { it.priceUsd * it.quantity }
      val totalBs = totalUsd * config.exchangeRateUsdToBs

      // Nuevos ítems opcionales a descontar
      val newlyArr = json.optJSONArray("newlyAddedItems") ?: JSONArray()
      val newlyItems = mutableListOf<CartOrderItem>()
      for (i in 0 until newlyArr.length()) {
        val o = newlyArr.getJSONObject(i)
        newlyItems.add(
          CartOrderItem(
            menuItemId = o.optString("menuItemId", ""),
            dishName = o.getString("dishName"),
            quantity = o.getInt("quantity"),
            priceUsd = o.getDouble("priceUsd")
          )
        )
      }

      val notes = if (json.has("notes")) json.getString("notes") else null

      repository.updateOpenOrder(
        orderId = orderId,
        updatedItems = updatedItems,
        newTotalUsd = totalUsd,
        newTotalBs = totalBs,
        newlyAddedItems = newlyItems,
        menuList = menuList,
        notes = notes
      )

      JSONObject().apply {
        put("success", true)
        put("totalUsd", totalUsd)
        put("totalBs", totalBs)
      }.toString()
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleRestock(body: String): String {
    return try {
      val json = JSONObject(body)
      val supplyId = json.getString("supplyId")
      val amount = json.getDouble("amount")
      repository.restockSupply(supplyId, amount)
      JSONObject().apply { put("success", true) }.toString()
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleUpdateRate(body: String): String {
    return try {
      val json = JSONObject(body)
      val rate = json.getDouble("rate")
      val config = repository.observeConfig().first()
      repository.saveConfig(config.copy(exchangeRateUsdToBs = rate))
      JSONObject().apply { put("success", true) }.toString()
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  private suspend fun handleCreateClosure(body: String): String {
    return try {
      val json = JSONObject(body)
      val type = json.optString("type", "DIARIO")
      val notes = json.optString("notes", "")

      val orders = repository.observeOrders().first()
      val closures = repository.observeClosures().first()
      val config = repository.observeConfig().first()

      val now = System.currentTimeMillis()
      val relevantOrders = orders.filter { it.status != "CANCELADO" }

      val totalSalesUsd = relevantOrders.sumOf { it.totalUsd }
      val totalSalesBs = relevantOrders.sumOf { it.totalBs }
      val totalUsdCash = relevantOrders.sumOf { it.amountUsdPaid }
      val totalPagoMovilBs = relevantOrders.sumOf { it.amountPagoMovilBs }
      val totalPuntoVentaBs = relevantOrders.sumOf { it.amountPuntoVentaBs }
      val nextClosureNumber = (closures.maxOfOrNull { it.closureNumber } ?: 0) + 1

      val closure = ClosureDoc(
        closureType = type,
        closureNumber = nextClosureNumber,
        timestamp = now,
        startDate = now - 86400000L,
        endDate = now,
        totalSalesUsd = totalSalesUsd,
        totalSalesBs = totalSalesBs,
        totalUsdCash = totalUsdCash,
        totalPagoMovilBs = totalPagoMovilBs,
        totalPuntoVentaBs = totalPuntoVentaBs,
        orderCount = relevantOrders.size,
        closedBy = "Portal Web PC",
        notes = notes
      )

      val closureId = repository.saveCashClosure(closure)
      val ticketText = ThermalTicketHelper.generateClosureReceiptText(closure.copy(id = closureId), config)

      JSONObject().apply {
        put("success", true)
        put("closureId", closureId)
        put("ticket", ticketText)
      }.toString()
    } catch (e: Exception) {
      JSONObject().apply { put("success", false); put("error", e.localizedMessage) }.toString()
    }
  }

  /**
   * Genera el Portal Web completo para PC y computadoras conectadas a la red local o navegador
   */
  fun generateWebDashboardHtml(): String {
    return """
      <!DOCTYPE html>
      <html lang="es">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>La Parrilla 200 - Portal de Gestión para Computadora</title>
        <style>
          :root {
            --bg-color: #121212;
            --surface-color: #1E1E22;
            --card-color: #26262B;
            --primary-red: #D32F2F;
            --fire-red: #E53935;
            --text-white: #FFFFFF;
            --text-muted: #AAAAAA;
            --amber-accent: #FFA000;
            --green-success: #2E7D32;
            --border-color: #38383E;
          }
          * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
          body { background: var(--bg-color); color: var(--text-white); display: flex; flex-direction: column; min-height: 100vh; }
          header { background: var(--surface-color); border-bottom: 2px solid var(--primary-red); padding: 14px 24px; display: flex; justify-content: space-between; align-items: center; }
          .brand { display: flex; align-items: center; gap: 12px; }
          .logo-badge { background: var(--primary-red); color: white; width: 42px; height: 42px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 20px; }
          .brand h1 { font-size: 20px; font-weight: 900; letter-spacing: 0.5px; }
          .brand p { font-size: 12px; color: var(--amber-accent); font-weight: bold; }
          .header-actions { display: flex; align-items: center; gap: 16px; }
          .status-chip { background: #1B5E20; color: #E8F5E9; padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; display: flex; align-items: center; gap: 6px; }
          .status-dot { width: 8px; height: 8px; background: #66BB6A; border-radius: 50%; box-shadow: 0 0 6px #66BB6A; }
          
          nav { background: var(--card-color); display: flex; border-bottom: 1px solid var(--border-color); padding: 0 24px; }
          nav button { background: none; border: none; color: var(--text-muted); padding: 14px 20px; font-size: 14px; font-weight: bold; cursor: pointer; border-bottom: 3px solid transparent; transition: all 0.2s; }
          nav button.active { color: var(--text-white); border-bottom-color: var(--primary-red); background: rgba(211,47,47,0.1); }
          nav button:hover { color: var(--text-white); }

          main { padding: 20px 24px; flex: 1; max-width: 1400px; width: 100%; margin: 0 auto; }
          .tab-content { display: none; }
          .tab-content.active { display: block; }

          /* POS RÁPIDO PARA PC */
          .pos-layout { display: grid; grid-template-columns: 1fr 420px; gap: 20px; }
          .menu-section { display: flex; flex-direction: column; gap: 16px; }
          .category-bar { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 6px; }
          .cat-btn { background: var(--card-color); border: 1px solid var(--border-color); color: var(--text-muted); padding: 8px 16px; border-radius: 20px; cursor: pointer; font-size: 13px; font-weight: bold; white-space: nowrap; }
          .cat-btn.active { background: var(--primary-red); color: white; border-color: var(--primary-red); }
          .dishes-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
          .dish-card { background: var(--surface-color); border: 1px solid var(--border-color); border-radius: 12px; padding: 14px; cursor: pointer; transition: transform 0.15s, border-color 0.15s; }
          .dish-card:hover { transform: translateY(-2px); border-color: var(--primary-red); }
          .dish-card h3 { font-size: 15px; margin-bottom: 4px; }
          .dish-card p { font-size: 12px; color: var(--text-muted); margin-bottom: 10px; height: 32px; overflow: hidden; }
          .dish-price { display: flex; justify-content: space-between; align-items: center; }
          .price-usd { font-size: 18px; font-weight: 900; color: var(--fire-red); }
          .price-bs { font-size: 12px; color: var(--amber-accent); font-weight: bold; }

          /* CARRITO PC */
          .cart-card { background: var(--surface-color); border: 1px solid var(--border-color); border-radius: 12px; padding: 18px; display: flex; flex-direction: column; gap: 14px; position: sticky; top: 20px; }
          .cart-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-color); padding-bottom: 10px; }
          .input-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
          .form-input { background: #121214; border: 1px solid var(--border-color); color: white; padding: 10px 12px; border-radius: 8px; font-size: 13px; width: 100%; }
          .cart-items { max-height: 260px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; border-bottom: 1px solid var(--border-color); padding-bottom: 10px; }
          .cart-item { display: flex; justify-content: space-between; align-items: center; background: var(--card-color); padding: 8px 12px; border-radius: 8px; font-size: 13px; }
          .cart-totals { display: flex; flex-direction: column; gap: 6px; }
          .total-row { display: flex; justify-content: space-between; font-size: 14px; }
          .total-big { font-size: 22px; font-weight: 900; color: white; }

          /* BOTÓN ACCION RÁPIDA */
          .btn-primary { background: var(--primary-red); color: white; border: none; padding: 14px; border-radius: 8px; font-size: 15px; font-weight: bold; cursor: pointer; width: 100%; transition: background 0.2s; display: flex; align-items: center; justify-content: center; gap: 8px; }
          .btn-primary:hover { background: #B71C1C; }
          .btn-secondary { background: var(--card-color); color: white; border: 1px solid var(--border-color); padding: 12px; border-radius: 8px; font-size: 14px; font-weight: bold; cursor: pointer; width: 100%; }
          .btn-secondary:hover { background: #38383E; }

          /* KITCHEN KDS */
          .kds-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
          .kds-card { background: var(--surface-color); border: 2px solid var(--border-color); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
          .kds-card.urgent { border-color: var(--primary-red); }
          .kds-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-color); padding-bottom: 8px; }
          .order-badge { background: var(--primary-red); color: white; padding: 4px 10px; border-radius: 6px; font-weight: 900; font-size: 16px; }
          .kds-items { display: flex; flex-direction: column; gap: 8px; }
          .kds-item-row { font-size: 15px; font-weight: bold; }
          .kds-warning { color: #FF8A80; font-size: 12px; font-weight: bold; margin-left: 20px; }

          /* TABLAS Y REPORTES */
          table { width: 100%; border-collapse: collapse; background: var(--surface-color); border-radius: 8px; overflow: hidden; margin-top: 12px; }
          th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--border-color); font-size: 13px; }
          th { background: var(--card-color); color: var(--text-muted); font-size: 12px; text-transform: uppercase; }
          .badge-critical { background: rgba(211,47,47,0.2); color: #FF8A80; padding: 3px 8px; border-radius: 4px; font-weight: bold; font-size: 11px; }

          /* MODALES Y TICKET PREVIEW */
          .modal-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.8); z-index: 999; justify-content: center; align-items: center; }
          .modal-overlay.open { display: flex; }
          .modal-box { background: var(--surface-color); border: 1px solid var(--border-color); border-radius: 14px; width: 90%; max-width: 540px; padding: 20px; }
          .thermal-ticket-view { background: #FFFFFF; color: #000000; font-family: 'Courier New', Courier, monospace; font-size: 13px; line-height: 1.25; padding: 14px; border-radius: 6px; white-space: pre-wrap; max-height: 380px; overflow-y: auto; border: 1px solid #CCC; margin: 12px 0; }

          /* REGLAS OBLIGATORIAS PARA IMPRESIÓN PURA: CERO BORDES, CERO MARCOS, CERO BARRAS DE SCROLL */
          @page {
            size: auto;
            margin: 0mm !important;
          }

          @media print {
            * {
              box-sizing: border-box !important;
              -webkit-print-color-adjust: exact !important;
              print-color-adjust: exact !important;
            }
            html, body {
              background: #FFFFFF !important;
              color: #000000 !important;
              margin: 0 !important;
              padding: 0 !important;
              width: 100% !important;
              height: auto !important;
            }
            /* Ocultar elementos de navegación y cabecera */
            header, nav, main, .no-print, button, input, select, .status-chip {
              display: none !important;
            }
            /* Desarmar la ventana modal: quitar marco, fondo oscuro y sombra */
            .modal-overlay {
              position: static !important;
              background: transparent !important;
              display: block !important;
              padding: 0 !important;
              margin: 0 !important;
              border: none !important;
              width: 100% !important;
              height: auto !important;
              inset: auto !important;
            }
            .modal-box {
              background: transparent !important;
              border: none !important;
              outline: none !important;
              box-shadow: none !important;
              border-radius: 0 !important;
              padding: 0 !important;
              margin: 0 !important;
              width: 100% !important;
              max-width: 100% !important;
            }
            /* Ocultar título del modal y botones en la impresión */
            #ticketModalTitle, .modal-box h3, .modal-actions, .modal-box button, .modal-box > div:last-child {
              display: none !important;
            }
            /* Vista del ticket sin marco, sin bordes y sin barras de desplazamiento */
            .thermal-ticket-view {
              background: transparent !important;
              border: none !important;
              outline: none !important;
              box-shadow: none !important;
              border-radius: 0 !important;
              padding: 2mm 0 !important;
              margin: 0 !important;
              width: 100% !important;
              max-height: none !important;
              height: auto !important;
              overflow: visible !important;
              white-space: pre-wrap !important;
              font-family: 'Courier New', Courier, monospace !important;
              font-size: 11pt !important;
              font-weight: 900 !important;
              line-height: 1.25 !important;
              color: #000000 !important;
              scrollbar-width: none !important;
            }
            .thermal-ticket-view::-webkit-scrollbar {
              display: none !important;
            }
          }
        </style>
      </head>
      <body>
        <header>
          <div class="brand">
            <div class="logo-badge">🔥</div>
            <div>
              <h1 id="headerBizName">LA PARRILLA 200</h1>
              <p id="headerRate">Tasa: 65.00 Bs/$ • Control en Computadora</p>
            </div>
          </div>
          <div class="header-actions">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 12px; color: #AAA; font-weight: bold;">👤 Perfil PC:</span>
              <select id="pcUserSelect" class="form-input" style="width: auto; padding: 6px 12px; font-weight: bold; background: #26262B;" onchange="onPcUserSelected()">
              </select>
            </div>
            <div class="status-chip">
              <span class="status-dot"></span>
              <span id="pcUserRoleBadge">ENLACE ACTIVO</span>
            </div>
            <button class="btn-secondary" style="width: auto; padding: 8px 14px;" onclick="loadData()">🔄 Actualizar</button>
          </div>
        </header>

        <!-- Banner de Acceso Restringido por Perfil -->
        <div id="pcAccessDeniedBanner" style="display: none; background: #B71C1C; color: white; padding: 22px; text-align: center; border-radius: 12px; margin: 20px 24px; font-size: 15px; font-weight: bold;">
          ⛔ ACCESO DENEGADO EN COMPUTADORA<br>
          <span style="font-size: 13px; font-weight: normal; opacity: 0.9;" id="pcAccessDeniedReason">El Administrador ha restringido el acceso a la Computadora para este perfil.</span>
        </div>

        <nav id="mainNav">
          <button id="navBtnPos" class="active" onclick="switchTab('posTab', this)">🛒 Toma Rápida de Pedidos</button>
          <button id="navBtnKitchen" onclick="switchTab('kitchenTab', this)">🍳 Monitor de Cocina (KDS)</button>
          <button id="navBtnBilling" onclick="switchTab('openOrdersTab', this)">💳 Cuentas (<span id="unpaidCount">0</span>)</button>
          <button id="navBtnMenu" onclick="switchTab('menuTab', this)">🍔 Menú y Recetas</button>
          <button id="navBtnInventory" onclick="switchTab('inventoryTab', this)">📦 Inventario e Insumos</button>
          <button id="navBtnReports" onclick="switchTab('reportsTab', this)">📊 Cierres & Finanzas</button>
        </nav>

        <main id="mainContainer">
          <!-- 1. POS RÁPIDO -->
          <div id="posTab" class="tab-content active">
            <div class="pos-layout">
              <div class="menu-section">
                <div class="category-bar" id="categoryBar">
                  <button class="cat-btn active" onclick="filterCategory('Todas', this)">Todas</button>
                  <button class="cat-btn" onclick="filterCategory('Parrillas', this)">Parrillas</button>
                  <button class="cat-btn" onclick="filterCategory('Hamburguesas & Pepitos', this)">Hamburguesas & Pepitos</button>
                  <button class="cat-btn" onclick="filterCategory('Entradas & Guarniciones', this)">Entradas & Guarniciones</button>
                  <button class="cat-btn" onclick="filterCategory('Bebidas', this)">Bebidas</button>
                </div>
                <div class="dishes-grid" id="dishesGrid">Cargando menú...</div>
              </div>

              <!-- Carrito Rápido -->
              <div class="cart-card">
                <div class="cart-header">
                  <h3>Comanda Actual</h3>
                  <button class="btn-secondary" style="width: auto; padding: 4px 8px; font-size: 11px;" onclick="clearCart()">Vaciar</button>
                </div>
                <div class="input-row">
                  <input type="text" id="custName" class="form-input" placeholder="Cliente (ej. Carlos)" value="Cliente">
                  <select id="tableDest" class="form-input">
                    <option value="Mesa 1">Mesa 1</option>
                    <option value="Mesa 2">Mesa 2</option>
                    <option value="Mesa 3">Mesa 3</option>
                    <option value="Mesa 4">Mesa 4</option>
                    <option value="Para Llevar">Para Llevar</option>
                    <option value="Delivery">Delivery</option>
                    <option value="Barra">Barra</option>
                  </select>
                </div>

                <div style="margin: 8px 0;">
                  <label style="font-size: 11px; color: var(--amber-accent); font-weight: bold;">📝 Instrucción Especial para Cocina:</label>
                  <input type="text" id="orderNotes" class="form-input" style="margin-top: 4px;" placeholder="Ej: Carne 3/4, sin cebolla, traer salsas aparte...">
                </div>

                <div class="cart-items" id="cartItemsList">
                  <div style="color: #888; text-align: center; padding: 20px;">Toca un plato para agregarlo al pedido</div>
                </div>

                <div class="cart-totals">
                  <div class="total-row">
                    <span style="color: #AAA;">Total USD:</span>
                    <span class="total-big" id="cartTotalUsd">$0.00</span>
                  </div>
                  <div class="total-row">
                    <span style="color: #AAA;">Total Bolívares:</span>
                    <span style="font-size: 16px; font-weight: bold; color: var(--amber-accent);" id="cartTotalBs">0.00 Bs</span>
                  </div>
                </div>

                <!-- Botón de Envío Rápido a Cocina con Comanda Automática -->
                <button class="btn-primary" onclick="submitOrderFast()">
                  ⚡ ENVIAR A COCINA (COMANDA AUTOMÁTICA)
                </button>
                <button class="btn-secondary" onclick="openDirectCheckoutModal()">
                  💳 Cobro Inmediato en Caja
                </button>
              </div>
            </div>
          </div>

          <!-- 2. COCINA KDS -->
          <div id="kitchenTab" class="tab-content">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px;">
              <h2>Monitor de Cocina La Parrilla 200</h2>
              <div style="display: flex; gap: 8px; align-items: center;">
                <span style="font-size: 12px; color: #AAA; font-weight: bold;">Vista de Pantalla:</span>
                <button id="kdsModeNormalBtn" class="btn-primary" style="width: auto; padding: 6px 12px; font-size: 12px; background: #D32F2F;" onclick="setKitchenDisplayMode('NORMAL')">
                  📱 Tarjetas
                </button>
                <button id="kdsModeTabletBtn" class="btn-secondary" style="width: auto; padding: 6px 12px; font-size: 12px;" onclick="setKitchenDisplayMode('TABLET')">
                  📟 Modo Tablet / Pantalla Cocina
                </button>
              </div>
            </div>
            <div class="kds-grid" id="kdsGrid">Cargando comandas activas...</div>
          </div>

          <!-- 3. CUENTAS: PENDIENTES VS PAGADAS -->
          <div id="openOrdersTab" class="tab-content">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px;">
              <h2>Gestión de Cuentas: Por Cobrar vs Pagadas</h2>
              <div style="display: flex; gap: 8px;">
                <button id="subTabPendingBtn" class="btn-primary" style="width: auto; padding: 8px 16px; font-size: 13px; background: #D32F2F;" onclick="switchOpenOrdersSubTab('PENDING')">
                  ⏳ Cuentas por Cobrar (<span id="subTabUnpaidCount">0</span>)
                </button>
                <button id="subTabPaidBtn" class="btn-secondary" style="width: auto; padding: 8px 16px; font-size: 13px;" onclick="switchOpenOrdersSubTab('PAID')">
                  ✅ Cuentas Pagadas (<span id="subTabPaidCount">0</span>)
                </button>
              </div>
            </div>
            <div id="openOrdersContainer">Cargando cuentas...</div>
          </div>

          <!-- 4. INVENTARIO -->
          <div id="inventoryTab" class="tab-content">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
              <h2>Control de Inventario e Insumos en Vivo</h2>
              <div id="critAlertBadge"></div>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Insumo</th>
                  <th>Stock Actual</th>
                  <th>Punto Crítico</th>
                  <th>Costo USD</th>
                  <th>Estado</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody id="suppliesTableBody"></tbody>
            </table>
          </div>

          <!-- 5. REPORTES Y CIERRES -->
          <div id="reportsTab" class="tab-content">
            <h2 style="margin-bottom: 14px;">Cierres de Caja y Auditoría Financiera</h2>
            <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px;">
              <div style="background: var(--surface-color); padding: 16px; border-radius: 10px; border-left: 4px solid var(--primary-red);">
                <div style="color: #AAA; font-size: 12px;">Total Dólares Hoy</div>
                <div style="font-size: 24px; font-weight: 900;" id="kpiUsd">$0.00</div>
              </div>
              <div style="background: var(--surface-color); padding: 16px; border-radius: 10px; border-left: 4px solid #64B5F6;">
                <div style="color: #AAA; font-size: 12px;">Pago Móvil (Bs)</div>
                <div style="font-size: 22px; font-weight: 900; color: #64B5F6;" id="kpiPm">0.00 Bs</div>
              </div>
              <div style="background: var(--surface-color); padding: 16px; border-radius: 10px; border-left: 4px solid #FFB74D;">
                <div style="color: #AAA; font-size: 12px;">Punto de Venta (Bs)</div>
                <div style="font-size: 22px; font-weight: 900; color: #FFB74D;" id="kpiPv">0.00 Bs</div>
              </div>
              <div style="background: var(--surface-color); padding: 16px; border-radius: 10px; border-left: 4px solid var(--green-success);">
                <div style="color: #AAA; font-size: 12px;">Efectivo USD ($)</div>
                <div style="font-size: 22px; font-weight: 900; color: #81C784;" id="kpiCash">$0.00</div>
              </div>
            </div>

            <div style="display: flex; gap: 10px; margin-bottom: 20px;">
              <button class="btn-primary" style="width: auto; padding: 12px 20px;" onclick="generateClosure('DIARIO')">🔒 Generar Cierre Diario</button>
              <button class="btn-secondary" style="width: auto; padding: 12px 20px;" onclick="generateClosure('SEMANAL')">Cierre Semanal</button>
              <button class="btn-secondary" style="width: auto; padding: 12px 20px;" onclick="generateClosure('MENSUAL')">Cierre Mensual</button>
            </div>

            <h3 style="margin: 16px 0 8px;">Historial de Cierres Emitidos</h3>
            <div id="closuresHistory"></div>
          </div>

          <!-- 6. GESTOR DE MENÚ Y RECETAS (FÁCIL Y VISUAL) -->
          <div id="menuTab" class="tab-content">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; flex-wrap: wrap; gap: 10px;">
              <div>
                <h2>🍔 Catálogo del Menú, Recetas y Extras</h2>
                <p style="color: #AAA; font-size: 13px; margin-top: 4px;">Modifica precios, agrega platos nuevos, define los insumos a descontar del inventario y los extras opcionales.</p>
              </div>
              <button class="btn-primary" style="width: auto; padding: 10px 20px; font-size: 14px; background: #2E7D32;" onclick="openCreateDishModal()">
                ➕ Crear Nuevo Plato
              </button>
            </div>

            <!-- Filtros por Categoría del Menú -->
            <div style="display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap;" id="menuCategoriesBar">
              <!-- Categorías dinámicas -->
            </div>

            <!-- Tabla de Platos del Menú -->
            <table>
              <thead>
                <tr>
                  <th>Plato</th>
                  <th>Categoría</th>
                  <th>Precio USD</th>
                  <th>Precio Bs</th>
                  <th>Insumos que descuenta (Receta)</th>
                  <th>Extras Disponibles</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody id="menuTableBody">
                <tr><td colspan="8" style="text-align: center; color: #AAA; padding: 20px;">Cargando catálogo del menú...</td></tr>
              </tbody>
            </table>
          </div>
        </main>

        <!-- Contenedor flotante de notificaciones de cocina para PC -->
        <div id="readyToastContainer" style="position: fixed; top: 16px; right: 16px; z-index: 10000; display: flex; flex-direction: column; gap: 10px; max-width: 380px;"></div>

        <!-- Modal de Impresión Rápida para PC (Hoja Completa o Ticket Térmico) -->
        <div id="ticketModal" class="modal-overlay">
          <div class="modal-box">
            <h3 id="ticketModalTitle">Vista Previa de Impresión</h3>
            
            <div style="display: flex; gap: 8px; align-items: center; margin: 10px 0 6px;">
              <span style="font-size: 12px; color: #AAA; font-weight: bold;">Tamaño de Letra:</span>
              <button id="fontBtnNormal" class="btn-secondary" style="width: auto; padding: 4px 10px; font-size: 11px;" onclick="setThermalFontSize('11.5pt', this)">Normal</button>
              <button id="fontBtnGrande" class="btn-secondary" style="width: auto; padding: 4px 10px; font-size: 11px; background: var(--primary-red); border-color: var(--primary-red);" onclick="setThermalFontSize('14pt', this)">Grande (Recomendado)</button>
              <button id="fontBtnExtra" class="btn-secondary" style="width: auto; padding: 4px 10px; font-size: 11px;" onclick="setThermalFontSize('16.5pt', this)">Extra Grande</button>
            </div>

            <div class="thermal-ticket-view" id="ticketContent"></div>
            <div style="display: flex; flex-direction: column; gap: 10px;">
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                <button class="btn-primary" style="background: #1976D2;" onclick="printCurrentDocument('SHEET')">
                  📄 Imprimir Hoja (Carta / A4)
                </button>
                <button class="btn-primary" style="background: #D32F2F;" onclick="printCurrentDocument('THERMAL')">
                  🧾 Imprimir Ticket Térmico
                </button>
              </div>
              <button class="btn-secondary" onclick="closeTicketModal()">Cerrar</button>
            </div>
          </div>
        </div>

        <!-- Modal de Cobro Multimoneda -->
        <div id="checkoutModal" class="modal-overlay">
          <div class="modal-box">
            <h3 style="margin-bottom: 12px;">Cobrar Pedido</h3>
            <div style="margin-bottom: 10px; color: var(--amber-accent);" id="checkoutOrderSummary"></div>
            <div style="display: flex; flex-direction: column; gap: 10px;">
              <label style="font-size: 12px; color: #AAA;">Método de Pago:</label>
              <select id="payMethodSelect" class="form-input" onchange="updatePayFields()">
                <option value="USD_EFECTIVO">💵 Efectivo Dólares ($)</option>
                <option value="PAGO_MOVIL">📱 Pago Móvil (Bs)</option>
                <option value="PUNTO_VENTA">💳 Punto de Venta (Bs)</option>
                <option value="MIXTO">🔄 Pago Mixto ($ / Bs)</option>
              </select>
              
              <div id="payFieldsArea"></div>

              <button class="btn-primary" onclick="confirmPaymentAndPrint()">
                💳 Confirmar Pago e Imprimir Factura (48mm)
              </button>
              <button class="btn-secondary" onclick="closeCheckoutModal()">Cancelar</button>
            </div>
          </div>
        </div>

        <!-- Modal para Modificar Pedido o Colocar Instrucción desde PC -->
        <div id="editOrderModal" class="modal-overlay">
          <div class="modal-box" style="max-width: 580px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <h3 id="editOrderModalTitle" style="color: white; font-weight: 900;">Modificar Pedido</h3>
              <button class="btn-secondary" style="width: auto; padding: 4px 10px;" onclick="closeEditOrderModal()">✕</button>
            </div>
            <p id="editOrderSubtitle" style="font-size: 12px; color: var(--amber-accent); font-weight: bold; margin-bottom: 12px;"></p>

            <div style="margin-bottom: 12px;">
              <label style="font-size: 12px; color: #AAA; font-weight: bold;">📝 Instrucción o Nota Especial para Cocina:</label>
              <input type="text" id="editOrderNotesInput" class="form-input" style="margin-top: 4px;" placeholder="Ej: Carne término 3/4, sin cebolla, salsas aparte...">
            </div>

            <div style="font-size: 12px; color: #AAA; font-weight: bold; margin-bottom: 6px;">Platos y Bebidas del Pedido:</div>
            <div id="editOrderItemsList" style="max-height: 180px; overflow-y: auto; margin-bottom: 12px; display: flex; flex-direction: column; gap: 6px;"></div>

            <div style="font-size: 12px; color: var(--amber-accent); font-weight: bold; margin-bottom: 6px;">+ Agregar Bebida o Plato Adicional:</div>
            <div style="display: flex; gap: 8px; margin-bottom: 16px;">
              <select id="editAddDishSelect" class="form-input" style="flex: 1;"></select>
              <button class="btn-secondary" style="width: auto; padding: 0 16px; background: #2E7D32;" onclick="addItemToEditingOrder()">+ Añadir</button>
            </div>

            <div style="display: flex; gap: 10px;">
              <button class="btn-primary" style="flex: 1;" onclick="saveEditedOrder()">
                💾 Guardar Cambios y Notificar a Cocina
              </button>
              <button class="btn-secondary" style="width: auto; padding: 0 16px;" onclick="closeEditOrderModal()">
                Volver
              </button>
            </div>
          </div>
        </div>

        <!-- Modal Intuitivo para Crear/Editar Plato del Menú y sus Insumos/Extras -->
        <div id="dishModal" class="modal-overlay">
          <div class="modal-box" style="max-width: 650px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
              <h3 id="dishModalTitle">Editar Plato del Menú</h3>
              <button class="btn-secondary" style="width: auto; padding: 4px 10px;" onclick="closeDishModal()">✖ Cerrar</button>
            </div>

            <div style="max-height: 68vh; overflow-y: auto; padding-right: 6px;">
              <!-- 1. Datos Básicos del Plato -->
              <div style="background: #25252D; padding: 14px; border-radius: 8px; margin-bottom: 14px;">
                <h4 style="color: #FF5252; margin-bottom: 10px; font-size: 13.5px;">1. Datos Básicos del Plato</h4>
                <div class="input-row" style="margin-bottom: 8px;">
                  <div style="flex: 2;">
                    <label style="font-size: 11px; color: #AAA; font-weight: bold;">Nombre del Plato:</label>
                    <input type="text" id="dishNameInput" class="form-input" placeholder="Ej: Hamburguesa Parrillera Doble">
                  </div>
                  <div style="flex: 1.2;">
                    <label style="font-size: 11px; color: #AAA; font-weight: bold;">Categoría:</label>
                    <select id="dishCategoryInput" class="form-input">
                      <option value="Parrillas">Parrillas</option>
                      <option value="Hamburguesas & Pepitos">Hamburguesas & Pepitos</option>
                      <option value="Entradas & Guarniciones">Entradas & Guarniciones</option>
                      <option value="Bebidas">Bebidas</option>
                      <option value="Postres">Postres</option>
                    </select>
                  </div>
                </div>

                <div class="input-row">
                  <div style="flex: 1;">
                    <label style="font-size: 11px; color: #AAA; font-weight: bold;">Precio en Dólares ($ USD):</label>
                    <input type="number" step="0.5" id="dishPriceInput" class="form-input" placeholder="Ej: 8.50" oninput="updateDishBsPreview()">
                  </div>
                  <div style="flex: 1;">
                    <label style="font-size: 11px; color: #AAA; font-weight: bold;">Equivalente en Bolívares (Bs):</label>
                    <input type="text" id="dishPriceBsPreview" class="form-input" style="background: #1C1C22; color: #FFB300; font-weight: bold;" readonly value="0.00 Bs">
                  </div>
                </div>

                <div style="margin-top: 8px;">
                  <label style="font-size: 11px; color: #AAA; font-weight: bold;">Descripción o ingredientes principales (visible al vender):</label>
                  <input type="text" id="dishDescInput" class="form-input" placeholder="Ej: Doble carne 150g, queso cheddar, tocineta, papas fritas...">
                </div>

                <div style="margin-top: 8px; display: flex; align-items: center; gap: 8px;">
                  <input type="checkbox" id="dishAvailableInput" checked style="width: 18px; height: 18px; cursor: pointer;">
                  <label for="dishAvailableInput" style="font-size: 13px; color: #EEE; cursor: pointer;">Plato activo y disponible para la venta</label>
                </div>
              </div>

              <!-- 2. Insumos que utiliza (Receta para descontar automáticamente de Inventario) -->
              <div style="background: #25252D; padding: 14px; border-radius: 8px; margin-bottom: 14px;">
                <h4 style="color: #4CAF50; margin-bottom: 6px; font-size: 13.5px;">2. Insumos del Inventario (Receta)</h4>
                <p style="color: #AAA; font-size: 11.5px; margin-bottom: 10px;">
                  Selecciona los insumos que se consumen al preparar este plato. Al venderlo, el sistema descontará automáticamente las cantidades de tu inventario.
                </p>

                <div id="dishIngredientsList" style="margin-bottom: 10px; display: flex; flex-direction: column; gap: 6px;"></div>

                <div style="background: #1E1E26; padding: 10px; border-radius: 6px; border: 1px dashed #444;">
                  <span style="font-size: 11px; color: #4CAF50; font-weight: bold;">➕ Agregar Insumo a este plato:</span>
                  <div class="input-row" style="margin-top: 6px;">
                    <select id="newIngSupplySelect" class="form-input" style="flex: 2;"></select>
                    <input type="number" step="0.05" id="newIngQtyInput" class="form-input" style="flex: 1;" placeholder="Cantidad (ej: 1 o 0.150)">
                    <button class="btn-primary" style="width: auto; background: #2E7D32; padding: 8px 12px; font-size: 12px;" onclick="addIngredientToCurrentDish()">
                      Agregar
                    </button>
                  </div>
                </div>
              </div>

              <!-- 3. Extras o Modificadores Opcionales -->
              <div style="background: #25252D; padding: 14px; border-radius: 8px; margin-bottom: 14px;">
                <h4 style="color: #FFB74D; margin-bottom: 6px; font-size: 13.5px;">3. Extras Opcionales (Adicionales con costo)</h4>
                <p style="color: #AAA; font-size: 11.5px; margin-bottom: 10px;">
                  Adicionales que el cliente puede pedir con este plato (ej: Tocineta Extra, Huevo Frito, Queso Fundido).
                </p>

                <div id="dishExtrasList" style="margin-bottom: 10px; display: flex; flex-direction: column; gap: 6px;"></div>

                <div style="background: #1E1E26; padding: 10px; border-radius: 6px; border: 1px dashed #444;">
                  <span style="font-size: 11px; color: #FFB74D; font-weight: bold;">➕ Agregar Extra Opcional:</span>
                  <div class="input-row" style="margin-top: 6px;">
                    <input type="text" id="newExtraNameInput" class="form-input" style="flex: 2;" placeholder="Nombre del extra (ej: Tocineta Extra)">
                    <input type="number" step="0.25" id="newExtraPriceInput" class="form-input" style="flex: 1;" placeholder="Precio $ USD (ej: 1.50)">
                    <button class="btn-primary" style="width: auto; background: #E65100; padding: 8px 12px; font-size: 12px;" onclick="addExtraToCurrentDish()">
                      Agregar
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Botón Guardar Definitivo -->
            <div style="display: flex; gap: 10px; margin-top: 14px;">
              <button class="btn-primary" style="background: #2E7D32; font-size: 14px; padding: 12px;" onclick="saveCurrentDish()">
                💾 Guardar Plato en el Menú y en la Nube
              </button>
              <button class="btn-secondary" style="width: auto; padding: 12px 20px;" onclick="closeDishModal()">
                Cancelar
              </button>
            </div>
          </div>
        </div>

        <script>
          let appData = { config: {}, supplies: [], menu: [], orders: [], closures: [] };
          let cart = [];
          let currentPayingOrder = null;
          let currentThermalFontSize = '14pt';
          let knownOrderStatuses = {};
          let audioCtx = null;
          let currentPcUserId = localStorage.getItem('pc_selected_user_id') || '';
          let openOrdersSubTab = 'PENDING';

          async function loadData() {
            try {
              const res = await fetch('/api/data');
              const fresh = await res.json();
              if (appData.orders && appData.orders.length > 0 && fresh.orders) {
                checkOrderReadyNotifications(fresh.orders);
              } else if (fresh.orders) {
                fresh.orders.forEach(o => { knownOrderStatuses[o.id] = o.status; });
              }
              appData = fresh;
              renderAll();
            } catch (e) {
              console.error(e);
            }
          }

          function populatePcUsers() {
            const select = document.getElementById('pcUserSelect');
            if (!select) return;
            if (!appData.users || appData.users.length === 0) {
              select.innerHTML = '<option value="admin">Administrador (ADMIN)</option>';
              applyPcPermissions();
              return;
            }

            const prevVal = select.value || currentPcUserId;
            select.innerHTML = appData.users.map(u => `
              <option value="§§{u.id}">§§{u.name} (§§{u.role})</option>
            `).join('');

            const exists = appData.users.some(u => u.id === prevVal);
            if (exists) {
              select.value = prevVal;
              currentPcUserId = prevVal;
            } else {
              currentPcUserId = appData.users[0].id;
              select.value = currentPcUserId;
            }
            applyPcPermissions();
          }

          function onPcUserSelected() {
            const select = document.getElementById('pcUserSelect');
            currentPcUserId = select.value;
            localStorage.setItem('pc_selected_user_id', currentPcUserId);
            applyPcPermissions();
          }

          function getActivePcUser() {
            if (!appData.users || appData.users.length === 0) {
              return { name: 'Administrador', role: 'ADMIN', canAccessPc: true, canUsePosPc: true, canUseKitchenPc: true, canUseBillingPc: true, canUseInventoryPc: true, canUseReportsPc: true };
            }
            const user = appData.users.find(u => u.id === currentPcUserId) || appData.users[0];
            return user;
          }

          function applyPcPermissions() {
            const user = getActivePcUser();
            const deniedBanner = document.getElementById('pcAccessDeniedBanner');
            const mainContent = document.getElementById('mainContainer');
            const nav = document.getElementById('mainNav');
            const roleBadge = document.getElementById('pcUserRoleBadge');

            if (roleBadge) {
              roleBadge.innerText = (user.name || 'Usuario') + ' (' + (user.role || 'STAFF') + ')';
            }

            const isAdmin = user.role === 'ADMIN';

            // Si el admin no le otorgó acceso a la computadora
            if (!user.canAccessPc && !isAdmin) {
              if (deniedBanner) {
                deniedBanner.style.display = 'block';
                const reasonEl = document.getElementById('pcAccessDeniedReason');
                if (reasonEl) reasonEl.innerText = 'El Administrador ha restringido el acceso a la Computadora para el perfil "' + (user.name || '') + '". Pide al Administrador que active tus permisos PC en la App Android.';
              }
              if (mainContent) mainContent.style.display = 'none';
              if (nav) nav.style.display = 'none';
              return;
            }

            if (deniedBanner) deniedBanner.style.display = 'none';
            if (mainContent) mainContent.style.display = 'block';
            if (nav) nav.style.display = 'flex';

            // Mostrar u ocultar pestañas según los permisos del perfil
            const tabPos = document.getElementById('navBtnPos');
            const tabKitchen = document.getElementById('navBtnKitchen');
            const tabBilling = document.getElementById('navBtnBilling');
            const tabMenu = document.getElementById('navBtnMenu');
            const tabInv = document.getElementById('navBtnInventory');
            const tabReports = document.getElementById('navBtnReports');

            if (tabPos) tabPos.style.display = (isAdmin || user.canUsePosPc) ? 'inline-block' : 'none';
            if (tabKitchen) tabKitchen.style.display = (isAdmin || user.canUseKitchenPc) ? 'inline-block' : 'none';
            if (tabBilling) tabBilling.style.display = (isAdmin || user.canUseBillingPc) ? 'inline-block' : 'none';
            if (tabMenu) tabMenu.style.display = (isAdmin || user.canUseMenuApp || user.canUseSettingsApp) ? 'inline-block' : 'none';
            if (tabInv) tabInv.style.display = (isAdmin || user.canUseInventoryPc) ? 'inline-block' : 'none';
            if (tabReports) tabReports.style.display = (isAdmin || user.canUseReportsPc) ? 'inline-block' : 'none';
          }

          function renderAll() {
            document.getElementById('headerBizName').innerText = appData.config.businessName || 'LA PARRILLA 200';
            document.getElementById('headerRate').innerText = 'Tasa: ' + (appData.config.exchangeRateUsdToBs || 65.0).toFixed(2) + ' Bs/$ • Control en PC';
            
            populatePcUsers();
            renderDishes('Todas');
            renderKitchen();
            renderOpenOrders();
            renderSupplies();
            renderReports();
            renderMenuCatalog();
          }

          function switchTab(tabId, btn) {
            document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
            document.querySelectorAll('nav button').forEach(el => el.classList.remove('active'));
            document.getElementById(tabId).classList.add('active');
            btn.classList.add('active');
          }

          function filterCategory(cat, btn) {
            document.querySelectorAll('.cat-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            renderDishes(cat);
          }

          function renderDishes(cat) {
            const grid = document.getElementById('dishesGrid');
            const rate = appData.config.exchangeRateUsdToBs || 65.0;
            const filtered = cat === 'Todas' ? appData.menu : appData.menu.filter(d => d.category.toLowerCase() === cat.toLowerCase());
            
            grid.innerHTML = filtered.map(dish => `
              <div class="dish-card" onclick="addToCart('§§{dish.id}')">
                <h3>§§{dish.name}</h3>
                <p>§§{dish.description || ''}</p>
                <div class="dish-price">
                  <span class="price-usd">USD §§{dish.priceUsd.toFixed(2)}</span>
                  <span class="price-bs">§§{(dish.priceUsd * rate).toFixed(2)} Bs</span>
                </div>
              </div>
            `).join('');
          }

          function addToCart(dishId) {
            const dish = appData.menu.find(d => d.id === dishId);
            if (!dish) return;
            const existing = cart.find(i => i.menuItemId === dishId && (!i.excludedIngredients || i.excludedIngredients.length === 0));
            if (existing) {
              existing.quantity++;
            } else {
              cart.push({
                menuItemId: dish.id,
                dishName: dish.name,
                priceUsd: dish.priceUsd,
                quantity: 1,
                excludedIngredients: [],
                specialInstructions: ''
              });
            }
            renderCart();
          }

          function updateCartQty(idx, delta) {
            cart[idx].quantity += delta;
            if (cart[idx].quantity <= 0) cart.splice(idx, 1);
            renderCart();
          }

          function clearCart() {
            cart = [];
            renderCart();
          }

          function renderCart() {
            const list = document.getElementById('cartItemsList');
            const rate = appData.config.exchangeRateUsdToBs || 65.0;
            if (cart.length === 0) {
              list.innerHTML = '<div style="color: #888; text-align: center; padding: 20px;">Toca un plato para agregarlo al pedido</div>';
              document.getElementById('cartTotalUsd').innerText = '$0.00';
              document.getElementById('cartTotalBs').innerText = '0.00 Bs';
              return;
            }

            const totalUsd = cart.reduce((acc, i) => acc + (i.priceUsd * i.quantity), 0);
            const totalBs = totalUsd * rate;

            list.innerHTML = cart.map((item, idx) => `
              <div class="cart-item">
                <div style="flex: 1;">
                  <div style="font-weight: bold;">§§{item.dishName}</div>
                  <div style="font-size: 11px; color: #AAA;">USD §§{item.priceUsd.toFixed(2)} c/u</div>
                  §§{item.specialInstructions ? `<div style="font-size: 11px; color: #FFA000; font-weight: bold;">📝 §§{item.specialInstructions}</div>` : ''}
                  <button class="btn-secondary" style="font-size: 10px; padding: 2px 6px; width: auto; margin-top: 4px;" onclick="promptItemInstruction(§§{idx})">
                    §§{item.specialInstructions ? '✏️ Cambiar Nota' : '+ Instrucción'}
                  </button>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <button class="btn-secondary" style="width: 24px; height: 24px; padding: 0;" onclick="updateCartQty(§§{idx}, -1)">-</button>
                  <span style="font-weight: bold;">§§{item.quantity}</span>
                  <button class="btn-secondary" style="width: 24px; height: 24px; padding: 0;" onclick="updateCartQty(§§{idx}, 1)">+</button>
                </div>
              </div>
            `).join('');

            document.getElementById('cartTotalUsd').innerText = '$' + totalUsd.toFixed(2);
            document.getElementById('cartTotalBs').innerText = totalBs.toFixed(2) + ' Bs';
          }

          function promptItemInstruction(idx) {
            const current = cart[idx].specialInstructions || '';
            const val = prompt('Instrucción especial para ' + cart[idx].dishName + ' (ej: Bien cocida, poco queso, salsa aparte):', current);
            if (val !== null) {
              cart[idx].specialInstructions = val.trim();
              renderCart();
            }
          }

          let currentPrintContext = { title: '', text: '', id: null, type: 'TEXT' };

          async function submitOrderFast() {
            if (cart.length === 0) return alert('El carrito está vacío');
            const cust = document.getElementById('custName').value || 'Cliente';
            const table = document.getElementById('tableDest').value || 'Mesa 1';
            const notes = document.getElementById('orderNotes')?.value || '';

            const payload = {
              customerName: cust,
              tableOrDestination: table,
              notes: notes,
              items: cart
            };

            const res = await fetch('/api/order', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
              clearCart();
              const notesEl = document.getElementById('orderNotes');
              if (notesEl) notesEl.value = '';
              loadData();
              showTicketModal('Comanda de Cocina #' + data.orderNumber, data.kitchenTicket, data.orderId, 'ORDER');
            } else {
              alert('Error: ' + data.error);
            }
          }

          let kitchenDisplayMode = 'NORMAL'; // 'NORMAL' o 'TABLET'

          function setKitchenDisplayMode(mode) {
            kitchenDisplayMode = mode;
            const normBtn = document.getElementById('kdsModeNormalBtn');
            const tabBtn = document.getElementById('kdsModeTabletBtn');
            if (normBtn && tabBtn) {
              if (mode === 'TABLET') {
                tabBtn.style.background = '#D32F2F';
                tabBtn.style.color = '#FFFFFF';
                normBtn.style.background = 'var(--card-color)';
                normBtn.style.color = 'var(--text-muted)';
              } else {
                normBtn.style.background = '#D32F2F';
                normBtn.style.color = '#FFFFFF';
                tabBtn.style.background = 'var(--card-color)';
                tabBtn.style.color = 'var(--text-muted)';
              }
            }
            renderKitchen();
          }

          function renderKitchen() {
            const grid = document.getElementById('kdsGrid');
            const pending = appData.orders.filter(o => o.status === 'EN_COCINA');
            if (pending.length === 0) {
              grid.innerHTML = '<div style="color: #66BB6A; font-weight: bold; padding: 24px; text-align: center; font-size: 16px; background: var(--surface-color); border-radius: 8px;">✅ ¡Todas las órdenes están listas! No hay comandas pendientes en cocina.</div>';
              return;
            }

            const isTablet = kitchenDisplayMode === 'TABLET';
            grid.style.gridTemplateColumns = isTablet ? 'repeat(auto-fill, minmax(360px, 1fr))' : 'repeat(auto-fill, minmax(280px, 1fr))';

            grid.innerHTML = pending.map(order => {
              let items = [];
              try { items = JSON.parse(order.itemsJson); } catch(e) {}
              const elapsed = Math.floor((Date.now() - order.timestamp) / 60000);
              return `
                <div class="kds-card §§{elapsed >= 15 ? 'urgent' : ''}" style="§§{isTablet ? 'padding: 18px; border-width: 2px;' : ''}">
                  <div class="kds-header">
                    <div>
                      <span class="order-badge" style="§§{isTablet ? 'font-size: 18px; padding: 6px 12px;' : ''}">#§§{order.orderNumber}</span>
                      <strong style="margin-left: 8px; §§{isTablet ? 'font-size: 18px;' : ''}">§§{order.tableOrDestination}</strong>
                    </div>
                    <span style="font-size: §§{isTablet ? '15px' : '12px'}; color: #AAA; font-weight: bold;">⏱ §§{elapsed} min</span>
                  </div>
                  <div class="kds-items" style="§§{isTablet ? 'font-size: 16px; margin: 12px 0;' : ''}">
                    §§{order.notes ? `<div style="background: rgba(255,160,0,0.22); border-left: 4px solid #FFA000; padding: 8px 10px; margin-bottom: 10px; border-radius: 6px; font-weight: 900; color: #FFA000; font-size: §§{isTablet ? '15px' : '13px'};">📝 INSTRUCCIÓN GENERAL: §§{order.notes.toUpperCase()}</div>` : ''}
                    §§{items.map(i => `
                      <div class="kds-item-row" style="§§{isTablet ? 'font-size: 18px; font-weight: 900; padding: 4px 0;' : ''}">
                        <span style="color: #FF5252; font-weight: 900;">§§{i.quantity}x</span> §§{i.dishName.toUpperCase()}
                      </div>
                      §§{i.excludedIngredientNames && i.excludedIngredientNames.length ? `<div class="kds-warning" style="§§{isTablet ? 'font-size: 14px; font-weight: 900; padding: 4px 8px;' : ''}">⚠️ SIN: §§{i.excludedIngredientNames.join(', ').toUpperCase()}</div>` : ''}
                      §§{i.specialInstructions ? `<div style="font-size: §§{isTablet ? '14px' : '12px'}; color: #FFA000; font-weight: bold; margin-left: 15px; margin-top: 2px;">👉 NOTA: §§{i.specialInstructions.toUpperCase()}</div>` : ''}
                    `).join('')}
                  </div>
                  <div style="display: flex; gap: 8px; margin-top: 12px;">
                    <button class="btn-primary" style="flex: 1; background: #2E7D32; font-size: §§{isTablet ? '16px' : '13px'}; font-weight: 900; padding: §§{isTablet ? '14px' : '10px'};" onclick="updateOrderStatus('§§{order.id}', 'LISTO')">
                      ✓ ¡LISTO!
                    </button>
                    <button class="btn-secondary" style="width: auto; padding: 8px 12px; background: #FFA000; color: #000; font-weight: bold;" onclick="openEditOrderModal('§§{order.id}')">
                      ✏️ Nota
                    </button>
                    <button class="btn-secondary" style="width: auto; padding: 8px 12px;" onclick="printOrderDirectly('§§{order.id}')">
                      🖨️
                    </button>
                  </div>
                </div>
              `;
            }).join('');
          }

          function switchOpenOrdersSubTab(subTab) {
            openOrdersSubTab = subTab;
            renderOpenOrders();
          }

          function renderOpenOrders() {
            const container = document.getElementById('openOrdersContainer');
            const unpaid = appData.orders.filter(o => !o.isPaid && o.status !== 'CANCELADO');
            const paid = appData.orders.filter(o => o.isPaid && o.status !== 'CANCELADO');

            const unpaidCountEl = document.getElementById('unpaidCount');
            const subUnpaidEl = document.getElementById('subTabUnpaidCount');
            const subPaidEl = document.getElementById('subTabPaidCount');
            if (unpaidCountEl) unpaidCountEl.innerText = unpaid.length;
            if (subUnpaidEl) subUnpaidEl.innerText = unpaid.length;
            if (subPaidEl) subPaidEl.innerText = paid.length;

            const pendingBtn = document.getElementById('subTabPendingBtn');
            const paidBtn = document.getElementById('subTabPaidBtn');
            if (pendingBtn && paidBtn) {
              if (openOrdersSubTab === 'PENDING') {
                pendingBtn.style.background = '#D32F2F';
                pendingBtn.style.color = '#FFFFFF';
                paidBtn.style.background = 'var(--card-color)';
                paidBtn.style.color = 'var(--text-muted)';
              } else {
                pendingBtn.style.background = 'var(--card-color)';
                pendingBtn.style.color = 'var(--text-muted)';
                paidBtn.style.background = '#2E7D32';
                paidBtn.style.color = '#FFFFFF';
              }
            }

            if (openOrdersSubTab === 'PENDING') {
              if (unpaid.length === 0) {
                container.innerHTML = '<div style="background: var(--surface-color); border: 1px solid var(--border-color); border-radius: 8px; color: #81C784; font-weight: bold; padding: 24px; text-align: center;">✅ ¡Excelente! No hay cuentas pendientes por cobrar. Todas están al día.</div>';
                return;
              }

              container.innerHTML = `
                <table>
                  <thead>
                    <tr>
                      <th>Orden</th>
                      <th>Destino</th>
                      <th>Cliente / Instrucción</th>
                      <th>Total USD</th>
                      <th>Total Bs</th>
                      <th>Estado</th>
                      <th>Acciones Rápidas</th>
                    </tr>
                  </thead>
                  <tbody>
                    §§{unpaid.map(o => `
                      <tr>
                        <td><strong>#§§{o.orderNumber}</strong></td>
                        <td>§§{o.tableOrDestination}</td>
                        <td>
                          <strong>§§{o.customerName}</strong>
                          §§{o.notes ? `<div style="font-size: 11px; color: #FFA000; font-weight: bold; margin-top: 2px;">📝 §§{o.notes}</div>` : ''}
                        </td>
                        <td><strong style="color: var(--fire-red);">USD §§{o.totalUsd.toFixed(2)}</strong></td>
                        <td><strong style="color: var(--amber-accent);">§§{o.totalBs.toFixed(2)} Bs</strong></td>
                        <td><span style="background: rgba(255,160,0,0.2); color: var(--amber-accent); padding: 2px 6px; border-radius: 4px; font-weight: bold;">§§{o.status}</span></td>
                        <td>
                          <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                            <!-- Botón Directo: Pagado Rápido en Efectivo -->
                            <button class="btn-primary" style="padding: 6px 12px; font-size: 12px; background: #2E7D32;" onclick="quickPayCashOrder('§§{o.id}')">
                              ⚡ PAGADO
                            </button>
                            <!-- Botón Cobro Detallado con Punto/Pago Móvil -->
                            <button class="btn-primary" style="padding: 6px 12px; font-size: 12px;" onclick="openPaymentForOrder('§§{o.id}')">
                              💳 Cobrar
                            </button>
                            <button class="btn-secondary" style="padding: 6px 10px; font-size: 12px; background: #FFA000; color: #000; font-weight: bold;" onclick="openEditOrderModal('§§{o.id}')">
                              ✏️ Modificar
                            </button>
                            <button class="btn-secondary" style="padding: 6px 10px; font-size: 12px;" onclick="printOrderDirectly('§§{o.id}')">
                              🖨️ Imprimir
                            </button>
                            <!-- Botón Eliminar Cuenta / Pedido de Prueba -->
                            <button class="btn-secondary" style="padding: 6px 10px; font-size: 12px; background: #5D101D; border-color: #B71C1C; color: #FFCDD2;" onclick="deleteOrderPermanently('§§{o.id}', §§{o.orderNumber})">
                              🗑️ Eliminar
                            </button>
                          </div>
                        </td>
                      </tr>
                    `).join('')}
                  </tbody>
                </table>
              `;
            } else {
              // LISTA DE CUENTAS YA PAGADAS (Separadas y protegidas contra cobro doble)
              if (paid.length === 0) {
                container.innerHTML = '<div style="color: #AAA; padding: 20px; text-align: center;">Aún no se han registrado cobros hoy.</div>';
                return;
              }

              container.innerHTML = `
                <table>
                  <thead>
                    <tr>
                      <th>Orden</th>
                      <th>Destino</th>
                      <th>Cliente</th>
                      <th>Total Pagado</th>
                      <th>Método de Pago</th>
                      <th>Referencia</th>
                      <th>Estado</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    §§{paid.map(o => `
                      <tr style="background: rgba(46,125,50,0.06);">
                        <td><strong style="color: #81C784;">#§§{o.orderNumber}</strong></td>
                        <td>§§{o.tableOrDestination}</td>
                        <td><strong>§§{o.customerName}</strong></td>
                        <td>
                          <strong style="color: #81C784;">USD §§{o.totalUsd.toFixed(2)}</strong>
                          <div style="font-size: 11px; color: var(--amber-accent);">§§{o.totalBs.toFixed(2)} Bs</div>
                        </td>
                        <td><span style="font-weight: bold; color: #E0E0E0;">§§{o.paymentMethod.replace('_', ' ')}</span></td>
                        <td><span style="color: #90CAF9; font-size: 12px;">§§{o.referenceCode || 'N/A'}</span></td>
                        <td><span style="background: rgba(46,125,50,0.3); color: #81C784; padding: 3px 8px; border-radius: 4px; font-weight: bold; font-size: 11px;">✅ PAGADA</span></td>
                        <td>
                          <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                            <!-- Botón de Factura: NO permite volver a cobrar -->
                            <button class="btn-secondary" style="padding: 6px 12px; font-size: 12px; background: #263238; border-color: #455A64;" onclick="printOrderDirectly('§§{o.id}')">
                              🖨️ Factura
                            </button>
                            <!-- Botón Eliminar Cuenta Pagada / Prueba -->
                            <button class="btn-secondary" style="padding: 6px 10px; font-size: 12px; background: #5D101D; border-color: #B71C1C; color: #FFCDD2;" onclick="deleteOrderPermanently('§§{o.id}', §§{o.orderNumber})">
                              🗑️ Eliminar
                            </button>
                          </div>
                        </td>
                      </tr>
                    `).join('')}
                  </tbody>
                </table>
              `;
            }
          }

          async function quickPayCashOrder(orderId) {
            const order = appData.orders.find(o => o.id === orderId);
            if (!order) return;
            if (order.isPaid) {
              return alert('Esta cuenta ya fue liquidada y no se puede volver a cobrar.');
            }
            const ok = confirm(`¿Marcar Comanda #§§{order.orderNumber} como PAGADA en EFECTIVO por USD §§{order.totalUsd.toFixed(2)} (§§{order.totalBs.toFixed(2)} Bs)?\n\nAl confirmar, la cuenta se quitará de las cuentas por cobrar inmediatamente.`);
            if (!ok) return;

            const res = await fetch('/api/pay', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                orderId: order.id,
                paymentMethod: 'USD_EFECTIVO',
                amountUsdPaid: order.totalUsd,
                amountPagoMovilBs: 0,
                amountPuntoVentaBs: 0,
                referenceCode: ''
              })
            });
            const data = await res.json();
            if (data.success) {
              openOrdersSubTab = 'PAID';
              loadData();
              showTicketModal('Factura de Cliente #' + order.orderNumber, data.clientTicket, order.id, 'ORDER');
            } else {
              alert('Error al liquidar pago: ' + data.error);
            }
          }

          async function deleteOrderPermanently(orderId, orderNum) {
            const ok = confirm(`¿Eliminar definitivamente la cuenta #§§{orderNum}?\n\nEsta acción borrará el pedido de la base de datos en la nube y no quedará registrado. Es ideal para borrar cuentas que hiciste de prueba.`);
            if (!ok) return;

            try {
              const res = await fetch('/api/order/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId: orderId })
              });
              const data = await res.json();
              if (data.success) {
                alert(`Cuenta #§§{orderNum} eliminada definitivamente.`);
                loadData();
              } else {
                alert('Error al eliminar cuenta: ' + data.error);
              }
            } catch (e) {
              alert('Error de conexión: ' + e.message);
            }
          }

          function openPaymentForOrder(orderId) {
            const order = appData.orders.find(o => o.id === orderId);
            if (!order) return;
            if (order.isPaid) {
              return alert('Esta cuenta ya está liquidada y no se puede volver a cobrar.');
            }
            currentPayingOrder = order;
            document.getElementById('checkoutOrderSummary').innerText = `Orden #§§{order.orderNumber} (§§{order.tableOrDestination} - §§{order.customerName}) • Total: USD §§{order.totalUsd.toFixed(2)} (§§{order.totalBs.toFixed(2)} Bs)`;
            updatePayFields();
            document.getElementById('checkoutModal').classList.add('open');
          }

          function updatePayFields() {
            if (!currentPayingOrder) return;
            const method = document.getElementById('payMethodSelect').value;
            const area = document.getElementById('payFieldsArea');
            if (method === 'USD_EFECTIVO') {
              area.innerHTML = `
                <label style="font-size: 12px; color: #AAA;">Monto Recibido en Dólares ($):</label>
                <input type="number" id="payUsdInput" class="form-input" value="§§{currentPayingOrder.totalUsd.toFixed(2)}">
              `;
            } else if (method === 'PAGO_MOVIL') {
              area.innerHTML = `
                <label style="font-size: 12px; color: #AAA;">Monto en Bolívares (Bs):</label>
                <input type="number" id="payPmBsInput" class="form-input" value="§§{currentPayingOrder.totalBs.toFixed(2)}">
                <label style="font-size: 12px; color: #AAA; margin-top: 6px;">Referencia Pago Móvil:</label>
                <input type="text" id="payRefInput" class="form-input" placeholder="Últimos 4 dígitos">
              `;
            } else if (method === 'PUNTO_VENTA') {
              area.innerHTML = `
                <label style="font-size: 12px; color: #AAA;">Monto en Bolívares (Bs):</label>
                <input type="number" id="payPvBsInput" class="form-input" value="§§{currentPayingOrder.totalBs.toFixed(2)}">
                <label style="font-size: 12px; color: #AAA; margin-top: 6px;">Lote / Aprobación POS:</label>
                <input type="text" id="payRefInput" class="form-input" placeholder="Referencia POS">
              `;
            } else if (method === 'MIXTO') {
              area.innerHTML = `
                <label style="font-size: 12px; color: #AAA;">Parte en USD ($):</label>
                <input type="number" id="payUsdInput" class="form-input" value="0.00">
                <label style="font-size: 12px; color: #AAA; margin-top: 6px;">Parte en Bs:</label>
                <input type="number" id="payPmBsInput" class="form-input" value="0.00">
              `;
            }
          }

          async function confirmPaymentAndPrint() {
            if (!currentPayingOrder) return;
            if (currentPayingOrder.isPaid) {
              closeCheckoutModal();
              return alert('Esta cuenta ya fue liquidada y pagada.');
            }
            const method = document.getElementById('payMethodSelect').value;
            let usdPaid = 0, pmBs = 0, pvBs = 0, ref = '';

            if (method === 'USD_EFECTIVO') {
              usdPaid = parseFloat(document.getElementById('payUsdInput')?.value || currentPayingOrder.totalUsd);
            } else if (method === 'PAGO_MOVIL') {
              pmBs = parseFloat(document.getElementById('payPmBsInput')?.value || currentPayingOrder.totalBs);
              ref = document.getElementById('payRefInput')?.value || '';
            } else if (method === 'PUNTO_VENTA') {
              pvBs = parseFloat(document.getElementById('payPvBsInput')?.value || currentPayingOrder.totalBs);
              ref = document.getElementById('payRefInput')?.value || '';
            } else if (method === 'MIXTO') {
              usdPaid = parseFloat(document.getElementById('payUsdInput')?.value || 0);
              pmBs = parseFloat(document.getElementById('payPmBsInput')?.value || 0);
            }

            const payload = {
              orderId: currentPayingOrder.id,
              paymentMethod: method,
              amountUsdPaid: usdPaid,
              amountPagoMovilBs: pmBs,
              amountPuntoVentaBs: pvBs,
              referenceCode: ref
            };

            const res = await fetch('/api/pay', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
              const paidId = currentPayingOrder.id;
              const ordNum = currentPayingOrder.orderNumber;
              closeCheckoutModal();
              loadData();
              showTicketModal('Factura de Cliente #' + ordNum, data.clientTicket, paidId, 'ORDER');
            } else {
              alert('Error: ' + data.error);
            }
          }

          function closeCheckoutModal() {
            document.getElementById('checkoutModal').classList.remove('open');
            currentPayingOrder = null;
          }

          function renderSupplies() {
            const tbody = document.getElementById('suppliesTableBody');
            const critCount = appData.supplies.filter(s => s.currentStock <= s.minStockThreshold).length;
            document.getElementById('critAlertBadge').innerHTML = critCount > 0 ? `<span class="badge-critical">⚠️ §§{critCount} Insumos Críticos</span>` : '<span style="color: #81C784; font-size: 12px; font-weight: bold;">✓ Stock Óptimo</span>';

            tbody.innerHTML = appData.supplies.map(s => {
              const isCrit = s.currentStock <= s.minStockThreshold;
              return `
                <tr>
                  <td><strong>§§{s.name}</strong></td>
                  <td style="color: §§{isCrit ? '#FF5252' : 'white'}; font-weight: bold;">§§{s.currentStock.toFixed(2)} §§{s.unit}</td>
                  <td>§§{s.minStockThreshold} §§{s.unit}</td>
                  <td>USD §§{s.costUsd.toFixed(2)}</td>
                  <td>§§{isCrit ? '<span class="badge-critical">CRÍTICO</span>' : '<span style="color: #81C784;">OK</span>'}</td>
                  <td>
                    <button class="btn-secondary" style="padding: 4px 8px; font-size: 11px;" onclick="quickRestock('§§{s.id}', §§{s.recommendedRestock})">+§§{s.recommendedRestock} §§{s.unit}</button>
                  </td>
                </tr>
              `;
            }).join('');
          }

          async function quickRestock(supId, amount) {
            const res = await fetch('/api/restock', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ supplyId: supId, amount: amount })
            });
            const data = await res.json();
            if (data.success) loadData();
          }

          async function updateOrderStatus(orderId, status) {
            const res = await fetch('/api/order/status', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ orderId: orderId, newStatus: status })
            });
            const data = await res.json();
            if (data.success) loadData();
          }

          function renderReports() {
            const todayOrders = appData.orders.filter(o => o.status !== 'CANCELADO');
            const totalUsd = todayOrders.reduce((a, o) => a + (o.totalUsd || 0), 0);
            const totalCash = todayOrders.reduce((a, o) => a + (o.amountUsdPaid || 0), 0);
            const totalPm = todayOrders.reduce((a, o) => a + (o.amountPagoMovilBs || 0), 0);
            const totalPv = todayOrders.reduce((a, o) => a + (o.amountPuntoVentaBs || 0), 0);

            document.getElementById('kpiUsd').innerText = '$' + totalUsd.toFixed(2);
            document.getElementById('kpiCash').innerText = '$' + totalCash.toFixed(2);
            document.getElementById('kpiPm').innerText = totalPm.toFixed(2) + ' Bs';
            document.getElementById('kpiPv').innerText = totalPv.toFixed(2) + ' Bs';

            const hist = document.getElementById('closuresHistory');
            hist.innerHTML = appData.closures.map(c => `
              <div style="background: var(--surface-color); border: 1px solid var(--border-color); padding: 12px; border-radius: 8px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center;">
                <div>
                  <strong>Cierre §§{c.closureType} #§§{c.closureNumber}</strong>
                  <div style="font-size: 11px; color: #AAA;">Pedidos: §§{c.orderCount} • Responsable: §§{c.closedBy}</div>
                </div>
                <div style="text-align: right;">
                  <div style="font-size: 15px; font-weight: bold;">USD §§{c.totalSalesUsd.toFixed(2)}</div>
                  <div style="font-size: 12px; color: var(--amber-accent);">§§{c.totalSalesBs.toFixed(2)} Bs</div>
                  <button class="btn-secondary" style="padding: 4px 10px; font-size: 11px; margin-top: 4px;" onclick="printClosureDirectly('§§{c.id}')">
                    🖨️ Imprimir
                  </button>
                </div>
              </div>
            `).join('');
          }

          async function generateClosure(type) {
            if (!confirm('¿Desea generar el Cierre ' + type + ' ahora? Se totalizarán los ingresos en USD, Pago Móvil y Punto de Venta.')) return;
            const res = await fetch('/api/closure', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ type: type })
            });
            const data = await res.json();
            if (data.success) {
              loadData();
              showTicketModal('Cierre de Caja ' + type, data.ticket, data.closureId, 'CLOSURE');
            }
          }

          function setThermalFontSize(size, btn) {
            currentThermalFontSize = size;
            document.querySelectorAll('#ticketModal .btn-secondary').forEach(b => {
              if (b.id && b.id.startsWith('fontBtn')) {
                b.style.background = '';
                b.style.borderColor = '';
              }
            });
            if (btn) {
              btn.style.background = 'var(--primary-red)';
              btn.style.borderColor = 'var(--primary-red)';
            }
            const tc = document.getElementById('ticketContent');
            if (tc) tc.style.fontSize = size;
          }

          function showTicketModal(title, text, entityId = null, entityType = 'TEXT') {
            currentPrintContext = { title: title, text: text, id: entityId, type: entityType };
            document.getElementById('ticketModalTitle').innerText = title;
            const tc = document.getElementById('ticketContent');
            tc.innerText = text;
            tc.style.fontSize = currentThermalFontSize;
            document.getElementById('ticketModal').classList.add('open');
          }

          function closeTicketModal() {
            document.getElementById('ticketModal').classList.remove('open');
          }

          function printOrderDirectly(orderId) {
            const order = appData.orders.find(o => o.id === orderId);
            if (!order) return;
            let items = [];
            try { items = JSON.parse(order.itemsJson); } catch(e) {}
            const rate = order.exchangeRate || appData.config.exchangeRateUsdToBs || 65.0;
            const dateStr = new Date(order.timestamp).toLocaleString();

            let preview = `================================\nLA PARRILLA 200 - ORDEN #§§{order.orderNumber}\nCliente: §§{order.customerName}\nDestino: §§{order.tableOrDestination}\nFecha: §§{dateStr}\n--------------------------------\n`;
            items.forEach(i => {
              preview += `§§{i.quantity}x §§{i.dishName.toUpperCase()} - USD §§{(i.priceUsd * i.quantity).toFixed(2)}\n`;
              if (i.excludedIngredientNames && i.excludedIngredientNames.length) {
                preview += `  [-] SIN: §§{i.excludedIngredientNames.join(', ').toUpperCase()}\n`;
              }
              if (i.specialInstructions) {
                preview += `  [!] NOTA: §§{i.specialInstructions.toUpperCase()}\n`;
              }
            });
            preview += `--------------------------------\nTOTAL USD: $§§{order.totalUsd.toFixed(2)}\nTOTAL BS: §§{order.totalBs.toFixed(2)} Bs\nESTADO: §§{order.isPaid ? 'PAGADO' : 'POR COBRAR'}\n================================`;

            showTicketModal('Orden #' + order.orderNumber, preview, order.id, 'ORDER');
          }

          function printClosureDirectly(closureId) {
            const closure = appData.closures.find(c => c.id === closureId);
            if (!closure) return;
            const dateStr = new Date(closure.timestamp).toLocaleString();
            let preview = `================================\nCIERRE DE CAJA §§{closure.closureType} #§§{closure.closureNumber}\nEmisión: §§{dateStr}\nPedidos: §§{closure.orderCount} | Responsable: §§{closure.closedBy}\n--------------------------------\nEfectivo USD: $§§{closure.totalUsdCash.toFixed(2)}\nPago Móvil: §§{closure.totalPagoMovilBs.toFixed(2)} Bs\nPunto Venta: §§{closure.totalPuntoVentaBs.toFixed(2)} Bs\n--------------------------------\nTOTAL GENERAL BS: §§{closure.totalSalesBs.toFixed(2)} Bs\nTOTAL GENERAL USD: $§§{closure.totalSalesUsd.toFixed(2)}\n================================`;

            showTicketModal('Cierre ' + closure.closureType + ' #' + closure.closureNumber, preview, closure.id, 'CLOSURE');
          }

          function printCurrentDocument(format) {
            if (format === 'SHEET') {
              const html = generateFullSheetHtml(currentPrintContext);
              printHtmlContent(html);
            } else {
              const html = generateThermalPrintHtml(currentPrintContext);
              printHtmlContent(html);
            }
          }

          function printHtmlContent(htmlContent) {
            const iframe = document.createElement('iframe');
            iframe.style.position = 'fixed';
            iframe.style.right = '0';
            iframe.style.bottom = '0';
            iframe.style.width = '0';
            iframe.style.height = '0';
            iframe.style.border = '0';
            document.body.appendChild(iframe);
            const doc = iframe.contentWindow.document;
            doc.open();
            doc.write(htmlContent);
            doc.close();
            iframe.contentWindow.focus();
            setTimeout(() => {
              iframe.contentWindow.print();
              setTimeout(() => {
                if (iframe.parentNode) iframe.parentNode.removeChild(iframe);
              }, 3000);
            }, 300);
          }

          function generateFullSheetHtml(ctx) {
            const cfg = appData.config || {};
            const bizName = cfg.businessName || 'LA PARRILLA 200';
            const rif = cfg.rif || 'J-40982314-0';
            const phone = cfg.phone || '+58 412-5552000';
            const addr = cfg.address || 'Sector Parrillera 200';

            if (ctx.type === 'CLOSURE') {
              const c = appData.closures.find(x => x.id === ctx.id);
              const dateStr = c ? new Date(c.timestamp).toLocaleString() : new Date().toLocaleString();
              return `
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <title>Reporte de Cierre de Caja</title>
                  <style>
                    @page { size: auto; margin: 15mm; }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; color: #000; background: #fff; line-height: 1.35; padding: 10px; font-size: 13px; }
                    .header { display: flex; justify-content: space-between; border-bottom: 2px solid #000; padding-bottom: 12px; margin-bottom: 20px; }
                    .kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 18px 0; }
                    .kpi-card { border: 1px solid #000; padding: 12px; border-radius: 6px; text-align: center; }
                    .kpi-val { font-size: 18px; font-weight: 900; margin-top: 4px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th, td { border: 1px solid #333; padding: 8px 10px; text-align: left; }
                    th { background: #f0f0f0; font-weight: bold; }
                    .sig-section { display: flex; justify-content: space-between; margin-top: 60px; }
                    .sig-line { width: 42%; border-top: 1px solid #000; text-align: center; padding-top: 6px; font-weight: bold; }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <div>
                      <h1 style="font-size: 24px; font-weight: 900;">§§{bizName}</h1>
                      <p>RIF: §§{rif} • Teléfono: §§{phone}</p>
                      <p>Dirección: §§{addr}</p>
                    </div>
                    <div style="text-align: right;">
                      <div style="background: #000; color: #fff; padding: 6px 14px; font-size: 16px; font-weight: bold; border-radius: 4px;">REPORTE DE AUDITORÍA</div>
                      <p style="margin-top: 4px;">Fecha de Emisión: §§{dateStr}</p>
                    </div>
                  </div>

                  <h2 style="font-size: 18px; margin-bottom: 10px;">RESUMEN DE CIERRE DE CAJA §§{c ? c.closureType : ''} #§§{c ? c.closureNumber : ''}</h2>
                  <p>Responsable del Cierre: <strong>§§{c ? c.closedBy : 'Administrador'}</strong> | Total de Pedidos Auditados: <strong>§§{c ? c.orderCount : 0}</strong></p>

                  <div class="kpi-grid">
                    <div class="kpi-card"><div>Efectivo USD ($)</div><div class="kpi-val">$§§{c ? c.totalUsdCash.toFixed(2) : '0.00'}</div></div>
                    <div class="kpi-card"><div>Pago Móvil (Bs)</div><div class="kpi-val">§§{c ? c.totalPagoMovilBs.toFixed(2) : '0.00'} Bs</div></div>
                    <div class="kpi-card"><div>Punto de Venta (Bs)</div><div class="kpi-val">§§{c ? c.totalPuntoVentaBs.toFixed(2) : '0.00'} Bs</div></div>
                    <div class="kpi-card" style="border: 2px solid #000; background: #fafafa;"><div>TOTAL DÓLARES</div><div class="kpi-val" style="color: #000;">$§§{c ? c.totalSalesUsd.toFixed(2) : '0.00'}</div></div>
                  </div>

                  <table>
                    <thead>
                      <tr><th>Concepto</th><th>Moneda</th><th>Monto Total</th><th>Estado</th></tr>
                    </thead>
                    <tbody>
                      <tr><td>Ventas en Efectivo Dólares</td><td>USD ($)</td><td>$§§{c ? c.totalUsdCash.toFixed(2) : '0.00'}</td><td>Verificado en Caja</td></tr>
                      <tr><td>Ventas por Pago Móvil</td><td>VES (Bs)</td><td>§§{c ? c.totalPagoMovilBs.toFixed(2) : '0.00'} Bs</td><td>Bancarizado</td></tr>
                      <tr><td>Ventas por Punto de Venta</td><td>VES (Bs)</td><td>§§{c ? c.totalPuntoVentaBs.toFixed(2) : '0.00'} Bs</td><td>Bancarizado</td></tr>
                      <tr style="font-weight: 900; background: #f5f5f5;">
                        <td colspan="2">TOTAL GENERAL FACTURADO EN BOLÍVARES</td>
                        <td colspan="2" style="font-size: 15px;">§§{c ? c.totalSalesBs.toFixed(2) : '0.00'} Bs</td>
                      </tr>
                      <tr style="font-weight: 900; background: #eee;">
                        <td colspan="2">TOTAL GENERAL FACTURADO EN DÓLARES</td>
                        <td colspan="2" style="font-size: 16px;">$§§{c ? c.totalSalesUsd.toFixed(2) : '0.00'} USD</td>
                      </tr>
                    </tbody>
                  </table>

                  <div class="sig-section">
                    <div class="sig-line">ADMINISTRADOR RESPONSABLE<br><span style="font-size: 11px; font-weight: normal;">Firma y Cédula</span></div>
                    <div class="sig-line">AUDITORÍA Y CAJA GENERAL<br><span style="font-size: 11px; font-weight: normal;">Conforme y Recibido</span></div>
                  </div>
                </body>
                </html>
              `;
            }

            // Para Órdenes / Facturas
            const order = ctx.id ? appData.orders.find(o => o.id === ctx.id) : null;
            let items = [];
            if (order) {
              try { items = JSON.parse(order.itemsJson); } catch(e) {}
            }
            const ordNum = order ? order.orderNumber : '0000';
            const dateStr = order ? new Date(order.timestamp).toLocaleString() : new Date().toLocaleString();
            const cust = order ? order.customerName : 'Cliente';
            const dest = order ? order.tableOrDestination : 'Mesa';
            const totalUsd = order ? order.totalUsd : 0;
            const totalBs = order ? order.totalBs : 0;
            const rate = order ? (order.exchangeRate || 65.0) : 65.0;

            const itemsRows = items.map((it, idx) => `
              <tr>
                <td style="text-align: center; font-weight: bold;">§§{it.quantity}x</td>
                <td>
                  <strong>§§{it.dishName}</strong>
                  §§{it.excludedIngredientNames && it.excludedIngredientNames.length ? `<div style="font-size: 11px; color: #D32F2F; font-weight: bold;">[-] SIN: §§{it.excludedIngredientNames.join(', ')}</div>` : ''}
                  §§{it.specialInstructions ? `<div style="font-size: 11px; font-style: italic;">Nota: §§{it.specialInstructions}</div>` : ''}
                </td>
                <td style="text-align: right;">$§§{it.priceUsd.toFixed(2)}</td>
                <td style="text-align: right; font-weight: bold;">$§§{(it.priceUsd * it.quantity).toFixed(2)}</td>
              </tr>
            `).join('');

            return `
              <!DOCTYPE html>
              <html lang="es">
              <head>
                <meta charset="utf-8">
                <title>Nota de Entrega / Factura #§§{ordNum}</title>
                <style>
                  @page { size: auto; margin: 15mm; }
                  * { box-sizing: border-box; margin: 0; padding: 0; }
                  body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif; color: #000; background: #fff; line-height: 1.35; padding: 10px; font-size: 13px; }
                  .header { display: flex; justify-content: space-between; border-bottom: 2px solid #000; padding-bottom: 12px; margin-bottom: 15px; }
                  .info-box { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; border: 1px solid #ccc; background: #f9f9f9; padding: 10px 14px; border-radius: 6px; margin-bottom: 16px; }
                  table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
                  th, td { border: 1px solid #ccc; padding: 8px 10px; text-align: left; }
                  th { background: #f0f0f0; font-weight: bold; text-transform: uppercase; font-size: 12px; }
                  .bottom-layout { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; }
                  .pay-details { flex: 1; border: 1px solid #ccc; padding: 12px; border-radius: 6px; background: #fafafa; }
                  .totals-box { width: 280px; border: 2px solid #000; padding: 12px; border-radius: 6px; }
                  .total-line { display: flex; justify-content: space-between; margin-bottom: 4px; }
                  .grand-total { border-top: 2px solid #000; padding-top: 6px; margin-top: 6px; font-size: 16px; font-weight: 900; }
                  .sig-section { display: flex; justify-content: space-between; margin-top: 40px; }
                  .sig-line { width: 42%; border-top: 1px solid #000; text-align: center; padding-top: 6px; font-weight: bold; font-size: 11px; }
                </style>
              </head>
              <body>
                <div class="header">
                  <div>
                    <h1 style="font-size: 24px; font-weight: 900;">§§{bizName}</h1>
                    <p style="font-size: 12px; color: #333;">Auténtico Sabor a la Brasa</p>
                    <p style="font-size: 12px;">RIF: §§{rif} • Teléfono: §§{phone}</p>
                    <p style="font-size: 12px;">§§{addr}</p>
                  </div>
                  <div style="text-align: right;">
                    <div style="background: #000; color: #fff; padding: 6px 14px; font-size: 16px; font-weight: bold; border-radius: 4px; display: inline-block;">FACTURA / NOTA DE ENTREGA</div>
                    <p style="font-size: 15px; font-weight: bold; margin-top: 6px;">NRO: #§§{ordNum}</p>
                    <p style="font-size: 12px;">Fecha: §§{dateStr}</p>
                  </div>
                </div>

                <div class="info-box">
                  <div><strong>CLIENTE:</strong> §§{cust}</div>
                  <div><strong>MESA / DESTINO:</strong> §§{dest}</div>
                  <div><strong>ESTADO:</strong> §§{order && order.isPaid ? 'PAGADO' : 'POR COBRAR'}</div>
                  <div><strong>ATENDIDO POR:</strong> §§{order && order.createdBy ? order.createdBy : 'Portal PC'}</div>
                </div>

                <table>
                  <thead>
                    <tr>
                      <th style="width: 10%; text-align: center;">Cant.</th>
                      <th style="width: 55%;">Descripción del Plato / Insumos</th>
                      <th style="width: 15%; text-align: right;">Precio Unit.</th>
                      <th style="width: 20%; text-align: right;">Total USD</th>
                    </tr>
                  </thead>
                  <tbody>
                    §§{itemsRows || '<tr><td colspan="4" style="text-align:center;">Detalle de pedido</td></tr>'}
                  </tbody>
                </table>

                <div class="bottom-layout">
                  <div class="pay-details">
                    <h4 style="margin-bottom: 6px;">Detalle de Pago:</h4>
                    <p>Método: <strong>§§{order ? (order.paymentMethod || 'Efectivo / Pago Móvil') : 'Pendiente'}</strong></p>
                    §§{order && order.amountUsdPaid > 0 ? `<p>USD Efectivo: $§§{order.amountUsdPaid.toFixed(2)}</p>` : ''}
                    §§{order && order.amountPagoMovilBs > 0 ? `<p>Pago Móvil: §§{order.amountPagoMovilBs.toFixed(2)} Bs</p>` : ''}
                    §§{order && order.amountPuntoVentaBs > 0 ? `<p>Punto de Venta: §§{order.amountPuntoVentaBs.toFixed(2)} Bs</p>` : ''}
                    §§{order && order.referenceCode ? `<p>Referencia: <strong>§§{order.referenceCode}</strong></p>` : ''}
                  </div>

                  <div class="totals-box">
                    <div class="total-line"><span>Sub-Total USD:</span><span>$§§{totalUsd.toFixed(2)}</span></div>
                    <div class="total-line"><span>Tasa del Día:</span><span>§§{rate.toFixed(2)} Bs/$</span></div>
                    <div class="total-line grand-total"><span>TOTAL USD:</span><span>$§§{totalUsd.toFixed(2)}</span></div>
                    <div class="total-line" style="font-size: 15px; font-weight: bold; margin-top: 4px;"><span>TOTAL EN BS:</span><span>§§{totalBs.toFixed(2)} Bs</span></div>
                  </div>
                </div>

                <div class="sig-section">
                  <div class="sig-line">DESPACHADO POR LA PARRILLA 200<br><span style="font-size: 10px; font-weight: normal;">Control de Cocina y Caja</span></div>
                  <div class="sig-line">RECIBIDO CONFORME<br><span style="font-size: 10px; font-weight: normal;">Firma del Cliente</span></div>
                </div>
              </body>
              </html>
            `;
          }

          function generateThermalPrintHtml(ctx) {
            return `
              <!DOCTYPE html>
              <html lang="es">
              <head>
                <meta charset="utf-8">
                <title>§§{ctx.title}</title>
                <style>
                  @page {
                    size: 80mm auto;
                    margin: 0mm !important;
                  }
                  * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                  }
                  html, body {
                    width: 80mm !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    background: #ffffff !important;
                    color: #000000 !important;
                    font-family: 'Courier New', Courier, monospace;
                    font-size: §§{currentThermalFontSize};
                    font-weight: 900;
                    line-height: 1.28;
                    letter-spacing: 0.2px;
                    -webkit-print-color-adjust: exact;
                    print-color-adjust: exact;
                  }
                  .ticket {
                    width: 72mm;
                    margin: 0 auto;
                    padding: 3mm 1mm 15mm 1mm;
                    white-space: pre-wrap;
                    word-break: break-word;
                    overflow-wrap: break-word;
                  }
                  @media print {
                    html, body {
                      width: 80mm !important;
                      margin: 0 !important;
                      padding: 0 !important;
                      background: #fff !important;
                      color: #000 !important;
                    }
                    .ticket {
                      width: 72mm !important;
                      margin: 0 auto !important;
                      padding: 2mm 0 10mm 0 !important;
                    }
                  }
                </style>
              </head>
              <body>
                <div class="ticket">§§{ctx.text}</div>
              </body>
              </html>
            `;
          }

          // MODIFICAR PEDIDO / AGREGAR INSTRUCCIONES DESDE PC
          let editingOrderId = null;
          let editingOrderItems = [];

          function openEditOrderModal(orderId) {
            const order = appData.orders.find(o => o.id === orderId);
            if (!order) return;
            editingOrderId = order.id;
            try {
              editingOrderItems = JSON.parse(order.itemsJson) || [];
            } catch(e) {
              editingOrderItems = [];
            }
            document.getElementById('editOrderModalTitle').innerText = 'Modificar Pedido #' + order.orderNumber;
            document.getElementById('editOrderSubtitle').innerText = (order.tableOrDestination || 'Mesa') + ' • ' + (order.customerName || 'Cliente') + ' • Total Actual: USD $' + order.totalUsd.toFixed(2);
            document.getElementById('editOrderNotesInput').value = order.notes || '';

            // Llenar selector de platos del menú
            const dishSelect = document.getElementById('editAddDishSelect');
            dishSelect.innerHTML = appData.menu.map(d => `<option value="§§{d.id}">§§{d.name} ($§§{d.priceUsd.toFixed(2)})</option>`).join('');

            renderEditOrderItems();
            document.getElementById('editOrderModal').classList.add('open');
          }

          function renderEditOrderItems() {
            const container = document.getElementById('editOrderItemsList');
            if (editingOrderItems.length === 0) {
              container.innerHTML = '<div style="color: #AAA; font-size: 12px; padding: 10px;">No hay platos en este pedido.</div>';
              return;
            }
            container.innerHTML = editingOrderItems.map((item, idx) => `
              <div style="display: flex; justify-content: space-between; align-items: center; background: var(--surface-color); padding: 8px 10px; border-radius: 6px; border: 1px solid var(--border-color);">
                <div style="flex: 1;">
                  <div style="font-weight: bold; font-size: 13px;">§§{item.dishName}</div>
                  <div style="font-size: 11px; color: #AAA;">USD $§§{item.priceUsd.toFixed(2)} c/u</div>
                  §§{item.specialInstructions ? `<div style="font-size: 11px; color: #FFA000; font-weight: bold;">📝 §§{item.specialInstructions}</div>` : ''}
                  <button class="btn-secondary" style="font-size: 10px; padding: 2px 6px; width: auto; margin-top: 3px;" onclick="promptEditItemNote(§§{idx})">
                    §§{item.specialInstructions ? '✏️ Cambiar Nota' : '+ Nota Plato'}
                  </button>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <button class="btn-secondary" style="width: 26px; height: 26px; padding: 0;" onclick="updateEditItemQty(§§{idx}, -1)">-</button>
                  <span style="font-weight: bold; min-width: 20px; text-align: center;">§§{item.quantity}</span>
                  <button class="btn-secondary" style="width: 26px; height: 26px; padding: 0;" onclick="updateEditItemQty(§§{idx}, 1)">+</button>
                  <button class="btn-secondary" style="width: 26px; height: 26px; padding: 0; background: #C62828; margin-left: 6px;" onclick="removeEditItem(§§{idx})">✕</button>
                </div>
              </div>
            `).join('');
          }

          function promptEditItemNote(idx) {
            const current = editingOrderItems[idx].specialInstructions || '';
            const val = prompt('Instrucción especial para ' + editingOrderItems[idx].dishName + ':', current);
            if (val !== null) {
              editingOrderItems[idx].specialInstructions = val.trim();
              renderEditOrderItems();
            }
          }

          function updateEditItemQty(idx, delta) {
            editingOrderItems[idx].quantity += delta;
            if (editingOrderItems[idx].quantity <= 0) {
              editingOrderItems.splice(idx, 1);
            }
            renderEditOrderItems();
          }

          function removeEditItem(idx) {
            editingOrderItems.splice(idx, 1);
            renderEditOrderItems();
          }

          function addItemToEditingOrder() {
            const dishId = document.getElementById('editAddDishSelect').value;
            const dish = appData.menu.find(d => d.id === dishId);
            if (!dish) return;
            const existing = editingOrderItems.find(i => i.menuItemId === dishId && (!i.specialInstructions || i.specialInstructions === ''));
            if (existing) {
              existing.quantity++;
            } else {
              editingOrderItems.push({
                menuItemId: dish.id,
                dishName: dish.name,
                priceUsd: dish.priceUsd,
                quantity: 1,
                excludedIngredientNames: [],
                specialInstructions: ''
              });
            }
            renderEditOrderItems();
          }

          function closeEditOrderModal() {
            document.getElementById('editOrderModal').classList.remove('open');
            editingOrderId = null;
            editingOrderItems = [];
          }

          async function saveEditedOrder() {
            if (!editingOrderId) return;
            if (editingOrderItems.length === 0) {
              alert('La orden debe tener al menos 1 plato o bebida.');
              return;
            }
            const newNotes = document.getElementById('editOrderNotesInput').value.trim();

            const origOrder = appData.orders.find(o => o.id === editingOrderId);
            let origItems = [];
            try { origItems = JSON.parse(origOrder.itemsJson); } catch(e) {}

            const newlyAdded = [];
            editingOrderItems.forEach(cur => {
              const orig = origItems.find(o => o.menuItemId === cur.menuItemId);
              const addedQty = orig ? (cur.quantity - orig.quantity) : cur.quantity;
              if (addedQty > 0) {
                newlyAdded.push({
                  menuItemId: cur.menuItemId,
                  dishName: cur.dishName,
                  quantity: addedQty,
                  priceUsd: cur.priceUsd
                });
              }
            });

            const payload = {
              orderId: editingOrderId,
              updatedItems: editingOrderItems,
              notes: newNotes,
              newlyAddedItems: newlyAdded
            };

            const res = await fetch('/api/order/update', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (data.success) {
              closeEditOrderModal();
              loadData();
              alert('¡Pedido actualizado con éxito! La instrucción y los platos han sido modificados.');
            } else {
              alert('Error al actualizar el pedido: ' + data.error);
            }
          }

          // NOTIFICACIONES Y SONIDOS DE COCINA A CAJA Y MESONEROS
          function playOrderReadySound() {
            try {
              const AudioContext = window.AudioContext || window.webkitAudioContext;
              if (!AudioContext) return;
              if (!audioCtx) audioCtx = new AudioContext();
              if (audioCtx.state === 'suspended') audioCtx.resume();

              const osc = audioCtx.createOscillator();
              const gain = audioCtx.createGain();
              osc.connect(gain);
              gain.connect(audioCtx.destination);

              // Tono doble tipo campana de restaurante (D5 -> A5)
              osc.type = 'sine';
              const now = audioCtx.currentTime;
              osc.frequency.setValueAtTime(587.33, now);
              osc.frequency.setValueAtTime(880.0, now + 0.15);
              gain.gain.setValueAtTime(0.35, now);
              gain.gain.exponentialRampToValueAtTime(0.001, now + 0.65);

              osc.start(now);
              osc.stop(now + 0.65);
            } catch (e) {
              console.error('Audio play error', e);
            }
          }

          function showReadyToast(order) {
            const container = document.getElementById('readyToastContainer');
            if (!container) return;
            const toast = document.createElement('div');
            toast.style.background = '#1B5E20';
            toast.style.color = '#FFFFFF';
            toast.style.border = '2px solid #66BB6A';
            toast.style.borderRadius = '10px';
            toast.style.padding = '14px 16px';
            toast.style.boxShadow = '0 6px 18px rgba(0,0,0,0.7)';
            toast.innerHTML = `
              <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                <strong style="font-size: 15px; color: #E8F5E9;">🔔 ¡ORDEN #§§{order.orderNumber} LISTA!</strong>
                <button style="background: none; border: none; color: #FFF; font-size: 16px; cursor: pointer; margin-left: 8px;" onclick="this.parentElement.parentElement.remove()">✕</button>
              </div>
              <div style="font-size: 13px; margin-top: 4px; font-weight: bold; color: #FFA000;">§§{order.tableOrDestination} - §§{order.customerName}</div>
              <div style="font-size: 12px; margin-top: 2px;">El pedido ya está listo para ser servido y cobrado.</div>
              <div style="display: flex; gap: 8px; margin-top: 10px;">
                <button class="btn-primary" style="padding: 6px 12px; font-size: 11px; background: #2E7D32;" onclick="openPaymentForOrder('§§{order.id}'); this.parentElement.parentElement.remove();">
                  💳 Cobrar
                </button>
                <button class="btn-secondary" style="padding: 6px 12px; font-size: 11px;" onclick="printOrderDirectly('§§{order.id}'); this.parentElement.parentElement.remove();">
                  🖨️ Ticket
                </button>
              </div>
            `;
            container.appendChild(toast);
            setTimeout(() => { if (toast.parentNode) toast.parentNode.removeChild(toast); }, 15000);
          }

          function checkOrderReadyNotifications(orders) {
            if (!orders || !Array.isArray(orders)) return;
            orders.forEach(o => {
              const prev = knownOrderStatuses[o.id];
              if (prev && prev !== 'LISTO' && o.status === 'LISTO') {
                playOrderReadySound();
                showReadyToast(o);
                if (window.Notification && Notification.permission === 'granted') {
                  new Notification('🔔 ¡Pedido Listo para Servir!', {
                    body: `Orden #§§{o.orderNumber} - §§{o.tableOrDestination} (§§{o.customerName}) está lista en cocina.`,
                    icon: '/favicon.ico'
                  });
                }
              }
              knownOrderStatuses[o.id] = o.status;
            });
          }

          window.addEventListener('click', () => {
            if (!audioCtx) {
              try {
                const AudioContext = window.AudioContext || window.webkitAudioContext;
                if (AudioContext) audioCtx = new AudioContext();
              } catch(e) {}
            }
            if (window.Notification && Notification.permission === 'default') {
              Notification.requestPermission();
            }
          }, { once: true });

          // GESTIÓN DE MENÚ, RECETAS E INSUMOS DESDE PC
          let currentEditingDish = null;
          let currentDishIngredients = [];
          let currentDishExtras = [];
          let currentMenuCategoryFilter = 'Todas';

          function renderMenuCatalog() {
            const tbody = document.getElementById('menuTableBody');
            if (!tbody) return;
            const rate = appData.config.exchangeRateUsdToBs || 65.0;

            const catsBar = document.getElementById('menuCategoriesBar');
            if (catsBar) {
              const allCategories = ['Todas', ...new Set(appData.menu.map(m => m.category).filter(Boolean))];
              catsBar.innerHTML = allCategories.map(cat => `
                <button class="btn-secondary" style="width: auto; padding: 6px 14px; font-size: 12px; §§{currentMenuCategoryFilter === cat ? 'background: var(--primary-red); border-color: var(--primary-red); color: white;' : ''}" onclick="filterMenuCatalog('§§{cat}')">
                  §§{cat}
                </button>
              `).join('');
            }

            const filtered = currentMenuCategoryFilter === 'Todas' ? appData.menu : appData.menu.filter(m => m.category === currentMenuCategoryFilter);

            if (filtered.length === 0) {
              tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #AAA; padding: 24px;">No hay platos en esta categoría. Puedes crear uno con el botón "➕ Crear Nuevo Plato".</td></tr>';
              return;
            }

            tbody.innerHTML = filtered.map(dish => {
              let ings = [];
              try { ings = JSON.parse(dish.ingredientsJson || '[]'); } catch(e) {}
              let extras = [];
              try { extras = JSON.parse(dish.extrasJson || '[]'); } catch(e) {}

              const ingsSummary = ings.length > 0
                ? ings.map(i => `<span style="background: rgba(46,125,50,0.25); color: #81C784; padding: 2px 6px; border-radius: 4px; font-size: 11px; margin-right: 4px; display: inline-block; margin-bottom: 2px;">§§{i.quantityRequired} §§{i.unit} §§{i.supplyName}</span>`).join('')
                : '<span style="color: #888; font-size: 11px; font-style: italic;">Sin receta (no descuenta)</span>';

              const extrasSummary = extras.length > 0
                ? extras.map(e => `<span style="background: rgba(255,160,0,0.2); color: #FFB74D; padding: 2px 6px; border-radius: 4px; font-size: 11px; margin-right: 4px; display: inline-block; margin-bottom: 2px;">+§§{e.name} ($§§{Number(e.priceUsd).toFixed(2)})</span>`).join('')
                : '<span style="color: #777; font-size: 11px;">Sin extras</span>';

              const priceBs = (dish.priceUsd * rate).toFixed(2);

              return `
                <tr>
                  <td>
                    <strong>§§{dish.name}</strong>
                    §§{dish.description ? `<div style="font-size: 11.5px; color: #AAA; margin-top: 2px;">§§{dish.description}</div>` : ''}
                  </td>
                  <td><span style="background: #33333E; color: #EEE; padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;">§§{dish.category}</span></td>
                  <td><strong style="color: #4CAF50; font-size: 14px;">$§§{dish.priceUsd.toFixed(2)}</strong></td>
                  <td><strong style="color: #FFB300; font-size: 13px;">§§{priceBs} Bs</strong></td>
                  <td style="max-width: 250px;">§§{ingsSummary}</td>
                  <td style="max-width: 200px;">§§{extrasSummary}</td>
                  <td>
                    §§{dish.isAvailable ? '<span style="color: #81C784; font-weight: bold; font-size: 11px;">✓ ACTIVO</span>' : '<span style="color: #FF5252; font-weight: bold; font-size: 11px;">✕ PAUSADO</span>'}
                  </td>
                  <td>
                    <div style="display: flex; gap: 6px;">
                      <button class="btn-secondary" style="padding: 6px 12px; font-size: 12px; background: #1976D2; color: white;" onclick="openEditDishModal('§§{dish.id}')">
                        ✏️ Editar
                      </button>
                      <button class="btn-secondary" style="padding: 6px 10px; font-size: 12px; background: #5D101D; color: #FFCDD2; border-color: #B71C1C;" onclick="deleteDishPrompt('§§{dish.id}', '§§{dish.name.replace(/'/g, "\\'")}')">
                        🗑️
                      </button>
                    </div>
                  </td>
                </tr>
              `;
            }).join('');
          }

          function filterMenuCatalog(cat) {
            currentMenuCategoryFilter = cat;
            renderMenuCatalog();
          }

          function populateSupplySelect() {
            const select = document.getElementById('newIngSupplySelect');
            if (!select) return;
            select.innerHTML = appData.supplies.map(s => `
              <option value="§§{s.id}">§§{s.name} (en §§{s.unit})</option>
            `).join('');
          }

          function openCreateDishModal() {
            currentEditingDish = { id: '', name: '', category: 'Parrillas', priceUsd: 5.0, description: '', isAvailable: true, ingredientsJson: '[]', extrasJson: '[]' };
            currentDishIngredients = [];
            currentDishExtras = [];

            document.getElementById('dishModalTitle').innerText = '➕ Crear Nuevo Plato en el Menú';
            document.getElementById('dishNameInput').value = '';
            document.getElementById('dishCategoryInput').value = 'Parrillas';
            document.getElementById('dishPriceInput').value = '5.00';
            document.getElementById('dishDescInput').value = '';
            document.getElementById('dishAvailableInput').checked = true;

            updateDishBsPreview();
            populateSupplySelect();
            renderDishIngredientsList();
            renderDishExtrasList();

            document.getElementById('dishModal').classList.add('open');
          }

          function openEditDishModal(dishId) {
            const dish = appData.menu.find(m => m.id === dishId);
            if (!dish) return;
            currentEditingDish = dish;

            try { currentDishIngredients = JSON.parse(dish.ingredientsJson || '[]'); } catch(e) { currentDishIngredients = []; }
            try { currentDishExtras = JSON.parse(dish.extrasJson || '[]'); } catch(e) { currentDishExtras = []; }

            document.getElementById('dishModalTitle').innerText = '✏️ Editar Plato: ' + dish.name;
            document.getElementById('dishNameInput').value = dish.name;
            document.getElementById('dishCategoryInput').value = dish.category || 'Parrillas';
            document.getElementById('dishPriceInput').value = dish.priceUsd;
            document.getElementById('dishDescInput').value = dish.description || '';
            document.getElementById('dishAvailableInput').checked = dish.isAvailable !== false;

            updateDishBsPreview();
            populateSupplySelect();
            renderDishIngredientsList();
            renderDishExtrasList();

            document.getElementById('dishModal').classList.add('open');
          }

          function closeDishModal() {
            document.getElementById('dishModal').classList.remove('open');
            currentEditingDish = null;
          }

          function updateDishBsPreview() {
            const val = parseFloat(document.getElementById('dishPriceInput').value) || 0;
            const rate = appData.config.exchangeRateUsdToBs || 65.0;
            const bs = val * rate;
            const preview = document.getElementById('dishPriceBsPreview');
            if (preview) preview.value = bs.toFixed(2) + ' Bs';
          }

          function renderDishIngredientsList() {
            const container = document.getElementById('dishIngredientsList');
            if (!container) return;
            if (currentDishIngredients.length === 0) {
              container.innerHTML = '<div style="color: #888; font-size: 12px; font-style: italic;">No has agregado ningún insumo aún. Si agregas insumos, se descontarán del stock al venderse.</div>';
              return;
            }

            container.innerHTML = currentDishIngredients.map((ing, idx) => `
              <div style="display: flex; justify-content: space-between; align-items: center; background: #1C1C24; padding: 6px 10px; border-radius: 6px; border-left: 3px solid #4CAF50;">
                <div>
                  <span style="font-weight: bold; color: white;">§§{ing.supplyName}</span>
                  <span style="color: #81C784; font-size: 12px; margin-left: 8px;">§§{ing.quantityRequired} §§{ing.unit}</span>
                </div>
                <button class="btn-secondary" style="width: auto; padding: 2px 8px; font-size: 11px; background: #5D101D; border-color: #B71C1C; color: #FFCDD2;" onclick="removeIngredientFromCurrentDish(§§{idx})">
                  🗑️ Quitar
                </button>
              </div>
            `).join('');
          }

          function addIngredientToCurrentDish() {
            const supplyId = document.getElementById('newIngSupplySelect').value;
            const qty = parseFloat(document.getElementById('newIngQtyInput').value);
            if (!supplyId || isNaN(qty) || qty <= 0) {
              return alert('Ingresa una cantidad válida mayor a cero para el insumo.');
            }
            const supply = appData.supplies.find(s => s.id === supplyId);
            if (!supply) return;

            const existingIdx = currentDishIngredients.findIndex(i => i.supplyId === supplyId);
            if (existingIdx >= 0) {
              currentDishIngredients[existingIdx].quantityRequired = qty;
            } else {
              currentDishIngredients.push({
                supplyId: supply.id,
                supplyName: supply.name,
                quantityRequired: qty,
                unit: supply.unit,
                isRemovable: true
              });
            }
            document.getElementById('newIngQtyInput').value = '';
            renderDishIngredientsList();
          }

          function removeIngredientFromCurrentDish(idx) {
            currentDishIngredients.splice(idx, 1);
            renderDishIngredientsList();
          }

          function renderDishExtrasList() {
            const container = document.getElementById('dishExtrasList');
            if (!container) return;
            if (currentDishExtras.length === 0) {
              container.innerHTML = '<div style="color: #888; font-size: 12px; font-style: italic;">No has agregado extras opcionales a este plato.</div>';
              return;
            }

            container.innerHTML = currentDishExtras.map((extra, idx) => `
              <div style="display: flex; justify-content: space-between; align-items: center; background: #1C1C24; padding: 6px 10px; border-radius: 6px; border-left: 3px solid #FFB74D;">
                <div>
                  <span style="font-weight: bold; color: white;">+ §§{extra.name}</span>
                  <span style="color: #FFB74D; font-size: 12px; margin-left: 8px;">+$§§{Number(extra.priceUsd).toFixed(2)} USD</span>
                </div>
                <button class="btn-secondary" style="width: auto; padding: 2px 8px; font-size: 11px; background: #5D101D; border-color: #B71C1C; color: #FFCDD2;" onclick="removeExtraFromCurrentDish(§§{idx})">
                  🗑️ Quitar
                </button>
              </div>
            `).join('');
          }

          function addExtraToCurrentDish() {
            const name = document.getElementById('newExtraNameInput').value.trim();
            const price = parseFloat(document.getElementById('newExtraPriceInput').value);
            if (!name) return alert('Ingresa el nombre del extra (ej: Tocineta Extra).');
            if (isNaN(price) || price < 0) return alert('Ingresa un precio válido en USD (puede ser 0 para gratis).');

            currentDishExtras.push({
              name: name,
              priceUsd: price
            });
            document.getElementById('newExtraNameInput').value = '';
            document.getElementById('newExtraPriceInput').value = '';
            renderDishExtrasList();
          }

          function removeExtraFromCurrentDish(idx) {
            currentDishExtras.splice(idx, 1);
            renderDishExtrasList();
          }

          async function saveCurrentDish() {
            const name = document.getElementById('dishNameInput').value.trim();
            const category = document.getElementById('dishCategoryInput').value;
            const priceUsd = parseFloat(document.getElementById('dishPriceInput').value);
            const description = document.getElementById('dishDescInput').value.trim();
            const isAvailable = document.getElementById('dishAvailableInput').checked;

            if (!name) return alert('Por favor escribe el nombre del plato.');
            if (isNaN(priceUsd) || priceUsd < 0) return alert('Por favor escribe un precio válido en USD.');

            const payload = {
              id: currentEditingDish ? currentEditingDish.id : '',
              name: name,
              category: category,
              priceUsd: priceUsd,
              description: description,
              isAvailable: isAvailable,
              ingredientsJson: JSON.stringify(currentDishIngredients),
              extrasJson: JSON.stringify(currentDishExtras)
            };

            try {
              const res = await fetch('/api/menu/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
              });
              const data = await res.json();
              if (data.success) {
                closeDishModal();
                alert(`¡Plato "§§{name}" guardado exitosamente en el menú y sincronizado con la nube!`);
                loadData();
              } else {
                alert('Error al guardar plato: ' + data.error);
              }
            } catch (e) {
              alert('Error de conexión: ' + e.message);
            }
          }

          async function deleteDishPrompt(dishId, dishName) {
            if (!confirm(`¿Estás seguro de eliminar el plato "§§{dishName}" del menú? Dejará de aparecer en las ventas y en la aplicación.`)) return;
            try {
              const res = await fetch('/api/menu/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id: dishId })
              });
              const data = await res.json();
              if (data.success) {
                alert(`Plato "§§{dishName}" eliminado del menú.`);
                loadData();
              } else {
                alert('Error al eliminar plato: ' + data.error);
              }
            } catch (e) {
              alert('Error de conexión: ' + e.message);
            }
          }

          // Iniciar
          loadData();
          setInterval(loadData, 5000); // Sincronización en vivo cada 5 segundos
        </script>
      </body>
      </html>
    """.trimIndent().replace("§§", "$")
  }
}
