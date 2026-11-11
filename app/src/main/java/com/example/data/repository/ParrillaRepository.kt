package com.example.data.repository

import android.util.Log
import com.example.data.model.BusinessConfigDoc
import com.example.data.model.CartOrderItem
import com.example.data.model.ClosureDoc
import com.example.data.model.IngredientRequirement
import com.example.data.model.MenuItemDoc
import com.example.data.model.OrderDoc
import com.example.data.model.SupplyDoc
import com.example.data.model.SystemUserDoc
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repositorio central de La Parrilla 200 con alta resiliencia ante fallos de red,
 * reconexión automática en tiempo real con Firestore y tolerancia a nulos.
 */
class ParrillaRepository(
  private val db: FirebaseFirestore
) {

  companion object {
    private const val TAG = "ParrillaRepository"
    private const val MAX_RETRY_DELAY_MS = 10000L
  }

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val ingredientListType = Types.newParameterizedType(List::class.java, IngredientRequirement::class.java)
  private val ingredientAdapter = moshi.adapter<List<IngredientRequirement>>(ingredientListType)

  private val cartItemListType = Types.newParameterizedType(List::class.java, CartOrderItem::class.java)
  private val cartItemAdapter = moshi.adapter<List<CartOrderItem>>(cartItemListType)

  fun parseIngredients(json: String?): List<IngredientRequirement> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
      ingredientAdapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
      Log.w(TAG, "Error parseando ingredientes JSON: ${e.message}")
      emptyList()
    }
  }

  fun serializeIngredients(list: List<IngredientRequirement>?): String {
    if (list.isNullOrEmpty()) return "[]"
    return try {
      ingredientAdapter.toJson(list)
    } catch (e: Exception) {
      Log.w(TAG, "Error serializando ingredientes: ${e.message}")
      "[]"
    }
  }

  fun parseCartItems(json: String?): List<CartOrderItem> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
      cartItemAdapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
      Log.w(TAG, "Error parseando items de carrito: ${e.message}")
      emptyList()
    }
  }

  fun serializeCartItems(list: List<CartOrderItem>?): String {
    if (list.isNullOrEmpty()) return "[]"
    return try {
      cartItemAdapter.toJson(list)
    } catch (e: Exception) {
      Log.w(TAG, "Error serializando items de carrito: ${e.message}")
      "[]"
    }
  }

  // --- Motor de Consultas en Tiempo Real con Reconexión Automática ---

  /**
   * Crea un flujo en tiempo real resiliente utilizando addSnapshotListener.
   * Si ocurre un parpadeo de red o un error temporal, el listener no destruye el flujo.
   * Si el listener llega a cancelarse o cerrarse, retryWhen reconecta automáticamente
   * con retroceso exponencial sin congelar la interfaz ni cerrar la app.
   */
  private fun <T : Any> createResilientCollectionFlow(
    collectionPath: String,
    mapper: (DocumentSnapshot) -> T?,
    sorter: ((List<T>) -> List<T>)? = null
  ): Flow<List<T>> = callbackFlow {
    var isClosed = false
    var listenerRegistration: ListenerRegistration? = null

    try {
      listenerRegistration = db.collection(collectionPath)
        .addSnapshotListener { snapshot, error ->
          if (isClosed) return@addSnapshotListener

          if (error != null) {
            Log.w(TAG, "Aviso de red/Firestore en colección '$collectionPath': ${error.message}")
            // No cancelamos el canal en desconexiones temporales; Firestore maneja reintentos internos,
            // pero si la suscripción se rompe, se cerrará y retryWhen la reconstruirá.
            return@addSnapshotListener
          }

          if (snapshot != null) {
            val list = ArrayList<T>(snapshot.size())
            for (doc in snapshot.documents) {
              try {
                if (doc.exists()) {
                  val parsed = mapper(doc)
                  if (parsed != null) {
                    list.add(parsed)
                  }
                }
              } catch (e: Exception) {
                Log.e(TAG, "Error deserializando documento ${doc.id} en '$collectionPath'", e)
              }
            }
            val processed = if (sorter != null) {
              try {
                sorter(list)
              } catch (e: Exception) {
                Log.w(TAG, "Error ordenando lista de '$collectionPath': ${e.message}")
                list
              }
            } else {
              list
            }
            trySend(processed)
          }
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error al registrar listener en '$collectionPath'", e)
      close(e)
    }

    awaitClose {
      isClosed = true
      try {
        listenerRegistration?.remove()
      } catch (_: Exception) {}
    }
  }
  .retryWhen { cause, attempt ->
    Log.w(TAG, "Reconectando flujo de colección '$collectionPath' (intento #$attempt) tras: ${cause.message}")
    val backoff = minOf(1000L * (1L shl minOf(attempt.toInt(), 4)), MAX_RETRY_DELAY_MS)
    delay(backoff)
    true // Reintenta indefinidamente para reconectar al volver la red
  }
  .catch { e ->
    Log.e(TAG, "Excepción no recuperable en colección '$collectionPath', emitiendo lista vacía de resguardo", e)
    emit(emptyList())
  }
  .flowOn(Dispatchers.IO)

  /**
   * Crea un flujo resiliente para un documento individual con reconexión automática.
   */
  private fun <T : Any> createResilientDocumentFlow(
    collectionPath: String,
    documentId: String,
    fallback: T,
    mapper: (DocumentSnapshot) -> T?
  ): Flow<T> = callbackFlow {
    var isClosed = false
    var listenerRegistration: ListenerRegistration? = null

    try {
      listenerRegistration = db.collection(collectionPath).document(documentId)
        .addSnapshotListener { snapshot, error ->
          if (isClosed) return@addSnapshotListener

          if (error != null) {
            Log.w(TAG, "Aviso de red/Firestore en doc '$collectionPath/$documentId': ${error.message}")
            return@addSnapshotListener
          }

          if (snapshot != null && snapshot.exists()) {
            try {
              val parsed = mapper(snapshot) ?: fallback
              trySend(parsed)
            } catch (e: Exception) {
              Log.e(TAG, "Error deserializando doc '$collectionPath/$documentId'", e)
              trySend(fallback)
            }
          } else {
            trySend(fallback)
          }
        }
    } catch (e: Exception) {
      Log.e(TAG, "Error al registrar listener en '$collectionPath/$documentId'", e)
      close(e)
    }

    awaitClose {
      isClosed = true
      try {
        listenerRegistration?.remove()
      } catch (_: Exception) {}
    }
  }
  .retryWhen { cause, attempt ->
    Log.w(TAG, "Reconectando flujo de documento '$collectionPath/$documentId' (intento #$attempt)")
    val backoff = minOf(1000L * (1L shl minOf(attempt.toInt(), 4)), MAX_RETRY_DELAY_MS)
    delay(backoff)
    true
  }
  .catch { e ->
    Log.e(TAG, "Excepción en doc '$collectionPath/$documentId', emitiendo valor por defecto", e)
    emit(fallback)
  }
  .flowOn(Dispatchers.IO)

  // --- Observaciones en tiempo real públicas ---

  fun observeSupplies(): Flow<List<SupplyDoc>> =
    createResilientCollectionFlow(
      collectionPath = "supplies",
      mapper = { doc ->
        val supply = doc.toObject(SupplyDoc::class.java)
        supply?.copy(id = doc.id)
      },
      sorter = { list -> list.sortedBy { it.name.lowercase() } }
    )

  fun observeMenu(): Flow<List<MenuItemDoc>> =
    createResilientCollectionFlow(
      collectionPath = "menu",
      mapper = { doc ->
        val item = doc.toObject(MenuItemDoc::class.java)
        item?.copy(id = doc.id)
      },
      sorter = { list -> list.sortedBy { it.name.lowercase() } }
    )

  fun observeOrders(): Flow<List<OrderDoc>> =
    createResilientCollectionFlow(
      collectionPath = "orders",
      mapper = { doc ->
        val order = doc.toObject(OrderDoc::class.java)
        order?.copy(id = doc.id)
      },
      sorter = { list -> list.sortedByDescending { it.timestamp } }
    )

  fun observeClosures(): Flow<List<ClosureDoc>> =
    createResilientCollectionFlow(
      collectionPath = "closures",
      mapper = { doc ->
        val closure = doc.toObject(ClosureDoc::class.java)
        closure?.copy(id = doc.id)
      },
      sorter = { list -> list.sortedByDescending { it.timestamp } }
    )

  fun observeConfig(): Flow<BusinessConfigDoc> =
    createResilientDocumentFlow(
      collectionPath = "business",
      documentId = "config",
      fallback = BusinessConfigDoc(),
      mapper = { doc ->
        doc.toObject(BusinessConfigDoc::class.java)
      }
    )

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    createResilientDocumentFlow(
      collectionPath = "users",
      documentId = userId,
      fallback = UserProfile(id = userId, userId = userId),
      mapper = { doc ->
        doc.toObject(UserProfile::class.java)?.copy(id = doc.id)
      }
    )

  fun observeAllUsers(): Flow<List<UserProfile>> =
    createResilientCollectionFlow(
      collectionPath = "users",
      mapper = { doc ->
        doc.toObject(UserProfile::class.java)?.copy(id = doc.id)
      },
      sorter = { list -> list.sortedBy { it.displayName.lowercase() } }
    )

  fun observeSystemUsers(): Flow<List<SystemUserDoc>> =
    createResilientCollectionFlow(
      collectionPath = "system_users",
      mapper = { doc ->
        doc.toObject(SystemUserDoc::class.java)?.copy(id = doc.id)
      },
      sorter = { list -> list.sortedBy { it.username.lowercase() } }
    )

  // --- Operaciones de Escritura Asíncronas Robustas con Try-Catch ---

  suspend fun registerOrUpdateUser(
    userId: String,
    email: String,
    displayName: String
  ): UserProfile = withContext(Dispatchers.IO) {
    try {
      val docRef = db.collection("users").document(userId)
      val snap = docRef.get().await()
      if (snap != null && snap.exists()) {
        snap.toObject(UserProfile::class.java)
          ?: UserProfile(id = userId, userId = userId, email = email, displayName = displayName)
      } else {
        val usersSnapshot = try { db.collection("users").get().await() } catch (_: Exception) { null }
        val isFirstUser = usersSnapshot == null || usersSnapshot.isEmpty
        val initialRole = if (isFirstUser) UserRole.ADMIN.name else UserRole.STAFF.name

        val newProfile = UserProfile(
          id = userId,
          userId = userId,
          email = email,
          displayName = displayName.ifBlank { "Personal Parrilla" },
          role = initialRole,
          createdAt = System.currentTimeMillis()
        )
        docRef.set(newProfile).await()
        newProfile
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error en registerOrUpdateUser para $userId: ${e.message}", e)
      UserProfile(
        id = userId,
        userId = userId,
        email = email,
        displayName = displayName.ifBlank { "Usuario Local" },
        role = UserRole.STAFF.name
      )
    }
  }

  suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      db.collection("users").document(userId)
        .update("role", newRole)
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error actualizando rol de usuario $userId: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun saveConfig(config: BusinessConfigDoc): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      db.collection("business").document("config")
        .set(config)
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error guardando configuración del negocio: ${e.message}", e)
      Result.failure(e)
    }
  }

  /**
   * Crea un nuevo pedido y descuenta automáticamente los insumos en tiempo real.
   * Si la deducción de inventario falla por temas de red, el pedido igualmente
   * se guarda con éxito para que el cliente no pierda su orden ni su ticket.
   */
  suspend fun createOrderAndDeductStock(
    order: OrderDoc,
    cartItems: List<CartOrderItem>,
    menuList: List<MenuItemDoc>
  ): String = withContext(Dispatchers.IO) {
    val orderRef = db.collection("orders").document()
    val serializedItems = serializeCartItems(cartItems)
    val orderToSave = order.copy(
      id = orderRef.id,
      itemsJson = serializedItems
    )

    // 1. Guardar el pedido con reintento seguro
    try {
      orderRef.set(orderToSave).await()
    } catch (e: Exception) {
      Log.e(TAG, "Error al guardar el pedido en Firestore: ${e.message}", e)
      throw e
    }

    // 2. Descontar insumos en tiempo real (tolerante a fallos)
    try {
      val menuMap = menuList.associateBy { it.id }
      val suppliesSnapshot = db.collection("supplies").get().await()
      val supplyMap = suppliesSnapshot.toObjects(SupplyDoc::class.java).associateBy { it.id }

      for (cartItem in cartItems) {
        val menuItem = menuMap[cartItem.menuItemId] ?: continue
        val recipe = parseIngredients(menuItem.ingredientsJson)

        for (ingredient in recipe) {
          val isExcluded = cartItem.excludedIngredientNames.any {
            it.equals(ingredient.supplyName, ignoreCase = true)
          }
          if (isExcluded) continue

          val supply = supplyMap[ingredient.supplyId]
          if (supply != null) {
            val totalDeduction = (ingredient.quantityRequired * cartItem.quantity).coerceAtLeast(0.0)
            val newStock = (supply.currentStock - totalDeduction).coerceAtLeast(0.0)
            try {
              db.collection("supplies").document(supply.id)
                .update("currentStock", newStock)
                .await()
            } catch (deductErr: Exception) {
              Log.w(TAG, "Aviso deduciendo insumo ${supply.name}: ${deductErr.message}")
            }
          }
        }
      }
    } catch (invError: Exception) {
      Log.w(TAG, "Aviso en deducción automática de inventario: ${invError.message}")
    }

    orderRef.id
  }

  suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (orderId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de orden inválido"))
      db.collection("orders").document(orderId)
        .update("status", newStatus)
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error actualizando estado de orden $orderId a $newStatus: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun markOrderAsPaid(
    orderId: String,
    paymentMethod: String,
    amountUsd: Double,
    amountPagoMovilBs: Double,
    amountPuntoVentaBs: Double,
    refCode: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (orderId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de orden inválido"))
      val updateData = mapOf(
        "isPaid" to true,
        "paymentMethod" to paymentMethod,
        "amountUsdPaid" to amountUsd,
        "amountPagoMovilBs" to amountPagoMovilBs,
        "amountPuntoVentaBs" to amountPuntoVentaBs,
        "referenceCode" to refCode
      )
      // Usar set con merge para idempotencia total en desconexiones
      db.collection("orders").document(orderId)
        .set(updateData, SetOptions.merge())
        .await()
      Log.i(TAG, "Orden $orderId marcada como pagada exitosamente a la primera")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error marcando orden $orderId como pagada: ${e.message}", e)
      Result.failure(e)
    }
  }

  /**
   * Cancela una orden y restaura automáticamente los insumos al inventario.
   */
  suspend fun cancelOrder(
    orderId: String,
    restoreStock: Boolean = true
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (orderId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de orden inválido"))
      val orderDocRef = db.collection("orders").document(orderId)
      val orderSnap = orderDocRef.get().await()

      if (restoreStock && orderSnap.exists()) {
        val itemsJson = orderSnap.getString("itemsJson")
        val items = parseCartItems(itemsJson)
        val menuList = db.collection("menu").get().await().toObjects(MenuItemDoc::class.java)
        val menuMap = menuList.associateBy { it.id }
        val suppliesSnapshot = db.collection("supplies").get().await()
        val supplyMap = suppliesSnapshot.toObjects(SupplyDoc::class.java).associateBy { it.id }

        for (cartItem in items) {
          val menuItem = menuMap[cartItem.menuItemId] ?: continue
          val recipe = parseIngredients(menuItem.ingredientsJson)

          for (ingredient in recipe) {
            val isExcluded = cartItem.excludedIngredientNames.any {
              it.equals(ingredient.supplyName, ignoreCase = true)
            }
            if (isExcluded) continue

            val supply = supplyMap[ingredient.supplyId]
            if (supply != null) {
              val returnAmount = (ingredient.quantityRequired * cartItem.quantity).coerceAtLeast(0.0)
              val restoredStock = supply.currentStock + returnAmount
              try {
                db.collection("supplies").document(supply.id)
                  .update("currentStock", restoredStock)
                  .await()
              } catch (resErr: Exception) {
                Log.w(TAG, "Aviso restaurando insumo ${supply.name}: ${resErr.message}")
              }
            }
          }
        }
      }

      // Marcar orden como CANCELADO
      orderDocRef.set(mapOf("status" to "CANCELADO"), SetOptions.merge()).await()
      Log.i(TAG, "Orden $orderId cancelada exitosamente")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error cancelando orden $orderId: ${e.message}", e)
      Result.failure(e)
    }
  }

  /**
   * Elimina definitivamente una orden de la base de datos Firestore.
   */
  suspend fun deleteOrder(orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (orderId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de orden inválido"))
      db.collection("orders").document(orderId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error eliminando orden $orderId: ${e.message}", e)
      Result.failure(e)
    }
  }

  /**
   * Modifica una cuenta abierta existente (agrega bebidas, platos adicionales o cambios de notas),
   * actualiza los totales y descuenta los insumos de los platos recién agregados.
   */
  suspend fun updateOpenOrder(
    orderId: String,
    updatedItems: List<CartOrderItem>,
    newTotalUsd: Double,
    newTotalBs: Double,
    newlyAddedItems: List<CartOrderItem> = emptyList(),
    menuList: List<MenuItemDoc> = emptyList(),
    notes: String? = null
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (orderId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de orden inválido"))
      val serializedItems = serializeCartItems(updatedItems)
      val orderDocRef = db.collection("orders").document(orderId)

      val updateMap = mutableMapOf<String, Any>(
        "itemsJson" to serializedItems,
        "totalUsd" to newTotalUsd,
        "totalBs" to newTotalBs,
        "status" to "EN_COCINA" // Notifica a cocina que la cuenta tiene adiciones pendientes
      )
      if (notes != null) {
        updateMap["notes"] = notes
      }
      orderDocRef.set(updateMap, SetOptions.merge()).await()

      // Si se agregaron ítems nuevos, descontar los insumos de esos ítems únicamente
      if (newlyAddedItems.isNotEmpty() && menuList.isNotEmpty()) {
        try {
          val menuMap = menuList.associateBy { it.id }
          val suppliesSnapshot = db.collection("supplies").get().await()
          val supplyMap = suppliesSnapshot.toObjects(SupplyDoc::class.java).associateBy { it.id }

          for (cartItem in newlyAddedItems) {
            val menuItem = menuMap[cartItem.menuItemId] ?: continue
            val recipe = parseIngredients(menuItem.ingredientsJson)

            for (ingredient in recipe) {
              val isExcluded = cartItem.excludedIngredientNames.any {
                it.equals(ingredient.supplyName, ignoreCase = true)
              }
              if (isExcluded) continue

              val supply = supplyMap[ingredient.supplyId]
              if (supply != null) {
                val totalDeduction = (ingredient.quantityRequired * cartItem.quantity).coerceAtLeast(0.0)
                val newStock = (supply.currentStock - totalDeduction).coerceAtLeast(0.0)
                try {
                  db.collection("supplies").document(supply.id)
                    .update("currentStock", newStock)
                    .await()
                } catch (deductErr: Exception) {
                  Log.w(TAG, "Aviso deduciendo insumo adicional ${supply.name}: ${deductErr.message}")
                }
              }
            }
          }
        } catch (invErr: Exception) {
          Log.w(TAG, "Aviso en deducción de adición a cuenta abierta: ${invErr.message}")
        }
      }

      Log.i(TAG, "Orden abierta $orderId actualizada con nuevos productos")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error actualizando cuenta abierta $orderId: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun restockSupply(supplyId: String, addedAmount: Double): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (supplyId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de insumo inválido"))
      val docRef = db.collection("supplies").document(supplyId)
      val snap = docRef.get().await()
      val current = snap.getDouble("currentStock") ?: 0.0
      docRef.update("currentStock", current + addedAmount).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error reabasteciendo insumo $supplyId: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun addOrUpdateSupply(supply: SupplyDoc): Result<String> = withContext(Dispatchers.IO) {
    try {
      val ref = if (supply.id.isBlank()) db.collection("supplies").document() else db.collection("supplies").document(supply.id)
      val toSave = supply.copy(id = ref.id)
      ref.set(toSave).await()
      Result.success(ref.id)
    } catch (e: Exception) {
      Log.e(TAG, "Error guardando insumo: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun addOrUpdateMenuItem(item: MenuItemDoc): Result<String> = withContext(Dispatchers.IO) {
    try {
      val ref = if (item.id.isBlank()) db.collection("menu").document() else db.collection("menu").document(item.id)
      val toSave = item.copy(id = ref.id)
      ref.set(toSave).await()
      Result.success(ref.id)
    } catch (e: Exception) {
      Log.e(TAG, "Error guardando plato: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteMenuItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (itemId.isBlank()) return@withContext Result.failure(IllegalArgumentException("ID de plato inválido"))
      db.collection("menu").document(itemId).delete().await()
      Log.i(TAG, "Plato $itemId eliminado del menú en Firestore")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error eliminando plato $itemId: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun saveCashClosure(closure: ClosureDoc): String = withContext(Dispatchers.IO) {
    try {
      val ref = db.collection("closures").document()
      val toSave = closure.copy(id = ref.id)
      ref.set(toSave).await()
      ref.id
    } catch (e: Exception) {
      Log.e(TAG, "Error guardando cierre de caja: ${e.message}", e)
      throw e
    }
  }

  /**
   * Pre-carga datos iniciales completos y realistas de La Parrilla 200 si la base está vacía.
   */
  suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
    try {
      val suppliesSnap = db.collection("supplies").limit(1).get().await()
      if (!suppliesSnap.isEmpty) return@withContext

      // 1. Insumos iniciales
      val defaultSupplies = listOf(
        SupplyDoc(id = "sup_carne_solomo", name = "Carne Solomo de Cuerito", currentStock = 18.5, minStockThreshold = 5.0, unit = "kg", costUsd = 6.50, recommendedRestock = 15.0),
        SupplyDoc(id = "sup_punta_trasera", name = "Punta Trasera Angus", currentStock = 12.0, minStockThreshold = 4.0, unit = "kg", costUsd = 8.50, recommendedRestock = 10.0),
        SupplyDoc(id = "sup_pollo_pechuga", name = "Pechuga de Pollo", currentStock = 15.0, minStockThreshold = 5.0, unit = "kg", costUsd = 4.20, recommendedRestock = 12.0),
        SupplyDoc(id = "sup_chorizo", name = "Chorizo Ahumado Parrillero", currentStock = 35.0, minStockThreshold = 10.0, unit = "ud", costUsd = 1.00, recommendedRestock = 30.0),
        SupplyDoc(id = "sup_morcilla", name = "Morcilla Artesanal", currentStock = 20.0, minStockThreshold = 8.0, unit = "ud", costUsd = 0.90, recommendedRestock = 20.0),
        SupplyDoc(id = "sup_queso_llanero", name = "Queso Llanero para Asar", currentStock = 8.0, minStockThreshold = 3.0, unit = "kg", costUsd = 4.50, recommendedRestock = 8.0),
        SupplyDoc(id = "sup_yuca", name = "Yuca Lista para Freír/Hervir", currentStock = 25.0, minStockThreshold = 6.0, unit = "kg", costUsd = 1.20, recommendedRestock = 20.0),
        SupplyDoc(id = "sup_papas", name = "Papas para Bastones Fritos", currentStock = 20.0, minStockThreshold = 6.0, unit = "kg", costUsd = 1.50, recommendedRestock = 15.0),
        SupplyDoc(id = "sup_pan_hamburguesa", name = "Pan Artesanal Parrillero", currentStock = 30.0, minStockThreshold = 10.0, unit = "ud", costUsd = 0.50, recommendedRestock = 30.0),
        SupplyDoc(id = "sup_pan_pepito", name = "Pan Baguette Pepito 30cm", currentStock = 18.0, minStockThreshold = 6.0, unit = "ud", costUsd = 0.70, recommendedRestock = 20.0),
        SupplyDoc(id = "sup_guasacaca", name = "Guasacaca Especial de la Casa", currentStock = 6.5, minStockThreshold = 2.0, unit = "lt", costUsd = 3.00, recommendedRestock = 5.0),
        SupplyDoc(id = "sup_salsa_bbq", name = "Salsa BBQ Ahumada 200", currentStock = 5.0, minStockThreshold = 1.5, unit = "lt", costUsd = 3.50, recommendedRestock = 4.0),
        SupplyDoc(id = "sup_salsa_ajo", name = "Salsa de Ajo Criolla", currentStock = 4.0, minStockThreshold = 1.5, unit = "lt", costUsd = 2.80, recommendedRestock = 4.0),
        SupplyDoc(id = "sup_cebolla_asada", name = "Cebolla & Pimentón Asados", currentStock = 7.0, minStockThreshold = 2.5, unit = "kg", costUsd = 1.80, recommendedRestock = 6.0),
        SupplyDoc(id = "sup_refresco", name = "Refresco 1.5L Variados", currentStock = 24.0, minStockThreshold = 6.0, unit = "ud", costUsd = 1.40, recommendedRestock = 24.0),
        SupplyDoc(id = "sup_cerveza", name = "Cerveza Polar Fría / Malta", currentStock = 48.0, minStockThreshold = 12.0, unit = "ud", costUsd = 0.85, recommendedRestock = 48.0)
      )

      for (s in defaultSupplies) {
        db.collection("supplies").document(s.id).set(s).await()
      }

      // 2. Platos del menú con sus recetas de insumos
      val defaultMenu = listOf(
        MenuItemDoc(
          id = "dish_parrilla_familiar",
          name = "Parrilla 200 Mixta Familiar (4 personas)",
          description = "400g Solomo, 300g Pollo, 2 Chorizos, 1 Morcilla, Yuca con Queso a la Plancha y Guasacaca.",
          category = "Parrillas",
          priceUsd = 22.00,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_carne_solomo", "Carne Solomo de Cuerito", 0.40, "kg"),
              IngredientRequirement("sup_pollo_pechuga", "Pechuga de Pollo", 0.30, "kg"),
              IngredientRequirement("sup_chorizo", "Chorizo Ahumado Parrillero", 2.0, "ud"),
              IngredientRequirement("sup_morcilla", "Morcilla Artesanal", 1.0, "ud"),
              IngredientRequirement("sup_yuca", "Yuca Lista para Freír/Hervir", 0.35, "kg"),
              IngredientRequirement("sup_queso_llanero", "Queso Llanero para Asar", 0.20, "kg"),
              IngredientRequirement("sup_guasacaca", "Guasacaca Especial de la Casa", 0.10, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_parrilla_solomo",
          name = "Parrilla Solomo de Cuerito Especial",
          description = "350g de jugoso Solomo a la brasa, acompañado de Yuca frita, Queso frito y Guasacaca.",
          category = "Parrillas",
          priceUsd = 13.50,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_carne_solomo", "Carne Solomo de Cuerito", 0.35, "kg"),
              IngredientRequirement("sup_yuca", "Yuca Lista para Freír/Hervir", 0.25, "kg"),
              IngredientRequirement("sup_queso_llanero", "Queso Llanero para Asar", 0.15, "kg"),
              IngredientRequirement("sup_guasacaca", "Guasacaca Especial de la Casa", 0.08, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_punta_trasera",
          name = "Punta Trasera Angus Premium (400g)",
          description = "Corte seleccionado sellado al carbón con sal marina, Yuca al grill y salsa BBQ 200.",
          category = "Parrillas",
          priceUsd = 16.00,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_punta_trasera", "Punta Trasera Angus", 0.40, "kg"),
              IngredientRequirement("sup_yuca", "Yuca Lista para Freír/Hervir", 0.25, "kg"),
              IngredientRequirement("sup_salsa_bbq", "Salsa BBQ Ahumada 200", 0.08, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_parrilla_pollo",
          name = "Parrilla de Pollo a la Brasa",
          description = "350g pechuga de pollo marinada al grill, Papas fritas crocantes y salsa de ajo.",
          category = "Parrillas",
          priceUsd = 10.50,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_pollo_pechuga", "Pechuga de Pollo", 0.35, "kg"),
              IngredientRequirement("sup_papas", "Papas para Bastones Fritos", 0.25, "kg"),
              IngredientRequirement("sup_salsa_ajo", "Salsa de Ajo Criolla", 0.08, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_hamburguesa_200",
          name = "Hamburguesa Parrillera La 200",
          description = "200g carne a la parrilla, Queso a la plancha, Cebolla y Pimentón asados, salsa BBQ en pan brioche.",
          category = "Hamburguesas & Pepitos",
          priceUsd = 8.50,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_pan_hamburguesa", "Pan Artesanal Parrillero", 1.0, "ud"),
              IngredientRequirement("sup_carne_solomo", "Carne Solomo de Cuerito", 0.20, "kg"),
              IngredientRequirement("sup_queso_llanero", "Queso Llanero para Asar", 0.08, "kg"),
              IngredientRequirement("sup_cebolla_asada", "Cebolla & Pimentón Asados", 0.06, "kg"),
              IngredientRequirement("sup_salsa_bbq", "Salsa BBQ Ahumada 200", 0.04, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_pepito_mixto",
          name = "Pepito Mixto Gratinado 30cm",
          description = "Carne y pollo salteados a la brasa, queso llanero gratinado, salsa de ajo y papitas ralladas.",
          category = "Hamburguesas & Pepitos",
          priceUsd = 9.50,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_pan_pepito", "Pan Baguette Pepito 30cm", 1.0, "ud"),
              IngredientRequirement("sup_carne_solomo", "Carne Solomo de Cuerito", 0.15, "kg"),
              IngredientRequirement("sup_pollo_pechuga", "Pechuga de Pollo", 0.15, "kg"),
              IngredientRequirement("sup_queso_llanero", "Queso Llanero para Asar", 0.10, "kg"),
              IngredientRequirement("sup_salsa_ajo", "Salsa de Ajo Criolla", 0.05, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_choripan",
          name = "Choripán Artesanal con Guasacaca",
          description = "Chorizo parrillero ahumado en pan tostado con abundate guasacaca y salsa de ajo.",
          category = "Entradas & Guarniciones",
          priceUsd = 4.50,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_pan_hamburguesa", "Pan Artesanal Parrillero", 1.0, "ud"),
              IngredientRequirement("sup_chorizo", "Chorizo Ahumado Parrillero", 1.0, "ud"),
              IngredientRequirement("sup_guasacaca", "Guasacaca Especial de la Casa", 0.05, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_racion_yuca",
          name = "Ración de Yuca Frita con Queso Asado",
          description = "Bastones de yuca crujientes con dados de queso a la brasa y guasacaca.",
          category = "Entradas & Guarniciones",
          priceUsd = 4.00,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_yuca", "Yuca Lista para Freír/Hervir", 0.30, "kg"),
              IngredientRequirement("sup_queso_llanero", "Queso Llanero para Asar", 0.12, "kg"),
              IngredientRequirement("sup_guasacaca", "Guasacaca Especial de la Casa", 0.05, "lt")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_refresco_15l",
          name = "Refresco Frío 1.5 Litros",
          description = "Coca-Cola, Pepsi o Chinotto bien frío.",
          category = "Bebidas",
          priceUsd = 2.50,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_refresco", "Refresco 1.5L Variados", 1.0, "ud")
            )
          )
        ),
        MenuItemDoc(
          id = "dish_cerveza_polar",
          name = "Cerveza Polar / Malta Bien Fría",
          description = "Polar Pilsen / Light / Malta a punto de nieve.",
          category = "Bebidas",
          priceUsd = 1.50,
          ingredientsJson = serializeIngredients(
            listOf(
              IngredientRequirement("sup_cerveza", "Cerveza Polar Fría / Malta", 1.0, "ud")
            )
          )
        )
      )

      for (m in defaultMenu) {
        db.collection("menu").document(m.id).set(m).await()
      }

      // 3. Configuración del negocio
      val config = BusinessConfigDoc()
      db.collection("business").document("config").set(config).await()

      // 4. Usuarios del sistema
      seedDefaultSystemUsersIfEmpty()
    } catch (e: Exception) {
      Log.w(TAG, "Aviso en seedInitialDataIfEmpty: ${e.message}")
    }
  }

  // --- Gestión de Usuarios y Claves de Personal ---

  suspend fun seedDefaultSystemUsersIfEmpty() = withContext(Dispatchers.IO) {
    try {
      val snap = db.collection("system_users").get().await()
      if (snap.isEmpty) {
        val defaultAdmin = SystemUserDoc(
          id = "admin",
          username = "admin",
          password = "admin200",
          displayName = "Administrador Principal",
          role = "ADMIN",
          createdAt = System.currentTimeMillis(),
          isActive = true,
          canUsePosApp = true,
          canUseKitchenApp = true,
          canUseBillingApp = true,
          canUseInventoryApp = true,
          canUseClosuresApp = true,
          canUseMenuApp = true,
          canUseSettingsApp = true,
          canAccessPc = true,
          canUsePosPc = true,
          canUseKitchenPc = true,
          canUseBillingPc = true,
          canUseInventoryPc = true,
          canUseReportsPc = true
        )
        val defaultCashier = SystemUserDoc(
          id = "cajero",
          username = "cajero",
          password = "1234",
          displayName = "Cajero 1",
          role = "STAFF",
          createdAt = System.currentTimeMillis(),
          isActive = true,
          canUsePosApp = true,
          canUseKitchenApp = true,
          canUseBillingApp = true,
          canUseInventoryApp = false,
          canUseClosuresApp = false,
          canUseMenuApp = false,
          canUseSettingsApp = false,
          canAccessPc = true,
          canUsePosPc = true,
          canUseKitchenPc = false,
          canUseBillingPc = true,
          canUseInventoryPc = false,
          canUseReportsPc = false
        )
        val defaultKitchen = SystemUserDoc(
          id = "cocina",
          username = "cocina",
          password = "1234",
          displayName = "Personal de Cocina",
          role = "STAFF",
          createdAt = System.currentTimeMillis(),
          isActive = true,
          canUsePosApp = false,
          canUseKitchenApp = true,
          canUseBillingApp = false,
          canUseInventoryApp = true,
          canUseClosuresApp = false,
          canUseMenuApp = false,
          canUseSettingsApp = false,
          canAccessPc = true,
          canUsePosPc = false,
          canUseKitchenPc = true,
          canUseBillingPc = false,
          canUseInventoryPc = true,
          canUseReportsPc = false
        )
        db.collection("system_users").document("admin").set(defaultAdmin).await()
        db.collection("system_users").document("cajero").set(defaultCashier).await()
        db.collection("system_users").document("cocina").set(defaultKitchen).await()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Aviso en seedDefaultSystemUsersIfEmpty: ${e.message}")
    }
  }

  suspend fun authenticateSystemUser(username: String, password: String): SystemUserDoc? = withContext(Dispatchers.IO) {
    val cleanUsername = username.trim().lowercase()
    val cleanPass = password.trim()
    try {
      val doc = db.collection("system_users").document(cleanUsername).get().await()
      if (doc != null && doc.exists()) {
        val user = doc.toObject(SystemUserDoc::class.java)
        if (user != null && user.isActive && user.password == cleanPass) {
          return@withContext user
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Aviso al autenticar '$cleanUsername': ${e.message}")
    }

    // Fallback: Si la base está vacía o sin inicializar, asegurar default admin
    if (cleanUsername == "admin" && (cleanPass == "admin200" || cleanPass == "admin")) {
      try {
        seedDefaultSystemUsersIfEmpty()
      } catch (_: Exception) {}
      return@withContext SystemUserDoc(
        id = "admin",
        username = "admin",
        password = cleanPass,
        displayName = "Administrador",
        role = "ADMIN"
      )
    }
    null
  }

  suspend fun saveOrUpdateSystemUser(
    user: SystemUserDoc,
    previousUsername: String? = null
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val targetId = user.username.trim().lowercase()
      if (targetId.isBlank()) return@withContext Result.failure(IllegalArgumentException("El usuario no puede estar vacío"))
      if (user.password.trim().isBlank()) return@withContext Result.failure(IllegalArgumentException("La clave no puede estar vacía"))

      val normalizedUser = user.copy(
        id = targetId,
        username = targetId
      )

      // Si cambió el nombre de usuario, eliminar el documento previo
      if (previousUsername != null) {
        val oldId = previousUsername.trim().lowercase()
        if (oldId.isNotBlank() && oldId != targetId) {
          try {
            db.collection("system_users").document(oldId).delete().await()
          } catch (delErr: Exception) {
            Log.w(TAG, "Aviso eliminando usuario previo $oldId: ${delErr.message}")
          }
        }
      }

      db.collection("system_users").document(targetId).set(normalizedUser).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error guardando usuario de sistema: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteSystemUser(username: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val targetId = username.trim().lowercase()
      val snap = db.collection("system_users").get().await()
      val users = snap.toObjects(SystemUserDoc::class.java)
      val userToDelete = users.firstOrNull { it.username.equals(targetId, ignoreCase = true) }

      if (userToDelete?.role.equals("ADMIN", ignoreCase = true)) {
        val adminCount = users.count { it.role.equals("ADMIN", ignoreCase = true) }
        if (adminCount <= 1) {
          return@withContext Result.failure(IllegalStateException("No puedes eliminar el único Administrador del sistema"))
        }
      }

      db.collection("system_users").document(targetId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Error eliminando usuario '$username': ${e.message}", e)
      Result.failure(e)
    }
  }
}
