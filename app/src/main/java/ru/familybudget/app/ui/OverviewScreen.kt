package ru.familybudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ru.familybudget.app.logic.Stats
import ru.familybudget.app.logic.Verdict
import ru.familybudget.app.logic.delta
import java.time.LocalDate

@Composable
fun OverviewScreen(vm: BudgetViewModel, padding: PaddingValues, onOpenSettings: () -> Unit) {
    val ops by vm.operations.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val mandatory by vm.mandatory.collectAsState()
    val categories by vm.categories.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val ym = vm.month
    val today = remember { LocalDate.now() }

    val initialTotal = accounts.sumOf { it.initialCents }
    val balance = initialTotal + ops.sumOf { it.delta() }
    val monthOps = remember(ops, ym) { Stats.opsInMonth(ops, ym) }
    val paid = remember(monthOps) { Stats.paidIds(monthOps) }
    val summary = remember(ops, accounts, mandatory, ym, paid) {
        Stats.monthSummary(ops, ym, initialTotal, mandatory, paid)
    }
    val limitTotal = categories.filter { !it.isIncome }.sumOf { it.limitCents }
    val budget = if (settings.monthlyBudget > 0) settings.monthlyBudget else limitTotal
    val forecast = remember(monthOps, ym, summary, budget, settings.monthlyIncome) {
        Stats.forecast(monthOps, ym, today, summary, budget, settings.monthlyIncome)
    }
    val diff = summary.income - summary.expense
    val incomeColor = AppColors.income()
    val expenseColor = AppColors.expense()
    val warnColor = AppColors.warning()

    val stepsDone = listOf(
        settings.userName.isNotBlank() && settings.monthlyIncome > 0,
        ops.isNotEmpty(),
        categories.any { !it.isIncome && it.limitCents > 0 },
        mandatory.isNotEmpty()
    )

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp)) {
                Text(
                    if (settings.userName.isBlank()) "Здравствуйте!" else "Здравствуйте, ${settings.userName}!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Вот как дела с деньгами",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!settings.hideTips && stepsDone.any { !it }) {
            item {
                Section("🚀 Начало работы: ${stepsDone.count { it }} из ${stepsDone.size}") {
                    val labels = listOf(
                        "Указать имя, доход и бюджет (Ещё → Профиль и бюджет)",
                        "Добавить первую операцию кнопкой «+»",
                        "Задать лимиты по категориям (Ещё → Категории)",
                        "Добавить обязательные платежи (Ещё → Обязательные платежи)"
                    )
                    labels.forEachIndexed { i, label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (stepsDone[i]) "✅" else "⬜", fontSize = 18.sp)
                            Text(
                                "  $label",
                                modifier = Modifier.weight(1f),
                                color = if (stepsDone[i]) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { vm.startNew() }) { Text("Добавить операцию") }
                        TextButton(onClick = onOpenSettings) { Text("Настройки") }
                        TextButton(onClick = { vm.hideTips() }) { Text("Скрыть") }
                    }
                }
            }
        }
        item {
            Section("💸 Текущий баланс") {
                Text(
                    Fmt.money(balance, cur),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                HelpText("Столько денег сейчас на всех ваших счетах вместе.")
            }
        }
        item {
            MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
        }
        item {
            val tone = when (forecast.verdict) {
                Verdict.GOOD -> Tone.INCOME
                Verdict.WARN -> Tone.WARN
                Verdict.BAD -> Tone.EXPENSE
                Verdict.NONE -> Tone.NEUTRAL
            }
            val title = when {
                forecast.isPast -> "📘 Итоги месяца"
                !forecast.isCurrent -> "🗓 Месяц впереди"
                forecast.verdict == Verdict.GOOD -> "✅ Всё идёт хорошо"
                forecast.verdict == Verdict.WARN -> "⚠️ Будьте внимательны"
                forecast.verdict == Verdict.BAD -> "🚨 Есть риск перерасхода"
                else -> "🔮 Прогноз"
            }
            Banner(title, tone) {
                if (forecast.isPast) {
                    Text("Потрачено " + Fmt.money(forecast.spent, cur, false) + ", остаток на конец месяца " + Fmt.money(summary.closing, cur, false) + ".")
                } else if (!forecast.isCurrent) {
                    Text(
                        if (budget > 0) "План трат на месяц: " + Fmt.money(budget, cur, false) + ". Обязательных платежей: " + Fmt.money(summary.mandatoryTotal, cur, false) + "."
                        else "Задайте месячный бюджет, чтобы видеть планы на будущие месяцы."
                    )
                } else {
                    Text("Если тратить так же, к концу месяца вы потратите около " + Fmt.money(forecast.projectedExpense, cur, false) +
                        (if (budget > 0) " из " + Fmt.money(budget, cur, false) + " запланированных." else "."))
                    Text(
                        "Прогноз остатка на конец месяца: " + Fmt.money(forecast.projectedBalance, cur, false) + ".",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (budget > 0) {
                        Text(
                            if (forecast.safeDaily > 0) "Чтобы уложиться, можно тратить около " + Fmt.money(forecast.safeDaily, cur, false) + " в день."
                            else "Бюджет на этот месяц уже исчерпан или почти исчерпан."
                        )
                    } else {
                        HelpText("Совет: укажите бюджет (Ещё → Профиль и бюджет), и приложение подскажет, сколько можно тратить в день.")
                    }
                    if (!forecast.reliable) HelpText("В начале месяца прогноз неточный. Через несколько дней он станет надёжнее.")
                }
            }
        }
        if (forecast.isCurrent || forecast.isPast) {
            item {
                Section("📉 Траты по дням: факт и прогноз") {
                    if (forecast.cumulative.isEmpty() || forecast.spent == 0L) {
                        EmptyHint("Пока нет расходов в этом месяце. Добавьте первую операцию кнопкой «+», и здесь появится график.")
                    } else {
                        BurnChart(
                            actual = forecast.cumulative,
                            daysInMonth = forecast.daysInMonth,
                            projectedEnd = if (forecast.isCurrent) forecast.projectedExpense else null,
                            budget = budget,
                            actualColor = MaterialTheme.colorScheme.primary,
                            projectionColor = warnColor,
                            budgetColor = expenseColor
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("● факт", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            if (forecast.isCurrent) Text("┅ прогноз", color = warnColor, fontSize = 12.sp)
                            if (budget > 0) Text("┅ бюджет", color = expenseColor, fontSize = 12.sp)
                        }
                        HelpText("Линия показывает, как растут ваши траты с начала месяца. Если она пересекает линию бюджета, вы выходите за план.")
                    }
                }
            }
        }
        item {
            Section("📊 Месячный обзор") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Остаток на начало месяца", Fmt.money(summary.opening, cur), Modifier.weight(1f))
                    StatCard("Остаток на конец месяца", Fmt.money(summary.closing, cur), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Доходы за месяц", Fmt.money(summary.income, cur), Modifier.weight(1f), Tone.INCOME)
                    StatCard("Расходы за месяц", Fmt.money(summary.expense, cur), Modifier.weight(1f), Tone.EXPENSE)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Обязательные платежи", Fmt.money(summary.mandatoryTotal, cur), Modifier.weight(1f), Tone.WARN)
                    StatCard("Разница доход/расход", Fmt.money(diff, cur), Modifier.weight(1f))
                }
            }
        }
        item {
            Section("💰 Обязательные платежи") {
                if (mandatory.isEmpty()) {
                    EmptyHint("Здесь будут ежемесячные платежи: аренда, связь, интернет. Добавьте их в разделе Ещё → Обязательные платежи.")
                } else {
                    Row(
                        Modifier.fillMaxWidth().background(Sheet.header()).padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("№", Modifier.weight(0.5f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Название", Modifier.weight(2.2f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Дата", Modifier.weight(0.8f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Сумма", Modifier.weight(1.5f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("✅", Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 14.sp)
                    }
                    mandatory.forEachIndexed { i, m ->
                        val isPaid = m.id in paid
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.toggleMandatory(m, ym, !isPaid) }.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text((i + 1).toString(), Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 13.sp)
                            Text(m.name, Modifier.weight(2.2f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(m.day.toString(), Modifier.weight(0.8f), textAlign = TextAlign.Center, fontSize = 13.sp)
                            Text(Fmt.money(m.amountCents, cur), Modifier.weight(1.5f), textAlign = TextAlign.End, fontSize = 13.sp)
                            Row(Modifier.weight(0.8f), horizontalArrangement = Arrangement.Center) {
                                Checkbox(checked = isPaid, onCheckedChange = { vm.toggleMandatory(m, ym, it) })
                            }
                        }
                        HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                    }
                    KeyValueRow(
                        "Осталось оплатить",
                        Fmt.money(summary.mandatoryUnpaid, cur),
                        if (summary.mandatoryUnpaid > 0) expenseColor else incomeColor
                    )
                    HelpText("Оплатили платёж? Поставьте галочку, и расход добавится сам.")
                }
            }
        }
        if (budget > 0) {
            item {
                val fraction = (summary.expense.toFloat() / budget).coerceIn(0f, 1f)
                val over = summary.expense > budget
                Section("🎯 Бюджет месяца") {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (over) expenseColor else MaterialTheme.colorScheme.primary
                    )
                    KeyValueRow("Потрачено", Fmt.money(summary.expense, cur, false) + " из " + Fmt.money(budget, cur, false))
                    KeyValueRow(
                        if (over) "Превышение" else "Осталось",
                        Fmt.money(kotlin.math.abs(budget - summary.expense), cur, false),
                        if (over) expenseColor else incomeColor
                    )
                }
            }
        }
        item {
            Section("🏦 Счета") {
                if (accounts.isEmpty()) EmptyHint("Нет счетов.")
                accounts.forEach { a ->
                    KeyValueRow(a.name, Fmt.money(Stats.accountBalance(a, ops), cur))
                }
            }
        }
    }
}
