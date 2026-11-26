package ru.familybudget.app.logic

import ru.familybudget.app.data.Deposit
import ru.familybudget.app.data.InterestEntry
import java.time.YearMonth

data class DepositInfo(
    val deposit: Deposit,
    /** Сумма сейчас: базовая сумма плюс все начисленные проценты. */
    val balance: Long,
    /** Сколько ожидается в этом месяце (придёт, когда месяц закончится). */
    val expectedMonthly: Long,
    /** Сколько процентов уже пришло за всё время. */
    val received: Long,
    val entries: List<InterestEntry>
)

object Deposits {
    /** Ставка за один месяц в долях (0,005 = 0,5%). Годовая ставка делится на 12. */
    fun monthlyRate(d: Deposit): Double =
        if (d.ratePeriod == 1) d.ratePercent / 100.0 else d.ratePercent / 100.0 / 12.0

    /**
     * Проценты за все завершённые месяцы после базового, которых ещё нет в списке.
     * Уже существующие (в том числе исправленные вручную) не трогаются, но учитываются в расчёте следующих.
     */
    fun missing(d: Deposit, existing: List<InterestEntry>, now: YearMonth): List<InterestEntry> {
        val byYm = existing.associateBy { it.ym }
        val out = ArrayList<InterestEntry>()
        var month = YearMonth.parse(d.baselineYm).plusMonths(1)
        var sum = 0L
        while (month.isBefore(now)) {
            val found = byYm[month.toString()]
            if (found != null) {
                sum += found.amountCents
            } else {
                val base = if (d.compound) d.principalCents + sum else d.principalCents
                val amount = Math.round(base * monthlyRate(d))
                out.add(InterestEntry(depositId = d.id, ym = month.toString(), amountCents = amount))
                sum += amount
            }
            month = month.plusMonths(1)
        }
        return out
    }

    fun info(d: Deposit, allEntries: List<InterestEntry>): DepositInfo {
        val mine = allEntries.filter { it.depositId == d.id }.sortedBy { it.ym }
        val received = mine.sumOf { it.amountCents }
        val balance = d.principalCents + received
        val base = if (d.compound) balance else d.principalCents
        return DepositInfo(d, balance, Math.round(base * monthlyRate(d)), received, mine)
    }

    fun rateText(d: Deposit): String {
        val v = if (d.ratePercent % 1.0 == 0.0) d.ratePercent.toLong().toString()
        else d.ratePercent.toString().replace('.', ',')
        return v + if (d.ratePeriod == 1) "% в месяц" else "% годовых"
    }
}
