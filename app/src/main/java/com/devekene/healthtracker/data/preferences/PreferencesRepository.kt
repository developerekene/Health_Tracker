package com.devekene.healthtracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devekene.healthtracker.core.model.BiologicalSex
import com.devekene.healthtracker.core.model.ThemeMode
import com.devekene.healthtracker.core.model.UnitSystem
import com.devekene.healthtracker.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.healthPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "health_tracker_preferences"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_HEIGHT_CM = doublePreferencesKey("default_height_cm")
        val TARGET_WEIGHT_KG = doublePreferencesKey("target_weight_kg")
        val DEFAULT_AGE_YEARS = intPreferencesKey("default_age_years")
        val DEFAULT_SEX = stringPreferencesKey("default_sex")
    }

    val preferencesFlow: Flow<UserPreferences> = context.healthPreferencesDataStore.data.map { prefs ->
        val unitSystem = prefs[Keys.UNIT_SYSTEM]
            ?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
            ?: UnitSystem.METRIC
        val themeMode = prefs[Keys.THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.DARK
        val heightCm = prefs[Keys.DEFAULT_HEIGHT_CM] ?: 172.0
        val targetWeightKg = prefs[Keys.TARGET_WEIGHT_KG] ?: 70.0
        val ageYears = prefs[Keys.DEFAULT_AGE_YEARS] ?: 30
        val sex = prefs[Keys.DEFAULT_SEX]
            ?.let { runCatching { BiologicalSex.valueOf(it) }.getOrNull() }
            ?: BiologicalSex.UNSPECIFIED

        UserPreferences(
            unitSystem = unitSystem,
            themeMode = themeMode,
            defaultHeightCm = heightCm,
            targetWeightKg = targetWeightKg,
            defaultAgeYears = ageYears,
            defaultSex = sex
        )
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        context.healthPreferencesDataStore.edit { it[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.healthPreferencesDataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun setDefaultHeightCm(heightCm: Double) {
        context.healthPreferencesDataStore.edit { it[Keys.DEFAULT_HEIGHT_CM] = heightCm }
    }

    suspend fun setTargetWeightKg(targetWeightKg: Double) {
        context.healthPreferencesDataStore.edit { it[Keys.TARGET_WEIGHT_KG] = targetWeightKg }
    }

    suspend fun setDefaultProfile(ageYears: Int, sex: BiologicalSex) {
        context.healthPreferencesDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_AGE_YEARS] = ageYears
            prefs[Keys.DEFAULT_SEX] = sex.name
        }
    }
}
