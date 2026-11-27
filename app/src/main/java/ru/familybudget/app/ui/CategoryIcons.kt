package ru.familybudget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CategoryIconSpec(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val lightBg: Color,
    val lightFg: Color,
    val darkBg: Color,
    val darkFg: Color
)

/** Набор тонких векторных иконок категорий в мягких пастельных кружках (стиль современного финтеха). */
object CategoryIcons {
    private val s = SolidColor(Color.Black)

    private fun vec(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    val all: List<CategoryIconSpec> by lazy {
        listOf(
            CategoryIconSpec(
                "home", "Жильё",
                vec("CatHome") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(3.5f, 10.5f); lineTo(12f, 4f); lineTo(20.5f, 10.5f)
                        moveTo(6f, 9.5f); verticalLineTo(19.5f); horizontalLineTo(18f); verticalLineTo(9.5f)
                        moveTo(10f, 19.5f); verticalLineTo(14.5f); horizontalLineTo(14f); verticalLineTo(19.5f)
                    }
                },
                Color(0xFFE3F2F5), Color(0xFF1B6B7A), Color(0xFF173136), Color(0xFF67D5E8)
            ),
            CategoryIconSpec(
                "marketplace", "Маркетплейсы",
                vec("CatMarketplace") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 8f); lineTo(12f, 4f); lineTo(20f, 8f); verticalLineTo(16.5f); lineTo(12f, 20.5f); lineTo(4f, 16.5f); close()
                        moveTo(4f, 8f); lineTo(12f, 12f); lineTo(20f, 8f)
                        moveTo(12f, 12f); verticalLineTo(20.5f)
                    }
                },
                Color(0xFFEDE7F6), Color(0xFF6A1B9A), Color(0xFF2B1C3D), Color(0xFFCE93D8)
            ),
            CategoryIconSpec(
                "installment", "Сплит и рассрочки",
                vec("CatInstallment") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(3.5f, 7f); horizontalLineTo(20.5f); verticalLineTo(17.5f); horizontalLineTo(3.5f); close()
                        moveTo(3.5f, 11f); horizontalLineTo(20.5f)
                        moveTo(12f, 11f); verticalLineTo(17.5f)
                        moveTo(7f, 14.5f); horizontalLineTo(9.5f)
                        moveTo(14.5f, 14.5f); horizontalLineTo(17f)
                    }
                },
                Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFF3A2412), Color(0xFFFFB74D)
            ),
            CategoryIconSpec(
                "car", "Транспорт",
                vec("CatCar") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 14f); lineTo(6f, 8.5f); horizontalLineTo(18f); lineTo(20f, 14f)
                        moveTo(3.5f, 14f); horizontalLineTo(20.5f); verticalLineTo(18f); horizontalLineTo(3.5f); close()
                        moveTo(7f, 18f); verticalLineTo(20f); moveTo(17f, 18f); verticalLineTo(20f)
                    }
                },
                Color(0xFFE5EEFA), Color(0xFF2B5EA8), Color(0xFF1B2B42), Color(0xFF7FB3FF)
            ),
            CategoryIconSpec(
                "phone", "Связь",
                vec("CatPhone") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(7.5f, 3.5f); horizontalLineTo(16.5f); verticalLineTo(20.5f); horizontalLineTo(7.5f); close()
                        moveTo(11f, 17.5f); horizontalLineTo(13f)
                    }
                },
                Color(0xFFECE8FA), Color(0xFF6348B8), Color(0xFF282040), Color(0xFFB59CFF)
            ),
            CategoryIconSpec(
                "scissors", "Красота",
                vec("CatScissors") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(6f, 6f); lineTo(18f, 18f)
                        moveTo(18f, 6f); lineTo(6f, 18f)
                        moveTo(5.5f, 16.5f); horizontalLineTo(7.5f)
                        moveTo(5.5f, 7.5f); horizontalLineTo(7.5f)
                    }
                },
                Color(0xFFFBE9F2), Color(0xFFB83E7C), Color(0xFF3C1C2E), Color(0xFFFF8EC6)
            ),
            CategoryIconSpec(
                "cart", "Продукты",
                vec("CatCart") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(3f, 5f); horizontalLineTo(6f); lineTo(8.5f, 15.5f); horizontalLineTo(18.5f); lineTo(20.5f, 8.5f); horizontalLineTo(7f)
                        moveTo(9.5f, 19.5f); horizontalLineTo(10f); moveTo(17f, 19.5f); horizontalLineTo(17.5f)
                    }
                },
                Color(0xFFE5F6ED), Color(0xFF168A53), Color(0xFF153325), Color(0xFF4ADE80)
            ),
            CategoryIconSpec(
                "film", "Досуг",
                vec("CatFilm") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 6f); horizontalLineTo(20f); verticalLineTo(18f); horizontalLineTo(4f); close()
                        moveTo(10f, 9.5f); lineTo(15f, 12f); lineTo(10f, 14.5f); close()
                    }
                },
                Color(0xFFFFF3E0), Color(0xFFC26D10), Color(0xFF3B2912), Color(0xFFFBBF24)
            ),
            CategoryIconSpec(
                "bolt", "Коммуналка",
                vec("CatBolt") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(13f, 3.5f); lineTo(6.5f, 13f); horizontalLineTo(12f); lineTo(11f, 20.5f); lineTo(17.5f, 11f); horizontalLineTo(12f); close()
                    }
                },
                Color(0xFFFEF6DC), Color(0xFFB3860E), Color(0xFF382E12), Color(0xFFFDE047)
            ),
            CategoryIconSpec(
                "coffee", "Кафе",
                vec("CatCoffee") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4.5f, 8.5f); horizontalLineTo(16.5f); verticalLineTo(15.5f)
                        curveTo(16.5f, 18f, 14.5f, 19.5f, 10.5f, 19.5f)
                        curveTo(6.5f, 19.5f, 4.5f, 18f, 4.5f, 15.5f); close()
                        moveTo(16.5f, 10f); horizontalLineTo(19f); verticalLineTo(14f); horizontalLineTo(16.5f)
                    }
                },
                Color(0xFFF5ECE6), Color(0xFF8C5332), Color(0xFF332319), Color(0xFFE0A47E)
            ),
            CategoryIconSpec(
                "heart", "Здоровье",
                vec("CatHeart") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 20f); lineTo(4.5f, 12.5f)
                        curveTo(3f, 10.8f, 3.2f, 7.8f, 5.5f, 6.5f)
                        curveTo(7.5f, 5.4f, 10f, 6.2f, 12f, 8.5f)
                        curveTo(14f, 6.2f, 16.5f, 5.4f, 18.5f, 6.5f)
                        curveTo(20.8f, 7.8f, 21f, 10.8f, 19.5f, 12.5f); close()
                    }
                },
                Color(0xFFFDEAEA), Color(0xFFCC4444), Color(0xFF3B1B1B), Color(0xFFF87171)
            ),
            CategoryIconSpec(
                "bag", "Одежда",
                vec("CatBag") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(5f, 8f); horizontalLineTo(19f); lineTo(18f, 20f); horizontalLineTo(6f); close()
                        moveTo(9f, 8f); verticalLineTo(6f); curveTo(9f, 4.3f, 15f, 4.3f, 15f, 6f); verticalLineTo(8f)
                    }
                },
                Color(0xFFE8F4F8), Color(0xFF287496), Color(0xFF182E38), Color(0xFF7DD3FC)
            ),
            CategoryIconSpec(
                "gift", "Подарки",
                vec("CatGift") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 9f); horizontalLineTo(20f); verticalLineTo(13f); horizontalLineTo(4f); close()
                        moveTo(5.5f, 13f); verticalLineTo(20f); horizontalLineTo(18.5f); verticalLineTo(13f)
                        moveTo(12f, 9f); verticalLineTo(20f)
                    }
                },
                Color(0xFFFCEBF0), Color(0xFFC2416C), Color(0xFF3A1D29), Color(0xFFF472B6)
            ),
            CategoryIconSpec(
                "shield", "Подушка",
                vec("CatShield") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 3.5f); lineTo(19f, 6.5f); verticalLineTo(12f)
                        curveTo(19f, 16.5f, 15.8f, 19.5f, 12f, 21f)
                        curveTo(8.2f, 19.5f, 5f, 16.5f, 5f, 12f); verticalLineTo(6.5f); close()
                    }
                },
                Color(0xFFFEF5DF), Color(0xFFB88014), Color(0xFF382C13), Color(0xFFF59E0B)
            ),
            CategoryIconSpec(
                "wallet", "Доход",
                vec("CatWallet") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(3.5f, 6.5f); horizontalLineTo(20.5f); verticalLineTo(18.5f); horizontalLineTo(3.5f); close()
                        moveTo(15f, 12.5f); horizontalLineTo(18.5f)
                    }
                },
                Color(0xFFE4F6EC), Color(0xFF168A53), Color(0xFF143225), Color(0xFF34D399)
            ),
            CategoryIconSpec(
                "tag", "Прочее",
                vec("CatTag") {
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 4f); horizontalLineTo(11f); lineTo(20f, 13f); lineTo(13f, 20f); lineTo(4f, 11f); close()
                        moveTo(8f, 8f); horizontalLineTo(8.5f)
                    }
                },
                Color(0xFFEDF1F5), Color(0xFF526273), Color(0xFF212B30), Color(0xFF94A3B8)
            )
        )
    }

    fun byKey(key: String): CategoryIconSpec = all.firstOrNull { it.key == key } ?: all.last()

    /** Автоматический подбор иконки по названию категории. */
    fun autoKey(name: String): String {
        val n = name.lowercase()
        return when {
            "маркетплейс" in n || "ozon" in n || "озон" in n || "wb" in n || "wildberries" in n || "яндекс маркет" in n -> "marketplace"
            "сплит" in n || "рассрочк" in n || "долям" in n -> "installment"
            "аренд" in n || "жиль" in n || "кварт" in n || "дом" in n || "быт" in n -> "home"
            "транспорт" in n || "бенз" in n || "такси" in n || "авто" in n || "метро" in n -> "car"
            "связ" in n || "интернет" in n || "телеф" in n || "подписк" in n -> "phone"
            "стриж" in n || "маникюр" in n || "красот" in n || "салон" in n -> "scissors"
            "продукт" in n || "магаз" in n || "еда" in n -> "cart"
            "развлеч" in n || "досуг" in n || "кино" in n || "отдых" in n || "спорт" in n -> "film"
            "коммун" in n || "жкх" in n || "свет" in n || "вода" in n -> "bolt"
            "кафе" in n || "кофе" in n || "рестор" in n || "обед" in n -> "coffee"
            "здоров" in n || "аптек" in n || "врач" in n || "лекар" in n -> "heart"
            "одежд" in n || "обув" in n || "шопинг" in n -> "bag"
            "подар" in n || "цвет" in n -> "gift"
            "подуш" in n || "вклад" in n || "накоп" in n || "кредит" in n -> "shield"
            "зарплат" in n || "аванс" in n || "доход" in n || "подработ" in n -> "wallet"
            else -> "tag"
        }
    }

    fun resolve(categoryName: String, overrides: Map<String, String>): CategoryIconSpec {
        val key = overrides[categoryName]?.takeIf { it.isNotBlank() } ?: autoKey(categoryName)
        return byKey(key)
    }
}

@Composable
fun CategoryBadge(
    categoryName: String,
    overrides: Map<String, String> = emptyMap(),
    size: Dp = 36.dp
) {
    val spec = CategoryIcons.resolve(categoryName, overrides)
    val dark = isSystemInDarkTheme()
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(if (dark) spec.darkBg else spec.lightBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = categoryName,
            tint = if (dark) spec.darkFg else spec.lightFg,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}
