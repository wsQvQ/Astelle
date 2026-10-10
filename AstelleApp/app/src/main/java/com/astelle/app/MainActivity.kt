package com.astelle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.astelle.app.data.settings.ColorMode
import com.astelle.app.data.settings.GlassModeHolder
import com.astelle.app.data.settings.SettingsStore
import com.astelle.app.data.settings.ThemeModeHolder
import com.astelle.app.ui.navigation.AstelleRoot
import com.astelle.app.ui.theme.AstelleTheme
import com.astelle.app.ui.theme.LocalAstelleColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 颜色模式播种（10-10）：设置页改的是 ThemeModeHolder，主题实时跟着走
        ThemeModeHolder.mode.value = SettingsStore(this).colorMode
        ThemeModeHolder.dynamicColor.value = SettingsStore(this).dynamicColor
        GlassModeHolder.enabled.value = SettingsStore(this).glassMenus
        setContent {
            val mode = ThemeModeHolder.mode.value
            val systemDark = isSystemInDarkTheme()
            AstelleTheme(
                darkTheme = when (mode) {
                    ColorMode.LIGHT -> false
                    ColorMode.DARK -> true
                    ColorMode.SYSTEM -> systemDark
                },
                dynamicColor = ThemeModeHolder.dynamicColor.value,
            ) {
                val colors = LocalAstelleColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colors.paper,
                ) {
                    AstelleRoot()
                }
            }
        }
    }
}
