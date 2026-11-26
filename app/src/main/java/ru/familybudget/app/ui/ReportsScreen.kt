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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import ru.familybudget.app.logic.Plans
import ru.familybudget.app.logic.Stats
import java.time.LocalDate

/** Вкладка «Отчёты»: переключатель Месяц / Год и кнопка выгрузки красивого отчёта в таблицу. */
@Composable
fun ReportsHost(vm: BudgetViewModel, padding: PaddingValues) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Месяц") })
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Год") })
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { vm.exportReport(vm.month) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("📤 В таблицу", fontSize = 13.sp) }
        }
        Box(Modifier.weight(1f)) {
            if (mode == 0) ReportsScreen(vm, PaddingValues(0.dp)) else YearScreen(vm, PaddingValues(0.dp))
        }
    }
}

@Composable
fun ReportsScreen(vm: BudgetViewModel, padding: PaddingValues) {
    val ops by vm.operations.collectAsState()
    val categories by vm.categories.collectAsState()
    val plans by vm.plans.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val ym = vm.month
    var showIncome by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val type = if (showIncome) OpType.INCOME else OpType.EXPENSE

    val monthOps = remember(ops, ym) { Stats.opsInMonth(ops, ym) }
    val stats = remember(monthOps, type) { Stats.categoryStats(monthOps, type) }
    val total = stats.sumOf { it.sum }
    val daily = remember(monthOps, ym) { Stats.dailyExpenses(monthOps, ym) }
    val spentByCategory = remember(monthOps) {
        Stats.categoryStats(monthOps, OpType.EXPENSE).associate { it.name to it.sum }
    }
    val limits = remember(plans, categories, ym) { Plans.limitsFor(plans, categories, ym) }
    val limitRows = remember(limits) {
        limits.filter { it.value > 0 }.toList().sortedBy { Fmt.planRank(it.first) }
    }
    val today = remember { LocalDate.now() }
    val elapsed = Stats.elapsedDays(ym, today)
    val isCurrentMonth = ym == java.time.YearMonth.from(today)
    val expenseColor = AppColors.expense()
    val incomeColor = AppColors.income()

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
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
            Section(if (showIncome) "🧾 Доходы" else "🧾 Расходы") {
                if (stats.isEmpty()) {
                    EmptyHint("Пока пусто")
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(stats.map { it.sum.toFloat() }, AppColors.chart)
                        Text(Fmt.money(total, cur, false), fontWeight = FontWeight.Bold)
                    }
                    stats.forEachIndexed { i, s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(AppColors.chart[i % AppColors.chart.size], CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(Fmt.emoji(s.name) + " " + s.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${s.count} · ${Fmt.percent(s.share)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(Fmt.money(s.sum, cur, false), fontWeight = FontWeight.SemiBold, color = if (showIncome) incomeColor else expenseColor)
                        }
                    }
                }
            }
        }
        if (!showIncome) {
            item {
                Section("📅 По дням") {
                    if (daily.all { it == 0L }) {
                        EmptyHint("Пока пусто")
                    } else {
                        val labels = daily.indices.map { d -> if ((d + 1) == 1 || (d + 1) % 5 == 0) (d + 1).toString() else "" }
                        BarChart(daily, labels, expenseColor)
                        val maxDay = daily.indices.maxByOrNull { daily[it] } ?: 0
                        KeyValueRow("Самый затратный день", "${maxDay + 1}-е · " + Fmt.money(daily[maxDay], cur, false))
                    }
                }
            }
            item {
                Section("🎯 План и факт") {
                    if (limitRows.isEmpty()) {
                        EmptyHint("Составьте план на вкладке «План»")
                    } else {
                        limitRows.forEach { row ->
                            val name = row.first
                            val limit = row.second
                            val spent = spentByCategory[name] ?: 0L
                            val left = limit - spent
                            val fraction = (spent.toFloat() / limit).coerceIn(0f, 1f)
                            val over = left < 0
                            val warn = !over && fraction >= 0.85f
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(Fmt.emoji(name) + " " + name, fontWeight = FontWeight.Medium)
                                    Text(Fmt.money(spent, cur, false) + " / " + Fmt.money(limit, cur, false), fontSize = 13.sp)
                                }
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = if (over) expenseColor else if (warn) AppColors.warning() else MaterialTheme.colorScheme.primary
                                )
                                if (isCurrentMonth && elapsed >= 5 && !over && !Fmt.isBill(name)) {
                                    val projected = Stats.projectCategory(spent, elapsed, ym.lengthOfMonth())
                                    if (projected > limit) {
                                        Text(
                                            "🔮 К концу месяца выйдет около " + Fmt.money(projected, cur, false),
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
