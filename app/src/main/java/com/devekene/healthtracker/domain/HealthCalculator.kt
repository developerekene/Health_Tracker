package com.devekene.healthtracker.domain

import com.devekene.healthtracker.core.model.BiologicalSex
import com.devekene.healthtracker.core.model.BloodPressureCategory
import com.devekene.healthtracker.core.model.BmiCategory
import com.devekene.healthtracker.core.model.HealthCheckSummary
import com.devekene.healthtracker.core.model.HeartRateZone
import com.devekene.healthtracker.core.model.WaistRiskCategory
import kotlin.math.roundToInt

object HealthCalculator {

    private const val KG_TO_LBS = 2.20462262185
    private const val CM_TO_IN = 0.3937007874

    fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (weightKg <= 0.0 || heightCm <= 0.0) return 0.0
        val heightMeters = heightCm / 100.0
        val raw = weightKg / (heightMeters * heightMeters)
        return (raw * 10.0).roundToInt() / 10.0
    }

    fun classifyBmi(bmi: Double): BmiCategory = when {
        bmi < 18.5 -> BmiCategory.UNDERWEIGHT
        bmi < 25.0 -> BmiCategory.NORMAL
        bmi < 30.0 -> BmiCategory.OVERWEIGHT
        bmi < 35.0 -> BmiCategory.OBESE_I
        bmi < 40.0 -> BmiCategory.OBESE_II
        else -> BmiCategory.OBESE_III
    }

    fun healthyWeightRangeKg(heightCm: Double): Pair<Double, Double> {
        if (heightCm <= 0.0) return 0.0 to 0.0
        val heightMeters = heightCm / 100.0
        val minKg = 18.5 * heightMeters * heightMeters
        val maxKg = 24.9 * heightMeters * heightMeters
        return round1(minKg) to round1(maxKg)
    }

    fun calculateWaistToHeightRatio(waistCm: Double?, heightCm: Double): Double? {
        if (waistCm == null || waistCm <= 0.0 || heightCm <= 0.0) return null
        val ratio = waistCm / heightCm
        return (ratio * 100.0).roundToInt() / 100.0
    }

    fun classifyWaistRisk(ratio: Double?): WaistRiskCategory? {
        if (ratio == null) return null
        return when {
            ratio < 0.50 -> WaistRiskCategory.HEALTHY
            ratio < 0.60 -> WaistRiskCategory.INCREASED
            else -> WaistRiskCategory.HIGH
        }
    }

    fun calculateBmrMifflinStJeor(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int?,
        sex: BiologicalSex
    ): Int? {
        if (weightKg <= 0.0 || heightCm <= 0.0 || ageYears == null || ageYears <= 0) return null
        if (sex == BiologicalSex.UNSPECIFIED) return null
        val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * ageYears)
        val offset = when (sex) {
            BiologicalSex.MALE -> 5.0
            BiologicalSex.FEMALE -> -161.0
            BiologicalSex.UNSPECIFIED -> 0.0
        }
        return (base + offset).roundToInt().coerceAtLeast(0)
    }

    fun estimateBodyFatDeurenberg(
        bmi: Double,
        ageYears: Int?,
        sex: BiologicalSex
    ): Double? {
        if (bmi <= 0.0 || ageYears == null || ageYears <= 0) return null
        if (sex == BiologicalSex.UNSPECIFIED) return null
        val sexFactor = if (sex == BiologicalSex.MALE) 1.0 else 0.0
        val raw = (1.20 * bmi) + (0.23 * ageYears) - (10.8 * sexFactor) - 5.4
        return round1(raw.coerceIn(3.0, 65.0))
    }

    fun classifyBloodPressure(systolic: Int?, diastolic: Int?): BloodPressureCategory? {
        if (systolic == null || diastolic == null || systolic <= 0 || diastolic <= 0) return null
        return when {
            systolic > 180 || diastolic > 120 -> BloodPressureCategory.CRISIS
            systolic >= 140 || diastolic >= 90 -> BloodPressureCategory.STAGE_2
            systolic in 130..139 || diastolic in 80..89 -> BloodPressureCategory.STAGE_1
            systolic in 120..129 && diastolic < 80 -> BloodPressureCategory.ELEVATED
            else -> BloodPressureCategory.NORMAL
        }
    }

    fun classifyHeartRate(bpm: Int?): HeartRateZone? {
        if (bpm == null || bpm <= 0) return null
        return when {
            bpm < 60 -> HeartRateZone.ATHLETIC_LOW
            bpm <= 100 -> HeartRateZone.NORMAL
            else -> HeartRateZone.ELEVATED
        }
    }

    fun summarize(
        weightKg: Double,
        heightCm: Double,
        waistCm: Double? = null,
        systolicBp: Int? = null,
        diastolicBp: Int? = null,
        restingHeartRate: Int? = null,
        ageYears: Int? = null,
        sex: BiologicalSex = BiologicalSex.UNSPECIFIED
    ): HealthCheckSummary {
        val bmi = calculateBmi(weightKg, heightCm)
        val bmiCat = classifyBmi(bmi)
        val (minHealthy, maxHealthy) = healthyWeightRangeKg(heightCm)
        val deltaToHealthy = when {
            weightKg < minHealthy -> round1(minHealthy - weightKg)
            weightKg > maxHealthy -> round1(maxHealthy - weightKg)
            else -> 0.0
        }
        val whtr = calculateWaistToHeightRatio(waistCm, heightCm)
        val waistRisk = classifyWaistRisk(whtr)
        val bpCat = classifyBloodPressure(systolicBp, diastolicBp)
        val hrZone = classifyHeartRate(restingHeartRate)
        val bmr = calculateBmrMifflinStJeor(weightKg, heightCm, ageYears, sex)
        val bodyFat = estimateBodyFatDeurenberg(bmi, ageYears, sex)

        return HealthCheckSummary(
            bmi = bmi,
            bmiCategory = bmiCat,
            healthyWeightMinKg = minHealthy,
            healthyWeightMaxKg = maxHealthy,
            weightDeltaToHealthyKg = deltaToHealthy,
            waistToHeightRatio = whtr,
            waistRiskCategory = waistRisk,
            bloodPressureCategory = bpCat,
            heartRateZone = hrZone,
            bmrKcal = bmr,
            bodyFatEstimatePct = bodyFat
        )
    }

    fun kgToLbs(kg: Double): Double = round1(kg * KG_TO_LBS)
    fun lbsToKg(lbs: Double): Double = lbs / KG_TO_LBS
    fun cmToInches(cm: Double): Double = round1(cm * CM_TO_IN)
    fun inchesToCm(inches: Double): Double = inches / CM_TO_IN

    fun cmToFeetAndInches(cm: Double): Pair<Int, Int> {
        val totalInches = (cm * CM_TO_IN).roundToInt().coerceAtLeast(0)
        val feet = totalInches / 12
        val inches = totalInches % 12
        return feet to inches
    }

    fun feetAndInchesToCm(feet: Int, inches: Int): Double {
        val totalInches = (feet * 12) + inches
        return round1(totalInches / CM_TO_IN)
    }

    fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}
