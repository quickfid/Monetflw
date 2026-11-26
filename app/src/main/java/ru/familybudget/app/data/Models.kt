package ru.familybudget.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Типы операций совпадают с теми, что использует Google-таблица. */
object OpType {
    const val EXPENSE = "Расход"
    const val INCOME = "Доход"
    const val TRANSFER = "Перевод"
    val all = listOf(EXPENSE, INCOME, TRANSFER)
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isIncome: Boolean = false,
    /** Месячный лимит в копейках (0 = без лимита). Только для расходов. */
    val limitCents: Long = 0
)

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialCents: Long = 0
)

/** Обязательный ежемесячный платёж (аренда, связь, интернет...). */
@Entity(tableName = "mandatory")
data class Mandatory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val day: Int = 1,
    val amountCents: Long,
    val category: String = ""
)

/** План расходов на конкретный месяц: сколько запланировано на категорию. ym в формате «2026-11». */
@Entity(tableName = "plans", indices = [Index(value = ["ym", "category"], unique = true)])
data class PlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ym: String,
    val category: String,
    val amountCents: Long
)

/** Вклад или накопительный счёт. principalCents — сумма на момент baselineYm (месяц «ГГГГ-ММ»). */
@Entity(tableName = "deposits")
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String = "Вклад",
    val principalCents: Long,
    val baselineYm: String,
    val ratePercent: Double,
    /** 0 — ставка годовая, 1 — ставка в месяц. */
    val ratePeriod: Int = 0,
    /** true — проценты прибавляются к сумме и сами приносят проценты. */
    val compound: Boolean = true
)

/** Проценты, пришедшие по вкладу за месяц. manual = true, если сумму поправил человек. */
@Entity(tableName = "interest_entries", indices = [Index(value = ["depositId", "ym"], unique = true)])
data class InterestEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val depositId: Long,
    val ym: String,
    val amountCents: Long,
    val manual: Boolean = false
)

@Entity(
    tableName = "operations",
    indices = [Index("date"), Index(value = ["uid"], unique = true)]
)
data class Operation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String = UUID.randomUUID().toString(),
    /** День в формате LocalDate.toEpochDay(). */
    val date: Long,
    val amountCents: Long,
    val type: String,
    val category: String,
    val comment: String = "",
    val person: String = "",
    /** Счёт операции; для перевода — счёт-отправитель. */
    val account: String = "",
    /** Только для перевода — счёт-получатель. */
    val destAccount: String = "",
    /** Если операция создана галочкой «обязательный платёж». */
    val mandatoryId: Long? = null,
    val synced: Boolean = false
)

@Dao
interface BudgetDao {
    // --- операции
    @Query("SELECT * FROM operations ORDER BY date DESC, id DESC")
    fun operations(): Flow<List<Operation>>

    @Insert
    suspend fun insertOperation(op: Operation): Long

    @Update
    suspend fun updateOperation(op: Operation)

    @Delete
    suspend fun deleteOperation(op: Operation)

    @Query("SELECT * FROM operations WHERE synced = 0 ORDER BY date, id")
    suspend fun unsynced(): List<Operation>

    @Query("UPDATE operations SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("UPDATE operations SET category = :newName WHERE category = :oldName")
    suspend fun renameCategoryInOperations(oldName: String, newName: String)

    @Query("UPDATE mandatory SET category = :newName WHERE category = :oldName")
    suspend fun renameCategoryInMandatory(oldName: String, newName: String)

    @Query("UPDATE operations SET account = :newName WHERE account = :oldName")
    suspend fun renameAccountInOperations(oldName: String, newName: String)

    @Query("UPDATE operations SET destAccount = :newName WHERE destAccount = :oldName")
    suspend fun renameDestAccountInOperations(oldName: String, newName: String)

    @Query("DELETE FROM operations")
    suspend fun clearOperations()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM mandatory")
    suspend fun clearMandatory()

    // --- планы по месяцам
    @Query("SELECT * FROM plans")
    fun plans(): Flow<List<PlanItem>>

    @Query("DELETE FROM plans WHERE ym = :ym")
    suspend fun clearPlan(ym: String)

    @Query("DELETE FROM plans")
    suspend fun clearAllPlans()

    @Insert
    suspend fun insertPlans(list: List<PlanItem>)

    @Query("UPDATE plans SET category = :newName WHERE category = :oldName")
    suspend fun renameCategoryInPlans(oldName: String, newName: String)

    @Query("DELETE FROM plans WHERE ym = :ym AND category = :category")
    suspend fun deletePlanItem(ym: String, category: String)

    // --- вклады и проценты
    @Query("SELECT * FROM deposits ORDER BY id")
    fun deposits(): Flow<List<Deposit>>

    @Query("SELECT * FROM deposits ORDER BY id")
    suspend fun depositsOnce(): List<Deposit>

    @Insert
    suspend fun insertDeposit(d: Deposit): Long

    @Update
    suspend fun updateDeposit(d: Deposit)

    @Delete
    suspend fun deleteDeposit(d: Deposit)

    @Query("DELETE FROM deposits")
    suspend fun clearDeposits()

    @Query("SELECT * FROM interest_entries ORDER BY ym")
    fun interest(): Flow<List<InterestEntry>>

    @Query("SELECT * FROM interest_entries ORDER BY ym")
    suspend fun interestOnce(): List<InterestEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInterest(list: List<InterestEntry>)

    @Update
    suspend fun updateInterest(e: InterestEntry)

    @Query("DELETE FROM interest_entries WHERE depositId = :depositId")
    suspend fun clearInterest(depositId: Long)

    @Query("DELETE FROM interest_entries WHERE depositId = :depositId AND manual = 0")
    suspend fun clearAutoInterest(depositId: Long)

    @Query("DELETE FROM interest_entries")
    suspend fun clearAllInterest()

    // --- категории
    @Query("SELECT * FROM categories ORDER BY id")
    fun categories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun categoriesOnce(): List<Category>

    @Query("SELECT * FROM mandatory ORDER BY id")
    suspend fun mandatoryOnce(): List<Mandatory>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Insert
    suspend fun insertCategory(c: Category): Long

    @Insert
    suspend fun insertCategories(list: List<Category>)

    @Update
    suspend fun updateCategory(c: Category)

    @Delete
    suspend fun deleteCategory(c: Category)

    // --- счета
    @Query("SELECT * FROM accounts ORDER BY id")
    fun accounts(): Flow<List<Account>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun accountCount(): Int

    @Insert
    suspend fun insertAccount(a: Account): Long

    @Update
    suspend fun updateAccount(a: Account)

    @Delete
    suspend fun deleteAccount(a: Account)

    // --- обязательные платежи
    @Query("SELECT * FROM mandatory ORDER BY day, id")
    fun mandatory(): Flow<List<Mandatory>>

    @Insert
    suspend fun insertMandatory(m: Mandatory): Long

    @Update
    suspend fun updateMandatory(m: Mandatory)

    @Delete
    suspend fun deleteMandatory(m: Mandatory)
}

@Database(
    entities = [
        Category::class, Account::class, Mandatory::class, Operation::class, PlanItem::class,
        Deposit::class, InterestEntry::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BudgetDb : RoomDatabase() {
    abstract fun dao(): BudgetDao

    companion object {
        @Volatile
        private var instance: BudgetDb? = null

        fun get(context: Context): BudgetDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, BudgetDb::class.java, "budget.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}

/** Версия 1 → 2: добавлена таблица планов по месяцам. Данные пользователя не затрагиваются. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`ym` TEXT NOT NULL, `category` TEXT NOT NULL, `amountCents` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_plans_ym_category` ON `plans` (`ym`, `category`)")
    }
}

/** Версия 2 → 3: вклады и начисленные проценты. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `deposits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `principalCents` INTEGER NOT NULL, " +
                "`baselineYm` TEXT NOT NULL, `ratePercent` REAL NOT NULL, `ratePeriod` INTEGER NOT NULL, " +
                "`compound` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `interest_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`depositId` INTEGER NOT NULL, `ym` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`manual` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_interest_entries_depositId_ym` " +
                "ON `interest_entries` (`depositId`, `ym`)"
        )
    }
}
