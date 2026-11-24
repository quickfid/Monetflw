package ru.familybudget.app.ui

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatCard(
                label = "Текущий баланс",
                value = Fmt.money(balance, cur),
                container = MaterialTheme.colorScheme.primaryContainer,
                valueColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        item {
            MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Остаток на начало", Fmt.money(summary.opening, cur), Modifier.weight(1f))
                    StatCard("Остаток на конец", Fmt.money(summary.closing, cur), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Доходы", Fmt.money(summary.income, cur), Modifier.weight(1f), valueColor = incomeColor)
                    StatCard("Расходы", Fmt.money(summary.expense, cur), Modifier.weight(1f), valueColor = expenseColor)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Обязательные платежи", Fmt.money(summary.mandatoryTotal, cur), Modifier.weight(1f))
                    StatCard(
                        "Разница доход/расход",
                        (if (diff > 0) "+" else "") + Fmt.money(diff, cur),
                        Modifier.weight(1f),
                        valueColor = if (diff >= 0) incomeColor else expenseColor
                    )
                }
            }
        }
        if (limitTotal > 0) {
            item {
                val fraction = (summary.expense.toFloat() / limitTotal).coerceIn(0f, 1f)
                val over = summary.expense > limitTotal
                Section("Бюджет месяца") {
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
            Section("Обязательные платежи") {
                if (mandatory.isEmpty()) {
                    EmptyHint("Добавьте платежи в разделе «Ещё» → «Обязательные платежи».")
                } else {
                    mandatory.forEach { m ->
                        val isPaid = m.id in paid
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.toggleMandatory(m, ym, !isPaid) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isPaid, onCheckedChange = { vm.toggleMandatory(m, ym, it) })
                            Column(Modifier.weight(1f)) {
                                Text(m.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "до ${m.day} числа",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(Fmt.money(m.amountCents, cur), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    KeyValueRow("Осталось оплатить", Fmt.money(summary.mandatoryUnpaid, cur), if (summary.mandatoryUnpaid > 0) expenseColor else incomeColor)
                }
            }
        }
        item {
            Section("Счета") {
                if (accounts.isEmpty()) EmptyHint("Нет счетов.")
                accounts.forEach { a ->
                    KeyValueRow(a.name, Fmt.money(Stats.accountBalance(a, ops), cur))
                }
            }
        }
    }
}
