package ru.familybudget.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Plans
import java.time.YearMonth

/** В начале каждого месяца спрашиваем актуальные суммы: аренда, кредит, связь, коммунальные. */
@Composable
fun FixedPaymentsDialog(vm: BudgetViewModel, ym: YearMonth) {
    val categories by vm.categories.collectAsState()
    val plans by vm.plans.collectAsState()
    val mandatory by vm.mandatory.collectAsState()
    val cur = vm.settings.collectAsState().value.currency
    val names = remember(categories) { Plans.orderedExpenseCategories(categories).filter { Fmt.isBill(it) } }
    val suggestion = remember(plans, categories, ym) { Plans.suggest(plans, categories, ym) }
    val values = remember(ym, names) {
        mutableStateMapOf<String, String>().apply {
            names.forEach { n ->
                val v = suggestion[n] ?: mandatory.firstOrNull { it.category == n }?.amountCents ?: 0L
                put(n, if (v > 0) Fmt.plain(v) else "")
            }
        }
    }
    AlertDialog(
        onDismissRequest = { vm.dismissFixed(ym) },
        title = { Text("🗓 " + Fmt.monthTitle(ym)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Суммы изменились? Поправьте.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PlanFields(names, values, cur)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val map = HashMap<String, Long>()
                names.forEach { n -> map[n] = Fmt.parseCents(values[n].orEmpty()) ?: 0L }
                vm.saveFixed(ym, map)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = { vm.dismissFixed(ym) }) { Text("Позже") } }
    )
}
