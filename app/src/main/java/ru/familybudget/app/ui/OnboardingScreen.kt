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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.OnboardingData
import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.logic.Fmt

private class PayDraft(val name: String, val amount: String, val day: String)

private const val LAST_STEP = 5

/** Пошаговая первая настройка: имя, деньги сейчас, доходы, бюджет, обязательные платежи. */
@Composable
fun OnboardingScreen(vm: BudgetViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var partner by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("₽") }
    var card by rememberSaveable { mutableStateOf("") }
    var cash by rememberSaveable { mutableStateOf("") }
    var income by rememberSaveable { mutableStateOf("") }
    var recordIncome by rememberSaveable { mutableStateOf(false) }
    var savePct by rememberSaveable { mutableIntStateOf(10) }
    var budgetText by rememberSaveable { mutableStateOf("") }
    var distribute by rememberSaveable { mutableStateOf(true) }
    val payments = remember { mutableStateListOf<PayDraft>() }

    val incomeCents = Fmt.parseCents(income) ?: 0L
    val budgetCents = if (incomeCents > 0) incomeCents * (100 - savePct) / 100 else (Fmt.parseCents(budgetText) ?: 0L)

    BackHandler(enabled = step > 0) { step -= 1 }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Шаг ${step + 1} из ${LAST_STEP + 1}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { vm.skipOnboarding() }) { Text("Пропустить") }
            }
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / (LAST_STEP + 1) },
                modifier = Modifier.fillMaxWidth()
            )

            when (step) {
                0 -> {
                    Text("👋 Добро пожаловать!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Приложение поможет понять, куда уходят деньги, и спрогнозирует траты до конца месяца. Настройка займёт пару минут, всё можно изменить позже.")
                    TextField2(name, { name = it }, "Как вас зовут?")
                    TextField2(partner, { partner = it }, "Кто ещё ведёт бюджет? (необязательно)")
                    ChipPicker("Валюта", currency, listOf("₽", "$", "€", "₸", "₴", "Br"), { currency = it })
                }
                1 -> {
                    Text("💳 Сколько денег у вас сейчас?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HelpText("Это начальные остатки. Можно примерно, потом поправите в разделе «Счета».")
                    MoneyField(card, { card = it }, "На карте, $currency")
                    MoneyField(cash, { cash = it }, "Наличными, $currency")
                }
                2 -> {
                    Text("💼 Ваши доходы", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HelpText("Сколько в среднем приходит за месяц: зарплата, подработка, пособия, всё вместе. На это опирается прогноз.")
                    MoneyField(income, { income = it }, "Доход в месяц, $currency")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recordIncome, onCheckedChange = { recordIncome = it })
                        Text("Деньги уже пришли в этом месяце, записать как доход", modifier = Modifier.weight(1f))
                    }
                    if (recordIncome) HelpText("Мы запишем доход так, чтобы текущий баланс остался таким, как вы указали выше.")
                }
                3 -> {
                    Text("🎯 План трат на месяц", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (incomeCents > 0) {
                        HelpText("Сколько процентов дохода хотите откладывать? Остальное станет вашим бюджетом на месяц.")
                        ChipPicker("Откладывать", "$savePct%", listOf("0%", "10%", "20%", "30%"), { savePct = it.removeSuffix("%").toInt() })
                        Banner("На жизнь в месяц", Tone.INCOME) {
                            Text(Fmt.money(budgetCents, currency, false), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Откладывается: " + Fmt.money(incomeCents - budgetCents, currency, false))
                        }
                    } else {
                        HelpText("Вы не указали доход. Введите, сколько готовы тратить в месяц.")
                        MoneyField(budgetText, { budgetText = it }, "Бюджет на месяц, $currency")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = distribute, onCheckedChange = { distribute = it })
                        Text("Разделить бюджет по категориям автоматически", modifier = Modifier.weight(1f))
                    }
                }
                4 -> {
                    Text("🔁 Обязательные платежи", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    HelpText("То, что платится каждый месяц. Отмечайте оплату галочкой на главном экране, и расход запишется сам. Можно пропустить.")
                    ChipPicker(
                        "Быстро добавить", "",
                        listOf("Аренда", "ЖКХ", "Интернет", "Мобильная связь", "Кредит", "Спортзал"),
                        { payments.add(PayDraft(it, "", "1")) }
                    )
                    payments.forEachIndexed { i, p ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { payments.removeAt(i) }) { Text("Убрать") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MoneyField(p.amount, { payments[i] = PayDraft(p.name, it, p.day) }, "Сумма, $currency", Modifier.weight(2f))
                                OutlinedTextField(
                                    value = p.day,
                                    onValueChange = { v -> if (v.length <= 2 && v.all { c -> c.isDigit() }) payments[i] = PayDraft(p.name, p.amount, v) },
                                    label = { Text("День") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text("✅ Всё готово!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Banner("Ваши данные", Tone.NEUTRAL) {
                        KeyValueRow("Имя", name.ifBlank { "—" })
                        KeyValueRow("Сейчас на счетах", Fmt.money((Fmt.parseCents(card) ?: 0L) + (Fmt.parseCents(cash) ?: 0L), currency, false))
                        KeyValueRow("Доход в месяц", Fmt.money(incomeCents, currency, false))
                        KeyValueRow("Бюджет на месяц", Fmt.money(budgetCents, currency, false))
                        KeyValueRow("Обязательных платежей", payments.count { it.name.isNotBlank() && (Fmt.parseCents(it.amount) ?: 0L) > 0 }.toString())
                    }
                    HelpText("Дальше добавляйте расходы кнопкой «+». На главном экране появятся прогноз и подсказки.")
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
                    ) { Text("Далее") }
                } else {
                    Button(
                        onClick = {
                            val list = payments.mapNotNull { p ->
                                val cents = Fmt.parseCents(p.amount) ?: 0L
                                val day = p.day.toIntOrNull() ?: 1
                                if (p.name.isNotBlank() && cents > 0) {
                                    Mandatory(name = p.name, day = day.coerceIn(1, 31), amountCents = cents)
                                } else null
                            }
                            vm.completeOnboarding(
                                OnboardingData(
                                    name = name, partner = partner, currency = currency,
                                    cardCents = Fmt.parseCents(card) ?: 0L,
                                    cashCents = Fmt.parseCents(cash) ?: 0L,
                                    incomeCents = incomeCents, recordIncome = recordIncome,
                                    budgetCents = budgetCents, payments = list,
                                    distributeLimits = distribute
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
