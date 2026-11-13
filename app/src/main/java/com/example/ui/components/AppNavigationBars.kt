package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserRole
import com.example.data.model.SystemUserDoc
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.AppScreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParrillaTopAppBar(
  businessName: String,
  exchangeRate: Double,
  effectiveRole: UserRole,
  isActualAdmin: Boolean,
  criticalStockCount: Int,
  isAiActive: Boolean = false,
  onToggleRoleSimulation: () -> Unit,
  onNavigateToInventory: () -> Unit,
  onNavigateToAi: () -> Unit = {},
  onSignOut: () -> Unit
) {
  TopAppBar(
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(GrillCrimson),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_parrilla_logo),
            contentDescription = "Logo La Parrilla 200",
            modifier = Modifier.size(28.dp)
          )
        }
        Column {
          Text(
            text = businessName.ifBlank { "LA PARRILLA 200" },
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = Color.White
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = String.format(Locale.US, "Tasa: %.2f Bs/$", exchangeRate),
              fontSize = 11.sp,
              color = Color(0xFFFFB300),
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Rol activo con indicador
            Surface(
              color = if (effectiveRole == UserRole.ADMIN) GrillCrimson else Color(0xFF2E7D32),
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier.testTag("role_badge_top")
            ) {
              Text(
                text = if (effectiveRole == UserRole.ADMIN) "👑 ADMIN" else "🍳 PERSONAL",
                fontSize = 9.5.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }
          }
        }
      }
    },
    actions = {
      // Alerta de Stock Crítico (si hay insumos agotándose)
      if (criticalStockCount > 0) {
        Surface(
          color = AmberWarning.copy(alpha = 0.2f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .clickable(onClick = onNavigateToInventory)
            .testTag("critical_stock_chip")
            .padding(end = 4.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Alerta Stock",
              tint = AmberWarning,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$criticalStockCount bajos",
              color = AmberWarning,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Botón Asistente Gemini AI
      IconButton(
        onClick = onNavigateToAi,
        modifier = Modifier
          .size(48.dp)
          .testTag("ai_assistant_button")
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = "Asistente Gemini AI",
          tint = if (isAiActive) GrillFireRed else AmberWarning
        )
      }

      // Si es Admin, permitir conmutar vista para probar vista de Cocina/Personal
      if (isActualAdmin) {
        IconButton(
          onClick = onToggleRoleSimulation,
          modifier = Modifier.testTag("toggle_role_button")
        ) {
          Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "Cambiar vista Admin/Personal",
            tint = Color.White
          )
        }
      }

      // Botón Cerrar Sesión
      IconButton(
        onClick = onSignOut,
        modifier = Modifier.testTag("sign_out_button")
      ) {
        Icon(
          imageVector = Icons.Default.ExitToApp,
          contentDescription = "Cerrar sesión",
          tint = Color(0xFFAAAAAA)
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = CharcoalBlack,
      titleContentColor = Color.White
    )
  )
}

@Composable
fun ParrillaBottomNavigationBar(
  currentScreen: AppScreen,
  effectiveRole: UserRole,
  pendingKitchenCount: Int,
  criticalStockCount: Int,
  loggedUser: SystemUserDoc? = null,
  onScreenSelect: (AppScreen) -> Unit
) {
  val isAdminView = effectiveRole == UserRole.ADMIN

  NavigationBar(
    containerColor = DeepCharcoal,
    modifier = Modifier.testTag("bottom_navigation_bar")
  ) {
    // 1. Pedidos / Ventas (Si es Admin o tiene permiso en la App)
    if (isAdminView || (loggedUser?.hasPosApp() != false)) {
      NavigationBarItem(
        selected = currentScreen == AppScreen.POS,
        onClick = { onScreenSelect(AppScreen.POS) },
        icon = {
          Icon(Icons.Default.PointOfSale, contentDescription = "Pedidos")
        },
        label = { Text("Pedidos", fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color.White,
          selectedTextColor = GrillFireRed,
          indicatorColor = GrillCrimson,
          unselectedIconColor = Color(0xFF999999),
          unselectedTextColor = Color(0xFF999999)
        ),
        modifier = Modifier.testTag("nav_pos")
      )
    }

    // 2. Cocina / KDS (Si es Admin o tiene permiso en la App)
    if (isAdminView || (loggedUser?.hasKitchenApp() != false)) {
      NavigationBarItem(
        selected = currentScreen == AppScreen.KITCHEN,
        onClick = { onScreenSelect(AppScreen.KITCHEN) },
        icon = {
          if (pendingKitchenCount > 0) {
            BadgedBox(
              badge = {
                Badge(containerColor = GrillCrimson) {
                  Text("$pendingKitchenCount", color = Color.White, fontSize = 10.sp)
                }
              }
            ) {
              Icon(Icons.Default.Restaurant, contentDescription = "Cocina")
            }
          } else {
            Icon(Icons.Default.Restaurant, contentDescription = "Cocina")
          }
        },
        label = { Text("Cocina", fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color.White,
          selectedTextColor = GrillFireRed,
          indicatorColor = GrillCrimson,
          unselectedIconColor = Color(0xFF999999),
          unselectedTextColor = Color(0xFF999999)
        ),
        modifier = Modifier.testTag("nav_kitchen")
      )
    }

    // 3. Inventario (Si es Admin o tiene permiso en la App)
    if (isAdminView || (loggedUser?.hasInventoryApp() == true)) {
      NavigationBarItem(
        selected = currentScreen == AppScreen.INVENTORY,
        onClick = { onScreenSelect(AppScreen.INVENTORY) },
        icon = {
          if (criticalStockCount > 0) {
            BadgedBox(
              badge = {
                Badge(containerColor = AmberWarning) {
                  Text("$criticalStockCount", color = CharcoalBlack, fontSize = 10.sp)
                }
              }
            ) {
              Icon(Icons.Default.Inventory, contentDescription = "Inventario")
            }
          } else {
            Icon(Icons.Default.Inventory, contentDescription = "Inventario")
          }
        },
        label = { Text("Inventario", fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color.White,
          selectedTextColor = GrillFireRed,
          indicatorColor = GrillCrimson,
          unselectedIconColor = Color(0xFF999999),
          unselectedTextColor = Color(0xFF999999)
        ),
        modifier = Modifier.testTag("nav_inventory")
      )
    }

    // 4. Cierres & Reportes (Si es Admin o tiene permiso otorgado)
    if (isAdminView || (loggedUser?.hasClosuresApp() == true)) {
      NavigationBarItem(
        selected = currentScreen == AppScreen.CLOSURES,
        onClick = { onScreenSelect(AppScreen.CLOSURES) },
        icon = {
          Icon(Icons.Default.Assessment, contentDescription = "Cierres")
        },
        label = { Text("Reportes", fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color.White,
          selectedTextColor = GrillFireRed,
          indicatorColor = GrillCrimson,
          unselectedIconColor = Color(0xFF999999),
          unselectedTextColor = Color(0xFF999999)
        ),
        modifier = Modifier.testTag("nav_closures")
      )
    }

    // 5. Menú & Recetas (Si es Admin o tiene permiso otorgado)
    if (isAdminView || (loggedUser?.hasMenuApp() == true)) {
      NavigationBarItem(
        selected = currentScreen == AppScreen.MENU_ADMIN,
        onClick = { onScreenSelect(AppScreen.MENU_ADMIN) },
        icon = {
          Icon(Icons.Default.RestaurantMenu, contentDescription = "Menú")
        },
        label = { Text("Menú", fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color.White,
          selectedTextColor = GrillFireRed,
          indicatorColor = GrillCrimson,
          unselectedIconColor = Color(0xFF999999),
          unselectedTextColor = Color(0xFF999999)
        ),
        modifier = Modifier.testTag("nav_menu")
      )
    }

    // 6. Ajustes del Sistema & Personal (Si es Admin o tiene permiso)
    if (isAdminView || (loggedUser?.hasSettingsApp() == true)) {
      NavigationBarItem(
        selected = currentScreen == AppScreen.SETTINGS,
        onClick = { onScreenSelect(AppScreen.SETTINGS) },
        icon = {
          Icon(Icons.Default.Settings, contentDescription = "Ajustes")
        },
        label = { Text("Ajustes", fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color.White,
          selectedTextColor = GrillFireRed,
          indicatorColor = GrillCrimson,
          unselectedIconColor = Color(0xFF999999),
          unselectedTextColor = Color(0xFF999999)
        ),
        modifier = Modifier.testTag("nav_settings")
      )
    }

    // 7. Pestaña Mi Perfil (para Personal)
    if (!isAdminView) {
      NavigationBarItem(
        selected = currentScreen == AppScreen.PROFILE,
        onClick = { onScreenSelect(AppScreen.PROFILE) },
        icon = {
          Icon(Icons.Default.AccountCircle, contentDescription = "Mi Perfil")
        },
        label = { Text("Mi Perfil", fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color.White,
          selectedTextColor = GrillFireRed,
          indicatorColor = Color(0xFF2E7D32),
          unselectedIconColor = Color(0xFF999999),
          unselectedTextColor = Color(0xFF999999)
        ),
        modifier = Modifier.testTag("nav_profile")
      )
    }
  }
}
