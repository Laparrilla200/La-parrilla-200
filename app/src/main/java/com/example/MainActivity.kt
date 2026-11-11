package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.data.repository.ParrillaRepository
import com.example.ui.components.ParrillaBottomNavigationBar
import com.example.ui.components.ParrillaTopAppBar
import com.example.ui.components.ThermalTicketDialog
import com.example.ui.screens.AccessDeniedScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClosuresScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.KitchenScreen
import com.example.ui.screens.MenuAdminScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StaffProfileScreen
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ParrillaViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val context = LocalContext.current
        val databaseId = remember { context.getString(R.string.firestore_database_id) }
        val firestore = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
        val repository = remember(firestore) { ParrillaRepository(firestore) }
        val viewModel: ParrillaViewModel = viewModel(
          initializer = { ParrillaViewModel(repository, context.applicationContext) }
        )

        // Solicitar permiso de notificaciones para alertas de inventario (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val notifLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { /* Permiso procesado */ }
          )
          LaunchedEffect(Unit) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
          }
        }

        val loggedUser by viewModel.currentLoggedUser.collectAsState()

        if (loggedUser == null) {
          AuthScreen(
            viewModel = viewModel,
            onSignedIn = {
              viewModel.setScreen(AppScreen.POS)
            }
          )
        } else {
          MainContent(
            viewModel = viewModel,
            onSignOut = { viewModel.logout() }
          )
        }
      }
    }
  }
}

@Composable
fun MainContent(viewModel: ParrillaViewModel, onSignOut: () -> Unit) {

  val currentScreen by viewModel.currentScreen.collectAsState()
  val config by viewModel.config.collectAsState()
  val criticalSupplies by viewModel.criticalSupplies.collectAsState()
  val orders by viewModel.orders.collectAsState()
  val userProfile by viewModel.userProfile.collectAsState()
  val userMessage by viewModel.userMessage.collectAsState()

  val showTicketDialog by viewModel.showTicketDialog.collectAsState()
  val ticketTitle by viewModel.currentTicketTitle.collectAsState()
  val ticketText by viewModel.currentTicketText.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  val pendingKitchenCount = orders.count { it.status == "EN_COCINA" }
  val effectiveRole = viewModel.currentEffectiveRole
  val isActualAdmin = userProfile?.role.equals("ADMIN", ignoreCase = true)

  // Mensajes de Toast / Snackbar
  LaunchedEffect(userMessage) {
    userMessage?.let {
      coroutineScope.launch {
        snackbarHostState.showSnackbar(it)
        viewModel.clearUserMessage()
      }
    }
  }

  // BackHandler para navegación limpia en Compose
  BackHandler(enabled = currentScreen != AppScreen.POS) {
    viewModel.setScreen(AppScreen.POS)
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = CharcoalBlack,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      ParrillaTopAppBar(
        businessName = config.businessName,
        exchangeRate = config.exchangeRateUsdToBs,
        effectiveRole = effectiveRole,
        isActualAdmin = isActualAdmin,
        criticalStockCount = criticalSupplies.size,
        onToggleRoleSimulation = {
          if (effectiveRole == UserRole.ADMIN) {
            viewModel.toggleRoleViewMode(UserRole.STAFF)
          } else {
            viewModel.toggleRoleViewMode(UserRole.ADMIN)
          }
        },
        onNavigateToInventory = { viewModel.setScreen(AppScreen.INVENTORY) },
        onSignOut = onSignOut
      )
    },
    bottomBar = {
      val loggedUser by viewModel.currentLoggedUser.collectAsState()
      ParrillaBottomNavigationBar(
        currentScreen = currentScreen,
        effectiveRole = effectiveRole,
        pendingKitchenCount = pendingKitchenCount,
        criticalStockCount = criticalSupplies.size,
        loggedUser = loggedUser,
        onScreenSelect = { viewModel.setScreen(it) }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(CharcoalBlack)
    ) {
      when (currentScreen) {
        AppScreen.POS -> {
          if (viewModel.canUserAccessScreen(AppScreen.POS)) {
            PosScreen(viewModel = viewModel)
          } else {
            AccessDeniedScreen(
              featureName = "Toma de Pedidos (POS)",
              onReturnToPos = { viewModel.setScreen(AppScreen.KITCHEN) }
            )
          }
        }
        AppScreen.KITCHEN -> {
          if (viewModel.canUserAccessScreen(AppScreen.KITCHEN)) {
            KitchenScreen(viewModel = viewModel)
          } else {
            AccessDeniedScreen(
              featureName = "Monitor de Cocina (KDS)",
              onReturnToPos = { viewModel.setScreen(AppScreen.POS) }
            )
          }
        }
        AppScreen.INVENTORY -> {
          if (viewModel.canUserAccessScreen(AppScreen.INVENTORY)) {
            InventoryScreen(viewModel = viewModel)
          } else {
            AccessDeniedScreen(
              featureName = "Control de Inventario e Insumos",
              onReturnToPos = { viewModel.setScreen(AppScreen.POS) }
            )
          }
        }
        AppScreen.PROFILE -> StaffProfileScreen(viewModel = viewModel, onSignOut = onSignOut)
        AppScreen.CLOSURES -> {
          if (viewModel.canUserAccessScreen(AppScreen.CLOSURES)) {
            ClosuresScreen(viewModel = viewModel)
          } else {
            AccessDeniedScreen(
              featureName = "Cierres de Caja y Reportes Financieros",
              onReturnToPos = { viewModel.setScreen(AppScreen.POS) }
            )
          }
        }
        AppScreen.MENU_ADMIN -> {
          if (viewModel.canUserAccessScreen(AppScreen.MENU_ADMIN)) {
            MenuAdminScreen(viewModel = viewModel)
          } else {
            AccessDeniedScreen(
              featureName = "Menú y Recetas",
              onReturnToPos = { viewModel.setScreen(AppScreen.POS) }
            )
          }
        }
        AppScreen.SETTINGS -> {
          if (viewModel.canUserAccessScreen(AppScreen.SETTINGS)) {
            SettingsScreen(viewModel = viewModel)
          } else {
            AccessDeniedScreen(
              featureName = "Configuraciones del Sistema y Roles",
              onReturnToPos = { viewModel.setScreen(AppScreen.POS) }
            )
          }
        }
      }
    }
  }

  // Diálogo emergente del Ticket Térmico de 48mm
  if (showTicketDialog) {
    ThermalTicketDialog(
      title = ticketTitle,
      ticketText = ticketText,
      onDismiss = { viewModel.closeTicketDialog() }
    )
  }
}
