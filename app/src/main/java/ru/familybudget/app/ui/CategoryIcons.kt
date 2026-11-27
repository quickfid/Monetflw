package ru.familybudget.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Авторские векторные иконки категорий в стиле «Фарфоровое Оригами со световым ребром»:
 * мягкая минерально-пастельная палитра (шалфей, пыльная бирюза, тёплый песок, терракот, лаванда),
 * гранёные складки оригами и жемчужный блик вдоль ребра.
 */
object CategoryIcons {
    private val s = SolidColor(Color.Black)

    private fun vec(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    val all: List<CategoryIconSpec> by lazy {
        listOf(
            CategoryIconSpec(
                "food", "Продукты",
                vec("OrigamiFood") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(4f, 10f); lineTo(20f, 10f); lineTo(18f, 19.5f); lineTo(6f, 19.5f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(12f, 10f); lineTo(20f, 10f); lineTo(18f, 19.5f); lineTo(12f, 19.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 10f); lineTo(20f, 10f); lineTo(18f, 19.5f); lineTo(6f, 19.5f); close()
                        moveTo(8f, 10f); lineTo(12f, 4.5f); lineTo(16f, 10f)
                    }
                    path(stroke = s, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                        moveTo(12f, 10.5f); lineTo(12f, 19f)
                    }
                },
                Color(0xFFE4F0EC), Color(0xFF2D6A5E), Color(0xFF1A2D2A), Color(0xFF8CCFB8)
            ),
            CategoryIconSpec(
                "home", "Жильё",
                vec("OrigamiHomeCat") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 3.5f); lineTo(3.5f, 10.5f); lineTo(5.5f, 10.5f); lineTo(5.5f, 19.5f); lineTo(12f, 19.5f); close()
                    }
                    path(fill = s, fillAlpha = 0.13f) {
                        moveTo(12f, 3.5f); lineTo(20.5f, 10.5f); lineTo(18.5f, 10.5f); lineTo(18.5f, 19.5f); lineTo(12f, 19.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(3.5f, 10.5f); lineTo(12f, 3.5f); lineTo(20.5f, 10.5f)
                        moveTo(5.5f, 9.2f); lineTo(5.5f, 19.5f); lineTo(18.5f, 19.5f); lineTo(18.5f, 9.2f)
                    }
                    path(stroke = s, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                        moveTo(12f, 4f); lineTo(12f, 19f)
                    }
                },
                Color(0xFFECE9F5), Color(0xFF675896), Color(0xFF252036), Color(0xFFC0B2E8)
            ),
            CategoryIconSpec(
                "car", "Транспорт",
                vec("OrigamiCar") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(4f, 12f); lineTo(7.5f, 6.5f); lineTo(16.5f, 6.5f); lineTo(20f, 12f); lineTo(20f, 17f); lineTo(4f, 17f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(12f, 6.5f); lineTo(16.5f, 6.5f); lineTo(20f, 12f); lineTo(20f, 17f); lineTo(12f, 17f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 12f); lineTo(7.5f, 6.5f); lineTo(16.5f, 6.5f); lineTo(20f, 12f); lineTo(20f, 17f); lineTo(4f, 17f); close()
                        moveTo(4.5f, 12f); lineTo(19.5f, 12f)
                        moveTo(7f, 17f); lineTo(7f, 19.2f)
                        moveTo(17f, 17f); lineTo(17f, 19.2f)
                    }
                },
                Color(0xFFE5EFF5), Color(0xFF396682), Color(0xFF1A2934), Color(0xFF8EC2DE)
            ),
            CategoryIconSpec(
                "fun", "Развлечения",
                vec("OrigamiFun") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 3.5f); lineTo(14.8f, 9.2f); lineTo(21f, 10f); lineTo(16.5f, 14.3f)
                        lineTo(17.6f, 20.5f); lineTo(12f, 17.4f); lineTo(6.4f, 20.5f); lineTo(7.5f, 14.3f)
                        lineTo(3f, 10f); lineTo(9.2f, 9.2f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 3.5f); lineTo(14.8f, 9.2f); lineTo(21f, 10f); lineTo(16.5f, 14.3f)
                        lineTo(17.6f, 20.5f); lineTo(12f, 17.4f); lineTo(6.4f, 20.5f); lineTo(7.5f, 14.3f)
                        lineTo(3f, 10f); lineTo(9.2f, 9.2f); close()
                    }
                    path(stroke = s, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                        moveTo(12f, 4.2f); lineTo(12f, 17f)
                    }
                },
                Color(0xFFF6EFE3), Color(0xFF9C7234), Color(0xFF302619), Color(0xFFE4C287)
            ),
            CategoryIconSpec(
                "health", "Здоровье",
                vec("OrigamiHealth") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 20f); lineTo(4f, 12f); lineTo(4f, 7.5f); lineTo(8f, 4.5f)
                        lineTo(12f, 8f); lineTo(16f, 4.5f); lineTo(20f, 7.5f); lineTo(20f, 12f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(12f, 8f); lineTo(16f, 4.5f); lineTo(20f, 7.5f); lineTo(20f, 12f); lineTo(12f, 20f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 20f); lineTo(4f, 12f); lineTo(4f, 7.5f); lineTo(8f, 4.5f)
                        lineTo(12f, 8f); lineTo(16f, 4.5f); lineTo(20f, 7.5f); lineTo(20f, 12f); close()
                    }
                    path(stroke = s, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                        moveTo(12f, 8.5f); lineTo(12f, 19.2f)
                    }
                },
                Color(0xFFF7EAEA), Color(0xFFA35454), Color(0xFF341F21), Color(0xFFE39E9E)
            ),
            CategoryIconSpec(
                "cloth", "Одежда",
                vec("OrigamiCloth") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(8f, 4.5f); lineTo(4f, 8f); lineTo(6.5f, 11f); lineTo(8f, 9.5f)
                        lineTo(8f, 19.5f); lineTo(16f, 19.5f); lineTo(16f, 9.5f); lineTo(17.5f, 11f)
                        lineTo(20f, 8f); lineTo(16f, 4.5f); lineTo(12f, 7f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(8f, 4.5f); lineTo(4f, 8f); lineTo(6.5f, 11f); lineTo(8f, 9.5f)
                        lineTo(8f, 19.5f); lineTo(16f, 19.5f); lineTo(16f, 9.5f); lineTo(17.5f, 11f)
                        lineTo(20f, 8f); lineTo(16f, 4.5f); lineTo(12f, 7f); close()
                    }
                },
                Color(0xFFF5EBF1), Color(0xFF965678), Color(0xFF311E29), Color(0xFFDEA5C4)
            ),
            CategoryIconSpec(
                "net", "Связь",
                vec("OrigamiNet") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 19.5f); lineTo(8.5f, 16f); lineTo(12f, 12.5f); lineTo(15.5f, 16f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 19.5f); lineTo(8.5f, 16f); lineTo(12f, 12.5f); lineTo(15.5f, 16f); close()
                        moveTo(6.5f, 12.5f); lineTo(12f, 7.5f); lineTo(17.5f, 12.5f)
                        moveTo(3.5f, 9.5f); lineTo(12f, 3.5f); lineTo(20.5f, 9.5f)
                    }
                },
                Color(0xFFE5EFF4), Color(0xFF3A7088), Color(0xFF1A2A32), Color(0xFF8ECAE0)
            ),
            CategoryIconSpec(
                "marketplace", "Маркетплейсы",
                vec("OrigamiMarketplace") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 3.5f); lineTo(20f, 7.5f); lineTo(20f, 16.5f); lineTo(12f, 20.5f)
                        lineTo(4f, 16.5f); lineTo(4f, 7.5f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(12f, 11.5f); lineTo(20f, 7.5f); lineTo(20f, 16.5f); lineTo(12f, 20.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 3.5f); lineTo(20f, 7.5f); lineTo(20f, 16.5f); lineTo(12f, 20.5f)
                        lineTo(4f, 16.5f); lineTo(4f, 7.5f); close()
                        moveTo(4f, 7.5f); lineTo(12f, 11.5f); lineTo(20f, 7.5f)
                        moveTo(12f, 11.5f); lineTo(12f, 20.5f)
                    }
                },
                Color(0xFFEEEAF7), Color(0xFF6B59A3), Color(0xFF262038), Color(0xFFC2B3ED)
            ),
            CategoryIconSpec(
                "installment", "Сплит и рассрочки",
                vec("OrigamiInstallment") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(4f, 6.5f); lineTo(14f, 6.5f); lineTo(10f, 17.5f); lineTo(4f, 17.5f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(14f, 6.5f); lineTo(20f, 6.5f); lineTo(20f, 17.5f); lineTo(10f, 17.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 6.5f); lineTo(20f, 6.5f); lineTo(20f, 17.5f); lineTo(4f, 17.5f); close()
                        moveTo(14f, 6.5f); lineTo(10f, 17.5f)
                    }
                },
                Color(0xFFF6EFE2), Color(0xFF9E7032), Color(0xFF302518), Color(0xFFE6C184)
            ),
            CategoryIconSpec(
                "kids", "Дети",
                vec("OrigamiKids") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 4f); lineTo(19f, 9f); lineTo(16.5f, 19f); lineTo(7.5f, 19f); lineTo(5f, 9f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 4f); lineTo(19f, 9f); lineTo(16.5f, 19f); lineTo(7.5f, 19f); lineTo(5f, 9f); close()
                        moveTo(9.5f, 13.5f); lineTo(12f, 15.5f); lineTo(14.5f, 13.5f)
                    }
                },
                Color(0xFFF7EEE6), Color(0xFFA36D45), Color(0xFF32241B), Color(0xFFE6B792)
            ),
            CategoryIconSpec(
                "pets", "Питомцы",
                vec("OrigamiPets") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(5f, 7f); lineTo(9.5f, 10.5f); lineTo(14.5f, 10.5f); lineTo(19f, 7f)
                        lineTo(18f, 16f); lineTo(12f, 20f); lineTo(6f, 16f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(5f, 7f); lineTo(9.5f, 10.5f); lineTo(14.5f, 10.5f); lineTo(19f, 7f)
                        lineTo(18f, 16f); lineTo(12f, 20f); lineTo(6f, 16f); close()
                    }
                    path(stroke = s, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                        moveTo(12f, 11f); lineTo(12f, 19.2f)
                    }
                },
                Color(0xFFF5EFE6), Color(0xFF8C6B48), Color(0xFF2E251C), Color(0xFFD9BA96)
            ),
            CategoryIconSpec(
                "edu", "Образование",
                vec("OrigamiEdu") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 4.5f); lineTo(21f, 9.5f); lineTo(12f, 14.5f); lineTo(3f, 9.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 4.5f); lineTo(21f, 9.5f); lineTo(12f, 14.5f); lineTo(3f, 9.5f); close()
                        moveTo(6.5f, 11.8f); lineTo(6.5f, 17f); lineTo(12f, 19.5f); lineTo(17.5f, 17f); lineTo(17.5f, 11.8f)
                    }
                },
                Color(0xFFE8EEF7), Color(0xFF476496), Color(0xFF1C2536), Color(0xFF9EB9E6)
            ),
            CategoryIconSpec(
                "gift", "Подарки",
                vec("OrigamiGift") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(4.5f, 9.5f); lineTo(19.5f, 9.5f); lineTo(19.5f, 19.5f); lineTo(4.5f, 19.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4.5f, 9.5f); lineTo(19.5f, 9.5f); lineTo(19.5f, 19.5f); lineTo(4.5f, 19.5f); close()
                        moveTo(12f, 9.5f); lineTo(12f, 19.5f)
                        moveTo(12f, 9.5f); lineTo(8f, 5.5f); lineTo(12f, 4.5f); lineTo(16f, 5.5f); close()
                    }
                },
                Color(0xFFF7EBF0), Color(0xFFA15572), Color(0xFF331E27), Color(0xFFE3A1BA)
            ),
            CategoryIconSpec(
                "salary", "Доход",
                vec("OrigamiSalary") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(4f, 6.5f); lineTo(20f, 6.5f); lineTo(20f, 17.5f); lineTo(4f, 17.5f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(12f, 9f); lineTo(15.5f, 12f); lineTo(12f, 15f); lineTo(8.5f, 12f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4f, 6.5f); lineTo(20f, 6.5f); lineTo(20f, 17.5f); lineTo(4f, 17.5f); close()
                        moveTo(12f, 9f); lineTo(15.5f, 12f); lineTo(12f, 15f); lineTo(8.5f, 12f); close()
                    }
                },
                Color(0xFFE5F3EC), Color(0xFF2B7A57), Color(0xFF182F25), Color(0xFF8AD4B0)
            ),
            CategoryIconSpec(
                "transfer", "Перевод",
                vec("OrigamiTransfer") {
                    path(fill = s, fillAlpha = 0.22f) {
                        moveTo(15f, 5.5f); lineTo(20f, 9.5f); lineTo(15f, 13.5f); close()
                        moveTo(9f, 10.5f); lineTo(4f, 14.5f); lineTo(9f, 18.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(4.5f, 9.5f); lineTo(19.5f, 9.5f); lineTo(15f, 5.5f)
                        moveTo(19.5f, 14.5f); lineTo(4.5f, 14.5f); lineTo(9f, 18.5f)
                    }
                },
                Color(0xFFE5F0F4), Color(0xFF396D85), Color(0xFF192B33), Color(0xFF8CC5DE)
            ),
            CategoryIconSpec(
                "utilities", "Коммунальные платежи",
                vec("OrigamiUtilities") {
                    // Гранёная лампочка-капля оригами (отличается от дома «Аренда»)
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 3.5f); lineTo(18f, 9f); lineTo(15.5f, 15.5f); lineTo(8.5f, 15.5f); lineTo(6f, 9f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(12f, 3.5f); lineTo(18f, 9f); lineTo(15.5f, 15.5f); lineTo(12f, 15.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 3.5f); lineTo(18f, 9f); lineTo(15.5f, 15.5f); lineTo(8.5f, 15.5f); lineTo(6f, 9f); close()
                        moveTo(9.5f, 18.2f); lineTo(14.5f, 18.2f)
                        moveTo(10.5f, 20.5f); lineTo(13.5f, 20.5f)
                    }
                    path(stroke = s, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                        moveTo(12f, 4.2f); lineTo(12f, 15f)
                    }
                },
                Color(0xFFF6F1E1), Color(0xFF9C7B2E), Color(0xFF302918), Color(0xFFE6CE83)
            ),
            CategoryIconSpec(
                "credit", "Кредит",
                vec("OrigamiCredit") {
                    // Гранёная банковская карта с диагональным ребром и чипом
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(3.5f, 6.5f); lineTo(20.5f, 6.5f); lineTo(20.5f, 17.5f); lineTo(3.5f, 17.5f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(3.5f, 10.5f); lineTo(20.5f, 10.5f); lineTo(20.5f, 17.5f); lineTo(3.5f, 17.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(3.5f, 6.5f); lineTo(20.5f, 6.5f); lineTo(20.5f, 17.5f); lineTo(3.5f, 17.5f); close()
                        moveTo(3.5f, 10.5f); lineTo(20.5f, 10.5f)
                        moveTo(7f, 14f); lineTo(10.5f, 14f)
                    }
                },
                Color(0xFFEAEAF6), Color(0xFF4F5499), Color(0xFF1F2238), Color(0xFFAAB0EB)
            ),
            CategoryIconSpec(
                "cafe", "Кафе и рестораны",
                vec("OrigamiCafe") {
                    // Гранёная чашка-оригами с паром
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(5f, 9.5f); lineTo(16.5f, 9.5f); lineTo(15f, 18.5f); lineTo(6.5f, 18.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(5f, 9.5f); lineTo(16.5f, 9.5f); lineTo(15f, 18.5f); lineTo(6.5f, 18.5f); close()
                        moveTo(16.5f, 11f); lineTo(19.5f, 11f); lineTo(19.5f, 14.5f); lineTo(16f, 15.5f)
                        moveTo(8.5f, 6.5f); lineTo(9.5f, 4.5f)
                        moveTo(12.5f, 6.5f); lineTo(13.5f, 4.5f)
                    }
                },
                Color(0xFFF6ECE5), Color(0xFFA05F3C), Color(0xFF32221A), Color(0xFFE6AE8C)
            ),
            CategoryIconSpec(
                "sport", "Спорт",
                vec("OrigamiSport") {
                    // Гранёная молния-энергия оригами
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(13.5f, 3.5f); lineTo(5.5f, 13f); lineTo(11.5f, 13f); lineTo(10.5f, 20.5f)
                        lineTo(18.5f, 11f); lineTo(12.5f, 11f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(13.5f, 3.5f); lineTo(5.5f, 13f); lineTo(11.5f, 13f); lineTo(10.5f, 20.5f)
                        lineTo(18.5f, 11f); lineTo(12.5f, 11f); close()
                    }
                },
                Color(0xFFE3F3F0), Color(0xFF28786C), Color(0xFF172E2A), Color(0xFF84D4C6)
            ),
            CategoryIconSpec(
                "beauty", "Уход и красота",
                vec("OrigamiBeauty") {
                    // Гранёный цветок-кристалл оригами
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 3.5f); lineTo(15f, 9f); lineTo(20.5f, 12f); lineTo(15f, 15f)
                        lineTo(12f, 20.5f); lineTo(9f, 15f); lineTo(3.5f, 12f); lineTo(9f, 9f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 3.5f); lineTo(15f, 9f); lineTo(20.5f, 12f); lineTo(15f, 15f)
                        lineTo(12f, 20.5f); lineTo(9f, 15f); lineTo(3.5f, 12f); lineTo(9f, 9f); close()
                    }
                },
                Color(0xFFF6EAF2), Color(0xFF9C5280), Color(0xFF331D2B), Color(0xFFE3A0CA)
            ),
            CategoryIconSpec(
                "subscriptions", "Подписки",
                vec("OrigamiSubscriptions") {
                    // Гранёный двойной ромб-повтор оригами
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(7.5f, 7.5f); lineTo(12f, 12f); lineTo(7.5f, 16.5f); lineTo(3f, 12f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(16.5f, 7.5f); lineTo(21f, 12f); lineTo(16.5f, 16.5f); lineTo(12f, 12f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(7.5f, 7.5f); lineTo(12f, 12f); lineTo(7.5f, 16.5f); lineTo(3f, 12f); close()
                        moveTo(16.5f, 7.5f); lineTo(21f, 12f); lineTo(16.5f, 16.5f); lineTo(12f, 12f); close()
                    }
                },
                Color(0xFFECEAF7), Color(0xFF5D54A3), Color(0xFF221F38), Color(0xFFB7AFF0)
            ),
            CategoryIconSpec(
                "other", "Прочее",
                vec("OrigamiOther") {
                    path(fill = s, fillAlpha = 0.24f) {
                        moveTo(12f, 3.5f); lineTo(20.5f, 12f); lineTo(12f, 20.5f); lineTo(3.5f, 12f); close()
                    }
                    path(fill = s, fillAlpha = 0.14f) {
                        moveTo(12f, 3.5f); lineTo(20.5f, 12f); lineTo(12f, 20.5f); close()
                    }
                    path(stroke = s, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                        moveTo(12f, 3.5f); lineTo(20.5f, 12f); lineTo(12f, 20.5f); lineTo(3.5f, 12f); close()
                    }
                    path(stroke = s, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                        moveTo(12f, 4.2f); lineTo(12f, 19.8f)
                    }
                },
                Color(0xFFECEFF2), Color(0xFF546370), Color(0xFF202A30), Color(0xFFA7B7C4)
            )
        )
    }

    fun byKey(key: String): CategoryIconSpec = all.firstOrNull { it.key == key } ?: all.last()

    fun autoKey(name: String): String {
        val n = name.lowercase()
        return when {
            listOf("кафе", "ресторан", "кофе", "доставк").any { it in n } -> "cafe"
            listOf("продукт", "еда", "супермарк").any { it in n } -> "food"
            listOf("коммун", "жкх", "свет", "вод", "электрич").any { it in n } -> "utilities"
            listOf("жил", "кварт", "аренд", "ипотек", "дом", "ремонт").any { it in n } -> "home"
            listOf("кредит", "займ", "долг").any { it in n } -> "credit"
            listOf("сплит", "рассрочк", "долями", "подели").any { it in n } -> "installment"
            listOf("трансп", "авто", "бенз", "такси", "метро", "проезд", "парков").any { it in n } -> "car"
            listOf("развлеч", "кино", "игр", "хобби", "отдых", "путеш", "отпуск", "досуг").any { it in n } -> "fun"
            listOf("спорт", "фитнес", "тренир", "зал").any { it in n } -> "sport"
            listOf("здоров", "аптек", "врач", "медиц", "стомат").any { it in n } -> "health"
            listOf("стрижк", "маникюр", "красот", "парикм", "космет", "уход").any { it in n } -> "beauty"
            listOf("одежд", "обув").any { it in n } -> "cloth"
            listOf("подпис").any { it in n } -> "subscriptions"
            listOf("связ", "интерн", "телеф").any { it in n } -> "net"
            listOf("маркетплейс", "ozon", "озон", "wildberries", "wb", "вайлдберриз", "яндекс маркет").any { it in n } -> "marketplace"
            listOf("дет", "ребён", "ребен", "сад", "школ").any { it in n } -> "kids"
            listOf("питом", "кот", "собак", "живот", "вет").any { it in n } -> "pets"
            listOf("образ", "курс", "учёб", "учеб", "книг").any { it in n } -> "edu"
            listOf("подар", "праздн", "цвет").any { it in n } -> "gift"
            listOf("зарпл", "доход", "прем", "аванс", "подработ", "кэшбэк", "процент").any { it in n } -> "salary"
            listOf("перевод", "пополн").any { it in n } -> "transfer"
            else -> "other"
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
    size: Dp = 40.dp
) {
    val spec = CategoryIcons.resolve(categoryName, overrides)
    val dark = isSystemInDarkTheme()
    val bg = if (dark) spec.darkBg else spec.lightBg
    val fg = if (dark) spec.darkFg else spec.lightFg
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size * 0.34f),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = if (dark) 0.34f else 0.22f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    }
}
