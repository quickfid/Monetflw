package ru.familybudget.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
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

private enum class Page { MENU, PROFILE, CATEGORIES, MANDATORY, ACCOUNTS, PEOPLE, NOTIFICATIONS, SYNC, REPORTS }

private data class Quad(val title: String, val sub: String, val iconKey: String, val page: Page)

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
                        Page.NOTIFICATIONS -> "Уведомления"
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
            Page.NOTIFICATIONS -> NotificationsPage(vm)
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
    var confirmReset by remember { mutableStateOf(false) }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить все данные?") },
            text = { Text("Все операции, счета, планы и вклады на этом телефоне будут удалены, и откроется стартовая настройка заново.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; vm.resetAllData() }) {
                    Text("Сбросить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } }
        )
    }
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

    var reorderMode by remember { mutableStateOf(false) }
    val defaultMenuKeys = remember { listOf("profile", "accounts", "mandatory", "categories", "notifications", "people", "sync") }
    val orderedMenuKeys = remember(settings.menuOrder) {
        val custom = settings.menuOrder.filter { it in defaultMenuKeys }
        custom + defaultMenuKeys.filter { it !in custom }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ещё", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { reorderMode = !reorderMode }) {
                Text(if (reorderMode) "Готово ✓" else "↕ Порядок")
            }
        }
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Sheet.glass(),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
        ) {
            Column {
                orderedMenuKeys.forEachIndexed { idx, key ->
                    val entry = when (key) {
                        "profile" -> Quad("Профиль", "Имя, доход и даты поступлений", "other", Page.PROFILE)
                        "accounts" -> Quad("Счета", "Карты и наличные", "income", Page.ACCOUNTS)
                        "mandatory" -> Quad("Платежи", "Ежемесячные, окно дат и галочки", "utilities", Page.MANDATORY)
                        "categories" -> Quad("Категории", "Расходы и доходы", "shopping", Page.CATEGORIES)
                        "notifications" -> Quad("Уведомления", "Время, дни и напоминания", "phone", Page.NOTIFICATIONS)
                        "people" -> Quad("Люди", settings.persons.joinToString(", "), "family", Page.PEOPLE)
                        else -> Quad(
                            "Google Таблица",
                            if (settings.apiUrl.isBlank()) "Не подключена" else "Не отправлено: $unsynced",
                            "phone",
                            Page.SYNC
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            MenuItem(entry.title, entry.sub, entry.iconKey) {
                                if (!reorderMode) open(entry.page)
                            }
                        }
                        if (reorderMode) {
                            Row(Modifier.padding(end = 6.dp)) {
                                TextButton(
                                    onClick = {
                                        if (idx > 0) {
                                            val next = orderedMenuKeys.toMutableList()
                                            val tmp = next[idx - 1]
                                            next[idx - 1] = next[idx]
                                            next[idx] = tmp
                                            vm.setMenuOrder(next)
                                        }
                                    },
                                    enabled = idx > 0
                                ) { Text("↑") }
                                TextButton(
                                    onClick = {
                                        if (idx < orderedMenuKeys.lastIndex) {
                                            val next = orderedMenuKeys.toMutableList()
                                            val tmp = next[idx + 1]
                                            next[idx + 1] = next[idx]
                                            next[idx] = tmp
                                            vm.setMenuOrder(next)
                                        }
                                    },
                                    enabled = idx < orderedMenuKeys.lastIndex
                                ) { Text("↓") }
                            }
                        }
                    }
                }
                MenuItem("Отчёты", "Анализ доходов, расходов и динамики", "savings") { open(Page.REPORTS) }
                MenuItem("Отправить отчёт в таблицу", "Таблицы и графики для другого человека", "transfer") {
                    vm.exportReport(vm.month)
                }
                MenuItem("Экспорт CSV", "Все операции в файл", "education") {
                    exportLauncher.launch("budget_${LocalDate.now()}.csv")
                }
                MenuItem("Резервная копия", "Сохранить все данные в файл", "credit") {
                    backupLauncher.launch("family_budget_backup_${LocalDate.now()}.json")
                }
                MenuItem("Восстановить", "Из файла резервной копии", "health") { confirmRestore = true }
                MenuItem("Сбросить все данные", "Очистить всё и пройти настройку заново", "other") { confirmReset = true }
            }
        }
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Sheet.glass(),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
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
                HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
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
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        "Прозрачность стекла: ${(settings.glassOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.glassOpacity,
                        onValueChange = { vm.setGlassOpacity(it) },
                        valueRange = 0.20f..0.85f
                    )
                }
                if (settings.wallpaperBackground && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "Сила размытия обоев: ${settings.blurRadius}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.blurRadius.toFloat(),
                            onValueChange = { vm.setBlurRadius(it.toInt()) },
                            valueRange = 0f..120f
                        )
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Акцентный цвет", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(
                            Triple("calm_glass", "Матовое", Color(0xFF22343D)),
                            Triple("teal", "Бирюза", Color(0xFF1B6B7A)),
                            Triple("emerald", "Изумруд", Color(0xFF127A52)),
                            Triple("indigo", "Индиго", Color(0xFF3E44A8)),
                            Triple("graphite", "Графит", Color(0xFF2E3A47))
                        ).forEach { (key, label, swatch) ->
                            val selected = settings.accentTheme == key
                            Surface(
                                shape = CircleShape,
                                color = swatch,
                                border = BorderStroke(if (selected) 3.dp else 1.dp, if (selected) Color.White else Sheet.border()),
                                modifier = Modifier.size(34.dp).clickable { vm.setAccentTheme(key) }
                            ) {}
                        }
                    }
                }
                HorizontalDivider(color = Sheet.border().copy(alpha = 0.5f))
                MenuItem(
                    "Проверить обновления",
                    if (vm.checkingUpdate) "Проверяем…" else "Установить новую версию прямо в приложении",
                    "transfer"
                ) { vm.checkForUpdate(silent = false) }
            }
        }
        val ctx = LocalContext.current
        val appVer = remember(ctx) { AppUpdater.currentVersion(ctx) }
        Text(
            "Данные только на вашем телефоне · v$appVer",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MenuItem(title: String, subtitle: String, iconKey: String = "other", onClick: () -> Unit) {
    ListItem(
        leadingContent = { CategoryBadge(title, mapOf(title to iconKey), size = 34.dp) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, maxLines = 2) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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

        var sal1 by remember(settings.salary1Days) { mutableStateOf(settings.salary1Days) }
        var sal2 by remember(settings.salary2Days) { mutableStateOf(settings.salary2Days) }
        var benef by remember(settings.benefitsDay) { mutableStateOf(settings.benefitsDay) }
        var bonus by remember(settings.bonusDay) { mutableStateOf(settings.bonusDay) }
        Section("📅 Даты поступлений") {
            Text(
                "Укажите числа месяца (через запятую, если аванс и зарплата, например: 10, 25)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextField2(sal1, { sal1 = it }, "Зарплата 1-го участника (напр. 10, 25)")
            TextField2(sal2, { sal2 = it }, "Зарплата 2-го участника (напр. 5, 20)")
            TextField2(benef, { benef = it }, "Пособия (число месяца, напр. 3)")
            TextField2(bonus, { bonus = it }, "Ежемесячные премии (число, напр. 15)")
            OutlinedButton(
                onClick = { vm.setIncomeDates(sal1, sal2, benef, bonus) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить даты поступлений") }
        }
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
    val list = categories
        .filter { it.isIncome == income }
        .sortedWith(compareByDescending<Category> { it.limitCents > 0L }.thenBy { it.name })

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !income, onClick = { income = false }, label = { Text("Расходы") })
            FilterChip(selected = income, onClick = { income = true }, label = { Text("Доходы") })
        }
        list.forEach { c ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Sheet.glass(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                ListItem(
                    leadingContent = { CategoryBadge(c.name, settings.categoryIcons, size = 36.dp) },
                    headlineContent = { Text(c.name) },
                    supportingContent = {
                        if (!c.isIncome) Text(if (c.limitCents > 0) "Лимит: " + Fmt.money(c.limitCents, cur, false) else "Без лимита")
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { dialog = c }
                )
            }
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
        items.sortedBy { it.day }.forEach { m ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Sheet.glass(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                ListItem(
                    headlineContent = { Text(m.name) },
                    supportingContent = {
                        val dateStr = if (m.endDay > m.day) "с ${m.day} по ${m.endDay} числа" else "до ${m.day} числа"
                        Text("$dateStr · ${m.category}")
                    },
                    trailingContent = { Text(Fmt.money(m.amountCents, cur)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { dialog = m }
                )
            }
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
        var endDay by remember(m) { mutableStateOf(if (m.endDay > m.day) m.endDay.toString() else "") }
        var amount by remember(m) { mutableStateOf(if (m.amountCents > 0) Fmt.plain(m.amountCents) else "") }
        var category by remember(m) { mutableStateOf(m.category) }
        val cents = Fmt.parseCents(amount)
        val dayNum = day.toIntOrNull()
        val endDayNum = endDay.toIntOrNull() ?: 0
        val validEnd = endDay.isBlank() || (endDayNum in 1..31 && dayNum != null && endDayNum >= dayNum)
        FormDialog(
            title = if (m.id == 0L) "Новый платёж" else "Платёж",
            onDismiss = { dialog = null },
            confirmEnabled = name.isNotBlank() && cents != null && cents > 0 && dayNum != null && dayNum in 1..31 && validEnd,
            onConfirm = {
                val finalEnd = if (endDayNum > (dayNum ?: 1)) endDayNum else 0
                vm.saveMandatory(
                    m.copy(
                        name = name.trim(),
                        day = dayNum ?: 1,
                        endDay = finalEnd,
                        amountCents = cents ?: 0L,
                        category = category
                    )
                )
                dialog = null
            },
            onDelete = if (m.id != 0L) ({ vm.deleteMandatory(m); dialog = null }) else null
        ) {
            TextField2(name, { name = it }, "Название")
            MoneyField(amount, { amount = it }, "Сумма, $cur")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = day,
                    onValueChange = { if (it.length <= 2 && it.all { ch -> ch.isDigit() }) day = it },
                    label = { Text("С числа", maxLines = 1) },
                    placeholder = { Text("1–31") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endDay,
                    onValueChange = { if (it.length <= 2 && it.all { ch -> ch.isDigit() }) endDay = it },
                    label = { Text("По число", maxLines = 1) },
                    placeholder = { Text("опц.") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Если указать «По число», уведомления «Обязательные платежи» и «Утренний брифинг» будут напоминать в течение этого окна, пока платёж не оплачен.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Sheet.glass(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                ListItem(
                    headlineContent = { Text(a.name) },
                    supportingContent = {
                        Text(
                            if (a.initialCents >= 0L) "Начальный остаток: " + Fmt.money(a.initialCents, cur)
                            else "Баланс скорректирован под стартовые операции"
                        )
                    },
                    trailingContent = { Text(Fmt.money(ru.familybudget.app.logic.Stats.accountBalance(a, ops), cur)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { dialog = a }
                )
            }
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
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Sheet.glass(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
            ) {
                ListItem(
                    headlineContent = { Text(p) },
                    trailingContent = {
                        if (settings.persons.size > 1) TextButton(onClick = { vm.removePerson(p) }) { Text("Удалить") }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
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

// ---------------------------------------------------------------- уведомления
@Composable
private fun NotificationsPage(vm: BudgetViewModel) {
    val s by vm.settings.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val needPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (needPermission) {
            Section("🔔 Разрешение на уведомления") {
                Text(
                    "На Android 13+ нужно разрешить показ уведомлений, чтобы напоминания приходили вовремя.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Разрешить уведомления") }
            }
        }

        val listenerEnabled = remember(s.bankAutoTrackEnabled) {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            flat.contains(context.packageName)
        }
        Section("🏦 Авто-учёт оплат из банков (Push)") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Записывать оплаты автоматически",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Сбер, Т-Банк, Альфа, ВТБ, Ozon, Яндекс Пэй. Только покупки и оплаты — переводы и СБП игнорируются.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = s.bankAutoTrackEnabled,
                    onCheckedChange = { vm.setBankAutoTrack(it) }
                )
            }
            Text(
                if (listenerEnabled) "✅ Доступ к уведомлениям Android выдан"
                else "⚠️ Чтобы читать Push от банков, выдайте доступ к уведомлениям Android",
                style = MaterialTheme.typography.bodySmall,
                color = if (listenerEnabled) AppColors.income() else AppColors.warning()
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Открыть доступ к уведомлениям Android") }
        }

        NotifCard(
            title = "🌅 Утренний брифинг",
            subtitle = "Свободный лимит и обязательные платежи, у которых сегодня окно оплаты",
            enabled = s.notifMorningEnabled,
            onToggle = { on -> vm.updateNotifications { it.copy(notifMorningEnabled = on) } },
            hour = s.notifMorningHour,
            onHourChange = { h -> vm.updateNotifications { it.copy(notifMorningHour = h) } }
        )

        NotifCard(
            title = "🌙 Вечернее напоминание",
            subtitle = "Приходит только если за сегодня ещё не внесено ни одной операции",
            enabled = s.notifEveningEnabled,
            onToggle = { on -> vm.updateNotifications { it.copy(notifEveningEnabled = on, dailyReminder = on) } },
            hour = s.notifEveningHour,
            onHourChange = { h -> vm.updateNotifications { it.copy(notifEveningHour = h) } }
        )

        NotifCard(
            title = "📅 Обязательные платежи",
            subtitle = "Связано с датами и окном оплаты («с … по … число») в разделе «Платежи»",
            enabled = s.notifBillsEnabled,
            onToggle = { on -> vm.updateNotifications { it.copy(notifBillsEnabled = on) } },
            hour = s.notifBillsHour,
            onHourChange = { h -> vm.updateNotifications { it.copy(notifBillsHour = h) } },
            extraLabel = "Напомнить заранее",
            extraValue = if (s.notifBillsDaysBefore == 0) "В день оплаты" else "За ${s.notifBillsDaysBefore} дн.",
            onExtraMinus = { vm.updateNotifications { it.copy(notifBillsDaysBefore = (it.notifBillsDaysBefore - 1).coerceAtLeast(0)) } },
            onExtraPlus = { vm.updateNotifications { it.copy(notifBillsDaysBefore = (it.notifBillsDaysBefore + 1).coerceAtMost(7)) } }
        )

        NotifCard(
            title = "⚠️ Предупреждение о лимите",
            subtitle = "Сообщает, если какая-то категория расходов достигла 85% месячного лимита",
            enabled = s.notifLimitEnabled,
            onToggle = { on -> vm.updateNotifications { it.copy(notifLimitEnabled = on) } },
            hour = s.notifLimitHour,
            onHourChange = { h -> vm.updateNotifications { it.copy(notifLimitHour = h) } }
        )

        NotifCard(
            title = "📊 Итоги недели",
            subtitle = "Сумма трат за последние 7 дней и главная категория расходов",
            enabled = s.notifWeeklyEnabled,
            onToggle = { on -> vm.updateNotifications { it.copy(notifWeeklyEnabled = on) } },
            hour = s.notifWeeklyHour,
            onHourChange = { h -> vm.updateNotifications { it.copy(notifWeeklyHour = h) } },
            extraLabel = "День недели",
            extraValue = weekDays[(s.notifWeeklyDay - 1).coerceIn(0, 6)],
            onExtraMinus = { vm.updateNotifications { it.copy(notifWeeklyDay = if (it.notifWeeklyDay <= 1) 7 else it.notifWeeklyDay - 1) } },
            onExtraPlus = { vm.updateNotifications { it.copy(notifWeeklyDay = if (it.notifWeeklyDay >= 7) 1 else it.notifWeeklyDay + 1) } }
        )

        NotifCard(
            title = "🛡️ Пополнение подушки",
            subtitle = "Напоминает пополнить финансовую подушку или перенести свободный остаток",
            enabled = s.notifCushionEnabled,
            onToggle = { on -> vm.updateNotifications { it.copy(notifCushionEnabled = on) } },
            hour = s.notifCushionHour,
            onHourChange = { h -> vm.updateNotifications { it.copy(notifCushionHour = h) } },
            extraLabel = "Число месяца",
            extraValue = "${s.notifCushionDay}-го",
            onExtraMinus = { vm.updateNotifications { it.copy(notifCushionDay = if (it.notifCushionDay <= 1) 28 else it.notifCushionDay - 1) } },
            onExtraPlus = { vm.updateNotifications { it.copy(notifCushionDay = if (it.notifCushionDay >= 28) 1 else it.notifCushionDay + 1) } }
        )

        OutlinedButton(
            onClick = { vm.sendTestNotification() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("🔔 Отправить тестовое уведомление") }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NotifCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    hour: Int,
    onHourChange: (Int) -> Unit,
    extraLabel: String? = null,
    extraValue: String? = null,
    onExtraMinus: (() -> Unit)? = null,
    onExtraPlus: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Sheet.glass(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.2.dp, Sheet.glassBorderBrush())
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (enabled) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Время", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onHourChange(if (hour <= 0) 23 else hour - 1) }) { Text("−") }
                        Text(
                            String.format(java.util.Locale.getDefault(), "%02d:00", hour),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { onHourChange(if (hour >= 23) 0 else hour + 1) }) { Text("+") }
                    }
                }
                if (extraLabel != null && extraValue != null && onExtraMinus != null && onExtraPlus != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(extraLabel, style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onExtraMinus) { Text("−") }
                            Text(extraValue, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = onExtraPlus) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}
