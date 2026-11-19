package com.devekene.healthtracker.ui.components

import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.domain.HealthCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HealthFormatters {

    fun formatWeight(weightKg: Double, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> String.format(Locale.US, "%.1f kg", weightKg)
            UnitSystem.IMPERIAL -> String.format(Locale.US, "%.1f lbs", HealthCalculator.kgToLbs(weightKg))
        }
    }

    fun formatWeightNumber(weightKg: Double, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> String.format(Locale.US, "%.1f", weightKg)
            UnitSystem.IMPERIAL -> String.format(Locale.US, "%.1f", HealthCalculator.kgToLbs(weightKg))
        }
    }

    fun formatWeightDelta(deltaKg: Double, unitSystem: UnitSystem): String {
        val value = when (unitSystem) {
            UnitSystem.METRIC -> deltaKg
            UnitSystem.IMPERIAL -> HealthCalculator.kgToLbs(deltaKg)
        }
        val unit = if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"
        val sign = if (value > 0) "+" else ""
        return String.format(Locale.US, "%s%.1f %s", sign, value, unit)
    }

    fun formatHeight(heightCm: Double, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> String.format(Locale.US, "%.0f cm", heightCm)
            UnitSystem.IMPERIAL -> {
                val (ft, inches) = HealthCalculator.cmToFeetAndInches(heightCm)
                "$ft' $inches\""
            }
        }
    }

    fun formatShortDate(millis: Long): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatByteSize(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            else -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        }
    }
}
