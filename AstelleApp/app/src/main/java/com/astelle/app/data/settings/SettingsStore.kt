package com.astelle.app.data.settings

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/** 颜色模式（设置页「通用设置」，用户 10-10 指定三选项：浅色 / 深色 / 跟随系统） */
enum class ColorMode { LIGHT, DARK, SYSTEM }

/**
 * 偏好存储 v1：`SharedPreferences`（零新依赖 —— 构建离线，别引 DataStore）。
 *
 * 写入时同步 [ThemeModeHolder]，主题**选完即换**，不用重启。
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("astelle_settings", Context.MODE_PRIVATE)

    var colorMode: ColorMode
        get() = runCatching {
            ColorMode.valueOf(prefs.getString(KEY_COLOR_MODE, ColorMode.SYSTEM.name).orEmpty())
        }.getOrDefault(ColorMode.SYSTEM)
        set(value) {
            prefs.edit().putString(KEY_COLOR_MODE, value.name).apply()
            ThemeModeHolder.mode.value = value
        }

    /**
     * 返回手势开关（10-10：接口先留，设置页的开关行下一批补）。
     * 预测性返回（Predictive Back）的 manifest 开关已就位；
     * 这个偏好以后接「手势返回」的启停。
     */
    var backGestureEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACK_GESTURE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_BACK_GESTURE, value).apply()
        }

    /** 动态取色（莫奈）开关。默认关 = 品牌色；API 31+ 才真的生效 */
    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
            ThemeModeHolder.dynamicColor.value = value
        }

    /** 卡片默认密度：true = 默认收起（只留标题+日期） */
    var compactCardsDefault: Boolean
        get() = prefs.getBoolean(KEY_COMPACT_DEFAULT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_COMPACT_DEFAULT, value).apply()
        }

    /** 图片压缩档位（10-10）：标准 / 高质量 / 省空间 */
    var imageQuality: ImageQuality
        get() = runCatching {
            ImageQuality.valueOf(prefs.getString(KEY_IMAGE_QUALITY, ImageQuality.STANDARD.name).orEmpty())
        }.getOrDefault(ImageQuality.STANDARD)
        set(value) {
            prefs.edit().putString(KEY_IMAGE_QUALITY, value.name).apply()
        }

    /**
     * 玻璃材质开关（10-11 用户定稿作用域：菜单/浮层 + 悬浮工具栏）。
     * 渲染＝自研轻玻璃（快照/图层 + RenderEffect 模糊 + 高光，零新依赖）；
     * 关着 = 原纸感，零额外开销。写入同步 [GlassModeHolder] 实时生效。
     */
    var glassMenus: Boolean
        get() = prefs.getBoolean(KEY_GLASS_MENUS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_GLASS_MENUS, value).apply()
            GlassModeHolder.enabled.value = value
        }

    private companion object {
        const val KEY_COLOR_MODE = "colorMode"
        const val KEY_BACK_GESTURE = "backGestureEnabled"
        const val KEY_DYNAMIC_COLOR = "dynamicColor"
        const val KEY_COMPACT_DEFAULT = "compactCardsDefault"
        const val KEY_IMAGE_QUALITY = "imageQuality"
        const val KEY_GLASS_MENUS = "glassMenus"
    }
}

/** 图片压缩档位：max 边长 / JPEG 质量（以「不明显变糊」为准的手感档） */
enum class ImageQuality(val maxDim: Int, val quality: Int) {
    HIGH(2400, 92),
    STANDARD(1600, 85),
    SAVING(1280, 78),
}

/** 全局可观察：MainActivity 启动时播种，设置页改它即实时生效 */
object ThemeModeHolder {
    val mode: MutableState<ColorMode> = mutableStateOf(ColorMode.SYSTEM)

    /** 动态取色开关（同上，实时生效） */
    val dynamicColor: MutableState<Boolean> = mutableStateOf(false)
}

/** 玻璃材质开关（10-11）：全局可观察，菜单/浮层/悬浮工具栏实时跟着切 */
object GlassModeHolder {
    val enabled: MutableState<Boolean> = mutableStateOf(false)
}
