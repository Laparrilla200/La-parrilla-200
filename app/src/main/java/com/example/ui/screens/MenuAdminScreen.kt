package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.IngredientRequirement
import com.example.data.model.MenuItemDoc
import com.example.data.model.SupplyDoc
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

@Composable
fun MenuAdminScreen(viewModel: ParrillaViewModel) {
  val menuItems by viewModel.menuItems.collectAsState()
  val supplies by viewModel.supplies.collectAsState()
  val config by viewModel.config.collectAsState()
  val role = viewModel.currentEffectiveRole

  var selectedCategory by remember { mutableStateOf("Todas") }
  var dishToEdit by remember { mutableStateOf<MenuItemDoc?>(null) }
  var showNewDishDialog by remember { mutableStateOf(false) }

  val categories = listOf("Todas", "Parrillas", "Hamburguesas & Pepitos", "Entradas & Guarniciones", "Bebidas")
  val filteredDishes = if (selectedCategory == "Todas") {
    menuItems
  } else {
    menuItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(CharcoalBlack)
      .padding(12.dp)
      .testTag("menu_admin_screen")
  ) {
    // Encabezado
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = GrillFireRed, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Menú y Recetas",
          fontWeight = FontWeight.ExtraBold,
          fontSize = 19.sp,
          color = Color.White
        )
      }

      if (role == UserRole.ADMIN) {
        Button(
          onClick = { showNewDishDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.testTag("add_dish_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Nuevo Plato", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Categorías
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(categories) { cat ->
        val isSel = selectedCategory == cat
        FilterChip(
          selected = isSel,
          onClick = { selectedCategory = cat },
          label = { Text(cat, fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = GrillCrimson,
            selectedLabelColor = Color.White,
            containerColor = SurfaceVariantDark,
            labelColor = Color(0xFFCCCCCC)
          )
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Lista de Platos y Recetas
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(filteredDishes) { dish ->
        val ingredients = remember(dish.ingredientsJson) { viewModel.parseIngredients(dish.ingredientsJson) }
        DishAdminCard(
          dish = dish,
          ingredients = ingredients,
          exchangeRate = config.exchangeRateUsdToBs,
          isAdmin = role == UserRole.ADMIN,
          onToggleAvailability = { isAvail ->
            viewModel.saveMenuItem(dish.copy(isAvailable = isAvail))
          },
          onEdit = { dishToEdit = dish }
        )
      }
    }

    // Diálogo de Edición / Nuevo Plato
    if (showNewDishDialog || dishToEdit != null) {
      val editing = dishToEdit
      EditMenuItemDialog(
        initialDish = editing,
        allSupplies = supplies,
        initialIngredients = if (editing != null) viewModel.parseIngredients(editing.ingredientsJson) else emptyList(),
        onDismiss = {
          showNewDishDialog = false
          dishToEdit = null
        },
        onSave = { dishToSave, ingredientsToSave ->
          val json = viewModel.serializeIngredients(ingredientsToSave)
          viewModel.saveMenuItem(dishToSave.copy(ingredientsJson = json))
          showNewDishDialog = false
          dishToEdit = null
        }
      )
    }
  }
}

@Composable
private fun DishAdminCard(
  dish: MenuItemDoc,
  ingredients: List<IngredientRequirement>,
  exchangeRate: Double,
  isAdmin: Boolean,
  onToggleAvailability: (Boolean) -> Unit,
  onEdit: () -> Unit
) {
  val priceBs = dish.priceUsd * exchangeRate

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(0.5.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
      .testTag("admin_dish_card_${dish.id}"),
    colors = CardDefaults.cardColors(containerColor = DeepCharcoal)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = dish.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
            color = Color.White
          )
          Text(
            text = "${dish.category}  •  ${dish.description.take(55)}",
            fontSize = 11.5.sp,
            color = Color(0xFFAAAAAA),
            maxLines = 1
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = String.format(Locale.US, "$%.2f", dish.priceUsd),
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              color = GrillFireRed
            )
            Text(
              text = String.format(Locale.US, "%.2f Bs", priceBs),
              fontSize = 11.sp,
              color = Color(0xFFFFB300),
              fontWeight = FontWeight.Bold
            )
          }

          if (isAdmin) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2E2E32)))
      Spacer(modifier = Modifier.height(8.dp))

      // Insumos que descuenta en tiempo real
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Insumos descontados (${ingredients.size}):",
            fontSize = 11.sp,
            color = Color(0xFFCCCCCC),
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (ingredients.isEmpty()) "Sin receta vinculada" else ingredients.joinToString(", ") { "${it.quantityRequired} ${it.unit} ${it.supplyName}" },
            fontSize = 10.5.sp,
            color = Color(0xFF999999),
            maxLines = 2
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (dish.isAvailable) "Disponible" else "Agotado",
            fontSize = 10.5.sp,
            color = if (dish.isAvailable) GreenSuccess else Color(0xFFFF5252),
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Switch(
            checked = dish.isAvailable,
            onCheckedChange = onToggleAvailability,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = GreenSuccess,
              uncheckedThumbColor = Color.White,
              uncheckedTrackColor = Color(0xFF444444)
            )
          )
        }
      }
    }
  }
}

@Composable
private fun EditMenuItemDialog(
  initialDish: MenuItemDoc?,
  allSupplies: List<SupplyDoc>,
  initialIngredients: List<IngredientRequirement>,
  onDismiss: () -> Unit,
  onSave: (MenuItemDoc, List<IngredientRequirement>) -> Unit
) {
  var name by remember { mutableStateOf(initialDish?.name ?: "") }
  var description by remember { mutableStateOf(initialDish?.description ?: "") }
  var category by remember { mutableStateOf(initialDish?.category ?: "Parrillas") }
  var priceText by remember { mutableStateOf("${initialDish?.priceUsd ?: 10.0}") }
  val ingredientsList = remember { mutableStateListOf<IngredientRequirement>().apply { addAll(initialIngredients) } }

  var selectedSupplyToAdd by remember { mutableStateOf<SupplyDoc?>(null) }
  var amountToAddText by remember { mutableStateOf("0.25") }

  val categories = listOf("Parrillas", "Hamburguesas & Pepitos", "Entradas & Guarniciones", "Bebidas")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("edit_menu_item_dialog"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = if (initialDish == null) "Nuevo Plato" else "Editar Plato",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nombre del Plato") },
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
          value = description,
          onValueChange = { description = it },
          label = { Text("Descripción") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GrillCrimson,
            unfocusedBorderColor = Color(0xFF444444),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Precio USD ($)") },
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
            value = category,
            onValueChange = { category = it },
            label = { Text("Categoría") },
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

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Receta / Insumos a Descontar (${ingredientsList.size}):",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )

        // Lista de insumos vinculados
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
        ) {
          items(ingredientsList) { ing ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "• ${ing.supplyName}: ${ing.quantityRequired} ${ing.unit}",
                color = Color(0xFFDDDDDD),
                fontSize = 11.5.sp
              )
              IconButton(
                onClick = { ingredientsList.remove(ing) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        // Agregar insumo a la receta
        if (allSupplies.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            val supply = selectedSupplyToAdd ?: allSupplies.firstOrNull()
            OutlinedTextField(
              value = amountToAddText,
              onValueChange = { amountToAddText = it },
              label = { Text("Cant (${supply?.unit ?: "kg"})") },
              modifier = Modifier.width(90.dp),
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
                val targetSup = supply ?: return@Button
                val qty = amountToAddText.toDoubleOrNull() ?: 0.1
                ingredientsList.add(
                  IngredientRequirement(
                    supplyId = targetSup.id,
                    supplyName = targetSup.name,
                    quantityRequired = qty,
                    unit = targetSup.unit,
                    isRemovable = true
                  )
                )
              },
              colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
              modifier = Modifier.weight(1f)
            ) {
              Text("+ ${supply?.name?.take(14) ?: ""}", fontSize = 11.sp, color = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Cancelar", color = Color.White)
          }
          Button(
            onClick = {
              if (name.isNotBlank()) {
                val toSave = MenuItemDoc(
                  id = initialDish?.id ?: "",
                  name = name,
                  description = description,
                  category = category,
                  priceUsd = priceText.toDoubleOrNull() ?: 10.0,
                  isAvailable = initialDish?.isAvailable ?: true
                )
                onSave(toSave, ingredientsList.toList())
              }
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = GrillCrimson)
          ) {
            Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }
}
