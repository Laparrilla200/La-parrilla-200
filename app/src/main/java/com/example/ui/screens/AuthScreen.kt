package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.viewmodel.ParrillaViewModel

@Composable
fun AuthScreen(
  viewModel: ParrillaViewModel,
  onSignedIn: () -> Unit
) {
  val systemUsers by viewModel.systemUsers.collectAsState()
  var usernameInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(horizontal = 20.dp, vertical = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      // Logo de La Parrilla 200
      Box(
        modifier = Modifier
          .size(110.dp)
          .clip(CircleShape)
          .background(Color.Black),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_parrilla_logo),
          contentDescription = "Logo Parrilla 200",
          modifier = Modifier.size(105.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "LA PARRILLA 200",
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        letterSpacing = 1.sp
      )

      Text(
        text = "Control de Acceso • Punto de Venta e Inventario",
        fontSize = 13.sp,
        color = Color(0xFFDDDDDD),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Tarjeta de Inicio de Sesión
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Acceso de Personal",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Text(
            text = "Ingresa con tu usuario y clave asignada para iniciar turno.",
            fontSize = 12.sp,
            color = Color(0xFFAAAAAA)
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Campo Usuario
          OutlinedTextField(
            value = usernameInput,
            onValueChange = {
              usernameInput = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }
              errorMessage = null
            },
            label = { Text("Usuario (Ej. admin, cajero)") },
            leadingIcon = {
              Icon(Icons.Default.Person, contentDescription = null, tint = GrillFireRed)
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("username_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrillCrimson,
              unfocusedBorderColor = Color(0xFF444444),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.None,
              keyboardType = KeyboardType.Ascii,
              imeAction = ImeAction.Next
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Campo Contraseña
          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              errorMessage = null
            },
            label = { Text("Contraseña / Clave") },
            leadingIcon = {
              Icon(Icons.Default.Lock, contentDescription = null, tint = GrillFireRed)
            },
            trailingIcon = {
              IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                  imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                  contentDescription = if (isPasswordVisible) "Ocultar" else "Mostrar",
                  tint = Color(0xFFAAAAAA)
                )
              }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("password_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrillCrimson,
              unfocusedBorderColor = Color(0xFF444444),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                if (!isLoading && usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                  isLoading = true
                  viewModel.login(usernameInput, passwordInput) { success, error ->
                    isLoading = false
                    if (success) onSignedIn() else errorMessage = error
                  }
                }
              }
            )
          )

          // Mensaje de Error
          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              color = Color(0xFFB71C1C).copy(alpha = 0.35f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = errorMessage ?: "",
                color = Color(0xFFFF8A80),
                fontSize = 12.sp,
                modifier = Modifier.padding(10.dp),
                textAlign = TextAlign.Center
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Botón Ingresar
          Button(
            onClick = {
              if (isLoading) return@Button
              isLoading = true
              errorMessage = null

              viewModel.login(usernameInput, passwordInput) { success, error ->
                isLoading = false
                if (success) {
                  onSignedIn()
                } else {
                  errorMessage = error
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("login_button"),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
            enabled = !isLoading && usernameInput.isNotBlank() && passwordInput.isNotBlank()
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Ingresar al Sistema",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }

          // Accesos rápidos de selección de usuario
          if (systemUsers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Selección Rápida de Usuario:",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFAAAAAA)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              for (u in systemUsers.take(3)) {
                val isSelected = usernameInput.equals(u.username, ignoreCase = true)
                Surface(
                  color = if (isSelected) GrillCrimson else SurfaceCharcoal,
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .clickable {
                      usernameInput = u.username
                      errorMessage = null
                    }
                    .testTag("chip_user_${u.username}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (u.role.equals("ADMIN", ignoreCase = true)) "👑 ${u.username}" else "👤 ${u.username}",
                      fontSize = 11.5.sp,
                      color = Color.White,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Tarjeta Informativa de Clave de Administrador
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCharcoal)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = GrillFireRed,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Acceso Inicial por Defecto:",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = Color.White
            )
            Text(
              text = "Usuario: admin  |  Clave: admin200",
              fontWeight = FontWeight.Black,
              fontSize = 13.sp,
              color = Color(0xFFFFB300)
            )
            Text(
              text = "Puedes cambiar el usuario, la clave y agregar más personal en Ajustes.",
              fontSize = 11.sp,
              color = Color(0xFFAAAAAA)
            )
          }
        }
      }
    }
  }
}
