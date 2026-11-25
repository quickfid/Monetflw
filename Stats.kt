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

object Stats {
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
