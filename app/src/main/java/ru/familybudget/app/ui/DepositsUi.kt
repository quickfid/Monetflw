package ru.familybudget.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
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
fun DepositsPage(vm: BudgetViewModel, padding: PaddingValues = PaddingValues(0.dp)) {
    val deposits by vm.deposits.collectAsState()
    val interest by vm.interest.collectAsState()
    val mandatory by vm.mandatory.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    var editing by remember { mutableStateOf<Deposit?>(null) }
    var history by remember { mutableStateOf<Deposit?>(null) }
    var editingGoal by remember { mutableStateOf<ru.familybudget.app.data.SavingsGoal?>(null) }
    val incomeColor = AppColors.income()
    val savingsColor = AppColors.savings()
    val infos = remember(deposits, interest) { deposits.map { Deposits.info(it, interest) } }
    val totalSavings = remember(infos) { infos.sumOf { it.balance } }
    val totalExpected = remember(infos) { infos.sumOf { it.expectedMonthly } }
    val totalReceived = remember(infos) { infos.sumOf { it.received } }
    val monthlyBase = remember(settings.monthlyBudget, mandatory) {
        val mSum = mandatory.sumOf { it.amountCents }
        if (settings.monthlyBudget > 0) settings.monthlyBudget else mSum
    }
    val cushionGoal = monthlyBase * 3L

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (infos.isNotEmpty()) {
            Banner("Общая сумма накоплений", Tone.SAVINGS) {
                Text(
                    Fmt.money(totalSavings, cur),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = savingsColor
                )
                KeyValueRow("Ожидается процентов в этом месяце", "+" + Fmt.money(totalExpected, cur), incomeColor)
                KeyValueRow("Всего начислено процентов", "+" + Fmt.money(totalReceived, cur), incomeColor)
            }
        }

        // Стратегии и советы по накоплениям (игровой вызов — сразу на видном месте)
        val activeStrategy = settings.activeStrategy
        val ops by vm.operations.collectAsState()
        val nowYm = remember { YearMonth.now() }
        val monthOps = remember(ops, nowYm) { ru.familybudget.app.logic.Stats.opsInMonth(ops, nowYm) }
        val monthIncome = remember(monthOps) { monthOps.filter { it.type == ru.familybudget.app.data.OpType.INCOME }.sumOf { it.amountCents } }
        val monthExpense = remember(monthOps) { monthOps.filter { it.type == ru.familybudget.app.data.OpType.EXPENSE }.sumOf { it.amountCents } }
        val savedThisMonth = (monthIncome - monthExpense).coerceAtLeast(0L)
        val daysLogged = remember(monthOps) { monthOps.map { it.date }.distinct().size }

        val strategies = remember(settings.savingsPercent) {
            listOf(
                Triple("pay_yourself", "1. Сначала заплати себе", "Откладывайте ${settings.savingsPercent}% от дохода сразу в день получения, до основных трат."),
                Triple("rule_50_30_20", "2. Правило 50 / 30 / 20", "50% дохода — на нужды, 30% — на желания, 20% — в накопления и вклады."),
                Triple("rule_24h", "3. Правило 24 часов", "Откладывайте незапланированную покупку на сутки — половина импульсивных трат отпадёт сама."),
                Triple("weekly_round", "4. Округление в конце недели", "Каждое воскресенье переводите «хвостик» с карты на накопительный счёт.")
            )
        }

        if (activeStrategy.isNotBlank()) {
            val activeInfo = strategies.firstOrNull { it.first == activeStrategy }
            if (activeInfo != null) {
                val targetPct = when (activeStrategy) {
                    "rule_50_30_20" -> 20
                    "pay_yourself" -> settings.savingsPercent.coerceAtLeast(5)
                    else -> 0
                }
                val progressVal = when (activeStrategy) {
                    "pay_yourself", "rule_50_30_20" -> {
                        val goal = (if (monthIncome > 0) monthIncome else settings.monthlyIncome) * targetPct / 100L
                        if (goal > 0) (savedThisMonth.toFloat() / goal).coerceIn(0f, 1f) else 0f
                    }
                    else -> (daysLogged.toFloat() / 14f).coerceIn(0f, 1f)
                }
                val animatedStrat by animateFloatAsState(
                    targetValue = progressVal,
                    animationSpec = tween(550),
                    label = "strategyProgress"
                )
                val pctDone = (progressVal * 100).toInt()
                Banner("🎮 Активный вызов: ${activeInfo.second}", Tone.INCOME) {
                    Text(activeInfo.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { animatedStrat },
                        modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp))
                    )
                    KeyValueRow("Прогресс месяца", "$pctDone%", incomeColor)
                    TextButton(onClick = { vm.setStrategy("") }) { Text("Отказаться от вызова") }
                }
            }
        }

        if (deposits.isEmpty()) {
            EmptyHint("Добавьте банковский вклад или накопительный счёт, чтобы видеть общую сумму накоплений и начисленные проценты.")
        }
        infos.forEach { info ->
            val d = info.deposit
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { editing = d },
                shape = RoundedCornerShape(20.dp),
                color = Sheet.glass(),
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(d.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(d.kind + " · " + Deposits.rateText(d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    KeyValueRow("Сумма сейчас", Fmt.money(info.balance, cur))
                    KeyValueRow("Проценты в этом месяце ≈", "+" + Fmt.money(info.expectedMonthly, cur), incomeColor)
                    KeyValueRow("Уже пришло процентов", "+" + Fmt.money(info.received, cur), incomeColor)
                    Row {
                        TextButton(onClick = { history = d }) { Text("История процентов") }
                        TextButton(onClick = { editing = d }) { Text("Изменить") }
                    }
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

        Section("🎮 Стратегии накоплений (выберите вызов)") {
            strategies.forEachIndexed { idx, (id, title, desc) ->
                if (idx > 0) HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                val isActive = activeStrategy == id
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isActive) {
                            TextButton(onClick = { vm.setStrategy("") }) { Text("Отказаться") }
                        } else {
                            OutlinedButton(onClick = { vm.setStrategy(id) }) { Text("Принять вызов 🎮") }
                        }
                    }
                }
            }
        }

        // Финансовая подушка (отдельный независимый резерв, не связанный со вкладами)
        var cushionDialogMode by remember { mutableStateOf<String?>(null) } // "add" | "set"
        var cushionInput by remember { mutableStateOf("") }
        if (cushionDialogMode != null) {
            val isAdd = cushionDialogMode == "add"
            AlertDialog(
                onDismissRequest = { cushionDialogMode = null },
                title = { Text(if (isAdd) "🛡️ Пополнить подушку" else "🛡️ Сумма в подушке") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (isAdd) "Сколько вы внесли в финансовую подушку?"
                            else "Укажите текущую / стартовую сумму в вашей финансовой подушке (независимо от вкладов):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MoneyField(cushionInput, { cushionInput = it }, "Сумма ($cur)")
                        if (isAdd) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5_000L, 10_000L, 20_000L).forEach { v ->
                                    TextButton(onClick = {
                                        val curRub = (Fmt.parseCents(cushionInput) ?: 0L) / 100L
                                        cushionInput = (curRub + v).toString()
                                    }) { Text("+${v / 1000}к") }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val cents = Fmt.parseCents(cushionInput) ?: 0L
                        if (isAdd) vm.addToCushion(cents) else vm.setCushionCents(cents)
                        cushionDialogMode = null
                    }) { Text(if (isAdd) "Внести" else "Сохранить") }
                },
                dismissButton = {
                    TextButton(onClick = { cushionDialogMode = null }) { Text("Отмена") }
                }
            )
        }

        val cushionNow = settings.cushionCents
        val fraction = if (cushionGoal > 0) (cushionNow.toFloat() / cushionGoal).coerceIn(0f, 1f) else 0f
        val animatedCushion by animateFloatAsState(
            targetValue = fraction,
            animationSpec = tween(500),
            label = "cushionProgress"
        )
        val monthsCovered = if (monthlyBase > 0) cushionNow.toDouble() / monthlyBase else 0.0
        Section("🛡️ Финансовая подушка (цель: 3 месяца)") {
            KeyValueRow("Накоплено в подушке", Fmt.money(cushionNow, cur, false), savingsColor)
            if (cushionGoal > 0) {
                LinearProgressIndicator(
                    progress = { animatedCushion },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = savingsColor
                )
                KeyValueRow(
                    "Покрыто месяцев",
                    String.format(java.util.Locale.getDefault(), "%.1f из 3,0 мес.", monthsCovered),
                    savingsColor
                )
                KeyValueRow("Цель (3 месяца бюджета)", Fmt.money(cushionGoal, cur, false))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { cushionInput = ""; cushionDialogMode = "add" },
                    modifier = Modifier.weight(1f)
                ) { Text("+ Пополнить") }
                TextButton(
                    onClick = {
                        cushionInput = if (cushionNow > 0) (cushionNow / 100L).toString() else ""
                        cushionDialogMode = "set"
                    }
                ) { Text("Изменить") }
            }
        }

        // Финансовые цели (копилки на мечту)
        Section("🎯 Финансовые цели") {
            if (settings.goals.isEmpty()) {
                HelpText("Создайте копилку на конкретную цель — например, отпуск, ремонт или новый телефон.")
            } else {
                settings.goals.forEachIndexed { idx, g ->
                    if (idx > 0) HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                    val gRatio = if (g.targetCents > 0) (g.savedCents.toFloat() / g.targetCents).coerceIn(0f, 1f) else 0f
                    val gPct = (gRatio * 100).toInt()
                    Column(
                        Modifier.fillMaxWidth().clickable { editingGoal = g },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(g.title, fontWeight = FontWeight.SemiBold)
                            Text("$gPct%", color = savingsColor, fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(
                            progress = { gRatio },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = savingsColor
                        )
                        KeyValueRow("Накоплено", "${Fmt.money(g.savedCents, cur, false)} из ${Fmt.money(g.targetCents, cur, false)}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { vm.saveGoal(g.copy(savedCents = g.savedCents + 100_000L)) }) { Text("+1 000") }
                            TextButton(onClick = { vm.saveGoal(g.copy(savedCents = g.savedCents + 500_000L)) }) { Text("+5 000") }
                            TextButton(onClick = { editingGoal = g }) { Text("Изменить") }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { editingGoal = ru.familybudget.app.data.SavingsGoal(0L, "", 0L, 0L) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  Добавить цель")
            }
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
        val preview = Math.round(balanceCents * Deposits.monthlyRate(draft))
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

    editingGoal?.let { g ->
        val isNewGoal = g.id == 0L
        var title by remember(g) { mutableStateOf(g.title) }
        var target by remember(g) { mutableStateOf(if (g.targetCents > 0) Fmt.plain(g.targetCents) else "") }
        var saved by remember(g) { mutableStateOf(if (g.savedCents > 0) Fmt.plain(g.savedCents) else "") }
        val targetCents = Fmt.parseCents(target) ?: 0L
        val savedCents = Fmt.parseCents(saved) ?: 0L
        FormDialog(
            title = if (isNewGoal) "Новая цель" else g.title,
            onDismiss = { editingGoal = null },
            confirmEnabled = title.isNotBlank() && targetCents > 0L,
            onConfirm = {
                vm.saveGoal(g.copy(title = title.trim(), targetCents = targetCents, savedCents = savedCents))
                editingGoal = null
            },
            onDelete = if (!isNewGoal) ({ vm.deleteGoal(g.id); editingGoal = null }) else null
        ) {
            TextField2(title, { title = it }, "Название цели (например, Отпуск)")
            MoneyField(target, { target = it }, "Нужная сумма, $cur")
            MoneyField(saved, { saved = it }, "Уже накоплено, $cur")
        }
    }
}
