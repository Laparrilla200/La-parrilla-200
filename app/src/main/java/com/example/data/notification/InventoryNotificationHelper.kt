package com.example.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Gestor de notificaciones push / sistema para insumos críticos o faltantes en La Parrilla 200.
 * Notifica EXCLUSIVAMENTE al usuario con rol de Administrador.
 */
object InventoryNotificationHelper {

  private const val TAG = "InventoryNotify"
  const val CHANNEL_ID = "parrilla_inventory_alerts"
  private const val CHANNEL_NAME = "Alertas de Inventario e Insumos"
  private const val CHANNEL_DESC = "Notificaciones automáticas cuando un insumo está bajo o agotado"

  private val recentlyNotifiedSupplies = mutableSetOf<String>()

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
      }
      notificationManager?.createNotificationChannel(channel)
    }
  }

  fun notifyCriticalSupply(
    context: Context,
    supplyId: String,
    supplyName: String,
    currentStock: Double,
    minThreshold: Double,
    unit: String
  ) {
    try {
      // Solo notificar si no hemos notificado este insumo en esta tanda
      val notifyKey = "$supplyId-${currentStock.toInt()}"
      if (recentlyNotifiedSupplies.contains(notifyKey)) return

      // Verificar permiso en Android 13+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
          Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido para alertas de inventario")
          return
        }
      }

      createNotificationChannel(context)

      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return

      val notificationId = supplyId.hashCode()

      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setContentTitle("⚠️ Insumo Crítico: $supplyName")
        .setContentText("Stock bajo: $currentStock $unit (Mínimo: $minThreshold $unit). Reabastecer.")
        .setStyle(
          NotificationCompat.BigTextStyle().bigText(
            "ALERTA ADMINISTRADOR: El insumo '$supplyName' ha alcanzado su nivel crítico.\n" +
              "Stock actual: $currentStock $unit | Mínimo requerido: $minThreshold $unit.\n" +
              "Por favor reabastezca este insumo para no detener la cocina."
          )
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

      notificationManager.notify(notificationId, notification)
      recentlyNotifiedSupplies.add(notifyKey)
      Log.i(TAG, "Notificación enviada a Admin por insumo bajo: $supplyName")
    } catch (e: Exception) {
      Log.e(TAG, "Error enviando notificación de inventario: ${e.message}", e)
    }
  }

  fun resetNotifiedSupplies() {
    recentlyNotifiedSupplies.clear()
  }
}
