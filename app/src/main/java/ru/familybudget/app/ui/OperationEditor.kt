package ru.familybudget.app.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    BackHandler { vm.closeEditor() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.closeEditor() }) { Icon(Icons.Default.Close, contentDescription = "Закрыть") }
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
                        label = { Text(t) }
                    )
                }
            }
            MoneyField(amount, { amount = it }, "Сумма, ${settings.currency}")
            if (isTransfer) {
                Picker("Со счёта", account, accountNames, { account = it })
                Picker("На счёт", destAccount, accountNames, { destAccount = it })
            } else {
                Picker("Категория", category, categoryNames, { category = it })
                Picker("Счёт", account, accountNames, { account = it })
            }
            Picker("Кто", person, settings.persons, { person = it })
            DateField(date, { date = it })
            TextField2(comment, { comment = it }, "Комментарий", singleLine = false)

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
                    onClick = { vm.deleteOperation(initial) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Удалить операцию") }
            }
        }
    }
}
