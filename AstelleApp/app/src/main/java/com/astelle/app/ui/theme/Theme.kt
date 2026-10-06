package com.astelle.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Astelle 扩展色：M3 ColorScheme 装不下的品牌辅助色。
 */
data class AstelleColors(
    val paper: Color = Paper,
    val paperWarm: Color = PaperWarm,
    val divider: Color = Divider,
    val ink: Color = Ink,
    val inkSoft: Color = InkSoft,
    val muted: Color = Muted,
    val ghost: Color = Ghost,
    val accent: Color = Accent,
    val accentMist: Color = AccentMist,
    val surfaceFloat: Color = SurfaceFloat,
    val dangerBg: Color = DangerBg,
    val danger: Color = Danger,
)

val LocalAstelleColors = staticCompositionLocalOf { AstelleColors() }

val AstelleTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Paper,
    primaryContainer = AccentMist,
    onPrimaryContainer = Ink,
    secondary = InkSoft,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperWarm,
    onSurfaceVariant = Muted,
    outline = Ghost,
    outlineVariant = Divider,
    error = Danger,
    onError = Paper,
    errorContainer = DangerBg,
    onErrorContainer = Danger,
)

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    primaryContainer = Accent,
    onPrimaryContainer = Ink,
    secondary = Ghost,
    onSecondary = Ink,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = InkSoft,
    onSurfaceVariant = Ghost,
    outline = Muted,
    outlineVariant = InkSoft,
    error = Danger,
    onError = Paper,
)

/**
 * 品牌主题：始终使用 Astelle 色板，不启用 Material You 动态取色。
 */
@Composable
fun AstelleTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val astelleColors = if (darkTheme) {
        AstelleColors(
            paper = Ink,
            paperWarm = InkSoft,
            divider = InkSoft,
            ink = Paper,
            inkSoft = Ghost,
            muted = Muted,
            ghost = Muted,
            surfaceFloat = InkSoft,
        )
    } else {
        AstelleColors()
    }

    CompositionLocalProvider(LocalAstelleColors provides astelleColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AstelleTypography,
            content = content,
        )
    }
}

object AstelleTheme {
    val colors: AstelleColors
        @Composable @ReadOnlyComposable get() = LocalAstelleColors.current
}
