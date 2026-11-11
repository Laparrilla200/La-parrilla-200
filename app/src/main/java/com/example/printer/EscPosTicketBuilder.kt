package com.example.printer

import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.ClosureDoc
import com.example.data.model.OrderDoc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Constructor de comandos ESC/POS para impresión directa en impresoras térmicas (Bluetooth / Red IP)
 * con soporte para anchos de 58mm (32 columnas) y 80mm (48 columnas), alineaciones y corte de papel.
 */
object EscPosTicketBuilder {

  private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

  /**
   * Genera el flujo binario ESC/POS para el recibo de venta / factura de cliente.
   */
  fun buildOrderReceiptBytes(
    order: OrderDoc,
    items: List<CartOrderItem>,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): ByteArray {
    val builder = EscPosBuilder(paperWidth)
    val dateStr = dateFormat.format(Date(order.timestamp))
    val businessName = config.businessName.ifBlank { "LA PARRILLA 200" }

    // Encabezado centrado
    builder.align(EscAlign.CENTER)
    builder.bold(true).doubleSize(true).textLine(businessName)
    builder.doubleSize(false)
    if (config.rif.isNotBlank()) builder.textLine("RIF: ${config.rif}")
    if (config.phone.isNotBlank()) builder.textLine("Telf: ${config.phone}")
    if (config.address.isNotBlank()) builder.textLine(config.address)
    builder.bold(false)

    builder.divider('=')

    // Información de Orden
    builder.row("TICKET: #${String.format(Locale.US, "%04d", order.orderNumber)}", order.tableOrDestination)
    builder.row("CLIENTE: ${order.customerName.take(18)}", "")
    builder.row("FECHA: $dateStr", "")
    if (order.createdBy.isNotBlank()) {
      builder.row("OPERADOR: ${order.createdBy.take(18)}", "")
    }

    builder.divider('-')
    builder.row("CANT DESCRIPCION", "TOTAL($)")
    builder.divider('-')

    // Platos
    for (item in items) {
      val itemTotal = item.priceUsd * item.quantity
      val nameLine = "${item.quantity}x ${item.dishName}"
      val priceLine = String.format(Locale.US, "$%.2f", itemTotal)
      val maxNameLen = builder.columns - priceLine.length - 1

      builder.bold(true)
      builder.row(nameLine.take(maxNameLen), priceLine)
      builder.bold(false)

      // Insumos modificados / notas
      if (item.excludedIngredientNames.isNotEmpty()) {
        for (excluded in item.excludedIngredientNames) {
          builder.textLine("   [!] SIN: ${excluded.uppercase()}")
        }
      }
      if (item.specialInstructions.isNotBlank()) {
        builder.textLine("   * NOTA: ${item.specialInstructions}")
      }
    }

    builder.divider('-')

    // Totales
    builder.bold(true)
    builder.row("TOTAL DOLARES:", String.format(Locale.US, "$%.2f", order.totalUsd))
    builder.bold(false)
    builder.row("TASA DEL DIA:", String.format(Locale.US, "%.2f Bs/$", order.exchangeRate))
    builder.bold(true)
    builder.row("TOTAL EN BS:", String.format(Locale.US, "%.2f Bs", order.totalBs))
    builder.bold(false)

    builder.divider('-')

    // Sección de Pago
    builder.align(EscAlign.CENTER).bold(true).textLine("PAGO Y DETALLE").bold(false)
    val metodoName = when (order.paymentMethod) {
      "USD_EFECTIVO" -> "EFECTIVO DOLARES"
      "PAGO_MOVIL" -> "PAGO MOVIL (Bs)"
      "PUNTO_VENTA" -> "PUNTO DE VENTA (Bs)"
      "MIXTO" -> "PAGO MIXTO"
      else -> "PENDIENTE"
    }
    builder.row("METODO:", metodoName)
    if (order.amountUsdPaid > 0) {
      builder.row("USD EFECTIVO:", String.format(Locale.US, "$%.2f", order.amountUsdPaid))
    }
    if (order.amountPagoMovilBs > 0) {
      builder.row("PAGO MOVIL:", String.format(Locale.US, "%.2f Bs", order.amountPagoMovilBs))
    }
    if (order.amountPuntoVentaBs > 0) {
      builder.row("PUNTO VENTA:", String.format(Locale.US, "%.2f Bs", order.amountPuntoVentaBs))
    }
    if (order.referenceCode.isNotBlank()) {
      builder.row("REF / LOTE:", order.referenceCode)
    }
    builder.row("ESTADO:", if (order.isPaid) "PAGADO - CONFORME" else "PENDIENTE")

    builder.divider('=')

    // Pie de página
    if (config.ticketFooter.isNotBlank()) {
      for (line in config.ticketFooter.lines()) {
        builder.centerLine(line)
      }
    } else {
      builder.centerLine("GRACIAS POR SU PREFERENCIA")
    }
    builder.centerLine("*** TICKET TERMICO ${paperWidth.label.take(4)} ***")

    // Alimentar papel y cortar ordenadamente
    builder.cut()

    return builder.build()
  }

  /**
   * Genera el flujo binario ESC/POS para la comanda de cocina.
   */
  fun buildKitchenTicketBytes(
    order: OrderDoc,
    items: List<CartOrderItem>,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): ByteArray {
    val builder = EscPosBuilder(paperWidth)
    val dateStr = dateFormat.format(Date(order.timestamp))

    builder.divider('*')
    builder.align(EscAlign.CENTER)
    builder.bold(true).doubleSize(true).textLine("*** COMANDA COCINA ***")
    builder.doubleSize(false).bold(false)
    builder.divider('*')

    builder.bold(true)
    builder.row("ORDEN: #${String.format(Locale.US, "%04d", order.orderNumber)}", order.tableOrDestination)
    builder.bold(false)
    builder.row("HORA: $dateStr", "")
    if (order.customerName.isNotBlank()) {
      builder.row("CLIENTE: ${order.customerName}", "")
    }

    builder.divider('=')
    builder.centerLine("PLATOS A PREPARAR")
    builder.divider('=')

    for (item in items) {
      builder.bold(true).doubleSize(true)
      builder.textLine(">> [ ${item.quantity}x ] ${item.dishName.uppercase()}")
      builder.doubleSize(false).bold(false)

      if (item.excludedIngredientNames.isNotEmpty()) {
        builder.bold(true)
        builder.textLine("   >>> ATENCION MODIFICADO:")
        for (excluded in item.excludedIngredientNames) {
          builder.textLine("   [-] SIN ${excluded.uppercase()}")
        }
        builder.bold(false)
      }

      if (item.specialInstructions.isNotBlank()) {
        builder.textLine("   [!] NOTA: ${item.specialInstructions.uppercase()}")
      }
      builder.divider('-')
    }

    if (order.notes.isNotBlank()) {
      builder.bold(true).textLine("OBSERVACION GENERAL:").bold(false)
      builder.textLine(order.notes)
      builder.divider('-')
    }

    builder.centerLine("DESPACHO RAPIDO LA PARRILLA")
    builder.divider('*')

    builder.cut()
    return builder.build()
  }

  /**
   * Genera el flujo binario ESC/POS para el cierre de caja.
   */
  fun buildClosureReceiptBytes(
    closure: ClosureDoc,
    config: BusinessConfigDoc,
    paperWidth: PaperWidth = PaperWidth.WIDTH_58MM
  ): ByteArray {
    val builder = EscPosBuilder(paperWidth)
    val dateStr = dateFormat.format(Date(closure.timestamp))
    val startStr = dateFormat.format(Date(closure.startDate))
    val endStr = dateFormat.format(Date(closure.endDate))
    val businessName = config.businessName.ifBlank { "LA PARRILLA 200" }

    builder.divider('=')
    builder.align(EscAlign.CENTER)
    builder.bold(true).doubleSize(true).textLine(businessName)
    builder.doubleSize(false)
    builder.textLine("CIERRE DE CAJA ${closure.closureType}")
    builder.bold(false)
    builder.divider('=')

    builder.row("CIERRE NRO:", "#${String.format(Locale.US, "%04d", closure.closureNumber)}")
    builder.row("EMISION:", dateStr)
    builder.row("DESDE:", startStr)
    builder.row("HASTA:", endStr)
    builder.row("TOTAL PEDIDOS:", "${closure.orderCount}")
    if (closure.closedBy.isNotBlank()) {
      builder.row("RESPONSABLE:", closure.closedBy.take(18))
    }

    builder.divider('-')
    builder.centerLine("DESGLOSE POR METODO DE PAGO")
    builder.divider('-')

    builder.row("EFECTIVO DOLARES ($):", String.format(Locale.US, "$%.2f", closure.totalUsdCash))
    builder.row("PAGO MOVIL (Bs):", String.format(Locale.US, "%.2f Bs", closure.totalPagoMovilBs))
    builder.row("PUNTO DE VENTA (Bs):", String.format(Locale.US, "%.2f Bs", closure.totalPuntoVentaBs))

    builder.divider('-')
    val totalDigital = closure.totalPagoMovilBs + closure.totalPuntoVentaBs
    builder.row("TOTAL BS (P.MOVIL+PUNTO):", String.format(Locale.US, "%.2f Bs", totalDigital))

    builder.bold(true)
    builder.row("TOTAL GENERAL BS:", String.format(Locale.US, "%.2f Bs", closure.totalSalesBs))
    builder.row("TOTAL GENERAL USD:", String.format(Locale.US, "$%.2f", closure.totalSalesUsd))
    builder.bold(false)

    builder.divider('=')

    if (closure.notes.isNotBlank()) {
      builder.textLine("OBSERVACIONES:")
      builder.textLine(closure.notes)
      builder.divider('-')
    }

    builder.feed(2)
    builder.centerLine("____________________________")
    builder.centerLine("FIRMA RESPONSABLE / ADMIN")
    builder.centerLine("AUDITORIA CONFORME")
    builder.divider('=')

    builder.cut()
    return builder.build()
  }
}
