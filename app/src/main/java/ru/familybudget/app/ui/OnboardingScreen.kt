package ru.familybudget.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.OnboardingData
import ru.familybudget.app.OnboardingDeposit
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Plans

private const val LAST_STEP = 2

/** Упрощённая стартовая настройка в 3 шага: деньги сейчас → вклады и подушка → лимиты с быстрыми чипами. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: BudgetViewModel) {
    val categories by vm.categories.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var partner by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("₽") }
    var card by rememberSaveable { mutableStateOf("") }
    var cash by rememberSaveable { mutableStateOf("") }
    var income by rememberSaveable { mutableStateOf("") }
    var recordIncome by rememberSaveable { mutableStateOf(false) }
    var billsAlreadyPaid by rememberSaveable { mutableStateOf(false) }
    var savingsPercent by rememberSaveable { mutableIntStateOf(10) }

    // Вклады на старте (Шаг 2)
    val deposits = remember { mutableStateListOf<OnboardingDeposit>() }
    var depName by rememberSaveable { mutableStateOf("") }
    var depAmount by rememberSaveable { mutableStateOf("") }
    var depRate by rememberSaveable { mutableStateOf("18") }

    val coreGoals = remember {
        listOf(
            "Аренда квартиры",
            "Коммунальные платежи",
            "Кредит",
            "Продукты",
            "Транспорт и бензин",
            "Связь и интернет",
            "Развлечения и досуг"
        )
    }
    val names = remember(categories) {
        val rest = Plans.orderedExpenseCategories(categories).filter { it !in coreGoals }
        coreGoals + rest
    }
    val values = remember { mutableStateMapOf<String, String>() }

    val incomeCents = Fmt.parseCents(income) ?: 0L
    val cushionCents = incomeCents * savingsPercent / 100L
    val total = names.sumOf { Fmt.parseCents(values[it].orEmpty()) ?: 0L }
    val free = incomeCents - total - cushionCents

    fun addDelta(current: String, deltaRub: Long): String {
        val curRub = (Fmt.parseCents(current) ?: 0L) / 100L
        return (curRub + deltaRub).coerceAtLeast(0L).toString()
    }

    fun applyPreset(mode: String) {
        val baseRub = (incomeCents / 100L).takeIf { it > 0L } ?: 100_000L
        val mult = when (mode) {
            "eco" -> 0.75
            "comfort" -> 0.95
            else -> 0.85
        }
        val avail = (baseRub * mult).toLong()
        fun roundK(v: Double): String = (((v / 1000.0).toLong()).coerceAtLeast(1L) * 1000L).toString()
        values["Аренда квартиры"] = roundK(avail * 0.30)
        values["Коммунальные платежи"] = roundK(avail * 0.06)
        values["Продукты"] = roundK(avail * 0.25)
        values["Транспорт и бензин"] = roundK(avail * 0.08)
        values["Связь и интернет"] = roundK(avail * 0.03)
        values["Развлечения и досуг"] = roundK(avail * 0.10)
    }

    BackHandler(enabled = step > 0) { step -= 1 }

    Box(Modifier.fillMaxSize().background(AppColors.ambientBackground())) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🚀 Шаг ${step + 1} из ${LAST_STEP + 1}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { vm.skipOnboarding() }) { Text("Пропустить") }
            }
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / (LAST_STEP + 1) },
                modifier = Modifier.fillMaxWidth()
            )

            when (step) {
                0 -> {
                    Text("Привет! Настроим бюджет за минуту", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Sheet.glass(),
                        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextField2(name, { name = it }, "Ваше имя")
                            TextField2(partner, { partner = it }, "Партнёр / семья (необязательно)")
                            ChipPicker("Валюта", currency, listOf("₽", "$", "€", "₸", "₴", "Br"), { currency = it })
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Sheet.glass(),
                        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("💰 Деньги сейчас", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            AmountField(card, { card = it }, "На карте ($currency)")
                            QuickChips(listOf(5_000L, 10_000L, 20_000L, 50_000L)) { d -> card = addDelta(card, d) }
                            AmountField(cash, { cash = it }, "Наличными ($currency)")
                            AmountField(income, { income = it }, "Ежемесячный доход ($currency)")
                            QuickChips(listOf(50_000L, 100_000L, 150_000L, 200_000L)) { d -> income = addDelta(income, d) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = recordIncome, onCheckedChange = { recordIncome = it })
                                Text("Записать этот доход в текущий месяц", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                1 -> {
                    Text("🏦 Вклады и подушка безопасности", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Добавьте текущие вклады или накопительные счета — они будут считаться отдельно от свободных денег на траты.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Sheet.glass(),
                        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextField2(depName, { depName = it }, "Название (например, Накопительный счёт)")
                            AmountField(depAmount, { depAmount = it }, "Сумма на вкладе ($currency)")
                            QuickChips(listOf(10_000L, 50_000L, 100_000L, 250_000L)) { d -> depAmount = addDelta(depAmount, d) }
                            AmountField(depRate, { depRate = it }, "Ставка % годовых (например, 18)")
                            OutlinedButton(
                                onClick = {
                                    val cents = Fmt.parseCents(depAmount) ?: 0L
                                    val rate = depRate.replace(',', '.').toDoubleOrNull() ?: 0.0
                                    if (cents > 0L) {
                                        deposits.add(
                                            OnboardingDeposit(
                                                name = depName.trim().ifBlank { "Вклад ${deposits.size + 1}" },
                                                principalCents = cents,
                                                ratePercent = rate
                                            )
                                        )
                                        depName = ""
                                        depAmount = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Добавить вклад")
                            }
                            if (deposits.isNotEmpty()) {
                                HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                                deposits.forEachIndexed { idx, d ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(d.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${Fmt.money(d.principalCents, currency, false)} · ${d.ratePercent}% годовых",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = { deposits.removeAt(idx) }) { Text("Убрать") }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Sheet.glass(),
                        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ Откладывать на подушку безопасности", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 10, 15, 20).forEach { pct ->
                                    FilterChip(
                                        selected = savingsPercent == pct,
                                        onClick = { savingsPercent = pct },
                                        label = { Text("$pct%") }
                                    )
                                }
                            }
                            if (incomeCents > 0L) {
                                Text(
                                    "Это ${Fmt.money(cushionCents, currency, false)} в месяц",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.income(),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                else -> {
                    Text("🎯 Лимиты на месяц", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Выберите готовый шаблон в 1 касание или наберите суммы быстрыми кнопками:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = false, onClick = { applyPreset("eco") }, label = { Text("🌱 Экономный") })
                        FilterChip(selected = false, onClick = { applyPreset("balanced") }, label = { Text("⚖️ Сбалансированный") })
                        FilterChip(selected = false, onClick = { applyPreset("comfort") }, label = { Text("✨ Комфортный") })
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Sheet.tone(if (free >= 0) Tone.INCOME else Tone.EXPENSE),
                        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Запланировано: ${Fmt.money(total, currency, false)}", fontWeight = FontWeight.SemiBold)
                            if (incomeCents > 0L) {
                                Text("Подушка ($savingsPercent%): ${Fmt.money(cushionCents, currency, false)}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (free >= 0) "Свободный остаток: ${Fmt.money(free, currency, false)}"
                                    else "Превышение на: ${Fmt.money(-free, currency, false)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (free >= 0) AppColors.income() else AppColors.expense()
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = billsAlreadyPaid, onCheckedChange = { billsAlreadyPaid = it })
                        Text(
                            "Обязательные платежи в этом месяце уже оплачены (не вычитать из текущего баланса)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Sheet.glass(),
                        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            names.forEach { cat ->
                                val curVal = values[cat].orEmpty()
                                AmountField(curVal, { values[cat] = it }, cat)
                                QuickChips(listOf(5_000L, 10_000L, 20_000L)) { d ->
                                    values[cat] = addDelta(values[cat].orEmpty(), d)
                                }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) {
                    OutlinedButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f)) {
                        Text("Назад")
                    }
                }
                Button(
                    onClick = {
                        if (step < LAST_STEP) {
                            step += 1
                        } else {
                            val plan = names.mapNotNull { n ->
                                val c = Fmt.parseCents(values[n].orEmpty()) ?: 0L
                                if (c > 0L) n to c else null
                            }.toMap()
                            vm.completeOnboarding(
                                OnboardingData(
                                    name = name,
                                    partner = partner,
                                    currency = currency,
                                    cardCents = Fmt.parseCents(card) ?: 0L,
                                    cashCents = Fmt.parseCents(cash) ?: 0L,
                                    incomeCents = incomeCents,
                                    recordIncome = recordIncome,
                                    plan = plan,
                                    billsAlreadyPaid = billsAlreadyPaid,
                                    savingsPercent = savingsPercent,
                                    initialDeposits = deposits.toList()
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (step < LAST_STEP) "Дальше →" else "Готово! 🚀")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickChips(deltas: List<Long>, onAdd: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        deltas.forEach { d ->
            val label = "+${d / 1000}к"
            Surface(
                shape = RoundedCornerShape(50),
                color = Sheet.glass(),
                border = BorderStroke(1.dp, Sheet.glassBorderBrush()),
                modifier = Modifier.clickable { onAdd(d) }
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
