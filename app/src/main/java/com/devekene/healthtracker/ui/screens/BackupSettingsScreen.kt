package com.devekene.healthtracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.ThemeMode
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.domain.HealthCalculator
import com.devekene.healthtracker.ui.components.HealthFormatters
import com.devekene.healthtracker.ui.components.NumericEditDialog
import com.devekene.healthtracker.ui.theme.StatusEmerald
import com.devekene.healthtracker.ui.viewmodel.ExportFormat
import com.devekene.healthtracker.ui.viewmodel.HealthTrackerUiState
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupSettingsScreen(
    state: HealthTrackerUiState,
    contentPadding: PaddingValues,
    onExportJson: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onTargetWeightChange: (Double) -> Unit,
    onClearAllRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showTargetWeightDialog by remember { mutableStateOf(false) }

    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportJson(uri)
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) onExportCsv(uri)
    }

    val timestampTag = remember(state.records.size) {
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
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
            Text(
                text = stringResource(R.string.backup_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Encrypted Local Vault Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = StatusEmerald.copy(alpha = 0.16f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Encrypted vault active",
                                tint = StatusEmerald
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vault_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.vault_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.records.size} encrypted records on device",
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusEmerald
                        )
                    }
                }
            }

            // Local Backup Export Card
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.backup_section_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            jsonLauncher.launch("health_tracker_backup_$timestampTag.json")
                        },
                        enabled = !state.isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("export_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_export_json))
                    }

                    OutlinedButton(
                        onClick = {
                            csvLauncher.launch("health_tracker_records_$timestampTag.csv")
                        },
                        enabled = !state.isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_export_csv))
                    }

                    state.lastExportInfo?.let { info ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = StatusEmerald.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Last ${info.format.label} export (${info.recordCount} records)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusEmerald
                                )
                                Text(
                                    text = HealthFormatters.formatByteSize(info.byteSize),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StatusEmerald
                                )
                            }
                        }
                    }
                }
            }

            // Appearance & Unit Preferences Card
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_appearance_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Interface Theme",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_mode_selector")
                    ) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.preferences.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size
                                )
                            ) {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Text(
                        text = "Measurement Units",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UnitSystem.entries.forEachIndexed { index, unit ->
                            SegmentedButton(
                                selected = state.preferences.unitSystem == unit,
                                onClick = { onUnitSystemChange(unit) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = UnitSystem.entries.size
                                )
                            ) {
                                Text(
                                    text = if (unit == UnitSystem.METRIC) {
                                        stringResource(R.string.unit_metric)
                                    } else {
                                        stringResource(R.string.unit_imperial)
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Goal Weight",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = HealthFormatters.formatWeight(
                                    state.preferences.targetWeightKg,
                                    state.preferences.unitSystem
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(onClick = { showTargetWeightDialog = true }) {
                            Text(stringResource(R.string.action_set_goal))
                        }
                    }
                }
            }

            // Local Data Danger Zone
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
                    Text(
                        text = stringResource(R.string.danger_zone_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        enabled = state.records.isNotEmpty(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("clear_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_clear_all))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllRecords()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showTargetWeightDialog) {
        val unitSystem = state.preferences.unitSystem
        val displayGoal = if (unitSystem == UnitSystem.METRIC) {
            state.preferences.targetWeightKg
        } else {
            HealthCalculator.kgToLbs(state.preferences.targetWeightKg)
        }
        val suffix = if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"
        NumericEditDialog(
            title = "Set Goal Weight ($suffix)",
            initialValue = String.format(Locale.US, "%.1f", displayGoal),
            suffix = suffix,
            onDismiss = { showTargetWeightDialog = false },
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
                showTargetWeightDialog = false
            }
        )
    }
}
