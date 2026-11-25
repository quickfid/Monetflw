package ru.familybudget.app.logic

import ru.familybudget.app.data.Account
import ru.familybudget.app.data.AppSettings
import ru.familybudget.app.data.Category
import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import ru.familybudget.app.data.PlanItem
import java.time.LocalDate
import java.time.YearMonth

/** Все расчёты по одному месяцу в одном месте: считаются один раз и используются несколькими экранами. */
data class MonthData(
    val ym: YearMonth,
    val ops: List<Operation>,
    val summary: MonthSummary,
    val paid: Set<Long>,
    val hasPlan: Boolean,
    /** План по категориям на этот месяц (если плана нет — лимиты категорий по умолчанию). */
    val limits: Map<String, Long>,
    val planTotal: Long,
    val budget: Long,
    val spentByCategory: Map<String, Long>,
    val forecast: Forecast
)

object Plans {
    fun key(ym: YearMonth): String = ym.toString()

    fun hasPlan(plans: List<PlanItem>, ym: YearMonth): Boolean = plans.any { it.ym == key(ym) }

    fun limitsFor(plans: List<PlanItem>, categories: List<Category>, ym: YearMonth): Map<String, Long> {
        val rows = plans.filter { it.ym == key(ym) }
        return if (rows.isNotEmpty()) {
            rows.associate { it.category to it.amountCents }
        } else {
            categories.filter { !it.isIncome && it.limitCents > 0 }.associate { it.name to it.limitCents }
        }
    }

    fun monthData(
        ops: List<Operation>,
        accounts: List<Account>,
        mandatory: List<Mandatory>,
        plans: List<PlanItem>,
        categories: List<Category>,
        settings: AppSettings,
        ym: YearMonth,
        today: LocalDate
    ): MonthData {
        val monthOps = Stats.opsInMonth(ops, ym)
        val paid = Stats.paidIds(monthOps)
        val summary = Stats.monthSummary(ops, ym, accounts.sumOf { it.initialCents }, mandatory, paid)
        val limits = limitsFor(plans, categories, ym)
        val planTotal = limits.values.sum()
        val budget = if (planTotal > 0) planTotal else settings.monthlyBudget
        val spent = monthOps.filter { it.type == OpType.EXPENSE }
            .groupBy { it.category.ifBlank { "Без категории" } }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
        val forecast = Stats.forecast(monthOps, ym, today, summary, budget, settings.monthlyIncome)
        return MonthData(ym, monthOps, summary, paid, hasPlan(plans, ym), limits, planTotal, budget, spent, forecast)
    }

    /** Заготовка плана для месяца: прошлый план → лимиты категорий → пусто. */
    fun suggest(plans: List<PlanItem>, categories: List<Category>, ym: YearMonth): Map<String, Long> {
        val current = plans.filter { it.ym == key(ym) }
        if (current.isNotEmpty()) return current.associate { it.category to it.amountCents }
        val previous = plans.map { it.ym }.filter { it < key(ym) }.maxOrNull()
        if (previous != null) {
            return plans.filter { it.ym == previous }.associate { it.category to it.amountCents }
        }
        return categories.filter { !it.isIncome && it.limitCents > 0 }.associate { it.name to it.limitCents }
    }

    /** Расходные категории в порядке плана. */
    fun orderedExpenseCategories(categories: List<Category>): List<String> =
        categories.filter { !it.isIncome }.map { it.name }.sortedBy { Fmt.planRank(it) }
}
