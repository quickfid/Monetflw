package ru.familybudget.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import ru.familybudget.app.logic.Fmt
import java.time.LocalDate

@Composable
fun OperationEditor(vm: BudgetViewModel, initial: Operation) {
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val settings by vm.settings.collectAsState()
    val isNew = initial.id == 0L

    var type by remember(initial) { mutableStateOf(initial.type) }
    var amount by remember(initial) {
        mutableStateOf(if (initial.amountCents > 0) Fmt.plain(initial.amountCents) else "")
    }
    var category by remember(initial) { mutableStateOf(initial.category) }
    var date by remember(initial) { mutableStateOf(initial.date) }
    var account by remember(initial) { mutableStateOf(initial.account) }
    var destAccount by remember(initial) { mutableStateOf(initial.destAccount) }
    var person by remember(initial) { mutableStateOf(initial.person) }
    var comment by remember(initial) { mutableStateOf(initial.comment) }

    val accountNames = accounts.map { it.name }
    val categoryNames = categories.filter { it.isIncome == (type == OpType.INCOME) }.map { it.name }

    LaunchedEffect(type, categoryNames) {
        if (type != OpType.TRANSFER && category.isBlank() && categoryNames.isNotEmpty()) {
            category = categoryNames.first()
        }
    }

    val cents = Fmt.parseCents(amount)
    val isTransfer = type == OpType.TRANSFER
    val valid = cents != null && cents > 0 && person.isNotBlank() &&
        (if (isTransfer) (account.isNotBlank() && destAccount.isNotBlank() && account != destAccount) else category.isNotBlank())

    val today = LocalDate.now().toEpochDay()
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler { vm.closeEditor() }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить операцию?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteOperation(initial) }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } }
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.closeEditor() }) { Icon(AppIcons.Close, contentDescription = "Закрыть") }
                Text(
                    if (isNew) "Новая операция" else "Редактирование",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OpType.all.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { if (t != type) { type = t; category = "" } },
                        label = { Text(if (t == OpType.EXPENSE) "➖ Расход" else if (t == OpType.INCOME) "➕ Доход" else "🔁 Перевод") }
                    )
                }
            }
            MoneyField(amount, { amount = it }, "Сумма, ${settings.currency}")

            if (isNew && type == OpType.EXPENSE) {
                val templates = remember(categoryNames) {
                    listOf(
                        Triple("☕ Кофе", "250", categoryNames.firstOrNull { it.contains("кафе", true) || it.contains("еда", true) || it.contains("продукт", true) } ?: categoryNames.firstOrNull().orEmpty()),
                        Triple("🛒 Продукты", "1500", categoryNames.firstOrNull { it.contains("продукт", true) } ?: categoryNames.firstOrNull().orEmpty()),
                        Triple("🍽️ Обед", "500", categoryNames.firstOrNull { it.contains("кафе", true) || it.contains("продукт", true) } ?: categoryNames.firstOrNull().orEmpty()),
                        Triple("🚕 Такси", "400", categoryNames.firstOrNull { it.contains("транспорт", true) } ?: categoryNames.firstOrNull().orEmpty())
                    )
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { (label, sum, cat) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                amount = sum
                                if (cat.isNotBlank()) category = cat
                                comment = label.substringAfter(" ")
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (isTransfer) {
                ChipPicker("Со счёта", account, accountNames, { account = it })
                ChipPicker("На счёт", destAccount, accountNames, { destAccount = it })
            } else {
                ChipPicker(if (type == OpType.INCOME) "Откуда деньги" else "На что потратили", category, categoryNames, { category = it })
                TextField2(
                    value = category,
                    onChange = { category = it },
                    label = "Или введите свою категорию вручную"
                )
                ChipPicker("Счёт", account, accountNames, { account = it })
            }
            if (settings.persons.size > 1) {
                ChipPicker("Кто", person, settings.persons, { person = it })
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Когда", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = date == today, onClick = { date = today }, label = { Text("Сегодня") })
                    FilterChip(selected = date == today - 1, onClick = { date = today - 1 }, label = { Text("Вчера") })
                }
                DateField(date, { date = it })
            }
            TextField2(comment, { comment = it }, "Комментарий (необязательно)", singleLine = false)

            Button(
                onClick = {
                    vm.saveOperation(
                        initial.copy(
                            date = date,
                            amountCents = cents ?: 0L,
                            type = type,
                            category = if (isTransfer) "Перевод" else category,
                            comment = comment.trim(),
                            person = person,
                            account = account,
                            destAccount = if (isTransfer) destAccount else ""
                        )
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isNew) "Добавить" else "Сохранить") }

            if (!isNew) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Удалить операцию") }
            }
        }
    }
}
