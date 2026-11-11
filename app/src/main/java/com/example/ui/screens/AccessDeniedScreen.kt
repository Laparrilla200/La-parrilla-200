package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GrillCrimson

@Composable
fun AccessDeniedScreen(
  featureName: String,
  onReturnToPos: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(24.dp)
      .testTag("access_denied_screen"),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DeepCharcoal),
      modifier = Modifier.padding(16.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(Color(0xFFB71C1C).copy(alpha = 0.2f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Acceso Denegado",
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Acceso Restringido",
          fontWeight = FontWeight.Black,
          fontSize = 20.sp,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "La sección \"$featureName\" solo está disponible para usuarios con rol de Administrador.",
          fontSize = 13.5.sp,
          color = Color(0xFFCCCCCC),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "El personal solo tiene autorización para realizar pedidos y consultar inventario de insumos.",
          fontSize = 11.5.sp,
          color = Color(0xFFAAAAAA),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onReturnToPos,
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.testTag("return_to_pos_button")
        ) {
          Text("Volver a Ventas / Pedidos", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}
