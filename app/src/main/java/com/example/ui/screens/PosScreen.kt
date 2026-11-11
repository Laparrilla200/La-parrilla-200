package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.OrderDoc
import com.example.printer.PaperWidth
import com.example.printer.ThermalPrinterManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CartOrderItem
import com.example.data.model.IngredientRequirement
import com.example.data.model.MenuItemDoc
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.ParrillaViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(viewModel: ParrillaViewModel) {
  val context = LocalContext.current
  val menuItems by viewModel.menuItems.collectAsState()
  val cart by viewModel.cart.collectAsState()
  val config by viewModel.config.collectAsState()
  val customerName by viewModel.customerName.collectAsState()
  val tableOrDestination by viewModel.tableOrDestination.collectAsState()
  val unpaidOrders by viewModel.unpaidOrders.collectAsState()
  val paidOrders by viewModel.paidOrders.collectAsState()

  var accountsTab by remember { mutableStateOf("PENDING") } // "PENDING" o "PAID"
  var paidOrderToView by remember { mutableStateOf<OrderDoc?>(null) }
  var orderToQuickPayConfirm by remember { mutableStateOf<OrderDoc?>(null) }
  val canBill = viewModel.canUserBill()

  var selectedCategory by remember { mutableStateOf("Todas") }
  var customizingItem by remember { mutableStateOf<MenuItemDoc?>(null) }
  var showCartSheet by remember { mutableStateOf(false) }
  var showCheckoutDialog by remember { mutableStateOf(false) }
  var orderToSettle by remember { mutableStateOf<OrderDoc?>(null) }
  var orderToEdit by remember { mutableStateOf<OrderDoc?>(null) }
  var orderToCancel by remember { mutableStateOf<OrderDoc?>(null) }
  var orderToDelete by remember { mutableStateOf<OrderDoc?>(null) }
  var selectedPaperWidth by remember {
    mutableStateOf(ThermalPrinterManager.getPaperWidth(context))
  }

  val categories = listOf("Todas", "Parrillas", "Hamburguesas & Pepitos", "Entradas & Guarniciones", "Bebidas")
  val tablePresets = listOf("Mesa 1", "Mesa 2", "Mesa 3", "Mesa 4", "Mesa 5", "Mesa 6", "Barra", "Para Llevar")

  val filteredDishes = if (selectedCategory == "Todas") {
    menuItems
  } else {
    menuItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
  }

  val totalCartUsd = cart.sumOf { it.priceUsd * it.quantity }
  val totalCartBs = totalCartUsd * config.exchangeRateUsdToBs
  val cartCount = cart.sumOf { it.quantity }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .testTag("pos_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = if (cart.isNotEmpty()) 80.dp else 0.dp)
    ) {
      // 1. Barra Rápida de Destino / Mesa y Cliente
      Surface(
        color = DeepCharcoal,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = customerName,
              onValueChange = { viewModel.setCustomerName(it) },
              label = { Text("Cliente", fontSize = 11.sp) },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("pos_customer_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )

            OutlinedTextField(
              value = tableOrDestination,
              onValueChange = { viewModel.setTableOrDestination(it) },
              label = { Text("Mesa/Destino", fontSize = 11.sp) },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("pos_table_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Chips rápidos de mesas para seleccionar con un solo toque
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tablePresets) { preset ->
              val isSel = tableOrDestination.equals(preset, ignoreCase = true)
              Surface(
                color = if (isSel) GrillCrimson else Color(0xFF25252A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .clickable { viewModel.setTableOrDestination(preset) }
                  .testTag("table_chip_$preset")
              ) {
                Text(
                  text = preset,
                  fontSize = 11.sp,
                  color = Color.White,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Barra de Configuración Rápida de Impresora Térmica (58mm / 80mm)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(14.dp))
              Text("Ticket Térmico:", fontSize = 11.sp, color = Color(0xFFAAAAAA))
              Surface(
                color = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) GrillCrimson else Color(0xFF28282E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selectedPaperWidth == PaperWidth.WIDTH_58MM) GrillFireRed else Color(0xFF444444)),
                modifier = Modifier
                  .clickable {
                    selectedPaperWidth = PaperWidth.WIDTH_58MM
                    ThermalPrinterManager.savePaperWidth(context, PaperWidth.WIDTH_58MM)
                  }
                  .testTag("toggle_printer_58mm")
              ) {
                Text(
                  text = "58mm",
                  fontSize = 11.sp,
                  color = Color.White,
                  fontWeight = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
              Surface(
                color = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) GrillCrimson else Color(0xFF28282E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selectedPaperWidth == PaperWidth.WIDTH_80MM) GrillFireRed else Color(0xFF444444)),
                modifier = Modifier
                  .clickable {
                    selectedPaperWidth = PaperWidth.WIDTH_80MM
                    ThermalPrinterManager.savePaperWidth(context, PaperWidth.WIDTH_80MM)
                  }
                  .testTag("toggle_printer_80mm")
              ) {
                Text(
                  text = "80mm",
                  fontSize = 11.sp,
                  color = Color.White,
                  fontWeight = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) FontWeight.Bold else FontWeight.Normal,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Text(
              text = "Tasa: ${String.format(Locale.US, "%.2f", config.exchangeRateUsdToBs)} Bs/$",
              color = Color(0xFF00E676),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // 2. Sección de Gestión de Cuentas: PENDIENTES vs PAGADAS
      Surface(
        color = Color(0xFF1C1C22),
        border = BorderStroke(1.dp, if (accountsTab == "PENDING" && unpaidOrders.isNotEmpty()) GrillFireRed.copy(alpha = 0.6f) else Color(0xFF33333E)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp)
      ) {
        Column(modifier = Modifier.padding(8.dp)) {
          // Selector de Pestañas: Por Cobrar vs Pagadas
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                color = if (accountsTab == "PENDING") GrillCrimson else Color(0xFF282830),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (accountsTab == "PENDING") Color(0xFFFF5252) else Color(0xFF3E3E48)),
                modifier = Modifier
                  .clickable { accountsTab = "PENDING" }
                  .testTag("tab_accounts_pending")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "⏳ Por Cobrar (${unpaidOrders.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = Color.White
                  )
                }
              }

              Surface(
                color = if (accountsTab == "PAID") Color(0xFF1B5E20) else Color(0xFF282830),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (accountsTab == "PAID") Color(0xFF4CAF50) else Color(0xFF3E3E48)),
                modifier = Modifier
                  .clickable { accountsTab = "PAID" }
                  .testTag("tab_accounts_paid")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "✅ Pagadas (${paidOrders.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = Color.White
                  )
                }
              }
            }

            Text(
              text = "Papel: ${selectedPaperWidth.label.take(4)}",
              fontSize = 10.sp,
              color = Color(0xFFAAAAAA),
              fontWeight = FontWeight.Medium
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          if (accountsTab == "PENDING") {
            // LISTA DE CUENTAS PENDIENTES POR COBRAR
            if (unpaidOrders.isEmpty()) {
              Surface(
                color = Color(0xFF24242A),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("¡Excelente! Todas las cuentas del día están cobradas y al día.", color = Color(0xFFCCCCCC), fontSize = 11.5.sp)
                }
              }
            } else {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(unpaidOrders) { order ->
                  Surface(
                    color = Color(0xFF28282E),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF444450)),
                    modifier = Modifier.testTag("open_order_chip_${order.orderNumber}")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(
                        modifier = Modifier
                          .clickable { orderToSettle = order }
                          .padding(end = 6.dp)
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            text = "#${order.orderNumber} • ${order.tableOrDestination}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                          )
                        }
                        Text(
                          text = String.format(Locale.US, "$%.2f (%.0f Bs)", order.totalUsd, order.totalBs),
                          fontSize = 11.sp,
                          color = Color(0xFFFFB300),
                          fontWeight = FontWeight.Black
                        )
                      }

                      // Botón Editar / Agregar Ítems a la Cuenta
                      IconButton(
                        onClick = { orderToEdit = order },
                        modifier = Modifier
                          .size(28.dp)
                          .background(Color(0xFF33333E), CircleShape)
                          .testTag("edit_open_order_${order.orderNumber}")
                      ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modificar Pedido", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                      }

                      Spacer(modifier = Modifier.width(4.dp))

                      // Botón Rápido: PAGADO (Efectivo rápido sin modal pesado)
                      Surface(
                        color = Color(0xFF2E7D32),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                          .clickable(enabled = canBill) {
                            orderToQuickPayConfirm = order
                          }
                          .testTag("quick_pay_button_${order.orderNumber}")
                      ) {
                        Text(
                          text = "⚡ PAGADO",
                          color = Color.White,
                          fontWeight = FontWeight.Black,
                          fontSize = 9.sp,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                      }

                      Spacer(modifier = Modifier.width(4.dp))

                      // Botón COBRAR (Abre modal para punto, pago móvil o mixto)
                      Surface(
                        color = GrillCrimson,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                          .clickable(enabled = canBill) { orderToSettle = order }
                          .testTag("settle_button_${order.orderNumber}")
                      ) {
                        Text(
                          text = "COBRAR",
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          fontSize = 9.sp,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                      }

                      Spacer(modifier = Modifier.width(4.dp))

                      // Botón Eliminar Comanda / Prueba
                      IconButton(
                        onClick = { orderToDelete = order },
                        modifier = Modifier
                          .size(26.dp)
                          .background(Color(0xFF3E2723), CircleShape)
                          .testTag("delete_open_order_${order.orderNumber}")
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Cuenta", tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                      }
                    }
                  }
                }
              }
            }
          } else {
            // LISTA DE CUENTAS YA PAGADAS (Separadas para no volver a cobrar)
            if (paidOrders.isEmpty()) {
              Surface(
                color = Color(0xFF24242A),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Aún no se han registrado cobros el día de hoy.", color = Color(0xFFAAAAAA), fontSize = 11.5.sp)
                }
              }
            } else {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(paidOrders) { order ->
                  Surface(
                    color = Color(0xFF1E2820),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                    modifier = Modifier.testTag("paid_order_chip_${order.orderNumber}")
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(
                        modifier = Modifier
                          .clickable { paidOrderToView = order }
                          .padding(end = 6.dp)
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            text = "✅ #${order.orderNumber} • ${order.tableOrDestination}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF81C784)
                          )
                        }
                        Text(
                          text = String.format(Locale.US, "$%.2f (%.0f Bs) • %s", order.totalUsd, order.totalBs, order.paymentMethod.replace("_", " ")),
                          fontSize = 10.sp,
                          color = Color(0xFFE0E0E0),
                          fontWeight = FontWeight.SemiBold
                        )
                      }

                      // Botón Re-imprimir Factura Térmica
                      Surface(
                        color = Color(0xFF263238),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF455A64)),
                        modifier = Modifier
                          .clickable {
                            viewModel.reprintPaidOrderReceipt(context, order, selectedPaperWidth)
                          }
                          .testTag("reprint_button_${order.orderNumber}")
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Default.Print, contentDescription = "Reimprimir", tint = Color(0xFF81D4FA), modifier = Modifier.size(11.dp))
                          Spacer(modifier = Modifier.width(3.dp))
                          Text(
                            text = "Factura",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                          )
                        }
                      }

                      Spacer(modifier = Modifier.width(4.dp))

                      // Botón Eliminar Cuenta Pagada (Para limpiar pruebas)
                      IconButton(
                        onClick = { orderToDelete = order },
                        modifier = Modifier
                          .size(26.dp)
                          .background(Color(0xFF3E2723), CircleShape)
                          .testTag("delete_paid_order_${order.orderNumber}")
                      ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Cuenta", tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 3. Selector de Categorías
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(categories) { category ->
          val isSelected = selectedCategory == category
          FilterChip(
            selected = isSelected,
            onClick = { selectedCategory = category },
            label = {
              Text(
                text = category,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = GrillCrimson,
              selectedLabelColor = Color.White,
              containerColor = SurfaceVariantDark,
              labelColor = Color(0xFFCCCCCC)
            ),
            modifier = Modifier.testTag("cat_chip_$category")
          )
        }
      }

      // 4. Catálogo de Platos con Suma/Resta Rápida al Dictar
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredDishes) { dish ->
          val inCartQty = cart.find { it.menuItemId == dish.id }?.quantity ?: 0
          DishPosCard(
            dish = dish,
            exchangeRate = config.exchangeRateUsdToBs,
            quantityInCart = inCartQty,
            onIncrement = { viewModel.addToCart(dish) },
            onDecrement = {
              val idx = cart.indexOfFirst { it.menuItemId == dish.id }
              if (idx >= 0) {
                viewModel.updateCartItemQuantity(idx, cart[idx].quantity - 1)
              }
            },
            onCustomizeAndAdd = { customizingItem = dish }
          )
        }
      }
    }

    // 5. Barra Flotante Inferior del Carrito: Dos acciones rápidas
    if (cart.isNotEmpty()) {
      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(10.dp)
          .clip(RoundedCornerShape(16.dp))
          .testTag("cart_floating_bar"),
        color = DeepCharcoal,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GrillCrimson, RoundedCornerShape(16.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showCartSheet = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              BadgedBox(
                badge = {
                  Badge(containerColor = GrillCrimson) {
                    Text("$cartCount", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Default.ShoppingCart,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "$tableOrDestination • $customerName (Toca para ver detalle)",
                  color = Color(0xFFAAAAAA),
                  fontSize = 11.sp
                )
                Text(
                  text = String.format(Locale.US, "$%.2f  (%.2f Bs)", totalCartUsd, totalCartBs),
                  color = Color.White,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 15.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Botones de acción ultra-rápidos:
          // Botón 1: ⚡ Enviar a Cocina (imprime comanda auto y deja abierta para cobrar después)
          // Botón 2: 💳 Cobrar Directo (para cuando el cliente paga de una vez)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                viewModel.submitFastOrderToKitchen(context, paperWidth = selectedPaperWidth)
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("fast_send_kitchen_button")
            ) {
              Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("⚡ A Cocina (${selectedPaperWidth.label.take(4)})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            }

            Button(
              onClick = { showCheckoutDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("checkout_floating_button")
            ) {
              Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("💳 Cobrar Directo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Modal de Personalización de Ingredientes por Plato
    customizingItem?.let { dish ->
      val recipe = remember(dish) { viewModel.parseIngredients(dish.ingredientsJson) }
      ItemCustomizerDialog(
        dish = dish,
        recipe = recipe,
        exchangeRate = config.exchangeRateUsdToBs,
        onDismiss = { customizingItem = null },
        onConfirm = { qty, excluded, notes ->
          viewModel.addToCart(
            menuItem = dish,
            quantity = qty,
            excludedIngredients = excluded,
            specialInstructions = notes
          )
          customizingItem = null
        }
      )
    }

    // BottomSheet con el Detalle del Carrito
    if (showCartSheet) {
      ModalBottomSheet(
        onDismissRequest = { showCartSheet = false },
        containerColor = DeepCharcoal,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ) {
        CartSheetContent(
          cart = cart,
          exchangeRate = config.exchangeRateUsdToBs,
          customerName = customerName,
          tableOrDestination = tableOrDestination,
          onCustomerNameChange = { viewModel.setCustomerName(it) },
          onTableChange = { viewModel.setTableOrDestination(it) },
          onUpdateQuantity = { idx, q -> viewModel.updateCartItemQuantity(idx, q) },
          onClearCart = { viewModel.clearCart() },
          onSendToKitchenFast = {
            showCartSheet = false
            viewModel.submitFastOrderToKitchen(context, paperWidth = selectedPaperWidth)
          },
          onProceedToCheckout = {
            showCartSheet = false
            showCheckoutDialog = true
          }
        )
      }
    }

    // Modal de Cobro Multimoneda Directo ($ USD, Pago Móvil, Punto de Venta)
    if (showCheckoutDialog) {
      CheckoutMultiCurrencyDialog(
        totalUsd = totalCartUsd,
        exchangeRate = config.exchangeRateUsdToBs,
        customerName = customerName,
        tableOrDestination = tableOrDestination,
        initialPaperWidth = selectedPaperWidth,
        onDismiss = { showCheckoutDialog = false },
        onConfirmCheckout = { method, usdPaid, pagoMovilBs, puntoVentaBs, ref, isPaid, notes, paperWidth ->
          try {
            viewModel.submitOrderAndCheckout(
              paymentMethod = method,
              amountUsdPaid = usdPaid,
              amountPagoMovilBs = pagoMovilBs,
              amountPuntoVentaBs = puntoVentaBs,
              referenceCode = ref,
              isPaid = isPaid,
              notes = notes,
              context = context,
              paperWidth = paperWidth
            )
          } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Error al procesar: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
          }
          showCheckoutDialog = false
        }
      )
    }

    // Modal de Cobro de Cuenta Abierta / Comanda Pendiente
    orderToSettle?.let { openOrder ->
      SettlePaymentDialog(
        order = openOrder,
        exchangeRate = config.exchangeRateUsdToBs,
        initialPaperWidth = selectedPaperWidth,
        onDismiss = { orderToSettle = null },
        onEditOrder = {
          orderToEdit = openOrder
          orderToSettle = null
        },
        onRequestCancel = {
          orderToCancel = openOrder
          orderToSettle = null
        },
        onConfirmSettle = { method, usdPaid, pagoMovilBs, puntoVentaBs, ref, notes, paperWidth ->
          try {
            viewModel.settlePaymentAndPrintReceipt(
              context = context,
              order = openOrder,
              paymentMethod = method,
              usdPaid = usdPaid,
              pagoMovilBs = pagoMovilBs,
              puntoVentaBs = puntoVentaBs,
              refCode = ref,
              notes = notes,
              paperWidth = paperWidth
            )
          } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Error al liquidar pago: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
          }
          orderToSettle = null
        }
      )
    }

    // Modal de Modificación de Cuenta Abierta (Agregar bebidas/platos, modificar cantidades)
    orderToEdit?.let { openOrder ->
      EditOpenOrderDialog(
        order = openOrder,
        menuItems = menuItems,
        exchangeRate = config.exchangeRateUsdToBs,
        onDismiss = { orderToEdit = null },
        onSave = { updatedItems, newTotalUsd, newTotalBs, newlyAddedItems ->
          viewModel.editOpenOrder(
            orderId = openOrder.id,
            updatedItems = updatedItems,
            newlyAddedItems = newlyAddedItems
          )
          orderToEdit = null
          android.widget.Toast.makeText(context, "Cuenta #${openOrder.orderNumber} actualizada exitosamente", android.widget.Toast.LENGTH_SHORT).show()
        },
        onRequestCancel = {
          orderToCancel = openOrder
          orderToEdit = null
        }
      )
    }

    // Modal de Confirmación para Cancelar Pedido (Restaura Stock)
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
            text = "Se cancelará el pedido #${order.orderNumber} de la ${order.tableOrDestination} y se restaurarán los ingredientes al inventario automáticamente.",
            color = Color(0xFFDDDDDD),
            fontSize = 13.5.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.cancelOrder(order.id, restoreStock = true)
              orderToCancel = null
              android.widget.Toast.makeText(context, "Pedido #${order.orderNumber} cancelado", android.widget.Toast.LENGTH_SHORT).show()
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

    // Modal de Confirmación Rápida de Pago en Efectivo (Para marcar pagado inmediatamente)
    orderToQuickPayConfirm?.let { order ->
      AlertDialog(
        onDismissRequest = { orderToQuickPayConfirm = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(8.dp))
            Text("¿Marcar Comanda #${order.orderNumber} como Pagada?", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        text = {
          Column {
            Text(
              text = "Mesa/Destino: ${order.tableOrDestination} (${order.customerName})",
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Total a cobrar: $${String.format(Locale.US, "%.2f", order.totalUsd)} USD (${String.format(Locale.US, "%.2f", order.totalBs)} Bs)",
              color = Color(0xFFFFB300),
              fontWeight = FontWeight.Black,
              fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Al confirmar, la cuenta quedará liquidada en EFECTIVO USD, se imprimirá la factura térmica y pasará a la lista de 'Cuentas Pagadas' para evitar que se vuelva a cobrar.",
              color = Color(0xFFCCCCCC),
              fontSize = 12.5.sp
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.quickMarkOrderAsPaidCash(context, order, selectedPaperWidth)
              orderToQuickPayConfirm = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            modifier = Modifier.testTag("confirm_quick_pay_button")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sí, Marcar Pagado", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { orderToQuickPayConfirm = null }) {
            Text("Cancelar", color = Color(0xFFAAAAAA))
          }
        },
        containerColor = Color(0xFF1E1E24)
      )
    }

    // Modal de Detalle de Cuenta Pagada (Sin botón de cobro para evitar duplicados)
    paidOrderToView?.let { order ->
      AlertDialog(
        onDismissRequest = { paidOrderToView = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cuenta Pagada #${order.orderNumber}", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        text = {
          Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
              color = Color(0xFF1B5E20).copy(alpha = 0.3f),
              border = BorderStroke(1.dp, Color(0xFF4CAF50)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("CUENTA LIQUIDADA Y CERRADA", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF81C784))
                  Text("Esta cuenta ya fue cobrada y no se puede volver a cobrar.", fontSize = 11.sp, color = Color.White)
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Mesa/Destino: ${order.tableOrDestination}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Cliente: ${order.customerName}", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Text("Método de Pago: ${order.paymentMethod.replace("_", " ")}", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (order.referenceCode.isNotBlank()) {
              Text("Referencia: ${order.referenceCode}", color = Color(0xFF81D4FA), fontSize = 12.sp)
            }
            Text("Total Pagado: $${String.format(Locale.US, "%.2f", order.totalUsd)} (${String.format(Locale.US, "%.2f", order.totalBs)} Bs)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Text("Platos consumidos:", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            val parsedItems = remember(order.itemsJson) { viewModel.parseCartItems(order.itemsJson) }
            parsedItems.forEach { item ->
              Text("• ${item.quantity}x ${item.dishName} ($${String.format(Locale.US, "%.2f", item.priceUsd * item.quantity)})", color = Color(0xFFDDDDDD), fontSize = 11.5.sp)
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.reprintPaidOrderReceipt(context, order, selectedPaperWidth)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
          ) {
            Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Re-imprimir Factura", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { paidOrderToView = null }) {
            Text("Cerrar", color = Color.White)
          }
        },
        containerColor = Color(0xFF1E1E24)
      )
    }

    // Modal de Confirmación para Eliminar Cuenta (Pruebas o Canceladas)
    orderToDelete?.let { order ->
      AlertDialog(
        onDismissRequest = { orderToDelete = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = GrillFireRed)
            Spacer(modifier = Modifier.width(8.dp))
            Text("¿Eliminar Cuenta #${order.orderNumber}?", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        text = {
          Column {
            Text(
              text = "Mesa/Destino: ${order.tableOrDestination} (${order.customerName})",
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Estado: ${if (order.isPaid) "PAGADA" else "POR COBRAR"}",
              color = if (order.isPaid) Color(0xFF81C784) else Color(0xFFFFB300),
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Esta acción eliminará definitivamente esta cuenta de la base de datos en la nube y de la lista. Ideal para limpiar cuentas de prueba.",
              color = Color(0xFFCCCCCC),
              fontSize = 12.5.sp
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteOrder(order.id)
              orderToDelete = null
              android.widget.Toast.makeText(context, "Cuenta #${order.orderNumber} eliminada", android.widget.Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GrillFireRed),
            modifier = Modifier.testTag("confirm_delete_order_button")
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sí, Eliminar Definitivamente", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { orderToDelete = null }) {
            Text("Cancelar", color = Color(0xFFAAAAAA))
          }
        },
        containerColor = Color(0xFF1E1E24)
      )
    }
  }
}

@Composable
private fun DishPosCard(
  dish: MenuItemDoc,
  exchangeRate: Double,
  quantityInCart: Int,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onCustomizeAndAdd: () -> Unit
) {
  val priceBs = dish.priceUsd * exchangeRate

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dish_card_${dish.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Ícono de parrilla / comida
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(SurfaceVariantDark),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.LocalFireDepartment,
          contentDescription = null,
          tint = GrillFireRed,
          modifier = Modifier.size(28.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = dish.name,
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp,
          color = Color.White
        )
        if (dish.description.isNotBlank()) {
          Text(
            text = dish.description,
            fontSize = 11.5.sp,
            color = Color(0xFFAAAAAA),
            maxLines = 2
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = String.format(Locale.US, "$%.2f", dish.priceUsd),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = GrillFireRed
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = String.format(Locale.US, "•  %.2f Bs", priceBs),
            fontSize = 12.sp,
            color = Color(0xFFFFB300),
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        if (quantityInCart > 0) {
          // Contador rápido en la misma tarjeta: restar / cantidad / sumar
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = onDecrement,
              modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF33333A), CircleShape)
                .testTag("decrement_${dish.id}")
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Restar", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(
              text = "$quantityInCart",
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
              onClick = onIncrement,
              modifier = Modifier
                .size(32.dp)
                .background(GrillCrimson, CircleShape)
                .testTag("increment_${dish.id}")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Sumar", tint = Color.White, modifier = Modifier.size(18.dp))
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Botón secundario para personalizar ingredientes
          IconButton(
            onClick = onCustomizeAndAdd,
            modifier = Modifier
              .size(28.dp)
              .background(Color(0xFF26262B), CircleShape)
              .testTag("customize_button_${dish.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Personalizar ingredientes",
              tint = Color(0xFFAAAAAA),
              modifier = Modifier.size(15.dp)
            )
          }
        } else {
          // Si no está en el carrito, mostrar botón rápido de añadir y botón de personalizar
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onCustomizeAndAdd,
              modifier = Modifier
                .size(34.dp)
                .background(Color(0xFF2C2C30), CircleShape)
                .testTag("customize_button_${dish.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Personalizar ingredientes",
                tint = Color.White,
                modifier = Modifier.size(17.dp)
              )
            }
            IconButton(
              onClick = onIncrement,
              modifier = Modifier
                .size(36.dp)
                .background(GrillCrimson, CircleShape)
                .testTag("quick_add_${dish.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar rápido",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ItemCustomizerDialog(
  dish: MenuItemDoc,
  recipe: List<IngredientRequirement>,
  exchangeRate: Double,
  onDismiss: () -> Unit,
  onConfirm: (quantity: Int, excluded: List<String>, notes: String) -> Unit
) {
  var quantity by remember { mutableIntStateOf(1) }
  val excludedList = remember { mutableStateListOf<String>() }
  var specialNotes by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("item_customizer_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = dish.name,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = Color.White
            )
            Text(
              text = String.format(Locale.US, "$%.2f  •  %.2f Bs c/u", dish.priceUsd, dish.priceUsd * exchangeRate),
              fontSize = 12.sp,
              color = GrillFireRed,
              fontWeight = FontWeight.SemiBold
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Personalizar Insumos (Desmarca para excluir del plato y stock):",
          fontSize = 12.sp,
          color = Color(0xFFCCCCCC),
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Lista de Ingredientes de la Receta
        if (recipe.isEmpty()) {
          Text(
            text = "Este ítem no tiene ingredientes editables configurados.",
            fontSize = 11.5.sp,
            color = Color(0xFF888888)
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
          ) {
            items(recipe) { ing ->
              val isIncluded = !excludedList.contains(ing.supplyName)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    if (isIncluded) excludedList.add(ing.supplyName) else excludedList.remove(ing.supplyName)
                  }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isIncluded,
                  onCheckedChange = { checked ->
                    if (!checked) excludedList.add(ing.supplyName) else excludedList.remove(ing.supplyName)
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = GrillCrimson,
                    uncheckedColor = Color(0xFF777777),
                    checkmarkColor = Color.White
                  )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text(
                    text = if (isIncluded) ing.supplyName else "SIN ${ing.supplyName.uppercase()}",
                    fontSize = 13.sp,
                    color = if (isIncluded) Color.White else Color(0xFFFF8A80),
                    fontWeight = if (isIncluded) FontWeight.Normal else FontWeight.Bold
                  )
                  Text(
                    text = "${ing.quantityRequired} ${ing.unit}",
                    fontSize = 11.sp,
                    color = Color(0xFF888888)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Instrucción especial para cocina
        OutlinedTextField(
          value = specialNotes,
          onValueChange = { specialNotes = it },
          label = { Text("Instrucción para cocina (ej. Término 3/4)") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("dish_special_notes_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GrillCrimson,
            unfocusedBorderColor = Color(0xFF444444),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector de Cantidad
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Cantidad:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { if (quantity > 1) quantity-- },
              modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF2C2C30), CircleShape)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Menos", tint = Color.White)
            }
            Text(
              text = "$quantity",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              modifier = Modifier.padding(horizontal = 14.dp)
            )
            IconButton(
              onClick = { quantity++ },
              modifier = Modifier
                .size(32.dp)
                .background(GrillCrimson, CircleShape)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Más", tint = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = { onConfirm(quantity, excludedList.toList(), specialNotes) },
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("confirm_custom_item_button"),
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          shape = RoundedCornerShape(23.dp)
        ) {
          Text("Agregar al Pedido", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}

@Composable
private fun CartSheetContent(
  cart: List<CartOrderItem>,
  exchangeRate: Double,
  customerName: String,
  tableOrDestination: String,
  onCustomerNameChange: (String) -> Unit,
  onTableChange: (String) -> Unit,
  onUpdateQuantity: (Int, Int) -> Unit,
  onClearCart: () -> Unit,
  onSendToKitchenFast: () -> Unit,
  onProceedToCheckout: () -> Unit
) {
  val totalUsd = cart.sumOf { it.priceUsd * it.quantity }
  val totalBs = totalUsd * exchangeRate
  val tables = listOf("Mesa 1", "Mesa 2", "Mesa 3", "Mesa 4", "Para Llevar", "Delivery", "Barra")

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("cart_sheet_content")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Resumen del Pedido",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = Color.White
      )
      IconButton(onClick = onClearCart, modifier = Modifier.testTag("clear_cart_button")) {
        Icon(Icons.Default.Delete, contentDescription = "Vaciar", tint = Color(0xFFFF5252))
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Cliente y Destino
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = customerName,
        onValueChange = onCustomerNameChange,
        label = { Text("Cliente") },
        modifier = Modifier
          .weight(1f)
          .testTag("cart_customer_name_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = GrillCrimson,
          unfocusedBorderColor = Color(0xFF444444),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
        ),
        singleLine = true
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(tables) { table ->
        val isSel = tableOrDestination == table
        Surface(
          color = if (isSel) GrillCrimson else SurfaceVariantDark,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.clickable { onTableChange(table) }
        ) {
          Text(
            text = table,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Lista de ítems en carrito
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
    ) {
      itemsIndexed(cart) { idx, item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.dishName,
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp,
              color = Color.White
            )
            Text(
              text = String.format(Locale.US, "$%.2f c/u", item.priceUsd),
              fontSize = 11.sp,
              color = Color(0xFFAAAAAA)
            )
            if (item.excludedIngredientNames.isNotEmpty()) {
              Text(
                text = "Sin: ${item.excludedIngredientNames.joinToString(", ")}",
                color = Color(0xFFFF8A80),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            if (item.specialInstructions.isNotBlank()) {
              Text(
                text = "Nota: ${item.specialInstructions}",
                color = Color(0xFFFFD54F),
                fontSize = 11.sp
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { onUpdateQuantity(idx, item.quantity - 1) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
            }
            Text(
              text = "${item.quantity}",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
              onClick = { onUpdateQuantity(idx, item.quantity + 1) },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Total
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Total a Pagar:", color = Color(0xFFAAAAAA), fontSize = 12.sp)
        Text(
          text = String.format(Locale.US, "$%.2f", totalUsd),
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = Color.White
        )
      }
      Text(
        text = String.format(Locale.US, "%.2f Bs", totalBs),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFFB300)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onSendToKitchenFast,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("cart_sheet_kitchen_button"),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
        shape = RoundedCornerShape(24.dp)
      ) {
        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("⚡ A Cocina (48mm)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
      }

      Button(
        onClick = onProceedToCheckout,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("cart_proceed_checkout_button"),
        colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
        shape = RoundedCornerShape(24.dp)
      ) {
        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Cobrar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun CheckoutMultiCurrencyDialog(
  totalUsd: Double,
  exchangeRate: Double,
  customerName: String,
  tableOrDestination: String,
  initialPaperWidth: PaperWidth = PaperWidth.WIDTH_58MM,
  onDismiss: () -> Unit,
  onConfirmCheckout: (
    method: String,
    usdPaid: Double,
    pagoMovilBs: Double,
    puntoVentaBs: Double,
    ref: String,
    isPaid: Boolean,
    notes: String,
    paperWidth: PaperWidth
  ) -> Unit
) {
  val totalBs = totalUsd * exchangeRate

  var paymentMethod by remember { mutableStateOf("USD_EFECTIVO") }
  var usdPaidText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalUsd)) }
  var pagoMovilBsText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalBs)) }
  var puntoVentaBsText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalBs)) }
  var referenceCode by remember { mutableStateOf("") }
  var orderNotes by remember { mutableStateOf("") }
  var selectedPaperWidth by remember { mutableStateOf(initialPaperWidth) }
  var isSubmitting by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("checkout_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Cobro de Pedido",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 18.sp,
              color = Color.White
            )
            Text(
              text = "$tableOrDestination • $customerName",
              fontSize = 12.sp,
              color = Color(0xFFAAAAAA)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Resumen de Montos
        Surface(
          color = SurfaceVariantDark,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Total USD", color = Color(0xFFAAAAAA), fontSize = 11.sp)
              Text(
                text = String.format(Locale.US, "$%.2f", totalUsd),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = String.format(Locale.US, "Tasa: %.2f Bs/$", exchangeRate),
                color = Color(0xFFFFB300),
                fontSize = 11.sp
              )
              Text(
                text = String.format(Locale.US, "%.2f Bs", totalBs),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFFFFB300)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Seleccione Método de Pago:",
          fontSize = 12.sp,
          color = Color(0xFFCCCCCC),
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Métodos de pago
        val methods = listOf(
          "USD_EFECTIVO" to "💵 Efectivo $",
          "PAGO_MOVIL" to "📱 Pago Móvil (Bs)",
          "PUNTO_VENTA" to "💳 Punto de Venta (Bs)",
          "MIXTO" to "🔄 Mixto ($ / Bs)"
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          methods.take(2).forEach { (key, label) ->
            val isSel = paymentMethod == key
            Surface(
              color = if (isSel) GrillCrimson else SurfaceVariantDark,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable { paymentMethod = key }
                .testTag("method_$key")
            ) {
              Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                maxLines = 1
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          methods.drop(2).forEach { (key, label) ->
            val isSel = paymentMethod == key
            Surface(
              color = if (isSel) GrillCrimson else SurfaceVariantDark,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable { paymentMethod = key }
                .testTag("method_$key")
            ) {
              Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                maxLines = 1
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Campos según el método seleccionado
        when (paymentMethod) {
          "USD_EFECTIVO" -> {
            OutlinedTextField(
              value = usdPaidText,
              onValueChange = { usdPaidText = it },
              label = { Text("Monto Recibido en Dólares ($)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_usd_paid"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
            val tenderUsd = usdPaidText.toDoubleOrNull() ?: 0.0
            if (tenderUsd > totalUsd) {
              val changeUsd = tenderUsd - totalUsd
              val changeBs = changeUsd * exchangeRate
              Text(
                text = String.format(Locale.US, "Cambio / Vuelto: $%.2f (%.2f Bs)", changeUsd, changeBs),
                color = Color(0xFF66BB6A),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
          "PAGO_MOVIL" -> {
            OutlinedTextField(
              value = pagoMovilBsText,
              onValueChange = { pagoMovilBsText = it },
              label = { Text("Monto en Bolívares (Bs)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_pagomovil_bs"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = referenceCode,
              onValueChange = { referenceCode = it },
              label = { Text("Referencia Pago Móvil (últimos 4 dígitos)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_reference_code"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
          }
          "PUNTO_VENTA" -> {
            OutlinedTextField(
              value = puntoVentaBsText,
              onValueChange = { puntoVentaBsText = it },
              label = { Text("Monto Punto de Venta (Bs)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_puntoventa_bs"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = referenceCode,
              onValueChange = { referenceCode = it },
              label = { Text("Número de Lote / Aprobación POS") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
          }
          "MIXTO" -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = usdPaidText,
                onValueChange = { usdPaidText = it },
                label = { Text("Parte USD ($)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GrillCrimson,
                  unfocusedBorderColor = Color(0xFF444444),
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                singleLine = true
              )
              OutlinedTextField(
                value = pagoMovilBsText,
                onValueChange = { pagoMovilBsText = it },
                label = { Text("Parte Bs Digital") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GrillCrimson,
                  unfocusedBorderColor = Color(0xFF444444),
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                singleLine = true
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selector de Formato de Ticket Térmico (58mm vs 80mm)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Formato de Ticket:",
            fontSize = 11.sp,
            color = Color(0xFFAAAAAA)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = selectedPaperWidth == PaperWidth.WIDTH_58MM,
              onClick = { selectedPaperWidth = PaperWidth.WIDTH_58MM },
              label = { Text("58mm", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GrillCrimson,
                selectedLabelColor = Color.White
              )
            )
            FilterChip(
              selected = selectedPaperWidth == PaperWidth.WIDTH_80MM,
              onClick = { selectedPaperWidth = PaperWidth.WIDTH_80MM },
              label = { Text("80mm", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GrillCrimson,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          enabled = !isSubmitting,
          onClick = {
            if (isSubmitting) return@Button
            isSubmitting = true
            val usdVal = usdPaidText.toDoubleOrNull() ?: 0.0
            val pmBsVal = pagoMovilBsText.toDoubleOrNull() ?: 0.0
            val pvBsVal = puntoVentaBsText.toDoubleOrNull() ?: 0.0

            val (finalUsd, finalPm, finalPv) = when (paymentMethod) {
              "USD_EFECTIVO" -> Triple(totalUsd, 0.0, 0.0)
              "PAGO_MOVIL" -> Triple(0.0, pmBsVal, 0.0)
              "PUNTO_VENTA" -> Triple(0.0, 0.0, pvBsVal)
              "MIXTO" -> Triple(usdVal, pmBsVal, 0.0)
              else -> Triple(0.0, 0.0, 0.0)
            }

            onConfirmCheckout(
              paymentMethod,
              finalUsd,
              finalPm,
              finalPv,
              referenceCode,
              true,
              orderNotes,
              selectedPaperWidth
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_checkout_button"),
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          shape = RoundedCornerShape(25.dp)
        ) {
          Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isSubmitting) "Procesando pedido..." else "Confirmar e Imprimir (${selectedPaperWidth.label.take(4)})",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}

@Composable
private fun SettlePaymentDialog(
  order: OrderDoc,
  exchangeRate: Double,
  initialPaperWidth: PaperWidth = PaperWidth.WIDTH_58MM,
  onDismiss: () -> Unit,
  onEditOrder: (() -> Unit)? = null,
  onRequestCancel: (() -> Unit)? = null,
  onConfirmSettle: (
    method: String,
    usdPaid: Double,
    pagoMovilBs: Double,
    puntoVentaBs: Double,
    ref: String,
    notes: String,
    paperWidth: PaperWidth
  ) -> Unit
) {
  val totalUsd = order.totalUsd
  val totalBs = if (order.totalBs > 0) order.totalBs else (totalUsd * exchangeRate)

  var paymentMethod by remember { mutableStateOf("USD_EFECTIVO") }
  var usdPaidText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalUsd)) }
  var pagoMovilBsText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalBs)) }
  var puntoVentaBsText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalBs)) }
  var referenceCode by remember { mutableStateOf("") }
  var settlementNotes by remember { mutableStateOf("") }
  var selectedPaperWidth by remember { mutableStateOf(initialPaperWidth) }
  var isSubmitting by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("settle_order_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Cobro de Cuenta Abierta",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 17.sp,
              color = Color.White
            )
            Text(
              text = "Comanda #${order.orderNumber} • ${order.tableOrDestination} (${order.customerName})",
              fontSize = 12.sp,
              color = Color(0xFFAAAAAA)
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onEditOrder != null) {
              IconButton(
                onClick = onEditOrder,
                modifier = Modifier.size(32.dp).background(Color(0xFF263238), CircleShape)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Modificar", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
              }
            }
            if (onRequestCancel != null) {
              IconButton(
                onClick = onRequestCancel,
                modifier = Modifier.size(32.dp).background(Color(0xFF3E2723), CircleShape)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Cancelar", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
              }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (order.isPaid) {
          Surface(
            color = Color(0xFF1B5E20),
            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text("CUENTA YA PAGADA", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF81C784))
                Text("Esta cuenta ya está liquidada. No se permite cobrarla de nuevo.", fontSize = 11.sp, color = Color.White)
              }
            }
          }
        }

        // Resumen del Total
        Surface(
          color = SurfaceVariantDark,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Total a Cobrar:", color = Color(0xFFAAAAAA), fontSize = 11.sp)
              Text(
                text = String.format(Locale.US, "$%.2f USD", totalUsd),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Equivalente en Bs:", color = Color(0xFFAAAAAA), fontSize = 11.sp)
              Text(
                text = String.format(Locale.US, "%.2f Bs", totalBs),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFFFFB300)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Método de Pago Utilizado:",
          fontSize = 12.sp,
          color = Color(0xFFCCCCCC),
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        val methods = listOf(
          "USD_EFECTIVO" to "💵 Efectivo $",
          "PAGO_MOVIL" to "📱 Pago Móvil (Bs)",
          "PUNTO_VENTA" to "💳 Punto de Venta (Bs)",
          "MIXTO" to "🔄 Mixto ($ / Bs)"
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          methods.take(2).forEach { (key, label) ->
            val isSel = paymentMethod == key
            Surface(
              color = if (isSel) GrillCrimson else SurfaceVariantDark,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable { paymentMethod = key }
                .testTag("settle_method_$key")
            ) {
              Text(
                text = label,
                color = Color.White,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                maxLines = 1
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          methods.drop(2).forEach { (key, label) ->
            val isSel = paymentMethod == key
            Surface(
              color = if (isSel) GrillCrimson else SurfaceVariantDark,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable { paymentMethod = key }
                .testTag("settle_method_$key")
            ) {
              Text(
                text = label,
                color = Color.White,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                maxLines = 1
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (paymentMethod) {
          "USD_EFECTIVO" -> {
            OutlinedTextField(
              value = usdPaidText,
              onValueChange = { usdPaidText = it },
              label = { Text("Monto Recibido en Dólares ($)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settle_input_usd"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
            val tenderUsd = usdPaidText.toDoubleOrNull() ?: 0.0
            if (tenderUsd > totalUsd) {
              val changeUsd = tenderUsd - totalUsd
              val changeBs = changeUsd * exchangeRate
              Text(
                text = String.format(Locale.US, "Vuelto a Entregar: $%.2f  (%.2f Bs)", changeUsd, changeBs),
                color = Color(0xFF66BB6A),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
          "PAGO_MOVIL" -> {
            OutlinedTextField(
              value = pagoMovilBsText,
              onValueChange = { pagoMovilBsText = it },
              label = { Text("Monto en Bolívares (Bs)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settle_input_pagomovil_bs"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = referenceCode,
              onValueChange = { referenceCode = it },
              label = { Text("Referencia Pago Móvil (últimos 4 dígitos)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settle_input_ref"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
          }
          "PUNTO_VENTA" -> {
            OutlinedTextField(
              value = puntoVentaBsText,
              onValueChange = { puntoVentaBsText = it },
              label = { Text("Monto Punto de Venta (Bs)") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settle_input_pv_bs"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = referenceCode,
              onValueChange = { referenceCode = it },
              label = { Text("Lote / Código de Aprobación POS") },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )
          }
          "MIXTO" -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              OutlinedTextField(
                value = usdPaidText,
                onValueChange = { usdPaidText = it },
                label = { Text("Parte USD ($)") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GrillCrimson,
                  unfocusedBorderColor = Color(0xFF444444),
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                singleLine = true
              )
              OutlinedTextField(
                value = pagoMovilBsText,
                onValueChange = { pagoMovilBsText = it },
                label = { Text("Parte Bs Digital") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GrillCrimson,
                  unfocusedBorderColor = Color(0xFF444444),
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White
                ),
                singleLine = true
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selector de Formato de Ticket Térmico (58mm vs 80mm)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Formato de Factura:",
            fontSize = 11.sp,
            color = Color(0xFFAAAAAA)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = selectedPaperWidth == PaperWidth.WIDTH_58MM,
              onClick = { selectedPaperWidth = PaperWidth.WIDTH_58MM },
              label = { Text("58mm", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GrillCrimson,
                selectedLabelColor = Color.White
              )
            )
            FilterChip(
              selected = selectedPaperWidth == PaperWidth.WIDTH_80MM,
              onClick = { selectedPaperWidth = PaperWidth.WIDTH_80MM },
              label = { Text("80mm", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GrillCrimson,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          enabled = !isSubmitting && !order.isPaid,
          onClick = {
            if (isSubmitting || order.isPaid) return@Button
            isSubmitting = true
            val usdVal = usdPaidText.toDoubleOrNull() ?: 0.0
            val pmBsVal = pagoMovilBsText.toDoubleOrNull() ?: 0.0
            val pvBsVal = puntoVentaBsText.toDoubleOrNull() ?: 0.0

            val (finalUsd, finalPm, finalPv) = when (paymentMethod) {
              "USD_EFECTIVO" -> Triple(totalUsd, 0.0, 0.0)
              "PAGO_MOVIL" -> Triple(0.0, pmBsVal, 0.0)
              "PUNTO_VENTA" -> Triple(0.0, 0.0, pvBsVal)
              "MIXTO" -> Triple(usdVal, pmBsVal, 0.0)
              else -> Triple(0.0, 0.0, 0.0)
            }

            onConfirmSettle(
              paymentMethod,
              finalUsd,
              finalPm,
              finalPv,
              referenceCode,
              settlementNotes,
              selectedPaperWidth
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("confirm_settle_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (order.isPaid) Color(0xFF33333E) else GrillCrimson,
            disabledContainerColor = Color(0xFF2E2E36)
          ),
          shape = RoundedCornerShape(25.dp)
        ) {
          Icon(
            if (order.isPaid) Icons.Default.CheckCircle else Icons.Default.Print,
            contentDescription = null,
            tint = if (order.isPaid) Color(0xFF81C784) else Color.White
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = when {
              order.isPaid -> "Cuenta ya Cobrada (Cerrada)"
              isSubmitting -> "Procesando pago..."
              else -> "💳 Registrar Pago e Imprimir (${selectedPaperWidth.label.take(4)})"
            },
            fontWeight = FontWeight.Bold,
            color = if (order.isPaid) Color(0xFF81C784) else Color.White,
            fontSize = 13.5.sp
          )
        }
      }
    }
  }
}

/**
 * Diálogo de Edición de Cuenta Abierta:
 * Permite al cajero agregar bebidas, platos o modificar cantidades a una comanda abierta
 * antes de que el cliente pida la cuenta final.
 */
@Composable
private fun EditOpenOrderDialog(
  order: OrderDoc,
  menuItems: List<MenuItemDoc>,
  exchangeRate: Double,
  onDismiss: () -> Unit,
  onSave: (
    updatedItems: List<CartOrderItem>,
    newTotalUsd: Double,
    newTotalBs: Double,
    newlyAddedItems: List<CartOrderItem>
  ) -> Unit,
  onRequestCancel: () -> Unit
) {
  val initialItems = remember(order.itemsJson) {
    try {
      val arr = org.json.JSONArray(order.itemsJson)
      val list = mutableListOf<CartOrderItem>()
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val exArr = o.optJSONArray("excludedIngredientNames") ?: org.json.JSONArray()
        val ex = mutableListOf<String>()
        for (j in 0 until exArr.length()) ex.add(exArr.getString(j))
        list.add(
          CartOrderItem(
            menuItemId = o.optString("menuItemId", ""),
            dishName = o.getString("dishName"),
            quantity = o.getInt("quantity"),
            priceUsd = o.getDouble("priceUsd"),
            excludedIngredientNames = ex,
            specialInstructions = o.optString("specialInstructions", "")
          )
        )
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }

  val currentItems = remember {
    mutableStateListOf<CartOrderItem>().apply { addAll(initialItems) }
  }

  var selectedCatalogCategory by remember { mutableStateOf("Bebidas") }
  val catalogCategories = listOf("Bebidas", "Parrillas", "Hamburguesas & Pepitos", "Entradas & Guarniciones")

  val currentTotalUsd = currentItems.sumOf { it.priceUsd * it.quantity }
  val currentTotalBs = currentTotalUsd * exchangeRate

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(580.dp)
        .testTag("edit_open_order_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A20))
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Cabecera
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Modificar Cuenta Abierta",
              fontWeight = FontWeight.Black,
              fontSize = 17.sp,
              color = Color.White
            )
            Text(
              text = "Comanda #${order.orderNumber} • ${order.tableOrDestination} (${order.customerName})",
              fontSize = 12.sp,
              color = Color(0xFF38BDF8),
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total dinámico
        Surface(
          color = SurfaceVariantDark,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Total Cuenta:", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Text(
              text = String.format(Locale.US, "$%.2f USD  (%.2f Bs)", currentTotalUsd, currentTotalBs),
              color = Color(0xFFFFB300),
              fontWeight = FontWeight.Black,
              fontSize = 15.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista de Ítems actuales en la cuenta
        Text(
          text = "Consumo Actual en Mesa:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          itemsIndexed(currentItems) { index, item ->
            Surface(
              color = Color(0xFF26262E),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.dishName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  Text(
                    text = String.format(Locale.US, "$%.2f c/u", item.priceUsd),
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  IconButton(
                    onClick = {
                      if (item.quantity > 1) {
                        currentItems[index] = item.copy(quantity = item.quantity - 1)
                      } else {
                        currentItems.removeAt(index)
                      }
                    },
                    modifier = Modifier.size(28.dp).background(Color(0xFF33333E), CircleShape)
                  ) {
                    Icon(Icons.Default.Remove, contentDescription = "Restar", tint = Color.White, modifier = Modifier.size(14.dp))
                  }

                  Text(
                    text = "${item.quantity}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                  )

                  IconButton(
                    onClick = {
                      currentItems[index] = item.copy(quantity = item.quantity + 1)
                    },
                    modifier = Modifier.size(28.dp).background(GrillCrimson, CircleShape)
                  ) {
                    Icon(Icons.Default.Add, contentDescription = "Sumar", tint = Color.White, modifier = Modifier.size(14.dp))
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sección: Agregar más Bebidas o Platos al cliente
        Text(
          text = "+ Agregar Bebida o Plato a la Cuenta:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF38BDF8)
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(catalogCategories) { cat ->
            FilterChip(
              selected = selectedCatalogCategory == cat,
              onClick = { selectedCatalogCategory = cat },
              label = { Text(cat, fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GrillCrimson,
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF26262E),
                labelColor = Color(0xFFCCCCCC)
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val dishesToAdd = menuItems.filter { it.category.equals(selectedCatalogCategory, ignoreCase = true) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(dishesToAdd) { dish ->
            Surface(
              color = Color(0xFF2D2D38),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .clickable {
                  val existingIdx = currentItems.indexOfFirst { it.menuItemId == dish.id }
                  if (existingIdx >= 0) {
                    val curr = currentItems[existingIdx]
                    currentItems[existingIdx] = curr.copy(quantity = curr.quantity + 1)
                  } else {
                    currentItems.add(
                      CartOrderItem(
                        menuItemId = dish.id,
                        dishName = dish.name,
                        quantity = 1,
                        priceUsd = dish.priceUsd,
                        excludedIngredientNames = emptyList(),
                        specialInstructions = ""
                      )
                    )
                  }
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                  Text(dish.name, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                  Text(String.format(Locale.US, "$%.2f", dish.priceUsd), color = Color(0xFFFFB300), fontSize = 10.5.sp)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botones de Guardar / Cancelar Pedido
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onRequestCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
            border = BorderStroke(1.dp, Color(0xFFFF5252))
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Cancelar", fontSize = 11.5.sp)
          }

          Button(
            onClick = {
              val newlyAdded = mutableListOf<CartOrderItem>()
              for (cur in currentItems) {
                val prev = initialItems.find { it.menuItemId == cur.menuItemId }
                val prevQty = prev?.quantity ?: 0
                if (cur.quantity > prevQty) {
                  newlyAdded.add(cur.copy(quantity = cur.quantity - prevQty))
                }
              }

              onSave(currentItems.toList(), currentTotalUsd, currentTotalBs, newlyAdded)
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson)
          ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Guardar Cambios", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}


