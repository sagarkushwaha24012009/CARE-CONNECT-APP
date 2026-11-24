package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val DisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

val BodyFontFamily = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Medium),
    Font(R.font.nunito, FontWeight.SemiBold),
    Font(R.font.nunito, FontWeight.Bold)
)

fun buildCareTypography(scale: TextScaleOption): Typography {
    val m = scale.multiplier
    return Typography(
        displaySmall = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (30 * m).sp,
            lineHeight = (36 * m).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (24 * m).sp,
            lineHeight = (30 * m).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (21 * m).sp,
            lineHeight = (27 * m).sp
        ),
        titleLarge = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (19 * m).sp,
            lineHeight = (25 * m).sp
        ),
        titleMedium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (17 * m).sp,
            lineHeight = (22 * m).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * m).sp,
            lineHeight = (23 * m).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (15 * m).sp,
            lineHeight = (21 * m).sp
        ),
        labelLarge = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (15 * m).sp,
            lineHeight = (20 * m).sp
        ),
        labelMedium = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (13 * m).sp,
            lineHeight = (18 * m).sp
        )
    )
}
