package com.devekene.healthtracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.HealthCheckSummary
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.ui.theme.StatusAmber
import com.devekene.healthtracker.ui.theme.StatusBlue
import com.devekene.healthtracker.ui.theme.StatusCoral
import com.devekene.healthtracker.ui.theme.StatusEmerald
import com.devekene.healthtracker.ui.theme.accentColor
import com.example.R
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BmiGaugeCard(
    summary: HealthCheckSummary,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    val accent = summary.bmiCategory.accentColor()
    val clampedBmi = summary.bmi.toFloat().coerceIn(12f, 44f)
    val targetFraction = ((clampedBmi - 12f) / (44f - 12f)).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bmi_gauge_fraction"
    )

    val trackBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val markerCenterColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bmi_gauge_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_bmi_score),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = accent.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        )
                        .border(
                            width = 1.dp,
                            color = accent.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = summary.bmiCategory.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(220.dp, 168.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.width.coerceAtMost(size.height * 1.45f) - strokeWidth
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = strokeWidth
                    )
                    val arcSize = Size(diameter, diameter)
                    val startAngle = 160f
                    val totalSweep = 220f

                    // Base muted track
                    drawArc(
                        color = trackBackground,
                        startAngle = startAngle,
                        sweepAngle = totalSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Subtle clinical zone bands (12..44 -> 32 BMI span)
                    // Underweight: 12..18.5 (6.5 / 32 = 20.3%)
                    // Normal: 18.5..25.0 (6.5 / 32 = 20.3%)
                    // Overweight: 25.0..30.0 (5.0 / 32 = 15.6%)
                    // Obese: 30.0..44.0 (14.0 / 32 = 43.8%)
                    val segments = listOf(
                        0.00f to 0.203f to StatusBlue,
                        0.203f to 0.406f to StatusEmerald,
                        0.406f to 0.562f to StatusAmber,
                        0.562f to 1.00f to StatusCoral
                    )
                    val gapDeg = 2.5f
                    segments.forEach { (range, color) ->
                        val segStart = startAngle + range.first * totalSweep + gapDeg / 2f
                        val segSweep = ((range.second - range.first) * totalSweep - gapDeg).coerceAtLeast(1f)
                        drawArc(
                            color = color.copy(alpha = 0.28f),
                            startAngle = segStart,
                            sweepAngle = segSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }

                    // Active sweep up to current BMI
                    val activeSweep = (animatedFraction * totalSweep).coerceAtLeast(2f)
                    drawArc(
                        color = accent,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth * 0.55f, cap = StrokeCap.Round)
                    )

                    // Indicator thumb
                    val angleRad = (startAngle + activeSweep) * (PI / 180.0)
                    val radius = diameter / 2f
                    val center = Offset(topLeft.x + radius, topLeft.y + radius)
                    val thumbPos = Offset(
                        x = center.x + (radius * cos(angleRad)).toFloat(),
                        y = center.y + (radius * sin(angleRad)).toFloat()
                    )

                    drawCircle(
                        color = accent.copy(alpha = 0.32f),
                        radius = 14.dp.toPx(),
                        center = thumbPos
                    )
                    drawCircle(
                        color = accent,
                        radius = 8.dp.toPx(),
                        center = thumbPos
                    )
                    drawCircle(
                        color = markerCenterColor,
                        radius = 3.5.dp.toPx(),
                        center = thumbPos
                    )
                }

                Column(
                    modifier = Modifier.padding(top = 34.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", summary.bmi),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("bmi_value_text")
                    )
                    Text(
                        text = "WHO range ${summary.bmiCategory.rangeDescription}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val minHealthy = HealthFormatters.formatWeight(summary.healthyWeightMinKg, unitSystem)
            val maxHealthy = HealthFormatters.formatWeight(summary.healthyWeightMaxKg, unitSystem)

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_healthy_range),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$minHealthy – $maxHealthy",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = summary.bmiCategory.guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
