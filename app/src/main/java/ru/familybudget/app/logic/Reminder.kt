package ru.familybudget.app.logic

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.familybudget.app.MainActivity
import ru.familybudget.app.R
import ru.familybudget.app.data.AppSettings
import ru.familybudget.app.data.BudgetDb
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.SettingsStore
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar

private const val CHANNEL_ID = "family_budget_smart_alerts"
private const val EXTRA_TYPE = "notif_type"

object NotifType {
    const val MORNING = "morning"
    const val EVENING = "evening"
    const val BILLS = "bills"
    const val LIMIT = "limit"
    const val WEEKLY = "weekly"
    const val CUSHION = "cushion"

    fun requestCode(type: String): Int = when (type) {
        MORNING -> 2001
        EVENING -> 2002
        BILLS -> 2003
        LIMIT -> 2004
        WEEKLY -> 2005
        CUSHION -> 2006
        else -> 2000
    }
}

/** Умный приёмник локальных уведомлений (все 6 типов + привязка к окну дат обязательных платежей). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val type = intent?.getStringExtra(EXTRA_TYPE) ?: NotifType.EVENING
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val settings = SettingsStore(appCtx).flow.value
                val dao = BudgetDb.get(appCtx).dao()
                val today = LocalDate.now()
                val ym = YearMonth.from(today)
                val cur = settings.currency

                when (type) {
                    NotifType.MORNING -> if (settings.notifMorningEnabled) {
                        val mandatory = dao.mandatoryOnce()
                        val ops = dao.operationsOnce()
                        val monthOps = ops.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == ym }
                        val paidIds = monthOps.mapNotNull { it.mandatoryId }.toSet()
                        val dueToday = mandatory.filter { m ->
                            m.id !in paidIds && isDayInWindow(today.dayOfMonth, m.day, m.endDay)
                        }
                        val text = if (dueToday.isNotEmpty()) {
                            val names = dueToday.joinToString(", ") { "${it.name} (${Fmt.money(it.amountCents, cur, false)})" }
                            "Сегодня окно оплаты: $names"
                        } else {
                            "Доброе утро! Проверьте свободный лимит на сегодня и держите бюджет под контролем."
                        }
                        ReminderScheduler.show(appCtx, NotifType.requestCode(type), "🌅 Утренний брифинг", text)
                    }

                    NotifType.EVENING -> if (settings.notifEveningEnabled || settings.dailyReminder) {
                        val ops = dao.operationsOnce()
                        val todayEpoch = today.toEpochDay()
                        val hasToday = ops.any { it.date == todayEpoch }
                        if (!hasToday) {
                            ReminderScheduler.show(
                                appCtx,
                                NotifType.requestCode(type),
                                "🌙 Запишите траты за сегодня",
                                "Вы ещё не внесли операции за сегодня — сохраните серию 🔥 (+10 XP)"
                            )
                        }
                    }

                    NotifType.BILLS -> if (settings.notifBillsEnabled) {
                        val mandatory = dao.mandatoryOnce()
                        val ops = dao.operationsOnce()
                        val monthOps = ops.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == ym }
                        val paidIds = monthOps.mapNotNull { it.mandatoryId }.toSet()
                        val lead = settings.notifBillsDaysBefore
                        val upcoming = mandatory.filter { m ->
                            if (m.id in paidIds) return@filter false
                            val inWindow = isDayInWindow(today.dayOfMonth, m.day, m.endDay)
                            val diff = m.day - today.dayOfMonth
                            inWindow || (diff in 0..lead)
                        }
                        if (upcoming.isNotEmpty()) {
                            val lines = upcoming.joinToString("; ") { m ->
                                val windowStr = if (m.endDay > m.day) "с ${m.day} по ${m.endDay}" else "до ${m.day}"
                                "${m.name} — ${Fmt.money(m.amountCents, cur, false)} ($windowStr)"
                            }
                            ReminderScheduler.show(
                                appCtx,
                                NotifType.requestCode(type),
                                "📅 Обязательные платежи",
                                lines
                            )
                        }
                    }

                    NotifType.LIMIT -> if (settings.notifLimitEnabled) {
                        val categories = dao.categoriesOnce().filter { !it.isIncome && it.limitCents > 0L }
                        val ops = dao.operationsOnce()
                        val monthExpenses = ops.filter {
                            it.type == OpType.EXPENSE && YearMonth.from(LocalDate.ofEpochDay(it.date)) == ym
                        }
                        val warned = categories.mapNotNull { c ->
                            val spent = monthExpenses.filter { it.category == c.name }.sumOf { it.amountCents }
                            val pct = (spent * 100 / c.limitCents).toInt()
                            if (pct >= 85) "${c.name}: $pct%" else null
                        }
                        if (warned.isNotEmpty()) {
                            ReminderScheduler.show(
                                appCtx,
                                NotifType.requestCode(type),
                                "⚠️ Приближение к лимиту",
                                warned.joinToString(" · ")
                            )
                        }
                    }

                    NotifType.WEEKLY -> if (settings.notifWeeklyEnabled && today.dayOfWeek.value == settings.notifWeeklyDay) {
                        val weekAgo = today.minusDays(6).toEpochDay()
                        val ops = dao.operationsOnce().filter { it.type == OpType.EXPENSE && it.date >= weekAgo }
                        val total = ops.sumOf { it.amountCents }
                        val topCat = ops.groupBy { it.category }.maxByOrNull { e -> e.value.sumOf { it.amountCents } }?.key
                        val body = if (topCat != null) {
                            "За 7 дней потрачено ${Fmt.money(total, cur, false)}. Топ-категория: $topCat"
                        } else {
                            "За последние 7 дней расходов не было — отличная экономия!"
                        }
                        ReminderScheduler.show(appCtx, NotifType.requestCode(type), "📊 Итоги недели", body)
                    }

                    NotifType.CUSHION -> if (settings.notifCushionEnabled && today.dayOfMonth == settings.notifCushionDay) {
                        ReminderScheduler.show(
                            appCtx,
                            NotifType.requestCode(type),
                            "🛡️ Финансовая подушка",
                            "Пора пополнить финансовую подушку (сейчас ${Fmt.money(settings.cushionCents, cur, false)}) или перенести свободный остаток!"
                        )
                    }
                }

                // Перепланируем все активные уведомления на следующий цикл
                ReminderScheduler.rescheduleAll(appCtx, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isDayInWindow(dayOfMonth: Int, startDay: Int, endDay: Int): Boolean {
        return if (endDay > startDay) dayOfMonth in startDay..endDay else dayOfMonth == startDay
    }
}

/** Восстанавливает все включённые уведомления после перезагрузки устройства. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val appCtx = context.applicationContext
            val settings = SettingsStore(appCtx).flow.value
            ReminderScheduler.rescheduleAll(appCtx, settings)
        }
    }
}

object ReminderScheduler {
    fun rescheduleAll(context: Context, s: AppSettings) {
        scheduleOrCancel(context, NotifType.MORNING, s.notifMorningEnabled, s.notifMorningHour)
        scheduleOrCancel(context, NotifType.EVENING, s.notifEveningEnabled || s.dailyReminder, s.notifEveningHour)
        scheduleOrCancel(context, NotifType.BILLS, s.notifBillsEnabled, s.notifBillsHour)
        scheduleOrCancel(context, NotifType.LIMIT, s.notifLimitEnabled, s.notifLimitHour)
        scheduleOrCancel(context, NotifType.WEEKLY, s.notifWeeklyEnabled, s.notifWeeklyHour)
        scheduleOrCancel(context, NotifType.CUSHION, s.notifCushionEnabled, s.notifCushionHour)
    }

    fun scheduleDaily20(context: Context) {
        scheduleOrCancel(context, NotifType.EVENING, true, 20)
    }

    fun cancel(context: Context) {
        scheduleOrCancel(context, NotifType.EVENING, false, 20)
    }

    fun sendTest(context: Context) {
        show(
            context,
            2099,
            "🔔 Уведомления настроены!",
            "Умные напоминания «Семейного бюджета» работают корректно."
        )
    }

    private fun scheduleOrCancel(context: Context, type: String, enabled: Boolean, hour: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, type)
        if (!enabled) {
            am.cancel(pi)
            return
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    internal fun show(context: Context, notifId: Int, title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Умные уведомления бюджета",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Утренний брифинг, обязательные платежи, лимиты, итоги недели и подушка"
            }
            nm.createNotificationChannel(channel)
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId, notification)
    }

    private fun pendingIntent(context: Context, type: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_TYPE, type)
        return PendingIntent.getBroadcast(
            context,
            NotifType.requestCode(type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
