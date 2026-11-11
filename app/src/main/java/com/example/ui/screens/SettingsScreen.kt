package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.model.SystemUserDoc
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessConfigDoc
import com.example.data.model.UserRole
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.ParrillaViewModel
import java.util.Locale

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

@Composable
fun SettingsScreen(viewModel: ParrillaViewModel) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val config by viewModel.config.collectAsState()
  val serverUrl by viewModel.webServerUrl.collectAsState()
  val currentUserProfile by viewModel.userProfile.collectAsState()
  val loggedUser by viewModel.currentLoggedUser.collectAsState()
  val systemUsers by viewModel.systemUsers.collectAsState()
  val role = viewModel.currentEffectiveRole
  val actualRole = if (loggedUser?.role.equals("ADMIN", ignoreCase = true)) UserRole.ADMIN else UserRole.STAFF

  // Estados para diálogos de usuarios y claves
  var showAddUserDialog by remember { mutableStateOf(false) }
  var userToEdit by remember { mutableStateOf<SystemUserDoc?>(null) }
  var userToDelete by remember { mutableStateOf<SystemUserDoc?>(null) }
  var showAdminQuickChangeDialog by remember { mutableStateOf(false) }

  // Formulario de Negocio y Ticket
  var businessName by remember(config) { mutableStateOf(config.businessName) }
  var rif by remember(config) { mutableStateOf(config.rif) }
  var phone by remember(config) { mutableStateOf(config.phone) }
  var address by remember(config) { mutableStateOf(config.address) }
  var ticketHeader by remember(config) { mutableStateOf(config.ticketHeader) }
  var ticketFooter by remember(config) { mutableStateOf(config.ticketFooter) }
  var rateText by remember(config) { mutableStateOf(String.format(Locale.US, "%.2f", config.exchangeRateUsdToBs)) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(12.dp)
      .testTag("settings_screen"),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Encabezado
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Panel de Configuración",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 19.sp,
          color = Color.White
        )
      }
    }

    // 1. Tasa de Cambio del Día (Bs / USD)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Tasa de Cambio Oficial (Bs/$)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = rateText,
              onValueChange = { rateText = it },
              label = { Text("Valor en Bs por cada $1 USD") },
              modifier = Modifier
                .weight(1f)
                .testTag("exchange_rate_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrillCrimson,
                unfocusedBorderColor = Color(0xFF444444),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              singleLine = true
            )

            Button(
              onClick = {
                val r = rateText.toDoubleOrNull() ?: 65.0
                viewModel.updateExchangeRate(r)
              },
              colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
              modifier = Modifier.testTag("save_rate_button")
            ) {
              Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }
    }

    // 2. Acceso y Gestión desde Computadora (PC / Web)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("pc_web_server_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Computer, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Gestión por Computadora (PC / Web)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
            Surface(
              color = Color(0xFF1B5E20),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = "🟢 SERVIDOR ACTIVO",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Conecta cualquier PC, laptop o tablet a la misma red WiFi para operar el sistema en pantalla completa. Podrás tomar pedidos rápido, ver la pantalla de cocina (KDS), cobrar comandas y revisar inventario y cierres.",
            fontSize = 11.5.sp,
            color = Color(0xFFAAAAAA)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            color = Color(0xFF16161A),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF33333C)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("Dirección de Acceso Local (Abrir en Chrome, Edge o Firefox):", fontSize = 10.5.sp, color = Color(0xFF999999))
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = serverUrl,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF4FC3F7)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                clipboardManager.setText(AnnotatedString(serverUrl))
                Toast.makeText(context, "Enlace copiado: $serverUrl", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
              modifier = Modifier
                .weight(1f)
                .testTag("copy_pc_url_button"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Copiar", fontSize = 11.5.sp, color = Color.White)
            }

            Button(
              onClick = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                  putExtra(Intent.EXTRA_TEXT, "Acceso a La Parrilla 200 en Computadora: $serverUrl")
                  type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Compartir enlace PC")
                context.startActivity(shareIntent)
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
              modifier = Modifier
                .weight(1f)
                .testTag("share_pc_url_button"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Compartir", fontSize = 11.5.sp, color = Color.White)
            }

            Button(
              onClick = {
                try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
                  context.startActivity(intent)
                } catch (e: Exception) {
                  Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
              modifier = Modifier
                .weight(1f)
                .testTag("open_pc_browser_button"),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Abrir", fontSize = 11.5.sp, color = Color.White)
            }
          }
        }
      }
    }

    // 3. Personalización de la Impresión Térmica (15t2 - 48mm)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Print, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Impresión Térmica 15t2 (48mm x 216mm)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }
          }

          Text(
            text = "Personaliza el encabezado y pie de página de los comprobantes y comandas.",
            fontSize = 11.5.sp,
            color = Color(0xFFAAAAAA)
          )

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Nombre del Negocio") },
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
              value = rif,
              onValueChange = { rif = it },
              label = { Text("RIF") },
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
              value = phone,
              onValueChange = { phone = it },
              label = { Text("Teléfono") },
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

          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Dirección") },
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

          OutlinedTextField(
            value = ticketHeader,
            onValueChange = { ticketHeader = it },
            label = { Text("Mensaje de Encabezado") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrillCrimson,
              unfocusedBorderColor = Color(0xFF444444),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )

          Spacer(modifier = Modifier.height(6.dp))

          OutlinedTextField(
            value = ticketFooter,
            onValueChange = { ticketFooter = it },
            label = { Text("Mensaje de Despedida / Pie de Ticket") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrillCrimson,
              unfocusedBorderColor = Color(0xFF444444),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val updated = config.copy(
                  businessName = businessName,
                  rif = rif,
                  phone = phone,
                  address = address,
                  ticketHeader = ticketHeader,
                  ticketFooter = ticketFooter
                )
                viewModel.saveConfig(updated)
              },
              colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
              modifier = Modifier
                .weight(1f)
                .testTag("save_printer_config_button")
            ) {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Guardar Cambios", fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
              onClick = {
                val sampleText = """
                  ================================
                          LA PARRILLA 200         
                         RIF: $rif
                     Telf: $phone
                  ================================
                  TICKET DE PRUEBA 48MM
                  IMPRESORA TERMICA 15T2
                  --------------------------------
                  * Impresión de 32 columnas OK
                  * Sincronización en la nube OK
                  * Descuento de stock en vivo OK
                  ================================
                  $ticketFooter
                  ================================
                """.trimIndent()
                viewModel.showThermalTicket("Prueba Térmica 15t2", sampleText)
              },
              modifier = Modifier.testTag("test_print_button"),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
              Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Probar 48mm")
            }
          }
        }
      }
    }

    // 3. Gestión de Usuarios, Personal y Claves (Sin Correos / Google)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Group, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Gestión de Personal y Claves", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            }

            if (actualRole == UserRole.ADMIN) {
              Button(
                onClick = { showAddUserDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_staff_button")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nuevo Personal", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Text(
            text = "Administra las cuentas de acceso, cambia usuarios y claves en cualquier momento y agrega nuevo personal.",
            fontSize = 11.5.sp,
            color = Color(0xFFAAAAAA)
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Tarjeta destacada de Modificación Rápida de Administrador
          val adminUser = systemUsers.firstOrNull { it.role.equals("ADMIN", ignoreCase = true) }
            ?: SystemUserDoc(username = "admin", password = "admin200", displayName = "Administrador", role = "ADMIN")

          Surface(
            color = Color(0xFF261D1D),
            border = androidx.compose.foundation.BorderStroke(1.dp, GrillCrimson),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("👑 Cuenta Administrador", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                  Text(
                    text = "Usuario: @${adminUser.username}  |  Clave: ${adminUser.password}",
                    fontSize = 12.sp,
                    color = Color(0xFFFFB300),
                    fontWeight = FontWeight.Bold
                  )
                }

                if (actualRole == UserRole.ADMIN) {
                  Button(
                    onClick = { showAdminQuickChangeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("change_admin_creds_button")
                  ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cambiar Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Conmutador de vista de rol
          if (actualRole == UserRole.ADMIN) {
            Surface(
              color = Color(0xFF26262B),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Modo de Vista de Pantalla", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                  Text(
                    text = "Viendo como: ${if (role == UserRole.ADMIN) "👑 Administrador (Acceso Total)" else "🍳 Personal (Solo Pedidos e Inventario)"}",
                    fontSize = 11.sp,
                    color = if (role == UserRole.ADMIN) GrillCrimson else GreenSuccess,
                    fontWeight = FontWeight.Bold
                  )
                }

                Button(
                  onClick = {
                    if (role == UserRole.ADMIN) {
                      viewModel.toggleRoleViewMode(UserRole.STAFF)
                    } else {
                      viewModel.toggleRoleViewMode(UserRole.ADMIN)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = if (role == UserRole.ADMIN) GreenSuccess else GrillCrimson),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (role == UserRole.ADMIN) "Ver como Cocina" else "Ver como Admin",
                    fontSize = 11.sp,
                    color = Color.White
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text("Cuentas de Usuarios Registradas:", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color.White)
          Spacer(modifier = Modifier.height(8.dp))

          if (systemUsers.isEmpty()) {
            Text(
              text = "Cargando usuarios o lista vacía. El admin inicial es @admin / clave: admin200.",
              fontSize = 11.sp,
              color = Color(0xFF888888)
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              for (usr in systemUsers) {
                val isCurrentLoggedUser = usr.username.equals(loggedUser?.username, ignoreCase = true)
                var showPassword by remember { mutableStateOf(false) }

                Surface(
                  color = Color(0xFF222226),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = usr.displayName.ifBlank { usr.username },
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.5.sp,
                          color = Color.White
                        )
                        if (isCurrentLoggedUser) {
                          Spacer(modifier = Modifier.width(6.dp))
                          Surface(
                            color = Color(0xFF455A64),
                            shape = RoundedCornerShape(4.dp)
                          ) {
                            Text(
                              text = "SESIÓN ACTUAL",
                              fontSize = 9.sp,
                              color = Color.White,
                              fontWeight = FontWeight.Black,
                              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                          }
                        }
                      }

                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = "@${usr.username}",
                          fontSize = 11.5.sp,
                          color = Color(0xFFFFB300),
                          fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "• Clave: ${if (showPassword) usr.password else "••••••"}",
                          fontSize = 11.sp,
                          color = Color(0xFFAAAAAA)
                        )
                        IconButton(
                          onClick = { showPassword = !showPassword },
                          modifier = Modifier.size(24.dp)
                        ) {
                          Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Mostrar clave",
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(14.dp)
                          )
                        }
                      }

                      Surface(
                        color = if (usr.role.equals("ADMIN", ignoreCase = true)) GrillCrimson.copy(alpha = 0.2f) else GreenSuccess.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                      ) {
                        Text(
                          text = if (usr.role.equals("ADMIN", ignoreCase = true)) "👑 ROL: ADMINISTRADOR (CONTROL TOTAL)" else "🍳 ROL: PERSONAL (STAFF)",
                          fontSize = 9.5.sp,
                          color = if (usr.role.equals("ADMIN", ignoreCase = true)) Color(0xFFFF8A80) else Color(0xFFA5D6A7),
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }

                      if (!usr.isAdmin) {
                        val appPerms = mutableListOf<String>()
                        if (usr.canUsePosApp) appPerms.add("POS")
                        if (usr.canUseKitchenApp) appPerms.add("Cocina")
                        if (usr.canUseBillingApp) appPerms.add("Cobrar")
                        if (usr.canUseInventoryApp) appPerms.add("Inventario")
                        if (usr.canUseClosuresApp) appPerms.add("Cierres")
                        if (usr.canUseMenuApp) appPerms.add("Menú")
                        if (usr.canUseSettingsApp) appPerms.add("Ajustes")

                        val pcPerms = mutableListOf<String>()
                        if (usr.canAccessPc) {
                          if (usr.canUsePosPc) pcPerms.add("POS")
                          if (usr.canUseKitchenPc) pcPerms.add("Cocina")
                          if (usr.canUseBillingPc) pcPerms.add("Cobros")
                          if (usr.canUseInventoryPc) pcPerms.add("Stock")
                          if (usr.canUseReportsPc) pcPerms.add("Reportes")
                        }

                        Text(
                          text = "📱 App: ${if (appPerms.isEmpty()) "Sin permisos" else appPerms.joinToString(", ")}",
                          fontSize = 10.sp,
                          color = Color(0xFF81D4FA),
                          modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                          text = if (usr.canAccessPc) "💻 PC: ${if (pcPerms.isEmpty()) "Solo lectura" else pcPerms.joinToString(", ")}" else "💻 PC: 🚫 Sin acceso a computadora",
                          fontSize = 10.sp,
                          color = if (usr.canAccessPc) Color(0xFFA5D6A7) else Color(0xFFFF8A80),
                          modifier = Modifier.padding(top = 1.dp)
                        )
                      }
                    }

                    // Acciones de edición y eliminación (solo si el que está conectado es ADMIN)
                    if (actualRole == UserRole.ADMIN) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                      ) {
                        IconButton(
                          onClick = { userToEdit = usr },
                          modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_user_${usr.username}")
                        ) {
                          Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(18.dp)
                          )
                        }

                        IconButton(
                          onClick = { userToDelete = usr },
                          modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_user_${usr.username}")
                        ) {
                          Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // --- DIÁLOGOS DE GESTIÓN DE USUARIOS ---

  // 1. Diálogo Agregar Nuevo Personal
  // 1. Diálogo Agregar Nuevo Personal
  if (showAddUserDialog) {
    var newName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("STAFF") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    // Permisos App
    var canUsePosApp by remember { mutableStateOf(true) }
    var canUseKitchenApp by remember { mutableStateOf(true) }
    var canUseBillingApp by remember { mutableStateOf(true) }
    var canUseInventoryApp by remember { mutableStateOf(false) }
    var canUseClosuresApp by remember { mutableStateOf(false) }
    var canUseMenuApp by remember { mutableStateOf(false) }
    var canUseSettingsApp by remember { mutableStateOf(false) }

    // Permisos PC
    var canAccessPc by remember { mutableStateOf(true) }
    var canUsePosPc by remember { mutableStateOf(true) }
    var canUseKitchenPc by remember { mutableStateOf(true) }
    var canUseBillingPc by remember { mutableStateOf(true) }
    var canUseInventoryPc by remember { mutableStateOf(false) }
    var canUseReportsPc by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showAddUserDialog = false },
      containerColor = DeepCharcoal,
      title = {
        Text("Agregar Nuevo Personal / Usuario", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            OutlinedTextField(
              value = newName,
              onValueChange = { newName = it },
              label = { Text("Nombre del Colaborador (Ej. María Gómez)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("add_user_name_input"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          }

          item {
            OutlinedTextField(
              value = newUsername,
              onValueChange = { newUsername = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
              label = { Text("Usuario para Iniciar Sesión (Ej. maria)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("add_user_username_input"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          }

          item {
            OutlinedTextField(
              value = newPassword,
              onValueChange = { newPassword = it },
              label = { Text("Clave / Contraseña (Ej. 1234)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("add_user_password_input"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          }

          item {
            Text("Rol Principal:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                selected = newRole == "STAFF",
                onClick = { newRole = "STAFF" },
                colors = RadioButtonDefaults.colors(selectedColor = GreenSuccess)
              )
              Text("🍳 Personal (Con Permisos Personalizados)", color = Color.White, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                selected = newRole == "ADMIN",
                onClick = { newRole = "ADMIN" },
                colors = RadioButtonDefaults.colors(selectedColor = GrillCrimson)
              )
              Text("👑 Administrador (Acceso Total a Todo)", color = Color.White, fontSize = 12.sp)
            }
          }

          if (newRole == "STAFF") {
            item {
              Surface(
                color = Color(0xFF222226),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("📱 Permisos en la Aplicación Android:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                  PermissionCheckboxItem("Tomar Pedidos (POS)", canUsePosApp) { canUsePosApp = it }
                  PermissionCheckboxItem("Monitor de Cocina (KDS)", canUseKitchenApp) { canUseKitchenApp = it }
                  PermissionCheckboxItem("Cobrar Cuentas y Facturar", canUseBillingApp) { canUseBillingApp = it }
                  PermissionCheckboxItem("Inventario e Insumos", canUseInventoryApp) { canUseInventoryApp = it }
                  PermissionCheckboxItem("Cierres de Caja y Finanzas", canUseClosuresApp) { canUseClosuresApp = it }
                  PermissionCheckboxItem("Menú y Platos", canUseMenuApp) { canUseMenuApp = it }
                  PermissionCheckboxItem("Ajustes del Sistema", canUseSettingsApp) { canUseSettingsApp = it }
                }
              }
            }

            item {
              Surface(
                color = Color(0xFF222226),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("💻 Permisos en la Computadora (PC / Web):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFA5D6A7))
                  PermissionCheckboxItem("Permitir Acceso a la Computadora (PC)", canAccessPc) { canAccessPc = it }
                  if (canAccessPc) {
                    PermissionCheckboxItem("• Tomar Pedidos en PC", canUsePosPc) { canUsePosPc = it }
                    PermissionCheckboxItem("• Monitor de Cocina en PC", canUseKitchenPc) { canUseKitchenPc = it }
                    PermissionCheckboxItem("• Cobros y Cuentas en PC", canUseBillingPc) { canUseBillingPc = it }
                    PermissionCheckboxItem("• Inventario en PC", canUseInventoryPc) { canUseInventoryPc = it }
                    PermissionCheckboxItem("• Cierres y Finanzas en PC", canUseReportsPc) { canUseReportsPc = it }
                  } else {
                    Text("🚫 Acceso a PC bloqueado para este usuario", fontSize = 10.sp, color = Color(0xFFFF8A80), modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                  }
                }
              }
            }
          }

          if (dialogError != null) {
            item {
              Text(text = dialogError ?: "", color = Color(0xFFFF5252), fontSize = 11.5.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newUsername.isBlank() || newPassword.isBlank()) {
              dialogError = "Usuario y clave son obligatorios"
              return@Button
            }
            val newUser = SystemUserDoc(
              id = newUsername.trim().lowercase(),
              username = newUsername.trim().lowercase(),
              password = newPassword.trim(),
              displayName = newName.ifBlank { newUsername },
              role = newRole,
              canUsePosApp = if (newRole == "ADMIN") true else canUsePosApp,
              canUseKitchenApp = if (newRole == "ADMIN") true else canUseKitchenApp,
              canUseBillingApp = if (newRole == "ADMIN") true else canUseBillingApp,
              canUseInventoryApp = if (newRole == "ADMIN") true else canUseInventoryApp,
              canUseClosuresApp = if (newRole == "ADMIN") true else canUseClosuresApp,
              canUseMenuApp = if (newRole == "ADMIN") true else canUseMenuApp,
              canUseSettingsApp = if (newRole == "ADMIN") true else canUseSettingsApp,
              canAccessPc = if (newRole == "ADMIN") true else canAccessPc,
              canUsePosPc = if (newRole == "ADMIN") true else canUsePosPc,
              canUseKitchenPc = if (newRole == "ADMIN") true else canUseKitchenPc,
              canUseBillingPc = if (newRole == "ADMIN") true else canUseBillingPc,
              canUseInventoryPc = if (newRole == "ADMIN") true else canUseInventoryPc,
              canUseReportsPc = if (newRole == "ADMIN") true else canUseReportsPc
            )
            viewModel.saveSystemUser(newUser) { success, err ->
              if (success) {
                showAddUserDialog = false
              } else {
                dialogError = err
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          modifier = Modifier.testTag("confirm_add_user_button")
        ) {
          Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddUserDialog = false }) {
          Text("Cancelar", color = Color(0xFFAAAAAA))
        }
      }
    )
  }

  // 2. Diálogo Editar Usuario y Clave con Permisos
  userToEdit?.let { targetUser ->
    var editName by remember(targetUser) { mutableStateOf(targetUser.displayName) }
    var editUsername by remember(targetUser) { mutableStateOf(targetUser.username) }
    var editPassword by remember(targetUser) { mutableStateOf(targetUser.password) }
    var editRole by remember(targetUser) { mutableStateOf(targetUser.role) }
    var editError by remember(targetUser) { mutableStateOf<String?>(null) }

    // Permisos App
    var editCanUsePosApp by remember(targetUser) { mutableStateOf(targetUser.canUsePosApp) }
    var editCanUseKitchenApp by remember(targetUser) { mutableStateOf(targetUser.canUseKitchenApp) }
    var editCanUseBillingApp by remember(targetUser) { mutableStateOf(targetUser.canUseBillingApp) }
    var editCanUseInventoryApp by remember(targetUser) { mutableStateOf(targetUser.canUseInventoryApp) }
    var editCanUseClosuresApp by remember(targetUser) { mutableStateOf(targetUser.canUseClosuresApp) }
    var editCanUseMenuApp by remember(targetUser) { mutableStateOf(targetUser.canUseMenuApp) }
    var editCanUseSettingsApp by remember(targetUser) { mutableStateOf(targetUser.canUseSettingsApp) }

    // Permisos PC
    var editCanAccessPc by remember(targetUser) { mutableStateOf(targetUser.canAccessPc) }
    var editCanUsePosPc by remember(targetUser) { mutableStateOf(targetUser.canUsePosPc) }
    var editCanUseKitchenPc by remember(targetUser) { mutableStateOf(targetUser.canUseKitchenPc) }
    var editCanUseBillingPc by remember(targetUser) { mutableStateOf(targetUser.canUseBillingPc) }
    var editCanUseInventoryPc by remember(targetUser) { mutableStateOf(targetUser.canUseInventoryPc) }
    var editCanUseReportsPc by remember(targetUser) { mutableStateOf(targetUser.canUseReportsPc) }

    AlertDialog(
      onDismissRequest = { userToEdit = null },
      containerColor = DeepCharcoal,
      title = {
        Text("Editar Usuario y Permisos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            OutlinedTextField(
              value = editName,
              onValueChange = { editName = it },
              label = { Text("Nombre Completo") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          }

          item {
            OutlinedTextField(
              value = editUsername,
              onValueChange = { editUsername = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
              label = { Text("Nombre de Usuario") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("edit_username_input"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          }

          item {
            OutlinedTextField(
              value = editPassword,
              onValueChange = { editPassword = it },
              label = { Text("Contraseña / Clave") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("edit_password_input"),
              colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
          }

          item {
            Text("Rol Asignado:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                selected = editRole == "STAFF",
                onClick = { editRole = "STAFF" },
                colors = RadioButtonDefaults.colors(selectedColor = GreenSuccess)
              )
              Text("🍳 Personal (Con Permisos Asignados)", color = Color.White, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              RadioButton(
                selected = editRole == "ADMIN",
                onClick = { editRole = "ADMIN" },
                colors = RadioButtonDefaults.colors(selectedColor = GrillCrimson)
              )
              Text("👑 Administrador (Acceso Total)", color = Color.White, fontSize = 12.sp)
            }
          }

          if (editRole == "STAFF") {
            item {
              Surface(
                color = Color(0xFF222226),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("📱 Permisos en la Aplicación Android:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF38BDF8))
                  PermissionCheckboxItem("Tomar Pedidos (POS)", editCanUsePosApp) { editCanUsePosApp = it }
                  PermissionCheckboxItem("Monitor de Cocina (KDS)", editCanUseKitchenApp) { editCanUseKitchenApp = it }
                  PermissionCheckboxItem("Cobrar Cuentas y Facturar", editCanUseBillingApp) { editCanUseBillingApp = it }
                  PermissionCheckboxItem("Inventario e Insumos", editCanUseInventoryApp) { editCanUseInventoryApp = it }
                  PermissionCheckboxItem("Cierres de Caja y Finanzas", editCanUseClosuresApp) { editCanUseClosuresApp = it }
                  PermissionCheckboxItem("Menú y Platos", editCanUseMenuApp) { editCanUseMenuApp = it }
                  PermissionCheckboxItem("Ajustes del Sistema", editCanUseSettingsApp) { editCanUseSettingsApp = it }
                }
              }
            }

            item {
              Surface(
                color = Color(0xFF222226),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text("💻 Permisos en la Computadora (PC / Web):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFA5D6A7))
                  PermissionCheckboxItem("Permitir Acceso a la Computadora (PC)", editCanAccessPc) { editCanAccessPc = it }
                  if (editCanAccessPc) {
                    PermissionCheckboxItem("• Tomar Pedidos en PC", editCanUsePosPc) { editCanUsePosPc = it }
                    PermissionCheckboxItem("• Monitor de Cocina en PC", editCanUseKitchenPc) { editCanUseKitchenPc = it }
                    PermissionCheckboxItem("• Cobros y Cuentas en PC", editCanUseBillingPc) { editCanUseBillingPc = it }
                    PermissionCheckboxItem("• Inventario en PC", editCanUseInventoryPc) { editCanUseInventoryPc = it }
                    PermissionCheckboxItem("• Cierres y Finanzas en PC", editCanUseReportsPc) { editCanUseReportsPc = it }
                  } else {
                    Text("🚫 Acceso a PC bloqueado para este usuario", fontSize = 10.sp, color = Color(0xFFFF8A80), modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                  }
                }
              }
            }
          }

          if (editError != null) {
            item {
              Text(text = editError ?: "", color = Color(0xFFFF5252), fontSize = 11.5.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editUsername.isBlank() || editPassword.isBlank()) {
              editError = "Usuario y clave no pueden estar vacíos"
              return@Button
            }
            val updated = targetUser.copy(
              displayName = editName.ifBlank { editUsername },
              username = editUsername.trim().lowercase(),
              password = editPassword.trim(),
              role = editRole,
              canUsePosApp = if (editRole == "ADMIN") true else editCanUsePosApp,
              canUseKitchenApp = if (editRole == "ADMIN") true else editCanUseKitchenApp,
              canUseBillingApp = if (editRole == "ADMIN") true else editCanUseBillingApp,
              canUseInventoryApp = if (editRole == "ADMIN") true else editCanUseInventoryApp,
              canUseClosuresApp = if (editRole == "ADMIN") true else editCanUseClosuresApp,
              canUseMenuApp = if (editRole == "ADMIN") true else editCanUseMenuApp,
              canUseSettingsApp = if (editRole == "ADMIN") true else editCanUseSettingsApp,
              canAccessPc = if (editRole == "ADMIN") true else editCanAccessPc,
              canUsePosPc = if (editRole == "ADMIN") true else editCanUsePosPc,
              canUseKitchenPc = if (editRole == "ADMIN") true else editCanUseKitchenPc,
              canUseBillingPc = if (editRole == "ADMIN") true else editCanUseBillingPc,
              canUseInventoryPc = if (editRole == "ADMIN") true else editCanUseInventoryPc,
              canUseReportsPc = if (editRole == "ADMIN") true else editCanUseReportsPc
            )
            viewModel.saveSystemUser(updated, previousUsername = targetUser.username) { success, err ->
              if (success) {
                userToEdit = null
              } else {
                editError = err
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          modifier = Modifier.testTag("confirm_edit_user_button")
        ) {
          Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { userToEdit = null }) {
          Text("Cancelar", color = Color(0xFFAAAAAA))
        }
      }
    )
  }

  // 3. Diálogo Eliminar Usuario
  userToDelete?.let { targetUser ->
    AlertDialog(
      onDismissRequest = { userToDelete = null },
      containerColor = DeepCharcoal,
      title = {
        Text("¿Eliminar Usuario?", color = Color.White, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          text = "¿Estás seguro de que deseas eliminar la cuenta de '@${targetUser.username}' (${targetUser.displayName})? Esta acción no se puede deshacer.",
          color = Color(0xFFDDDDDD),
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteSystemUser(targetUser.username) { _, _ ->
              userToDelete = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
          modifier = Modifier.testTag("confirm_delete_user_button")
        ) {
          Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { userToDelete = null }) {
          Text("Cancelar", color = Color(0xFFAAAAAA))
        }
      }
    )
  }

  // 4. Diálogo Modificar Usuario y Clave de Administrador
  if (showAdminQuickChangeDialog) {
    val currentAdmin = systemUsers.firstOrNull { it.role.equals("ADMIN", ignoreCase = true) }
      ?: SystemUserDoc(username = "admin", password = "admin200", displayName = "Administrador", role = "ADMIN")

    var adminUsernameInput by remember { mutableStateOf(currentAdmin.username) }
    var adminPasswordInput by remember { mutableStateOf(currentAdmin.password) }
    var adminNameInput by remember { mutableStateOf(currentAdmin.displayName) }
    var adminError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showAdminQuickChangeDialog = false },
      containerColor = DeepCharcoal,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Cambiar Usuario y Clave Admin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Personaliza las credenciales maestras del Administrador a tu antojo.",
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA)
          )

          OutlinedTextField(
            value = adminNameInput,
            onValueChange = { adminNameInput = it },
            label = { Text("Nombre del Administrador") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_name_input"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
          )

          OutlinedTextField(
            value = adminUsernameInput,
            onValueChange = { adminUsernameInput = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
            label = { Text("Nuevo Usuario Administrador") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_username_input"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
          )

          OutlinedTextField(
            value = adminPasswordInput,
            onValueChange = { adminPasswordInput = it },
            label = { Text("Nueva Clave Administrador") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GrillCrimson, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
          )

          if (adminError != null) {
            Text(text = adminError ?: "", color = Color(0xFFFF5252), fontSize = 11.5.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (adminUsernameInput.isBlank() || adminPasswordInput.isBlank()) {
              adminError = "El usuario y la clave no pueden estar vacíos"
              return@Button
            }
            val updatedAdmin = currentAdmin.copy(
              displayName = adminNameInput.ifBlank { "Administrador" },
              username = adminUsernameInput.trim().lowercase(),
              password = adminPasswordInput.trim(),
              role = "ADMIN"
            )
            viewModel.saveSystemUser(updatedAdmin, previousUsername = currentAdmin.username) { success, err ->
              if (success) {
                showAdminQuickChangeDialog = false
              } else {
                adminError = err
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          modifier = Modifier.testTag("confirm_change_admin_button")
        ) {
          Text("Actualizar Admin", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAdminQuickChangeDialog = false }) {
          Text("Cancelar", color = Color(0xFFAAAAAA))
        }
      }
    )
  }
}

@Composable
private fun PermissionCheckboxItem(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) }
      .padding(vertical = 2.dp)
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(
        checkedColor = GrillCrimson,
        uncheckedColor = Color(0xFF666666),
        checkmarkColor = Color.White
      )
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      color = if (checked) Color.White else Color(0xFFAAAAAA),
      fontSize = 12.sp,
      fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
    )
  }
}
