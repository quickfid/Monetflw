package ru.familybudget.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AppSettings(
    val apiUrl: String = "",
    val token: String = "",
    val persons: List<String> = listOf("Я"),
    val currency: String = "₽",
    val autoSync: Boolean = false,
    val userName: String = "",
    val onboarded: Boolean = false,
    /** Ожидаемый ежемесячный доход, в копейках. */
    val monthlyIncome: Long = 0,
    /** План трат на месяц, в копейках (доход минус желаемые накопления). */
    val monthlyBudget: Long = 0,
    val hideTips: Boolean = false,
    /** Месяц «ГГГГ-ММ», для которого уже спросили суммы фиксированных платежей. */
    val fixedPromptYm: String = ""
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
            autoSync = prefs.getBoolean("autoSync", false),
            userName = prefs.getString("userName", "").orEmpty(),
            onboarded = prefs.getBoolean("onboarded", false),
            monthlyIncome = prefs.getLong("monthlyIncome", 0L),
            monthlyBudget = prefs.getLong("monthlyBudget", 0L),
            hideTips = prefs.getBoolean("hideTips", false),
            fixedPromptYm = prefs.getString("fixedPromptYm", "").orEmpty()
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
            .putString("userName", next.userName)
            .putBoolean("onboarded", next.onboarded)
            .putLong("monthlyIncome", next.monthlyIncome)
            .putLong("monthlyBudget", next.monthlyBudget)
            .putBoolean("hideTips", next.hideTips)
            .putString("fixedPromptYm", next.fixedPromptYm)
            .apply()
        state.value = next
    }
}
