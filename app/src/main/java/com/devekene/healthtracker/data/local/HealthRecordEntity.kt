package com.devekene.healthtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devekene.healthtracker.core.model.BiologicalSex
import com.devekene.healthtracker.core.model.BmiCategory
import com.devekene.healthtracker.core.model.HealthRecord

@Entity(tableName = "health_records")
data class HealthRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestampMillis: Long,
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double? = null,
    val systolicBp: Int? = null,
    val diastolicBp: Int? = null,
    val restingHeartRate: Int? = null,
    val ageYears: Int? = null,
    val biologicalSex: String = BiologicalSex.UNSPECIFIED.name,
    val bmi: Double,
    val bmiCategory: String,
    val waistToHeightRatio: Double? = null,
    val bmrKcal: Int? = null,
    val bodyFatEstimatePct: Double? = null,
    val notes: String = ""
)

fun HealthRecordEntity.toDomain(): HealthRecord {
    val sex = runCatching { BiologicalSex.valueOf(biologicalSex) }
        .getOrDefault(BiologicalSex.UNSPECIFIED)
    val category = runCatching { BmiCategory.valueOf(bmiCategory) }
        .getOrDefault(BmiCategory.NORMAL)
    return HealthRecord(
        id = id,
        timestampMillis = timestampMillis,
        weightKg = weightKg,
        heightCm = heightCm,
        waistCm = waistCm,
        systolicBp = systolicBp,
        diastolicBp = diastolicBp,
        restingHeartRate = restingHeartRate,
        ageYears = ageYears,
        biologicalSex = sex,
        bmi = bmi,
        bmiCategory = category,
        waistToHeightRatio = waistToHeightRatio,
        bmrKcal = bmrKcal,
        bodyFatEstimatePct = bodyFatEstimatePct,
        notes = notes
    )
}

fun HealthRecord.toEntity(): HealthRecordEntity = HealthRecordEntity(
    id = id,
    timestampMillis = timestampMillis,
    weightKg = weightKg,
    heightCm = heightCm,
    waistCm = waistCm,
    systolicBp = systolicBp,
    diastolicBp = diastolicBp,
    restingHeartRate = restingHeartRate,
    ageYears = ageYears,
    biologicalSex = biologicalSex.name,
    bmi = bmi,
    bmiCategory = bmiCategory.name,
    waistToHeightRatio = waistToHeightRatio,
    bmrKcal = bmrKcal,
    bodyFatEstimatePct = bodyFatEstimatePct,
    notes = notes
)
