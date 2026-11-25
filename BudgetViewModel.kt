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
        store.update { s -> s.copy(persons = if (list.isEmpty()) s.persons else list, currency = currency.ifBlank { "₽" }) }
        return "Восстановлено операций: $restored."
    }

    fun clearMessage() { message = "" }

    private fun defaultCategories(): List<Category> {
        fun exp(name: String, limit: Long) = Category(name = name, isIncome = false, limitCents = limit * 100)
        return listOf(
            exp("Аренда жилья", 30000), exp("Продукты", 20000), exp("ЖКХ", 6000),
            exp("Транспорт", 4000), exp("Кафе и доставка", 4500), exp("Связь и интернет", 1500),
            exp("Здоровье", 3000), exp("Одежда и обувь", 4000), exp("Дом и быт", 3000),
            exp("Красота и уход", 2000), exp("Подарки и близкие", 3000), exp("Спорт", 2000),
            exp("Досуг", 3000), exp("Отпуск", 5000), exp("Подушка безопасности", 10000),
            Category(name = "Зарплата", isIncome = true),
            Category(name = "Аванс", isIncome = true),
            Category(name = "Подработка", isIncome = true),
            Category(name = "Прочие доходы", isIncome = true)
        )
    }
}
