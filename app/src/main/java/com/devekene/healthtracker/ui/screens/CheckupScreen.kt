package com.devekene.healthtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.BiologicalSex
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.domain.HealthCalculator
import com.devekene.healthtracker.ui.components.BmiGaugeCard
import com.devekene.healthtracker.ui.components.HeightInputCard
import com.devekene.healthtracker.ui.components.WeightInputCard
import com.devekene.healthtracker.ui.theme.accentColor
import com.devekene.healthtracker.ui.viewmodel.HealthTrackerUiState
import com.example.R
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckupScreen(
    state: HealthTrackerUiState,
    contentPadding: PaddingValues,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onWeightKgChange: (Double) -> Unit,
    onAdjustWeightKg: (Double) -> Unit,
    onHeightCmChange: (Double) -> Unit,
    onAdjustHeightCm: (Double) -> Unit,
    onWaistDisplayChange: (String) -> Unit,
    onSystolicChange: (String) -> Unit,
    onDiastolicChange: (String) -> Unit,
    onHeartRateChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onSexChange: (BiologicalSex) -> Unit,
    onNotesChange: (String) -> Unit,
    onTogglePhysicalCheck: () -> Unit,
    onSaveCheckup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unitSystem = state.preferences.unitSystem
    val draft = state.draft
    val summary = state.liveSummary

    var waistText by remember(unitSystem, draft.waistCmInput) {
        val cm = draft.waistCmInput.toDoubleOrNull()
        val initial = when {
            draft.waistCmInput.isBlank() -> ""
            cm != null && unitSystem == UnitSystem.IMPERIAL ->
                String.format(Locale.US, "%.1f", HealthCalculator.cmToInches(cm))
            cm != null -> String.format(Locale.US, "%.1f", cm)
            else -> draft.waistCmInput
        }
        mutableStateOf(initial)
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
            // Minimalist Header + Unit Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted local storage",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.badge_offline_encrypted),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.testTag("unit_system_toggle")
                ) {
                    UnitSystem.entries.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = unitSystem == unit,
                            onClick = { onUnitSystemChange(unit) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = UnitSystem.entries.size
                            )
                        ) {
                            Text(
                                text = if (unit == UnitSystem.METRIC) "kg/cm" else "lb/ft",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Live BMI Gauge Card
            BmiGaugeCard(
                summary = summary,
                unitSystem = unitSystem
            )

            // Weight & Height Cards
            WeightInputCard(
                weightKg = draft.weightKg,
                unitSystem = unitSystem,
                onWeightKgChange = onWeightKgChange,
                onAdjustKg = onAdjustWeightKg
            )

            HeightInputCard(
                heightCm = draft.heightCm,
                unitSystem = unitSystem,
                onHeightCmChange = onHeightCmChange,
                onAdjustCm = onAdjustHeightCm
            )

            // Expandable Physical Health Check Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(onClick = onTogglePhysicalCheck)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                            .testTag("physical_check_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorHeart,
                                contentDescription = "Physical health vitals",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.section_physical_check),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Waist ratio, blood pressure, heart rate & BMR",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (draft.isPhysicalCheckExpanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = if (draft.isPhysicalCheckExpanded) {
                                "Collapse physical check"
                            } else {
                                "Expand physical check"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = draft.isPhysicalCheckExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Waist circumference + live Waist-to-Height Ratio
                            val waistSuffix = if (unitSystem == UnitSystem.METRIC) "cm" else "in"
                            OutlinedTextField(
                                value = waistText,
                                onValueChange = { next ->
                                    waistText = next
                                    onWaistDisplayChange(next)
                                },
                                label = { Text(stringResource(R.string.label_waist)) },
                                suffix = { Text(waistSuffix) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("waist_input")
                            )

                            summary.waistToHeightRatio?.let { ratio ->
                                val risk = summary.waistRiskCategory
                                val color = risk?.accentColor() ?: MaterialTheme.colorScheme.primary
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = color.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Waist-to-Height Ratio",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", ratio)} • ${risk?.label ?: ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = color
                                        )
                                    }
                                }
                            }

                            // Blood Pressure
                            Text(
                                text = stringResource(R.string.label_blood_pressure),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = draft.systolicInput,
                                    onValueChange = onSystolicChange,
                                    label = { Text(stringResource(R.string.label_systolic)) },
                                    placeholder = { Text("120") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("systolic_input")
                                )
                                OutlinedTextField(
                                    value = draft.diastolicInput,
                                    onValueChange = onDiastolicChange,
                                    label = { Text(stringResource(R.string.label_diastolic)) },
                                    placeholder = { Text("80") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("diastolic_input")
                                )
                            }

                            summary.bloodPressureCategory?.let { bpCat ->
                                val color = bpCat.accentColor()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = color.copy(alpha = 0.14f),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Blood Pressure Status",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = bpCat.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = color
                                    )
                                }
                            }

                            // Resting Heart Rate & Age
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = draft.heartRateInput,
                                    onValueChange = onHeartRateChange,
                                    label = { Text(stringResource(R.string.label_heart_rate)) },
                                    placeholder = { Text("64") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("heart_rate_input")
                                )
                                OutlinedTextField(
                                    value = draft.ageInput,
                                    onValueChange = onAgeChange,
                                    label = { Text(stringResource(R.string.label_age)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .testTag("age_input")
                                )
                            }

                            // Sex for BMR / Body Fat Estimate
                            Text(
                                text = stringResource(R.string.label_sex),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BiologicalSex.entries.forEach { sex ->
                                    FilterChip(
                                        selected = draft.biologicalSex == sex,
                                        onClick = { onSexChange(sex) },
                                        label = { Text(sex.label) }
                                    )
                                }
                            }

                            if (summary.bmrKcal != null || summary.bodyFatEstimatePct != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        summary.bmrKcal?.let { bmr ->
                                            Column {
                                                Text(
                                                    text = "Est. BMR",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "$bmr kcal/day",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        summary.bodyFatEstimatePct?.let { bf ->
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Est. Body Fat",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = String.format(Locale.US, "%.1f%%", bf),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = draft.notes,
                                onValueChange = onNotesChange,
                                label = { Text(stringResource(R.string.label_notes)) },
                                placeholder = { Text(stringResource(R.string.hint_notes)) },
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("notes_input")
                            )
                        }
                    }
                }
            }

            // Primary Save Action
            Button(
                onClick = onSaveCheckup,
                enabled = !draft.isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag("save_checkup_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (draft.isSaving) "Saving…" else stringResource(R.string.action_save_checkup),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
