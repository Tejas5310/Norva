package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NovaraDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ElectricCyanDark,
    primaryContainer = Color(0xFF004E5C),
    onPrimaryContainer = Color(0xFFA8F5FF),
    secondary = PrismViolet,
    onSecondary = Color.White,
    secondaryContainer = PrismVioletContainer,
    onSecondaryContainer = Color(0xFFE0D4FF),
    tertiary = PulseCoral,
    onTertiary = Color.White,
    tertiaryContainer = PulseCoralContainer,
    onTertiaryContainer = Color(0xFFFFD6DF),
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceElevated,
    surfaceContainerHigh = ObsidianSurfaceVariant,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF252E52),
    error = Color(0xFFFF6B81),
    onError = Color(0xFF3E0010)
)

private val NovaraLightColorScheme = lightColorScheme(
    primary = CyanLightPrimary,
    onPrimary = Color.White,
    primaryContainer = CyanLightContainer,
    onPrimaryContainer = Color(0xFF002A34),
    secondary = VioletLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = VioletLightContainer,
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = CoralLightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = CoralLightContainer,
    onTertiaryContainer = Color(0xFF4C0519),
    background = AlabasterBackground,
    onBackground = TextPrimaryLight,
    surface = AlabasterSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = AlabasterSurfaceVariant,
    outline = AlabasterOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val NovaraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun NovaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NovaraDarkColorScheme else NovaraLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NovaraTypography,
        shapes = NovaraShapes,
        content = content
    )
}
