package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.SupplyDoc
import com.example.data.model.UserRole
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.ParrillaViewModel
import com.example.ui.viewmodel.RestockItem
import java.util.Locale

@Composable
fun InventoryScreen(viewModel: ParrillaViewModel) {
  val supplies by viewModel.supplies.collectAsState()
  val criticalSupplies by viewModel.criticalSupplies.collectAsState()
  val restockRecommendations by viewModel.restockRecommendations.collectAsState()
  val config by viewModel.config.collectAsState()
  val role = viewModel.currentEffectiveRole

  var selectedTab by remember { mutableIntStateOf(0) }
  var supplyToRestock by remember { mutableStateOf<SupplyDoc?>(null) }
  var supplyToEdit by remember { mutableStateOf<SupplyDoc?>(null) }
  var showNewSupplyDialog by remember { mutableStateOf(false) }

  val totalEstimatedBudgetUsd = restockRecommendations.sumOf { it.estimatedCostUsd }
  val totalEstimatedBudgetBs = restockRecommendations.sumOf { it.estimatedCostBs }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(12.dp)
      .testTag("inventory_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Cabecera
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Inventory, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(26.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Inventario e Insumos",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 19.sp,
            color = Color.White
          )
        }

        if (role == UserRole.ADMIN) {
          Button(
            onClick = { showNewSupplyDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("add_supply_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nuevo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Tarjetas de Resumen
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          color = DeepCharcoal,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Insumos", color = Color(0xFFAAAAAA), fontSize = 11.sp)
            Text("${supplies.size}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
          }
        }

        Surface(
          color = if (criticalSupplies.isNotEmpty()) GrillCrimson.copy(alpha = 0.25f) else DeepCharcoal,
          shape = RoundedCornerShape(12.dp),
          border = if (criticalSupplies.isNotEmpty()) androidx.compose.foundation.BorderStroke(1.dp, GrillCrimson) else null,
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Punto Crítico", color = if (criticalSupplies.isNotEmpty()) Color(0xFFFF8A80) else Color(0xFFAAAAAA), fontSize = 11.sp)
            Text(
              text = "${criticalSupplies.size}",
              fontWeight = FontWeight.Black,
              fontSize = 18.sp,
              color = if (criticalSupplies.isNotEmpty()) Color(0xFFFF5252) else Color.White
            )
          }
        }

        Surface(
          color = DeepCharcoal,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Presupuesto Sug.", color = Color(0xFFAAAAAA), fontSize = 10.5.sp)
            Text(
              text = String.format(Locale.US, "$%.1f", totalEstimatedBudgetUsd),
              fontWeight = FontWeight.Black,
              fontSize = 16.sp,
              color = Color(0xFFFFB300)
            )
          }
        }
      }

      // Alerta de Stock Crítico Banner
      if (criticalSupplies.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          color = GrillCrimson.copy(alpha = 0.2f),
          border = androidx.compose.foundation.BorderStroke(1.dp, GrillFireRed),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("critical_stock_banner")
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "¡Alerta de Reabastecimiento Crítico!",
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = Color.White
              )
              Text(
                text = "${criticalSupplies.size} insumos están en o por debajo de su punto de quiebre.",
                fontSize = 11.sp,
                color = Color(0xFFFFCDD2)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Pestañas
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = DeepCharcoal,
        contentColor = Color.White,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = GrillCrimson
          )
        }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Text(
              text = "Todos (${supplies.size})",
              fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp
            )
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Text(
              text = "Sugerencias de Compra (${restockRecommendations.size})",
              fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp,
              color = if (criticalSupplies.isNotEmpty()) Color(0xFFFF8A80) else Color.White
            )
          }
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Contenido de la pestaña
      if (selectedTab == 0) {
        // Lista Completa de Insumos
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(supplies) { supply ->
            SupplyItemRow(
              supply = supply,
              exchangeRate = config.exchangeRateUsdToBs,
              isAdmin = role == UserRole.ADMIN,
              onQuickRestock = { supplyToRestock = supply },
              onEdit = { supplyToEdit = supply }
            )
          }
        }
      } else {
        // Pestaña de Sugerencias de Reabastecimiento Automático
        if (restockRecommendations.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = "¡Excelente! Todos los insumos tienen stock saludable.",
              color = GreenSuccess,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item {
              Surface(
                color = SurfaceVariantDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("Presupuesto Total Estimado:", color = Color(0xFFAAAAAA), fontSize = 12.sp)
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = String.format(Locale.US, "$%.2f USD", totalEstimatedBudgetUsd),
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      fontSize = 14.sp
                    )
                    Text(
                      text = String.format(Locale.US, "%.2f Bs", totalEstimatedBudgetBs),
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFFFB300),
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }

            items(restockRecommendations) { item ->
              RestockRecommendationCard(
                item = item,
                onRestockRecommended = {
                  viewModel.restockSupply(item.supply.id, item.toBuyAmount)
                },
                onCustomRestock = { supplyToRestock = item.supply }
              )
            }
          }
        }
      }
    }

    // Diálogo de Reabastecer Stock
    supplyToRestock?.let { sup ->
      RestockDialog(
        supply = sup,
        exchangeRate = config.exchangeRateUsdToBs,
        onDismiss = { supplyToRestock = null },
        onConfirm = { amount ->
          viewModel.restockSupply(sup.id, amount)
          supplyToRestock = null
        }
      )
    }

    // Diálogo de Agregar / Editar Insumo
    if (showNewSupplyDialog || supplyToEdit != null) {
      val editing = supplyToEdit
      EditSupplyDialog(
        initialSupply = editing,
        onDismiss = {
          showNewSupplyDialog = false
          supplyToEdit = null
        },
        onSave = { saved ->
          viewModel.saveSupply(saved)
          showNewSupplyDialog = false
          supplyToEdit = null
        }
      )
    }
  }
}

@Composable
private fun SupplyItemRow(
  supply: SupplyDoc,
  exchangeRate: Double,
  isAdmin: Boolean,
  onQuickRestock: () -> Unit,
  onEdit: () -> Unit
) {
  val isCritical = supply.currentStock <= supply.minStockThreshold
  val progress = if (supply.minStockThreshold > 0) {
    (supply.currentStock / (supply.minStockThreshold * 2.0)).toFloat().coerceIn(0f, 1f)
  } else 1f

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isCritical) 1.5.dp else 0.5.dp,
        color = if (isCritical) GrillCrimson else Color(0xFF333333),
        shape = RoundedCornerShape(10.dp)
      )
      .testTag("supply_row_${supply.id}"),
    colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = supply.name,
              fontWeight = FontWeight.Bold,
              fontSize = 14.5.sp,
              color = Color.White
            )
            if (isCritical) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = GrillCrimson,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "CRÍTICO",
                  color = Color.White,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Black,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
          }
          Text(
            text = "Mínimo: ${supply.minStockThreshold} ${supply.unit}  •  Costo: $${supply.costUsd}/${supply.unit}",
            fontSize = 11.5.sp,
            color = Color(0xFFAAAAAA)
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "${String.format(Locale.US, "%.2f", supply.currentStock)} ${supply.unit}",
              fontWeight = FontWeight.ExtraBold,
              fontSize = 16.sp,
              color = if (isCritical) Color(0xFFFF5252) else Color.White
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = onQuickRestock,
            modifier = Modifier
              .size(32.dp)
              .background(GrillCrimson, CircleShape)
              .testTag("restock_button_${supply.id}")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Reponer", tint = Color.White, modifier = Modifier.size(18.dp))
          }
          if (isAdmin) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
        color = if (isCritical) GrillCrimson else GreenSuccess,
        trackColor = Color(0xFF333333)
      )
    }
  }
}

@Composable
private fun RestockRecommendationCard(
  item: RestockItem,
  onRestockRecommended: () -> Unit,
  onCustomRestock: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, AmberWarning, RoundedCornerShape(10.dp)),
    colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.supply.name,
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp,
          color = Color.White
        )
        Text(
          text = "Actual: ${item.supply.currentStock} ${item.supply.unit}  (Faltan: ${String.format(Locale.US, "%.1f", item.shortageAmount)} ${item.supply.unit})",
          fontSize = 11.5.sp,
          color = Color(0xFFFF8A80)
        )
        Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Comprar: +${item.toBuyAmount} ${item.supply.unit}",
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD54F),
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = String.format(Locale.US, "• Est: $%.2f (%.2f Bs)", item.estimatedCostUsd, item.estimatedCostBs),
            fontSize = 11.sp,
            color = Color(0xFFAAAAAA)
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Button(
          onClick = onRestockRecommended,
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Text("Comprar", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}

@Composable
private fun RestockDialog(
  supply: SupplyDoc,
  exchangeRate: Double,
  onDismiss: () -> Unit,
  onConfirm: (Double) -> Unit
) {
  var amountText by remember { mutableStateOf("${supply.recommendedRestock}") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier.fillMaxWidth().testTag("restock_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text("Reabastecer Insumo", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
        Text("${supply.name} (Stock actual: ${supply.currentStock} ${supply.unit})", fontSize = 12.sp, color = Color(0xFFAAAAAA))

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Cantidad a agregar (${supply.unit})") },
          modifier = Modifier.fillMaxWidth().testTag("restock_amount_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GrillCrimson,
            unfocusedBorderColor = Color(0xFF444444),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          singleLine = true
        )

        val addVal = amountText.toDoubleOrNull() ?: 0.0
        val costUsd = addVal * supply.costUsd
        val costBs = costUsd * exchangeRate

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = String.format(Locale.US, "Costo estimado: $%.2f USD  •  %.2f Bs", costUsd, costBs),
          color = Color(0xFFFFB300),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Cancelar", color = Color.White)
          }
          Button(
            onClick = {
              if (addVal > 0) onConfirm(addVal)
            },
            modifier = Modifier.weight(1f).testTag("confirm_restock_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson)
          ) {
            Text("Registrar", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
private fun EditSupplyDialog(
  initialSupply: SupplyDoc?,
  onDismiss: () -> Unit,
  onSave: (SupplyDoc) -> Unit
) {
  var name by remember { mutableStateOf(initialSupply?.name ?: "") }
  var stockText by remember { mutableStateOf("${initialSupply?.currentStock ?: 10.0}") }
  var thresholdText by remember { mutableStateOf("${initialSupply?.minStockThreshold ?: 5.0}") }
  var unit by remember { mutableStateOf(initialSupply?.unit ?: "kg") }
  var costText by remember { mutableStateOf("${initialSupply?.costUsd ?: 2.0}") }
  var recommendedText by remember { mutableStateOf("${initialSupply?.recommendedRestock ?: 10.0}") }

  val units = listOf("kg", "g", "ud", "lt", "porc")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier.fillMaxWidth().testTag("edit_supply_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = if (initialSupply == null) "Nuevo Insumo" else "Editar Insumo",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre del Insumo") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GrillCrimson,
            unfocusedBorderColor = Color(0xFF444444),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = stockText,
            onValueChange = { stockText = it },
            label = { Text("Stock Actual") },
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
            value = thresholdText,
            onValueChange = { thresholdText = it },
            label = { Text("Punto Crítico") },
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

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = costText,
            onValueChange = { costText = it },
            label = { Text("Costo USD ($)") },
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
            value = recommendedText,
            onValueChange = { recommendedText = it },
            label = { Text("Compra Sug.") },
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

        Spacer(modifier = Modifier.height(8.dp))
        Text("Unidad de Medida:", color = Color(0xFFAAAAAA), fontSize = 11.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          units.forEach { u ->
            val isSel = unit == u
            Surface(
              color = if (isSel) GrillCrimson else SurfaceVariantDark,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.clickable { unit = u }
            ) {
              Text(
                text = u,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Cancelar", color = Color.White)
          }
          Button(
            onClick = {
              if (name.isNotBlank()) {
                val toSave = SupplyDoc(
                  id = initialSupply?.id ?: "",
                  name = name,
                  currentStock = stockText.toDoubleOrNull() ?: 0.0,
                  minStockThreshold = thresholdText.toDoubleOrNull() ?: 5.0,
                  unit = unit,
                  costUsd = costText.toDoubleOrNull() ?: 0.0,
                  recommendedRestock = recommendedText.toDoubleOrNull() ?: 10.0
                )
                onSave(toSave)
              }
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson)
          ) {
            Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }
}
