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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Plans

private const val LAST_STEP = 4

/** Короткая стартовая настройка в виде игры: пять шагов, на выходе — готовый план на месяц. */
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
    val names = remember(categories) { Plans.orderedExpenseCategories(categories) }
    val values = remember { mutableStateMapOf<String, String>() }

    val incomeCents = Fmt.parseCents(income) ?: 0L
    val total = names.sumOf { Fmt.parseCents(values[it].orEmpty()) ?: 0L }
    val free = incomeCents - total

    BackHandler(enabled = step > 0) { step -= 1 }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🎮 Уровень ${step + 1} из ${LAST_STEP + 1}", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { vm.skipOnboarding() }) { Text("Пропустить") }
            }
            LinearProgressIndicator(progress = { (step + 1).toFloat() / (LAST_STEP + 1) }, modifier = Modifier.fillMaxWidth())

            when (step) {
                0 -> {
                    Text("🐣 Привет!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Настроим бюджет за минуту. Прокачивайте уровень, сохраняйте серию дней и открывайте награды.")
                    TextField2(name, { name = it }, "Ваше имя")
                    TextField2(partner, { partner = it }, "Кто ещё с вами? (необязательно)")
                    ChipPicker("Валюта", currency, listOf("₽", "$", "€", "₸", "₴", "Br"), { currency = it })
                }
                1 -> {
                    Text("💳 Сколько денег сейчас?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    MoneyField(card, { card = it }, "На карте, $currency")
                    MoneyField(cash, { cash = it }, "Наличными, $currency")
                }
                2 -> {
                    Text("💼 Ваш доход в месяц", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    MoneyField(income, { income = it }, "Всего за месяц, $currency")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recordIncome, onCheckedChange = { recordIncome = it })
                        Text("Деньги уже пришли в этом месяце", Modifier.weight(1f))
                    }
                }
                3 -> {
                    Text("📋 План на месяц", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Сколько уйдёт на каждое. Можно оставить пустым.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PlanFields(names, values, currency)
                    Banner("Итого", if (incomeCents > 0 && free < 0) Tone.EXPENSE else Tone.INCOME) {
                        KeyValueRow("План", Fmt.money(total, currency, false))
                        if (incomeCents > 0) {
                            KeyValueRow(if (free >= 0) "Свободно" else "Не хватает", Fmt.money(kotlin.math.abs(free), currency, false))
                        }
                    }
                }
                else -> {
                    Text("🚀 Готово!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Banner("Ваш старт", Tone.NEUTRAL) {
                        KeyValueRow("Имя", name.ifBlank { "—" })
                        KeyValueRow("Сейчас", Fmt.money((Fmt.parseCents(card) ?: 0L) + (Fmt.parseCents(cash) ?: 0L), currency, false))
                        KeyValueRow("Доход", Fmt.money(incomeCents, currency, false))
                        KeyValueRow("План", Fmt.money(total, currency, false))
                    }
                    Text("Первое задание: запишите сегодняшние траты кнопкой «+».")
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) {
                    OutlinedButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f)) { Text("Назад") }
                }
                if (step < LAST_STEP) {
                    Button(
                        onClick = { step += 1 },
                        enabled = step != 0 || name.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Дальше") }
                } else {
                    Button(
                        onClick = {
                            val plan = HashMap<String, Long>()
                            names.forEach { n ->
                                val c = Fmt.parseCents(values[n].orEmpty()) ?: 0L
                                if (c > 0) plan[n] = c
                            }
                            vm.completeOnboarding(
                                OnboardingData(
                                    name = name, partner = partner, currency = currency,
                                    cardCents = Fmt.parseCents(card) ?: 0L,
                                    cashCents = Fmt.parseCents(cash) ?: 0L,
                                    incomeCents = incomeCents, recordIncome = recordIncome, plan = plan
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Начать!") }
                }
            }
        }
    }
}
