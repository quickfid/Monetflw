package ru.familybudget.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.familybudget.app.data.Account
import ru.familybudget.app.data.AppSettings
import ru.familybudget.app.data.BudgetDb
import ru.familybudget.app.data.Category
import ru.familybudget.app.data.Deposit
import ru.familybudget.app.data.InterestEntry
import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.PlanItem
import ru.familybudget.app.logic.Deposits
import ru.familybudget.app.logic.Plans
import ru.familybudget.app.logic.Stats
import ru.familybudget.app.data.Operation
import ru.familybudget.app.data.SettingsStore
import ru.familybudget.app.data.SheetsSync
import ru.familybudget.app.logic.Fmt
import org.json.JSONArray
import org.json.JSONObject
import androidx.room.withTransaction
import java.time.LocalDate
import java.time.YearMonth

/** Всё, что человек ввёл на экране первоначальной настройки. */
data class OnboardingData(
    val name: String,
    val partner: String,
    val currency: String,
    val cardCents: Long,
    val cashCents: Long,
    val incomeCents: Long,
    val recordIncome: Boolean,
    /** План расходов на текущий месяц: категория → сумма в копейках. */
    val plan: Map<String, Long>,
    /** Обязательные платежи в этом месяце уже оплачены (остаток указан уже после них). */
    val billsAlreadyPaid: Boolean = false,
    /** Сколько % от дохода откладывать на подушку безопасности (5, 10, 15, 20). */
    val savingsPercent: Int = 10
)

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val db = BudgetDb.get(app)
    private val dao = db.dao()
    private val store = SettingsStore(app)

    val settings: StateFlow<AppSettings> = store.flow
    val operations: StateFlow<List<Operation>> =
        dao.operations().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories: StateFlow<List<Category>> =
        dao.categories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val accounts: StateFlow<List<Account>> =
        dao.accounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val mandatory: StateFlow<List<Mandatory>> =
        dao.mandatory().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val plans: StateFlow<List<PlanItem>> =
        dao.plans().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val deposits: StateFlow<List<Deposit>> =
        dao.deposits().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val interest: StateFlow<List<InterestEntry>> =
        dao.interest().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val unsyncedCount: StateFlow<Int> =
        operations.map { list -> list.count { !it.synced } }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    var month by mutableStateOf(YearMonth.now())
        private set
    var year by mutableIntStateOf(LocalDate.now().year)
        private set
    var editing by mutableStateOf<Operation?>(null)
        private set
    var syncing by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    /** Месяц, для которого сейчас открыт редактор плана (null — закрыт). */
    var planEditing by mutableStateOf<YearMonth?>(null)
        private set
    /** Выгрузка красивого отчёта в Google Таблицу: 0 — нет, 1 — идёт, 2 — готово, 3 — ошибка. */
    var exportState by mutableIntStateOf(0)
        private set
    var exportUrl by mutableStateOf("")
        private set
    var exportMessage by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.accountCount() == 0) {
                dao.insertAccount(Account(name = "Карта"))
                dao.insertAccount(Account(name = "Наличные"))
            }
            if (dao.categoryCount() == 0) dao.insertCategories(defaultCategories())
            // Для тех, кто обновился: гарантируем наличие ключевых категорий.
            val existing = dao.categoriesOnce().filter { !it.isIncome }.map { it.name }
            listOf(
                "Аренда квартиры", "Транспорт и бензин", "Связь и интернет",
                "Стрижка, маникюр", "Продукты", "Развлечения и досуг", "Коммунальные платежи",
                "Кафе и рестораны", "Подписки", "Подарки", "Спорт", "Кредит", "Прочие расходы"
            ).forEach { n ->
                if (n !in existing) dao.insertCategory(Category(name = n, isIncome = false))
            }
            ensureInterest()
        }
    }

    // ---------- навигация по периодам
    fun shiftMonth(delta: Int) { month = month.plusMonths(delta.toLong()) }
    fun shiftYear(delta: Int) { year += delta }

    // ---------- редактор операции
    fun startNew(type: String = OpType.EXPENSE) {
        val firstAccount = accounts.value.firstOrNull()?.name.orEmpty()
        editing = Operation(
            date = LocalDate.now().toEpochDay(),
            amountCents = 0,
            type = type,
            category = "",
            person = settings.value.persons.firstOrNull().orEmpty(),
            account = firstAccount
        )
    }

    fun edit(op: Operation) { editing = op }
    fun closeEditor() { editing = null }

    fun saveOperation(op: Operation) {
        viewModelScope.launch {
            val catName = op.category.trim()
            if (op.type != OpType.TRANSFER && catName.isNotEmpty()) {
                val isInc = op.type == OpType.INCOME
                val exists = dao.categoriesOnce().any { it.isIncome == isInc && it.name.equals(catName, ignoreCase = true) }
                if (!exists) dao.insertCategory(Category(name = catName, isIncome = isInc))
            }
            val clean = op.copy(category = catName, synced = false)
            if (clean.id == 0L) dao.insertOperation(clean) else dao.updateOperation(clean)
            editing = null
            if (settings.value.autoSync) sync()
        }
    }

    fun deleteOperation(op: Operation) {
        viewModelScope.launch {
            dao.deleteOperation(op)
            editing = null
        }
    }

    // ---------- обязательные платежи: галочка создаёт/удаляет расход
    fun toggleMandatory(m: Mandatory, ym: YearMonth, paid: Boolean) {
        viewModelScope.launch {
            val start = ym.atDay(1).toEpochDay()
            val end = ym.atEndOfMonth().toEpochDay()
            if (paid) {
                val today = LocalDate.now()
                val date = if (YearMonth.from(today) == ym) today else ym.atDay(minOf(m.day, ym.lengthOfMonth()))
                dao.insertOperation(
                    Operation(
                        date = date.toEpochDay(),
                        amountCents = m.amountCents,
                        type = OpType.EXPENSE,
                        category = m.category.ifBlank { "Обязательные платежи" },
                        comment = m.name,
                        person = settings.value.persons.firstOrNull().orEmpty(),
                        account = accounts.value.firstOrNull()?.name.orEmpty(),
                        mandatoryId = m.id
                    )
                )
                if (settings.value.autoSync) sync()
            } else {
                operations.value
                    .filter { it.mandatoryId == m.id && it.date in start..end }
                    .forEach { dao.deleteOperation(it) }
            }
        }
    }

    // ---------- первая настройка и профиль
    fun completeOnboarding(d: OnboardingData) {
        viewModelScope.launch {
            val nowYm = YearMonth.now()
            val name = d.name.trim()
            val people = listOf(name, d.partner.trim()).filter { it.isNotEmpty() }.distinct()
            val personName = name.ifEmpty { settings.value.persons.firstOrNull().orEmpty() }
            store.update { st ->
                st.copy(
                    userName = name,
                    persons = if (people.isEmpty()) st.persons else people,
                    currency = d.currency.ifBlank { "₽" },
                    monthlyIncome = d.incomeCents,
                    savingsPercent = d.savingsPercent,
                    fixedPromptYm = nowYm.toString(),
                    onboarded = true
                )
            }
            val accs = accounts.value
            val primaryAccount = accs.firstOrNull()?.name.orEmpty()

            // Сначала сохраняем план (он создаёт обязательные платежи).
            savePlanInternal(nowYm, d.plan, d.incomeCents)

            // Если обязательные платежи уже оплачены и остаток на карте указан уже после них,
            // отмечаем их оплаченными и компенсируем стартовый баланс, чтобы остаток не уходил в минус.
            var paidBillsCents = 0L
            if (d.billsAlreadyPaid) {
                val todayEpoch = LocalDate.now().toEpochDay()
                val bills = dao.mandatoryOnce().filter { it.amountCents > 0 }
                for (m in bills) {
                    dao.insertOperation(
                        Operation(
                            date = todayEpoch,
                            amountCents = m.amountCents,
                            type = OpType.EXPENSE,
                            category = m.category.ifBlank { m.name },
                            comment = "Оплачено до начала учёта",
                            person = personName,
                            account = primaryAccount,
                            mandatoryId = m.id
                        )
                    )
                    paidBillsCents += m.amountCents
                }
            }

            val recordIncome = d.recordIncome && d.incomeCents > 0
            val cardInitial = d.cardCents - (if (recordIncome) d.incomeCents else 0L) + paidBillsCents
            if (accs.isNotEmpty()) dao.updateAccount(accs[0].copy(initialCents = cardInitial))
            if (accs.size > 1) dao.updateAccount(accs[1].copy(initialCents = d.cashCents))
            if (recordIncome) {
                dao.insertOperation(
                    Operation(
                        date = LocalDate.now().toEpochDay(),
                        amountCents = d.incomeCents,
                        type = OpType.INCOME,
                        category = categories.value.firstOrNull { it.isIncome }?.name ?: "Зарплата",
                        comment = "Доход за месяц",
                        person = personName,
                        account = primaryAccount
                    )
                )
            }
        }
    }

    fun skipOnboarding() { store.update { it.copy(onboarded = true) } }

    fun saveProfile(name: String, incomeCents: Long, budgetCents: Long) {
        store.update { st ->
            val n = name.trim()
            val people = if (n.isEmpty() || n in st.persons) st.persons else listOf(n) + st.persons
            st.copy(userName = n, persons = people, monthlyIncome = incomeCents, monthlyBudget = budgetCents)
        }
        message = "Сохранено ✅"
    }

    fun hideTips() { store.update { it.copy(hideTips = true) } }

    // ---------- план на месяц
    fun openPlan(ym: YearMonth) { planEditing = ym }
    fun closePlan() { planEditing = null }

    fun savePlan(ym: YearMonth, items: Map<String, Long>, incomeCents: Long) {
        viewModelScope.launch {
            savePlanInternal(ym, items, incomeCents)
            planEditing = null
        }
    }

    private suspend fun savePlanInternal(ym: YearMonth, items: Map<String, Long>, incomeCents: Long) {
        val rows = items.filter { it.value > 0 }.map { PlanItem(ym = ym.toString(), category = it.key, amountCents = it.value) }
        db.withTransaction {
            dao.clearPlan(ym.toString())
            if (rows.isNotEmpty()) dao.insertPlans(rows)
        }
        val total = rows.sumOf { it.amountCents }
        store.update { st ->
            st.copy(
                monthlyIncome = if (incomeCents > 0) incomeCents else st.monthlyIncome,
                monthlyBudget = if (total > 0 && ym >= YearMonth.now()) total else st.monthlyBudget,
                fixedPromptYm = if (ym == YearMonth.now()) ym.toString() else st.fixedPromptYm
            )
        }
        syncBills(ym, rows)
        message = "План сохранён 🎉"
    }

    /** Ежемесячные платежи (аренда, кредит, связь, коммунальные) получают галочку «оплачено». */
    private suspend fun syncBills(ym: YearMonth, rows: List<PlanItem>) {
        if (ym < YearMonth.now()) return
        val existing = dao.mandatoryOnce()
        rows.filter { Fmt.isBill(it.category) }.forEach { r ->
            val found = existing.firstOrNull { it.category == r.category }
            if (found != null) {
                dao.updateMandatory(found.copy(amountCents = r.amountCents))
            } else {
                dao.insertMandatory(
                    Mandatory(name = r.category, day = Fmt.billDay(r.category), amountCents = r.amountCents, category = r.category)
                )
            }
        }
    }

    /** Ежемесячный вопрос про аренду, кредит, связь и коммунальные: сохраняем только эти строки плана. */
    fun saveFixed(ym: YearMonth, items: Map<String, Long>) {
        viewModelScope.launch {
            val key = ym.toString()
            val rows = items.filter { it.value > 0 }.map { PlanItem(ym = key, category = it.key, amountCents = it.value) }
            db.withTransaction {
                items.keys.forEach { dao.deletePlanItem(key, it) }
                if (rows.isNotEmpty()) dao.insertPlans(rows)
            }
            syncBills(ym, rows)
            store.update { it.copy(fixedPromptYm = key) }
            message = "Платежи сохранены ✅"
        }
    }

    fun dismissFixed(ym: YearMonth) { store.update { it.copy(fixedPromptYm = ym.toString()) } }

    // ---------- вклады и накопительные счета
    private suspend fun ensureInterest() {
        val now = YearMonth.now()
        val all = dao.interestOnce()
        dao.depositsOnce().forEach { d ->
            val add = Deposits.missing(d, all.filter { it.depositId == d.id }, now)
            if (add.isNotEmpty()) dao.insertInterest(add)
        }
    }

    /** balanceCents = null означает «сумму не меняли». Новая сумма становится точкой отсчёта с этого месяца. */
    fun saveDeposit(d: Deposit, balanceCents: Long?) {
        viewModelScope.launch {
            val now = YearMonth.now().toString()
            if (d.id == 0L) {
                dao.insertDeposit(d.copy(principalCents = balanceCents ?: 0L, baselineYm = now))
            } else if (balanceCents != null) {
                dao.updateDeposit(d.copy(principalCents = balanceCents, baselineYm = now))
                dao.clearInterest(d.id)
            } else {
                dao.updateDeposit(d)
            }
            ensureInterest()
            message = "Сохранено ✅"
        }
    }

    fun deleteDeposit(d: Deposit) {
        viewModelScope.launch {
            dao.clearInterest(d.id)
            dao.deleteDeposit(d)
        }
    }

    /** Исправить сумму процентов за конкретный месяц. */
    fun saveInterest(e: InterestEntry, amountCents: Long) {
        viewModelScope.launch { dao.updateInterest(e.copy(amountCents = amountCents, manual = true)) }
    }

    /** Пересчитать проценты заново, кроме тех, что вы исправили вручную. */
    fun recalcInterest(d: Deposit) {
        viewModelScope.launch {
            dao.clearAutoInterest(d.id)
            ensureInterest()
            message = "Проценты пересчитаны"
        }
    }

    // ---------- справочники
    fun saveCategory(c: Category, oldName: String?) {
        viewModelScope.launch {
            if (c.id == 0L) {
                dao.insertCategory(c)
            } else {
                dao.updateCategory(c)
                if (oldName != null && oldName != c.name) {
                    dao.renameCategoryInOperations(oldName, c.name)
                    dao.renameCategoryInMandatory(oldName, c.name)
                    dao.renameCategoryInPlans(oldName, c.name)
                }
            }
        }
    }

    fun deleteCategory(c: Category) { viewModelScope.launch { dao.deleteCategory(c) } }

    fun saveAccount(a: Account, oldName: String?) {
        viewModelScope.launch {
            if (a.id == 0L) {
                dao.insertAccount(a)
            } else {
                dao.updateAccount(a)
                if (oldName != null && oldName != a.name) {
                    dao.renameAccountInOperations(oldName, a.name)
                    dao.renameDestAccountInOperations(oldName, a.name)
                }
            }
        }
    }

    fun deleteAccount(a: Account) { viewModelScope.launch { dao.deleteAccount(a) } }

    fun saveMandatory(m: Mandatory) {
        viewModelScope.launch { if (m.id == 0L) dao.insertMandatory(m) else dao.updateMandatory(m) }
    }

    fun deleteMandatory(m: Mandatory) { viewModelScope.launch { dao.deleteMandatory(m) } }

    // ---------- настройки
    fun addPerson(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        store.update { s -> if (n in s.persons) s else s.copy(persons = s.persons + n) }
    }

    fun removePerson(name: String) {
        store.update { s -> if (s.persons.size > 1) s.copy(persons = s.persons - name) else s }
    }

    fun setCurrency(value: String) { store.update { it.copy(currency = value.trim().ifBlank { "₽" }) } }
    fun setApi(url: String, token: String) { store.update { it.copy(apiUrl = url.trim(), token = token.trim()) } }
    fun setAutoSync(value: Boolean) { store.update { it.copy(autoSync = value) } }
    fun setDailyReminder(enabled: Boolean) {
        store.update { it.copy(dailyReminder = enabled) }
        val ctx = getApplication<Application>()
        if (enabled) ru.familybudget.app.logic.ReminderScheduler.scheduleDaily20(ctx)
        else ru.familybudget.app.logic.ReminderScheduler.cancel(ctx)
    }
    fun setSavingsPercent(pct: Int) { store.update { it.copy(savingsPercent = pct) } }
    fun setStrategy(id: String) { store.update { it.copy(activeStrategy = id) } }
    fun saveGoal(goal: ru.familybudget.app.data.SavingsGoal) {
        store.update { st ->
            val id = if (goal.id == 0L) (st.goals.maxOfOrNull { it.id } ?: 0L) + 1L else goal.id
            val next = goal.copy(id = id)
            val list = if (st.goals.any { it.id == id }) st.goals.map { if (it.id == id) next else it } else st.goals + next
            st.copy(goals = list)
        }
    }
    fun deleteGoal(id: Long) {
        store.update { st -> st.copy(goals = st.goals.filterNot { it.id == id }) }
    }
    fun setCategoryIcon(categoryName: String, iconKey: String) {
        store.update { st ->
            val next = st.categoryIcons.toMutableMap()
            if (iconKey.isBlank()) next.remove(categoryName) else next[categoryName] = iconKey
            st.copy(categoryIcons = next)
        }
    }

    // ---------- синхронизация с Google-таблицей
    fun sync() {
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            message = SheetsSync.push(dao, settings.value)
            syncing = false
        }
    }

    fun pullFromSheets() {
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            message = SheetsSync.pull(dao, settings.value)
            syncing = false
        }
    }

    fun syncAll() {
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            message = SheetsSync.syncBoth(dao, settings.value)
            syncing = false
        }
    }

    /** Тихая двусторонняя синхронизация при сворачивании/закрытии приложения. */
    fun syncOnBackground() {
        val st = settings.value
        if (syncing || st.apiUrl.isBlank() || st.token.isBlank()) return
        viewModelScope.launch {
            syncing = true
            try {
                SheetsSync.syncBoth(dao, st)
            } finally {
                syncing = false
            }
        }
    }

    fun testConnection() {
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            message = SheetsSync.test(settings.value)
            syncing = false
        }
    }

    // ---------- экспорт CSV
    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val text = buildCsv()
            message = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    }
                    "Файл сохранён (${operations.value.size} операций)."
                } catch (e: Exception) {
                    "Не удалось сохранить файл: ${e.localizedMessage ?: "ошибка"}"
                }
            }
        }
    }

    private fun buildCsv(): String {
        fun q(s: String): String =
            if (s.contains(';') || s.contains('"') || s.contains('\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s

        val sb = StringBuilder("\uFEFF")
        sb.append("Дата;Сумма;Тип;Категория;Комментарий;Кто;Счёт;На счёт\n")
        for (o in operations.value.sortedBy { it.date }) {
            val cells = listOf(
                LocalDate.ofEpochDay(o.date).toString(), Fmt.plain(o.amountCents), o.type,
                o.category, o.comment, o.person, o.account, o.destAccount
            )
            sb.append(cells.joinToString(";") { q(it) }).append('\n')
        }
        return sb.toString()
    }

    // ---------- резервная копия (JSON)
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val text = buildBackup()
            message = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    }
                    "Резервная копия сохранена."
                } catch (e: Exception) {
                    "Не удалось сохранить копию: ${e.localizedMessage ?: "ошибка"}"
                }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            message = withContext(Dispatchers.IO) {
                try {
                    val text = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: return@withContext "Не удалось открыть файл."
                    restoreBackup(text)
                } catch (e: Exception) {
                    "Не удалось прочитать копию: ${e.localizedMessage ?: "неверный файл"}"
                }
            }
        }
    }

    private fun buildBackup(): String {
        val root = JSONObject()
        root.put("app", "family-budget")
        root.put("version", 1)
        root.put("currency", settings.value.currency)
        root.put("persons", JSONArray(settings.value.persons))
        root.put("userName", settings.value.userName)
        root.put("monthlyIncome", settings.value.monthlyIncome)
        root.put("monthlyBudget", settings.value.monthlyBudget)
        val accs = JSONArray()
        accounts.value.forEach { accs.put(JSONObject().put("name", it.name).put("initial", it.initialCents)) }
        root.put("accounts", accs)
        val cats = JSONArray()
        categories.value.forEach {
            cats.put(JSONObject().put("name", it.name).put("income", it.isIncome).put("limit", it.limitCents))
        }
        root.put("categories", cats)
        val mand = JSONArray()
        mandatory.value.forEach {
            mand.put(
                JSONObject().put("id", it.id).put("name", it.name).put("day", it.day)
                    .put("amount", it.amountCents).put("category", it.category)
            )
        }
        root.put("mandatory", mand)
        val planArr = JSONArray()
        plans.value.forEach {
            planArr.put(JSONObject().put("ym", it.ym).put("category", it.category).put("amount", it.amountCents))
        }
        root.put("plans", planArr)
        val depArr = JSONArray()
        deposits.value.forEach {
            depArr.put(
                JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind)
                    .put("principal", it.principalCents).put("baseline", it.baselineYm)
                    .put("rate", it.ratePercent).put("period", it.ratePeriod).put("compound", it.compound)
            )
        }
        root.put("deposits", depArr)
        val intArr = JSONArray()
        interest.value.forEach {
            intArr.put(
                JSONObject().put("deposit", it.depositId).put("ym", it.ym)
                    .put("amount", it.amountCents).put("manual", it.manual)
            )
        }
        root.put("interest", intArr)
        val opsArr = JSONArray()
        operations.value.sortedBy { it.date }.forEach { o ->
            val j = JSONObject()
                .put("uid", o.uid).put("date", o.date).put("amount", o.amountCents).put("type", o.type)
                .put("category", o.category).put("comment", o.comment).put("person", o.person)
                .put("account", o.account).put("dest", o.destAccount).put("synced", o.synced)
            if (o.mandatoryId != null) j.put("mandatoryId", o.mandatoryId)
            opsArr.put(j)
        }
        root.put("operations", opsArr)
        return root.toString()
    }

    private suspend fun restoreBackup(text: String): String {
        val root = JSONObject(text)
        if (root.optString("app") != "family-budget") return "Это не резервная копия «Семейного бюджета»."
        val accs = root.getJSONArray("accounts")
        val cats = root.getJSONArray("categories")
        val mand = root.getJSONArray("mandatory")
        val opsArr = root.getJSONArray("operations")
        var restored = 0
        db.withTransaction {
            dao.clearOperations()
            dao.clearMandatory()
            dao.clearCategories()
            dao.clearAccounts()
            dao.clearAllPlans()
            dao.clearDeposits()
            dao.clearAllInterest()
            val depJson = root.optJSONArray("deposits")
            val interestJson = root.optJSONArray("interest")
            if (depJson != null) {
                val idMap2 = HashMap<Long, Long>()
                for (i in 0 until depJson.length()) {
                    val o = depJson.getJSONObject(i)
                    val newId = dao.insertDeposit(
                        Deposit(
                            name = o.getString("name"), kind = o.optString("kind", "Вклад"),
                            principalCents = o.optLong("principal"), baselineYm = o.getString("baseline"),
                            ratePercent = o.optDouble("rate", 0.0), ratePeriod = o.optInt("period", 0),
                            compound = o.optBoolean("compound", true)
                        )
                    )
                    idMap2[o.optLong("id")] = newId
                }
                if (interestJson != null) {
                    val entries = ArrayList<InterestEntry>()
                    for (i in 0 until interestJson.length()) {
                        val o = interestJson.getJSONObject(i)
                        val newDep = idMap2[o.optLong("deposit")] ?: continue
                        entries.add(
                            InterestEntry(depositId = newDep, ym = o.getString("ym"), amountCents = o.optLong("amount"), manual = o.optBoolean("manual"))
                        )
                    }
                    if (entries.isNotEmpty()) dao.insertInterest(entries)
                }
            }
            val planJson = root.optJSONArray("plans")
            if (planJson != null) {
                val list = ArrayList<PlanItem>()
                for (i in 0 until planJson.length()) {
                    val o = planJson.getJSONObject(i)
                    list.add(PlanItem(ym = o.getString("ym"), category = o.getString("category"), amountCents = o.optLong("amount")))
                }
                if (list.isNotEmpty()) dao.insertPlans(list)
            }
            for (i in 0 until accs.length()) {
                val o = accs.getJSONObject(i)
                dao.insertAccount(Account(name = o.getString("name"), initialCents = o.optLong("initial")))
            }
            for (i in 0 until cats.length()) {
                val o = cats.getJSONObject(i)
                dao.insertCategory(
                    Category(name = o.getString("name"), isIncome = o.optBoolean("income"), limitCents = o.optLong("limit"))
                )
            }
            val idMap = HashMap<Long, Long>()
            for (i in 0 until mand.length()) {
                val o = mand.getJSONObject(i)
                val newId = dao.insertMandatory(
                    Mandatory(
                        name = o.getString("name"), day = o.optInt("day", 1),
                        amountCents = o.optLong("amount"), category = o.optString("category")
                    )
                )
                idMap[o.optLong("id")] = newId
            }
            for (i in 0 until opsArr.length()) {
                val o = opsArr.getJSONObject(i)
                val oldMandatory = if (o.has("mandatoryId")) o.optLong("mandatoryId") else null
                dao.insertOperation(
                    Operation(
                        uid = o.optString("uid").ifBlank { java.util.UUID.randomUUID().toString() },
                        date = o.getLong("date"),
                        amountCents = o.getLong("amount"),
                        type = o.getString("type"),
                        category = o.optString("category"),
                        comment = o.optString("comment"),
                        person = o.optString("person"),
                        account = o.optString("account"),
                        destAccount = o.optString("dest"),
                        mandatoryId = if (oldMandatory != null) idMap[oldMandatory] else null,
                        synced = o.optBoolean("synced")
                    )
                )
                restored++
            }
        }
        val persons = root.optJSONArray("persons")
        val list = ArrayList<String>()
        if (persons != null) {
            for (i in 0 until persons.length()) {
                val n = persons.optString(i).trim()
                if (n.isNotEmpty()) list.add(n)
            }
        }
        val currency = root.optString("currency", "₽")
        store.update { st ->
            st.copy(
                persons = if (list.isEmpty()) st.persons else list,
                currency = currency.ifBlank { "₽" },
                userName = root.optString("userName", st.userName),
                monthlyIncome = root.optLong("monthlyIncome", st.monthlyIncome),
                monthlyBudget = root.optLong("monthlyBudget", st.monthlyBudget),
                onboarded = true
            )
        }
        return "Восстановлено операций: $restored."
    }

    // ---------- красивый отчёт в Google Таблицу
    fun exportReport(ym: YearMonth) {
        if (exportState == 1) return
        val st = settings.value
        if (st.apiUrl.isBlank() || st.token.isBlank()) {
            exportState = 3
            exportMessage = "Сначала подключите таблицу: Ещё → Google Таблица."
            return
        }
        exportState = 1
        exportUrl = ""
        viewModelScope.launch {
            val payload = withContext(Dispatchers.Default) { buildReportJson(ym, st) }
            val result = SheetsSync.exportReport(st, payload)
            exportUrl = result.url
            exportMessage = result.message
            exportState = if (result.url.isNotEmpty()) 2 else 3
        }
    }

    fun closeExport() { exportState = 0 }

    private fun buildReportJson(ym: YearMonth, st: AppSettings): String {
        val today = LocalDate.now()
        val md = Plans.monthData(
            operations.value, accounts.value, mandatory.value, plans.value, categories.value, st, ym, today
        )
        fun rub(c: Long): Double = c / 100.0
        val root = JSONObject()
        root.put("token", st.token)
        root.put("action", "exportReport")

        val meta = JSONObject()
        meta.put("name", st.userName)
        meta.put("currency", st.currency)
        meta.put("ym", ym.toString())
        meta.put("title", Fmt.monthTitle(ym))
        meta.put("generated", today.toString())
        meta.put("income", rub(md.summary.income))
        meta.put("expense", rub(md.summary.expense))
        meta.put("opening", rub(md.summary.opening))
        meta.put("closing", rub(md.summary.closing))
        meta.put("budget", rub(md.budget))
        meta.put("projected", rub(md.forecast.projectedExpense))
        meta.put("elapsed", md.forecast.elapsed)
        meta.put("days", md.forecast.daysInMonth)
        meta.put("verdict", md.forecast.verdict.name.lowercase())
        root.put("meta", meta)

        val names = (md.limits.keys + md.spentByCategory.keys).toList().sortedBy { Fmt.planRank(it) }
        val planArr = JSONArray()
        names.forEach { n ->
            planArr.put(
                JSONObject().put("category", n).put("label", Fmt.emoji(n) + " " + n)
                    .put("plan", rub(md.limits[n] ?: 0L)).put("fact", rub(md.spentByCategory[n] ?: 0L))
            )
        }
        root.put("plan", planArr)

        val daily = Stats.dailyExpenses(md.ops, ym)
        root.put("daily", JSONArray(daily.map { rub(it) }))

        val yearOps = Stats.opsInYear(operations.value, ym.year)
        val exp = Stats.monthlyTotals(yearOps, OpType.EXPENSE)
        val inc = Stats.monthlyTotals(yearOps, OpType.INCOME)
        val months = JSONArray()
        for (i in 0 until 12) {
            months.put(JSONObject().put("name", Fmt.shortMonth(i + 1)).put("income", rub(inc[i])).put("expense", rub(exp[i])))
        }
        val top = JSONArray()
        Stats.categoryStats(yearOps, OpType.EXPENSE).take(15).forEach {
            top.put(JSONObject().put("label", Fmt.emoji(it.name) + " " + it.name).put("amount", rub(it.sum)))
        }
        root.put("year", JSONObject().put("year", ym.year).put("months", months).put("top", top))

        val depRows = JSONArray()
        deposits.value.forEach { d ->
            val info = Deposits.info(d, interest.value)
            depRows.put(
                JSONObject().put("label", "🐷 " + d.name).put("kind", d.kind).put("balance", rub(info.balance))
                    .put("rate", Deposits.rateText(d)).put("monthly", rub(info.expectedMonthly))
                    .put("received", rub(info.received))
            )
        }
        root.put("deposits", depRows)

        val opsArr = JSONArray()
        md.ops.sortedBy { it.date }.forEach { o ->
            opsArr.put(
                JSONObject().put("date", LocalDate.ofEpochDay(o.date).toString()).put("type", o.type)
                    .put("category", o.category).put("comment", o.comment).put("person", o.person)
                    .put("account", if (o.type == OpType.TRANSFER) o.account + " → " + o.destAccount else o.account)
                    .put("amount", rub(o.amountCents))
            )
        }
        root.put("operations", opsArr)
        return root.toString()
    }

    fun clearMessage() { message = "" }

    private fun defaultCategories(): List<Category> {
        fun exp(name: String) = Category(name = name, isIncome = false, limitCents = 0)
        return listOf(
            exp("Аренда квартиры"), exp("Транспорт и бензин"), exp("Связь и интернет"),
            exp("Стрижка, маникюр"), exp("Продукты"), exp("Развлечения и досуг"),
            exp("Коммунальные платежи"), exp("Кафе и рестораны"), exp("Здоровье"),
            exp("Одежда и обувь"), exp("Дом и быт"), exp("Подписки"), exp("Подарки"),
            exp("Спорт"), exp("Кредит"), exp("Подушка безопасности"), exp("Прочие расходы"),
            Category(name = "Зарплата", isIncome = true),
            Category(name = "Аванс", isIncome = true),
            Category(name = "Подработка", isIncome = true),
            Category(name = "Прочие доходы", isIncome = true)
        )
    }
}
