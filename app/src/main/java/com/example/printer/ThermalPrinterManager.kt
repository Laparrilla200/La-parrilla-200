package com.example.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/**
 * Gestor integral de impresión para La Parrilla 200:
 * - Impresión mediante WebView/Android PrintManager con inyección de CSS @media print (sin márgenes, sin barras).
 * - Impresión directa por Red Local (TCP socket puerto 9100) con comandos ESC/POS y corte automático.
 * - Impresión directa por Bluetooth térmico (RFCOMM SPP) con comandos ESC/POS.
 */
object ThermalPrinterManager {

  private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  private const val PREFS_NAME = "la_parrilla_printer_prefs"
  private const val KEY_PAPER_WIDTH = "selected_paper_width"
  private const val KEY_NETWORK_IP = "printer_network_ip"
  private const val KEY_NETWORK_PORT = "printer_network_port"
  private const val KEY_BT_ADDRESS = "printer_bt_address"

  /**
   * Guarda el ancho de papel preferido (58mm o 80mm)
   */
  fun savePaperWidth(context: Context, paperWidth: PaperWidth) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_PAPER_WIDTH, paperWidth.name).apply()
  }

  /**
   * Obtiene el ancho de papel preferido
   */
  fun getPaperWidth(context: Context): PaperWidth {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val name = prefs.getString(KEY_PAPER_WIDTH, PaperWidth.WIDTH_58MM.name) ?: PaperWidth.WIDTH_58MM.name
    return try {
      PaperWidth.valueOf(name)
    } catch (_: Exception) {
      PaperWidth.WIDTH_58MM
    }
  }

  fun savePrinterIp(context: Context, ip: String, port: Int = 9100) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_NETWORK_IP, ip).putInt(KEY_NETWORK_PORT, port).apply()
  }

  fun getPrinterIp(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_NETWORK_IP, "192.168.1.100") ?: "192.168.1.100"
  }

  fun getPrinterPort(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_NETWORK_PORT, 9100)
  }

  /**
   * Imprime el HTML en la cola de impresión de Android utilizando un WebView invisible
   * e inyectando las dimensiones exactas y márgenes 0 con @media print.
   */
  fun printHtmlTicket(
    context: Context,
    jobTitle: String,
    htmlContent: String,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ) {
    Handler(Looper.getMainLooper()).post {
      try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
          Toast.makeText(context, "Servicio de impresión no disponible", Toast.LENGTH_SHORT).show()
          return@post
        }

        val webView = WebView(context)
        webView.settings.javaScriptEnabled = false
        webView.webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            val printAdapter = webView.createPrintDocumentAdapter(jobTitle)

            val widthMils = if (paperWidth == PaperWidth.WIDTH_80MM) 3150 else 2283
            val lengthMils = 25000 // Rollo continuo de hasta 635mm para imprimir comanda completa
            val mediaSize = PrintAttributes.MediaSize("THERMAL_ROLL", "Rollo Térmico Continuo", widthMils, lengthMils)

            val printAttributes = PrintAttributes.Builder()
              .setMediaSize(mediaSize)
              .setResolution(PrintAttributes.Resolution("thermal", "Impresora Térmica", 203, 203))
              .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
              .build()

            printManager.print(jobTitle, printAdapter, printAttributes)
          }
        }
        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "utf-8", null)
      } catch (e: Exception) {
        Toast.makeText(context, "Error al preparar ticket: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  /**
   * Envía comandos binarios ESC/POS directamente a una impresora térmica de red (TCP port 9100).
   */
  suspend fun printViaNetwork(
    ip: String,
    port: Int = 9100,
    escPosBytes: ByteArray,
    timeoutMs: Int = 4000
  ): Result<String> = withContext(Dispatchers.IO) {
    var socket: Socket? = null
    var out: OutputStream? = null
    try {
      socket = Socket()
      socket.connect(InetSocketAddress(ip, port), timeoutMs)
      socket.soTimeout = timeoutMs
      out = socket.getOutputStream()
      out.write(escPosBytes)
      out.flush()
      Result.success("Ticket enviado con éxito a la impresora ($ip:$port)")
    } catch (e: Exception) {
      Result.failure(Exception("Error al conectar con impresora térmica en $ip:$port: ${e.localizedMessage}"))
    } finally {
      try { out?.close() } catch (_: Exception) {}
      try { socket?.close() } catch (_: Exception) {}
    }
  }

  /**
   * Envía comandos binarios ESC/POS a una impresora térmica conectada por Bluetooth (SPP).
   */
  suspend fun printViaBluetooth(
    macAddress: String,
    escPosBytes: ByteArray
  ): Result<String> = withContext(Dispatchers.IO) {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
      ?: return@withContext Result.failure(Exception("Bluetooth no está disponible en este dispositivo"))

    if (!bluetoothAdapter.isEnabled) {
      return@withContext Result.failure(Exception("Bluetooth está desactivado. Actívalo para imprimir"))
    }

    var socket: BluetoothSocket? = null
    var out: OutputStream? = null
    try {
      val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress)
      bluetoothAdapter.cancelDiscovery()
      socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
      socket.connect()
      out = socket.outputStream
      out.write(escPosBytes)
      out.flush()
      Result.success("Ticket enviado con éxito por Bluetooth a ${device.name ?: macAddress}")
    } catch (e: Exception) {
      Result.failure(Exception("Error en conexión Bluetooth con $macAddress: ${e.localizedMessage}"))
    } finally {
      try { out?.close() } catch (_: Exception) {}
      try { socket?.close() } catch (_: Exception) {}
    }
  }

  /**
   * Obtiene la lista de impresoras Bluetooth ya vinculadas.
   */
  fun getPairedBluetoothPrinters(): List<Pair<String, String>> {
    return try {
      val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
      if (!adapter.isEnabled) return emptyList()
      adapter.bondedDevices?.map { device ->
        (device.name ?: "Impresora Desconocida") to device.address
      } ?: emptyList()
    } catch (_: SecurityException) {
      emptyList()
    } catch (_: Exception) {
      emptyList()
    }
  }

  /**
   * Comparte el texto plano del ticket mediante Intent de Android (WhatsApp, Email, etc.).
   */
  fun shareTicketText(context: Context, title: String, text: String) {
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TITLE, title)
      putExtra(Intent.EXTRA_TEXT, text)
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Enviar o Compartir Ticket Térmico")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
  }
}
