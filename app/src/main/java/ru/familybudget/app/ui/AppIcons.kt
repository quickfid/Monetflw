package ru.familybudget.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Аккуратные векторные иконки для нижней навигации приложения. */
object AppIcons {
    private val stroke = SolidColor(Color.Black)

    val Home: ImageVector by lazy {
        ImageVector.Builder("NavHome", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 10.5f)
                lineTo(12f, 3.5f)
                lineTo(21f, 10.5f)
                moveTo(5.5f, 9.5f)
                verticalLineTo(19.5f)
                curveTo(5.5f, 20.05f, 5.95f, 20.5f, 6.5f, 20.5f)
                horizontalLineTo(17.5f)
                curveTo(18.05f, 20.5f, 18.5f, 20.05f, 18.5f, 19.5f)
                verticalLineTo(9.5f)
                moveTo(10f, 20.5f)
                verticalLineTo(14.5f)
                horizontalLineTo(14f)
                verticalLineTo(20.5f)
            }
        }.build()
    }

    val Plan: ImageVector by lazy {
        ImageVector.Builder("NavPlan", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5f, 4.5f)
                horizontalLineTo(19f)
                curveTo(20.1f, 4.5f, 21f, 5.4f, 21f, 6.5f)
                verticalLineTo(18.5f)
                curveTo(21f, 19.6f, 20.1f, 20.5f, 19f, 20.5f)
                horizontalLineTo(5f)
                curveTo(3.9f, 20.5f, 3f, 19.6f, 3f, 18.5f)
                verticalLineTo(6.5f)
                curveTo(3f, 5.4f, 3.9f, 4.5f, 5f, 4.5f)
                close()
                moveTo(8f, 2.5f)
                verticalLineTo(6f)
                moveTo(16f, 2.5f)
                verticalLineTo(6f)
                moveTo(3f, 9.5f)
                horizontalLineTo(21f)
                moveTo(8.5f, 14.5f)
                lineTo(10.8f, 16.8f)
                lineTo(15.5f, 12.2f)
            }
        }.build()
    }

    val Expenses: ImageVector by lazy {
        ImageVector.Builder("NavExpenses", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3.5f, 6.5f)
                horizontalLineTo(19.5f)
                curveTo(20.6f, 6.5f, 21.5f, 7.4f, 21.5f, 8.5f)
                verticalLineTo(17.5f)
                curveTo(21.5f, 18.6f, 20.6f, 19.5f, 19.5f, 19.5f)
                horizontalLineTo(4.5f)
                curveTo(3.4f, 19.5f, 2.5f, 18.6f, 2.5f, 17.5f)
                verticalLineTo(7.5f)
                curveTo(2.5f, 6.95f, 2.95f, 6.5f, 3.5f, 6.5f)
                close()
                moveTo(2.5f, 10.5f)
                horizontalLineTo(21.5f)
                moveTo(15.5f, 15f)
                horizontalLineTo(18f)
            }
        }.build()
    }

    val Savings: ImageVector by lazy {
        ImageVector.Builder("NavSavings", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 20.5f)
                verticalLineTo(11f)
                moveTo(12f, 14f)
                curveTo(12f, 10.5f, 9.2f, 8.2f, 5.5f, 8.5f)
                curveTo(5.8f, 12f, 8.2f, 14f, 12f, 14f)
                close()
                moveTo(12f, 11.5f)
                curveTo(12f, 7.8f, 14.8f, 5.5f, 18.5f, 5.8f)
                curveTo(18.2f, 9.3f, 15.8f, 11.5f, 12f, 11.5f)
                close()
                moveTo(7f, 20.5f)
                horizontalLineTo(17f)
            }
        }.build()
    }

    val More: ImageVector by lazy {
        ImageVector.Builder("NavMore", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = stroke, strokeLineWidth = 1.9f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 7f)
                horizontalLineTo(20f)
                moveTo(4f, 12f)
                horizontalLineTo(20f)
                moveTo(4f, 17f)
                horizontalLineTo(20f)
            }
        }.build()
    }
}
