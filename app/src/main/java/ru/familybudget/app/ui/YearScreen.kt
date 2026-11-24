package ru.familybudget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.data.OpType
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Stats

@Composable
fun YearScreen(vm: BudgetViewModel, padding: PaddingValues) {
    val ops by vm.operations.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val year = vm.year

    val yearOps = remember(ops, year) { Stats.opsInYear(ops, year) }
    val expenses = remember(yearOps) { Stats.monthlyTotals(yearOps, OpType.EXPENSE) }
    val incomes = remember(yearOps) { Stats.monthlyTotals(yearOps, OpType.INCOME) }
    val top = remember(yearOps) { Stats.categoryStats(yearOps, OpType.EXPENSE).take(15) }
    val monthsLabels = (1..12).map { Fmt.shortMonth(it) }
    val totalExpense = expenses.sum()
    val totalIncome = incomes.sum()
    val activeMonths = expenses.count { it > 0 }
    val avgExpense = if (activeMonths > 0) totalExpense / activeMonths else 0L
    val saved = totalIncome - totalExpense
    val incomeColor = AppColors.income()
    val expenseColor = AppColors.expense()

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MonthSwitcher(year.toString(), onPrev = { vm.shiftYear(-1) }, onNext = { vm.shiftYear(1) }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Доходы за год", Fmt.money(totalIncome, cur, false), Modifier.weight(1f), valueColor = incomeColor)
                    StatCard("Расходы за год", Fmt.money(totalExpense, cur, false), Modifier.weight(1f), valueColor = expenseColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        "Накоплено за год",
                        (if (saved > 0) "+" else "") + Fmt.money(saved, cur, false),
                        Modifier.weight(1f),
                        valueColor = if (saved >= 0) incomeColor else expenseColor
                    )
                    StatCard("Среднемесячные расходы", Fmt.money(avgExpense, cur, false), Modifier.weight(1f))
                }
            }
        }
        item {
            Section("Траты помесячно") { BarChart(expenses, monthsLabels, expenseColor) }
        }
        item {
            Section("Доходы помесячно") { BarChart(incomes, monthsLabels, incomeColor) }
        }
        item {
            Section("По месяцам") {
                var running = 0L
                (0 until 12).forEach { i ->
                    val diff = incomes[i] - expenses[i]
                    running += diff
                    if (incomes[i] != 0L || expenses[i] != 0L) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(Fmt.fullMonth(i + 1), fontWeight = FontWeight.Medium)
                                Text(
                                    (if (diff > 0) "+" else "") + Fmt.money(diff, cur, false),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (diff >= 0) incomeColor else expenseColor
                                )
                            }
                            Text(
                                "доход ${Fmt.money(incomes[i], cur, false)} · расход ${Fmt.money(expenses[i], cur, false)} · накопления ${Fmt.money(running, cur, false)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (activeMonths == 0 && totalIncome == 0L) EmptyHint("За этот год операций пока нет.")
            }
        }
        item {
            Section("Траты — ТОП-15 категорий") {
                if (top.isEmpty()) {
                    EmptyHint("Нет расходов за этот год.")
                } else {
                    top.forEachIndexed { i, s ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${i + 1}. ${s.name}", fontWeight = FontWeight.Medium)
                                Text(Fmt.money(s.sum, cur, false), fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(progress = { s.share }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    KeyValueRow("Итого за год", Fmt.money(totalExpense, cur, false))
                }
            }
        }
    }
}
