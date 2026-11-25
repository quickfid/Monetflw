package ru.familybudget.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Кольцевая диаграмма. Цвета берутся по кругу из [colors]. */
@Composable
fun DonutChart(values: List<Float>, colors: List<Color>, modifier: Modifier = Modifier.size(190.dp)) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val stroke = size.minDimension * 0.22f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        val total = values.sum()
        if (total <= 0f) {
            drawArc(emptyColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        } else {
            var start = -90f
            values.forEachIndexed { i, v ->
                val sweep = 360f * v / total
                drawArc(colors[i % colors.size], start, sweep, false, topLeft, arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
    }
}

/** Столбчатая диаграмма без Canvas: столбцы + подписи под ними. */
@Composable
fun BarChart(
    values: List<Long>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(height),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEach { v ->
                if (v <= 0L) {
                    Spacer(Modifier.weight(1f))
                } else {
                    val fraction = (v.toFloat() / max).coerceIn(0.03f, 1f)
                    Box(
                        Modifier.weight(1f).fillMaxHeight(fraction)
                            .background(color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            labels.forEach { l ->
                Text(
                    l,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Линейный график трат за месяц: сплошная линия — факт (накопительно),
 * пунктир — прогноз до конца месяца, горизонтальная линия — бюджет.
 */
@Composable
fun BurnChart(
    actual: List<Long>,
    daysInMonth: Int,
    projectedEnd: Long?,
    budget: Long,
    actualColor: Color,
    projectionColor: Color,
    budgetColor: Color,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(170.dp)) {
        val maxValue = maxOf(budget, projectedEnd ?: 0L, actual.lastOrNull() ?: 0L, 1L).toFloat() * 1.08f
        val w = size.width
        val h = size.height
        val lastIndex = (daysInMonth - 1).coerceAtLeast(1)
        fun px(i: Int): Float = w * i / lastIndex
        fun py(v: Long): Float = h - h * (v.toFloat() / maxValue)

        for (g in 0..3) {
            val y = h * g / 3f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        if (budget > 0) {
            drawLine(
                budgetColor, Offset(0f, py(budget)), Offset(w, py(budget)),
                strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
            )
        }
        if (actual.isNotEmpty()) {
            val path = Path()
            actual.forEachIndexed { i, v ->
                if (i == 0) path.moveTo(px(i), py(v)) else path.lineTo(px(i), py(v))
            }
            drawPath(path, actualColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
            if (projectedEnd != null && actual.size < daysInMonth) {
                val from = Offset(px(actual.size - 1), py(actual.last()))
                val to = Offset(px(daysInMonth - 1), py(projectedEnd))
                drawLine(
                    projectionColor, from, to, strokeWidth = 5f, cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                )
                drawCircle(projectionColor, radius = 8f, center = to)
            }
            drawCircle(actualColor, radius = 8f, center = Offset(px(actual.size - 1), py(actual.last())))
        }
    }
}
