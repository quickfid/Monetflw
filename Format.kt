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
}
