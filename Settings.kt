package ru.familybudget.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val apiUrl: String = "",
    val token: String = "",
    val persons: List<String> = listOf("Я"),
    val currency: String = "₽",
    val autoSync: Boolean = false
)

/** Простое хранилище настроек. URL и токен вводятся в приложении, пересобирать APK не нужно. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val flow: StateFlow<AppSettings> = state

    private fun load(): AppSettings {
        val persons = prefs.getString("persons", null)
            ?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.ifEmpty { null } ?: listOf("Я")
        return AppSettings(
            apiUrl = prefs.getString("apiUrl", "").orEmpty(),
            token = prefs.getString("token", "").orEmpty(),
            persons = persons,
            currency = prefs.getString("currency", "₽").orEmpty().ifBlank { "₽" },
            autoSync = prefs.getBoolean("autoSync", false)
        )
    }

    fun update(change: (AppSettings) -> AppSettings) {
        val next = change(state.value)
        prefs.edit()
            .putString("apiUrl", next.apiUrl)
            .putString("token", next.token)
            .putString("persons", next.persons.joinToString("\n"))
            .putString("currency", next.currency)
            .putBoolean("autoSync", next.autoSync)
            .apply()
        state.value = next
    }
}
