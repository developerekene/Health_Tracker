package com.devekene.healthtracker.domain

import com.devekene.healthtracker.core.model.HealthRecord
import com.devekene.healthtracker.core.model.UnitSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializable
data class HealthBackupRecord(
    val id: Long,
    val timestampMillis: Long,
    val recordedAtIso: String,
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double? = null,
    val systolicBp: Int? = null,
    val diastolicBp: Int? = null,
    val restingHeartRate: Int? = null,
    val ageYears: Int? = null,
    val biologicalSex: String,
    val bmi: Double,
    val bmiCategory: String,
    val waistToHeightRatio: Double? = null,
    val bmrKcal: Int? = null,
    val bodyFatEstimatePct: Double? = null,
    val notes: String = ""
)

@Serializable
data class HealthBackupPayload(
    val schemaVersion: Int = 1,
    val appName: String = "Health Tracker",
    val exportedAtIso: String,
    val preferredUnitSystem: String,
    val recordCount: Int,
    val records: List<HealthBackupRecord>
)

object HealthBackupSerializer {

    private val prettyJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    private fun formatIsoUtc(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }

    fun toJson(
        records: List<HealthRecord>,
        unitSystem: UnitSystem,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val backupRecords = records.map { record ->
            HealthBackupRecord(
                id = record.id,
                timestampMillis = record.timestampMillis,
                recordedAtIso = formatIsoUtc(record.timestampMillis),
                weightKg = HealthCalculator.round1(record.weightKg),
                heightCm = HealthCalculator.round1(record.heightCm),
                waistCm = record.waistCm?.let { HealthCalculator.round1(it) },
                systolicBp = record.systolicBp,
                diastolicBp = record.diastolicBp,
                restingHeartRate = record.restingHeartRate,
                ageYears = record.ageYears,
                biologicalSex = record.biologicalSex.name,
                bmi = record.bmi,
                bmiCategory = record.bmiCategory.label,
                waistToHeightRatio = record.waistToHeightRatio,
                bmrKcal = record.bmrKcal,
                bodyFatEstimatePct = record.bodyFatEstimatePct,
                notes = record.notes
            )
        }

        val payload = HealthBackupPayload(
            exportedAtIso = formatIsoUtc(nowMillis),
            preferredUnitSystem = unitSystem.name,
            recordCount = backupRecords.size,
            records = backupRecords
        )
        return prettyJson.encodeToString(payload)
    }

    fun toCsv(records: List<HealthRecord>): String {
        val sb = StringBuilder()
        sb.appendLine(
            "id,recorded_at_utc,timestamp_ms,weight_kg,height_cm,bmi,bmi_category," +
                "waist_cm,waist_to_height_ratio,systolic_bp,diastolic_bp,resting_hr_bpm," +
                "age_years,biological_sex,bmr_kcal,body_fat_pct,notes"
        )
        for (record in records) {
            val fields = listOf(
                record.id.toString(),
                formatIsoUtc(record.timestampMillis),
                record.timestampMillis.toString(),
                String.format(Locale.US, "%.1f", record.weightKg),
                String.format(Locale.US, "%.1f", record.heightCm),
                String.format(Locale.US, "%.1f", record.bmi),
                escapeCsv(record.bmiCategory.label),
                record.waistCm?.let { String.format(Locale.US, "%.1f", it) } ?: "",
                record.waistToHeightRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                record.systolicBp?.toString() ?: "",
                record.diastolicBp?.toString() ?: "",
                record.restingHeartRate?.toString() ?: "",
                record.ageYears?.toString() ?: "",
                record.biologicalSex.name,
                record.bmrKcal?.toString() ?: "",
                record.bodyFatEstimatePct?.let { String.format(Locale.US, "%.1f", it) } ?: "",
                escapeCsv(record.notes)
            )
            sb.appendLine(fields.joinToString(","))
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        if (!needsQuoting) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
