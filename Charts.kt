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
