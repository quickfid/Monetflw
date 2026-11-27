package ru.familybudget.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Единый авторский набор векторных иконок приложения в стиле «Фарфоровое Оригами со световым ребром»
 * (Luminous Matte Porcelain-Origami): гранёная геометрия, две сложенные плоскости разной плотности
 * и тончайшее жемчужное ребро сгиба.
 */
object AppIcons {
    private val stroke = SolidColor(Color.Black)
    private val facetLight = SolidColor(Color.Black)
    private val facetDeep = SolidColor(Color.Black)

    private fun origami(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    // ─── Нижняя навигация ─────────────────────────────────────────────────────

    /** Главная: гранёный дом-оригами со световым ребром крыши. */
    val Home: ImageVector by lazy {
        origami("OrigamiHome") {
            path(fill = facetLight, fillAlpha = 0.22f) {
                moveTo(12f, 3f); lineTo(3.5f, 10.5f); lineTo(5.5f, 10.5f); lineTo(5.5f, 20f)
                lineTo(12f, 20f); close()
            }
            path(fill = facetDeep, fillAlpha = 0.12f) {
                moveTo(12f, 3f); lineTo(20.5f, 10.5f); lineTo(18.5f, 10.5f); lineTo(18.5f, 20f)
                lineTo(12f, 20f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.85f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3.5f, 10.5f); lineTo(12f, 3f); lineTo(20.5f, 10.5f)
                moveTo(5.5f, 9.2f); lineTo(5.5f, 19.5f); lineTo(18.5f, 19.5f); lineTo(18.5f, 9.2f)
            }
            // Световое ребро сгиба
            path(stroke = stroke, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 3.6f); lineTo(12f, 19.2f)
            }
        }
    }

    /** План: гранёный календарь-оригами с двумя плоскостями. */
    val Plan: ImageVector by lazy {
        origami("OrigamiPlan") {
            path(fill = facetLight, fillAlpha = 0.24f) {
                moveTo(4f, 5.5f); lineTo(20f, 5.5f); lineTo(20f, 10f); lineTo(4f, 10f); close()
            }
            path(fill = facetDeep, fillAlpha = 0.12f) {
                moveTo(4f, 10f); lineTo(14f, 10f); lineTo(4f, 20f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.85f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 5.5f); lineTo(20f, 5.5f); lineTo(20f, 19.5f); lineTo(4f, 19.5f); close()
                moveTo(4f, 10f); lineTo(20f, 10f)
                moveTo(8f, 3.2f); lineTo(8f, 6.2f)
                moveTo(16f, 3.2f); lineTo(16f, 6.2f)
            }
            // Диагональное ребро + отметка
            path(stroke = stroke, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                moveTo(14.5f, 10f); lineTo(4.5f, 19.2f)
            }
            path(stroke = stroke, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(11f, 15f); lineTo(13.2f, 17f); lineTo(17f, 12.8f)
            }
        }
    }

    /** Траты: гранёный чек-оригами со сгибом. */
    val Expenses: ImageVector by lazy {
        origami("OrigamiExpenses") {
            path(fill = facetLight, fillAlpha = 0.22f) {
                moveTo(5f, 3.5f); lineTo(15f, 3.5f); lineTo(19f, 7.5f); lineTo(19f, 20.5f)
                lineTo(15.5f, 18.5f); lineTo(12f, 20.5f); lineTo(8.5f, 18.5f); lineTo(5f, 20.5f); close()
            }
            path(fill = facetDeep, fillAlpha = 0.16f) {
                moveTo(15f, 3.5f); lineTo(19f, 7.5f); lineTo(15f, 7.5f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.85f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5f, 3.5f); lineTo(15f, 3.5f); lineTo(19f, 7.5f); lineTo(19f, 20.5f)
                lineTo(15.5f, 18.5f); lineTo(12f, 20.5f); lineTo(8.5f, 18.5f); lineTo(5f, 20.5f); close()
                moveTo(15f, 3.5f); lineTo(15f, 7.5f); lineTo(19f, 7.5f)
                moveTo(8.5f, 11.5f); lineTo(15.5f, 11.5f)
                moveTo(8.5f, 15f); lineTo(13.5f, 15f)
            }
        }
    }

    /** Накопления: гранёный кристалл-бриллиант оригами. */
    val Savings: ImageVector by lazy {
        origami("OrigamiSavings") {
            path(fill = facetLight, fillAlpha = 0.24f) {
                moveTo(7f, 4.5f); lineTo(17f, 4.5f); lineTo(21f, 9.5f); lineTo(12f, 20.5f); lineTo(3f, 9.5f); close()
            }
            path(fill = facetDeep, fillAlpha = 0.14f) {
                moveTo(12f, 4.5f); lineTo(17f, 4.5f); lineTo(21f, 9.5f); lineTo(12f, 20.5f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.85f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(7f, 4.5f); lineTo(17f, 4.5f); lineTo(21f, 9.5f); lineTo(12f, 20.5f); lineTo(3f, 9.5f); close()
                moveTo(3f, 9.5f); lineTo(21f, 9.5f)
            }
            path(stroke = stroke, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9.5f, 9.5f); lineTo(12f, 4.5f); lineTo(14.5f, 9.5f); lineTo(12f, 20.5f); close()
            }
        }
    }

    /** Ещё: 4 гранёных ромба-оригами. */
    val More: ImageVector by lazy {
        origami("OrigamiMore") {
            path(fill = facetLight, fillAlpha = 0.24f) {
                moveTo(7.5f, 3.5f); lineTo(11f, 7f); lineTo(7.5f, 10.5f); lineTo(4f, 7f); close()
                moveTo(16.5f, 13.5f); lineTo(20f, 17f); lineTo(16.5f, 20.5f); lineTo(13f, 17f); close()
            }
            path(fill = facetDeep, fillAlpha = 0.14f) {
                moveTo(16.5f, 3.5f); lineTo(20f, 7f); lineTo(16.5f, 10.5f); lineTo(13f, 7f); close()
                moveTo(7.5f, 13.5f); lineTo(11f, 17f); lineTo(7.5f, 20.5f); lineTo(4f, 17f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.75f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(7.5f, 3.5f); lineTo(11f, 7f); lineTo(7.5f, 10.5f); lineTo(4f, 7f); close()
                moveTo(16.5f, 3.5f); lineTo(20f, 7f); lineTo(16.5f, 10.5f); lineTo(13f, 7f); close()
                moveTo(7.5f, 13.5f); lineTo(11f, 17f); lineTo(7.5f, 20.5f); lineTo(4f, 17f); close()
                moveTo(16.5f, 13.5f); lineTo(20f, 17f); lineTo(16.5f, 20.5f); lineTo(13f, 17f); close()
            }
        }
    }

    // ─── Действия, навигация, галочки, обязательные платежи ──────────────────

    val Add: ImageVector by lazy {
        origami("OrigamiAdd") {
            path(fill = facetLight, fillAlpha = 0.18f) {
                moveTo(12f, 3.5f); lineTo(20.5f, 12f); lineTo(12f, 20.5f); lineTo(3.5f, 12f); close()
            }
            path(stroke = stroke, strokeLineWidth = 2.0f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 6.5f); lineTo(12f, 17.5f)
                moveTo(6.5f, 12f); lineTo(17.5f, 12f)
            }
        }
    }

    val Close: ImageVector by lazy {
        origami("OrigamiClose") {
            path(stroke = stroke, strokeLineWidth = 2.0f, strokeLineCap = StrokeCap.Round) {
                moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f)
                moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
            }
        }
    }

    val Back: ImageVector by lazy {
        origami("OrigamiBack") {
            path(fill = facetLight, fillAlpha = 0.18f) {
                moveTo(11.5f, 5.5f); lineTo(5f, 12f); lineTo(11.5f, 18.5f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.95f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(11.5f, 5.5f); lineTo(5f, 12f); lineTo(11.5f, 18.5f)
                moveTo(5.5f, 12f); lineTo(19.5f, 12f)
            }
        }
    }

    val ChevronLeft: ImageVector by lazy {
        origami("OrigamiChevronLeft") {
            path(fill = facetLight, fillAlpha = 0.16f) {
                moveTo(14.5f, 6f); lineTo(8.5f, 12f); lineTo(14.5f, 18f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.95f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14.5f, 6f); lineTo(8.5f, 12f); lineTo(14.5f, 18f)
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        origami("OrigamiChevronRight") {
            path(fill = facetLight, fillAlpha = 0.16f) {
                moveTo(9.5f, 6f); lineTo(15.5f, 12f); lineTo(9.5f, 18f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.95f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9.5f, 6f); lineTo(15.5f, 12f); lineTo(9.5f, 18f)
            }
        }
    }

    val ArrowDropDown: ImageVector by lazy {
        origami("OrigamiDropDown") {
            path(fill = facetLight, fillAlpha = 0.24f) {
                moveTo(6.5f, 9f); lineTo(17.5f, 9f); lineTo(12f, 15.5f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.85f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6.5f, 9f); lineTo(12f, 15.5f); lineTo(17.5f, 9f)
            }
        }
    }

    val Calendar: ImageVector by lazy { Plan }

    /** Галочка выполнения в гранёном шестиугольнике-оригами. */
    val CheckBadge: ImageVector by lazy {
        origami("OrigamiCheckBadge") {
            path(fill = facetLight, fillAlpha = 0.24f) {
                moveTo(12f, 2.5f); lineTo(20f, 7.2f); lineTo(20f, 16.8f); lineTo(12f, 21.5f)
                lineTo(4f, 16.8f); lineTo(4f, 7.2f); close()
            }
            path(fill = facetDeep, fillAlpha = 0.12f) {
                moveTo(12f, 2.5f); lineTo(20f, 7.2f); lineTo(20f, 16.8f); lineTo(12f, 21.5f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.85f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2.5f); lineTo(20f, 7.2f); lineTo(20f, 16.8f); lineTo(12f, 21.5f)
                lineTo(4f, 16.8f); lineTo(4f, 7.2f); close()
                moveTo(8.2f, 12.2f); lineTo(11f, 15f); lineTo(16.2f, 9.5f)
            }
        }
    }

    /** Обязательный платёж: гранёный щит-календарь. */
    val Mandatory: ImageVector by lazy {
        origami("OrigamiMandatory") {
            path(fill = facetLight, fillAlpha = 0.24f) {
                moveTo(12f, 3f); lineTo(19.5f, 6f); lineTo(18.5f, 14f); lineTo(12f, 21f)
                lineTo(5.5f, 14f); lineTo(4.5f, 6f); close()
            }
            path(fill = facetDeep, fillAlpha = 0.12f) {
                moveTo(12f, 3f); lineTo(19.5f, 6f); lineTo(18.5f, 14f); lineTo(12f, 21f); close()
            }
            path(stroke = stroke, strokeLineWidth = 1.85f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3f); lineTo(19.5f, 6f); lineTo(18.5f, 14f); lineTo(12f, 21f)
                lineTo(5.5f, 14f); lineTo(4.5f, 6f); close()
            }
            path(stroke = stroke, strokeAlpha = 0.55f, strokeLineWidth = 1.15f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 3.6f); lineTo(12f, 20.2f)
            }
        }
    }
}
