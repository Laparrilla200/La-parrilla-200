package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClosureDoc
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ClosuresScreen(viewModel: ParrillaViewModel) {
  val orders by viewModel.orders.collectAsState()
  val closures by viewModel.closures.collectAsState()
  val config by viewModel.config.collectAsState()
  val role = viewModel.currentEffectiveRole

  var showConfirmClosureDialog by remember { mutableStateOf<String?>(null) } // "DIARIO", "SEMANAL", "MENSUAL"
  var filterClosureType by remember { mutableStateOf("TODOS") }

  // Calcular métricas de hoy en vivo
  val calendar = Calendar.getInstance()
  calendar.set(Calendar.HOUR_OF_DAY, 0)
  calendar.set(Calendar.MINUTE, 0)
  calendar.set(Calendar.SECOND, 0)
  val startOfToday = calendar.timeInMillis

  val todaysOrders = orders.filter { it.timestamp >= startOfToday && it.status != "CANCELADO" }
  val totalSalesTodayUsd = todaysOrders.sumOf { it.totalUsd }
  val totalSalesTodayBs = todaysOrders.sumOf { it.totalBs }
  val usdCashToday = todaysOrders.sumOf { it.amountUsdPaid }
  val pagoMovilBsToday = todaysOrders.sumOf { it.amountPagoMovilBs }
  val puntoVentaBsToday = todaysOrders.sumOf { it.amountPuntoVentaBs }

  val filteredClosures = if (filterClosureType == "TODOS") {
    closures
  } else {
    closures.filter { it.closureType.equals(filterClosureType, ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(12.dp)
      .testTag("closures_screen")
  ) {
    // Encabezado
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Assessment, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Cierres de Caja & Finanzas",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 19.sp,
          color = Color.White
        )
      }

      Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "${todaysOrders.size} pedidos hoy",
          fontSize = 12.sp,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Tarjeta de Ventas de Hoy en Vivo
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GrillCrimson, RoundedCornerShape(14.dp)),
          colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Turno de Hoy (En Vivo)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
              )
              Text(
                text = String.format(Locale.US, "Tasa: %.2f Bs/$", config.exchangeRateUsdToBs),
                color = Color(0xFFFFB300),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text("Total Dólares:", color = Color(0xFFAAAAAA), fontSize = 11.5.sp)
                Text(
                  text = String.format(Locale.US, "$%.2f", totalSalesTodayUsd),
                  fontWeight = FontWeight.Black,
                  fontSize = 22.sp,
                  color = Color.White
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text("Total Bolívares:", color = Color(0xFFAAAAAA), fontSize = 11.5.sp)
                Text(
                  text = String.format(Locale.US, "%.2f Bs", totalSalesTodayBs),
                  fontWeight = FontWeight.Black,
                  fontSize = 20.sp,
                  color = Color(0xFFFFB300)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF333333)))
            Spacer(modifier = Modifier.height(10.dp))

            Text("Desglose por Métodos de Pago:", color = Color(0xFFDDDDDD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            // Desglose: Efectivo USD, Pago Móvil Bs, Punto de Venta Bs
            PaymentMethodRow(
              icon = Icons.Default.Payments,
              iconColor = Color(0xFF81C784),
              label = "Efectivo Dólares ($):",
              amountText = String.format(Locale.US, "$%.2f", usdCashToday)
            )
            Spacer(modifier = Modifier.height(4.dp))
            PaymentMethodRow(
              icon = Icons.Default.PhoneAndroid,
              iconColor = Color(0xFF64B5F6),
              label = "Pago Móvil (Bs):",
              amountText = String.format(Locale.US, "%.2f Bs", pagoMovilBsToday)
            )
            Spacer(modifier = Modifier.height(4.dp))
            PaymentMethodRow(
              icon = Icons.Default.CreditCard,
              iconColor = Color(0xFFFFB74D),
              label = "Punto de Venta (Bs):",
              amountText = String.format(Locale.US, "%.2f Bs", puntoVentaBsToday)
            )
          }
        }
      }

      // 2. Botones de Generación de Cierres (Diario, Semanal, Mensual)
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Generar Cierre e Imprimir Ticket 48mm",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color.White
            )
            Text(
              text = "Calcula automáticamente el cuadre de caja y emite ticket térmico 15t2",
              fontSize = 11.5.sp,
              color = Color(0xFFAAAAAA)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Cierre Diario
              Button(
                onClick = { showConfirmClosureDialog = "DIARIO" },
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_cierre_diario"),
                colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cierre Diario", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
              }

              // Cierre Semanal
              OutlinedButton(
                onClick = { showConfirmClosureDialog = "SEMANAL" },
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_cierre_semanal"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Semanal", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }

              // Cierre Mensual
              OutlinedButton(
                onClick = { showConfirmClosureDialog = "MENSUAL" },
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_cierre_mensual"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Mensual", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 3. Historial de Cierres
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Historial de Cierres Emitidos",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
          )

          // Filtro
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("TODOS", "DIARIO", "SEMANAL", "MENSUAL").forEach { type ->
              val isSel = filterClosureType == type
              Surface(
                color = if (isSel) GrillCrimson else SurfaceVariantDark,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(vertical = 2.dp)
              ) {
                Text(
                  text = type,
                  fontSize = 9.sp,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }

      if (filteredClosures.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No hay registros de cierres guardados aún.",
              color = Color(0xFFAAAAAA),
              fontSize = 12.sp
            )
          }
        }
      } else {
        items(filteredClosures) { closure ->
          ClosureHistoryCard(
            closure = closure,
            onReprintTicket = { viewModel.showClosureTicket(closure) }
          )
        }
      }
    }

    // Diálogo de Confirmación de Cierre
    showConfirmClosureDialog?.let { closureType ->
      AlertDialog(
        onDismissRequest = { showConfirmClosureDialog = null },
        containerColor = Color(0xFF1E1E22),
        title = {
          Text("¿Generar Cierre $closureType?", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
          Column {
            Text(
              "Esta acción auditará y totalizará todas las ventas del período en dólares ($), pago móvil y punto de venta en bolívares, guardará el registro permanente en la nube y generará el ticket térmico para la máquina 15t2.",
              color = Color(0xFFDDDDDD),
              fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              "Total hoy estimado: $${String.format(Locale.US, "%.2f", totalSalesTodayUsd)} (${String.format(Locale.US, "%.2f", totalSalesTodayBs)} Bs)",
              color = Color(0xFFFFB300),
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.generateCashClosure(closureType)
              showConfirmClosureDialog = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
            modifier = Modifier.testTag("confirm_generate_closure_button")
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Generar e Imprimir 48mm", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        dismissButton = {
          OutlinedButton(onClick = { showConfirmClosureDialog = null }) {
            Text("Cancelar", color = Color.White)
          }
        }
      )
    }
  }
}

@Composable
private fun PaymentMethodRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconColor: Color,
  label: String,
  amountText: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(label, color = Color(0xFFAAAAAA), fontSize = 12.sp)
    }
    Text(amountText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
  }
}

@Composable
private fun ClosureHistoryCard(
  closure: ClosureDoc,
  onReprintTicket: () -> Unit
) {
  val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
  val dateStr = dateFormat.format(Date(closure.timestamp))

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("closure_card_${closure.id}"),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = GrillCrimson,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = closure.closureType,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Cierre #${String.format(Locale.US, "%04d", closure.closureNumber)}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(text = dateStr, color = Color(0xFFAAAAAA), fontSize = 11.sp)
        Text(
          text = "Pedidos: ${closure.orderCount} • Resp: ${closure.closedBy}",
          color = Color(0xFF888888),
          fontSize = 10.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = String.format(Locale.US, "$%.2f", closure.totalSalesUsd),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.5.sp,
            color = Color.White
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = String.format(Locale.US, "(%.2f Bs)", closure.totalSalesBs),
            fontSize = 12.sp,
            color = Color(0xFFFFB300),
            fontWeight = FontWeight.Bold
          )
        }
      }

      IconButton(
        onClick = onReprintTicket,
        modifier = Modifier
          .size(40.dp)
          .background(SurfaceVariantDark, RoundedCornerShape(8.dp))
          .testTag("reprint_closure_${closure.id}")
      ) {
        Icon(Icons.Default.Print, contentDescription = "Reimprimir", tint = Color.White, modifier = Modifier.size(20.dp))
      }
    }
  }
}
