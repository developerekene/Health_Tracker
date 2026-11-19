package com.devekene.healthtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.domain.HealthCalculator
import com.example.R
import java.util.Locale

@Composable
fun WeightInputCard(
    weightKg: Double,
    unitSystem: UnitSystem,
    onWeightKgChange: (Double) -> Unit,
    onAdjustKg: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDirectInput by remember { mutableStateOf(false) }

    val displayValue = when (unitSystem) {
        UnitSystem.METRIC -> weightKg
        UnitSystem.IMPERIAL -> HealthCalculator.kgToLbs(weightKg)
    }
    val unitLabel = if (unitSystem == UnitSystem.METRIC) {
        stringResource(R.string.unit_kg)
    } else {
        stringResource(R.string.unit_lbs)
    }
    val stepKg = if (unitSystem == UnitSystem.METRIC) 0.5 else 0.45359237
    val sliderRange = if (unitSystem == UnitSystem.METRIC) 35f..180f else 77f..397f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.label_weight),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", displayValue)} $unitLabel",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable { showDirectInput = true }
                            .testTag("weight_value_button")
                    )
                    Text(
                        text = "Tap value to type exact",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { onAdjustKg(-stepKg) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("weight_decrement_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease weight"
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { onAdjustKg(stepKg) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("weight_increment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase weight"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = displayValue.toFloat().coerceIn(sliderRange.start, sliderRange.endInclusive),
                onValueChange = { raw ->
                    val newKg = if (unitSystem == UnitSystem.METRIC) {
                        raw.toDouble()
                    } else {
                        HealthCalculator.lbsToKg(raw.toDouble())
                    }
                    onWeightKgChange(newKg)
                },
                valueRange = sliderRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.testTag("weight_slider")
            )
        }
    }

    if (showDirectInput) {
        NumericEditDialog(
            title = "Enter Weight ($unitLabel)",
            initialValue = String.format(Locale.US, "%.1f", displayValue),
            suffix = unitLabel,
            onDismiss = { showDirectInput = false },
            onConfirm = { input ->
                val parsed = input.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    val kg = if (unitSystem == UnitSystem.METRIC) parsed else HealthCalculator.lbsToKg(parsed)
                    onWeightKgChange(kg)
                }
                showDirectInput = false
            }
        )
    }
}

@Composable
fun HeightInputCard(
    heightCm: Double,
    unitSystem: UnitSystem,
    onHeightCmChange: (Double) -> Unit,
    onAdjustCm: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDirectInput by remember { mutableStateOf(false) }

    val formattedHeight = HealthFormatters.formatHeight(heightCm, unitSystem)
    val stepCm = if (unitSystem == UnitSystem.METRIC) 1.0 else 2.54

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.label_height),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedHeight,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clickable { showDirectInput = true }
                            .testTag("height_value_button")
                    )
                    Text(
                        text = "Tap value to type exact",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { onAdjustCm(-stepCm) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("height_decrement_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease height"
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { onAdjustCm(stepCm) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("height_increment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase height"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = heightCm.toFloat().coerceIn(120f, 220f),
                onValueChange = { onHeightCmChange(it.toDouble()) },
                valueRange = 120f..220f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                modifier = Modifier.testTag("height_slider")
            )
        }
    }

    if (showDirectInput) {
        if (unitSystem == UnitSystem.METRIC) {
            NumericEditDialog(
                title = "Enter Height (cm)",
                initialValue = String.format(Locale.US, "%.0f", heightCm),
                suffix = "cm",
                onDismiss = { showDirectInput = false },
                onConfirm = { input ->
                    val parsed = input.toDoubleOrNull()
                    if (parsed != null && parsed in 80.0..250.0) {
                        onHeightCmChange(parsed)
                    }
                    showDirectInput = false
                }
            )
        } else {
            val (initFt, initIn) = HealthCalculator.cmToFeetAndInches(heightCm)
            FeetInchesEditDialog(
                initialFeet = initFt,
                initialInches = initIn,
                onDismiss = { showDirectInput = false },
                onConfirm = { ft, inches ->
                    onHeightCmChange(HealthCalculator.feetAndInchesToCm(ft, inches))
                    showDirectInput = false
                }
            )
        }
    }
}

@Composable
fun NumericEditDialog(
    title: String,
    initialValue: String,
    suffix: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                suffix = { Text(suffix) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("numeric_dialog_input")
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun FeetInchesEditDialog(
    initialFeet: Int,
    initialInches: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var feetText by remember(initialFeet) { mutableStateOf(initialFeet.toString()) }
    var inchesText by remember(initialInches) { mutableStateOf(initialInches.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Height (ft / in)", style = MaterialTheme.typography.titleMedium) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = feetText,
                    onValueChange = { feetText = it.filter { ch -> ch.isDigit() }.take(1) },
                    label = { Text("Feet") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = inchesText,
                    onValueChange = { inchesText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Inches") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ft = (feetText.toIntOrNull() ?: initialFeet).coerceIn(3, 8)
                    val inches = (inchesText.toIntOrNull() ?: initialInches).coerceIn(0, 11)
                    onConfirm(ft, inches)
                }
            ) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
