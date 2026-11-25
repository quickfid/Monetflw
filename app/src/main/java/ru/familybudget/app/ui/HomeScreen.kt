package ru.familybudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Game
import ru.familybudget.app.logic.Plans
import ru.familybudget.app.logic.Verdict
import ru.familybudget.app.logic.delta
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: BudgetViewModel, padding: PaddingValues) {
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
    val game = remember(ops, plans, mandatory) { Game.compute(ops, plans, mandatory, today) }
    val balance = accounts.sumOf { it.initialCents } + ops.sumOf { it.delta() }
    val f = md.forecast
    val incomeColor = AppColors.income()
    val expenseColor = AppColors.expense()
    val warnColor = AppColors.warning()
    val todayDay = today.toEpochDay()
    val paidCount = mandatory.count { it.id in md.paid }
    val canPlan = !f.isPast

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- герой: уровень, опыт, серия
        item {
            Column(
                Modifier.fillMaxWidth().border(1.dp, Sheet.border()).background(Sheet.header()).padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(game.avatar, fontSize = 46.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (settings.userName.isBlank()) "Привет!" else "Привет, ${settings.userName}!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Уровень ${game.level} · ${game.title}", style = MaterialTheme.typography.labelLarge)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥 ${game.streak}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("дней", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LinearProgressIndicator(
                    progress = { game.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(10.dp)
                )
                Text(
                    "⭐ ${game.xp} XP · ещё ${game.xpToNext} до уровня ${game.level + 1}",
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
        }

        // ---- задания
        item {
            Section("🎯 Задания") {
                QuestRow(md.hasPlan, "План на месяц", "+100 XP", if (!md.hasPlan && canPlan) "Создать" else null) { vm.openPlan(ym) }
                QuestRow(ops.any { it.date == todayDay }, "Записать траты за сегодня", "+10 XP", "Добавить") { vm.startNew() }
                if (mandatory.isNotEmpty()) {
                    QuestRow(paidCount == mandatory.size, "Платежи оплачены: $paidCount из ${mandatory.size}", "+15 XP", null) {}
                }
                if (md.budget > 0) {
                    QuestRow(md.summary.expense <= md.budget, "Уложиться в план месяца", "🏅", null) {}
                }
            }
        }

        // ---- статус и прогноз
        item {
            val tone = when (f.verdict) {
                Verdict.GOOD -> Tone.INCOME
                Verdict.WARN -> Tone.WARN
                Verdict.BAD -> Tone.EXPENSE
                Verdict.NONE -> Tone.NEUTRAL
            }
            val title = when {
                f.isPast -> "📘 Итоги месяца"
                !f.isCurrent -> "🗓 Впереди"
                f.verdict == Verdict.GOOD -> "✅ Всё отлично"
                f.verdict == Verdict.WARN -> "⚠️ Осторожно"
                f.verdict == Verdict.BAD -> "🚨 Риск перерасхода"
                else -> "🔮 Прогноз"
            }
            Banner(title, tone) {
                if (f.isPast) {
                    KeyValueRow("Потрачено", Fmt.money(f.spent, cur, false))
                    KeyValueRow("Остаток", Fmt.money(md.summary.closing, cur, false))
                } else {
                    KeyValueRow("Прогноз трат", Fmt.money(f.projectedExpense, cur, false) + if (md.budget > 0) " из " + Fmt.money(md.budget, cur, false) else "")
                    if (f.isCurrent) {
                        KeyValueRow("Остаток к концу месяца", Fmt.money(f.projectedBalance, cur, false))
                        if (md.budget > 0) KeyValueRow("В день можно", Fmt.money(f.safeDaily, cur, false))
                        if (!f.reliable) Text("Через пару дней прогноз станет точнее", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ---- деньги
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("💸 Баланс сейчас", Fmt.money(balance, cur), Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Доходы", Fmt.money(md.summary.income, cur, false), Modifier.weight(1f), Tone.INCOME)
                    StatCard("Расходы", Fmt.money(md.summary.expense, cur, false), Modifier.weight(1f), Tone.EXPENSE)
                    StatCard("Остаток", Fmt.money(md.summary.closing, cur, false), Modifier.weight(1f))
                }
            }
        }
        if (md.budget > 0) {
            item {
                val fraction = (md.summary.expense.toFloat() / md.budget).coerceIn(0f, 1f)
                val over = md.summary.expense > md.budget
                Section("🎮 Бюджет месяца") {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = if (over) expenseColor else MaterialTheme.colorScheme.primary
                    )
                    KeyValueRow(
                        if (over) "Сверх плана" else "Осталось",
                        Fmt.money(kotlin.math.abs(md.budget - md.summary.expense), cur, false),
                        if (over) expenseColor else incomeColor
                    )
                }
            }
        }
        if ((f.isCurrent || f.isPast) && f.spent > 0) {
            item {
                Section("📉 Траты по дням") {
                    BurnChart(
                        actual = f.cumulative,
                        daysInMonth = f.daysInMonth,
                        projectedEnd = if (f.isCurrent) f.projectedExpense else null,
                        budget = md.budget,
                        actualColor = MaterialTheme.colorScheme.primary,
                        projectionColor = warnColor,
                        budgetColor = expenseColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("● факт", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        if (f.isCurrent) Text("┅ прогноз", color = warnColor, fontSize = 12.sp)
                        if (md.budget > 0) Text("┅ план", color = expenseColor, fontSize = 12.sp)
                    }
                }
            }
        }

        // ---- платежи
        if (mandatory.isNotEmpty()) {
            item {
                Section("💳 Платежи месяца") {
                    mandatory.forEach { m ->
                        val isPaid = m.id in md.paid
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.toggleMandatory(m, ym, !isPaid) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isPaid, onCheckedChange = { vm.toggleMandatory(m, ym, it) })
                            Text(Fmt.emoji(m.category.ifBlank { m.name }) + " " + m.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text(Fmt.money(m.amountCents, cur, false), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Sheet.border().copy(alpha = 0.4f))
                    }
                    KeyValueRow(
                        "Осталось оплатить",
                        Fmt.money(md.summary.mandatoryUnpaid, cur, false),
                        if (md.summary.mandatoryUnpaid > 0) expenseColor else incomeColor
                    )
                }
            }
        }

        // ---- награды
        item {
            Section("🏆 Награды ${game.badges.count { it.unlocked }} из ${game.badges.size}") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    game.badges.forEach { b ->
                        Column(
                            Modifier.width(100.dp).border(1.dp, Sheet.border())
                                .background(if (b.unlocked) Sheet.tone(Tone.WARN) else Sheet.tone(Tone.NEUTRAL))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (b.unlocked) b.emoji else "🔒", fontSize = 26.sp)
                            Text(
                                b.title,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = if (b.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestRow(done: Boolean, title: String, reward: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (done) "✅" else "⬜", fontSize = 20.sp)
        Text(
            "  $title",
            modifier = Modifier.weight(1f),
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        if (!done && action != null) {
            Button(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) { Text(action, fontSize = 12.sp) }
        } else {
            Text(reward, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
