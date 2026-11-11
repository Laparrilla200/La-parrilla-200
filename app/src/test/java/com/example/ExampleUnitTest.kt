package com.example

import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.ClosureDoc
import com.example.data.model.OrderDoc
import com.example.data.printer.ThermalTicketHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testOrderTicketGeneration() {
    val config = BusinessConfigDoc(
      businessName = "LA PARRILLA 200",
      exchangeRateUsdToBs = 65.0
    )
    val order = OrderDoc(
      orderNumber = 12,
      customerName = "Carlos",
      tableOrDestination = "Mesa 3",
      totalUsd = 22.00,
      exchangeRate = 65.0,
      totalBs = 1430.00,
      paymentMethod = "PAGO_MOVIL",
      amountPagoMovilBs = 1430.00,
      referenceCode = "4819",
      isPaid = true
    )
    val items = listOf(
      CartOrderItem(
        dishName = "Parrilla 200 Mixta",
        quantity = 1,
        priceUsd = 22.00,
        excludedIngredientNames = listOf("Cebolla"),
        specialInstructions = "Bien cocida"
      )
    )

    val ticketText = ThermalTicketHelper.generateOrderReceiptText(order, items, config)

    assertNotNull(ticketText)
    assertTrue("Debe contener el nombre del negocio", ticketText.contains("LA PARRILLA 200"))
    assertTrue("Debe contener el número de orden", ticketText.contains("0012"))
    assertTrue("Debe indicar el ingrediente excluido", ticketText.contains("SIN: CEBOLLA"))
    assertTrue("Debe incluir el total en dólares", ticketText.contains("$22.00"))
    assertTrue("Debe incluir el total en bolívares", ticketText.contains("1430.00 Bs"))
    assertTrue("Debe reflejar pago móvil", ticketText.contains("PAGO MOVIL"))
  }

  @Test
  fun testClosureTicketGeneration() {
    val config = BusinessConfigDoc(businessName = "LA PARRILLA 200")
    val closure = ClosureDoc(
      closureType = "DIARIO",
      closureNumber = 5,
      totalSalesUsd = 350.00,
      totalSalesBs = 22750.00,
      totalUsdCash = 150.00,
      totalPagoMovilBs = 12000.00,
      totalPuntoVentaBs = 10750.00,
      orderCount = 18,
      closedBy = "Admin"
    )

    val ticketText = ThermalTicketHelper.generateClosureReceiptText(closure, config)

    assertNotNull(ticketText)
    assertTrue("Debe indicar CIERRE DE CAJA DIARIO", ticketText.contains("CIERRE DE CAJA DIARIO"))
    assertTrue("Debe incluir total pedidos", ticketText.contains("18"))
    assertTrue("Debe incluir desglose de efectivo", ticketText.contains("$150.00"))
    assertTrue("Debe incluir desglose de pago móvil", ticketText.contains("12000.00 Bs"))
    assertTrue("Debe incluir desglose de punto de venta", ticketText.contains("10750.00 Bs"))
  }
}
