package ru.familybudget.app.logic

import ru.familybudget.app.data.Account
import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import java.time.LocalDate
import java.time.YearMonth

/** Влияние операции на общий баланс (переводы между своими счетами баланс не меняют). */
fun Operation.delta(): Long = when (type) {
    OpType.INCOME -> amountCents
    OpType.EXPENSE -> -amountCents
    else -> 0L
}

data class MonthSummary(
    val opening: Long,
    val closing: Long,
    val income: Long,
    val expense: Long,
    val mandatoryTotal: Long,
    val mandatoryUnpaid: Long
)

data class CatStat(val name: String, val sum: Long, val count: Int, val share: Float)

enum class Verdict { GOOD, WARN, BAD, NONE }

data class Forecast(
    val isCurrent: Boolean,
    val isPast: Boolean,
    val daysInMonth: Int,
    /** Сколько дней месяца уже прошло (для текущего месяца — включая сегодня). */
    val elapsed: Int,
    val daysLeft: Int,
    val spent: Long,
    val projectedExpense: Long,
    val projectedBalance: Long,
    val budget: Long,
    /** Сколько можно тратить в день, чтобы уложиться в план. */
    val safeDaily: Long,
    val verdict: Verdict,
    /** Прогноз становится надёжнее после первых дней месяца. */
    val reliable: Boolean,
    /** Накопленные расходы по дням, от 1-го числа до сегодняшнего дня. */
    val cumulative: List<Long>
)

object Stats {
    fun elapsedDays(ym: YearMonth, today: LocalDate): Int {
        val current = YearMonth.from(today)
        return when {
            ym == current -> today.dayOfMonth
            ym.isBefore(current) -> ym.lengthOfMonth()
            else -> 0
        }
    }

    /**
     * Прогноз трат на конец месяца: обычные траты растягиваются на весь месяц по среднему за день,
     * обязательные платежи считаются отдельно (оплаченные + ещё не оплаченные).
     */
    fun forecast(
        monthOps: List<Operation>,
        ym: YearMonth,
        today: LocalDate,
        summary: MonthSummary,
        budget: Long,
        expectedIncome: Long,
        limits: Map<String, Long>,
        mandatory: List<Mandatory>
    ): Forecast {
        val current = YearMonth.from(today)
        val days = ym.lengthOfMonth()
        val isCurrent = ym == current
        val isPast = ym.isBefore(current)
        val elapsed = elapsedDays(ym, today)
        val expenses = monthOps.filter { it.type == OpType.EXPENSE }

        // Обычные траты растягиваем на весь месяц по среднему за день.
        val variable = expenses.filter { !Fmt.isBill(it.category) && it.mandatoryId == null }.sumOf { it.amountCents }
        val projectedVariable = when {
            isCurrent && elapsed > 0 -> variable * days / elapsed
            isPast -> variable
            else -> 0L
        }

        // Фиксированные платежи (аренда, кредит, связь, коммунальные) НЕ растягиваем:
        // за месяц это либо сумма из плана, либо фактическая трата, если она больше.
        val spentBills = expenses.filter { Fmt.isBill(it.category) }
            .groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amountCents } }
        val planned = HashMap<String, Long>()
        mandatory.forEach { m ->
            val key = m.category.ifBlank { m.name }
            if (Fmt.isBill(key)) planned[key] = m.amountCents
        }
        limits.forEach { (category, amount) -> if (Fmt.isBill(category)) planned[category] = amount }
        var billsExpected = 0L
        var billsRemaining = 0L
        for (c in planned.keys + spentBills.keys) {
            val spent = spentBills[c] ?: 0L
            val plan = planned[c] ?: 0L
            billsExpected += if (isPast) spent else maxOf(spent, plan)
            if (!isPast) billsRemaining += maxOf(0L, plan - spent)
        }

        // Прочие обязательные платежи с галочкой (например, спортзал).
        val paid = monthOps.mapNotNull { it.mandatoryId }.toSet()
        val otherPaid = expenses.filter { !Fmt.isBill(it.category) && it.mandatoryId != null }.sumOf { it.amountCents }
        val otherUnpaid = if (isPast) 0L else mandatory
            .filter { !Fmt.isBill(it.category.ifBlank { it.name }) && it.id !in paid }
            .sumOf { it.amountCents }
        billsRemaining += otherUnpaid

        val projected = projectedVariable + billsExpected + otherPaid + otherUnpaid
        val incomeTotal = if (isPast) summary.income else maxOf(summary.income, expectedIncome)
        val projectedBalance = summary.opening + incomeTotal - projected
        val daysLeft = if (isCurrent) days - elapsed + 1 else 0
        val safeDaily = if (isCurrent && daysLeft > 0) {
            val pool = if (budget > 0) budget - summary.expense - billsRemaining
            else summary.opening + incomeTotal - summary.expense - billsRemaining
            (pool / daysLeft).coerceAtLeast(0L)
        } else 0L
        val verdict = when {
            !isCurrent && !isPast -> Verdict.NONE
            budget > 0 -> {
                if (projected * 100 <= budget * 95) Verdict.GOOD
                else if (projected * 100 <= budget * 105) Verdict.WARN
                else Verdict.BAD
            }
            else -> if (projectedBalance >= 0) Verdict.GOOD else Verdict.BAD
        }
        val daily = dailyExpenses(monthOps, ym)
        val cumulative = ArrayList<Long>()
        var running = 0L
        for (i in 0 until elapsed.coerceAtMost(days)) {
            running += daily[i]
            cumulative.add(running)
        }
        return Forecast(
            isCurrent = isCurrent, isPast = isPast, daysInMonth = days, elapsed = elapsed, daysLeft = daysLeft,
            spent = summary.expense, projectedExpense = projected, projectedBalance = projectedBalance,
            budget = budget, safeDaily = safeDaily, verdict = verdict,
            reliable = isPast || (isCurrent && elapsed >= 5), cumulative = cumulative
        )
    }

    /** Прогноз расхода по категории на конец месяца по среднему за прошедшие дни. */
    fun projectCategory(spent: Long, elapsed: Int, daysInMonth: Int): Long =
        if (elapsed <= 0) 0L else spent * daysInMonth / elapsed

    fun opsInMonth(ops: List<Operation>, ym: YearMonth): List<Operation> {
        val start = ym.atDay(1).toEpochDay()
        val end = ym.atEndOfMonth().toEpochDay()
        return ops.filter { it.date in start..end }
    }

    fun opsInYear(ops: List<Operation>, year: Int): List<Operation> {
        val start = LocalDate.of(year, 1, 1).toEpochDay()
        val end = LocalDate.of(year, 12, 31).toEpochDay()
        return ops.filter { it.date in start..end }
    }

    fun paidIds(monthOps: List<Operation>): Set<Long> =
        monthOps.mapNotNull { it.mandatoryId }.toSet()

    fun monthSummary(
        ops: List<Operation>,
        ym: YearMonth,
        initialTotal: Long,
        mandatory: List<Mandatory>,
        paid: Set<Long>
    ): MonthSummary {
        val start = ym.atDay(1).toEpochDay()
        val end = ym.atEndOfMonth().toEpochDay()
        var opening = initialTotal
        var income = 0L
        var expense = 0L
        for (o in ops) {
            if (o.date < start) {
                opening += o.delta()
            } else if (o.date <= end) {
                if (o.type == OpType.INCOME) income += o.amountCents
                if (o.type == OpType.EXPENSE) expense += o.amountCents
            }
        }
        return MonthSummary(
            opening = opening,
            closing = opening + income - expense,
            income = income,
            expense = expense,
            mandatoryTotal = mandatory.sumOf { it.amountCents },
            mandatoryUnpaid = mandatory.filter { it.id !in paid }.sumOf { it.amountCents }
        )
    }

    fun categoryStats(ops: List<Operation>, type: String): List<CatStat> {
        val groups = ops.filter { it.type == type }.groupBy { it.category.ifBlank { "Без категории" } }
        val total = groups.values.sumOf { list -> list.sumOf { it.amountCents } }
        return groups.map { (name, list) ->
            val sum = list.sumOf { it.amountCents }
            CatStat(name, sum, list.size, if (total > 0) sum.toFloat() / total else 0f)
        }.sortedByDescending { it.sum }
    }

    /** Расходы по дням месяца (индекс 0 = 1-е число). */
    fun dailyExpenses(monthOps: List<Operation>, ym: YearMonth): List<Long> {
        val days = LongArray(ym.lengthOfMonth())
        for (o in monthOps) {
            if (o.type == OpType.EXPENSE) {
                val d = LocalDate.ofEpochDay(o.date).dayOfMonth
                days[d - 1] += o.amountCents
            }
        }
        return days.toList()
    }

    /** Суммы по 12 месяцам года для типа операции. */
    fun monthlyTotals(yearOps: List<Operation>, type: String): List<Long> {
        val months = LongArray(12)
        for (o in yearOps) {
            if (o.type == type) {
                months[LocalDate.ofEpochDay(o.date).monthValue - 1] += o.amountCents
            }
        }
        return months.toList()
    }

    fun accountBalance(account: Account, ops: List<Operation>): Long {
        var balance = account.initialCents
        for (o in ops) {
            when (o.type) {
                OpType.INCOME -> if (o.account == account.name) balance += o.amountCents
                OpType.EXPENSE -> if (o.account == account.name) balance -= o.amountCents
                OpType.TRANSFER -> {
                    if (o.account == account.name) balance -= o.amountCents
                    if (o.destAccount == account.name) balance += o.amountCents
                }
            }
        }
        return balance
    }
}
