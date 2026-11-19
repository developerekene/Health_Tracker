package com.devekene.healthtracker.core.model

enum class UnitSystem {
    METRIC,
    IMPERIAL
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class BiologicalSex(val label: String) {
    FEMALE("Female"),
    MALE("Male"),
    UNSPECIFIED("Not specified")
}

enum class BmiCategory(
    val label: String,
    val shortLabel: String,
    val rangeDescription: String,
    val guidance: String
) {
    UNDERWEIGHT(
        label = "Underweight",
        shortLabel = "Underweight",
        rangeDescription = "< 18.5",
        guidance = "Focus on nutrient-dense meals and strength-building routines."
    ),
    NORMAL(
        label = "Healthy Weight",
        shortLabel = "Healthy",
        rangeDescription = "18.5 – 24.9",
        guidance = "Your BMI sits within the standard healthy clinical range."
    ),
    OVERWEIGHT(
        label = "Overweight",
        shortLabel = "Overweight",
        rangeDescription = "25.0 – 29.9",
        guidance = "Gradual activity increases and balanced nutrition can lower long-term risk."
    ),
    OBESE_I(
        label = "Obesity Class I",
        shortLabel = "Obese I",
        rangeDescription = "30.0 – 34.9",
        guidance = "Track waist ratio and blood pressure alongside steady weight trends."
    ),
    OBESE_II(
        label = "Obesity Class II",
        shortLabel = "Obese II",
        rangeDescription = "35.0 – 39.9",
        guidance = "Consider consulting a healthcare clinician for personalized metabolic guidance."
    ),
    OBESE_III(
        label = "Obesity Class III",
        shortLabel = "Obese III",
        rangeDescription = ">= 40.0",
        guidance = "Comprehensive clinical follow-up is recommended for cardiovascular health."
    )
}

enum class WaistRiskCategory(val label: String, val guidance: String) {
    HEALTHY("Healthy Ratio", "Waist is less than half of height (< 0.50)."),
    INCREASED("Increased Risk", "Waist-to-height ratio is moderately elevated (0.50 – 0.59)."),
    HIGH("High Metabolic Risk", "Waist-to-height ratio is >= 0.60; central adiposity risk.")
}

enum class BloodPressureCategory(val label: String) {
    NORMAL("Normal"),
    ELEVATED("Elevated"),
    STAGE_1("Stage 1 High"),
    STAGE_2("Stage 2 High"),
    CRISIS("Hypertensive Crisis")
}

enum class HeartRateZone(val label: String) {
    ATHLETIC_LOW("Low / Athletic"),
    NORMAL("Normal Resting"),
    ELEVATED("Elevated Resting")
}

data class HealthRecord(
    val id: Long = 0L,
    val timestampMillis: Long,
    val weightKg: Double,
    val heightCm: Double,
    val waistCm: Double? = null,
    val systolicBp: Int? = null,
    val diastolicBp: Int? = null,
    val restingHeartRate: Int? = null,
    val ageYears: Int? = null,
    val biologicalSex: BiologicalSex = BiologicalSex.UNSPECIFIED,
    val bmi: Double,
    val bmiCategory: BmiCategory,
    val waistToHeightRatio: Double? = null,
    val bmrKcal: Int? = null,
    val bodyFatEstimatePct: Double? = null,
    val notes: String = ""
)

data class UserPreferences(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultHeightCm: Double = 172.0,
    val targetWeightKg: Double = 70.0,
    val defaultAgeYears: Int = 30,
    val defaultSex: BiologicalSex = BiologicalSex.UNSPECIFIED
)

data class HealthCheckSummary(
    val bmi: Double,
    val bmiCategory: BmiCategory,
    val healthyWeightMinKg: Double,
    val healthyWeightMaxKg: Double,
    val weightDeltaToHealthyKg: Double,
    val waistToHeightRatio: Double?,
    val waistRiskCategory: WaistRiskCategory?,
    val bloodPressureCategory: BloodPressureCategory?,
    val heartRateZone: HeartRateZone?,
    val bmrKcal: Int?,
    val bodyFatEstimatePct: Double?
)
