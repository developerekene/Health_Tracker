package com.devekene.healthtracker

import com.devekene.healthtracker.core.model.BiologicalSex
import com.devekene.healthtracker.core.model.BloodPressureCategory
import com.devekene.healthtracker.core.model.BmiCategory
import com.devekene.healthtracker.core.model.HealthRecord
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.core.model.WaistRiskCategory
import com.devekene.healthtracker.domain.HealthBackupSerializer
import com.devekene.healthtracker.domain.HealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCalculatorAndBackupTest {

    @Test
    fun `bmi calculation and classification match WHO thresholds`() {
        val bmi = HealthCalculator.calculateBmi(weightKg = 70.0, heightCm = 175.0)
        assertEquals(22.9, bmi, 0.05)
        assertEquals(BmiCategory.NORMAL, HealthCalculator.classifyBmi(bmi))

        assertEquals(BmiCategory.UNDERWEIGHT, HealthCalculator.classifyBmi(17.8))
        assertEquals(BmiCategory.OVERWEIGHT, HealthCalculator.classifyBmi(27.4))
        assertEquals(BmiCategory.OBESE_I, HealthCalculator.classifyBmi(31.2))
        assertEquals(BmiCategory.OBESE_II, HealthCalculator.classifyBmi(36.5))
        assertEquals(BmiCategory.OBESE_III, HealthCalculator.classifyBmi(41.0))
    }

    @Test
    fun `waist to height ratio and blood pressure classify accurately`() {
        val whtr = HealthCalculator.calculateWaistToHeightRatio(waistCm = 80.0, heightCm = 175.0)
        assertEquals(0.46, whtr!!, 0.01)
        assertEquals(WaistRiskCategory.HEALTHY, HealthCalculator.classifyWaistRisk(whtr))

        assertEquals(BloodPressureCategory.NORMAL, HealthCalculator.classifyBloodPressure(116, 74))
        assertEquals(BloodPressureCategory.ELEVATED, HealthCalculator.classifyBloodPressure(124, 76))
        assertEquals(BloodPressureCategory.STAGE_1, HealthCalculator.classifyBloodPressure(134, 82))
        assertEquals(BloodPressureCategory.STAGE_2, HealthCalculator.classifyBloodPressure(146, 94))
    }

    @Test
    fun `bmr and unit conversions are accurate`() {
        val bmrMale = HealthCalculator.calculateBmrMifflinStJeor(
            weightKg = 75.0,
            heightCm = 180.0,
            ageYears = 30,
            sex = BiologicalSex.MALE
        )
        assertEquals(1730, bmrMale)

        val (ft, inches) = HealthCalculator.cmToFeetAndInches(178.0)
        assertEquals(5, ft)
        assertEquals(10, inches)
    }

    @Test
    fun `json and csv backup serializers produce valid output and escape commas`() {
        val records = listOf(
            HealthRecord(
                id = 1L,
                timestampMillis = 1_700_000_000_000L,
                weightKg = 72.5,
                heightCm = 175.0,
                waistCm = 82.0,
                systolicBp = 118,
                diastolicBp = 76,
                restingHeartRate = 62,
                ageYears = 30,
                biologicalSex = BiologicalSex.MALE,
                bmi = 23.7,
                bmiCategory = BmiCategory.NORMAL,
                waistToHeightRatio = 0.47,
                bmrKcal = 1699,
                bodyFatEstimatePct = 19.1,
                notes = "Morning check, feeling \"energized\""
            )
        )

        val json = HealthBackupSerializer.toJson(records, UnitSystem.METRIC, nowMillis = 1_700_000_100_000L)
        assertTrue(json.contains("\"appName\": \"Health Tracker\""))
        assertTrue(json.contains("\"recordCount\": 1"))
        assertTrue(json.contains("\"bmi\": 23.7"))

        val csv = HealthBackupSerializer.toCsv(records)
        assertTrue(csv.startsWith("id,recorded_at_utc,timestamp_ms,weight_kg,height_cm,bmi"))
        assertTrue(csv.contains("\"Morning check, feeling \"\"energized\"\"\""))
    }
}
