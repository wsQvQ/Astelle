package com.astelle.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 大屏判定 —— **一把尺两处用**：工具栏对齐（大屏靠右）和平板常驻侧栏。
 *
 * 规则取自 RikkaHub 的大屏分支：宽 > 高 且 ≥ 1100dp。改尺子只改这里。
 */
@Composable
internal fun isLargeScreen(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp > config.screenHeightDp && config.screenWidthDp >= 1100
}
