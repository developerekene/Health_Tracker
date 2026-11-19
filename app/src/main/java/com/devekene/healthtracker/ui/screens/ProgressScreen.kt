package com.devekene.healthtracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.domain.HealthCalculator
import com.devekene.healthtracker.ui.components.ChartDataPoint
import com.devekene.healthtracker.ui.components.EmptyHealthStateCard
import com.devekene.healthtracker.ui.components.HealthFormatters
import com.devekene.healthtracker.ui.components.NumericEditDialog
import com.devekene.healthtracker.ui.components.ProgressTrendChart
import com.devekene.healthtracker.ui.theme.StatusEmerald
import com.devekene.healthtracker.ui.theme.accentColor
import com.devekene.healthtracker.ui.viewmodel.HealthTrackerUiState
import com.devekene.healthtracker.ui.viewmodel.ProgressMetric
import com.devekene.healthtracker.ui.viewmodel.ProgressTimeRange
import com.example.R
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(
    state: HealthTrackerUiState,
    contentPadding: PaddingValues,
    onTimeRangeChange: (ProgressTimeRange) -> Unit,
    onMetricChange: (ProgressMetric) -> Unit,
    onTargetWeightChange: (Double) -> Unit,
    onNavigateToCheckup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unitSystem = state.preferences.unitSystem
    val targetWeightKg = state.preferences.targetWeightKg
    var showGoalDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val filteredRecords = remember(state.records, state.progressTimeRange) {
        val days = state.progressTimeRange.days
        val list = if (days == null) {
            state.records
        } else {
            val cutoff = now - days * 86_400_000L
            state.records.filter { it.timestampMillis >= cutoff }
        }
        // Chronological order (oldest -> newest) for charts
        list.reversed()
    }

    val chartPoints = remember(filteredRecords, state.progressMetric, unitSystem) {
        when (state.progressMetric) {
            ProgressMetric.BMI -> filteredRecords.map { rec ->
                ChartDataPoint(
                    timestampMillis = rec.timestampMillis,
                    value = rec.bmi,
                    label = "${String.format(Locale.US, "%.1f", rec.bmi)} BMI"
                )
            }
            ProgressMetric.WEIGHT -> filteredRecords.map { rec ->
                val displayW = if (unitSystem == UnitSystem.METRIC) {
                    rec.weightKg
                } else {
                    HealthCalculator.kgToLbs(rec.weightKg)
                }
                ChartDataPoint(
                    timestampMillis = rec.timestampMillis,
                    value = displayW,
                    label = HealthFormatters.formatWeight(rec.weightKg, unitSystem)
                )
            }
            ProgressMetric.WAIST_RATIO -> filteredRecords.mapNotNull { rec ->
                rec.waistToHeightRatio?.let { ratio ->
                    ChartDataPoint(
                        timestampMillis = rec.timestampMillis,
                        value = ratio,
                        label = "${String.format(Locale.US, "%.2f", ratio)} WHtR"
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.progress_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.progress_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = { showGoalDialog = true },
                    modifier = Modifier.testTag("edit_goal_button")
                ) {
                    Text(stringResource(R.string.action_set_goal))
                }
            }

            if (state.records.isEmpty()) {
                EmptyHealthStateCard(
                    title = stringResource(R.string.empty_progress_title),
                    body = stringResource(R.string.empty_progress_body),
                    actionLabel = stringResource(R.string.action_log_first),
                    onActionClick = onNavigateToCheckup
                )
            } else {
                val latest = state.records.first()
                val baseline = filteredRecords.firstOrNull() ?: state.records.last()
                val bmiDelta = HealthCalculator.round1(latest.bmi - baseline.bmi)
                val weightDelta = HealthCalculator.round1(latest.weightKg - baseline.weightKg)

                // KPI Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProgressKpiCard(
                        title = "Latest BMI",
                        value = String.format(Locale.US, "%.1f", latest.bmi),
                        subtitle = formatSigned(bmiDelta, ""),
                        accent = latest.bmiCategory.accentColor(),
                        modifier = Modifier.weight(1f)
                    )
                    ProgressKpiCard(
                        title = "Weight",
                        value = HealthFormatters.formatWeightNumber(latest.weightKg, unitSystem),
                        subtitle = HealthFormatters.formatWeightDelta(weightDelta, unitSystem),
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    ProgressKpiCard(
                        title = "Checkups",
                        value = state.records.size.toString(),
                        subtitle = "${filteredRecords.size} in view",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Metric Segmented Toggle
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("progress_metric_toggle")
                ) {
                    ProgressMetric.entries.forEachIndexed { index, metric ->
                        SegmentedButton(
                            selected = state.progressMetric == metric,
                            onClick = { onMetricChange(metric) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ProgressMetric.entries.size
                            )
                        ) {
                            Text(
                                text = metric.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Time Range Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressTimeRange.entries.forEach { range ->
                        FilterChip(
                            selected = state.progressTimeRange == range,
                            onClick = { onTimeRangeChange(range) },
                            label = { Text(range.label) },
                            modifier = Modifier.testTag("range_chip_${range.label}")
                        )
                    }
                }

                if (chartPoints.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "No ${state.progressMetric.label} data in this window",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add a waist measurement in your Physical Health Check or choose ALL time range.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    ProgressTrendChart(
                        points = chartPoints,
                        metric = state.progressMetric,
                        unitSystem = unitSystem,
                        targetWeightKg = targetWeightKg
                    )
                }

                // Target Weight Progress Card
                val remainingKg = HealthCalculator.round1(latest.weightKg - targetWeightKg)
                val firstWeight = state.records.last().weightKg
                val totalGoalSpan = abs(firstWeight - targetWeightKg).coerceAtLeast(1.0)
                val achievedFraction = (1f - (abs(latest.weightKg - targetWeightKg) / totalGoalSpan).toFloat())
                    .coerceIn(0.05f, 1f)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Goal Weight Progress",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Target: ${HealthFormatters.formatWeight(targetWeightKg, unitSystem)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = StatusEmerald
                            )
                        }

                        LinearProgressIndicator(
                            progress = { if (abs(remainingKg) < 0.3) 1f else achievedFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = StatusEmerald,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )

                        val goalCopy = when {
                            abs(remainingKg) < 0.3 -> "You have reached your target weight!"
                            remainingKg > 0 -> "${HealthFormatters.formatWeight(remainingKg, unitSystem)} above target"
                            else -> "${HealthFormatters.formatWeight(abs(remainingKg), unitSystem)} below target"
                        }
                        Text(
                            text = goalCopy,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Latest Physical Check Snapshot
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Latest Physical Snapshot",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        SnapshotRow(
                            label = "BMI Classification",
                            value = "${String.format(Locale.US, "%.1f", latest.bmi)} • ${latest.bmiCategory.label}"
                        )
                        latest.waistToHeightRatio?.let { ratio ->
                            val risk = HealthCalculator.classifyWaistRisk(ratio)
                            SnapshotRow(
                                label = "Waist-to-Height Ratio",
                                value = "${String.format(Locale.US, "%.2f", ratio)} • ${risk?.label ?: ""}"
                            )
                        }
                        if (latest.systolicBp != null && latest.diastolicBp != null) {
                            val bpCat = HealthCalculator.classifyBloodPressure(latest.systolicBp, latest.diastolicBp)
                            SnapshotRow(
                                label = "Blood Pressure",
                                value = "${latest.systolicBp}/${latest.diastolicBp} mmHg • ${bpCat?.label ?: ""}"
                            )
                        }
                        latest.restingHeartRate?.let { hr ->
                            SnapshotRow(
                                label = "Resting Heart Rate",
                                value = "$hr bpm"
                            )
                        }
                        latest.bmrKcal?.let { bmr ->
                            SnapshotRow(
                                label = "Basal Metabolic Rate",
                                value = "$bmr kcal/day"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        val displayGoal = if (unitSystem == UnitSystem.METRIC) {
            targetWeightKg
        } else {
            HealthCalculator.kgToLbs(targetWeightKg)
        }
        val suffix = if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"
        NumericEditDialog(
            title = "Set Goal Weight ($suffix)",
            initialValue = String.format(Locale.US, "%.1f", displayGoal),
            suffix = suffix,
            onDismiss = { showGoalDialog = false },
            onConfirm = { input ->
                val parsed = input.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    val kg = if (unitSystem == UnitSystem.METRIC) {
                        parsed
                    } else {
                        HealthCalculator.lbsToKg(parsed)
                    }
                    onTargetWeightChange(kg)
                }
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun ProgressKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}

@Composable
private fun SnapshotRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatSigned(value: Double, suffix: String): String {
    val sign = if (value > 0) "+" else ""
    return String.format(Locale.US, "%s%.1f%s", sign, value, suffix)
}
