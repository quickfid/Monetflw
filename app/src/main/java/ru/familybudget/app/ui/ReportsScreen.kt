package ru.familybudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.data.OpType
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Stats

@Composable
fun ReportsScreen(vm: BudgetViewModel, padding: PaddingValues) {
    val ops by vm.operations.collectAsState()
    val categories by vm.categories.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val ym = vm.month
    var showIncome by rememberSaveable { mutableStateOf(false) }
    val type = if (showIncome) OpType.INCOME else OpType.EXPENSE

    val monthOps = remember(ops, ym) { Stats.opsInMonth(ops, ym) }
    val stats = remember(monthOps, type) { Stats.categoryStats(monthOps, type) }
    val total = stats.sumOf { it.sum }
    val daily = remember(monthOps, ym) { Stats.dailyExpenses(monthOps, ym) }
    val spentByCategory = remember(monthOps) {
        Stats.categoryStats(monthOps, OpType.EXPENSE).associate { it.name to it.sum }
    }
    val limited = categories.filter { !it.isIncome && it.limitCents > 0 }
    val today = remember { java.time.LocalDate.now() }
    val elapsed = Stats.elapsedDays(ym, today)
    val isCurrentMonth = ym == java.time.YearMonth.from(today)
    val expenseColor = AppColors.expense()
    val incomeColor = AppColors.income()

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showIncome, onClick = { showIncome = false }, label = { Text("Расходы") })
                    FilterChip(selected = showIncome, onClick = { showIncome = true }, label = { Text("Доходы") })
                }
            }
        }
        item {
            Section(if (showIncome) "🧾 Отчёты: ДОХОДЫ" else "🧾 Отчёты: РАСХОДЫ") {
                if (stats.isEmpty()) {
                    EmptyHint("Нет данных за этот месяц.")
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(stats.map { it.sum.toFloat() }, AppColors.chart)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Всего", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Fmt.money(total, cur, false), fontWeight = FontWeight.Bold)
                        }
                    }
                    stats.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(AppColors.chart[i % AppColors.chart.size], CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${s.count} опер. · ${Fmt.percent(s.share)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(Fmt.money(s.sum, cur), fontWeight = FontWeight.SemiBold, color = if (showIncome) incomeColor else expenseColor)
                        }
                    }
                }
            }
        }
        if (!showIncome) {
            item {
                Section("📅 График: расходы по дням") {
                    if (daily.all { it == 0L }) {
                        EmptyHint("Нет расходов за этот месяц.")
                    } else {
                        val labels = daily.indices.map { d -> if ((d + 1) == 1 || (d + 1) % 5 == 0) (d + 1).toString() else "" }
                        BarChart(daily, labels, expenseColor)
                        val maxDay = daily.indices.maxByOrNull { daily[it] } ?: 0
                        KeyValueRow("Самый затратный день", "${maxDay + 1}-е · " + Fmt.money(daily[maxDay], cur, false))
                        val spentDays = daily.count { it > 0 }
                        KeyValueRow("В среднем за день с тратами", Fmt.money(if (spentDays > 0) daily.sum() / spentDays else 0L, cur, false))
                    }
                }
            }
            item {
                Section("🎯 Лимиты по категориям") {
                    if (limited.isEmpty()) {
                        EmptyHint("Лимит — сколько вы готовы тратить на категорию в месяц. Задайте лимиты: Ещё → Категории и лимиты. Можно разделить бюджет автоматически.")
                    } else {
                        limited.forEach { c ->
                            val spent = spentByCategory[c.name] ?: 0L
                            val left = c.limitCents - spent
                            val fraction = (spent.toFloat() / c.limitCents).coerceIn(0f, 1f)
                            val over = left < 0
                            val warn = !over && fraction >= 0.8f
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(c.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        Fmt.money(spent, cur, false) + " / " + Fmt.money(c.limitCents, cur, false),
                                        fontSize = 13.sp
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (over) expenseColor else if (warn) AppColors.warning() else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (over) "Превышено на " + Fmt.money(-left, cur, false) else "Осталось " + Fmt.money(left, cur, false),
                                    fontSize = 12.sp,
                                    color = if (over) expenseColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isCurrentMonth && elapsed >= 5 && !over) {
                                    val projected = Stats.projectCategory(spent, elapsed, ym.lengthOfMonth())
                                    if (projected > c.limitCents) {
                                        Text(
                                            "🔮 При таких тратах к концу месяца выйдет около " + Fmt.money(projected, cur, false) + ": лимит будет превышен",
                                            fontSize = 12.sp,
                                            color = AppColors.warning()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
