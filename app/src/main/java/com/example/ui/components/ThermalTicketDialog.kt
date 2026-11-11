package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.printer.ThermalTicketHelper
import com.example.printer.EscPosBuilder
import com.example.printer.PaperWidth
import com.example.printer.ThermalPrinterManager
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.ThermalInk
import com.example.ui.theme.ThermalPaper
import kotlinx.coroutines.launch

@Composable
fun ThermalTicketDialog(
  title: String,
  ticketText: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val coroutineScope = rememberCoroutineScope()

  var selectedPaperWidth by remember {
    mutableStateOf(ThermalPrinterManager.getPaperWidth(context))
  }
  var showDirectPrintDialog by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
        .testTag("thermal_ticket_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Encabezado del diálogo
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            )
            Text(
              text = "Impresora Térmica (${selectedPaperWidth.label})",
              color = Color(0xFFAAAAAA),
              fontSize = 11.sp
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_ticket_button")) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de Ancho de Papel Térmico (58mm vs 80mm)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = selectedPaperWidth == PaperWidth.WIDTH_58MM,
            onClick = {
              selectedPaperWidth = PaperWidth.WIDTH_58MM
              ThermalPrinterManager.savePaperWidth(context, PaperWidth.WIDTH_58MM)
            },
            label = { Text("Rollo 58mm (32 Col)", fontSize = 11.sp) },
            leadingIcon = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = GrillCrimson,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = selectedPaperWidth == PaperWidth.WIDTH_80MM,
            onClick = {
              selectedPaperWidth = PaperWidth.WIDTH_80MM
              ThermalPrinterManager.savePaperWidth(context, PaperWidth.WIDTH_80MM)
            },
            label = { Text("Rollo 80mm (48 Col)", fontSize = 11.sp) },
            leadingIcon = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) {
              { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = GrillCrimson,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visualización del Rollo Térmico
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp)
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp)),
          color = ThermalPaper,
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 4.dp
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 12.dp, vertical = 14.dp)
          ) {
            Text(
              text = ticketText,
              fontFamily = FontFamily.Monospace,
              fontSize = if (selectedPaperWidth == PaperWidth.WIDTH_58MM) 11.sp else 12.sp,
              color = ThermalInk,
              lineHeight = 15.sp,
              letterSpacing = (-0.2).sp
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Botones Principales de Acción
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Imprimir mediante WebView (con CSS @media print ajustado a 58/80mm sin márgenes)
          Button(
            onClick = {
              ThermalTicketHelper.printTicketViaPrintManager(
                context = context,
                title = title,
                ticketText = ticketText,
                paperWidth = selectedPaperWidth
              )
            },
            modifier = Modifier
              .weight(1.2f)
              .testTag("print_ticket_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson)
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Imprimir (WebView)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
          }

          // Imprimir directo ESC/POS (Red TCP o Bluetooth)
          Button(
            onClick = { showDirectPrintDialog = true },
            modifier = Modifier
              .weight(1f)
              .testTag("escpos_direct_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A30))
          ) {
            Icon(Icons.Default.Router, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ESC/POS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Opciones Secundarias (Compartir y Copiar)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              ThermalTicketHelper.shareTicketText(context, title, ticketText)
            },
            modifier = Modifier
              .weight(1f)
              .testTag("share_ticket_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
          ) {
            Icon(Icons.Default.Share, contentDescription = "Compartir", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Compartir", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(ticketText))
              Toast.makeText(context, "Ticket copiado al portapapeles", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
              .weight(1f)
              .testTag("copy_ticket_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copiar", fontSize = 12.sp)
          }
        }
      }
    }
  }

  // Diálogo para Envío Directo por Red (IP) o Bluetooth
  if (showDirectPrintDialog) {
    DirectEscPosDialog(
      paperWidth = selectedPaperWidth,
      ticketText = ticketText,
      onDismiss = { showDirectPrintDialog = false }
    )
  }
}

@Composable
private fun DirectEscPosDialog(
  paperWidth: PaperWidth,
  ticketText: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var printerMode by remember { mutableStateOf("NETWORK") } // "NETWORK" o "BLUETOOTH"
  var ipAddress by remember { mutableStateOf(ThermalPrinterManager.getPrinterIp(context)) }
  var portText by remember { mutableStateOf(ThermalPrinterManager.getPrinterPort(context).toString()) }
  var isSending by remember { mutableStateOf(false) }

  val pairedDevices = remember { ThermalPrinterManager.getPairedBluetoothPrinters() }
  var selectedBtAddress by remember {
    mutableStateOf(pairedDevices.firstOrNull()?.second ?: "")
  }

  Dialog(onDismissRequest = { if (!isSending) onDismiss() }) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF222226))
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Impresión Directa ESC/POS",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          IconButton(onClick = onDismiss, enabled = !isSending) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
          }
        }

        Text(
          text = "Envía comandos nativos con alineación, avance de papel y corte automático.",
          color = Color(0xFFAAAAAA),
          fontSize = 11.sp,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector Red vs Bluetooth
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = printerMode == "NETWORK",
            onClick = { printerMode = "NETWORK" },
            label = { Text("Red / Wi-Fi (IP)", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(14.dp)) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = GrillCrimson,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = printerMode == "BLUETOOTH",
            onClick = { printerMode = "BLUETOOTH" },
            label = { Text("Bluetooth", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(14.dp)) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = GrillCrimson,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (printerMode == "NETWORK") {
          OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("IP de Impresora (ej: 192.168.1.100)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrillCrimson,
              unfocusedBorderColor = Color(0xFF444444),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = portText,
            onValueChange = { portText = it },
            label = { Text("Puerto RAW (Estándar 9100)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrillCrimson,
              unfocusedBorderColor = Color(0xFF444444),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )
        } else {
          if (pairedDevices.isEmpty()) {
            Text(
              text = "No se encontraron impresoras Bluetooth emparejadas. Vincula tu impresora térmica en los ajustes de Bluetooth de Android.",
              color = Color(0xFFFFB74D),
              fontSize = 12.sp,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          } else {
            Text("Impresoras Vinculadas:", color = Color.White, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              pairedDevices.forEach { (name, address) ->
                FilterChip(
                  selected = selectedBtAddress == address,
                  onClick = { selectedBtAddress = address },
                  label = { Text("$name ($address)", fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GrillCrimson,
                    selectedLabelColor = Color.White
                  ),
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            isSending = true
            coroutineScope.launch {
              // Generamos el payload ESC/POS con el builder
              val builder = EscPosBuilder(paperWidth)
              // Dividimos el texto del ticket en líneas y lo enviamos estructurado
              for (line in ticketText.lines()) {
                if (line.startsWith("===") || line.startsWith("---") || line.startsWith("***")) {
                  builder.divider(line.first())
                } else if (line.contains("LA PARRILLA") || line.contains("TICKET:") || line.contains("COMANDA")) {
                  builder.bold(true).textLine(line).bold(false)
                } else {
                  builder.textLine(line)
                }
              }
              // Corte de papel con avance al final
              builder.cut()
              val bytes = builder.build()

              val result = if (printerMode == "NETWORK") {
                val port = portText.toIntOrNull() ?: 9100
                ThermalPrinterManager.savePrinterIp(context, ipAddress.trim(), port)
                ThermalPrinterManager.printViaNetwork(ipAddress.trim(), port, bytes)
              } else {
                ThermalPrinterManager.printViaBluetooth(selectedBtAddress, bytes)
              }

              isSending = false
              result.onSuccess { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onDismiss()
              }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Fallo al enviar a impresora", Toast.LENGTH_LONG).show()
              }
            }
          },
          enabled = !isSending && (printerMode == "NETWORK" || selectedBtAddress.isNotBlank()),
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson)
        ) {
          if (isSending) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enviando comandos ESC/POS...")
          } else {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enviar a Impresora Térmica", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
