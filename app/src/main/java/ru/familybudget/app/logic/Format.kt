package ru.familybudget.app.logic

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Fmt {
    private val ru = Locale("ru", "RU")
    private val dateFormat = DateTimeFormatter.ofPattern("d MMMM, EEE", ru)

    fun money(cents: Long, currency: String, decimals: Boolean = true): String {
        val nf = NumberFormat.getNumberInstance(ru)
        nf.minimumFractionDigits = if (decimals) 2 else 0
        nf.maximumFractionDigits = if (decimals) 2 else 0
        return nf.format(cents / 100.0) + " " + currency
    }

    /** Для полей ввода и CSV: «1234,50». */
    fun plain(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString().replace('.', ',')

    fun parseCents(text: String): Long? = try {
        val t = text.trim().replace(" ", "").replace(',', '.')
        if (t.isEmpty()) null
        else BigDecimal(t).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    } catch (e: Exception) {
        null
    }

    fun monthTitle(ym: YearMonth): String =
        ym.month.getDisplayName(TextStyle.FULL_STANDALONE, ru).replaceFirstChar { it.uppercase() } + " " + ym.year

    fun shortMonth(month: Int): String =
        java.time.Month.of(month).getDisplayName(TextStyle.SHORT_STANDALONE, ru)
            .replace(".", "").replaceFirstChar { it.uppercase() }

    fun fullMonth(month: Int): String =
        java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, ru).replaceFirstChar { it.uppercase() }

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormat)

    fun percent(share: Float): String = Math.round(share * 100f).toString() + "%"

    fun emoji(category: String): String {
        val n = category.lowercase()
        return when {
            n.contains("аренд") || n.contains("ипотек") -> "🏠"
            n.contains("кредит") || n.contains("долг") -> "💳"
            n.contains("транспорт") || n.contains("бензин") || n.contains("авто") -> "⛽"
            n.contains("связь") || n.contains("интернет") || n.contains("мобил") -> "📱"
            n.contains("коммун") || n.contains("жкх") -> "💡"
            n.contains("продукт") -> "🛒"
            n.contains("кафе") -> "🍔"
            n.contains("здоров") -> "💊"
            n.contains("одежд") -> "👕"
            n.contains("красот") -> "💅"
            n.contains("подар") -> "🎁"
            n.contains("спорт") -> "🏋️"
            n.contains("досуг") -> "🎮"
            n.contains("отпуск") -> "🏖️"
            n.contains("подуш") || n.contains("накоп") -> "🐷"
            n.contains("дом") -> "🛋️"
            n.contains("проч") -> "📦"
            n.contains("зарплат") || n.contains("аванс") -> "💼"
            else -> "🏷️"
        }
    }

    /** Порядок в плане: аренда, кредит, транспорт, связь, коммунальные, продукты, остальное, прочие. */
    fun planRank(category: String): Int {
        val n = category.lowercase()
        return when {
            n.contains("аренд") || n.contains("ипотек") -> 0
            n.contains("кредит") -> 1
            n.contains("транспорт") || n.contains("бензин") -> 2
            n.contains("связь") || n.contains("интернет") -> 3
            n.contains("коммун") || n.contains("жкх") -> 4
            n.contains("продукт") -> 5
            n.contains("проч") -> 99
            else -> 50
        }
    }

    /** Ежемесячные платежи с датой: для них в плане заводится галочка «оплачено». */
    fun isBill(category: String): Boolean {
        val n = category.lowercase()
        return n.contains("аренд") || n.contains("ипотек") || n.contains("кредит") ||
            n.contains("связь") || n.contains("интернет") || n.contains("коммун") || n.contains("жкх")
    }

    fun billDay(category: String): Int {
        val n = category.lowercase()
        return when {
            n.contains("аренд") || n.contains("ипотек") -> 5
            n.contains("кредит") -> 10
            n.contains("коммун") || n.contains("жкх") -> 15
            else -> 20
        }
    }
}
