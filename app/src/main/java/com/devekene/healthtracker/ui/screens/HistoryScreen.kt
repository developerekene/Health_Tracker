package com.devekene.healthtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.BmiCategory
import com.devekene.healthtracker.core.model.HealthRecord
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.domain.HealthCalculator
import com.devekene.healthtracker.ui.components.EmptyHealthStateCard
import com.devekene.healthtracker.ui.components.HealthFormatters
import com.devekene.healthtracker.ui.theme.accentColor
import com.devekene.healthtracker.ui.viewmodel.HealthTrackerUiState
import com.example.R
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HealthTrackerUiState,
    contentPadding: PaddingValues,
    onFilterChange: (BmiCategory?) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onNavigateToCheckup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unitSystem = state.preferences.unitSystem
    var selectedRecord by remember { mutableStateOf<HealthRecord?>(null) }
    var recordPendingDelete by remember { mutableStateOf<HealthRecord?>(null) }

    val filteredRecords = remember(state.records, state.historyCategoryFilter) {
        val filter = state.historyCategoryFilter
        if (filter == null) {
            state.records
        } else {
            state.records.filter { it.bmiCategory == filter }
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${state.records.size} encrypted local entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = state.historyCategoryFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("All") }
                )
                listOf(
                    BmiCategory.UNDERWEIGHT,
                    BmiCategory.NORMAL,
                    BmiCategory.OVERWEIGHT,
                    BmiCategory.OBESE_I
                ).forEach { cat ->
                    FilterChip(
                        selected = state.historyCategoryFilter == cat,
                        onClick = {
                            onFilterChange(if (state.historyCategoryFilter == cat) null else cat)
                        },
                        label = { Text(cat.shortLabel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredRecords.isEmpty()) {
                EmptyHealthStateCard(
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                    actionLabel = if (state.records.isEmpty()) {
                        stringResource(R.string.action_log_first)
                    } else null,
                    onActionClick = if (state.records.isEmpty()) onNavigateToCheckup else null
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("history_records_list")
                ) {
                    items(
                        items = filteredRecords,
                        key = { it.id }
                    ) { record ->
                        HealthHistoryItemCard(
                            record = record,
                            unitSystem = unitSystem,
                            onClick = { selectedRecord = record },
                            onRequestDelete = { recordPendingDelete = record }
                        )
                    }
                }
            }
        }
    }

    selectedRecord?.let { rec ->
        ModalBottomSheet(
            onDismissRequest = { selectedRecord = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            RecordDetailSheetContent(
                record = rec,
                unitSystem = unitSystem,
                onDelete = {
                    selectedRecord = null
                    recordPendingDelete = rec
                }
            )
        }
    }

    recordPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecord(pending.id)
                        recordPendingDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthHistoryItemCard(
    record: HealthRecord,
    unitSystem: UnitSystem,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val accent = record.bmiCategory.accentColor()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("history_item_${record.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .background(accent, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HealthFormatters.formatDateTime(record.timestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", record.bmi),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.16f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = record.bmiCategory.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Text(
                        text = HealthFormatters.formatWeight(record.weightKg, unitSystem),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val chips = buildList {
                    record.waistToHeightRatio?.let { add("WHtR ${String.format(Locale.US, "%.2f", it)}") }
                    if (record.systolicBp != null && record.diastolicBp != null) {
                        add("BP ${record.systolicBp}/${record.diastolicBp}")
                    }
                    record.restingHeartRate?.let { add("$it bpm") }
                }
                if (chips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chips.forEach { chip ->
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onRequestDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_record_${record.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete health record",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecordDetailSheetContent(
    record: HealthRecord,
    unitSystem: UnitSystem,
    onDelete: () -> Unit
) {
    val accent = record.bmiCategory.accentColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = HealthFormatters.formatDateTime(record.timestampMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "BMI ${String.format(Locale.US, "%.1f", record.bmi)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = record.bmiCategory.label,
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        DetailLine("Weight", HealthFormatters.formatWeight(record.weightKg, unitSystem))
        DetailLine("Height", HealthFormatters.formatHeight(record.heightCm, unitSystem))
        record.waistCm?.let { w ->
            val waistStr = if (unitSystem == UnitSystem.METRIC) {
                String.format(Locale.US, "%.1f cm", w)
            } else {
                String.format(Locale.US, "%.1f in", HealthCalculator.cmToInches(w))
            }
            DetailLine("Waist", waistStr)
        }
        record.waistToHeightRatio?.let { ratio ->
            val risk = HealthCalculator.classifyWaistRisk(ratio)
            DetailLine("Waist-to-Height", "${String.format(Locale.US, "%.2f", ratio)} (${risk?.label ?: ""})")
        }
        if (record.systolicBp != null && record.diastolicBp != null) {
            val bpCat = HealthCalculator.classifyBloodPressure(record.systolicBp, record.diastolicBp)
            DetailLine("Blood Pressure", "${record.systolicBp}/${record.diastolicBp} mmHg (${bpCat?.label ?: ""})")
        }
        record.restingHeartRate?.let { DetailLine("Resting Heart Rate", "$it bpm") }
        record.bmrKcal?.let { DetailLine("Estimated BMR", "$it kcal/day") }
        record.bodyFatEstimatePct?.let { DetailLine("Est. Body Fat", String.format(Locale.US, "%.1f%%", it)) }

        if (record.notes.isNotBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "Notes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = record.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete Record",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
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
