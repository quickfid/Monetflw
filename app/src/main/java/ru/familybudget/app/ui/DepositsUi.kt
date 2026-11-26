package ru.familybudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.data.Deposit
import ru.familybudget.app.data.InterestEntry
import ru.familybudget.app.logic.Deposits
import ru.familybudget.app.logic.Fmt
import java.time.YearMonth

/** Страница «Вклады и накопления»: список, добавление, исправление суммы и процентов. */
@Composable
fun DepositsPage(vm: BudgetViewModel) {
    val deposits by vm.deposits.collectAsState()
    val interest by vm.interest.collectAsState()
    val cur = vm.settings.collectAsState().value.currency
    var editing by remember { mutableStateOf<Deposit?>(null) }
    var history by remember { mutableStateOf<Deposit?>(null) }
    val incomeColor = AppColors.income()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (deposits.isEmpty()) EmptyHint("Добавьте вклад или накопительный счёт 🐷")
        deposits.forEach { d ->
            val info = Deposits.info(d, interest)
            Column(
                Modifier.fillMaxWidth().border(1.dp, Sheet.border()).background(MaterialTheme.colorScheme.surface)
                    .clickable { editing = d }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🐷 " + d.name, fontWeight = FontWeight.Bold)
                Text(d.kind + " · " + Deposits.rateText(d), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                KeyValueRow("Сумма сейчас", Fmt.money(info.balance, cur))
                KeyValueRow("Проценты в этом месяце ≈", "+" + Fmt.money(info.expectedMonthly, cur), incomeColor)
                KeyValueRow("Уже пришло процентов", "+" + Fmt.money(info.received, cur), incomeColor)
                Row {
                    TextButton(onClick = { history = d }) { Text("История процентов") }
                    TextButton(onClick = { editing = d }) { Text("Изменить") }
                }
            }
        }
        OutlinedButton(
            onClick = {
                editing = Deposit(name = "", principalCents = 0, baselineYm = YearMonth.now().toString(), ratePercent = 0.0)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("  Добавить вклад или счёт")
        }
    }

    editing?.let { d ->
        val isNew = d.id == 0L
        val info = Deposits.info(d, interest)
        var name by remember(d) { mutableStateOf(d.name) }
        var kind by remember(d) { mutableStateOf(d.kind) }
        var balance by remember(d) { mutableStateOf(if (isNew) "" else Fmt.plain(info.balance)) }
        var rate by remember(d) { mutableStateOf(if (d.ratePercent > 0) d.ratePercent.toString().replace('.', ',') else "") }
        var period by remember(d) { mutableStateOf(if (d.ratePeriod == 1) "в месяц" else "в год") }
        var compound by remember(d) { mutableStateOf(d.compound) }
        val balanceCents = Fmt.parseCents(balance) ?: 0L
        val rateValue = rate.replace(',', '.').toDoubleOrNull() ?: 0.0
        val draft = d.copy(
            name = name.trim(), kind = kind, ratePercent = rateValue,
            ratePeriod = if (period == "в месяц") 1 else 0, compound = compound
        )
        val preview = Math.round((if (compound) balanceCents else balanceCents) * Deposits.monthlyRate(draft))
        FormDialog(
            title = if (isNew) "Новый вклад" else "Вклад",
            onDismiss = { editing = null },
            confirmEnabled = name.isNotBlank() && rateValue >= 0.0,
            onConfirm = {
                val newBalance: Long? = if (isNew) balanceCents else if (balanceCents != info.balance) balanceCents else null
                vm.saveDeposit(draft, newBalance)
                editing = null
            },
            onDelete = if (!isNew) ({ vm.deleteDeposit(d); editing = null }) else null
        ) {
            TextField2(name, { name = it }, "Название (банк, счёт)")
            ChipPicker("Тип", kind, listOf("Вклад", "Накопительный счёт"), { kind = it })
            MoneyField(balance, { balance = it }, "Сумма сейчас, $cur")
            OutlinedTextField(
                value = rate,
                onValueChange = { v -> if (Regex("^\\d{0,3}([.,]\\d{0,2})?$").matches(v)) rate = v },
                label = { Text("Процент, %") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            ChipPicker("Ставка", period, listOf("в год", "в месяц"), { period = it })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = compound, onCheckedChange = { compound = it })
                Text("Проценты прибавляются к сумме", Modifier.weight(1f), fontSize = 13.sp)
            }
            Text("Примерно в месяц: +" + Fmt.money(preview, cur), fontSize = 13.sp, color = incomeColor)
            if (!isNew) Text("Если сумма на самом деле другая, просто впишите верную.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    history?.let { d ->
        val info = Deposits.info(d, interest)
        var fixing by remember(d) { mutableStateOf<InterestEntry?>(null) }
        AlertDialog(
            onDismissRequest = { history = null },
            title = { Text("Проценты · " + d.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Ожидается в этом месяце: +" + Fmt.money(info.expectedMonthly, cur),
                        color = incomeColor, fontWeight = FontWeight.SemiBold
                    )
                    if (info.entries.isEmpty()) {
                        Text("Первые проценты придут после конца месяца.", fontSize = 13.sp)
                    }
                    info.entries.reversed().forEach { e ->
                        Row(
                            Modifier.fillMaxWidth().clickable { fixing = e }.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Fmt.monthTitle(YearMonth.parse(e.ym)) + if (e.manual) " ✍️" else "")
                            Text("+" + Fmt.money(e.amountCents, cur), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (info.entries.isNotEmpty()) Text("Нажмите на месяц, чтобы исправить сумму.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { vm.recalcInterest(d) }) { Text("Пересчитать") } },
            dismissButton = { TextButton(onClick = { history = null }) { Text("Закрыть") } }
        )
        fixing?.let { e ->
            var amount by remember(e) { mutableStateOf(Fmt.plain(e.amountCents)) }
            FormDialog(
                title = Fmt.monthTitle(YearMonth.parse(e.ym)),
                onDismiss = { fixing = null },
                confirmEnabled = Fmt.parseCents(amount) != null,
                onConfirm = { vm.saveInterest(e, Fmt.parseCents(amount) ?: 0L); fixing = null }
            ) {
                MoneyField(amount, { amount = it }, "Пришло процентов, $cur")
            }
        }
    }
}
