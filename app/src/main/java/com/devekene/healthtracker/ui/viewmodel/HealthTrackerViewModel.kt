package com.devekene.healthtracker.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.devekene.healthtracker.core.model.BiologicalSex
import com.devekene.healthtracker.core.model.BmiCategory
import com.devekene.healthtracker.core.model.HealthCheckSummary
import com.devekene.healthtracker.core.model.HealthRecord
import com.devekene.healthtracker.core.model.ThemeMode
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.core.model.UserPreferences
import com.devekene.healthtracker.data.preferences.PreferencesRepository
import com.devekene.healthtracker.data.repository.HealthRepository
import com.devekene.healthtracker.domain.HealthCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProgressTimeRange(val label: String, val days: Int?) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    QUARTER("90D", 90),
    ALL("ALL", null)
}

enum class ProgressMetric(val label: String) {
    BMI("BMI"),
    WEIGHT("Weight"),
    WAIST_RATIO("Waist Ratio")
}

enum class ExportFormat(val label: String) {
    JSON("JSON"),
    CSV("CSV")
}

data class LastExportInfo(
    val format: ExportFormat,
    val recordCount: Int,
    val byteSize: Int,
    val timestampMillis: Long
)

data class CheckupDraftState(
    val weightKg: Double = 72.0,
    val heightCm: Double = 175.0,
    val waistCmInput: String = "",
    val systolicInput: String = "",
    val diastolicInput: String = "",
    val heartRateInput: String = "",
    val ageInput: String = "30",
    val biologicalSex: BiologicalSex = BiologicalSex.UNSPECIFIED,
    val notes: String = "",
    val isPhysicalCheckExpanded: Boolean = false,
    val isSaving: Boolean = false
) {
    val parsedWaistCm: Double?
        get() = waistCmInput.toDoubleOrNull()?.takeIf { it in 30.0..250.0 }

    val parsedSystolic: Int?
        get() = systolicInput.toIntOrNull()?.takeIf { it in 60..260 }

    val parsedDiastolic: Int?
        get() = diastolicInput.toIntOrNull()?.takeIf { it in 30..160 }

    val parsedHeartRate: Int?
        get() = heartRateInput.toIntOrNull()?.takeIf { it in 30..220 }

    val parsedAge: Int?
        get() = ageInput.toIntOrNull()?.takeIf { it in 5..120 }

    fun summary(): HealthCheckSummary = HealthCalculator.summarize(
        weightKg = weightKg,
        heightCm = heightCm,
        waistCm = parsedWaistCm,
        systolicBp = parsedSystolic,
        diastolicBp = parsedDiastolic,
        restingHeartRate = parsedHeartRate,
        ageYears = parsedAge,
        sex = biologicalSex
    )
}

data class HealthTrackerUiState(
    val preferences: UserPreferences = UserPreferences(),
    val records: List<HealthRecord> = emptyList(),
    val draft: CheckupDraftState = CheckupDraftState(),
    val liveSummary: HealthCheckSummary = CheckupDraftState().summary(),
    val progressTimeRange: ProgressTimeRange = ProgressTimeRange.MONTH,
    val progressMetric: ProgressMetric = ProgressMetric.BMI,
    val historyCategoryFilter: BmiCategory? = null,
    val lastExportInfo: LastExportInfo? = null,
    val isExporting: Boolean = false
)

sealed interface HealthUiEvent {
    data object RecordSaved : HealthUiEvent
    data class ExportSuccess(val format: ExportFormat, val recordCount: Int) : HealthUiEvent
    data object ExportEmpty : HealthUiEvent
    data class Error(val message: String) : HealthUiEvent
}

class HealthTrackerViewModel(
    private val repository: HealthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val draftFlow = MutableStateFlow(CheckupDraftState())
    private val progressRangeFlow = MutableStateFlow(ProgressTimeRange.MONTH)
    private val progressMetricFlow = MutableStateFlow(ProgressMetric.BMI)
    private val historyFilterFlow = MutableStateFlow<BmiCategory?>(null)
    private val exportStateFlow = MutableStateFlow<Pair<Boolean, LastExportInfo?>>(false to null)

    private val _events = MutableSharedFlow<HealthUiEvent>()
    val events: SharedFlow<HealthUiEvent> = _events.asSharedFlow()

    private data class FilterAndExport(
        val range: ProgressTimeRange,
        val metric: ProgressMetric,
        val historyFilter: BmiCategory?,
        val exportState: Pair<Boolean, LastExportInfo?>
    )

    private val filterAndExportFlow = combine(
        progressRangeFlow,
        progressMetricFlow,
        historyFilterFlow,
        exportStateFlow
    ) { range, metric, historyFilter, exportState ->
        FilterAndExport(range, metric, historyFilter, exportState)
    }

    val uiState: StateFlow<HealthTrackerUiState> = combine(
        preferencesRepository.preferencesFlow,
        repository.recordsFlow,
        draftFlow,
        filterAndExportFlow
    ) { prefs, records, draft, extra ->
        HealthTrackerUiState(
            preferences = prefs,
            records = records,
            draft = draft,
            liveSummary = draft.summary(),
            progressTimeRange = extra.range,
            progressMetric = extra.metric,
            historyCategoryFilter = extra.historyFilter,
            isExporting = extra.exportState.first,
            lastExportInfo = extra.exportState.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HealthTrackerUiState()
    )

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val latest = repository.latestRecordFlow.first()
            draftFlow.update { current ->
                current.copy(
                    heightCm = latest?.heightCm ?: prefs.defaultHeightCm,
                    weightKg = latest?.weightKg ?: current.weightKg,
                    ageInput = (latest?.ageYears ?: prefs.defaultAgeYears).toString(),
                    biologicalSex = latest?.biologicalSex ?: prefs.defaultSex
                )
            }
        }
    }

    fun updateWeightKg(weightKg: Double) {
        val clamped = HealthCalculator.round1(weightKg.coerceIn(20.0, 350.0))
        draftFlow.update { it.copy(weightKg = clamped) }
    }

    fun adjustWeightKg(deltaKg: Double) {
        draftFlow.update { current ->
            val next = HealthCalculator.round1((current.weightKg + deltaKg).coerceIn(20.0, 350.0))
            current.copy(weightKg = next)
        }
    }

    fun updateHeightCm(heightCm: Double) {
        val clamped = HealthCalculator.round1(heightCm.coerceIn(80.0, 250.0))
        draftFlow.update { it.copy(heightCm = clamped) }
    }

    fun adjustHeightCm(deltaCm: Double) {
        draftFlow.update { current ->
            val next = HealthCalculator.round1((current.heightCm + deltaCm).coerceIn(80.0, 250.0))
            current.copy(heightCm = next)
        }
    }

    fun updateWaistInput(value: String) {
        draftFlow.update { it.copy(waistCmInput = value) }
    }

    fun updateWaistFromDisplayUnit(displayValue: String, unitSystem: UnitSystem) {
        if (displayValue.isBlank()) {
            draftFlow.update { it.copy(waistCmInput = "") }
            return
        }
        val num = displayValue.toDoubleOrNull()
        if (num == null) {
            draftFlow.update { it.copy(waistCmInput = displayValue) }
            return
        }
        val cm = if (unitSystem == UnitSystem.IMPERIAL) {
            HealthCalculator.round1(HealthCalculator.inchesToCm(num))
        } else {
            HealthCalculator.round1(num)
        }
        draftFlow.update { it.copy(waistCmInput = cm.toString()) }
    }

    fun updateSystolicInput(value: String) {
        draftFlow.update { it.copy(systolicInput = value.filter { ch -> ch.isDigit() }.take(3)) }
    }

    fun updateDiastolicInput(value: String) {
        draftFlow.update { it.copy(diastolicInput = value.filter { ch -> ch.isDigit() }.take(3)) }
    }

    fun updateHeartRateInput(value: String) {
        draftFlow.update { it.copy(heartRateInput = value.filter { ch -> ch.isDigit() }.take(3)) }
    }

    fun updateAgeInput(value: String) {
        draftFlow.update { it.copy(ageInput = value.filter { ch -> ch.isDigit() }.take(3)) }
    }

    fun updateBiologicalSex(sex: BiologicalSex) {
        draftFlow.update { it.copy(biologicalSex = sex) }
    }

    fun updateNotes(notes: String) {
        draftFlow.update { it.copy(notes = notes.take(280)) }
    }

    fun togglePhysicalCheckExpanded() {
        draftFlow.update { it.copy(isPhysicalCheckExpanded = !it.isPhysicalCheckExpanded) }
    }

    fun saveCurrentCheckup() {
        val currentDraft = draftFlow.value
        if (currentDraft.isSaving) return

        val summary = currentDraft.summary()
        if (summary.bmi <= 0.0) {
            viewModelScope.launch {
                _events.emit(HealthUiEvent.Error("Enter valid weight and height values."))
            }
            return
        }

        viewModelScope.launch {
            draftFlow.update { it.copy(isSaving = true) }
            try {
                val record = HealthRecord(
                    timestampMillis = System.currentTimeMillis(),
                    weightKg = currentDraft.weightKg,
                    heightCm = currentDraft.heightCm,
                    waistCm = currentDraft.parsedWaistCm,
                    systolicBp = currentDraft.parsedSystolic,
                    diastolicBp = currentDraft.parsedDiastolic,
                    restingHeartRate = currentDraft.parsedHeartRate,
                    ageYears = currentDraft.parsedAge,
                    biologicalSex = currentDraft.biologicalSex,
                    bmi = summary.bmi,
                    bmiCategory = summary.bmiCategory,
                    waistToHeightRatio = summary.waistToHeightRatio,
                    bmrKcal = summary.bmrKcal,
                    bodyFatEstimatePct = summary.bodyFatEstimatePct,
                    notes = currentDraft.notes.trim()
                )
                repository.saveRecord(record)
                preferencesRepository.setDefaultHeightCm(currentDraft.heightCm)
                currentDraft.parsedAge?.let { age ->
                    preferencesRepository.setDefaultProfile(age, currentDraft.biologicalSex)
                }
                draftFlow.update {
                    it.copy(
                        notes = "",
                        isSaving = false
                    )
                }
                _events.emit(HealthUiEvent.RecordSaved)
            } catch (e: Exception) {
                draftFlow.update { it.copy(isSaving = false) }
                _events.emit(HealthUiEvent.Error(e.message ?: "Failed to save health record"))
            }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAllRecords()
        }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch {
            preferencesRepository.setUnitSystem(unitSystem)
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setTargetWeightKg(targetWeightKg: Double) {
        viewModelScope.launch {
            preferencesRepository.setTargetWeightKg(targetWeightKg.coerceIn(30.0, 250.0))
        }
    }

    fun setProgressTimeRange(range: ProgressTimeRange) {
        progressRangeFlow.value = range
    }

    fun setProgressMetric(metric: ProgressMetric) {
        progressMetricFlow.value = metric
    }

    fun setHistoryFilter(category: BmiCategory?) {
        historyFilterFlow.value = category
    }

    fun exportBackup(
        format: ExportFormat,
        contentResolver: ContentResolver,
        destinationUri: Uri
    ) {
        viewModelScope.launch {
            exportStateFlow.update { true to it.second }
            try {
                val unitSystem = uiState.value.preferences.unitSystem
                val (count, content) = when (format) {
                    ExportFormat.JSON -> repository.buildJsonBackup(unitSystem)
                    ExportFormat.CSV -> repository.buildCsvBackup()
                }
                if (count == 0) {
                    exportStateFlow.update { false to it.second }
                    _events.emit(HealthUiEvent.ExportEmpty)
                    return@launch
                }

                val bytes = content.toByteArray(Charsets.UTF_8)
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(destinationUri)?.use { stream ->
                        stream.write(bytes)
                        stream.flush()
                    } ?: error("Unable to open destination file stream")
                }

                val info = LastExportInfo(
                    format = format,
                    recordCount = count,
                    byteSize = bytes.size,
                    timestampMillis = System.currentTimeMillis()
                )
                exportStateFlow.value = false to info
                _events.emit(HealthUiEvent.ExportSuccess(format, count))
            } catch (e: Exception) {
                exportStateFlow.update { false to it.second }
                _events.emit(HealthUiEvent.Error(e.message ?: "Failed to write backup file"))
            }
        }
    }

    class Factory(
        private val repository: HealthRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HealthTrackerViewModel(repository, preferencesRepository) as T
        }
    }
}
