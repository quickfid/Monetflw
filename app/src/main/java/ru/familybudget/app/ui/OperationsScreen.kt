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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Stats

private const val ALL = "Все"

@Composable
fun OperationsScreen(vm: BudgetViewModel, padding: PaddingValues) {
    val ops by vm.operations.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    val ym = vm.month
    var filter by rememberSaveable { mutableStateOf(ALL) }
    var query by rememberSaveable { mutableStateOf("") }

    val monthOps = remember(ops, ym) { Stats.opsInMonth(ops, ym) }
    val list = remember(monthOps, filter, query) {
        monthOps.filter { o ->
            (filter == ALL || o.type == filter) &&
                (query.isBlank() ||
                    o.category.contains(query, ignoreCase = true) ||
                    o.comment.contains(query, ignoreCase = true) ||
                    o.person.contains(query, ignoreCase = true))
        }
    }
    val grouped = remember(list) { list.groupBy { it.date } }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(ALL) + OpType.all).forEach { t ->
                        FilterChip(selected = filter == t, onClick = { filter = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("🔍 Поиск") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (list.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text("🧾", fontSize = 40.sp)
                    EmptyHint(
                        if (monthOps.isEmpty()) "Пока пусто. Запишите первую трату!"
                        else "Ничего не найдено"
                    )
                    if (monthOps.isEmpty()) {
                        androidx.compose.material3.Button(onClick = { vm.startNew() }) { Text("Добавить операцию") }
                    }
                }
            }
        }
        grouped.forEach { (day, dayOps) ->
            item(key = "h$day") {
                val dayTotal = dayOps.sumOf {
                    when (it.type) {
                        OpType.EXPENSE -> -it.amountCents
                        OpType.INCOME -> it.amountCents
                        else -> 0L
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Fmt.date(day), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        (if (dayTotal > 0) "+" else "") + Fmt.money(dayTotal, cur, false),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(dayOps, key = { it.id }) { op ->
                OperationRow(op, cur) { vm.edit(op) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun OperationRow(op: Operation, currency: String, onClick: () -> Unit) {
    val transfer = op.type == OpType.TRANSFER
    val title = if (transfer) "Перевод: ${op.account} → ${op.destAccount}" else op.category.ifBlank { "Без категории" }
    val subtitle = listOf(op.comment, op.person, if (transfer) "" else op.account)
        .filter { it.isNotBlank() }.joinToString(" · ")
    val color = when (op.type) {
        OpType.INCOME -> AppColors.income()
        OpType.EXPENSE -> AppColors.expense()
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sign = when (op.type) {
        OpType.INCOME -> "+"
        OpType.EXPENSE -> "−"
        else -> ""
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Text(sign + Fmt.money(op.amountCents, currency), color = color, fontWeight = FontWeight.SemiBold)
    }
}
