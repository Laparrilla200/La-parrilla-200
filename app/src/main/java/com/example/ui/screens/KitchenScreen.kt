package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CartOrderItem
import com.example.data.model.OrderDoc
import com.example.data.printer.ThermalTicketHelper
import com.example.printer.PaperWidth
import com.example.printer.ThermalPrinterManager
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.ParrillaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KitchenScreen(viewModel: ParrillaViewModel) {
  val context = LocalContext.current
  val orders by viewModel.orders.collectAsState()
  var filterStatus by remember { mutableStateOf("PENDIENTES") }

  var selectedPaperWidth by remember {
    mutableStateOf(ThermalPrinterManager.getPaperWidth(context))
  }

  // Estado para alternar entre Modo Teléfono y Modo Tablet KDS
  var isTabletMode by remember { mutableStateOf(false) }

  // Estado para visualización detallada en modo Tablet
  var selectedOrderForDetail by remember { mutableStateOf<OrderDoc?>(null) }
  // Estado para confirmación de cancelación de orden
  var orderToCancel by remember { mutableStateOf<OrderDoc?>(null) }

  val filteredOrders = try {
    when (filterStatus) {
      "PENDIENTES" -> orders.filter { it.status == "EN_COCINA" }
      "LISTOS" -> orders.filter { it.status == "LISTO" }
      else -> orders.filter { it.status != "CANCELADO" }
    }
  } catch (_: Exception) {
    emptyList()
  }

  val pendingCount = orders.count { it.status == "EN_COCINA" }
  val readyCount = orders.count { it.status == "LISTO" }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(12.dp)
      .testTag("kitchen_screen")
  ) {
    // Encabezado de Cocina KDS
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Restaurant,
          contentDescription = null,
          tint = GrillFireRed,
          modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Comandas de Cocina",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 19.sp,
          color = Color.White
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Selector Modo Teléfono vs Modo Tablet KDS
        Surface(
          color = if (!isTabletMode) GrillCrimson else Color(0xFF1E293B),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, if (!isTabletMode) GrillFireRed else Color(0xFF38BDF8).copy(alpha = 0.5f)),
          modifier = Modifier
            .clickable { isTabletMode = false }
            .testTag("toggle_kitchen_phone_mode")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("📱 Teléfono", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        Surface(
          color = if (isTabletMode) Color(0xFF0284C7) else Color(0xFF1E293B),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, if (isTabletMode) Color(0xFF38BDF8) else Color(0xFF475569)),
          modifier = Modifier
            .clickable { isTabletMode = true }
            .testTag("toggle_kitchen_tablet_mode")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.TabletAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("📟 Tablet KDS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        Surface(
          color = if (pendingCount > 0) GrillCrimson else Color(0xFF2E7D32),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "$pendingCount por preparar",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Fila de Filtros y Selector de Ancho Térmico de Impresión (58mm / 80mm)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = filterStatus == "PENDIENTES",
          onClick = { filterStatus = "PENDIENTES" },
          label = { Text("Pendientes ($pendingCount)", fontSize = 11.5.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GrillCrimson,
            selectedLabelColor = Color.White,
            containerColor = SurfaceVariantDark,
            labelColor = Color(0xFFCCCCCC)
          ),
          modifier = Modifier.testTag("filter_pendientes")
        )

        FilterChip(
          selected = filterStatus == "LISTOS",
          onClick = { filterStatus = "LISTOS" },
          label = { Text("Listos ($readyCount)", fontSize = 11.5.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GreenSuccess,
            selectedLabelColor = Color.White,
            containerColor = SurfaceVariantDark,
            labelColor = Color(0xFFCCCCCC)
          ),
          modifier = Modifier.testTag("filter_listos")
        )

        FilterChip(
          selected = filterStatus == "TODOS",
          onClick = { filterStatus = "TODOS" },
          label = { Text("Todos (${orders.size})", fontSize = 11.5.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF455A64),
            selectedLabelColor = Color.White,
            containerColor = SurfaceVariantDark,
            labelColor = Color(0xFFCCCCCC)
          ),
          modifier = Modifier.testTag("filter_todos")
        )
      }

      // Selector de Ancho de Impresora Térmica de Cocina (58mm / 80mm)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(13.dp))
        Surface(
          color = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) GrillCrimson else Color(0xFF28282E),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, if (selectedPaperWidth == PaperWidth.WIDTH_58MM) GrillFireRed else Color(0xFF444444)),
          modifier = Modifier
            .clickable {
              selectedPaperWidth = PaperWidth.WIDTH_58MM
              ThermalPrinterManager.savePaperWidth(context, PaperWidth.WIDTH_58MM)
            }
            .testTag("kitchen_toggle_printer_58mm")
        ) {
          Text(
            text = "58mm",
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Surface(
          color = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) GrillCrimson else Color(0xFF28282E),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, if (selectedPaperWidth == PaperWidth.WIDTH_80MM) GrillFireRed else Color(0xFF444444)),
          modifier = Modifier
            .clickable {
              selectedPaperWidth = PaperWidth.WIDTH_80MM
              ThermalPrinterManager.savePaperWidth(context, PaperWidth.WIDTH_80MM)
            }
            .testTag("kitchen_toggle_printer_80mm")
        ) {
          Text(
            text = "80mm",
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (filteredOrders.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GreenSuccess,
            modifier = Modifier.size(54.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "¡Todo al día en cocina!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "No hay órdenes pendientes en este momento",
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA)
          )
        }
      }
    } else {
      if (isTabletMode) {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 320.dp),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(
            items = filteredOrders,
            key = { it.id.ifBlank { "${it.orderNumber}_${it.timestamp}" } }
          ) { order ->
            val items = remember(order.itemsJson) {
              try {
                viewModel.parseCartItems(order.itemsJson)
              } catch (_: Exception) {
                emptyList()
              }
            }
            KitchenOrderCard(
              order = order,
              items = items,
              paperWidth = selectedPaperWidth,
              isTabletMode = true,
              onClickCard = { selectedOrderForDetail = order },
              onUpdateStatus = { nextStatus ->
                try {
                  viewModel.updateOrderStatus(order.id, nextStatus)
                } catch (e: Exception) {
                  Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              onPrintKitchenTicket = {
                try {
                  val ticket = ThermalTicketHelper.generateKitchenTicketText(order, items, selectedPaperWidth)
                  ThermalTicketHelper.printTicketViaPrintManager(
                    context = context,
                    title = "Comanda #${order.orderNumber}",
                    ticketText = ticket,
                    paperWidth = selectedPaperWidth
                  )
                  viewModel.showThermalTicket("Comanda Cocina #${order.orderNumber} (${selectedPaperWidth.label.take(4)})", ticket)
                } catch (e: Exception) {
                  Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              onPrintClientTicket = {
                try {
                  viewModel.showOrderReceiptTicket(order, selectedPaperWidth)
                } catch (e: Exception) {
                  Toast.makeText(context, "Error al visualizar factura: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              onRequestCancel = { orderToCancel = order }
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(
            items = filteredOrders,
            key = { it.id.ifBlank { "${it.orderNumber}_${it.timestamp}" } }
          ) { order ->
            val items = remember(order.itemsJson) {
              try {
                viewModel.parseCartItems(order.itemsJson)
              } catch (_: Exception) {
                emptyList()
              }
            }
            KitchenOrderCard(
              order = order,
              items = items,
              paperWidth = selectedPaperWidth,
              isTabletMode = false,
              onClickCard = { selectedOrderForDetail = order },
              onUpdateStatus = { nextStatus ->
                try {
                  viewModel.updateOrderStatus(order.id, nextStatus)
                } catch (e: Exception) {
                  Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              onPrintKitchenTicket = {
                try {
                  val ticket = ThermalTicketHelper.generateKitchenTicketText(order, items, selectedPaperWidth)
                  ThermalTicketHelper.printTicketViaPrintManager(
                    context = context,
                    title = "Comanda #${order.orderNumber}",
                    ticketText = ticket,
                    paperWidth = selectedPaperWidth
                  )
                  viewModel.showThermalTicket("Comanda Cocina #${order.orderNumber} (${selectedPaperWidth.label.take(4)})", ticket)
                } catch (e: Exception) {
                  Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              onPrintClientTicket = {
                try {
                  viewModel.showOrderReceiptTicket(order, selectedPaperWidth)
                } catch (e: Exception) {
                  Toast.makeText(context, "Error al visualizar factura: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              },
              onRequestCancel = { orderToCancel = order }
            )
          }
        }
      }
    }

    // Diálogo de Comanda Ampliada para Tablet de Cocina
    selectedOrderForDetail?.let { order ->
      val detailItems = remember(order.itemsJson) {
        try {
          viewModel.parseCartItems(order.itemsJson)
        } catch (_: Exception) {
          emptyList()
        }
      }
      KitchenTabletDetailDialog(
        order = order,
        items = detailItems,
        paperWidth = selectedPaperWidth,
        onDismiss = { selectedOrderForDetail = null },
        onUpdateStatus = { nextStatus ->
          viewModel.updateOrderStatus(order.id, nextStatus)
          selectedOrderForDetail = null
        },
        onPrintKitchenComanda = {
          try {
            val ticket = ThermalTicketHelper.generateKitchenTicketText(order, detailItems, selectedPaperWidth)
            ThermalTicketHelper.printTicketViaPrintManager(
              context = context,
              title = "Comanda #${order.orderNumber}",
              ticketText = ticket,
              paperWidth = selectedPaperWidth
            )
            viewModel.showThermalTicket("Comanda Cocina #${order.orderNumber}", ticket)
          } catch (e: Exception) {
            Toast.makeText(context, "Error al imprimir: ${e.message}", Toast.LENGTH_SHORT).show()
          }
        },
        onRequestCancel = {
          orderToCancel = order
          selectedOrderForDetail = null
        }
      )
    }

    // Diálogo de Confirmación para Cancelar Pedido (Restaura Stock)
    orderToCancel?.let { order ->
      AlertDialog(
        onDismissRequest = { orderToCancel = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = GrillFireRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("¿Cancelar Pedido #${order.orderNumber}?", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        text = {
          Text(
            text = "Al cancelar este pedido, los ingredientes e insumos descontados se devolverán automáticamente al inventario del sistema.\n\nEsta acción no se puede deshacer.",
            color = Color(0xFFDDDDDD),
            fontSize = 13.5.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.cancelOrder(order.id, restoreStock = true)
              orderToCancel = null
              Toast.makeText(context, "Pedido #${order.orderNumber} cancelado e inventario restaurado", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GrillFireRed)
          ) {
            Text("Sí, Cancelar Pedido", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { orderToCancel = null }) {
            Text("Volver", color = Color.White)
          }
        },
        containerColor = Color(0xFF1E1E24)
      )
    }
  }
}

@Composable
private fun KitchenOrderCard(
  order: OrderDoc,
  items: List<CartOrderItem>,
  paperWidth: PaperWidth,
  isTabletMode: Boolean = false,
  onClickCard: () -> Unit,
  onUpdateStatus: (String) -> Unit,
  onPrintKitchenTicket: () -> Unit,
  onPrintClientTicket: () -> Unit,
  onRequestCancel: () -> Unit
) {
  val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
  val orderTime = try {
    timeFormat.format(Date(order.timestamp))
  } catch (_: Exception) {
    "--:--"
  }
  val elapsedMinutes = try {
    ((System.currentTimeMillis() - order.timestamp) / 60000).toInt().coerceAtLeast(0)
  } catch (_: Exception) {
    0
  }

  val isUrgent = elapsedMinutes >= 15 && order.status == "EN_COCINA"
  val statusColor = when (order.status) {
    "EN_COCINA" -> if (isUrgent) GrillCrimson else AmberWarning
    "LISTO" -> GreenSuccess
    "ENTREGADO" -> Color(0xFF78909C)
    else -> Color(0xFF616161)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClickCard)
      .border(
        width = if (isUrgent) 2.dp else 1.dp,
        color = statusColor,
        shape = RoundedCornerShape(14.dp)
      )
      .testTag("kitchen_order_card_${order.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Cabecera de la comanda
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = GrillCrimson,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "#${String.format(Locale.US, "%03d", order.orderNumber)}",
              color = Color.White,
              fontWeight = FontWeight.Black,
              fontSize = 17.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = order.tableOrDestination.ifBlank { "Mesa" },
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
            Text(
              text = "Cliente: ${order.customerName.ifBlank { "Cliente" }}",
              color = Color(0xFFAAAAAA),
              fontSize = 11.5.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = "$orderTime (${elapsedMinutes}m)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
              color = statusColor.copy(alpha = 0.2f),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.padding(top = 2.dp)
            ) {
              Text(
                text = when (order.status) {
                  "EN_COCINA" -> "EN PREPARACIÓN"
                  "LISTO" -> "¡LISTO PARA ENTREGAR!"
                  "ENTREGADO" -> "ENTREGADO"
                  else -> order.status
                },
                color = statusColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          // Botón expandir a vista completa en tablet
          IconButton(
            onClick = onClickCard,
            modifier = Modifier
              .size(36.dp)
              .background(Color(0xFF282830), CircleShape)
          ) {
            Icon(Icons.Default.Fullscreen, contentDescription = "Ver en Tablet", tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF333333)))
      Spacer(modifier = Modifier.height(10.dp))

      // Lista de Platos para Cocina con Insumos Modificados Destacados
      for (item in items) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              color = GrillFireRed,
              shape = CircleShape,
              modifier = Modifier.size(if (isTabletMode) 34.dp else 26.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "${item.quantity}",
                  color = Color.White,
                  fontWeight = FontWeight.Black,
                  fontSize = if (isTabletMode) 17.sp else 13.sp
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = item.dishName.uppercase(),
              fontWeight = FontWeight.ExtraBold,
              fontSize = if (isTabletMode) 18.sp else 14.5.sp,
              color = Color.White,
              modifier = Modifier.weight(1f)
            )
          }

          // Advertencia prominente de ingredientes excluidos ("SIN CEBOLLA", etc.)
          if (item.excludedIngredientNames.isNotEmpty()) {
            Row(
              modifier = Modifier
                .padding(start = if (isTabletMode) 44.dp else 36.dp, top = 3.dp)
                .fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(if (isTabletMode) 18.dp else 15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "MODIFICADO: ${item.excludedIngredientNames.joinToString(", ") { "SIN " + it.uppercase() }}",
                color = Color(0xFFFF8A80),
                fontWeight = FontWeight.Black,
                fontSize = if (isTabletMode) 14.sp else 12.sp
              )
            }
          }

          if (item.specialInstructions.isNotBlank()) {
            Text(
              text = "👉 NOTA: ${item.specialInstructions}",
              color = Color(0xFFFFD54F),
              fontWeight = FontWeight.SemiBold,
              fontSize = if (isTabletMode) 14.sp else 12.sp,
              modifier = Modifier.padding(start = if (isTabletMode) 44.dp else 36.dp, top = 2.dp)
            )
          }
        }
      }

      if (order.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = Color(0xFF2C2C30),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Observación General: ${order.notes}",
            fontSize = if (isTabletMode) 14.sp else 11.5.sp,
            color = Color(0xFFDDDDDD),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(8.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Botones de Acción de Cocina
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Imprimir Comanda de Cocina (58mm o 80mm adaptativo)
        OutlinedButton(
          onClick = onPrintKitchenTicket,
          modifier = Modifier
            .height(if (isTabletMode) 50.dp else 40.dp)
            .testTag("print_kitchen_ticket_${order.id}"),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(if (isTabletMode) 18.dp else 16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Imprimir (${paperWidth.label.take(4)})", fontSize = if (isTabletMode) 13.sp else 11.sp)
        }

        // Cambio de Estado (Cocina -> Listo -> Entregado)
        when (order.status) {
          "EN_COCINA" -> {
            Button(
              onClick = { onUpdateStatus("LISTO") },
              modifier = Modifier
                .weight(1f)
                .height(if (isTabletMode) 50.dp else 40.dp)
                .testTag("mark_ready_button_${order.id}"),
              colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
              Text("¡Listo para Servir!", fontWeight = FontWeight.Bold, fontSize = if (isTabletMode) 15.sp else 13.sp, color = Color.White)
            }
          }
          "LISTO" -> {
            Button(
              onClick = { onUpdateStatus("ENTREGADO") },
              modifier = Modifier
                .weight(1f)
                .testTag("mark_delivered_button_${order.id}"),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
            ) {
              Text("Despachado / Entregado", fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
          else -> {
            OutlinedButton(
              onClick = onPrintClientTicket,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Ver Factura", fontSize = 11.sp, color = Color.White)
            }
          }
        }

        // Botón cancelar comanda
        IconButton(
          onClick = onRequestCancel,
          modifier = Modifier
            .size(38.dp)
            .background(Color(0xFF331A1A), CircleShape)
        ) {
          Icon(Icons.Default.Delete, contentDescription = "Cancelar Pedido", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

/**
 * Vista KDS de Pantalla Completa / Tablet para Cocineros:
 * - Letras gigantes y legibles a distancia en la cocina
 * - Checkbox interactivo para marcar ítems listos en la plancha/parrilla
 * - Destacado de advertencias "SIN CEBOLLA / SIN SALSA"
 * - Botón para enviar a imprimir a la impresora de atrás
 * - Botón para marcar lista o cancelar
 */
@Composable
private fun KitchenTabletDetailDialog(
  order: OrderDoc,
  items: List<CartOrderItem>,
  paperWidth: PaperWidth,
  onDismiss: () -> Unit,
  onUpdateStatus: (String) -> Unit,
  onPrintKitchenComanda: () -> Unit,
  onRequestCancel: () -> Unit
) {
  val checkedItems = remember { mutableStateListOf<Int>() }

  val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
  val orderTime = try {
    timeFormat.format(Date(order.timestamp))
  } catch (_: Exception) {
    "--:--"
  }
  val elapsedMinutes = try {
    ((System.currentTimeMillis() - order.timestamp) / 60000).toInt().coerceAtLeast(0)
  } catch (_: Exception) {
    0
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f),
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF141418),
      border = BorderStroke(2.dp, GrillFireRed)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Encabezado Grande para Tablet
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = GrillCrimson,
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = "COMANDA #${String.format(Locale.US, "%03d", order.orderNumber)}",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
              )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
              Text(
                text = "📍 ${order.tableOrDestination.ifBlank { "Mesa" }}",
                color = Color(0xFFFFD54F),
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
              )
              Text(
                text = "Cliente: ${order.customerName.ifBlank { "General" }}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
              color = Color(0xFF282834),
              shape = RoundedCornerShape(10.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "$orderTime  (${elapsedMinutes} min)",
                  color = Color.White,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(46.dp)
                .background(Color(0xFF33333E), CircleShape)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(24.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF33333E)))
        Spacer(modifier = Modifier.height(14.dp))

        // Lista de Ítems en Tamaño Gigante con Checklist de Cocina
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(items.size) { index ->
            val item = items[index]
            val isChecked = checkedItems.contains(index)

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (isChecked) checkedItems.remove(index) else checkedItems.add(index)
                },
              color = if (isChecked) Color(0xFF1E281E) else Color(0xFF22222A),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(
                width = if (isChecked) 2.dp else 1.dp,
                color = if (isChecked) GreenSuccess else Color(0xFF3E3E4C)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isChecked,
                  onCheckedChange = {
                    if (isChecked) checkedItems.remove(index) else checkedItems.add(index)
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = GreenSuccess,
                    uncheckedColor = Color(0xFFAAAAAA)
                  ),
                  modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                  color = if (isChecked) GreenSuccess else GrillFireRed,
                  shape = CircleShape,
                  modifier = Modifier.size(44.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = "${item.quantity}",
                      color = Color.White,
                      fontWeight = FontWeight.Black,
                      fontSize = 22.sp
                    )
                  }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.dishName.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isChecked) Color(0xFFA5D6A7) else Color.White
                  )

                  if (item.excludedIngredientNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                      color = Color(0xFF7F1D1D),
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, Color(0xFFEF4444))
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "⚠️ ATENCIÓN: ${item.excludedIngredientNames.joinToString(", ") { "SIN " + it.uppercase() }}",
                          fontSize = 16.sp,
                          fontWeight = FontWeight.Black,
                          color = Color.White
                        )
                      }
                    }
                  }

                  if (item.specialInstructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "👉 NOTA: ${item.specialInstructions}",
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFFFD54F)
                    )
                  }
                }
              }
            }
          }
        }

        if (order.notes.isNotBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            color = Color(0xFF2C2C36),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Observación General: ${order.notes}",
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFFFFF9C4),
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Barra Inferior de Acciones Rápidas para Cocineros
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Imprimir a la impresora de atrás (térmica)
          Button(
            onClick = onPrintKitchenComanda,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3440)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(56.dp)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Imprimir Comanda (${paperWidth.label.take(4)})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }

          // Cancelar Pedido
          OutlinedButton(
            onClick = onRequestCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
            border = BorderStroke(1.dp, Color(0xFFFF6B6B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(56.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
          }

          // Botón Principal: Marcar Listo para Servir
          Button(
            onClick = { onUpdateStatus("LISTO") },
            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .height(56.dp)
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("✓ ¡Comanda Lista para Servir!", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
          }
        }
      }
    }
  }
}
