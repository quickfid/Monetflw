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
import ru.familybudget.app.data.Mandatory
import ru.familybudget.app.data.OpType
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
    val budgetCents: Long,
    val payments: List<Mandatory>,
    val distributeLimits: Boolean
)

/** Примерные доли бюджета по категориям (в сумме 100%). */
private val expenseShares: Map<String, Double> = mapOf(
    "Аренда жилья" to 0.30, "Продукты" to 0.18, "ЖКХ" to 0.06, "Транспорт" to 0.06,
    "Кафе и доставка" to 0.05, "Связь и интернет" to 0.02, "Здоровье" to 0.04,
    "Одежда и обувь" to 0.05, "Дом и быт" to 0.04, "Красота и уход" to 0.03,
    "Подарки и близкие" to 0.03, "Спорт" to 0.02, "Досуг" to 0.05, "Отпуск" to 0.04,
    "Подушка безопасности" to 0.03
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.accountCount() == 0) {
                dao.insertAccount(Account(name = "Карта"))
                dao.insertAccount(Account(name = "Наличные"))
            }
            if (dao.categoryCount() == 0) dao.insertCategories(defaultCategories())
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
            if (op.id == 0L) dao.insertOperation(op) else dao.updateOperation(op)
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
            val name = d.name.trim()
            val people = listOf(name, d.partner.trim()).filter { it.isNotEmpty() }.distinct()
            store.update { st ->
                st.copy(
                    userName = name,
                    persons = if (people.isEmpty()) st.persons else people,
                    currency = d.currency.ifBlank { "₽" },
                    monthlyIncome = d.incomeCents,
                    monthlyBudget = d.budgetCents,
                    onboarded = true
                )
            }
            val accs = accounts.value
            // Если доход уже пришёл и записан, вычитаем его из стартового остатка,
            // чтобы текущий баланс остался таким, каким его ввёл человек.
            val recordIncome = d.recordIncome && d.incomeCents > 0
            val cardInitial = d.cardCents - (if (recordIncome) d.incomeCents else 0L)
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
                        person = name.ifEmpty { settings.value.persons.firstOrNull().orEmpty() },
                        account = accs.firstOrNull()?.name.orEmpty()
                    )
                )
            }
            val cats = categories.value
            d.payments.forEach { p ->
                dao.insertMandatory(p.copy(category = guessCategory(p.name, cats)))
            }
            if (d.distributeLimits && d.budgetCents > 0) applyLimits(d.budgetCents)
        }
    }

    fun skipOnboarding() { store.update { it.copy(onboarded = true) } }

    fun saveProfile(name: String, incomeCents: Long, budgetCents: Long) {
        store.update { st ->
            val n = name.trim()
            val people = if (n.isEmpty() || n in st.persons) st.persons else listOf(n) + st.persons
            st.copy(userName = n, persons = people, monthlyIncome = incomeCents, monthlyBudget = budgetCents)
        }
        message = "Профиль сохранён."
    }

    fun hideTips() { store.update { it.copy(hideTips = true) } }

    fun distributeLimits() {
        val budget = settings.value.monthlyBudget
        if (budget <= 0) {
            message = "Сначала укажите месячный бюджет: Ещё → Профиль и бюджет."
            return
        }
        viewModelScope.launch {
            applyLimits(budget)
            message = "Лимиты рассчитаны из бюджета. Их можно поправить вручную."
        }
    }

    private suspend fun applyLimits(budgetCents: Long) {
        categories.value.filter { !it.isIncome }.forEach { c ->
            val share = expenseShares[c.name]
            if (share != null) {
                val rubles = Math.round(budgetCents * share / 100.0)
                dao.updateCategory(c.copy(limitCents = rubles * 100L))
            }
        }
    }

    private fun guessCategory(name: String, cats: List<Category>): String {
        val n = name.lowercase()
        val wanted = when {
            n.contains("аренд") || n.contains("ипотек") -> "Аренда жилья"
            n.contains("жкх") || n.contains("коммун") -> "ЖКХ"
            n.contains("интернет") || n.contains("связ") || n.contains("мобил") -> "Связь и интернет"
            n.contains("спорт") || n.contains("фитнес") -> "Спорт"
            else -> ""
        }
        return if (wanted.isNotEmpty() && cats.any { it.name == wanted }) wanted else ""
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

    // ---------- синхронизация с Google-таблицей
    fun sync() {
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            message = SheetsSync.push(dao, settings.value)
            syncing = false
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

    fun clearMessage() { message = "" }

    private fun defaultCategories(): List<Category> {
        fun exp(name: String) = Category(name = name, isIncome = false, limitCents = 0)
        return listOf(
            exp("Аренда жилья"), exp("Продукты"), exp("ЖКХ"), exp("Транспорт"), exp("Кафе и доставка"),
            exp("Связь и интернет"), exp("Здоровье"), exp("Одежда и обувь"), exp("Дом и быт"),
            exp("Красота и уход"), exp("Подарки и близкие"), exp("Спорт"), exp("Досуг"), exp("Отпуск"),
            exp("Подушка безопасности"),
            Category(name = "Зарплата", isIncome = true),
            Category(name = "Аванс", isIncome = true),
            Category(name = "Подработка", isIncome = true),
            Category(name = "Прочие доходы", isIncome = true)
        )
    }
}
