package com.devekene.healthtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devekene.healthtracker.core.model.ThemeMode

private val HealthDarkColorScheme = darkColorScheme(
    primary = BiometricTeal,
    onPrimary = OnBiometricTeal,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = SkySecondary,
    onSecondary = OnSkySecondary,
    secondaryContainer = SkyContainer,
    onSecondaryContainer = OnSkyContainer,
    tertiary = MintTertiary,
    onTertiary = OnMintTertiary,
    tertiaryContainer = MintContainer,
    onTertiaryContainer = OnMintContainer,
    background = ObsidianBg,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateSurfaceContainer,
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurfaceLow,
    surfaceContainer = SlateSurfaceContainer,
    surfaceContainerHigh = SlateSurfaceHigh,
    surfaceContainerHighest = SlateSurfaceHighest,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant,
    error = StatusCoral,
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val HealthLightColorScheme = lightColorScheme(
    primary = ClinicalLightTeal,
    onPrimary = Color.White,
    primaryContainer = ClinicalLightTealContainer,
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = ClinicalLightBg,
    onBackground = ClinicalLightTextPrimary,
    surface = ClinicalLightSurface,
    onSurface = ClinicalLightTextPrimary,
    surfaceVariant = ClinicalLightContainer,
    onSurfaceVariant = ClinicalLightTextSecondary,
    surfaceContainerLow = ClinicalLightSurface,
    surfaceContainer = ClinicalLightContainer,
    surfaceContainerHigh = ClinicalLightHigh,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val HealthShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HealthTrackerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) HealthDarkColorScheme else HealthLightColorScheme,
        typography = HealthTypography,
        shapes = HealthShapes,
        content = content
    )
}
