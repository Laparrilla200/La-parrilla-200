package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BusinessConfigDoc
import com.example.data.model.IngredientRequirement
import com.example.data.model.MenuItemDoc
import com.example.data.model.MenuItemExtra
import com.example.data.model.OrderDoc
import com.example.data.model.SupplyDoc
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

private const val TAG = "AiAssistantViewModel"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

data class AiChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String
)

data class SuggestedIngredient(
  val supplyName: String,
  val quantityRequired: Double,
  val unit: String
)

data class GeneratedDishSuggestion(
  val name: String,
  val category: String,
  val description: String,
  val priceUsd: Double,
  val ingredients: List<SuggestedIngredient>,
  val extras: List<MenuItemExtra>
)

sealed interface AiChatUiState {
  data object Idle : AiChatUiState
  data object Loading : AiChatUiState
  data class Streaming(val partialText: String) : AiChatUiState
  data class Success(val text: String) : AiChatUiState
  data class Error(val message: String) : AiChatUiState
}

sealed interface DishGeneratorUiState {
  data object Idle : DishGeneratorUiState
  data object Loading : DishGeneratorUiState
  data class Success(val dish: GeneratedDishSuggestion) : DishGeneratorUiState
  data class Error(val message: String) : DishGeneratorUiState
}

class AiAssistantViewModel : ViewModel() {

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val ingredientAdapter = moshi.adapter<List<IngredientRequirement>>(
    Types.newParameterizedType(List::class.java, IngredientRequirement::class.java)
  )
  private val extraAdapter = moshi.adapter<List<MenuItemExtra>>(
    Types.newParameterizedType(List::class.java, MenuItemExtra::class.java)
  )

  private val chatModel by lazy {
    Firebase.ai.generativeModel(modelName = MODEL_NAME)
  }

  private val dishSchema = Schema.obj(
    properties = mapOf(
      "name" to Schema.string(description = "Nombre comercial atractivo del plato para La Parrilla 200"),
      "category" to Schema.enumeration(
        values = listOf(
          "Parrillas",
          "Hamburguesas & Pepitos",
          "Entradas & Guarniciones",
          "Bebidas"
        ),
        description = "Categoría exacta del menú"
      ),
      "description" to Schema.string(description = "Descripción breve y apetitosa en español"),
      "priceUsd" to Schema.double(description = "Precio sugerido en dólares USD realista para restaurante de carnes"),
      "ingredients" to Schema.array(
        items = Schema.obj(
          properties = mapOf(
            "supplyName" to Schema.string(description = "Nombre exacto del insumo"),
            "quantityRequired" to Schema.double(description = "Cantidad por plato"),
            "unit" to Schema.string(description = "Unidad: kg, g, ud, porc, lt")
          )
        ),
        description = "Insumos necesarios para preparar 1 porción"
      ),
      "extras" to Schema.array(
        items = Schema.obj(
          properties = mapOf(
            "name" to Schema.string(description = "Nombre del extra o adicional"),
            "priceUsd" to Schema.double(description = "Precio del extra en USD")
          )
        ),
        description = "Extras sugeridos opcionales para el cliente"
      )
    )
  )

  private val dishGeneratorModel by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = dishSchema
      }
    )
  }

  private val _chatUiState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
  val chatUiState: StateFlow<AiChatUiState> = _chatUiState.asStateFlow()

  private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
  val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

  private val _dishUiState = MutableStateFlow<DishGeneratorUiState>(DishGeneratorUiState.Idle)
  val dishUiState: StateFlow<DishGeneratorUiState> = _dishUiState.asStateFlow()

  fun sendOperationalPrompt(
    userPrompt: String,
    config: BusinessConfigDoc,
    orders: List<OrderDoc>,
    supplies: List<SupplyDoc>,
    menuItems: List<MenuItemDoc>,
    restockItems: List<RestockItem>
  ) {
    val cleanPrompt = userPrompt.trim()
    if (cleanPrompt.isBlank()) return

    _messages.value = _messages.value + AiChatMessage(isUser = true, text = cleanPrompt)

    viewModelScope.launch {
      _chatUiState.value = AiChatUiState.Loading
      val accumulated = StringBuilder()
      val promptWithContext = buildOperationalContextPrompt(
        userPrompt = cleanPrompt,
        config = config,
        orders = orders,
        supplies = supplies,
        menuItems = menuItems,
        restockItems = restockItems
      )
      try {
        withTimeout(AI_TIMEOUT_MS) {
          chatModel.generateContentStream(promptWithContext).collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) {
              accumulated.append(piece)
              _chatUiState.value = AiChatUiState.Streaming(accumulated.toString())
            }
          }
        }
        val finalText = accumulated.toString().trim()
        if (finalText.isNotBlank()) {
          _messages.value = _messages.value + AiChatMessage(isUser = false, text = finalText)
          _chatUiState.value = AiChatUiState.Success(finalText)
        } else {
          _chatUiState.value = AiChatUiState.Error("Gemini devolvió una respuesta vacía. Intenta nuevamente.")
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini chat generation timed out", e)
        _chatUiState.value = AiChatUiState.Error("Tiempo de espera agotado (60s). Verifica tu conexión e inténtalo de nuevo.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI chat generation failed", e)
        _chatUiState.value = AiChatUiState.Error(e.localizedMessage ?: "Error al consultar Gemini AI.")
      }
    }
  }

  fun generateStructuredDish(
    ideaPrompt: String,
    preferredCategory: String,
    supplies: List<SupplyDoc>,
    menuItems: List<MenuItemDoc>
  ) {
    val promptIdea = ideaPrompt.trim().ifBlank {
      "Crea una especialidad nueva y rentable en la categoría $preferredCategory usando insumos disponibles."
    }

    val suppliesContext = supplies.joinToString("\n") { sup ->
      "- ${sup.name} (Stock: ${String.format(Locale.US, "%.1f", sup.currentStock)} ${sup.unit}, Costo: $${String.format(Locale.US, "%.2f", sup.costUsd)}/${sup.unit})"
    }.ifBlank { "- Carne de res, Pechuga de pollo, Queso llanero, Chorizo, Papas" }

    val existingDishes = menuItems.take(12).joinToString(", ") { "${it.name} ($${it.priceUsd})" }

    val prompt = """
      Eres el chef y gerente gastronómico de "La Parrilla 200" (parrillas, hamburguesas, pepitos y bebidas en Venezuela).
      Diseña UN plato nuevo original que NO repita exactamente los ya existentes ($existingDishes).
      Categoría preferida: $preferredCategory.
      Solicitud del usuario: "$promptIdea"
      Insumos reales en inventario (prioriza usar nombres exactos de esta lista cuando apliquen):
      $suppliesContext
    """.trimIndent()

    viewModelScope.launch {
      _dishUiState.value = DishGeneratorUiState.Loading
      try {
        val response = withTimeout(AI_TIMEOUT_MS) {
          dishGeneratorModel.generateContent(prompt)
        }
        val jsonText = response.text
        if (jsonText.isNullOrBlank()) {
          _dishUiState.value = DishGeneratorUiState.Error("El modelo devolvió una respuesta vacía.")
          return@launch
        }

        val json = JSONObject(jsonText)
        val ingredientsArr = json.optJSONArray("ingredients")
        val ingredients = if (ingredientsArr != null) {
          (0 until ingredientsArr.length()).map { idx ->
            val obj = ingredientsArr.getJSONObject(idx)
            SuggestedIngredient(
              supplyName = obj.optString("supplyName", "Insumo"),
              quantityRequired = obj.optDouble("quantityRequired", 0.2),
              unit = obj.optString("unit", "kg")
            )
          }
        } else emptyList()

        val extrasArr = json.optJSONArray("extras")
        val extras = if (extrasArr != null) {
          (0 until extrasArr.length()).map { idx ->
            val obj = extrasArr.getJSONObject(idx)
            MenuItemExtra(
              name = obj.optString("name", "Extra"),
              priceUsd = obj.optDouble("priceUsd", 1.5)
            )
          }
        } else emptyList()

        val dish = GeneratedDishSuggestion(
          name = json.getString("name"),
          category = json.optString("category", preferredCategory),
          description = json.optString("description", ""),
          priceUsd = json.optDouble("priceUsd", 8.0),
          ingredients = ingredients,
          extras = extras
        )
        _dishUiState.value = DishGeneratorUiState.Success(dish)
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Structured dish generation timed out", e)
        _dishUiState.value = DishGeneratorUiState.Error("Tiempo de espera agotado (60s) al generar receta.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Structured dish generation failed", e)
        _dishUiState.value = DishGeneratorUiState.Error(e.localizedMessage ?: "No se pudo generar el plato con Gemini.")
      }
    }
  }

  fun toMenuItemDoc(
    suggestion: GeneratedDishSuggestion,
    availableSupplies: List<SupplyDoc>
  ): MenuItemDoc {
    val mappedIngredients = suggestion.ingredients.mapNotNull { ing ->
      val matchedSupply = availableSupplies.firstOrNull { sup ->
        sup.name.equals(ing.supplyName, ignoreCase = true) ||
          sup.name.contains(ing.supplyName, ignoreCase = true) ||
          ing.supplyName.contains(sup.name, ignoreCase = true)
      }
      if (matchedSupply != null) {
        IngredientRequirement(
          supplyId = matchedSupply.id,
          supplyName = matchedSupply.name,
          quantityRequired = ing.quantityRequired.coerceAtLeast(0.05),
          unit = matchedSupply.unit,
          isRemovable = true
        )
      } else null
    }
    return MenuItemDoc(
      name = suggestion.name,
      description = suggestion.description,
      category = suggestion.category,
      priceUsd = suggestion.priceUsd,
      isAvailable = true,
      ingredientsJson = ingredientAdapter.toJson(mappedIngredients),
      extrasJson = extraAdapter.toJson(suggestion.extras)
    )
  }

  fun clearChat() {
    _messages.value = emptyList()
    _chatUiState.value = AiChatUiState.Idle
  }

  fun resetDishState() {
    _dishUiState.value = DishGeneratorUiState.Idle
  }

  private fun buildOperationalContextPrompt(
    userPrompt: String,
    config: BusinessConfigDoc,
    orders: List<OrderDoc>,
    supplies: List<SupplyDoc>,
    menuItems: List<MenuItemDoc>,
    restockItems: List<RestockItem>
  ): String {
    val activeOrders = orders.filter { it.status != "CANCELADO" }
    val paidOrders = activeOrders.filter { it.isPaid }
    val unpaidOrders = activeOrders.filter { !it.isPaid }
    val totalPaidUsd = paidOrders.sumOf { it.totalUsd }
    val totalUnpaidUsd = unpaidOrders.sumOf { it.totalUsd }
    val inKitchenCount = activeOrders.count { it.status == "EN_COCINA" }
    val readyCount = activeOrders.count { it.status == "LISTO" }

    val lowStockLines = if (restockItems.isEmpty()) {
      "Todos los insumos están por encima del mínimo."
    } else {
      restockItems.joinToString("\n") { r ->
        "- ${r.supply.name}: queda ${String.format(Locale.US, "%.1f", r.supply.currentStock)} ${r.supply.unit} " +
          "(mínimo ${r.supply.minStockThreshold}), comprar ~${String.format(Locale.US, "%.1f", r.toBuyAmount)} ${r.supply.unit} " +
          "(~$${String.format(Locale.US, "%.2f", r.estimatedCostUsd)} / ${String.format(Locale.US, "%.0f", r.estimatedCostBs)} Bs)"
      }
    }

    val topStockLines = supplies
      .sortedByDescending { it.currentStock }
      .take(6)
      .joinToString(", ") { "${it.name} (${String.format(Locale.US, "%.1f", it.currentStock)} ${it.unit})" }

    val menuSummary = menuItems.take(15).joinToString(", ") { "${it.name} ($${String.format(Locale.US, "%.2f", it.priceUsd)})" }

    val historySnippet = _messages.value.takeLast(6).dropLast(1).joinToString("\n") { msg ->
      val speaker = if (msg.isUser) "Usuario" else "Asistente"
      "$speaker: ${msg.text.take(400)}"
    }

    return """
      Eres el Asistente Inteligente de Operaciones y Cocina de "${config.businessName}".
      Responde siempre en español claro, directo, práctico y con viñetas cortas cuando sea útil. Usa datos reales del restaurante proporcionados abajo y nunca inventes cifras que contradigan los datos.

      DATOS EN TIEMPO REAL DEL RESTAURANTE:
      - Tasa de cambio actual: ${String.format(Locale.US, "%.2f", config.exchangeRateUsdToBs)} Bs/$
      - Órdenes activas (no canceladas): ${activeOrders.size} (En cocina: $inKitchenCount, Listas: $readyCount)
      - Cuentas cobradas: ${paidOrders.size} ($${String.format(Locale.US, "%.2f", totalPaidUsd)} / ${String.format(Locale.US, "%.0f", totalPaidUsd * config.exchangeRateUsdToBs)} Bs)
      - Cuentas por cobrar: ${unpaidOrders.size} ($${String.format(Locale.US, "%.2f", totalUnpaidUsd)} / ${String.format(Locale.US, "%.0f", totalUnpaidUsd * config.exchangeRateUsdToBs)} Bs)
      - Inventario con stock crítico o bajo (${restockItems.size} insumos):
      $lowStockLines
      - Insumos con mayor disponibilidad: $topStockLines
      - Muestra del menú (${menuItems.size} platos totales): $menuSummary
      ${if (historySnippet.isNotBlank()) "\nCONVERSACIÓN RECIENTE:\n$historySnippet\n" else ""}
      CONSULTA ACTUAL DEL USUARIO:
      $userPrompt
    """.trimIndent()
  }
}
