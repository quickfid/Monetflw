package ru.familybudget.app.logic

import java.math.BigDecimal
import java.math.RoundingMode

data class ParsedBankPayment(
    val amountCents: Long,
    val merchant: String,
    val category: String
)

/**
 * Локальный парсер Push-уведомлений банков.
 * Учитывает ТОЛЬКО оплаты и покупки; переводы, СБП, пополнения, возвраты и кэшбэк строго игнорируются.
 */
object BankPushParser {
    private val bankPackages = setOf(
        "ru.sberbankmobile",
        "com.idamob.tinkoff.android",
        "ru.alfabank.mobile.android",
        "ru.vtb24.mobilebanking.android",
        "ru.ozon.fintech.finance",
        "ru.ozon.app.android",
        "com.yandex.bank",
        "com.yandex.pay",
        "ru.gazprombank.android.mobilebank.app",
        "ru.raiffeisennews",
        "ru.sovcomcard.halva.v1"
    )

    private val ignoredKeywords = listOf(
        "перевод", "пополн", "зачисл", "пришл", "получен",
        "возврат", "отмен", "кэшбэк", "кешбэк", "cashback",
        "процент", "снятие", "наличн", "банкомат",
        "код", "пароль", "подтверд", "вход в", "отказ", "недостаточно"
    )

    private val paymentKeywords = listOf(
        "покупк", "оплат", "списан", "потрачен", "платёж", "платеж", "чек", "вы оплатили", "сбп", "qr"
    )

    private val amountRegex = Regex("""(\d[\d\s\u00A0]{0,9}(?:[.,]\d{1,2})?)\s*(?:₽|руб\.?|р\.|rub)""", RegexOption.IGNORE_CASE)

    fun isSupportedBank(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg in bankPackages || bankPackages.any { pkg.startsWith("$it.") }
    }

    fun parsePayment(title: String, text: String): ParsedBankPayment? {
        val full = "$title $text".trim()
        if (full.isBlank()) return null
        val lower = full.lowercase()

        // Строго игнорируем переводы, СБП, пополнения, возвраты и коды подтверждения
        if (ignoredKeywords.any { it in lower }) return null

        // Должен быть явный признак оплаты/покупки или знак минуса перед суммой
        val hasPaymentWord = paymentKeywords.any { it in lower }
        val hasMinusAmount = Regex("""[-−]\s*\d""").containsMatchIn(full)
        if (!hasPaymentWord && !hasMinusAmount) return null

        val match = amountRegex.find(full) ?: return null
        val rawNum = match.groupValues[1]
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace(',', '.')
        val cents = try {
            BigDecimal(rawNum).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            return null
        }
        if (cents <= 0L) return null

        val merchant = extractMerchant(title, text)
        val category = detectCategory("$merchant $lower")
        return ParsedBankPayment(cents, merchant, category)
    }

    private fun extractMerchant(title: String, text: String): String {
        val clean = text
            .replace(amountRegex, "")
            .replace(Regex("""(?i)(покупка|оплата|списание|баланс|доступно|остаток|карта|\*\d{4}).*"""), "")
            .trim(' ', '·', '.', ',', ':', ';', '-', '—')
        return when {
            clean.length in 2..40 -> clean
            title.length in 2..40 -> title.trim()
            else -> "Оплата картой"
        }
    }

    private fun detectCategory(source: String): String {
        val s = source.lowercase()
        return when {
            listOf("пятёрочк", "пятерочк", "магнит", "вкусвилл", "перекрёсток", "перекресток", "лента", "дикси", "ашан", "метро кэш", "верный", "красное&белое", "самокат", "яндекс лавка", "продукт", "супермаркет").any { it in s } ->
                "Продукты"
            listOf("ozon", "озон", "wildberries", "wb", "вайлдберриз", "яндекс маркет", "мегамаркет", "lamoda", "aliexpress").any { it in s } ->
                "Маркетплейсы"
            listOf("сплит", "долями", "подели", "рассрочк", "плати частями").any { it in s } ->
                "Сплит и рассрочки"
            listOf("яндекс go", "yandex go", "такси", "uber", "метро", "ржд", "азс", "лукойл", "газпромнефть", "роснефть", "татнефть", "парковк", "транспорт", "каршеринг").any { it in s } ->
                "Транспорт"
            listOf("мтс", "билайн", "мегафон", "tele2", "теле2", "ростелеком", "дом.ру", "интернет", "связь").any { it in s } ->
                "Связь и интернет"
            listOf("аптек", "горздрав", "ригла", "инвитро", "гемотест", "клиник", "стоматолог").any { it in s } ->
                "Здоровье"
            listOf("вкусно и точка", "kfc", "rostics", "бургер", "додо", "кофе", "кофейн", "кафе", "ресторан", "шоколадниц", "яндекс еда").any { it in s } ->
                "Развлечения"
            else -> "Прочие расходы"
        }
    }
}
