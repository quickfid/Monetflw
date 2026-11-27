package ru.familybudget.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.familybudget.app.BudgetViewModel
import ru.familybudget.app.data.Account
import ru.familybudget.app.data.AppUpdater
import ru.familybudget.app.data.Category
import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.logic.Fmt
import java.time.LocalDate

private enum class Page { MENU, PROFILE, CATEGORIES, MANDATORY, ACCOUNTS, PEOPLE, SYNC, REPORTS }

@Composable
fun SettingsScreen(vm: BudgetViewModel, padding: PaddingValues) {
    var page by rememberSaveable { mutableStateOf(Page.MENU) }
    BackHandler(enabled = page != Page.MENU) { page = Page.MENU }

    Column(Modifier.fillMaxSize().padding(padding)) {
        if (page != Page.MENU) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { page = Page.MENU }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    when (page) {
                        Page.PROFILE -> "Профиль и бюджет"
                        Page.CATEGORIES -> "Категории и лимиты"
                        Page.MANDATORY -> "Обязательные платежи"
                        Page.ACCOUNTS -> "Счета"
                        Page.PEOPLE -> "Люди"
                        Page.REPORTS -> "Отчёты"
                        else -> "Google Таблица"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        when (page) {
            Page.MENU -> MenuPage(vm) { page = it }
            Page.PROFILE -> ProfilePage(vm)
            Page.CATEGORIES -> CategoriesPage(vm)
            Page.MANDATORY -> MandatoryPage(vm)
            Page.ACCOUNTS -> AccountsPage(vm)
            Page.PEOPLE -> PeoplePage(vm)
            Page.SYNC -> SyncPage(vm)
            Page.REPORTS -> ReportsHost(vm, PaddingValues(0.dp))
        }
    }
}

@Composable
private fun MenuPage(vm: BudgetViewModel, open: (Page) -> Unit) {
    val settings by vm.settings.collectAsState()
    val unsynced by vm.unsyncedCount.collectAsState()
    var currency by remember(settings.currency) { mutableStateOf(settings.currency) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importBackup(uri)
    }
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setDailyReminder(true)
    }
    var confirmRestore by remember { mutableStateOf(false) }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Восстановить из копии?") },
            text = { Text("Все текущие данные в приложении будут заменены данными из файла.") },
            confirmButton = {
                TextButton(onClick = { confirmRestore = false; restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                    Text("Выбрать файл")
                }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Отмена") } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("⚙️ Ещё", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        MenuItem("👤 Профиль", "Имя и доход") { open(Page.PROFILE) }
        MenuItem("🏷️ Категории", "Расходы и доходы") { open(Page.CATEGORIES) }
        MenuItem("💳 Платежи", "Ежемесячные, с галочками") { open(Page.MANDATORY) }
        MenuItem("🏦 Счета", "Карты и наличные") { open(Page.ACCOUNTS) }
        MenuItem("👨‍👩‍👧 Люди", settings.persons.joinToString(", ")) { open(Page.PEOPLE) }
        MenuItem(
            "Google Таблица",
            if (settings.apiUrl.isBlank()) "Не подключена" else "Не отправлено: $unsynced"
        ) { open(Page.SYNC) }
        MenuItem("📊 Отчёты", "Анализ доходов, расходов и динамики") { open(Page.REPORTS) }
        MenuItem("📤 Отправить отчёт в таблицу", "Таблицы и графики для другого человека") {
            vm.exportReport(vm.month)
        }
        MenuItem("📄 Экспорт CSV", "Все операции в файл") {
            exportLauncher.launch("budget_${LocalDate.now()}.csv")
        }
        MenuItem("💾 Резервная копия", "Сохранить все данные в файл") {
            backupLauncher.launch("family_budget_backup_${LocalDate.now()}.json")
        }
        MenuItem("♻️ Восстановить", "Из файла резервной копии") { confirmRestore = true }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = currency,
                onValueChange = { if (it.length <= 4) currency = it },
                label = { Text("Валюта") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { vm.setCurrency(currency) }) { Text("Применить") }
        }
        if (vm.message.isNotEmpty()) {
            Text(vm.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("🔔 Напоминание в 20:00", style = MaterialTheme.typography.bodyLarge)
                Text("Ежедневно вечером, чтобы не забыть записать траты", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.dailyReminder,
                onCheckedChange = { enabled ->
                    if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setDailyReminder(enabled)
                    }
                }
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("🖼️ Размытые обои рабочего стола", style = MaterialTheme.typography.bodyLarge)
                Text("Фон из обоев телефона с эффектом Liquid Glass", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.wallpaperBackground,
                onCheckedChange = { vm.setWallpaperBackground(it) }
            )
        }
        HorizontalDivider()
        MenuItem(
            "🔄 Проверить обновления",
            if (vm.checkingUpdate) "Проверяем…" else "Установить новую версию прямо в приложении"
        ) { vm.checkForUpdate(silent = false) }
        val ctx = LocalContext.current
        val appVer = remember(ctx) { AppUpdater.currentVersion(ctx) }
        Text(
            "Данные только на вашем телефоне · v$appVer",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MenuItem(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, maxLines = 2) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// ---------------------------------------------------------------- профиль
@Composable
private fun ProfilePage(vm: BudgetViewModel) {
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    var name by remember(settings.userName) { mutableStateOf(settings.userName) }
    var income by remember(settings.monthlyIncome) {
        mutableStateOf(if (settings.monthlyIncome > 0) Fmt.plain(settings.monthlyIncome) else "")
    }
    var budget by remember(settings.monthlyBudget) {
        mutableStateOf(if (settings.monthlyBudget > 0) Fmt.plain(settings.monthlyBudget) else "")
    }
    var savingsPct by remember(settings.savingsPercent) { mutableStateOf(settings.savingsPercent) }
    val incomeCents = Fmt.parseCents(income) ?: 0L
    val budgetCents = Fmt.parseCents(budget) ?: 0L

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextField2(name, { name = it }, "Ваше имя")
        MoneyField(income, { income = it }, "Доход в месяц, $cur")
        ChipPicker(
            label = "🛡️ Откладывать на подушку" +
                if (incomeCents > 0) " (${Fmt.money(incomeCents * savingsPct / 100L, cur, false)}/мес)" else "",
            value = "$savingsPct%",
            options = listOf("5%", "10%", "15%", "20%"),
            onSelect = {
                savingsPct = it.removeSuffix("%").toIntOrNull() ?: 10
                vm.setSavingsPercent(savingsPct)
            }
        )
        MoneyField(budget, { budget = it }, "План трат на месяц, $cur")
        if (incomeCents > 0 && budgetCents > 0) {
            val saving = incomeCents - budgetCents
            HelpText(
                if (saving >= 0) "Откладывать каждый месяц: " + Fmt.money(saving, cur, false) + " (" + Fmt.percent(saving.toFloat() / incomeCents) + " дохода)"
                else "План трат больше дохода на " + Fmt.money(-saving, cur, false) + ". Так деньги будут заканчиваться."
            )
        }
        HelpText("Бюджет и доход нужны для прогнозов и подсказок на главном экране.")
        Button(
            onClick = { vm.saveProfile(name, incomeCents, budgetCents) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Сохранить") }
        if (vm.message.isNotEmpty()) Text(vm.message, color = MaterialTheme.colorScheme.primary)
    }
}

// ---------------------------------------------------------------- категории
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesPage(vm: BudgetViewModel) {
    val categories by vm.categories.collectAsState()
    val settings by vm.settings.collectAsState()
    val cur = settings.currency
    var income by rememberSaveable { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Category?>(null) }
    val list = categories.filter { it.isIncome == income }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !income, onClick = { income = false }, label = { Text("Расходы") })
            FilterChip(selected = income, onClick = { income = true }, label = { Text("Доходы") })
        }
        list.forEach { c ->
            ListItem(
                leadingContent = { CategoryBadge(c.name, settings.categoryIcons, size = 36.dp) },
                headlineContent = { Text(c.name) },
                supportingContent = {
                    if (!c.isIncome) Text(if (c.limitCents > 0) "Лимит: " + Fmt.money(c.limitCents, cur, false) else "Без лимита")
                },
                modifier = Modifier.clickable { dialog = c }
            )
        }
        OutlinedButton(onClick = { dialog = Category(name = "", isIncome = income) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(if (income) "  Новая категория дохода" else "  Новая категория расхода")
        }
    }

    dialog?.let { c ->
        var name by remember(c) { mutableStateOf(c.name) }
        var limit by remember(c) { mutableStateOf(if (c.limitCents > 0) Fmt.plain(c.limitCents) else "") }
        var chosenIcon by remember(c) {
            mutableStateOf(settings.categoryIcons[c.name] ?: CategoryIcons.autoKey(c.name))
        }
        FormDialog(
            title = if (c.id == 0L) "Новая категория" else "Категория",
            onDismiss = { dialog = null },
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                val trimmed = name.trim()
                vm.setCategoryIcon(trimmed, chosenIcon)
                vm.saveCategory(c.copy(name = trimmed, limitCents = if (c.isIncome) 0L else (Fmt.parseCents(limit) ?: 0L)), c.name.ifBlank { null })
                dialog = null
            },
            onDelete = if (c.id != 0L) ({ vm.deleteCategory(c); dialog = null }) else null
        ) {
            TextField2(name, {
                name = it
                if (c.id == 0L) chosenIcon = CategoryIcons.autoKey(it)
            }, "Название")
            if (!c.isIncome) MoneyField(limit, { limit = it }, "Месячный лимит, $cur (пусто — без лимита)")
            Text("Иконка категории", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIcons.all.forEach { spec ->
                    val selected = chosenIcon == spec.key
                    Surface(
                        shape = CircleShape,
                        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.size(40.dp).clickable { chosenIcon = spec.key }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CategoryBadge(spec.label, mapOf(spec.label to spec.key), size = 34.dp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- обязательные платежи
@Composable
private fun MandatoryPage(vm: BudgetViewModel) {
    val items by vm.mandatory.collectAsState()
    val categories by vm.categories.collectAsState()
    val cur = vm.settings.collectAsState().value.currency
    var dialog by remember { mutableStateOf<Mandatory?>(null) }
    val expenseCategories = categories.filter { !it.isIncome }.map { it.name }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (items.isEmpty()) EmptyHint("Пока пусто. Отмеченный на обзоре платёж сам превращается в расход.")
        items.forEach { m ->
            ListItem(
                headlineContent = { Text(m.name) },
                supportingContent = { Text("до ${m.day} числа · ${m.category}") },
                trailingContent = { Text(Fmt.money(m.amountCents, cur)) },
                modifier = Modifier.clickable { dialog = m }
            )
        }
        OutlinedButton(
            onClick = { dialog = Mandatory(name = "", day = 1, amountCents = 0, category = expenseCategories.firstOrNull().orEmpty()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("  Новый платёж")
        }
    }

    dialog?.let { m ->
        var name by remember(m) { mutableStateOf(m.name) }
        var day by remember(m) { mutableStateOf(m.day.toString()) }
        var amount by remember(m) { mutableStateOf(if (m.amountCents > 0) Fmt.plain(m.amountCents) else "") }
        var category by remember(m) { mutableStateOf(m.category) }
        val cents = Fmt.parseCents(amount)
        val dayNum = day.toIntOrNull()
        FormDialog(
            title = if (m.id == 0L) "Новый платёж" else "Платёж",
            onDismiss = { dialog = null },
            confirmEnabled = name.isNotBlank() && cents != null && cents > 0 && dayNum != null && dayNum in 1..31,
            onConfirm = {
                vm.saveMandatory(m.copy(name = name.trim(), day = dayNum ?: 1, amountCents = cents ?: 0L, category = category))
                dialog = null
            },
            onDelete = if (m.id != 0L) ({ vm.deleteMandatory(m); dialog = null }) else null
        ) {
            TextField2(name, { name = it }, "Название")
            MoneyField(amount, { amount = it }, "Сумма, $cur")
            OutlinedTextField(
                value = day,
                onValueChange = { if (it.length <= 2 && it.all { ch -> ch.isDigit() }) day = it },
                label = { Text("День месяца (1–31)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Picker("Категория расхода", category, expenseCategories, { category = it })
        }
    }
}

// ---------------------------------------------------------------- счета
@Composable
private fun AccountsPage(vm: BudgetViewModel) {
    val accounts by vm.accounts.collectAsState()
    val ops by vm.operations.collectAsState()
    val cur = vm.settings.collectAsState().value.currency
    var dialog by remember { mutableStateOf<Account?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.forEach { a ->
            ListItem(
                headlineContent = { Text(a.name) },
                supportingContent = { Text("Начальный остаток: " + Fmt.money(a.initialCents, cur)) },
                trailingContent = { Text(Fmt.money(ru.familybudget.app.logic.Stats.accountBalance(a, ops), cur)) },
                modifier = Modifier.clickable { dialog = a }
            )
        }
        OutlinedButton(onClick = { dialog = Account(name = "") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("  Новый счёт")
        }
    }

    dialog?.let { a ->
        var name by remember(a) { mutableStateOf(a.name) }
        var initial by remember(a) { mutableStateOf(if (a.initialCents != 0L) Fmt.plain(a.initialCents) else "") }
        FormDialog(
            title = if (a.id == 0L) "Новый счёт" else "Счёт",
            onDismiss = { dialog = null },
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                vm.saveAccount(a.copy(name = name.trim(), initialCents = Fmt.parseCents(initial) ?: 0L), a.name.ifBlank { null })
                dialog = null
            },
            onDelete = if (a.id != 0L && accounts.size > 1) ({ vm.deleteAccount(a); dialog = null }) else null
        ) {
            TextField2(name, { name = it }, "Название")
            MoneyField(initial, { initial = it }, "Начальный остаток, $cur")
        }
    }
}

// ---------------------------------------------------------------- люди
@Composable
private fun PeoplePage(vm: BudgetViewModel) {
    val settings by vm.settings.collectAsState()
    var newName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        settings.persons.forEach { p ->
            ListItem(
                headlineContent = { Text(p) },
                trailingContent = {
                    if (settings.persons.size > 1) TextButton(onClick = { vm.removePerson(p) }) { Text("Удалить") }
                }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Имя (например, Аня)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { vm.addPerson(newName); newName = "" }, enabled = newName.isNotBlank()) { Text("Добавить") }
        }
    }
}

// ---------------------------------------------------------------- синхронизация
@Composable
private fun SyncPage(vm: BudgetViewModel) {
    val settings by vm.settings.collectAsState()
    val unsynced by vm.unsyncedCount.collectAsState()
    var url by remember(settings.apiUrl) { mutableStateOf(settings.apiUrl) }
    var token by remember(settings.token) { mutableStateOf(settings.token) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Необязательно. Приложение работает без интернета; операции можно отправлять в вашу Google-таблицу. " +
                "Как развернуть скрипт — в файле README_RU.md проекта (scripts/Code.gs).",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = url, onValueChange = { url = it }, label = { Text("URL веб-приложения Apps Script") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = token, onValueChange = { token = it }, label = { Text("Секретный токен") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { vm.setApi(url, token) }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Отправлять сразу после добавления", modifier = Modifier.weight(1f))
            Switch(checked = settings.autoSync, onCheckedChange = { vm.setAutoSync(it) })
        }
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { vm.setApi(url, token); vm.testConnection() },
                enabled = !vm.syncing, modifier = Modifier.weight(1f)
            ) { Text("Проверить") }
            Button(
                onClick = { vm.setApi(url, token); vm.sync() },
                enabled = !vm.syncing && unsynced > 0, modifier = Modifier.weight(1f)
            ) { Text("Отправить ($unsynced)") }
        }
        OutlinedButton(
            onClick = { vm.setApi(url, token); vm.pullFromSheets() },
            enabled = !vm.syncing,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Загрузить изменения из таблицы") }
        if (vm.syncing) CircularProgressIndicator()
        if (vm.message.isNotEmpty()) Text(vm.message, color = MaterialTheme.colorScheme.primary)
        Text(
            "При закрытии приложения данные синхронизируются с таблицей автоматически. " +
                "Токен хранится на устройстве; не публикуйте URL и токен.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
