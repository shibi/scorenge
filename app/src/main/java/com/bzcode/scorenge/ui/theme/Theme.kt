package com.bzcode.scorenge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Light scheme. Material's `primary` drives filled buttons with white text, so it is the deep orange
 * (#C93D18); the brighter brand orange is exposed separately through [ScorengeColors.brand].
 */
internal val LightColors = lightColorScheme(
    primary = BrandOrangeDeep,
    onPrimary = Color.White,
    primaryContainer = Peach,
    onPrimaryContainer = Color(0xFF5C1600),
    secondary = Color(0xFF8A4B2E),
    onSecondary = Color.White,
    secondaryContainer = Cream,
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCF5E3),
    onTertiaryContainer = Color(0xFF0B3D1D),
    background = WarmWhite,
    onBackground = InkDark,
    surface = WarmWhite,
    onSurface = InkDark,
    surfaceVariant = Color(0xFFF4EADB),
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF8EA),
    surfaceContainer = Color(0xFFFBF2E3),
    surfaceContainerHigh = Color(0xFFF6EDDE),
    surfaceContainerHighest = Color(0xFFF0E6D7),
    outline = Color(0xFFB8A79B),
    outlineVariant = Color(0xFFEADFD3),
)

internal val DarkColors = darkColorScheme(
    primary = BrandOrangeLight,
    onPrimary = Color(0xFF5C1600),
    primaryContainer = Color(0xFF7F2A10),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFFFB68F),
    onSecondary = Color(0xFF522200),
    secondaryContainer = Color(0xFF4A3B10),
    onSecondaryContainer = Cream,
    tertiary = Color(0xFF7DDB9A),
    onTertiary = Color(0xFF00391A),
    tertiaryContainer = Color(0xFF0E5229),
    onTertiaryContainer = Color(0xFFDCF5E3),
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkBackground,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = DarkInkMuted,
    surfaceContainerLowest = Color(0xFF16110E),
    surfaceContainerLow = Color(0xFF241C18),
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = Color(0xFF362C26),
    surfaceContainerHighest = Color(0xFF423731),
    outline = Color(0xFF9F8D83),
    outlineVariant = Color(0xFF52443C),
)

/** Colours outside the Material scheme. */
@Immutable
data class ScorengeColors(
    /** Brand accent for large headings and icons (not for small text on light backgrounds). */
    val brand: Color,
    /** Card fill for highlighted cards. */
    val cream: Color,
    val onCream: Color,
)

private val LightScorengeColors = ScorengeColors(brand = BrandOrange, cream = Cream, onCream = Color(0xFF3D2E00))
private val DarkScorengeColors = ScorengeColors(brand = BrandOrangeLight, cream = Color(0xFF3A3020), onCream = Cream)

val LocalScorengeColors = staticCompositionLocalOf { LightScorengeColors }

/** Scorenge theme. Dynamic (wallpaper) colour is intentionally off to keep the brand. */
@Composable
fun ScorengeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalScorengeColors provides if (darkTheme) DarkScorengeColors else LightScorengeColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = ScorengeTypography,
            shapes = ScorengeShapes,
            content = content,
        )
    }
}

/** Shortcut: `ScorengeTheme.colors.brand`. */
object ScorengeTheme {
    val colors: ScorengeColors
        @Composable get() = LocalScorengeColors.current
}
