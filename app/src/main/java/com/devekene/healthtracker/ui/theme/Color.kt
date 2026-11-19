package com.devekene.healthtracker.ui.theme

import androidx.compose.ui.graphics.Color
import com.devekene.healthtracker.core.model.BloodPressureCategory
import com.devekene.healthtracker.core.model.BmiCategory
import com.devekene.healthtracker.core.model.WaistRiskCategory

// Precision Clinical Dark Palette
val ObsidianBg = Color(0xFF0B0F14)
val SlateSurface = Color(0xFF121820)
val SlateSurfaceLow = Color(0xFF161E27)
val SlateSurfaceContainer = Color(0xFF1B2430)
val SlateSurfaceHigh = Color(0xFF232E3C)
val SlateSurfaceHighest = Color(0xFF2C394B)
val SlateOutline = Color(0xFF334155)
val SlateOutlineVariant = Color(0xFF222C38)

val BiometricTeal = Color(0xFF2DD4BF)
val OnBiometricTeal = Color(0xFF042F2E)
val TealContainer = Color(0xFF114B47)
val OnTealContainer = Color(0xFF99F6E4)

val SkySecondary = Color(0xFF38BDF8)
val OnSkySecondary = Color(0xFF082F49)
val SkyContainer = Color(0xFF0C4A6E)
val OnSkyContainer = Color(0xFFBAE6FD)

val MintTertiary = Color(0xFF34D399)
val OnMintTertiary = Color(0xFF022C22)
val MintContainer = Color(0xFF065F46)
val OnMintContainer = Color(0xFFA7F3D0)

val SlateTextPrimary = Color(0xFFF1F5F9)
val SlateTextSecondary = Color(0xFF94A3B8)

// Light Palette (Optional override in settings)
val ClinicalLightBg = Color(0xFFF8FAFC)
val ClinicalLightSurface = Color(0xFFFFFFFF)
val ClinicalLightContainer = Color(0xFFF1F5F9)
val ClinicalLightHigh = Color(0xFFE2E8F0)
val ClinicalLightTeal = Color(0xFF0F766E)
val ClinicalLightTealContainer = Color(0xFFCCFBF1)
val ClinicalLightTextPrimary = Color(0xFF0F172A)
val ClinicalLightTextSecondary = Color(0xFF475569)

// Semantic Clinical Colors
val StatusBlue = Color(0xFF60A5FA)
val StatusEmerald = Color(0xFF34D399)
val StatusAmber = Color(0xFFFBBF24)
val StatusCoral = Color(0xFFF87171)
val StatusRose = Color(0xFFFB7185)

fun BmiCategory.accentColor(): Color = when (this) {
    BmiCategory.UNDERWEIGHT -> StatusBlue
    BmiCategory.NORMAL -> StatusEmerald
    BmiCategory.OVERWEIGHT -> StatusAmber
    BmiCategory.OBESE_I -> StatusCoral
    BmiCategory.OBESE_II -> StatusCoral
    BmiCategory.OBESE_III -> StatusRose
}

fun WaistRiskCategory.accentColor(): Color = when (this) {
    WaistRiskCategory.HEALTHY -> StatusEmerald
    WaistRiskCategory.INCREASED -> StatusAmber
    WaistRiskCategory.HIGH -> StatusCoral
}

fun BloodPressureCategory.accentColor(): Color = when (this) {
    BloodPressureCategory.NORMAL -> StatusEmerald
    BloodPressureCategory.ELEVATED -> StatusAmber
    BloodPressureCategory.STAGE_1 -> StatusCoral
    BloodPressureCategory.STAGE_2 -> StatusRose
    BloodPressureCategory.CRISIS -> StatusRose
}
