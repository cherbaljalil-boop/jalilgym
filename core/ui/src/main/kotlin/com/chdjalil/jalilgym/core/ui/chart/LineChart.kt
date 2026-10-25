package com.chdjalil.jalilgym.core.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One chart point; [x] is usually epoch millis. */
data class ChartPoint(val x: Long, val y: Double)

/**
 * A minimal line chart: [points] as a line with dots, plus an optional dashed
 * [trend] series. The y range is padded so flat data stays readable; the
 * labels show the highest and lowest values, formatted by [formatY].
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    contentDescription: String,
    formatY: (Double) -> String,
    modifier: Modifier = Modifier,
    trend: List<ChartPoint> = emptyList(),
    height: Dp = 180.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    trendColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val all = points + trend
    if (all.isEmpty()) return
    val minY = all.minOf { it.y }
    val maxY = all.maxOf { it.y }
    val pad = ((maxY - minY) * 0.1).takeIf { it > 0 } ?: maxOf(1.0, maxY * 0.05)
    val low = minY - pad
    val high = maxY + pad
    val minX = all.minOf { it.x }
    val maxX = all.maxOf { it.x }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.semantics { this.contentDescription = contentDescription }) {
        Row(Modifier.fillMaxWidth()) {
            Text(formatY(maxY), style = labelStyle, color = labelColor)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val dot = 3.dp.toPx()
            fun position(point: ChartPoint): Offset {
                val fx = if (maxX == minX) 0.5f else (point.x - minX).toFloat() / (maxX - minX)
                val fy = ((point.y - low) / (high - low)).toFloat()
                return Offset(dot + fx * (size.width - 2 * dot), size.height * (1 - fy))
            }
            listOf(0f, 0.5f, 1f).forEach { f ->
                drawLine(gridColor, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 1.dp.toPx())
            }
            fun series(series: List<ChartPoint>, color: Color, dashed: Boolean) {
                if (series.size < 2) return
                val path = Path()
                series.forEachIndexed { i, point ->
                    val o = position(point)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(
                    path,
                    color,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null,
                    ),
                )
            }
            series(trend, trendColor, dashed = true)
            series(points, lineColor, dashed = false)
            points.forEach { drawCircle(lineColor, radius = dot, center = position(it)) }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(formatY(minY), style = labelStyle, color = labelColor)
            Spacer(Modifier.weight(1f))
        }
    }
}
