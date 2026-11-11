package com.example.data.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.SystemUserDoc

/**
 * Gestor persistente de sesión para La Parrilla 200.
 * Garantiza que la sesión de usuario (Admin o Personal) se mantenga guardada
 * y NUNCA se cierre de golpe por reinicio de actividad, suspensión o fallos de red,
 * cerrándose ÚNICAMENTE cuando el usuario presiona explícitamente "Cerrar Sesión".
 */
class SessionManager(context: Context) {

  companion object {
    private const val TAG = "SessionManager"
    private const val PREFS_NAME = "la_parrilla_user_session_prefs"
    private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_USERNAME = "key_username"
    private const val KEY_PASSWORD = "key_password"
    private const val KEY_DISPLAY_NAME = "key_display_name"
    private const val KEY_ROLE = "key_role"
    private const val KEY_CREATED_AT = "key_created_at"
    private const val KEY_IS_ACTIVE = "key_is_active"

    private const val KEY_CAN_POS_APP = "key_can_pos_app"
    private const val KEY_CAN_KITCHEN_APP = "key_can_kitchen_app"
    private const val KEY_CAN_BILLING_APP = "key_can_billing_app"
    private const val KEY_CAN_INVENTORY_APP = "key_can_inventory_app"
    private const val KEY_CAN_CLOSURES_APP = "key_can_closures_app"
    private const val KEY_CAN_MENU_APP = "key_can_menu_app"
    private const val KEY_CAN_SETTINGS_APP = "key_can_settings_app"

    private const val KEY_CAN_ACCESS_PC = "key_can_access_pc"
    private const val KEY_CAN_POS_PC = "key_can_pos_pc"
    private const val KEY_CAN_KITCHEN_PC = "key_can_kitchen_pc"
    private const val KEY_CAN_BILLING_PC = "key_can_billing_pc"
    private const val KEY_CAN_INVENTORY_PC = "key_can_inventory_pc"
    private const val KEY_CAN_REPORTS_PC = "key_can_reports_pc"
  }

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  /**
   * Guarda de forma persistente la sesión del usuario autenticado y sus permisos.
   */
  fun saveSession(user: SystemUserDoc) {
    try {
      prefs.edit()
        .putBoolean(KEY_IS_LOGGED_IN, true)
        .putString(KEY_USER_ID, user.id.ifBlank { user.username.trim().lowercase() })
        .putString(KEY_USERNAME, user.username.trim().lowercase())
        .putString(KEY_PASSWORD, user.password)
        .putString(KEY_DISPLAY_NAME, user.displayName)
        .putString(KEY_ROLE, user.role.uppercase())
        .putLong(KEY_CREATED_AT, user.createdAt)
        .putBoolean(KEY_IS_ACTIVE, user.isActive)
        .putBoolean(KEY_CAN_POS_APP, user.canUsePosApp)
        .putBoolean(KEY_CAN_KITCHEN_APP, user.canUseKitchenApp)
        .putBoolean(KEY_CAN_BILLING_APP, user.canUseBillingApp)
        .putBoolean(KEY_CAN_INVENTORY_APP, user.canUseInventoryApp)
        .putBoolean(KEY_CAN_CLOSURES_APP, user.canUseClosuresApp)
        .putBoolean(KEY_CAN_MENU_APP, user.canUseMenuApp)
        .putBoolean(KEY_CAN_SETTINGS_APP, user.canUseSettingsApp)
        .putBoolean(KEY_CAN_ACCESS_PC, user.canAccessPc)
        .putBoolean(KEY_CAN_POS_PC, user.canUsePosPc)
        .putBoolean(KEY_CAN_KITCHEN_PC, user.canUseKitchenPc)
        .putBoolean(KEY_CAN_BILLING_PC, user.canUseBillingPc)
        .putBoolean(KEY_CAN_INVENTORY_PC, user.canUseInventoryPc)
        .putBoolean(KEY_CAN_REPORTS_PC, user.canUseReportsPc)
        .apply()
      Log.i(TAG, "Sesión persistida con éxito para: ${user.username} (${user.role})")
    } catch (e: Exception) {
      Log.e(TAG, "Error persistiendo sesión de usuario", e)
    }
  }

  /**
   * Recupera la sesión guardada si existe con sus permisos. Retorna null si el usuario cerró sesión.
   */
  fun getSession(): SystemUserDoc? {
    try {
      val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
      if (!isLoggedIn) return null

      val username = prefs.getString(KEY_USERNAME, null) ?: return null
      val displayName = prefs.getString(KEY_DISPLAY_NAME, "Usuario") ?: "Usuario"
      val role = prefs.getString(KEY_ROLE, "STAFF") ?: "STAFF"
      val password = prefs.getString(KEY_PASSWORD, "") ?: ""
      val id = prefs.getString(KEY_USER_ID, username) ?: username
      val createdAt = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())
      val isActive = prefs.getBoolean(KEY_IS_ACTIVE, true)
      val isAdmin = role.equals("ADMIN", ignoreCase = true)

      return SystemUserDoc(
        id = id,
        username = username,
        password = password,
        displayName = displayName,
        role = role,
        createdAt = createdAt,
        isActive = isActive,
        canUsePosApp = prefs.getBoolean(KEY_CAN_POS_APP, true),
        canUseKitchenApp = prefs.getBoolean(KEY_CAN_KITCHEN_APP, true),
        canUseBillingApp = prefs.getBoolean(KEY_CAN_BILLING_APP, true),
        canUseInventoryApp = prefs.getBoolean(KEY_CAN_INVENTORY_APP, isAdmin),
        canUseClosuresApp = prefs.getBoolean(KEY_CAN_CLOSURES_APP, isAdmin),
        canUseMenuApp = prefs.getBoolean(KEY_CAN_MENU_APP, isAdmin),
        canUseSettingsApp = prefs.getBoolean(KEY_CAN_SETTINGS_APP, isAdmin),
        canAccessPc = prefs.getBoolean(KEY_CAN_ACCESS_PC, true),
        canUsePosPc = prefs.getBoolean(KEY_CAN_POS_PC, true),
        canUseKitchenPc = prefs.getBoolean(KEY_CAN_KITCHEN_PC, true),
        canUseBillingPc = prefs.getBoolean(KEY_CAN_BILLING_PC, true),
        canUseInventoryPc = prefs.getBoolean(KEY_CAN_INVENTORY_PC, isAdmin),
        canUseReportsPc = prefs.getBoolean(KEY_CAN_REPORTS_PC, isAdmin)
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error recuperando sesión persistida", e)
      return null
    }
  }

  /**
   * Cierra de manera explícita la sesión del usuario eliminando las credenciales guardadas.
   */
  fun clearSession() {
    try {
      prefs.edit().clear().apply()
      Log.i(TAG, "Sesión eliminada por solicitud explícita del usuario.")
    } catch (e: Exception) {
      Log.e(TAG, "Error limpiando sesión", e)
    }
  }

  fun isLoggedIn(): Boolean {
    return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
  }
}
