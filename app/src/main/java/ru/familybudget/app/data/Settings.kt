package ru.familybudget.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SavingsGoal(
    val id: Long,
    val title: String,
    val targetCents: Long,
    val savedCents: Long
)

private const val DEFAULT_API_URL =
    "https://script.google.com/macros/s/AKfycbwIJnvApZaZbpIF5C0k2zT2LPvn0GTNXVB0ffjC2BE1qDd0QI21pp057a6JQfcigDaV/exec"
private const val DEFAULT_API_TOKEN = "family-budget-secret-2026-xyz123456"

data class AppSettings(
    val apiUrl: String = DEFAULT_API_URL,
    val token: String = DEFAULT_API_TOKEN,
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
    val fixedPromptYm: String = "",
    /** Вечернее напоминание каждый день в 20:00. */
    val dailyReminder: Boolean = false,
    /** Сколько процентов от дохода откладывать на подушку безопасности (5, 10, 15, 20). */
    val savingsPercent: Int = 10,
    /** Выбранная игровая стратегия накоплений (пусто — не выбрана). */
    val activeStrategy: String = "",
    /** Финансовые цели (копилки на мечту). */
    val goals: List<SavingsGoal> = emptyList(),
    /** Пользовательские иконки категорий: название категории → ключ иконки. */
    val categoryIcons: Map<String, String> = emptyMap(),
    /** Использовать размытые обои рабочего стола как фон приложения. */
    val wallpaperBackground: Boolean = false,
    /** Режим приватности: скрывать суммы на главном экране. */
    val hideAmounts: Boolean = false,
    /** Прозрачность стеклянных карточек (0.20f .. 0.85f). */
    val glassOpacity: Float = 0.40f,
    /** Сила размытия обоев рабочего стола на Android 12+ (0 .. 120). */
    val blurRadius: Int = 48,
    /** Акцентная цветовая тема: teal, emerald, indigo, graphite. */
    val accentTheme: String = "teal",
    /** Отдельная сумма финансовой подушки безопасности (в копейках, не зависит от вкладов). */
    val cushionCents: Long = 0L,
    // Умные локальные уведомления
    val notifMorningEnabled: Boolean = false,
    val notifMorningHour: Int = 9,
    val notifEveningEnabled: Boolean = false,
    val notifEveningHour: Int = 21,
    val notifBillsEnabled: Boolean = true,
    val notifBillsHour: Int = 10,
    val notifBillsDaysBefore: Int = 1,
    val notifLimitEnabled: Boolean = true,
    val notifLimitHour: Int = 18,
    val notifWeeklyEnabled: Boolean = false,
    val notifWeeklyDay: Int = 7, // 1=Пн .. 7=Вс
    val notifWeeklyHour: Int = 19,
    val notifCushionEnabled: Boolean = false,
    val notifCushionDay: Int = 1, // 1..28
    val notifCushionHour: Int = 11
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
            apiUrl = prefs.getString("apiUrl", null).orEmpty().ifBlank { DEFAULT_API_URL },
            token = prefs.getString("token", null).orEmpty().ifBlank { DEFAULT_API_TOKEN },
            persons = persons,
            currency = prefs.getString("currency", "₽").orEmpty().ifBlank { "₽" },
            autoSync = prefs.getBoolean("autoSync", false),
            userName = prefs.getString("userName", "").orEmpty(),
            onboarded = prefs.getBoolean("onboarded", false),
            monthlyIncome = prefs.getLong("monthlyIncome", 0L),
            monthlyBudget = prefs.getLong("monthlyBudget", 0L),
            hideTips = prefs.getBoolean("hideTips", false),
            fixedPromptYm = prefs.getString("fixedPromptYm", "").orEmpty(),
            dailyReminder = prefs.getBoolean("dailyReminder", false),
            savingsPercent = prefs.getInt("savingsPercent", 10),
            activeStrategy = prefs.getString("activeStrategy", "").orEmpty(),
            goals = parseGoals(prefs.getString("goals_json", "").orEmpty()),
            categoryIcons = parseIcons(prefs.getString("category_icons_json", "").orEmpty()),
            wallpaperBackground = prefs.getBoolean("wallpaperBackground", false),
            hideAmounts = prefs.getBoolean("hideAmounts", false),
            glassOpacity = prefs.getFloat("glassOpacity", 0.40f).coerceIn(0.20f, 0.85f),
            blurRadius = prefs.getInt("blurRadius", 48).coerceIn(0, 120),
            accentTheme = prefs.getString("accentTheme", "teal").orEmpty().ifBlank { "teal" },
            cushionCents = prefs.getLong("cushionCents", 0L).coerceAtLeast(0L),
            notifMorningEnabled = prefs.getBoolean("notifMorningEnabled", false),
            notifMorningHour = prefs.getInt("notifMorningHour", 9).coerceIn(0, 23),
            notifEveningEnabled = prefs.getBoolean("notifEveningEnabled", false),
            notifEveningHour = prefs.getInt("notifEveningHour", 21).coerceIn(0, 23),
            notifBillsEnabled = prefs.getBoolean("notifBillsEnabled", true),
            notifBillsHour = prefs.getInt("notifBillsHour", 10).coerceIn(0, 23),
            notifBillsDaysBefore = prefs.getInt("notifBillsDaysBefore", 1).coerceIn(0, 7),
            notifLimitEnabled = prefs.getBoolean("notifLimitEnabled", true),
            notifLimitHour = prefs.getInt("notifLimitHour", 18).coerceIn(0, 23),
            notifWeeklyEnabled = prefs.getBoolean("notifWeeklyEnabled", false),
            notifWeeklyDay = prefs.getInt("notifWeeklyDay", 7).coerceIn(1, 7),
            notifWeeklyHour = prefs.getInt("notifWeeklyHour", 19).coerceIn(0, 23),
            notifCushionEnabled = prefs.getBoolean("notifCushionEnabled", false),
            notifCushionDay = prefs.getInt("notifCushionDay", 1).coerceIn(1, 28),
            notifCushionHour = prefs.getInt("notifCushionHour", 11).coerceIn(0, 23)
        )
    }

    private fun parseIcons(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = HashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeIcons(icons: Map<String, String>): String {
        val obj = JSONObject()
        icons.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun parseGoals(raw: String): List<SavingsGoal> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val title = o.optString("title").trim()
                if (title.isEmpty()) return@mapNotNull null
                SavingsGoal(
                    id = o.optLong("id", i.toLong() + 1L),
                    title = title,
                    targetCents = o.optLong("targetCents", 0L).coerceAtLeast(0L),
                    savedCents = o.optLong("savedCents", 0L).coerceAtLeast(0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeGoals(goals: List<SavingsGoal>): String {
        val arr = JSONArray()
        goals.forEach { g ->
            arr.put(
                JSONObject()
                    .put("id", g.id)
                    .put("title", g.title)
                    .put("targetCents", g.targetCents)
                    .put("savedCents", g.savedCents)
            )
        }
        return arr.toString()
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
            .putBoolean("dailyReminder", next.dailyReminder)
            .putInt("savingsPercent", next.savingsPercent)
            .putString("activeStrategy", next.activeStrategy)
            .putString("goals_json", serializeGoals(next.goals))
            .putString("category_icons_json", serializeIcons(next.categoryIcons))
            .putBoolean("wallpaperBackground", next.wallpaperBackground)
            .putBoolean("hideAmounts", next.hideAmounts)
            .putFloat("glassOpacity", next.glassOpacity)
            .putInt("blurRadius", next.blurRadius)
            .putString("accentTheme", next.accentTheme)
            .putLong("cushionCents", next.cushionCents)
            .putBoolean("notifMorningEnabled", next.notifMorningEnabled)
            .putInt("notifMorningHour", next.notifMorningHour.coerceIn(0, 23))
            .putBoolean("notifEveningEnabled", next.notifEveningEnabled)
            .putInt("notifEveningHour", next.notifEveningHour.coerceIn(0, 23))
            .putBoolean("notifBillsEnabled", next.notifBillsEnabled)
            .putInt("notifBillsHour", next.notifBillsHour.coerceIn(0, 23))
            .putInt("notifBillsDaysBefore", next.notifBillsDaysBefore.coerceIn(0, 7))
            .putBoolean("notifLimitEnabled", next.notifLimitEnabled)
            .putInt("notifLimitHour", next.notifLimitHour.coerceIn(0, 23))
            .putBoolean("notifWeeklyEnabled", next.notifWeeklyEnabled)
            .putInt("notifWeeklyDay", next.notifWeeklyDay.coerceIn(1, 7))
            .putInt("notifWeeklyHour", next.notifWeeklyHour.coerceIn(0, 23))
            .putBoolean("notifCushionEnabled", next.notifCushionEnabled)
            .putInt("notifCushionDay", next.notifCushionDay.coerceIn(1, 28))
            .putInt("notifCushionHour", next.notifCushionHour.coerceIn(0, 23))
            .apply()
        state.value = next
    }
}
