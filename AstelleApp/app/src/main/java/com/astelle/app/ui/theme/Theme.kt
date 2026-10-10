package com.astelle.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
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
    // M3 的 surfaceContainer* 默认走基线紫色调。不显式覆盖的话，
    // 所有用它们的组件（AlertDialog / DropdownMenu / BottomSheet /
    // Snackbar）都会发紫，跟暖纸色板打架。
    surfaceDim = PaperWarm,
    surfaceBright = Paper,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = PaperWarm,
    surfaceContainerHigh = PaperWarm,
    surfaceContainerHighest = PaperWarm,
    // 关掉 M3 在浮层上叠的那层色调，否则暖纸会被染成别的颜色
    surfaceTint = Color.Transparent,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = Accent,
    scrim = Ink,
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
    surfaceDim = Ink,
    surfaceBright = InkSoft,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Ink,
    surfaceContainer = InkSoft,
    surfaceContainerHigh = InkSoft,
    surfaceContainerHighest = InkSoft,
    surfaceTint = Color.Transparent,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    inversePrimary = Accent,
    scrim = Ink,
    outline = Muted,
    outlineVariant = InkSoft,
    error = Danger,
    onError = Paper,
)

/**
 * 品牌主题。默认使用 Astelle 色板（品牌色永不被覆盖）；
 * 设置里显式打开「动态取色」且 API 31+ 时，才把 Material You 的动态色
 * 映射进 [AstelleColors]（10-10 莫奈接口）。
 */
@Composable
fun AstelleTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // 色板桥（10-10）：静态 token 是计算属性，跟着这个开关走 ——
    // 全项目调用点零改动。必须在取色之前设，本帧就取对
    setPaletteDark(darkTheme)

    // 莫奈取色槽：null = 品牌色原样（默认）。映射纪律：
    // 只换「Accent 家族 + 面 + 墨梯度」，浅底类从 primary/surface 现算保持同一关系
    val context = LocalContext.current
    setDynamicPalette(
        if (dynamicColor && android.os.Build.VERSION.SDK_INT >= 31) {
            val m3 = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            AstelleColors(
                paper = m3.surface,
                paperWarm = lerp(m3.surface, m3.onSurface, 0.05f),
                divider = m3.outlineVariant,
                ink = m3.onSurface,
                inkSoft = lerp(m3.onSurface, m3.surface, 0.15f),
                muted = lerp(m3.onSurface, m3.surface, 0.42f),
                ghost = lerp(m3.onSurface, m3.surface, 0.58f),
                accent = m3.primary,
                accentMist = lerp(m3.surface, m3.primary, 0.08f),
                surfaceFloat = lerp(m3.surface, m3.primary, 0.042f),
                danger = m3.error,
                dangerBg = lerp(m3.surface, m3.error, 0.08f),
            )
        } else {
            null
        },
    )

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    // token 已是目标主题的值，AstelleColors() 的默认参数自动取对
    val astelleColors = AstelleColors()

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
