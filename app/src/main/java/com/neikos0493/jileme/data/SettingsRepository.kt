package com.neikos0493.jileme.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable

private val Context.settingsStore by preferencesDataStore("settings")

@Serializable
data class AppSettings(
    val monthlyBudgetMinor: Long = 0,
    val autoAddNotifications: Boolean = false,
    val themeMode: String = "SYSTEM",
    val lockType: String = "NONE",
    val gestureHash: String = "",
    val lastUpdateCheck: Long = 0,
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            p[Keys.BUDGET] ?: 0,
            p[Keys.AUTO_ADD] ?: false,
            p[Keys.THEME] ?: "SYSTEM",
            p[Keys.LOCK] ?: "NONE",
            p[Keys.GESTURE] ?: "",
            p[Keys.LAST_UPDATE] ?: 0,
        )
    }
    suspend fun setBudget(value: Long) = context.settingsStore.edit { it[Keys.BUDGET]=value }
    suspend fun setAutoAdd(value: Boolean) = context.settingsStore.edit { it[Keys.AUTO_ADD]=value }
    suspend fun setTheme(value: String) = context.settingsStore.edit { it[Keys.THEME]=value }
    suspend fun setLock(type: String, gestureHash: String="") = context.settingsStore.edit { it[Keys.LOCK]=type; it[Keys.GESTURE]=gestureHash }
    suspend fun setLastUpdateCheck(value: Long) = context.settingsStore.edit { it[Keys.LAST_UPDATE]=value }
    suspend fun export(): Map<String,String> {
        val s=settings.firstOrNull() ?: AppSettings()
        return mapOf("monthlyBudgetMinor" to s.monthlyBudgetMinor.toString(),"autoAddNotifications" to s.autoAddNotifications.toString(),"themeMode" to s.themeMode,"lockType" to s.lockType,"gestureHash" to s.gestureHash)
    }
    suspend fun import(values: Map<String,String>) {
        context.settingsStore.edit { p ->
            values["monthlyBudgetMinor"]?.toLongOrNull()?.let { p[Keys.BUDGET]=it }
            values["autoAddNotifications"]?.toBooleanStrictOrNull()?.let { p[Keys.AUTO_ADD]=it }
            values["themeMode"]?.let { p[Keys.THEME]=it }
            values["lockType"]?.let { p[Keys.LOCK]=it }
            values["gestureHash"]?.let { p[Keys.GESTURE]=it }
        }
    }
    private object Keys {
        val BUDGET=longPreferencesKey("monthly_budget")
        val AUTO_ADD=booleanPreferencesKey("auto_add")
        val THEME=stringPreferencesKey("theme")
        val LOCK=stringPreferencesKey("lock")
        val GESTURE=stringPreferencesKey("gesture")
        val LAST_UPDATE=longPreferencesKey("last_update")
    }
}

