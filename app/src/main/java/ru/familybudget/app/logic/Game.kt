package ru.familybudget.app.logic

import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import ru.familybudget.app.data.PlanItem
import java.time.LocalDate
import java.time.YearMonth

data class Badge(val emoji: String, val title: String, val unlocked: Boolean)

data class GameState(
    val xp: Int,
    val level: Int,
    val title: String,
    val avatar: String,
    val progress: Float,
    val xpToNext: Int,
    val streak: Int,
    val badges: List<Badge>
)

/** Игровой слой: опыт, уровни, серия дней и награды. Всё считается из данных, ничего отдельно не хранится. */
object Game {
    private const val XP_PER_LEVEL = 300

    private val titles = listOf("Новичок", "Копилка", "Экономист", "Финансист", "Мастер бюджета", "Легенда")
    private val avatars = listOf("🐣", "🐷", "🦊", "🦉", "🐉", "👑")

    fun compute(ops: List<Operation>, plans: List<PlanItem>, mandatory: List<Mandatory>, today: LocalDate): GameState {
        val paidOps = ops.count { it.mandatoryId != null }
        val planMonths = plans.map { it.ym }.distinct().size
        val days = ops.map { it.date }.distinct().sorted()
        val daySet = days.toHashSet()
        val todayDay = today.toEpochDay()

        var streak = 0
        var cursor = if (daySet.contains(todayDay)) todayDay else todayDay - 1
        while (daySet.contains(cursor)) {
            streak++
            cursor--
        }
        var best = 0
        var run = 0
        var prev = Long.MIN_VALUE
        for (d in days) {
            run = if (d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }

        val xp = ops.size * 10 + paidOps * 15 + planMonths * 100 + streak * 5
        val level = xp / XP_PER_LEVEL + 1
        val idx = (level - 1).coerceAtMost(titles.size - 1)

        // Месяцы, которые уложились в план / дали накопления
        val currentKey = YearMonth.from(today).toString()
        val expenseByMonth = HashMap<String, Long>()
        val incomeByMonth = HashMap<String, Long>()
        for (o in ops) {
            val k = YearMonth.from(LocalDate.ofEpochDay(o.date)).toString()
            if (o.type == OpType.EXPENSE) expenseByMonth[k] = (expenseByMonth[k] ?: 0L) + o.amountCents
            if (o.type == OpType.INCOME) incomeByMonth[k] = (incomeByMonth[k] ?: 0L) + o.amountCents
        }
        val planTotals = plans.groupBy { it.ym }.mapValues { e -> e.value.sumOf { it.amountCents } }
        val keptPlan = planTotals.any { (k, total) ->
            k < currentKey && total > 0 && (expenseByMonth[k] ?: 0L) > 0 && (expenseByMonth[k] ?: 0L) <= total
        }
        val saver = incomeByMonth.any { (k, income) ->
            income > 0 && (expenseByMonth[k] ?: 0L) > 0 && (expenseByMonth[k] ?: 0L) * 10 <= income * 9
        }

        val badges = listOf(
            Badge("🌱", "Первый шаг", ops.isNotEmpty()),
            Badge("🔥", "3 дня подряд", best >= 3),
            Badge("⚡", "Неделя подряд", best >= 7),
            Badge("📋", "План месяца", planMonths >= 1),
            Badge("🧾", "50 записей", ops.size >= 50),
            Badge("💯", "100 записей", ops.size >= 100),
            Badge("✅", "Платежи в срок", paidOps >= 3),
            Badge("🎯", "В рамках плана", keptPlan),
            Badge("🐷", "Накопитель", saver)
        )
        return GameState(
            xp = xp,
            level = level,
            title = titles[idx],
            avatar = avatars[idx],
            progress = (xp % XP_PER_LEVEL).toFloat() / XP_PER_LEVEL,
            xpToNext = XP_PER_LEVEL - xp % XP_PER_LEVEL,
            streak = streak,
            badges = badges
        )
    }
}
