package com.bzcode.scorenge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bzcode.scorenge.R

/** Plus Jakarta Sans, bundled as a variable font; each weight sets the font's wght axis. */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.jakarta(weight: FontWeight? = null) =
    copy(fontFamily = PlusJakartaSans, fontWeight = weight ?: fontWeight)

val ScorengeTypography = Typography(
    displayLarge = base.displayLarge.jakarta(FontWeight.ExtraBold),
    displayMedium = base.displayMedium.jakarta(FontWeight.ExtraBold),
    displaySmall = base.displaySmall.jakarta(FontWeight.ExtraBold),
    headlineLarge = base.headlineLarge.jakarta(FontWeight.ExtraBold).copy(fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = base.headlineMedium.jakarta(FontWeight.Bold),
    headlineSmall = base.headlineSmall.jakarta(FontWeight.Bold),
    titleLarge = base.titleLarge.jakarta(FontWeight.Bold),
    titleMedium = base.titleMedium.jakarta(FontWeight.Bold),
    titleSmall = base.titleSmall.jakarta(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.jakarta(),
    bodyMedium = base.bodyMedium.jakarta(),
    bodySmall = base.bodySmall.jakarta(),
    labelLarge = base.labelLarge.jakarta(FontWeight.SemiBold),
    labelMedium = base.labelMedium.jakarta(FontWeight.SemiBold),
    labelSmall = base.labelSmall.jakarta(FontWeight.Bold),
)
