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

    private companion object {
        const val KEY_COLOR_MODE = "colorMode"
        const val KEY_BACK_GESTURE = "backGestureEnabled"
    }
}

/** 全局可观察的颜色模式：MainActivity 启动时播种，设置页改它即实时生效 */
object ThemeModeHolder {
    val mode: MutableState<ColorMode> = mutableStateOf(ColorMode.SYSTEM)
}
