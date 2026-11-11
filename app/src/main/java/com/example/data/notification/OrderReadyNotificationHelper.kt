package com.example.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.data.model.OrderDoc
import java.util.Locale

/**
 * Gestor de notificaciones push / sistema para alertar a Cajeros y Mesoneros
 * cuando los cocineros marcan un pedido como "LISTO" para servir en La Parrilla 200.
 */
object OrderReadyNotificationHelper {

  private const val TAG = "OrderReadyNotify"
  const val CHANNEL_ID = "parrilla_order_ready_alerts"
  private const val CHANNEL_NAME = "Pedidos Listos en Cocina"
  private const val CHANNEL_DESC = "Avisos inmediatos a cajeros y mesoneros cuando un pedido está cocinado y listo para entregar"

  private val recentlyNotifiedOrderReady = mutableSetOf<String>()

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 300, 150, 300)
        setSound(soundUri, null)
      }
      notificationManager?.createNotificationChannel(channel)
    }
  }

  fun notifyOrderReady(
    context: Context,
    order: OrderDoc
  ) {
    try {
      val notifyKey = "${order.id}-LISTO"
      if (recentlyNotifiedOrderReady.contains(notifyKey)) return

      // Verificar permiso en Android 13+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
          Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido para alertas de pedido listo")
          return
        }
      }

      createNotificationChannel(context)

      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return

      val notificationId = (order.orderNumber * 1000) + 77

      val title = "🍽️ ¡Pedido #${String.format(Locale.US, "%03d", order.orderNumber)} LISTO en Cocina!"
      val shortText = "${order.tableOrDestination} • Cliente: ${order.customerName.ifBlank { "General" }}"
      val expandedText = "¡ATENCIÓN MESONEROS Y CAJA!\n" +
        "El pedido #${order.orderNumber} para ${order.tableOrDestination} está listo en la barra de despacho.\n" +
        "Cliente: ${order.customerName.ifBlank { "General" }}\n" +
        "Monto: $${String.format(Locale.US, "%.2f", order.totalUsd)} (${String.format(Locale.US, "%.2f", order.totalBs)} Bs)"

      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(shortText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setAutoCancel(true)
        .setVibrate(longArrayOf(0, 300, 150, 300))
        .build()

      notificationManager.notify(notificationId, notification)
      recentlyNotifiedOrderReady.add(notifyKey)
      Log.i(TAG, "Notificación push enviada a Cajero y Mesoneros por Pedido #${order.orderNumber} LISTO")
    } catch (e: Exception) {
      Log.e(TAG, "Error enviando notificación de pedido listo: ${e.message}", e)
    }
  }

  fun resetNotified() {
    recentlyNotifiedOrderReady.clear()
  }
}
