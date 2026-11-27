package ru.familybudget.app.logic

import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import ru.familybudget.app.data.PlanItem
import java.time.LocalDate
import java.time.YearMonth

data class Badge(val emoji: String, val title: String, val unlocked: Boolean, val hint: String = "")

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

    private val titles = listOf(
        "Новичок", "Копилка", "Экономист", "Финансист",
        "Мастер бюджета", "Гроссмейстер", "Финансовый магнат", "Легенда"
    )
    private val avatars = listOf("🐣", "🐷", "🦊", "🦉", "🐉", "🦁", "💎", "👑")

    fun compute(ops: List<Operation>, plans: List<PlanItem>, mandatory: List<Mandatory>, today: LocalDate): GameState {
        val paidOps = ops.filter { it.mandatoryId != null }
            .map { YearMonth.from(LocalDate.ofEpochDay(it.date)) to it.mandatoryId }
            .distinct()
            .size
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

        // Месяцы, которые уложились в план / дали накопления
        val currentKey = YearMonth.from(today).toString()
        val expenseByMonth = HashMap<String, Long>()
        val incomeByMonth = HashMap<String, Long>()
        val expenseDays = HashSet<Long>()
        val expenseCategories = HashSet<String>()
        val persons = HashSet<String>()
        var withNotes = 0
        var hasIncome = false

        for (o in ops) {
            val k = YearMonth.from(LocalDate.ofEpochDay(o.date)).toString()
            if (o.type == OpType.EXPENSE) {
                expenseByMonth[k] = (expenseByMonth[k] ?: 0L) + o.amountCents
                expenseDays.add(o.date)
                if (o.category.isNotBlank()) expenseCategories.add(o.category)
            }
            if (o.type == OpType.INCOME) {
                incomeByMonth[k] = (incomeByMonth[k] ?: 0L) + o.amountCents
                hasIncome = true
            }
            if (o.note.isNotBlank()) withNotes++
            if (o.person.isNotBlank()) persons.add(o.person)
        }
        val planTotals = plans.groupBy { it.ym }.mapValues { e -> e.value.sumOf { it.amountCents } }
        val keptPlan = planTotals.any { (k, total) ->
            k < currentKey && total > 0 && (expenseByMonth[k] ?: 0L) > 0 && (expenseByMonth[k] ?: 0L) <= total
        }
        val saver = incomeByMonth.any { (k, income) ->
            income > 0 && (expenseByMonth[k] ?: 0L) > 0 && (expenseByMonth[k] ?: 0L) * 10 <= income * 9
        }
        val superSaver = incomeByMonth.any { (k, income) ->
            income > 0 && (expenseByMonth[k] ?: 0L) > 0 && (expenseByMonth[k] ?: 0L) * 4 <= income * 3
        }
        val firstDay = days.firstOrNull()
        val zeroSpendDay = firstDay != null && firstDay < todayDay &&
            (firstDay until todayDay).any { d -> !expenseDays.contains(d) }

        val badges = listOf(
            Badge("🌱", "Первый шаг", ops.isNotEmpty(), "Запишите свою первую операцию"),
            Badge("💰", "Первый доход", hasIncome, "Добавьте первый доход в бюджет"),
            Badge("🔥", "3 дня подряд", best >= 3, "Ведите учёт 3 дня без перерыва"),
            Badge("⚡", "Неделя подряд", best >= 7, "Ведите учёт 7 дней подряд"),
            Badge("🏆", "Железная привычка", best >= 14, "Серия из 14 дней подряд"),
            Badge("💎", "Месяц дисциплины", best >= 30, "Серия из 30 дней подряд"),
            Badge("📋", "План месяца", planMonths >= 1, "Составьте первый план бюджета"),
            Badge("📅", "Стратег", planMonths >= 3, "Запланируйте бюджет на 3 разных месяца"),
            Badge("🧘", "День без трат", zeroSpendDay, "Проведите хотя бы 1 день без единого расхода"),
            Badge("🛒", "Разнообразие", expenseCategories.size >= 5, "Используйте 5 разных категорий расходов"),
            Badge("✍️", "Внимание к деталям", withNotes >= 10, "Добавьте заметку к 10 операциям"),
            Badge("🧾", "50 записей", ops.size >= 50, "Зафиксируйте 50 операций"),
            Badge("💯", "100 записей", ops.size >= 100, "Зафиксируйте 100 операций"),
            Badge("🚀", "Профи учёта", ops.size >= 250, "Зафиксируйте 250 операций"),
            Badge("✅", "Платежи в срок", paidOps >= 3, "Отметьте 3 обязательных платежа"),
            Badge("🛡️", "Без просрочек", paidOps >= 10, "Отметьте 10 обязательных платежей"),
            Badge("🎯", "В рамках плана", keptPlan, "Закройте месяц без превышения плана"),
            Badge("🐷", "Накопитель", saver, "Отложите от 10% дохода за месяц"),
            Badge("👑", "Супер-копилка", superSaver, "Отложите от 25% дохода за месяц"),
            Badge("👨‍👩‍👧", "Командная работа", persons.size >= 2, "Добавьте операции от 2 и более участников")
        )

        val badgeXp = badges.count { it.unlocked } * 25
        val xp = ops.size * 10 + paidOps * 15 + planMonths * 100 + streak * 5 + badgeXp
        val level = xp / XP_PER_LEVEL + 1
        val idx = (level - 1).coerceAtMost(titles.size - 1)

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
