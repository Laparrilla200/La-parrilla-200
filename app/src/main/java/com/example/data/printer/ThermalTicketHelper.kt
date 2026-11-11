package com.example.data.printer

import android.content.Context
import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.ClosureDoc
import com.example.data.model.OrderDoc
import com.example.printer.EscPosTicketBuilder
import com.example.printer.HtmlTicketGenerator
import com.example.printer.PaperWidth
import com.example.printer.ThermalPrinterManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ayudante de Tickets Térmicos de La Parrilla 200 con soporte para rollos de 58mm y 80mm,
 * WebView Print con @media print (cero márgenes, sin barras) y comandos nativos ESC/POS.
 */
object ThermalTicketHelper {

  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  private val currencyFormat = Locale.US

  private fun center(text: String, width: Int): String {
    if (text.length >= width) return text.take(width)
    val leftPadding = (width - text.length) / 2
    val rightPadding = width - text.length - leftPadding
    return " ".repeat(leftPadding) + text + " ".repeat(rightPadding)
  }

  private fun row(left: String, right: String, width: Int): String {
    val space = width - left.length - right.length
    return if (space > 0) {
      left + " ".repeat(space) + right
    } else {
      left.take(width - right.length - 1) + " " + right
    }
  }

  private fun divider(char: Char = '-', width: Int): String {
    return char.toString().repeat(width)
  }

  /**
   * Genera el texto en formato monoespaciado para el Ticket de Venta (58mm o 80mm).
   */
  fun generateOrderReceiptText(
    order: OrderDoc,
    items: List<CartOrderItem>,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): String {
    val width = paperWidth.columns
    val sb = StringBuilder()
    val dateStr = dateFormat.format(Date(order.timestamp))

    sb.appendLine(divider('=', width))
    sb.appendLine(center(config.businessName.ifBlank { "LA PARRILLA 200" }, width))
    if (config.rif.isNotBlank()) sb.appendLine(center("RIF: ${config.rif}", width))
    if (config.phone.isNotBlank()) sb.appendLine(center("Telf: ${config.phone}", width))
    if (config.address.isNotBlank()) sb.appendLine(center(config.address, width))
    sb.appendLine(divider('=', width))

    sb.appendLine(row("TICKET: #${String.format(Locale.US, "%04d", order.orderNumber)}", order.tableOrDestination, width))
    sb.appendLine(row("CLIENTE: ${order.customerName.take(width - 10)}", "", width))
    sb.appendLine(row("FECHA: $dateStr", "", width))
    if (order.createdBy.isNotBlank()) {
      sb.appendLine(row("OPERADOR: ${order.createdBy.take(width - 11)}", "", width))
    }
    sb.appendLine(divider('-', width))
    sb.appendLine(row("CANT DESCRIPCION", "TOTAL($)", width))
    sb.appendLine(divider('-', width))

    for (item in items) {
      val itemTotal = item.priceUsd * item.quantity
      val nameLine = "${item.quantity}x ${item.dishName}"
      val priceLine = String.format(currencyFormat, "$%.2f", itemTotal)
      val maxNameLen = width - priceLine.length - 1
      sb.appendLine(row(nameLine.take(maxNameLen), priceLine, width))

      if (item.excludedIngredientNames.isNotEmpty()) {
        for (excluded in item.excludedIngredientNames) {
          sb.appendLine("   [!] SIN: ${excluded.uppercase()}")
        }
      }
      if (item.specialInstructions.isNotBlank()) {
        sb.appendLine("   * NOTA: ${item.specialInstructions}")
      }
    }

    sb.appendLine(divider('-', width))
    sb.appendLine(row("TOTAL DOLARES:", String.format(currencyFormat, "$%.2f", order.totalUsd), width))
    sb.appendLine(row("TASA DEL DIA:", String.format(currencyFormat, "%.2f Bs/$", order.exchangeRate), width))
    sb.appendLine(row("TOTAL EN BS:", String.format(currencyFormat, "%.2f Bs", order.totalBs), width))
    sb.appendLine(divider('-', width))

    sb.appendLine(center("PAGO Y DETALLE", width))
    val metodoName = when (order.paymentMethod) {
      "USD_EFECTIVO" -> "EFECTIVO DOLARES"
      "PAGO_MOVIL" -> "PAGO MOVIL (Bs)"
      "PUNTO_VENTA" -> "PUNTO DE VENTA (Bs)"
      "MIXTO" -> "PAGO MIXTO"
      else -> "PENDIENTE"
    }
    sb.appendLine(row("METODO:", metodoName, width))
    if (order.amountUsdPaid > 0) {
      sb.appendLine(row("USD EFECTIVO:", String.format(currencyFormat, "$%.2f", order.amountUsdPaid), width))
    }
    if (order.amountPagoMovilBs > 0) {
      sb.appendLine(row("PAGO MOVIL:", String.format(currencyFormat, "%.2f Bs", order.amountPagoMovilBs), width))
    }
    if (order.amountPuntoVentaBs > 0) {
      sb.appendLine(row("PUNTO VENTA:", String.format(currencyFormat, "%.2f Bs", order.amountPuntoVentaBs), width))
    }
    if (order.referenceCode.isNotBlank()) {
      sb.appendLine(row("REF / LOTE:", order.referenceCode, width))
    }
    sb.appendLine(row("ESTADO:", if (order.isPaid) "PAGADO - CONFORME" else "PENDIENTE", width))
    sb.appendLine(divider('=', width))

    if (config.ticketFooter.isNotBlank()) {
      for (line in config.ticketFooter.lines()) {
        sb.appendLine(center(line, width))
      }
    } else {
      sb.appendLine(center("GRACIAS POR SU PREFERENCIA", width))
    }
    sb.appendLine(center("*** TICKET TERMICO ${paperWidth.label.take(4)} ***", width))
    sb.appendLine(divider('=', width))

    return sb.toString()
  }

  /**
   * Genera la comanda para el personal de cocina destacando insumos modificados.
   */
  fun generateKitchenTicketText(
    order: OrderDoc,
    items: List<CartOrderItem>,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): String {
    val width = paperWidth.columns
    val sb = StringBuilder()
    val dateStr = dateFormat.format(Date(order.timestamp))

    sb.appendLine(divider('*', width))
    sb.appendLine(center("*** COMANDA DE COCINA ***", width))
    sb.appendLine(divider('*', width))
    sb.appendLine(row("ORDEN: #${String.format(Locale.US, "%04d", order.orderNumber)}", order.tableOrDestination, width))
    sb.appendLine(row("HORA: $dateStr", "", width))
    if (order.customerName.isNotBlank()) {
      sb.appendLine(row("CLIENTE: ${order.customerName}", "", width))
    }
    sb.appendLine(divider('=', width))
    sb.appendLine(center("PLATOS A PREPARAR", width))
    sb.appendLine(divider('=', width))

    for (item in items) {
      sb.appendLine(">> [ ${item.quantity}x ] ${item.dishName.uppercase()}")
      if (item.excludedIngredientNames.isNotEmpty()) {
        sb.appendLine("   >>> ATENCION MODIFICADO:")
        for (excluded in item.excludedIngredientNames) {
          sb.appendLine("   [-] SIN ${excluded.uppercase()}")
        }
      }
      if (item.specialInstructions.isNotBlank()) {
        sb.appendLine("   [!] NOTA: ${item.specialInstructions.uppercase()}")
      }
      sb.appendLine(divider('-', width))
    }

    if (order.notes.isNotBlank()) {
      sb.appendLine("OBSERVACION GENERAL:")
      sb.appendLine(order.notes)
      sb.appendLine(divider('-', width))
    }
    sb.appendLine(center("DESPACHO RAPIDO LA PARRILLA", width))
    sb.appendLine(divider('*', width))
    return sb.toString()
  }

  /**
   * Genera el ticket de Cierre de Caja (Diario, Semanal, Mensual).
   */
  fun generateClosureReceiptText(
    closure: ClosureDoc,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): String {
    val width = paperWidth.columns
    val sb = StringBuilder()
    val dateStr = dateFormat.format(Date(closure.timestamp))
    val startStr = dateFormat.format(Date(closure.startDate))
    val endStr = dateFormat.format(Date(closure.endDate))

    sb.appendLine(divider('=', width))
    sb.appendLine(center(config.businessName.ifBlank { "LA PARRILLA 200" }, width))
    sb.appendLine(center("CIERRE DE CAJA ${closure.closureType}", width))
    sb.appendLine(divider('=', width))
    sb.appendLine(row("CIERRE NRO:", "#${String.format(Locale.US, "%04d", closure.closureNumber)}", width))
    sb.appendLine(row("EMISION:", dateStr, width))
    sb.appendLine(row("DESDE:", startStr, width))
    sb.appendLine(row("HASTA:", endStr, width))
    sb.appendLine(row("TOTAL PEDIDOS:", "${closure.orderCount}", width))
    if (closure.closedBy.isNotBlank()) {
      sb.appendLine(row("RESPONSABLE:", closure.closedBy.take(width - 14), width))
    }
    sb.appendLine(divider('-', width))
    sb.appendLine(center("DESGLOSE POR METODO DE PAGO", width))
    sb.appendLine(divider('-', width))

    sb.appendLine(row("EFECTIVO DOLARES ($):", String.format(currencyFormat, "$%.2f", closure.totalUsdCash), width))
    sb.appendLine(row("PAGO MOVIL (Bs):", String.format(currencyFormat, "%.2f Bs", closure.totalPagoMovilBs), width))
    sb.appendLine(row("PUNTO DE VENTA (Bs):", String.format(currencyFormat, "%.2f Bs", closure.totalPuntoVentaBs), width))
    sb.appendLine(divider('-', width))

    val totalBsDigital = closure.totalPagoMovilBs + closure.totalPuntoVentaBs
    sb.appendLine(row("TOTAL BS (P.MOVIL+PUNTO):", String.format(currencyFormat, "%.2f Bs", totalBsDigital), width))
    sb.appendLine(row("TOTAL GENERAL BS:", String.format(currencyFormat, "%.2f Bs", closure.totalSalesBs), width))
    sb.appendLine(row("TOTAL GENERAL USD:", String.format(currencyFormat, "$%.2f", closure.totalSalesUsd), width))
    sb.appendLine(divider('=', width))

    if (closure.notes.isNotBlank()) {
      sb.appendLine("OBSERVACIONES:")
      sb.appendLine(closure.notes)
      sb.appendLine(divider('-', width))
    }

    sb.appendLine("")
    sb.appendLine("FIRMA RESPONSABLE / ADMIN:")
    sb.appendLine("")
    sb.appendLine(divider('_', width))
    sb.appendLine(center("AUDITORIA CONFORME", width))
    sb.appendLine(divider('=', width))

    return sb.toString()
  }

  /**
   * Envía a la cola de impresión de Android (PrintManager) mediante un componente WebView invisible
   * con CSS @media print que elimina márgenes, cabeceras, barras de navegador y ajusta el ancho exacto del rollo.
   */
  fun printTicketViaPrintManager(
    context: Context,
    title: String,
    ticketText: String,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ) {
    val widthMm = paperWidth.mmWidth
    val printableMm = paperWidth.printWidthMm
    // Tamaño de letra optimizado mucho más grande pero sin desbordar el rollo
    val fontSize = if (paperWidth == PaperWidth.WIDTH_58MM) "11.5pt" else "13.5pt"

    val htmlContent = """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8">
        <title>$title</title>
        <style>
          @page {
            size: ${widthMm}mm auto;
            margin: 0mm !important;
          }
          @media print {
            html, body {
              width: 100% !important;
              max-width: ${printableMm}mm !important;
              margin: 0 auto !important;
              padding: 0 !important;
              background-color: #ffffff !important;
              color: #000000 !important;
              -webkit-print-color-adjust: exact !important;
              print-color-adjust: exact !important;
            }
            .ticket-body {
              width: 100% !important;
              max-width: ${printableMm}mm !important;
              margin: 0 auto !important;
              padding: 1mm 0 !important;
              box-sizing: border-box !important;
            }
            ::-webkit-scrollbar {
              display: none !important;
            }
            .no-print {
              display: none !important;
            }
          }
          * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
          }
          body {
            width: 100%;
            max-width: ${printableMm}mm;
            margin: 0 auto !important;
            padding: 0 !important;
            font-family: 'Courier New', Courier, monospace;
            font-size: $fontSize;
            font-weight: 900;
            line-height: 1.28;
            letter-spacing: 0.2px;
            color: #000000;
            background-color: #ffffff;
            white-space: pre-wrap;
            word-break: break-word;
            overflow-wrap: break-word;
            border: none !important;
          }
          .ticket-body {
            width: 100% !important;
            max-width: ${printableMm}mm !important;
            margin: 0 auto !important;
            padding: 1mm 0 !important;
            border: none !important;
            box-sizing: border-box !important;
          }
        </style>
      </head>
      <body>
        <div class="ticket-body">${escapeHtml(ticketText)}</div>
      </body>
      </html>
    """.trimIndent()

    ThermalPrinterManager.printHtmlTicket(
      context = context,
      jobTitle = title,
      htmlContent = htmlContent,
      paperWidth = paperWidth
    )
  }

  /**
   * Imprime directamente el ticket de orden con HTML formateado rico
   */
  fun printOrderReceiptHtml(
    context: Context,
    order: OrderDoc,
    items: List<CartOrderItem>,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ) {
    val html = HtmlTicketGenerator.generateOrderReceiptHtml(order, items, config, paperWidth)
    ThermalPrinterManager.printHtmlTicket(
      context = context,
      jobTitle = "Ticket #${order.orderNumber}",
      htmlContent = html,
      paperWidth = paperWidth
    )
  }

  /**
   * Imprime directamente la comanda de cocina con HTML formateado rico
   */
  fun printKitchenTicketHtml(
    context: Context,
    order: OrderDoc,
    items: List<CartOrderItem>,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ) {
    val html = HtmlTicketGenerator.generateKitchenTicketHtml(order, items, paperWidth)
    ThermalPrinterManager.printHtmlTicket(
      context = context,
      jobTitle = "Comanda #${order.orderNumber}",
      htmlContent = html,
      paperWidth = paperWidth
    )
  }

  /**
   * Genera los bytes ESC/POS listos para enviar a impresora térmica Bluetooth o de Red.
   */
  fun getOrderEscPosBytes(
    order: OrderDoc,
    items: List<CartOrderItem>,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): ByteArray {
    return EscPosTicketBuilder.buildOrderReceiptBytes(order, items, config, paperWidth)
  }

  fun getKitchenEscPosBytes(
    order: OrderDoc,
    items: List<CartOrderItem>,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): ByteArray {
    return EscPosTicketBuilder.buildKitchenTicketBytes(order, items, paperWidth)
  }

  fun getClosureEscPosBytes(
    closure: ClosureDoc,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): ByteArray {
    return EscPosTicketBuilder.buildClosureReceiptBytes(closure, config, paperWidth)
  }

  fun shareTicketText(context: Context, title: String, text: String) {
    ThermalPrinterManager.shareTicketText(context, title, text)
  }

  private fun escapeHtml(text: String): String {
    return text.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }
}
