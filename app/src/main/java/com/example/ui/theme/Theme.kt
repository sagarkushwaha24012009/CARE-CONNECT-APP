package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val CareShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun CareConnectTheme(
    themePreset: ThemePreset = ThemePreset.CLASSIC_PASTEL,
    darkMode: Boolean = isSystemInDarkTheme(),
    textScale: TextScaleOption = TextScaleOption.NORMAL,
    content: @Composable () -> Unit
) {
    val tokens = careTokensFor(themePreset, darkMode)
    val colorScheme = if (darkMode) {
        darkColorScheme(
            primary = tokens.butterYellow,
            onPrimary = tokens.ink,
            secondary = tokens.skyBlue,
            onSecondary = tokens.ink,
            tertiary = tokens.mintGreen,
            onTertiary = tokens.ink,
            error = tokens.sosCoral,
            onError = tokens.ink,
            background = tokens.background,
            onBackground = tokens.ink,
            surface = tokens.cardSurface,
            onSurface = tokens.ink,
            surfaceVariant = tokens.headerSurface,
            onSurfaceVariant = tokens.secondaryInk,
            outline = tokens.border
        )
    } else {
        lightColorScheme(
            primary = tokens.butterYellow,
            onPrimary = tokens.ink,
            secondary = tokens.skyBlue,
            onSecondary = tokens.ink,
            tertiary = tokens.mintGreen,
            onTertiary = tokens.ink,
            error = tokens.sosCoral,
            onError = tokens.ink,
            background = tokens.background,
            onBackground = tokens.ink,
            surface = tokens.cardSurface,
            onSurface = tokens.ink,
            surfaceVariant = tokens.headerSurface,
            onSurfaceVariant = tokens.secondaryInk,
            outline = tokens.border
        )
    }

    CompositionLocalProvider(LocalCareColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildCareTypography(textScale),
            shapes = CareShapes,
            content = content
        )
    }
}
