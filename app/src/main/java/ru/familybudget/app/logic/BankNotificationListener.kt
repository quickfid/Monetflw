package ru.familybudget.app.logic

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.familybudget.app.data.BudgetDb
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import ru.familybudget.app.data.SettingsStore
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Фоновый слушатель Push-уведомлений банков.
 * Оптимизирован под минимальное энергопотребление:
 * - мгновенно отсеивает чужие пакеты, групповые/постоянные уведомления и выключенный режим
 *   через один быстрый boolean-флаг в SharedPreferences без запуска корутин и без парсинга JSON;
 * - очищает кеш дедупликации и корректно освобождает CoroutineScope в onDestroy().
 */
class BankNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val recentKeys = ConcurrentHashMap<String, Long>()
        private const val DEDUP_WINDOW_MS = 30_000L
        private const val PREFS_NAME = "settings"
        private const val KEY_AUTO_TRACK = "bankAutoTrackEnabled"
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // 1. Быстрая проверка флага без создания SettingsStore и без парсинга JSON
        val appCtx = applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_AUTO_TRACK, false)) return

        // 2. Отсеиваем постоянные сервисные и групповые summary-уведомления
        val notif = sbn.notification ?: return
        val ignoredFlags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_GROUP_SUMMARY
        if ((notif.flags and ignoredFlags) != 0) return

        // 3. Проверяем пакет банка (O(1) по HashSet)
        val pkg = sbn.packageName ?: return
        if (!BankPushParser.isSupportedBank(pkg)) return

        val extras = notif.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // 4. Предкомпилированный парсер (без запуска IO-корутины, если это не оплата)
        val parsed = BankPushParser.parsePayment(title, text) ?: return

        // 5. Защита от дублей + периодическая очистка кеша
        val now = System.currentTimeMillis()
        if (recentKeys.size > 32) {
            recentKeys.entries.removeIf { now - it.value >= DEDUP_WINDOW_MS }
        }
        val dedupKey = "$pkg|${parsed.amountCents}|${parsed.merchant}"
        val prev = recentKeys[dedupKey]
        if (prev != null && now - prev < DEDUP_WINDOW_MS) return
        recentKeys[dedupKey] = now

        scope.launch {
            try {
                val settings = SettingsStore(appCtx).flow.value
                val dao = BudgetDb.get(appCtx).dao()
                val primaryAccount = dao.accountsOnce().firstOrNull()?.name.orEmpty()
                val primaryPerson = settings.persons.firstOrNull().orEmpty()
                dao.insertOperation(
                    Operation(
                        date = LocalDate.now().toEpochDay(),
                        amountCents = parsed.amountCents,
                        type = OpType.EXPENSE,
                        category = parsed.category,
                        comment = "${parsed.merchant} (авто)",
                        person = primaryPerson,
                        account = primaryAccount
                    )
                )
                ReminderScheduler.show(
                    appCtx,
                    2100 + (now % 500).toInt(),
                    "💳 Оплата записана автоматически",
                    "−${Fmt.money(parsed.amountCents, settings.currency, false)} · ${parsed.category} (${parsed.merchant})"
                )
            } catch (_: Exception) {
                // Игнорируем единичные ошибки фоновой записи
            }
        }
    }
}
