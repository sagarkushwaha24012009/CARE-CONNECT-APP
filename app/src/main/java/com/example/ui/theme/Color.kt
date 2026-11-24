package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemePreset(val id: String, val label: String) {
    CLASSIC_PASTEL("classic_pastel", "Classic Pastel"),
    WARM_CREAM("warm_cream", "Warm Cream"),
    HIGH_CONTRAST("high_contrast", "High Contrast");

    companion object {
        fun fromId(raw: String?): ThemePreset =
            entries.firstOrNull { it.id == raw } ?: CLASSIC_PASTEL
    }
}

enum class TextScaleOption(val id: String, val label: String, val multiplier: Float) {
    NORMAL("normal", "Normal", 1.0f),
    LARGE("large", "Large", 1.15f),
    EXTRA_LARGE("extra_large", "Extra Large", 1.30f);

    companion object {
        fun fromId(raw: String?): TextScaleOption =
            entries.firstOrNull { it.id == raw } ?: NORMAL
    }
}

@Immutable
data class CareColorTokens(
    val background: Color,
    val cardSurface: Color,
    val headerSurface: Color,
    val skyBlue: Color,
    val mintGreen: Color,
    val softPink: Color,
    val butterYellow: Color,
    val lavender: Color,
    val sosCoral: Color,
    val onlineGreen: Color,
    val progressTrack: Color,
    val progressFill: Color,
    val ink: Color,
    val secondaryInk: Color,
    val border: Color,
    val shadow: Color,
    val navActive: Color,
    val badgeRed: Color,
    val badgeText: Color,
    val isDark: Boolean
)

fun careTokensFor(preset: ThemePreset, darkMode: Boolean): CareColorTokens {
    if (!darkMode) {
        return when (preset) {
            ThemePreset.CLASSIC_PASTEL -> CareColorTokens(
                background = Color(0xFFFFFDF7),
                cardSurface = Color(0xFFFFFFFF),
                headerSurface = Color(0xFFFFEA8A),
                skyBlue = Color(0xFFA5D8FF),
                mintGreen = Color(0xFFB2F2BB),
                softPink = Color(0xFFFFB8C6),
                butterYellow = Color(0xFFFFE066),
                lavender = Color(0xFFD8C6FF),
                sosCoral = Color(0xFFFF7B7B),
                onlineGreen = Color(0xFF40C057),
                progressTrack = Color(0xFFFFFFFF),
                progressFill = Color(0xFF228BE6),
                ink = Color(0xFF111111),
                secondaryInk = Color(0xFF2B2B2B),
                border = Color(0xFF111111),
                shadow = Color(0xFF111111),
                navActive = Color(0xFFFFDF5D),
                badgeRed = Color(0xFFE03131),
                badgeText = Color(0xFFFFFFFF),
                isDark = false
            )
            ThemePreset.WARM_CREAM -> CareColorTokens(
                background = Color(0xFFFFF8EE),
                cardSurface = Color(0xFFFFFDF9),
                headerSurface = Color(0xFFFFD97D),
                skyBlue = Color(0xFFBFD7EA),
                mintGreen = Color(0xFFC3E8BD),
                softPink = Color(0xFFFFCAD4),
                butterYellow = Color(0xFFFFD166),
                lavender = Color(0xFFE2D4F0),
                sosCoral = Color(0xFFF27070),
                onlineGreen = Color(0xFF37B24D),
                progressTrack = Color(0xFFFFFDF9),
                progressFill = Color(0xFF1C7ED6),
                ink = Color(0xFF1A1612),
                secondaryInk = Color(0xFF383028),
                border = Color(0xFF1A1612),
                shadow = Color(0xFF1A1612),
                navActive = Color(0xFFFFD166),
                badgeRed = Color(0xFFC92A2A),
                badgeText = Color(0xFFFFFFFF),
                isDark = false
            )
            ThemePreset.HIGH_CONTRAST -> CareColorTokens(
                background = Color(0xFFFFFFFF),
                cardSurface = Color(0xFFFFFFFF),
                headerSurface = Color(0xFFFFE600),
                skyBlue = Color(0xFFD0EBFF),
                mintGreen = Color(0xFFD3F9D8),
                softPink = Color(0xFFFFD6DF),
                butterYellow = Color(0xFFFFE600),
                lavender = Color(0xFFEADCFD),
                sosCoral = Color(0xFFFF6B6B),
                onlineGreen = Color(0xFF2F9E44),
                progressTrack = Color(0xFFFFFFFF),
                progressFill = Color(0xFF005BBB),
                ink = Color(0xFF000000),
                secondaryInk = Color(0xFF000000),
                border = Color(0xFF000000),
                shadow = Color(0xFF000000),
                navActive = Color(0xFFFFE600),
                badgeRed = Color(0xFFD00000),
                badgeText = Color(0xFFFFFFFF),
                isDark = false
            )
        }
    } else {
        return when (preset) {
            ThemePreset.CLASSIC_PASTEL -> CareColorTokens(
                background = Color(0xFF13151A),
                cardSurface = Color(0xFF1E222A),
                headerSurface = Color(0xFF3D3418),
                skyBlue = Color(0xFF1E3A52),
                mintGreen = Color(0xFF1C3B28),
                softPink = Color(0xFF47222D),
                butterYellow = Color(0xFF463B16),
                lavender = Color(0xFF32284A),
                sosCoral = Color(0xFF691E22),
                onlineGreen = Color(0xFF2F9E44),
                progressTrack = Color(0xFF13151A),
                progressFill = Color(0xFF74C0FC),
                ink = Color(0xFFF8F9FA),
                secondaryInk = Color(0xFFE2E6EC),
                border = Color(0xFFF1F3F5),
                shadow = Color(0xFF000000),
                navActive = Color(0xFF463B16),
                badgeRed = Color(0xFFFF5252),
                badgeText = Color(0xFF111111),
                isDark = true
            )
            ThemePreset.WARM_CREAM -> CareColorTokens(
                background = Color(0xFF191512),
                cardSurface = Color(0xFF241E1A),
                headerSurface = Color(0xFF45361A),
                skyBlue = Color(0xFF233847),
                mintGreen = Color(0xFF223B26),
                softPink = Color(0xFF4A252C),
                butterYellow = Color(0xFF4C3A16),
                lavender = Color(0xFF362A45),
                sosCoral = Color(0xFF6E2121),
                onlineGreen = Color(0xFF37B24D),
                progressTrack = Color(0xFF191512),
                progressFill = Color(0xFF66D9E8),
                ink = Color(0xFFFFF8EE),
                secondaryInk = Color(0xFFEADFD0),
                border = Color(0xFFFFF3E0),
                shadow = Color(0xFF000000),
                navActive = Color(0xFF4C3A16),
                badgeRed = Color(0xFFFF6B6B),
                badgeText = Color(0xFF111111),
                isDark = true
            )
            ThemePreset.HIGH_CONTRAST -> CareColorTokens(
                background = Color(0xFF000000),
                cardSurface = Color(0xFF111111),
                headerSurface = Color(0xFF332D00),
                skyBlue = Color(0xFF002B4D),
                mintGreen = Color(0xFF003814),
                softPink = Color(0xFF4A0018),
                butterYellow = Color(0xFF3D3500),
                lavender = Color(0xFF29004D),
                sosCoral = Color(0xFF7A0000),
                onlineGreen = Color(0xFF40C057),
                progressTrack = Color(0xFF000000),
                progressFill = Color(0xFFFFE600),
                ink = Color(0xFFFFFFFF),
                secondaryInk = Color(0xFFFFFFFF),
                border = Color(0xFFFFFFFF),
                shadow = Color(0xFF333333),
                navActive = Color(0xFF3D3500),
                badgeRed = Color(0xFFFF3B30),
                badgeText = Color(0xFFFFFFFF),
                isDark = true
            )
        }
    }
}

val LocalCareColors = staticCompositionLocalOf {
    careTokensFor(ThemePreset.CLASSIC_PASTEL, darkMode = false)
}
