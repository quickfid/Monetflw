package ru.familybudget.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Plans
import java.time.LocalDate
import java.time.YearMonth

/** Вкладка «План»: каждая категория — как задание с полосой прогресса. */
@Composable
fun PlanScreen(vm: BudgetViewModel, padding: PaddingValues) {
    val ops by vm.operations.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val mandatory by vm.mandatory.collectAsState()
    val categories by vm.categories.collectAsState()
    val plans by vm.plans.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val ym = vm.month
    val today = remember { LocalDate.now() }
    val md = remember(ops, accounts, mandatory, plans, categories, settings, ym) {
        Plans.monthData(ops, accounts, mandatory, plans, categories, settings, ym, today)
    }
    val rows = remember(md) { (md.limits.keys + md.spentByCategory.keys).toList().sortedBy { Fmt.planRank(it) } }
    val free = settings.monthlyIncome - md.planTotal
    val expenseColor = AppColors.expense()
    val incomeColor = AppColors.income()
    val warnColor = AppColors.warning()

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) }) }
        if (!md.hasPlan) {
            item {
                Banner("📋 Плана на месяц ещё нет", Tone.WARN) {
                    Text("Аренда, кредит, транспорт, связь, коммунальные, прочее")
                    Button(onClick = { vm.openPlan(ym) }, modifier = Modifier.fillMaxWidth()) { Text("Создать план · +100 XP") }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Доход", Fmt.money(settings.monthlyIncome, cur, false), Modifier.weight(1f), Tone.INCOME)
                    StatCard("План", Fmt.money(md.planTotal, cur, false), Modifier.weight(1f), Tone.WARN)
                    StatCard("Свободно", Fmt.money(free, cur, false), Modifier.weight(1f), if (free >= 0) Tone.NEUTRAL else Tone.EXPENSE)
                }
            }
            item {
                OutlinedButton(onClick = { vm.openPlan(ym) }, modifier = Modifier.fillMaxWidth()) { Text("✏️ Изменить план") }
            }
        }
        items(rows, key = { it }) { name ->
            val plan = md.limits[name] ?: 0L
            val spent = md.spentByCategory[name] ?: 0L
            val ratio = if (plan > 0) spent.toFloat() / plan else 0f
            val over = plan > 0 && spent > plan
            val exactDone = plan > 0 && spent == plan
            val status = when {
                plan <= 0 -> "➕ вне плана"
                over -> "🔥 перерасход +" + Fmt.money(spent - plan, cur, false)
                exactDone -> "✅ Оплачено полностью"
                ratio >= 0.85f -> "⚠️ осталось " + Fmt.money(plan - spent, cur, false)
                else -> "✅ осталось " + Fmt.money(plan - spent, cur, false)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Sheet.glass(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryBadge(name, settings.categoryIcons, size = 32.dp)
                        Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(
                            Fmt.money(spent, cur, false) + if (plan > 0) " / " + Fmt.money(plan, cur, false) else "",
                            fontSize = 13.sp
                        )
                    }
                    if (plan > 0) {
                        val animRatio by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = ratio.coerceIn(0f, 1f),
                            animationSpec = androidx.compose.animation.core.tween(
                                680,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            ),
                            label = "planBar_$name"
                        )
                        LinearProgressIndicator(
                            progress = { animRatio },
                            modifier = Modifier.fillMaxWidth().height(9.dp),
                            color = when {
                                over -> expenseColor
                                exactDone -> AppColors.income()
                                ratio >= 0.85f -> warnColor
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    Text(
                        status,
                        fontSize = 12.sp,
                        color = if (over) expenseColor else if (plan <= 0) warnColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Поля плана: эмодзи, название и сумма. Используется и в мастере настройки, и в редакторе плана. */
@Composable
fun PlanFields(names: List<String>, values: MutableMap<String, String>, currency: String) {
    names.forEach { n ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.emoji(n) + " " + n, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            MoneyField(values[n].orEmpty(), { values[n] = it }, currency, Modifier.width(140.dp))
        }
    }
}

/** Полноэкранный редактор плана на месяц. Значения подставляются из прошлого месяца. */
@Composable
fun PlanEditor(vm: BudgetViewModel, ym: YearMonth) {
    val categories by vm.categories.collectAsState()
    val plans by vm.plans.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val names = remember(categories) { Plans.orderedExpenseCategories(categories) }
    val suggestion = remember(plans, categories, ym) { Plans.suggest(plans, categories, ym) }
    val values = remember(ym, names) {
        mutableStateMapOf<String, String>().apply {
            names.forEach { n ->
                val v = suggestion[n] ?: 0L
                put(n, if (v > 0) Fmt.plain(v) else "")
            }
        }
    }
    var income by remember(ym) {
        mutableStateOf(if (settings.monthlyIncome > 0) Fmt.plain(settings.monthlyIncome) else "")
    }
    val incomeCents = Fmt.parseCents(income) ?: 0L
    val total = names.sumOf { Fmt.parseCents(values[it].orEmpty()) ?: 0L }
    val free = incomeCents - total

    BackHandler { vm.closePlan() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.closePlan() }) { Icon(AppIcons.Close, contentDescription = "Закрыть") }
                Text("📋 План · " + Fmt.monthTitle(ym), style = MaterialTheme.typography.titleLarge)
            }
            MoneyField(income, { income = it }, "💼 Доход в месяц, $cur")
            PlanFields(names, values, cur)
            Banner("Итого", if (incomeCents > 0 && free < 0) Tone.EXPENSE else Tone.INCOME) {
                KeyValueRow("План", Fmt.money(total, cur, false))
                if (incomeCents > 0) {
                    KeyValueRow(if (free >= 0) "Свободно" else "Не хватает", Fmt.money(kotlin.math.abs(free), cur, false))
                }
            }
            Button(
                onClick = {
                    val map = HashMap<String, Long>()
                    names.forEach { n ->
                        val c = Fmt.parseCents(values[n].orEmpty()) ?: 0L
                        if (c > 0) map[n] = c
                    }
                    vm.savePlan(ym, map, incomeCents)
                },
                enabled = total > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Готово ✅") }
        }
    }
}
