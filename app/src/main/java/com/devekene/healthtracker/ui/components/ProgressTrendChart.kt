package com.devekene.healthtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.domain.HealthCalculator
import com.devekene.healthtracker.ui.theme.StatusEmerald
import com.devekene.healthtracker.ui.viewmodel.ProgressMetric
import java.util.Locale
import kotlin.math.abs

data class ChartDataPoint(
    val timestampMillis: Long,
    val value: Double,
    val label: String
)

@Composable
fun ProgressTrendChart(
    points: List<ChartDataPoint>,
    metric: ProgressMetric,
    unitSystem: UnitSystem,
    targetWeightKg: Double,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points, metric) { mutableIntStateOf(points.lastIndex) }
    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeIndex]

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val pointCenterColor = MaterialTheme.colorScheme.surface
    val targetLineColor = StatusEmerald.copy(alpha = 0.7f)

    val referenceValue: Double? = when (metric) {
        ProgressMetric.BMI -> 24.9
        ProgressMetric.WEIGHT -> when (unitSystem) {
            UnitSystem.METRIC -> targetWeightKg
            UnitSystem.IMPERIAL -> HealthCalculator.kgToLbs(targetWeightKg)
        }
        ProgressMetric.WAIST_RATIO -> 0.50
    }

    val allValues = buildList {
        addAll(points.map { it.value })
        referenceValue?.let { add(it) }
    }
    val rawMin = allValues.minOrNull() ?: 0.0
    val rawMax = allValues.maxOrNull() ?: 1.0
    val span = (rawMax - rawMin).coerceAtLeast(
        when (metric) {
            ProgressMetric.WAIST_RATIO -> 0.1
            else -> 2.0
        }
    )
    val yMin = (rawMin - span * 0.18).coerceAtLeast(0.0)
    val yMax = rawMax + span * 0.18
    val yRange = (yMax - yMin).coerceAtLeast(0.01)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("progress_trend_chart"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = HealthFormatters.formatDateTime(selectedPoint.timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedPoint.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                referenceValue?.let { ref ->
                    val refLabel = when (metric) {
                        ProgressMetric.BMI -> "Healthy <= 24.9"
                        ProgressMetric.WEIGHT -> "Goal ${
                            String.format(
                                Locale.US,
                                "%.1f %s",
                                ref,
                                if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"
                            )
                        }"
                        ProgressMetric.WAIST_RATIO -> "Healthy < 0.50"
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                color = StatusEmerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = refLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                if (points.size == 1) {
                                    selectedIndex = 0
                                } else {
                                    val pad = 16.dp.toPx()
                                    val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
                                    var nearestIdx = 0
                                    var minDist = Float.MAX_VALUE
                                    points.indices.forEach { i ->
                                        val px = pad + (i.toFloat() / (points.size - 1)) * usableW
                                        val dist = abs(tapOffset.x - px)
                                        if (dist < minDist) {
                                            minDist = dist
                                            nearestIdx = i
                                        }
                                    }
                                    selectedIndex = nearestIdx
                                }
                            }
                        }
                ) {
                    val padH = 16.dp.toPx()
                    val padV = 14.dp.toPx()
                    val chartW = (size.width - padH * 2f).coerceAtLeast(1f)
                    val chartH = (size.height - padV * 2f).coerceAtLeast(1f)

                    fun yFor(value: Double): Float {
                        val norm = ((value - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                        return padV + chartH * (1f - norm)
                    }

                    fun xFor(index: Int): Float {
                        return if (points.size <= 1) {
                            size.width / 2f
                        } else {
                            padH + (index.toFloat() / (points.size - 1)) * chartW
                        }
                    }

                    // Horizontal subtle gridlines
                    val gridSteps = 3
                    for (step in 0..gridSteps) {
                        val gy = padV + (chartH / gridSteps) * step
                        drawLine(
                            color = gridColor.copy(alpha = 0.55f),
                            start = Offset(padH, gy),
                            end = Offset(size.width - padH, gy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Dashed reference / goal line
                    referenceValue?.let { ref ->
                        val ry = yFor(ref)
                        drawLine(
                            color = targetLineColor,
                            start = Offset(padH, ry),
                            end = Offset(size.width - padH, ry),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        )
                    }

                    val offsets = points.mapIndexed { idx, point ->
                        Offset(x = xFor(idx), y = yFor(point.value))
                    }

                    if (offsets.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(offsets.first().x, offsets.first().y)
                            for (i in 1 until offsets.size) {
                                val prev = offsets[i - 1]
                                val curr = offsets[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(offsets.last().x, padV + chartH)
                            lineTo(offsets.first().x, padV + chartH)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                startY = padV,
                                endY = padV + chartH
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Selected point vertical guide
                    val selectedOffset = offsets[safeIndex]
                    drawLine(
                        color = lineColor.copy(alpha = 0.38f),
                        start = Offset(selectedOffset.x, padV),
                        end = Offset(selectedOffset.x, padV + chartH),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    // Data dots
                    offsets.forEachIndexed { idx, pos ->
                        val isSelected = idx == safeIndex
                        if (isSelected) {
                            drawCircle(
                                color = lineColor.copy(alpha = 0.25f),
                                radius = 12.dp.toPx(),
                                center = pos
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 6.dp.toPx(),
                                center = pos
                            )
                            drawCircle(
                                color = pointCenterColor,
                                radius = 2.5.dp.toPx(),
                                center = pos
                            )
                        } else {
                            drawCircle(
                                color = lineColor.copy(alpha = 0.85f),
                                radius = 3.5.dp.toPx(),
                                center = pos
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = HealthFormatters.formatShortDate(points.first().timestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (points.size > 1) {
                    Text(
                        text = "Tap any point to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = HealthFormatters.formatShortDate(points.last().timestampMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
