package ru.familybudget.app.logic

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.familybudget.app.data.BudgetDb
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import ru.familybudget.app.data.SettingsStore
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Фоновый слушатель Push-уведомлений банков.
 * Работает 100% локально на устройстве: разбирает только оплаты/покупки, игнорирует переводы.
 */
class BankNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val recentKeys = ConcurrentHashMap<String, Long>()
        private const val DEDUP_WINDOW_MS = 30_000L
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return
        if (!BankPushParser.isSupportedBank(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val appCtx = applicationContext
        val settings = SettingsStore(appCtx).flow.value
        if (!settings.bankAutoTrackEnabled) return

        val parsed = BankPushParser.parsePayment(title, text) ?: return

        // Защита от дублей при обновлении одного и того же уведомления
        val now = System.currentTimeMillis()
        val dedupKey = "$pkg|${parsed.amountCents}|${parsed.merchant}"
        val prev = recentKeys[dedupKey]
        if (prev != null && now - prev < DEDUP_WINDOW_MS) return
        recentKeys[dedupKey] = now

        scope.launch {
            try {
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
