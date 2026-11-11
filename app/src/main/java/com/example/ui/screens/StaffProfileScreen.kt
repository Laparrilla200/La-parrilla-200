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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.ParrillaViewModel

@Composable
fun StaffProfileScreen(
  viewModel: ParrillaViewModel,
  onSignOut: () -> Unit
) {
  val loggedUser by viewModel.currentLoggedUser.collectAsState()
  val role = viewModel.currentEffectiveRole
  val isActualAdmin = loggedUser?.role.equals("ADMIN", ignoreCase = true)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
      .testTag("staff_profile_screen"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Avatar de Usuario
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(if (role == UserRole.ADMIN) GrillCrimson else GreenSuccess),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Avatar",
        tint = Color.White,
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = loggedUser?.displayName ?: "Personal de Parrilla",
      fontWeight = FontWeight.ExtraBold,
      fontSize = 20.sp,
      color = Color.White
    )

    Text(
      text = "@${loggedUser?.username ?: "usuario"}",
      fontSize = 13.sp,
      color = Color(0xFFAAAAAA)
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Badge del rol asignado
    Surface(
      color = if (role == UserRole.ADMIN) GrillCrimson else Color(0xFF2E7D32),
      shape = RoundedCornerShape(12.dp)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (role == UserRole.ADMIN) "ROL: ADMINISTRADOR" else "ROL: PERSONAL (STAFF)",
          fontWeight = FontWeight.Black,
          fontSize = 12.sp,
          color = Color.White
        )
      }
    }

    // Banner si el admin está simulando la vista de personal
    if (isActualAdmin && role == UserRole.STAFF) {
      Spacer(modifier = Modifier.height(14.dp))
      Surface(
        color = GrillCrimson.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GrillCrimson),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Modo Simulación de Personal",
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp,
              color = Color.White
            )
            Text(
              text = "Eres Administrador probando la experiencia de un empleado.",
              fontSize = 11.sp,
              color = Color(0xFFFFCDD2)
            )
          }
          Button(
            onClick = { viewModel.toggleRoleViewMode(UserRole.ADMIN) },
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Volver a Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Permisos del Rol Personal
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Permisos Asignados a tu Cuenta",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
          isAllowed = true,
          title = "Tomar Pedidos y Cobro (POS)",
          description = "Acceso completo a la venta, selección de ingredientes y facturación en USD / Bs."
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionItem(
          isAllowed = true,
          title = "Comandas de Cocina (KDS)",
          description = "Visualización en tiempo real de órdenes de cocina, despacho e impresión de comandas."
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionItem(
          isAllowed = true,
          title = "Consultar Inventario y Stock",
          description = "Verificar disponibilidad de insumos, niveles de alerta y reposición de existencias."
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionItem(
          isAllowed = false,
          title = "Cierres de Caja y Reportes Financieros",
          description = "Restringido al Administrador. Cuadres de caja diarios, semanales y mensuales."
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionItem(
          isAllowed = false,
          title = "Configuración del Negocio y Tasa de Cambio",
          description = "Restringido al Administrador. Ajustes fiscales, datos de impresora y valor del dólar."
        )

        Spacer(modifier = Modifier.height(8.dp))

        PermissionItem(
          isAllowed = false,
          title = "Gestión de Roles y Usuarios",
          description = "Restringido al Administrador. Asignar o promover permisos a otros colaboradores."
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Botón Cerrar Sesión
    OutlinedButton(
      onClick = onSignOut,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("staff_logout_button"),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
      shape = RoundedCornerShape(24.dp)
    ) {
      Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Cerrar Turno / Salir", fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun PermissionItem(
  isAllowed: Boolean,
  title: String,
  description: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Icon(
      imageVector = if (isAllowed) Icons.Default.CheckCircle else Icons.Default.Lock,
      contentDescription = null,
      tint = if (isAllowed) GreenSuccess else Color(0xFFFF5252),
      modifier = Modifier
        .size(18.dp)
        .padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = if (isAllowed) Color.White else Color(0xFFAAAAAA)
      )
      Text(
        text = description,
        fontSize = 11.sp,
        color = Color(0xFF888888),
        lineHeight = 14.sp
      )
    }
  }
}
