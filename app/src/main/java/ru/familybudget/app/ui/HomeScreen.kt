package ru.familybudget.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.R
import ru.familybudget.app.data.OpType
import ru.familybudget.app.logic.Deposits
import ru.familybudget.app.logic.Fmt
import ru.familybudget.app.logic.Game
import ru.familybudget.app.logic.Plans
import ru.familybudget.app.logic.TrophyTier
import ru.familybudget.app.logic.Verdict
import ru.familybudget.app.logic.delta
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    val hidden = settings.hideAmounts
    fun money(cents: Long, showCents: Boolean = true): String =
        if (hidden) "•••• $cur" else Fmt.money(cents, cur, showCents)

    var showQuickExpense by remember { mutableStateOf(false) }
    var quickPreselectedCat by remember { mutableStateOf<String?>(null) }
    val expenseCats = remember(categories) { Plans.orderedExpenseCategories(categories) }
    val topCats = remember {
        listOf("Продукты", "Транспорт", "Прочие расходы", "Маркетплейсы", "Сплит и рассрочки")
    }
    if (showQuickExpense) {
        var quickCat by remember(quickPreselectedCat) {
            mutableStateOf(quickPreselectedCat ?: expenseCats.firstOrNull() ?: "Продукты")
        }
        var quickAmt by remember { mutableStateOf("") }
        var smartLine by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showQuickExpense = false; quickPreselectedCat = null }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡ Быстрый расход", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showQuickExpense = false; quickPreselectedCat = null; vm.startNew(OpType.EXPENSE) }) {
                        Text("Подробно →")
                    }
                }
                TextField2(
                    value = smartLine,
                    onChange = { raw ->
                        smartLine = raw
                        val numMatch = Regex("""(\d+(?:[.,]\d{1,2})?)""").find(raw)
                        if (numMatch != null) {
                            quickAmt = numMatch.groupValues[1]
                        }
                        val wordPart = raw.replace(Regex("""\d+(?:[.,]\d{1,2})?"""), "").trim().lowercase()
                        if (wordPart.length >= 2) {
                            val matched = expenseCats.firstOrNull { it.lowercase().contains(wordPart) }
                            if (matched != null) quickCat = matched
                        }
                    },
                    label = "Быстрая строка (напр. продукты 1450)"
                )
                MoneyField(quickAmt, { quickAmt = it }, "Сумма ($cur)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(200L, 500L, 1_000L, 2_000L, 5_000L).forEach { v ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val curRub = (Fmt.parseCents(quickAmt) ?: 0L) / 100L
                                quickAmt = (curRub + v).toString()
                            },
                            label = { Text("+${v}") }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    expenseCats.take(8).forEach { c ->
                        FilterChip(
                            selected = quickCat == c,
                            onClick = { quickCat = c },
                            label = { Text(c) }
                        )
                    }
                }
                Button(
                    onClick = {
                        val cents = Fmt.parseCents(quickAmt) ?: 0L
                        if (cents > 0L) {
                            vm.quickAddExpense(cents, quickCat)
                            showQuickExpense = false
                        }
                    },
                    enabled = (Fmt.parseCents(quickAmt) ?: 0L) > 0L,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Записать (+10 XP)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    val ym = vm.month
    val today = remember { LocalDate.now() }

    val md = remember(ops, accounts, mandatory, plans, categories, settings, ym) {
        Plans.monthData(ops, accounts, mandatory, plans, categories, settings, ym, today)
    }
    val prevExpense = remember(ops, ym) {
        ru.familybudget.app.logic.Stats.opsInMonth(ops, ym.minusMonths(1))
            .filter { it.type == OpType.EXPENSE }
            .sumOf { it.amountCents }
    }
    val game = remember(ops, plans, mandatory) { Game.compute(ops, plans, mandatory, today) }
    val balance = remember(accounts, ops) { accounts.sumOf { it.initialCents } + ops.sumOf { it.delta() } }
    val depositInfos = remember(deposits, interest) { deposits.map { Deposits.info(it, interest) } }
    val savings = remember(depositInfos) { depositInfos.sumOf { it.balance } }
    val expectedInterest = remember(depositInfos) { depositInfos.sumOf { it.expectedMonthly } }
    val recentOps = remember(md.ops) {
        md.ops.sortedWith(compareByDescending<ru.familybudget.app.data.Operation> { it.date }.thenByDescending { it.id }).take(5)
    }

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
        // 1. Компактная шапка: приветствие + серия + кнопка синхронизации справа
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (settings.userName.isBlank()) "Добрый день!" else "Привет, ${settings.userName}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Sheet.glass(),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.dp, Sheet.glassBorderBrush())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(levelDrawable(game.level)),
                                contentDescription = "Уровень ${game.level}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                            )
                            Text(
                                "Ур. ${game.level} · ${game.title} · 🔥 ${game.streak} дн.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Sheet.glass(),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.2.dp, Sheet.glassBorderBrush()),
                    modifier = Modifier.bouncyClickable { vm.toggleHideAmounts() }
                ) {
                    Text(
                        if (hidden) "🙈" else "👁️",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (settings.apiUrl.isNotBlank() && settings.token.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Sheet.glass(),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush()),
                        modifier = Modifier.bouncyClickable(enabled = !vm.syncing) { vm.syncAll() }
                    ) {
                        Text(
                            when {
                                vm.syncing -> "⏳"
                                unsynced > 0 -> "☁️ $unsynced"
                                else -> "☁️ ✓"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Главная акцентная карточка баланса (полупрозрачное стекло со световой кромкой)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                border = BorderStroke(1.4.dp, Sheet.glassBorderBrush()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(AppColors.heroBrush())
                        .padding(18.dp)
                ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "На счетах сейчас (свободно)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    val animatedBalance by animateFloatAsState(
                        targetValue = balance.toFloat(),
                        animationSpec = tween(720, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "heroBalance"
                    )
                    Text(
                        money(animatedBalance.toLong()),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (depositInfos.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "🏦 На вкладах: ${money(savings, false)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (expectedInterest > 0) {
                                Text(
                                    "+${money(expectedInterest, false)}/мес",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (f.isCurrent && (md.budget > 0 || f.safeDaily > 0)) {
                        val daysLeft = (ym.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)
                        val realFreeDaily = if (balance > 0) {
                            ((balance - md.summary.mandatoryUnpaid).coerceAtLeast(0L)) / daysLeft
                        } else {
                            f.safeDaily
                        }
                        val smartSafeDaily = if (f.safeDaily > 0 && realFreeDaily > 0) {
                            minOf(f.safeDaily, realFreeDaily)
                        } else {
                            maxOf(f.safeDaily, realFreeDaily)
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Безопасно тратить в день",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                money(smartSafeDaily, false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }
            }
            }
        }

        // 3. Практичные быстрые кнопки-капсулы в стиле Liquid Glass
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).bouncyClickable { showQuickExpense = true },
                    shape = RoundedCornerShape(50),
                    color = Sheet.tone(Tone.EXPENSE),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.3.dp, Sheet.glassBorderBrush())
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "− Расход (+10 XP)",
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.SemiBold,
                            color = expenseColor
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).bouncyClickable { vm.startNew(OpType.INCOME) },
                    shape = RoundedCornerShape(50),
                    color = Sheet.tone(Tone.INCOME),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.3.dp, Sheet.glassBorderBrush())
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "+ Доход",
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = FontWeight.SemiBold,
                            color = incomeColor
                        )
                    }
                }
            }
        }

        // 3.5. Умные кнопки частых категорий (1А: 1 тап → сразу ввод суммы)
        if (topCats.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topCats.forEach { cat ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .bouncyClickable {
                                    quickPreselectedCat = cat
                                    showQuickExpense = true
                                }
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Sheet.glass(),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(1.1.dp, Sheet.glassBorderBrush())
                            ) {
                                Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                                    CategoryBadge(cat, settings.categoryIcons, size = 34.dp)
                                }
                            }
                            Text(
                                cat,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3.6. Компактные «Квесты дня» (сворачиваемые, без визуального шума) + «День без трат»
        item {
            val todayIso = today.toString()
            val todayOps = remember(ops, todayDay) { ops.filter { it.date == todayDay } }
            val todayExpense = remember(todayOps) {
                todayOps.filter { it.type == OpType.EXPENSE }.sumOf { it.amountCents }
            }
            val markedNoSpend = todayIso in settings.noSpendDays
            val q1Done = todayOps.isNotEmpty() || markedNoSpend
            val q2Done = f.safeDaily <= 0L || todayExpense <= f.safeDaily
            val q3Done = mandatory.isEmpty() || paidCount > 0
            val doneCount = listOf(q1Done, q2Done, q3Done).count { it }
            var questsOpen by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Sheet.glass(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.1.dp, Sheet.glassBorderBrush())
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { questsOpen = !questsOpen },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🎯 Квесты дня ($doneCount/3)" + if (doneCount == 3) " · ✅" else "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (questsOpen) "Свернуть ▴" else "Развернуть ▾",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = questsOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuestRow(q1Done, "Записать операцию или день без трат", "+15 XP", null) {}
                            QuestRow(q2Done, "Уложиться в дневной лимит", "+25 XP", null) {}
                            QuestRow(q3Done, "Отметить обязательные платежи", "+10 XP", null) {}
                            if (todayExpense == 0L && !markedNoSpend) {
                                OutlinedButton(
                                    onClick = { vm.markNoSpendToday() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("🧘 Сегодня без трат (+20 XP)")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3.7. Компактный виджет «💳 Рассрочки и сплит» (как кредит: долг, дата платежа, комментарий)
        item {
            var editInstallment by remember { mutableStateOf<ru.familybudget.app.data.InstallmentItem?>(null) }
            var showInstallmentDialog by remember { mutableStateOf(false) }
            val installments = settings.installments
            val totalRemaining = installments.sumOf { (it.totalDebtCents - it.paidCents).coerceAtLeast(0L) }

            if (showInstallmentDialog) {
                val curItem = editInstallment
                var title by remember(curItem) { mutableStateOf(curItem?.title.orEmpty()) }
                var debt by remember(curItem) {
                    mutableStateOf(if ((curItem?.totalDebtCents ?: 0L) > 0L) Fmt.plain(curItem!!.totalDebtCents) else "")
                }
                var monthly by remember(curItem) {
                    mutableStateOf(if ((curItem?.monthlyPaymentCents ?: 0L) > 0L) Fmt.plain(curItem!!.monthlyPaymentCents) else "")
                }
                var payDay by remember(curItem) { mutableStateOf((curItem?.paymentDay ?: 10).toString()) }
                var comment by remember(curItem) { mutableStateOf(curItem?.comment.orEmpty()) }
                val debtCents = Fmt.parseCents(debt) ?: 0L
                val monthlyCents = Fmt.parseCents(monthly) ?: 0L
                val dayNum = payDay.toIntOrNull()?.coerceIn(1, 31) ?: 10

                FormDialog(
                    title = if (curItem == null) "Новая рассрочка / сплит" else "Рассрочка / сплит",
                    onDismiss = { showInstallmentDialog = false; editInstallment = null },
                    confirmEnabled = title.isNotBlank() && debtCents > 0L,
                    onConfirm = {
                        vm.saveInstallment(
                            ru.familybudget.app.data.InstallmentItem(
                                id = curItem?.id ?: 0L,
                                title = title.trim(),
                                totalDebtCents = debtCents,
                                paidCents = curItem?.paidCents ?: 0L,
                                monthlyPaymentCents = monthlyCents,
                                paymentDay = dayNum,
                                comment = comment.trim()
                            )
                        )
                        showInstallmentDialog = false
                        editInstallment = null
                    },
                    onDelete = if (curItem != null) ({
                        vm.deleteInstallment(curItem.id)
                        showInstallmentDialog = false
                        editInstallment = null
                    }) else null
                ) {
                    TextField2(title, { title = it }, "Название (напр. Яндекс Сплит, iPhone)")
                    MoneyField(debt, { debt = it }, "Общая сумма долга, $cur")
                    MoneyField(monthly, { monthly = it }, "Платёж в месяц, $cur")
                    androidx.compose.material3.OutlinedTextField(
                        value = payDay,
                        onValueChange = { if (it.length <= 2 && it.all { ch -> ch.isDigit() }) payDay = it },
                        label = { Text("Дата платежа (число 1–31)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField2(comment, { comment = it }, "Комментарий (опционально)", singleLine = false)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Sheet.glass(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryBadge("Сплит и рассрочки", settings.categoryIcons, size = 30.dp)
                            Column {
                                Text("Рассрочки и сплит", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (installments.isEmpty()) "Добавьте рассрочку или сплит"
                                    else "Остаток долга: ${money(totalRemaining, false)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { editInstallment = null; showInstallmentDialog = true }) {
                            Text("+ Добавить")
                        }
                    }
                    installments.sortedBy { it.paymentDay }.forEach { inst ->
                        val rem = (inst.totalDebtCents - inst.paidCents).coerceAtLeast(0L)
                        val progress = if (inst.totalDebtCents > 0L) (inst.paidCents.toFloat() / inst.totalDebtCents).coerceIn(0f, 1f) else 0f
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { editInstallment = inst; showInstallmentDialog = true }
                                .padding(vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(inst.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    val sub = buildString {
                                        append("до ${inst.paymentDay} числа")
                                        if (inst.monthlyPaymentCents > 0L) append(" · ${money(inst.monthlyPaymentCents, false)}/мес")
                                        if (inst.comment.isNotBlank()) append(" · ${inst.comment}")
                                    }
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(money(rem, false), fontWeight = FontWeight.Bold, color = if (rem == 0L) incomeColor else warnColor)
                                    if (rem > 0L && inst.monthlyPaymentCents > 0L) {
                                        TextButton(
                                            onClick = { vm.payInstallment(inst.id, inst.monthlyPaymentCents) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) { Text("Оплатить") }
                                    }
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (rem == 0L) incomeColor else warnColor
                            )
                        }
                    }
                }
            }
        }

        // 4. Переключатель месяца + Доходы / Расходы / Остаток
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthSwitcher(Fmt.monthTitle(ym), onPrev = { vm.shiftMonth(-1) }, onNext = { vm.shiftMonth(1) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Доходы", Fmt.money(md.summary.income, cur, false), Modifier.weight(1f), Tone.INCOME)
                    StatCard("Расходы", Fmt.money(md.summary.expense, cur, false), Modifier.weight(1f), Tone.EXPENSE)
                    StatCard("Остаток", Fmt.money(md.summary.closing, cur, false), Modifier.weight(1f))
                }
            }
        }

        // 5. Статус бюджета и прогресс плана
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
                if (md.budget > 0) {
                    val fraction = (md.summary.expense.toFloat() / md.budget).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(450),
                        label = "budgetProgress"
                    )
                    val pct = ((md.summary.expense * 100) / md.budget).toInt().coerceAtLeast(0)
                    val leftPct = (100 - pct).coerceIn(0, 100)
                    val over = md.summary.expense > md.budget
                    val ringColor = if (over) expenseColor else MaterialTheme.colorScheme.primary
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 8.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawArc(
                                    color = Color.White.copy(alpha = 0.35f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = stroke
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedFraction,
                                    useCenter = false,
                                    style = stroke
                                )
                            }
                            Text(
                                "${leftPct}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (over) expenseColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (over) "Перерасход плана" else "Осталось $leftPct% от плана",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${money(md.summary.expense, false)} из ${money(md.budget, false)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    KeyValueRow(
                        if (over) "Превышение плана ($pct%)" else "Осталось по плану ($pct%)",
                        Fmt.money(kotlin.math.abs(md.budget - md.summary.expense), cur, false),
                        if (over) expenseColor else incomeColor
                    )
                }
                if (f.isPast) {
                    KeyValueRow("Потрачено за месяц", Fmt.money(f.spent, cur, false))
                } else {
                    KeyValueRow(
                        "Прогноз расходов",
                        Fmt.money(f.projectedExpense, cur, false) + if (md.budget > 0) " из ${Fmt.money(md.budget, cur, false)}" else ""
                    )
                    if (f.isCurrent) {
                        KeyValueRow("Ожидаемый остаток", Fmt.money(f.projectedBalance, cur, false))
                        if (!f.reliable) {
                            HelpText("В начале месяца прогноз ориентировочный — через пару дней он станет точнее.")
                        }
                    }
                }
                if (prevExpense > 0L && md.summary.expense > 0L) {
                    val diffPct = (((md.summary.expense - prevExpense) * 100L) / prevExpense).toInt()
                    val cmpText = when {
                        diffPct < 0 -> "На ${-diffPct}% меньше, чем в прошлом месяце 👍"
                        diffPct > 0 -> "На $diffPct% больше, чем в прошлом месяце"
                        else -> "На уровне прошлого месяца"
                    }
                    KeyValueRow("Сравнение", cmpText, if (diffPct <= 0) incomeColor else expenseColor)
                }
            }
        }

        // 6. Последние операции (подняты выше для быстрого доступа)
        item {
            Section("Последние операции") {
                if (recentOps.isEmpty()) {
                    HelpText("В этом месяце пока нет записей. Нажмите «− Расход» или «+ Доход» выше.")
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CategoryBadge(
                                categoryName = if (op.type == OpType.TRANSFER) "Перевод" else op.category.ifBlank { "Прочее" },
                                overrides = settings.categoryIcons,
                                size = 36.dp
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (op.type == OpType.TRANSFER) "Перевод: ${op.account} → ${op.destAccount}"
                                    else op.category.ifBlank { "Без категории" },
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

        // 7. Обязательные платежи
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

        // 8. График трат по дням
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

        // 9. Краткая сводка по накоплениям
        if (depositInfos.isNotEmpty()) {
            item {
                Section("Накопления и вклады") {
                    KeyValueRow("Общая сумма накоплений", Fmt.money(savings, cur, false), savingsColor)
                    KeyValueRow("Ожидаемые проценты за месяц", "+" + Fmt.money(expectedInterest, cur, false), incomeColor)
                }
            }
        }

        // 10. Полезная финансовая подсказка и совет дня
        item {
            var tipIdx by remember { mutableIntStateOf(0) }
            val extraTips = remember {
                listOf(
                    "Переводите 10% от любого дохода в накопления в тот же день — привычка работает лучше силы воли.",
                    "Правило 24 часов: отложите спонтанную покупку на сутки — часто оказывается, что она не так уж нужна.",
                    "Раз в неделю заглядывайте в «Траты»: 2 минуты анализа экономят до 15% месячного бюджета.",
                    "Финансовая подушка на 3 месяца расходов даёт спокойствие при любых неожиданных ситуациях.",
                    "Поддерживайте серию дней: ежедневная запись трат занимает 10 секунд и приносит +10 XP.",
                    "Правило 24 часов: перед покупкой на маркетплейсе подождите сутки — 60% импульсивных трат отпадают сами.",
                    "Переводите 10% от каждого дохода в финансовую подушку сразу в день зарплаты, пока деньги не разошлись.",
                    "День без трат — лучший тренажёр дисциплины: нажмите «🧘 Сегодня без трат» в квестах дня и получите +20 XP.",
                    "Проверяйте активные подписки раз в месяц: часто 2–3 ненужные подписки съедают 1 500–3 000 ₽.",
                    "Отмечайте рассрочки и сплит в виджете на Главной, чтобы видеть реальную нагрузку на бюджет.",
                    "Свободный остаток в конце месяца можно в 1 касание перенести в финансовую подушку в разделе «Накопления».",
                    "Используйте быструю строку вроде «продукты 1450» — так запись расхода занимает меньше 3 секунд.",
                    "Разделяйте крупные цели на этапы: когда видите прогресс-бар цели, копить гораздо проще и приятнее.",
                    "Сравнивайте траты текущего месяца с прошлым — даже небольшое снижение на 5% даёт заметный плюс за год."
                )
            }
            androidx.compose.runtime.LaunchedEffect(extraTips.size) {
                while (true) {
                    kotlinx.coroutines.delay(10_000L)
                    tipIdx = (tipIdx + 1) % (extraTips.size + 1)
                }
            }
            val shownTip = if (tipIdx == 0) tip else extraTips[(tipIdx - 1) % extraTips.size]
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.accentBlue(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (tipIdx == 0) "💡 Персональная подсказка" else "✨ Совет по финансам",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = { tipIdx = (tipIdx + 1) % (extraTips.size + 1) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Другой совет", fontSize = 12.sp)
                        }
                    }
                    AnimatedContent(targetState = shownTip, label = "tipText") { text ->
                        Text(
                            text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 11. Прогресс уровня, задания и достижения
        item {
            val animatedXp by animateFloatAsState(
                targetValue = game.progress,
                animationSpec = tween(450),
                label = "xpProgress"
            )
            Section("Задания и достижения · ${game.badges.count { it.unlocked }} из ${game.badges.size}") {
                LinearProgressIndicator(
                    progress = { animatedXp },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                )
                HelpText("⭐ ${game.xp} XP · ещё ${game.xpToNext} до уровня ${game.level + 1}")
                QuestRow(md.hasPlan, "План на месяц", "+100 XP", if (!md.hasPlan && canPlan) "Создать" else null) { vm.openPlan(ym) }
                QuestRow(ops.any { it.date == todayDay }, "Записать траты за сегодня", "+10 XP", "Добавить") { vm.startNew() }
                if (mandatory.isNotEmpty()) {
                    QuestRow(paidCount == mandatory.size, "Платежи оплачены: $paidCount из ${mandatory.size}", "+15 XP", null) {}
                }
                if (md.budget > 0) {
                    QuestRow(md.summary.expense <= md.budget, "Уложиться в план месяца", "🏅", null) {}
                }
                HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                // Сводка трофеев в стиле PS5: Платина / Золото / Серебро / Бронза
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        TrophyTier.PLATINUM,
                        TrophyTier.GOLD,
                        TrophyTier.SILVER,
                        TrophyTier.BRONZE
                    ).forEach { tier ->
                        val earned = game.badges.count { it.tier == tier && it.unlocked }
                        val total = game.badges.count { it.tier == tier }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Image(
                                painter = painterResource(trophyDrawable(tier)),
                                contentDescription = tier.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(22.dp).clip(CircleShape)
                            )
                            Text(
                                "$earned/$total",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                var selectedBadge by remember { mutableStateOf<ru.familybudget.app.logic.Badge?>(null) }
                selectedBadge?.let { b ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { selectedBadge = null },
                        icon = {
                            Image(
                                painter = painterResource(trophyDrawable(b.tier)),
                                contentDescription = b.tier.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .alpha(if (b.unlocked) 1f else 0.45f)
                            )
                        },
                        title = { Text(b.title, textAlign = TextAlign.Center) },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${b.tier.label} · +${b.tier.xp} XP · " +
                                        if (b.unlocked) "✅ Получено" else "🔒 Не получено",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (b.unlocked) incomeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (b.hint.isNotBlank()) {
                                    Text(b.hint, textAlign = TextAlign.Center)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { selectedBadge = null }) { Text("Отлично") }
                        }
                    )
                }
                var trophiesExpanded by remember { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().clickable { trophiesExpanded = !trophiesExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Трофеи (${game.badges.count { it.unlocked }}/${game.badges.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        InfoHintIcon(
                            "Получайте бронзовые (+15 XP), серебряные (+30 XP), золотые (+90 XP) и платиновый (+300 XP) трофеи за ведение бюджета, серию дней и выполнение плана.",
                            title = "Система трофеев"
                        )
                    }
                    Text(
                        if (trophiesExpanded) "Свернуть ▴" else "Развернуть ▾",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                androidx.compose.animation.AnimatedVisibility(visible = trophiesExpanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "platGlow")
                    val platScale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.09f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "platScale"
                    )
                    val platGlowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.85f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "platGlowAlpha"
                    )
                    game.badges.forEach { b ->
                        val cardTone = when {
                            !b.unlocked -> Sheet.glass()
                            b.tier == TrophyTier.PLATINUM -> AppColors.accentBlue()
                            b.tier == TrophyTier.GOLD -> Sheet.tone(Tone.SAVINGS)
                            b.tier == TrophyTier.SILVER -> Sheet.glass()
                            else -> Sheet.tone(Tone.WARN)
                        }
                        val badgeBg = when (b.tier) {
                            TrophyTier.GOLD -> Color(0xFFFFD54F).copy(alpha = if (b.unlocked) 0.35f else 0.14f)
                            TrophyTier.SILVER -> Color(0xFFB0BEC5).copy(alpha = if (b.unlocked) 0.38f else 0.14f)
                            TrophyTier.BRONZE -> Color(0xFFD78852).copy(alpha = if (b.unlocked) 0.32f else 0.14f)
                            TrophyTier.PLATINUM -> Color(0xFF4FC3F7).copy(alpha = platGlowAlpha)
                        }
                        Surface(
                            modifier = Modifier.width(96.dp).clickable { selectedBadge = b },
                            shape = RoundedCornerShape(16.dp),
                            color = cardTone,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            border = BorderStroke(
                                if (b.tier == TrophyTier.PLATINUM) 1.6.dp else 1.dp,
                                if (b.tier == TrophyTier.PLATINUM) {
                                    androidx.compose.ui.graphics.SolidColor(Color(0xFF81D4FA).copy(alpha = platGlowAlpha))
                                } else {
                                    Sheet.glassBorderBrush()
                                }
                            )
                        ) {
                            Column(
                                Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (b.tier == TrophyTier.PLATINUM) {
                                    // Платиновый трофей: ТОЛЬКО яркий анимированный кубок с сиянием
                                    Box(
                                        modifier = Modifier.size(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .graphicsLayer {
                                                    scaleX = platScale
                                                    scaleY = platScale
                                                },
                                            shape = CircleShape,
                                            color = badgeBg
                                        ) {}
                                        Image(
                                            painter = painterResource(R.drawable.trophy_platinum),
                                            contentDescription = b.tier.label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .graphicsLayer {
                                                    scaleX = platScale
                                                    scaleY = platScale
                                                }
                                                .alpha(if (b.unlocked) 1f else 0.72f)
                                        )
                                    }
                                } else {
                                    // Уникальная иконка достижения в стиле PS5 + значок кубка (Бронза/Серебро/Золото)
                                    Box(
                                        modifier = Modifier.size(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(44.dp),
                                            shape = CircleShape,
                                            color = badgeBg,
                                            border = BorderStroke(1.dp, Sheet.glassBorderBrush())
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    b.emoji,
                                                    fontSize = 21.sp,
                                                    modifier = Modifier.alpha(if (b.unlocked) 1f else 0.35f)
                                                )
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.BottomEnd),
                                            shape = CircleShape,
                                            color = Sheet.glass(),
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            border = BorderStroke(0.8.dp, Sheet.glassBorderBrush())
                                        ) {
                                            Image(
                                                painter = painterResource(trophyDrawable(b.tier)),
                                                contentDescription = b.tier.label,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .alpha(if (b.unlocked) 1f else 0.45f)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    b.title,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (b.unlocked) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (b.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "+${b.tier.xp} XP",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

private fun trophyDrawable(tier: TrophyTier): Int = when (tier) {
    TrophyTier.PLATINUM -> R.drawable.trophy_platinum
    TrophyTier.GOLD -> R.drawable.trophy_gold
    TrophyTier.SILVER -> R.drawable.trophy_silver
    TrophyTier.BRONZE -> R.drawable.trophy_bronze
}

private fun levelDrawable(level: Int): Int = when {
    level >= 7 -> R.drawable.level_platinum
    level >= 5 -> R.drawable.level_gold
    level >= 3 -> R.drawable.level_silver
    else -> R.drawable.level_bronze
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
