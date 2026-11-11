package com.example.printer

import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.ClosureDoc
import com.example.data.model.OrderDoc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generador de tickets HTML con inyección de CSS `@media print` optimizado para impresoras
 * térmicas de 58mm y 80mm, eliminando márgenes de navegador, barras y ajustando ancho fijo.
 */
object HtmlTicketGenerator {

  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

  /**
   * Genera el HTML completo para un ticket de venta de cliente.
   */
  fun generateOrderReceiptHtml(
    order: OrderDoc,
    items: List<CartOrderItem>,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): String {
    val dateStr = dateFormat.format(Date(order.timestamp))
    val widthMm = paperWidth.mmWidth
    val printableMm = paperWidth.printWidthMm
    val fontSize = if (paperWidth == PaperWidth.WIDTH_58MM) "8.5pt" else "10pt"
    val businessName = config.businessName.ifBlank { "LA PARRILLA 200" }

    val itemsHtml = buildString {
      for (item in items) {
        val itemTotal = item.priceUsd * item.quantity
        append("""
          <tr>
            <td class="col-qty">${item.quantity}x</td>
            <td class="col-desc">
              <span class="bold">${escape(item.dishName)}</span>
        """)
        if (item.excludedIngredientNames.isNotEmpty()) {
          append("""<div class="tag-mod">[-] SIN: ${escape(item.excludedIngredientNames.joinToString(", "))}</div>""")
        }
        if (item.specialInstructions.isNotBlank()) {
          append("""<div class="tag-note">Nota: ${escape(item.specialInstructions)}</div>""")
        }
        append("""
            </td>
            <td class="col-price">$${String.format(Locale.US, "%.2f", itemTotal)}</td>
          </tr>
        """)
      }
    }

    val paymentMethodName = when (order.paymentMethod) {
      "USD_EFECTIVO" -> "Efectivo Dolares ($)"
      "PAGO_MOVIL" -> "Pago Movil (Bs)"
      "PUNTO_VENTA" -> "Punto de Venta (Bs)"
      "MIXTO" -> "Pago Mixto"
      else -> "Pendiente"
    }

    val paymentDetailsHtml = buildString {
      append("""<div class="row"><span>Metodo:</span><span class="bold">$paymentMethodName</span></div>""")
      if (order.amountUsdPaid > 0) {
        append("""<div class="row"><span>USD Efectivo:</span><span>$${String.format(Locale.US, "%.2f", order.amountUsdPaid)}</span></div>""")
      }
      if (order.amountPagoMovilBs > 0) {
        append("""<div class="row"><span>Pago Movil:</span><span>${String.format(Locale.US, "%.2f", order.amountPagoMovilBs)} Bs</span></div>""")
      }
      if (order.amountPuntoVentaBs > 0) {
        append("""<div class="row"><span>Punto de Venta:</span><span>${String.format(Locale.US, "%.2f", order.amountPuntoVentaBs)} Bs</span></div>""")
      }
      if (order.referenceCode.isNotBlank()) {
        append("""<div class="row"><span>Ref / Lote:</span><span class="bold">${escape(order.referenceCode)}</span></div>""")
      }
      append("""<div class="row"><span>Estado:</span><span class="bold">${if (order.isPaid) "PAGADO" else "POR COBRAR"}</span></div>""")
    }

    return """
      <!DOCTYPE html>
      <html lang="es">
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Ticket #${String.format(Locale.US, "%04d", order.orderNumber)}</title>
        <style>
          ${buildPrintCss(widthMm, printableMm, fontSize)}
        </style>
      </head>
      <body>
        <div class="ticket">
          <div class="header">
            <div class="title bold">${escape(businessName)}</div>
            ${if (config.rif.isNotBlank()) "<div>RIF: ${escape(config.rif)}</div>" else ""}
            ${if (config.phone.isNotBlank()) "<div>Telf: ${escape(config.phone)}</div>" else ""}
            ${if (config.address.isNotBlank()) "<div>${escape(config.address)}</div>" else ""}
          </div>

          <div class="divider-double"></div>

          <div class="meta-info">
            <div class="row"><span class="bold">TICKET: #${String.format(Locale.US, "%04d", order.orderNumber)}</span><span class="bold">${escape(order.tableOrDestination)}</span></div>
            <div class="row"><span>Cliente: ${escape(order.customerName)}</span></div>
            <div class="row"><span>Fecha: $dateStr</span></div>
            ${if (order.createdBy.isNotBlank()) """<div class="row"><span>Atendido por: ${escape(order.createdBy)}</span></div>""" else ""}
          </div>

          <div class="divider-dashed"></div>

          <table class="items-table">
            <thead>
              <tr>
                <th class="col-qty">Cant</th>
                <th class="col-desc">Descripcion</th>
                <th class="col-price">Total</th>
              </tr>
            </thead>
            <tbody>
              $itemsHtml
            </tbody>
          </table>

          <div class="divider-dashed"></div>

          <div class="totals-section">
            <div class="row total-row"><span class="bold">TOTAL DOLARES:</span><span class="bold double-text">$${String.format(Locale.US, "%.2f", order.totalUsd)}</span></div>
            <div class="row"><span>Tasa del dia:</span><span>${String.format(Locale.US, "%.2f", order.exchangeRate)} Bs/$</span></div>
            <div class="row"><span class="bold">TOTAL EN BS:</span><span class="bold">${String.format(Locale.US, "%.2f", order.totalBs)} Bs</span></div>
          </div>

          <div class="divider-dashed"></div>

          <div class="payment-section">
            <div class="center-text bold" style="margin-bottom: 2px;">DETALLE DE PAGO</div>
            $paymentDetailsHtml
          </div>

          <div class="divider-double"></div>

          <div class="footer">
            ${if (config.ticketFooter.isNotBlank()) "<div>${escape(config.ticketFooter)}</div>" else "<div>Gracias por su preferencia</div>"}
            <div style="margin-top: 4px; font-size: 0.8em; color: #555;">*** TICKET TERMICO ${paperWidth.label.take(4)} ***</div>
          </div>
        </div>
      </body>
      </html>
    """.trimIndent()
  }

  /**
   * Genera el HTML de la comanda para cocina.
   */
  fun generateKitchenTicketHtml(
    order: OrderDoc,
    items: List<CartOrderItem>,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): String {
    val dateStr = dateFormat.format(Date(order.timestamp))
    val widthMm = paperWidth.mmWidth
    val printableMm = paperWidth.printWidthMm
    val fontSize = if (paperWidth == PaperWidth.WIDTH_58MM) "9pt" else "11pt"

    val itemsHtml = buildString {
      for (item in items) {
        append("""
          <div class="kitchen-item">
            <div class="row">
              <span class="kitchen-qty bold">[ ${item.quantity}x ]</span>
              <span class="kitchen-name bold">${escape(item.dishName.uppercase())}</span>
            </div>
        """)
        if (item.excludedIngredientNames.isNotEmpty()) {
          append("""
            <div class="kitchen-alert bold">
              >>> ATENCION MODIFICADO:<br>
              ${item.excludedIngredientNames.joinToString("<br>") { "[-] SIN " + escape(it.uppercase()) }}
            </div>
          """)
        }
        if (item.specialInstructions.isNotBlank()) {
          append("""<div class="kitchen-note bold">[!] NOTA: ${escape(item.specialInstructions.uppercase())}</div>""")
        }
        append("""<div class="divider-dashed"></div></div>""")
      }
    }

    return """
      <!DOCTYPE html>
      <html lang="es">
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Comanda #${String.format(Locale.US, "%04d", order.orderNumber)}</title>
        <style>
          ${buildPrintCss(widthMm, printableMm, fontSize)}
          .kitchen-item { margin: 6px 0; }
          .kitchen-qty { font-size: 1.25em; margin-right: 4px; }
          .kitchen-name { font-size: 1.15em; }
          .kitchen-alert { background: #eee; padding: 2px 4px; margin: 3px 0; font-size: 0.95em; border-left: 3px solid #000; }
          .kitchen-note { padding: 2px 4px; margin: 2px 0; font-size: 0.9em; font-style: italic; }
        </style>
      </head>
      <body>
        <div class="ticket">
          <div class="header">
            <div class="title bold double-text">*** COMANDA COCINA ***</div>
            <div class="row" style="margin-top: 4px;"><span class="bold double-text">#${String.format(Locale.US, "%04d", order.orderNumber)}</span><span class="bold double-text">${escape(order.tableOrDestination)}</span></div>
            <div class="row"><span>Hora: $dateStr</span></div>
            ${if (order.customerName.isNotBlank()) """<div class="row"><span>Cliente: ${escape(order.customerName)}</span></div>""" else ""}
          </div>

          <div class="divider-double"></div>

          <div class="items-list">
            $itemsHtml
          </div>

          ${if (order.notes.isNotBlank()) """
            <div class="divider-dashed"></div>
            <div class="bold">OBSERVACION GENERAL:</div>
            <div>${escape(order.notes)}</div>
          """ else ""}

          <div class="divider-double"></div>
          <div class="center-text bold">LA PARRILLA 200 - DESPACHO RAPIDO</div>
        </div>
      </body>
      </html>
    """.trimIndent()
  }

  /**
   * Genera el HTML del reporte de Cierre de Caja.
   */
  fun generateClosureReceiptHtml(
    closure: ClosureDoc,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): String {
    val dateStr = dateFormat.format(Date(closure.timestamp))
    val startStr = dateFormat.format(Date(closure.startDate))
    val endStr = dateFormat.format(Date(closure.endDate))
    val widthMm = paperWidth.mmWidth
    val printableMm = paperWidth.printWidthMm
    val fontSize = if (paperWidth == PaperWidth.WIDTH_58MM) "8.5pt" else "10pt"
    val businessName = config.businessName.ifBlank { "LA PARRILLA 200" }

    val totalDigitalBs = closure.totalPagoMovilBs + closure.totalPuntoVentaBs

    return """
      <!DOCTYPE html>
      <html lang="es">
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Cierre #${String.format(Locale.US, "%04d", closure.closureNumber)}</title>
        <style>
          ${buildPrintCss(widthMm, printableMm, fontSize)}
        </style>
      </head>
      <body>
        <div class="ticket">
          <div class="header">
            <div class="title bold">${escape(businessName)}</div>
            <div class="bold double-text">CIERRE DE CAJA ${closure.closureType}</div>
            <div class="row"><span>Cierre Nro: #${String.format(Locale.US, "%04d", closure.closureNumber)}</span></div>
            <div class="row"><span>Emision: $dateStr</span></div>
            <div class="row"><span>Desde: $startStr</span></div>
            <div class="row"><span>Hasta: $endStr</span></div>
            <div class="row"><span>Total Pedidos: ${closure.orderCount}</span></div>
            ${if (closure.closedBy.isNotBlank()) """<div class="row"><span>Responsable: ${escape(closure.closedBy)}</span></div>""" else ""}
          </div>

          <div class="divider-double"></div>
          <div class="center-text bold">DESGLOSE POR METODO DE PAGO</div>
          <div class="divider-dashed"></div>

          <div class="row"><span>Efectivo Dolares ($):</span><span class="bold">$${String.format(Locale.US, "%.2f", closure.totalUsdCash)}</span></div>
          <div class="row"><span>Pago Movil (Bs):</span><span class="bold">${String.format(Locale.US, "%.2f", closure.totalPagoMovilBs)} Bs</span></div>
          <div class="row"><span>Punto de Venta (Bs):</span><span class="bold">${String.format(Locale.US, "%.2f", closure.totalPuntoVentaBs)} Bs</span></div>

          <div class="divider-dashed"></div>

          <div class="row"><span>Total Bs (Digital):</span><span class="bold">${String.format(Locale.US, "%.2f", totalDigitalBs)} Bs</span></div>
          <div class="row total-row"><span class="bold">TOTAL GENERAL BS:</span><span class="bold">${String.format(Locale.US, "%.2f", closure.totalSalesBs)} Bs</span></div>
          <div class="row total-row"><span class="bold">TOTAL GENERAL USD:</span><span class="bold double-text">$${String.format(Locale.US, "%.2f", closure.totalSalesUsd)}</span></div>

          <div class="divider-double"></div>

          ${if (closure.notes.isNotBlank()) """
            <div>Observaciones: ${escape(closure.notes)}</div>
            <div class="divider-dashed"></div>
          """ else ""}

          <div style="margin-top: 25px; text-align: center;">
            <div style="border-bottom: 1px solid #000; width: 70%; margin: 0 auto 5px auto;"></div>
            <div>FIRMA RESPONSABLE / ADMIN</div>
            <div style="font-size: 0.8em; color: #555;">Auditoria Conforme</div>
          </div>
        </div>
      </body>
      </html>
    """.trimIndent()
  }

  /**
   * Construye el CSS inyectado con @media print que elimina encabezados, márgenes, barras
   * y ajusta con precisión el ancho térmico fijo al ticket.
   */
  private fun buildPrintCss(widthMm: Int, printableMm: Int, fontSize: String): String {
    return """
      @page {
        size: ${widthMm}mm auto;
        margin: 0mm !important;
      }

      @media print {
        html, body {
          width: ${widthMm}mm !important;
          max-width: ${widthMm}mm !important;
          margin: 0 !important;
          padding: 0 !important;
          background: #ffffff !important;
          color: #000000 !important;
          -webkit-print-color-adjust: exact !important;
          print-color-adjust: exact !important;
        }
        .ticket {
          width: ${printableMm}mm !important;
          max-width: ${printableMm}mm !important;
          margin: 0 !important;
          padding: 0 !important;
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

      html, body {
        margin: 0;
        padding: 0;
        background-color: #ffffff;
        font-family: 'Courier New', Courier, monospace;
        font-size: $fontSize;
        line-height: 1.2;
        color: #000000;
        -webkit-text-size-adjust: 100%;
      }

      .ticket {
        width: ${printableMm}mm;
        max-width: ${printableMm}mm;
        margin: 0;
        padding: 0;
        background-color: #ffffff;
        color: #000000;
      }

      .header {
        text-align: center;
        margin-bottom: 3px;
        padding: 0;
      }

      .title {
        font-size: 1.25em;
        font-weight: 900;
        text-transform: uppercase;
      }

      .bold {
        font-weight: 900;
      }

      .double-text {
        font-size: 1.2em;
        font-weight: 900;
      }

      .row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        width: 100%;
        margin: 1px 0;
      }

      .divider-double {
        border-top: 2px solid #000000;
        margin: 3px 0;
      }

      .divider-dashed {
        border-top: 1px dashed #000000;
        margin: 3px 0;
      }

      .items-table {
        width: 100%;
        border-collapse: collapse;
        margin: 2px 0;
      }

      .items-table th {
        border-top: 1px dashed #000000;
        border-bottom: 1px dashed #000000;
        padding: 2px 0;
        font-weight: 900;
        text-align: left;
      }

      .items-table td {
        padding: 2px 0;
        vertical-align: top;
      }

      .col-qty {
        width: 15%;
        text-align: left;
        font-weight: 900;
      }

      .col-desc {
        width: 60%;
        text-align: left;
        word-break: break-word;
      }

      .col-price {
        width: 25%;
        text-align: right;
        white-space: nowrap;
        font-weight: 900;
      }

      .tag-mod {
        font-size: 0.9em;
        font-weight: 900;
        margin-top: 1px;
      }

      .tag-note {
        font-size: 0.9em;
        font-weight: 900;
        margin-top: 1px;
      }

      .total-row {
        font-size: 1.15em;
        font-weight: 900;
        margin: 2px 0;
      }

      .center-text {
        text-align: center;
      }

      .footer {
        text-align: center;
        margin-top: 4px;
      }
    """.trimIndent()
  }

  private fun escape(text: String): String {
    return text.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }
}
