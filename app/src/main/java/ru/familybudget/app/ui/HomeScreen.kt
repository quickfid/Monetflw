package ru.familybudget.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.data.OpType
import ru.familybudget.app.logic.Deposits
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Game
import ru.familybudget.app.logic.Plans
import ru.familybudget.app.logic.Verdict
import ru.familybudget.app.logic.delta
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: BudgetViewModel, padding: PaddingValues) {
    val ops by vm.operations.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val mandatory by vm.mandatory.collectAsState()
    val categories by vm.categories.collectAsState()
    val plans by vm.plans.collectAsState()
    val deposits by vm.deposits.collectAsState()
    val interest by vm.interest.collectAsState()
    val settings by vm.settings.collectAsState()
    val unsynced by vm.unsyncedCount.collectAsState()
    val cur = settings.currency
    val ym = vm.month
    val today = remember { LocalDate.now() }

    val md = remember(ops, accounts, mandatory, plans, categories, settings, ym) {
        Plans.monthData(ops, accounts, mandatory, plans, categories, settings, ym, today)
    }
    val game = remember(ops, plans, mandatory) { Game.compute(ops, plans, mandatory, today) }
    val balance = remember(accounts, ops) { accounts.sumOf { it.initialCents } + ops.sumOf { it.delta() } }
    val depositInfos = remember(deposits, interest) { deposits.map { Deposits.info(it, interest) } }
    val savings = remember(depositInfos) { depositInfos.sumOf { it.balance } }
    val expectedInterest = remember(depositInfos) { depositInfos.sumOf { it.expectedMonthly } }
    val recentOps = remember(md.ops) { md.ops.sortedWith(compareByDescending<ru.familybudget.app.data.Operation> { it.date }.thenByDescending { it.id }).take(5) }

    val f = md.forecast
    val incomeColor = AppColors.income()
    val expenseColor = AppColors.expense()
    val warnColor = AppColors.warning()
    val savingsColor = AppColors.savings()
    val todayDay = today.toEpochDay()
    val paidCount = mandatory.count { it.id in md.paid }
    val canPlan = !f.isPast

    val tip = remember(md, f, savings, cur) {
        when {
            !md.hasPlan && canPlan -> "Составьте план на месяц — так приложение сможет точно подсказывать, сколько безопасно тратить каждый день."
            f.isCurrent && f.verdict == Verdict.BAD -> "Темп трат выше плана. Попробуйте держаться в пределах ${Fmt.money(f.safeDaily, cur, false)} в день, чтобы вернуться в бюджет."
            f.isCurrent && f.verdict == Verdict.WARN -> "Расходы близки к границе плана. Проверьте необязательные покупки на ближайшие дни."
            f.isCurrent && md.summary.mandatoryUnpaid > 0 -> "В этом месяце остались неоплаченные обязательные платежи на ${Fmt.money(md.summary.mandatoryUnpaid, cur, false)}."
            savings == 0L && md.summary.closing > 0 -> "У вас есть свободный остаток. Даже небольшая сумма на накопительном счёте создаёт финансовую подушку."
            f.isCurrent && f.verdict == Verdict.GOOD -> "Отличный темп! Вы уверенно укладываетесь в бюджет и можете спокойно тратить до ${Fmt.money(f.safeDaily, cur, false)} в день."
            else -> "Записывайте покупки сразу — это занимает пару секунд и помогает видеть реальную картину месяца."
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Приветствие и компактный статус прогресса
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Sheet.border()),
                shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (settings.userName.isBlank()) "Добрый день!" else "Привет, ${settings.userName}!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${game.avatar} Уровень ${game.level} · ${game.title} · ${game.xp} XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Sheet.header()
                        ) {
                            Text(
                                "🔥 ${game.streak} дн.",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    val animatedXp by animateFloatAsState(
                        targetValue = game.progress,
                        animationSpec = tween(450),
                        label = "xpProgress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedXp },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Быстрые действия в одно нажатие
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.startNew(OpType.EXPENSE) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("− Расход (+10 XP)", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = { vm.startNew(OpType.INCOME) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("+ Доход", maxLines = 1, softWrap = false)
                    }
                }
                if (settings.apiUrl.isNotBlank() && settings.token.isNotBlank()) {
                    OutlinedButton(
                        onClick = { vm.syncAll() },
                        enabled = !vm.syncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            when {
                                vm.syncing -> "⏳ Синхронизация с таблицей..."
                                unsynced > 0 -> "☁️ Отправить в таблицу ($unsynced)"
                                else -> "☁️ Синхронизировать с таблицей"
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Переключатель месяца
        item {
            MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
        }

        // 2. Краткий финансовый статус, прогноз и дневной лимит
        item {
            val tone = when (f.verdict) {
                Verdict.GOOD -> Tone.INCOME
                Verdict.WARN -> Tone.WARN
                Verdict.BAD -> Tone.EXPENSE
                Verdict.NONE -> Tone.NEUTRAL
            }
            val title = when {
                f.isPast -> "Итоги месяца"
                !f.isCurrent -> "Будущий месяц"
                f.verdict == Verdict.GOOD -> "Бюджет в порядке"
                f.verdict == Verdict.WARN -> "Близко к лимиту"
                f.verdict == Verdict.BAD -> "Риск перерасхода"
                else -> "Прогноз месяца"
            }
            Banner(title, tone) {
                if (f.isPast) {
                    KeyValueRow("Потрачено за месяц", Fmt.money(f.spent, cur, false))
                    KeyValueRow("Итоговый остаток", Fmt.money(md.summary.closing, cur, false))
                } else {
                    KeyValueRow(
                        "Прогноз расходов",
                        Fmt.money(f.projectedExpense, cur, false) + if (md.budget > 0) " из ${Fmt.money(md.budget, cur, false)}" else ""
                    )
                    if (f.isCurrent) {
                        KeyValueRow("Ожидаемый остаток", Fmt.money(f.projectedBalance, cur, false))
                        if (md.budget > 0 || f.safeDaily > 0) {
                            KeyValueRow("Можно тратить в день", Fmt.money(f.safeDaily, cur, false), incomeColor)
                        }
                        if (!f.reliable) {
                            HelpText("В начале месяца прогноз ориентировочный — через несколько дней он станет точнее.")
                        }
                    }
                }
            }
        }

        // 3–5. Главные цифры: доступно, доходы, расходы, остаток
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (depositInfos.isEmpty()) {
                    StatCard("Доступно сейчас на счетах", Fmt.money(balance, cur), Modifier.fillMaxWidth())
                } else {
                    StatCard("Всего денег (счета + накопления)", Fmt.money(balance + savings, cur), Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("На счетах", Fmt.money(balance, cur, false), Modifier.weight(1f))
                        StatCard("Накопления", Fmt.money(savings, cur, false), Modifier.weight(1f), Tone.SAVINGS)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Доходы", Fmt.money(md.summary.income, cur, false), Modifier.weight(1f), Tone.INCOME)
                    StatCard("Расходы", Fmt.money(md.summary.expense, cur, false), Modifier.weight(1f), Tone.EXPENSE)
                    StatCard("Остаток", Fmt.money(md.summary.closing, cur, false), Modifier.weight(1f))
                }
            }
        }

        // 9. Прогресс выполнения месячного плана
        if (md.budget > 0) {
            item {
                val fraction = (md.summary.expense.toFloat() / md.budget).coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(450),
                    label = "budgetProgress"
                )
                val pct = ((md.summary.expense * 100) / md.budget).toInt().coerceAtLeast(0)
                val over = md.summary.expense > md.budget
                Section("Выполнение плана месяца · $pct%") {
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if (over) expenseColor else MaterialTheme.colorScheme.primary
                    )
                    KeyValueRow(
                        if (over) "Превышение плана" else "Осталось по плану",
                        Fmt.money(kotlin.math.abs(md.budget - md.summary.expense), cur, false),
                        if (over) expenseColor else incomeColor
                    )
                }
            }
        }

        // График трат по дням
        if ((f.isCurrent || f.isPast) && f.spent > 0) {
            item {
                Section("Динамика расходов по дням") {
                    BurnChart(
                        actual = f.cumulative,
                        daysInMonth = f.daysInMonth,
                        projectedEnd = if (f.isCurrent) f.projectedExpense else null,
                        budget = md.budget,
                        actualColor = MaterialTheme.colorScheme.primary,
                        projectionColor = warnColor,
                        budgetColor = expenseColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("● факт", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        if (f.isCurrent) Text("┅ прогноз", color = warnColor, fontSize = 12.sp)
                        if (md.budget > 0) Text("┅ план", color = expenseColor, fontSize = 12.sp)
                    }
                }
            }
        }

        // 8. Краткая информация о накоплениях
        item {
            Section("Накопления и вклады") {
                if (depositInfos.isEmpty()) {
                    HelpText("У вас пока нет добавленных вкладов. Перейдите во вкладку «Накопления», чтобы начать вести копилку.")
                } else {
                    KeyValueRow("Общая сумма накоплений", Fmt.money(savings, cur, false), savingsColor)
                    KeyValueRow("Ожидаемые проценты за месяц", "+" + Fmt.money(expectedInterest, cur, false), incomeColor)
                    depositInfos.take(3).forEach { info ->
                        HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(info.deposit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    Deposits.rateText(info.deposit) + " · +" + Fmt.money(info.expectedMonthly, cur, false) + "/мес",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(Fmt.money(info.balance, cur, false), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 10. Последние операции
        item {
            Section("Последние операции") {
                if (recentOps.isEmpty()) {
                    HelpText("В этом месяце пока нет записей. Нажмите «+» внизу, чтобы добавить первый расход или доход.")
                    OutlinedButton(onClick = { vm.startNew() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Записать первую операцию")
                    }
                } else {
                    recentOps.forEachIndexed { idx, op ->
                        if (idx > 0) HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                        val amountColor = when (op.type) {
                            OpType.INCOME -> incomeColor
                            OpType.EXPENSE -> expenseColor
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        val sign = when (op.type) {
                            OpType.INCOME -> "+"
                            OpType.EXPENSE -> "−"
                            else -> ""
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.edit(op) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (op.type == OpType.TRANSFER) "Перевод: ${op.account} → ${op.destAccount}"
                                    else Fmt.emoji(op.category) + " " + op.category.ifBlank { "Без категории" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    Fmt.date(op.date) + if (op.comment.isNotBlank()) " · ${op.comment}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                sign + Fmt.money(op.amountCents, cur, false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = amountColor
                            )
                        }
                    }
                }
            }
        }

        // Обязательные платежи
        if (mandatory.isNotEmpty()) {
            item {
                Section("Обязательные платежи") {
                    mandatory.forEach { m ->
                        val isPaid = m.id in md.paid
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.toggleMandatory(m, ym, !isPaid) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isPaid, onCheckedChange = { vm.toggleMandatory(m, ym, it) })
                            Text(Fmt.emoji(m.category.ifBlank { m.name }) + " " + m.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text(Fmt.money(m.amountCents, cur, false), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Sheet.border().copy(alpha = 0.4f))
                    }
                    KeyValueRow(
                        "Осталось оплатить",
                        Fmt.money(md.summary.mandatoryUnpaid, cur, false),
                        if (md.summary.mandatoryUnpaid > 0) expenseColor else incomeColor
                    )
                }
            }
        }

        // 11. Полезная финансовая подсказка и совет дня
        item {
            var tipIdx by remember { mutableIntStateOf(0) }
            val extraTips = remember {
                listOf(
                    "Переводите 10% от любого дохода в накопления в тот же день — привычка работает лучше силы воли.",
                    "Правило 24 часов: отложите спонтанную покупку на сутки — часто оказывается, что она не так уж нужна.",
                    "Раз в неделю заглядывайте в «Траты»: 2 минуты анализа экономят до 15% месячного бюджета.",
                    "Финансовая подушка на 3 месяца расходов даёт спокойствие при любых неожиданных ситуациях.",
                    "Поддерживайте серию дней: ежедневная запись трат занимает 10 секунд и приносит +10 XP."
                )
            }
            val shownTip = if (tipIdx == 0) tip else extraTips[(tipIdx - 1) % extraTips.size]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.accentBlue(),
                border = BorderStroke(1.dp, Sheet.border())
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (tipIdx == 0) "💡 Персональная подсказка" else "✨ Совет по финансам",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { tipIdx = (tipIdx + 1) % (extraTips.size + 1) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Другой совет", fontSize = 12.sp)
                        }
                    }
                    AnimatedContent(targetState = shownTip, label = "tipText") { text ->
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Задания и достижения (компактно, чтобы мотивировать, но не отвлекать)
        item {
            Section("Задания и достижения · ${game.badges.count { it.unlocked }} из ${game.badges.size}") {
                QuestRow(md.hasPlan, "План на месяц", "+100 XP", if (!md.hasPlan && canPlan) "Создать" else null) { vm.openPlan(ym) }
                QuestRow(ops.any { it.date == todayDay }, "Записать траты за сегодня", "+10 XP", "Добавить") { vm.startNew() }
                if (mandatory.isNotEmpty()) {
                    QuestRow(paidCount == mandatory.size, "Платежи оплачены: $paidCount из ${mandatory.size}", "+15 XP", null) {}
                }
                if (md.budget > 0) {
                    QuestRow(md.summary.expense <= md.budget, "Уложиться в план месяца", "🏅", null) {}
                }
                HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    game.badges.forEach { b ->
                        Surface(
                            modifier = Modifier.width(96.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (b.unlocked) Sheet.tone(Tone.SAVINGS) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Sheet.border())
                        ) {
                            Column(
                                Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(if (b.unlocked) b.emoji else "🔒", fontSize = 22.sp)
                                Text(
                                    b.title,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (b.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestRow(done: Boolean, title: String, reward: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (done) "✅" else "⬜", fontSize = 18.sp)
        Text(
            "  $title",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        if (!done && action != null) {
            Button(onClick = onAction, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) { Text(action, fontSize = 12.sp) }
        } else {
            Text(reward, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
