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
import ru.familybudget.app.logic.Stats
import ru.familybudget.app.logic.delta

@Composable
fun OverviewScreen(vm: BudgetViewModel, padding: PaddingValues) {
    val ops by vm.operations.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val mandatory by vm.mandatory.collectAsState()
    val categories by vm.categories.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val ym = vm.month

    val initialTotal = accounts.sumOf { it.initialCents }
    val balance = initialTotal + ops.sumOf { it.delta() }
    val monthOps = remember(ops, ym) { Stats.opsInMonth(ops, ym) }
    val paid = remember(monthOps) { Stats.paidIds(monthOps) }
    val summary = remember(ops, accounts, mandatory, ym, paid) {
        Stats.monthSummary(ops, ym, initialTotal, mandatory, paid)
    }
    val limitTotal = categories.filter { !it.isIncome }.sumOf { it.limitCents }
    val diff = summary.income - summary.expense
    val incomeColor = AppColors.income()
    val expenseColor = AppColors.expense()

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Section("💸 Текущий баланс") {
                Text(
                    Fmt.money(balance, cur),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            Section("📊 Месячный обзор") {
                MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
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
                    EmptyHint("Добавьте платежи: Ещё → Обязательные платежи.")
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
                }
            }
        }
        if (limitTotal > 0) {
            item {
                val fraction = (summary.expense.toFloat() / limitTotal).coerceIn(0f, 1f)
                val over = summary.expense > limitTotal
                Section("🎯 Бюджет месяца") {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (over) expenseColor else MaterialTheme.colorScheme.primary
                    )
                    KeyValueRow("Потрачено из лимитов", Fmt.money(summary.expense, cur, false) + " из " + Fmt.money(limitTotal, cur, false))
                    KeyValueRow(
                        if (over) "Превышение" else "Осталось",
                        Fmt.money(kotlin.math.abs(limitTotal - summary.expense), cur, false),
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
