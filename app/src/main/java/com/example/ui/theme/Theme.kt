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

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark Navy"),
    LIGHT("Crisp Light")
}

private val DarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = DeepViolet,
    onPrimaryContainer = SoftLavender,
    secondary = SkyAccent,
    onSecondary = MidnightNavy,
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = AmberGold,
    onTertiary = MidnightNavy,
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = MidnightNavy,
    onBackground = Color(0xFFF1F5F9),
    surface = DeepNavy,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF070B16),
    surfaceContainerLow = Color(0xFF0F172A),
    surfaceContainer = DeepNavy,
    surfaceContainerHigh = NavySurfaceVariant,
    surfaceContainerHighest = Color(0xFF243254),
    outline = NavyOutline,
    outlineVariant = Color(0xFF1E293B),
    error = CoralError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalPurple,
    onPrimary = Color.White,
    primaryContainer = LavenderMist,
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = CrispPaper,
    onBackground = InkDark,
    surface = PureWhite,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = SlateOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val StudyVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudyVaultTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudyVaultShapes,
        content = content
    )
}
