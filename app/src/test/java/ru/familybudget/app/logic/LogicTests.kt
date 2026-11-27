package ru.familybudget.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.familybudget.app.data.Account
import ru.familybudget.app.data.Deposit
import ru.familybudget.app.data.InterestEntry
import ru.familybudget.app.data.OpType
import ru.familybudget.app.data.Operation
import java.time.LocalDate
import java.time.YearMonth

class LogicTests {

    @Test
    fun parseAndFormatCents() {
        assertEquals(1400000L, Fmt.parseCents("14000"))
        assertEquals(1400050L, Fmt.parseCents("14000.50"))
        assertEquals(1400050L, Fmt.parseCents("14000,5"))
        assertEquals(0L, Fmt.parseCents("0"))
        assertEquals(null, Fmt.parseCents("abc"))
    }

    @Test
    fun operationDeltaAndAccountBalance() {
        val acc = Account(id = 1, name = "Карта", initialCents = 100_000L)
        val cash = Account(id = 2, name = "Наличные", initialCents = 50_000L)
        val today = LocalDate.of(2026, 5, 10).toEpochDay()
        val ops = listOf(
            Operation(id = 1, date = today, amountCents = 200_000L, type = OpType.INCOME, category = "Зарплата", account = "Карта"),
            Operation(id = 2, date = today, amountCents = 40_000L, type = OpType.EXPENSE, category = "Продукты", account = "Карта"),
            Operation(id = 3, date = today, amountCents = 30_000L, type = OpType.TRANSFER, category = "Перевод", account = "Карта", destAccount = "Наличные")
        )
        assertEquals(200_000L, ops[0].delta())
        assertEquals(-40_000L, ops[1].delta())
        assertEquals(0L, ops[2].delta())
        assertEquals(230_000L, Stats.accountBalance(acc, ops))
        assertEquals(80_000L, Stats.accountBalance(cash, ops))
    }

    @Test
    fun depositInterestCalculation() {
        val dep = Deposit(id = 1, name = "Вклад", principalCents = 12_000_000L, baselineYm = "2026-01", ratePercent = 12.0, ratePeriod = 0, compound = false)
        val missing = Deposits.missing(dep, emptyList(), YearMonth.of(2026, 4))
        assertEquals(2, missing.size) // 2026-02 and 2026-03
        assertEquals(120_000L, missing[0].amountCents)
        val info = Deposits.info(dep, missing)
        assertEquals(12_240_000L, info.balance)
        assertEquals(240_000L, info.received)
        assertEquals(120_000L, info.expectedMonthly)
    }

    @Test
    fun gameXpDeduplicatesMandatoryPayments() {
        val today = LocalDate.of(2026, 5, 10)
        val day = today.toEpochDay()
        val ops = listOf(
            Operation(id = 1, date = day, amountCents = 10_000L, type = OpType.EXPENSE, category = "Связь", mandatoryId = 1L),
            Operation(id = 2, date = day, amountCents = 10_000L, type = OpType.EXPENSE, category = "Связь", mandatoryId = 1L)
        )
        val state = Game.compute(ops, emptyList(), emptyList(), today)
        // 2 ops * 10 + 1 distinct mandatory * 15 + 1 streak * 5 + 1 unlocked bronze badge ("Первый шаг", +15 XP) = 55 XP
        assertEquals(55, state.xp)
        assertTrue(state.streak == 1)
        assertEquals(21, state.badges.size)
    }
}
