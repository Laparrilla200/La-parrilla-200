package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBlack
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GrillCrimson
import com.example.ui.theme.GrillFireRed
import com.example.ui.theme.MutedWhite
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.AiChatUiState
import com.example.ui.viewmodel.DishGeneratorUiState
import com.example.ui.viewmodel.ParrillaViewModel
import java.util.Locale

@Composable
fun AiAssistantScreen(
  parrillaViewModel: ParrillaViewModel,
  aiViewModel: AiAssistantViewModel = viewModel()
) {
  val config by parrillaViewModel.config.collectAsState()
  val orders by parrillaViewModel.orders.collectAsState()
  val supplies by parrillaViewModel.supplies.collectAsState()
  val menuItems by parrillaViewModel.menuItems.collectAsState()
  val restockRecommendations by parrillaViewModel.restockRecommendations.collectAsState()
  val loggedUser by parrillaViewModel.currentLoggedUser.collectAsState()

  val chatUiState by aiViewModel.chatUiState.collectAsState()
  val messages by aiViewModel.messages.collectAsState()
  val dishUiState by aiViewModel.dishUiState.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  val canEditMenu = parrillaViewModel.isCurrentUserAdmin || (loggedUser?.hasMenuApp() == true)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .testTag("ai_assistant_screen")
  ) {
    // Encabezado superior con contexto vivo
    Surface(
      color = DeepCharcoal,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = GrillCrimson.copy(alpha = 0.2f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Asistente Gemini AI",
                tint = AmberWarning,
                modifier = Modifier
                  .padding(8.dp)
                  .size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Asistente Gemini AI",
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
              )
              Text(
                text = "${supplies.size} insumos • ${orders.count { it.status != "CANCELADO" }} órdenes • ${menuItems.size} platos",
                color = MutedWhite,
                fontSize = 11.5.sp
              )
            }
          }

          if (selectedTab == 0 && messages.isNotEmpty()) {
            IconButton(
              onClick = { aiViewModel.clearChat() },
              modifier = Modifier
                .size(48.dp)
                .testTag("ai_clear_chat_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Limpiar conversación",
                tint = MutedWhite
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = SurfaceCharcoal,
          contentColor = PureWhite,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = AmberWarning
            )
          }
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Consultor del Negocio", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) },
            modifier = Modifier.testTag("ai_tab_chat")
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Crear Platos & Extras", fontWeight = FontWeight.Bold, fontSize = 12.5.sp) },
            modifier = Modifier.testTag("ai_tab_dish_generator")
          )
        }
      }
    }

    if (selectedTab == 0) {
      OperationalChatTab(
        messages = messages,
        chatUiState = chatUiState,
        criticalCount = restockRecommendations.size,
        onSendPrompt = { prompt ->
          aiViewModel.sendOperationalPrompt(
            userPrompt = prompt,
            config = config,
            orders = orders,
            supplies = supplies,
            menuItems = menuItems,
            restockItems = restockRecommendations
          )
        }
      )
    } else {
      DishGeneratorTab(
        dishUiState = dishUiState,
        canEditMenu = canEditMenu,
        exchangeRate = config.exchangeRateUsdToBs,
        onGenerate = { idea, category ->
          aiViewModel.generateStructuredDish(
            ideaPrompt = idea,
            preferredCategory = category,
            supplies = supplies,
            menuItems = menuItems
          )
        },
        onAddToMenu = { suggestion ->
          val newDish = aiViewModel.toMenuItemDoc(suggestion, supplies)
          parrillaViewModel.saveMenuItem(newDish)
          aiViewModel.resetDishState()
        }
      )
    }
  }
}

@Composable
private fun OperationalChatTab(
  messages: List<AiChatMessage>,
  chatUiState: AiChatUiState,
  criticalCount: Int,
  onSendPrompt: (String) -> Unit
) {
  var promptInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val isBusy = chatUiState is AiChatUiState.Loading || chatUiState is AiChatUiState.Streaming

  val quickPrompts = remember(criticalCount) {
    listOf(
      if (criticalCount > 0) "Plan de compras urgente ($criticalCount insumos bajos)" else "Resumen operativo de hoy",
      "Análisis de cuentas por cobrar y ventas",
      "Idea de combo rentable con el stock alto",
      "Consejos rápidos para agilizar cocina"
    )
  }

  LaunchedEffect(messages.size, chatUiState) {
    val targetIndex = messages.size
    if (targetIndex > 0) {
      listState.animateScrollToItem((targetIndex - 1).coerceAtLeast(0))
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(12.dp)
  ) {
    // Chips de consultas rápidas
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(vertical = 4.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(quickPrompts) { suggestion ->
        AssistChip(
          onClick = {
            if (!isBusy) onSendPrompt(suggestion)
          },
          enabled = !isBusy,
          label = { Text(suggestion, fontSize = 12.sp, color = PureWhite) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = AmberWarning,
              modifier = Modifier.size(16.dp)
            )
          },
          colors = AssistChipDefaults.assistChipColors(
            containerColor = SurfaceVariantDark,
            labelColor = PureWhite
          ),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("ai_quick_prompt_chip")
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Banner de error transparente si falla App Check / red / cuota
    if (chatUiState is AiChatUiState.Error) {
      ErrorBannerCard(
        message = chatUiState.message,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      )
    }

    // Lista de mensajes + stream en vivo
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 6.dp)
    ) {
      if (messages.isEmpty() && chatUiState is AiChatUiState.Idle) {
        item {
          EmptyAiWelcomeCard()
        }
      }

      items(messages, key = { it.id }) { msg ->
        ChatBubble(message = msg)
      }

      if (chatUiState is AiChatUiState.Loading) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            CircularProgressIndicator(
              color = AmberWarning,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "Consultando datos de La Parrilla 200 con Gemini...",
              color = MutedWhite,
              fontSize = 13.sp
            )
          }
        }
      } else if (chatUiState is AiChatUiState.Streaming) {
        item {
          ChatBubble(
            message = AiChatMessage(
              id = "streaming",
              isUser = false,
              text = chatUiState.partialText
            ),
            isStreaming = true
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Barra de entrada
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = promptInput,
        onValueChange = { promptInput = it },
        placeholder = { Text("Pregunta sobre ventas, compras o menú...", color = MutedWhite, fontSize = 13.sp) },
        modifier = Modifier
          .weight(1f)
          .testTag("ai_chat_input"),
        maxLines = 3,
        enabled = !isBusy,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = AmberWarning,
          unfocusedBorderColor = OutlineDark,
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedContainerColor = SurfaceCharcoal,
          unfocusedContainerColor = SurfaceCharcoal
        ),
        shape = RoundedCornerShape(14.dp)
      )

      Button(
        onClick = {
          val toSend = promptInput.trim()
          if (toSend.isNotBlank() && !isBusy) {
            promptInput = ""
            onSendPrompt(toSend)
          }
        },
        enabled = promptInput.isNotBlank() && !isBusy,
        colors = ButtonDefaults.buttonColors(
          containerColor = GrillCrimson,
          disabledContainerColor = SurfaceVariantDark
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .heightIn(min = 52.dp)
          .testTag("ai_send_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Enviar consulta a Gemini",
          tint = PureWhite
        )
      }
    }
  }
}

@Composable
private fun DishGeneratorTab(
  dishUiState: DishGeneratorUiState,
  canEditMenu: Boolean,
  exchangeRate: Double,
  onGenerate: (String, String) -> Unit,
  onAddToMenu: (com.example.ui.viewmodel.GeneratedDishSuggestion) -> Unit
) {
  val categories = listOf("Parrillas", "Hamburguesas & Pepitos", "Entradas & Guarniciones", "Bebidas")
  var selectedCategory by remember { mutableStateOf(categories.first()) }
  var ideaText by remember { mutableStateOf("") }
  val isLoading = dishUiState is DishGeneratorUiState.Loading

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text(
      text = "Diseñador de Platos, Recetas y Extras",
      color = PureWhite,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp
    )
    Text(
      text = "Gemini analiza tus insumos reales y genera un plato estructurado con ingredientes y extras listos para guardar en el menú.",
      color = MutedWhite,
      fontSize = 12.5.sp
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(categories) { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { selectedCategory = cat },
          label = { Text(cat, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GrillCrimson,
            selectedLabelColor = PureWhite,
            containerColor = SurfaceVariantDark,
            labelColor = MutedWhite
          ),
          modifier = Modifier.heightIn(min = 48.dp)
        )
      }
    }

    OutlinedTextField(
      value = ideaText,
      onValueChange = { ideaText = it },
      label = { Text("Idea o ingrediente estrella (opcional)") },
      placeholder = { Text("Ej: Hamburguesa doble con queso ahumado y salsa BBQ", color = MutedWhite) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("ai_dish_idea_input"),
      minLines = 2,
      maxLines = 4,
      enabled = !isLoading,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AmberWarning,
        unfocusedBorderColor = OutlineDark,
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedLabelColor = AmberWarning,
        unfocusedLabelColor = MutedWhite,
        focusedContainerColor = SurfaceCharcoal,
        unfocusedContainerColor = SurfaceCharcoal
      ),
      shape = RoundedCornerShape(14.dp)
    )

    Button(
      onClick = { onGenerate(ideaText, selectedCategory) },
      enabled = !isLoading,
      colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 50.dp)
        .testTag("ai_generate_dish_button")
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          color = PureWhite,
          strokeWidth = 2.5.dp,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text("Creando receta con Gemini...", fontWeight = FontWeight.Bold, color = PureWhite)
      } else {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberWarning)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Generar Plato con IA", fontWeight = FontWeight.Bold, color = PureWhite)
      }
    }

    when (dishUiState) {
      is DishGeneratorUiState.Idle -> Unit
      is DishGeneratorUiState.Loading -> Unit
      is DishGeneratorUiState.Error -> {
        ErrorBannerCard(
          message = dishUiState.message,
          modifier = Modifier.fillMaxWidth()
        )
      }
      is DishGeneratorUiState.Success -> {
        val dish = dishUiState.dish
        Card(
          colors = CardDefaults.cardColors(containerColor = SurfaceCharcoal),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("ai_generated_dish_card")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = dish.category.uppercase(Locale.getDefault()),
                  color = AmberWarning,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = dish.name,
                  color = PureWhite,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = String.format(Locale.US, "$%.2f", dish.priceUsd),
                  color = GreenSuccess,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.ExtraBold
                )
                Text(
                  text = String.format(Locale.US, "%.0f Bs", dish.priceUsd * exchangeRate),
                  color = MutedWhite,
                  fontSize = 11.5.sp
                )
              }
            }

            if (dish.description.isNotBlank()) {
              Text(
                text = dish.description,
                color = Color(0xFFE0E0E4),
                fontSize = 13.sp
              )
            }

            HorizontalDivider(color = OutlineDark)

            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Inventory, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Receta / Insumos (${dish.ingredients.size}):",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
            dish.ingredients.forEach { ing ->
              Text(
                text = "• ${ing.supplyName}: ${ing.quantityRequired} ${ing.unit}",
                color = MutedWhite,
                fontSize = 12.5.sp
              )
            }

            if (dish.extras.isNotEmpty()) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Extras Sugeridos (${dish.extras.size}):",
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
              dish.extras.forEach { extra ->
                Text(
                  text = "• ${extra.name} (+$${String.format(Locale.US, "%.2f", extra.priceUsd)})",
                  color = MutedWhite,
                  fontSize = 12.5.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (canEditMenu) {
              Button(
                onClick = { onAddToMenu(dish) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("ai_save_dish_to_menu_button")
              ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Plato en el Menú", fontWeight = FontWeight.Bold, color = PureWhite)
              }
            } else {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MutedWhite, modifier = Modifier.size(16.dp))
                Text(
                  text = "Solo usuarios con permiso de Menú o Admin pueden guardar nuevos platos.",
                  color = MutedWhite,
                  fontSize = 11.5.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyAiWelcomeCard() {
  Card(
    colors = CardDefaults.cardColors(containerColor = SurfaceCharcoal),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = "¿En qué te ayudo hoy en La Parrilla 200?",
        color = PureWhite,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
      )
      Text(
        text = "Toca una consulta rápida arriba o escribe tu pregunta. Analizo tus ventas, cuentas pendientes, inventario crítico y catálogo en tiempo real con Gemini.",
        color = MutedWhite,
        fontSize = 13.sp,
        lineHeight = 18.sp
      )
    }
  }
}

@Composable
private fun ChatBubble(
  message: AiChatMessage,
  isStreaming: Boolean = false
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      color = if (message.isUser) GrillCrimson else SurfaceCharcoal,
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isUser) 16.dp else 4.dp,
        bottomEnd = if (message.isUser) 4.dp else 16.dp
      ),
      modifier = Modifier
        .widthIn(max = 560.dp)
        .padding(horizontal = 4.dp)
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
          text = if (message.isUser) "Tú" else if (isStreaming) "Gemini AI (escribiendo...)" else "Gemini AI",
          color = if (message.isUser) PureWhite.copy(alpha = 0.85f) else AmberWarning,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = message.text,
          color = PureWhite,
          style = MaterialTheme.typography.bodyMedium,
          fontSize = 13.5.sp,
          lineHeight = 19.sp
        )
      }
    }
  }
}

@Composable
private fun ErrorBannerCard(
  message: String,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = GrillFireRed.copy(alpha = 0.18f)),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .border(1.dp, GrillFireRed, RoundedCornerShape(12.dp))
      .testTag("ai_error_banner")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = "Error de Gemini AI",
        tint = GrillFireRed,
        modifier = Modifier.size(22.dp)
      )
      Text(
        text = message,
        color = PureWhite,
        fontSize = 12.5.sp,
        modifier = Modifier.weight(1f)
      )
    }
  }
}
