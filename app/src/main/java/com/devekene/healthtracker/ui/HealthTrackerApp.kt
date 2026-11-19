package com.devekene.healthtracker.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devekene.healthtracker.data.local.HealthTrackerDatabase
import com.devekene.healthtracker.data.preferences.PreferencesRepository
import com.devekene.healthtracker.data.repository.HealthRepositoryImpl
import com.devekene.healthtracker.ui.navigation.HealthDestination
import com.devekene.healthtracker.ui.screens.BackupSettingsScreen
import com.devekene.healthtracker.ui.screens.CheckupScreen
import com.devekene.healthtracker.ui.screens.HistoryScreen
import com.devekene.healthtracker.ui.screens.ProgressScreen
import com.devekene.healthtracker.ui.theme.HealthTrackerTheme
import com.devekene.healthtracker.ui.viewmodel.ExportFormat
import com.devekene.healthtracker.ui.viewmodel.HealthTrackerViewModel
import com.devekene.healthtracker.ui.viewmodel.HealthUiEvent
import com.example.R

private data class NavTabItem(
    val destination: HealthDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_TABS = listOf(
    NavTabItem(
        destination = HealthDestination.Checkup,
        labelRes = R.string.nav_checkup,
        selectedIcon = Icons.Filled.MonitorHeart,
        unselectedIcon = Icons.Outlined.MonitorHeart,
        testTag = "nav_tab_checkup"
    ),
    NavTabItem(
        destination = HealthDestination.Progress,
        labelRes = R.string.nav_progress,
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart,
        testTag = "nav_tab_progress"
    ),
    NavTabItem(
        destination = HealthDestination.History,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_history"
    ),
    NavTabItem(
        destination = HealthDestination.Backup,
        labelRes = R.string.nav_backup,
        selectedIcon = Icons.Filled.Backup,
        unselectedIcon = Icons.Outlined.Backup,
        testTag = "nav_tab_backup"
    )
)

@Composable
fun HealthTrackerApp() {
    val context = LocalContext.current
    val viewModelFactory = remember(context.applicationContext) {
        val db = HealthTrackerDatabase.getInstance(context.applicationContext)
        val repository = HealthRepositoryImpl(db.healthRecordDao())
        val prefsRepository = PreferencesRepository(context.applicationContext)
        HealthTrackerViewModel.Factory(repository, prefsRepository)
    }

    val viewModel: HealthTrackerViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val savedMsg = stringResource(R.string.msg_record_saved)
    val emptyExportMsg = stringResource(R.string.msg_export_empty)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                HealthUiEvent.RecordSaved -> savedMsg
                is HealthUiEvent.ExportSuccess ->
                    "${event.format.label} backup exported (${event.recordCount} records)"
                HealthUiEvent.ExportEmpty -> emptyExportMsg
                is HealthUiEvent.Error -> event.message
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    HealthTrackerTheme(themeMode = uiState.preferences.themeMode) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        fun navigateTo(dest: HealthDestination) {
            navController.navigate(dest) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NAV_TABS.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.destination::class) == true
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Spacer(modifier = Modifier.weight(0.2f))
                            NAV_TABS.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.destination::class) == true
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    val contentPadding = if (useNavRail) {
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding()
                        )
                    } else {
                        innerPadding
                    }

                    NavHost(
                        navController = navController,
                        startDestination = HealthDestination.Checkup,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<HealthDestination.Checkup> {
                            CheckupScreen(
                                state = uiState,
                                contentPadding = contentPadding,
                                onUnitSystemChange = viewModel::setUnitSystem,
                                onWeightKgChange = viewModel::updateWeightKg,
                                onAdjustWeightKg = viewModel::adjustWeightKg,
                                onHeightCmChange = viewModel::updateHeightCm,
                                onAdjustHeightCm = viewModel::adjustHeightCm,
                                onWaistDisplayChange = { input ->
                                    viewModel.updateWaistFromDisplayUnit(
                                        input,
                                        uiState.preferences.unitSystem
                                    )
                                },
                                onSystolicChange = viewModel::updateSystolicInput,
                                onDiastolicChange = viewModel::updateDiastolicInput,
                                onHeartRateChange = viewModel::updateHeartRateInput,
                                onAgeChange = viewModel::updateAgeInput,
                                onSexChange = viewModel::updateBiologicalSex,
                                onNotesChange = viewModel::updateNotes,
                                onTogglePhysicalCheck = viewModel::togglePhysicalCheckExpanded,
                                onSaveCheckup = viewModel::saveCurrentCheckup
                            )
                        }

                        composable<HealthDestination.Progress> {
                            ProgressScreen(
                                state = uiState,
                                contentPadding = contentPadding,
                                onTimeRangeChange = viewModel::setProgressTimeRange,
                                onMetricChange = viewModel::setProgressMetric,
                                onTargetWeightChange = viewModel::setTargetWeightKg,
                                onNavigateToCheckup = { navigateTo(HealthDestination.Checkup) }
                            )
                        }

                        composable<HealthDestination.History> {
                            HistoryScreen(
                                state = uiState,
                                contentPadding = contentPadding,
                                onFilterChange = viewModel::setHistoryFilter,
                                onDeleteRecord = viewModel::deleteRecord,
                                onNavigateToCheckup = { navigateTo(HealthDestination.Checkup) }
                            )
                        }

                        composable<HealthDestination.Backup> {
                            BackupSettingsScreen(
                                state = uiState,
                                contentPadding = contentPadding,
                                onExportJson = { uri ->
                                    viewModel.exportBackup(
                                        ExportFormat.JSON,
                                        context.contentResolver,
                                        uri
                                    )
                                },
                                onExportCsv = { uri ->
                                    viewModel.exportBackup(
                                        ExportFormat.CSV,
                                        context.contentResolver,
                                        uri
                                    )
                                },
                                onThemeModeChange = viewModel::setThemeMode,
                                onUnitSystemChange = viewModel::setUnitSystem,
                                onTargetWeightChange = viewModel::setTargetWeightKg,
                                onClearAllRecords = viewModel::clearAllRecords
                            )
                        }
                    }
                }
            }
        }
    }
}
