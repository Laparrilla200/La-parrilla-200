package com.example

import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.OrderDoc
import com.example.data.printer.ThermalTicketHelper
import com.example.printer.EscAlign
import com.example.printer.EscPosBuilder
import com.example.printer.EscPosCommands
import com.example.printer.EscPosTicketBuilder
import com.example.printer.HtmlTicketGenerator
import com.example.printer.PaperWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPrinterTest {

  @Test
  fun testEscPosBuilder_basicCommandsAndCut() {
    val builder58 = EscPosBuilder(PaperWidth.WIDTH_58MM)
    builder58.align(EscAlign.CENTER)
      .bold(true)
      .doubleSize(true)
      .textLine("LA PARRILLA 200")
      .doubleSize(false)
      .bold(false)
      .divider('=')
      .row("ITEM 1", "$10.00")
      .feed(2)
      .cut()

    val bytes = builder58.build()
    assertNotNull(bytes)
    assertTrue("Bytes should not be empty", bytes.isNotEmpty())

    // Verificar presencia de corte de papel (GS V 66 0: 0x1D, 0x56, 0x42, 0x00)
    var foundCut = false
    for (i in 0 until bytes.size - 3) {
      if (bytes[i] == 0x1D.toByte() && bytes[i + 1] == 0x56.toByte() && bytes[i + 2] == 0x42.toByte()) {
        foundCut = true
        break
      }
    }
    assertTrue("Should include ESC/POS paper cut command", foundCut)
  }

  @Test
  fun testEscPosTicketBuilder_orderReceipt() {
    val sampleOrder = OrderDoc(
      id = "order-123",
      orderNumber = 1,
      customerName = "Carlos Perez",
      tableOrDestination = "Mesa 3",
      totalUsd = 25.50,
      totalBs = 1020.00,
      exchangeRate = 40.0,
      paymentMethod = "USD_EFECTIVO",
      amountUsdPaid = 25.50,
      isPaid = true
    )
    val sampleItems = listOf(
      CartOrderItem(
        menuItemId = "dish-1",
        dishName = "Parrilla Mixta 200",
        priceUsd = 18.00,
        quantity = 1,
        excludedIngredientNames = listOf("Cebolla")
      ),
      CartOrderItem(
        menuItemId = "dish-2",
        dishName = "Refresco",
        priceUsd = 2.50,
        quantity = 3
      )
    )
    val config = BusinessConfigDoc(businessName = "La Parrilla 200", rif = "J-12345678-9")

    // Test 58mm
    val bytes58 = EscPosTicketBuilder.buildOrderReceiptBytes(sampleOrder, sampleItems, config, PaperWidth.WIDTH_58MM)
    assertTrue(bytes58.isNotEmpty())

    // Test 80mm
    val bytes80 = EscPosTicketBuilder.buildOrderReceiptBytes(sampleOrder, sampleItems, config, PaperWidth.WIDTH_80MM)
    assertTrue(bytes80.isNotEmpty())
  }

  @Test
  fun testHtmlTicketGenerator_mediaPrintCssInjected() {
    val sampleOrder = OrderDoc(
      orderNumber = 5,
      customerName = "Maria Gomez",
      tableOrDestination = "Para Llevar",
      totalUsd = 12.00,
      totalBs = 480.00,
      exchangeRate = 40.0,
      paymentMethod = "PAGO_MOVIL",
      amountPagoMovilBs = 480.00,
      referenceCode = "REF9988",
      isPaid = true
    )
    val sampleItems = listOf(
      CartOrderItem(
        menuItemId = "dish-1",
        dishName = "Hamburguesa Especial",
        priceUsd = 12.00,
        quantity = 1,
        specialInstructions = "Bien cocida"
      )
    )
    val config = BusinessConfigDoc(businessName = "La Parrilla 200")

    val html58 = HtmlTicketGenerator.generateOrderReceiptHtml(sampleOrder, sampleItems, config, PaperWidth.WIDTH_58MM)
    assertTrue(html58.contains("@media print"))
    assertTrue(html58.contains("margin: 0"))
    assertTrue(html58.contains("58mm"))
    assertTrue(html58.contains("Hamburguesa Especial"))

    val html80 = HtmlTicketGenerator.generateOrderReceiptHtml(sampleOrder, sampleItems, config, PaperWidth.WIDTH_80MM)
    assertTrue(html80.contains("@media print"))
    assertTrue(html80.contains("80mm"))

    val kitchenHtml = HtmlTicketGenerator.generateKitchenTicketHtml(sampleOrder, sampleItems, PaperWidth.WIDTH_58MM)
    assertTrue(kitchenHtml.contains("COMANDA COCINA"))
    assertTrue(kitchenHtml.contains("@media print"))
  }

  @Test
  fun testThermalTicketHelper_textFormattingColumns() {
    val sampleOrder = OrderDoc(
      orderNumber = 10,
      customerName = "Pedro",
      tableOrDestination = "Mesa 1",
      totalUsd = 10.0,
      totalBs = 400.0,
      exchangeRate = 40.0
    )
    val sampleItems = listOf(
      CartOrderItem(menuItemId = "d1", dishName = "Parrilla 200", priceUsd = 10.0, quantity = 1)
    )
    val config = BusinessConfigDoc()

    val text58 = ThermalTicketHelper.generateOrderReceiptText(sampleOrder, sampleItems, config, PaperWidth.WIDTH_58MM)
    val lines58 = text58.lines().filter { it.isNotBlank() }
    for (line in lines58) {
      assertTrue("Line length in 58mm should be <= 32 chars: '$line'", line.length <= 32)
    }

    val text80 = ThermalTicketHelper.generateOrderReceiptText(sampleOrder, sampleItems, config, PaperWidth.WIDTH_80MM)
    val lines80 = text80.lines().filter { it.isNotBlank() }
    for (line in lines80) {
      assertTrue("Line length in 80mm should be <= 48 chars: '$line'", line.length <= 48)
    }
  }
}
