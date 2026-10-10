package com.astelle.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.diary.DiaryScreen
import com.astelle.app.ui.home.HomeRoute
import com.astelle.app.ui.plans.PlansScreen
import com.astelle.app.ui.settings.SettingsScreen
import com.astelle.app.ui.theme.LocalAstelleColors

/**
 * 应用根壳（12 号计划：**首页永生 + 子页浮层化** = 预测返回终局，A 方案）。
 *
 * 以前是 NavHost 换页：离开首页 = 首页组合销毁，返回 = 重组合（闪烁根源）。
 * 现在首页**永远活着**，日记/计划/设置是叠在上面的浮层卡（[OverlayLayer]）：
 *  - 预测返回跟手推卡，露出的是一直活着的**真页面**（零闪烁，快照退役）
 *  - 开页 = 卡从右缘滑入 + 首页轻视差左移（盖住期间缩到 0.97、压暗 10%）
 *  - 卡的左缘一道**柔影带**跟着走（深度感，比逐帧 elevation 阴影便宜得多）
 *  - 返回键三层优先级天然成立：浮层（OverlayLayer 的 handler，注册最晚）>
 *    抽屉（HomeRoute 里的 BackHandler）> 系统退出
 *
 * 同类页（日记/计划/设置）互斥不叠；子页状态由 SaveableStateProvider 保住
 * （对齐旧 restoreState 行为）；首页状态（抽屉/滚动/编辑态）从此不可能丢。
 */
@Composable
fun AstelleRoot() {
    val colors = LocalAstelleColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val saveableStateHolder = rememberSaveableStateHolder()

    // 浮层栈（同类互斥，实际深度 ≤ 1；留栈是给未来「设置 → 二级页」嵌套用）
    val overlays = remember { mutableStateListOf<OverlayState>() }
    val currentDestination = overlays.lastOrNull()?.dest ?: AstelleDestination.Home

    // 开卡：收键盘清焦点（编辑器的光标别留在卡背后闪）
    val push: (AstelleDestination) -> Unit = { dest ->
        if (dest != currentDestination) {
            keyboard?.hide()
            focusManager.clearFocus(force = true)
            overlays.clear()
            overlays.add(OverlayState(dest))
        }
    }
    // 「回到开着侧栏的主页」：收卡即可——首页永生，抽屉压根没关过
    val openDrawerThenHome: () -> Unit = { overlays.clear() }

    Box(Modifier.fillMaxSize()) {
        // ── 永生首页层 ──（被盖住时轻视差：左移 10%宽 + 缩 0.97，卡退时归位）
        val topSlide = overlays.lastOrNull()?.slide?.value ?: 1f
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val cover = 1f - topSlide
                    translationX = -0.10f * size.width * cover
                    val sc = 0.97f + 0.03f * topSlide
                    scaleX = sc
                    scaleY = sc
                }
        ) {
            HomeRoute(
                currentDestination = currentDestination,
                onNavigate = push,
            )
        }

        // ── 压暗幕 + 卡片左缘柔影带（垫在首页和卡之间）──
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val s = overlays.lastOrNull()?.slide?.value ?: 1f
                    if (s < 1f) {
                        drawRect(colors.ink.copy(alpha = 0.10f * (1f - s)))
                        val edgeX = s * size.width
                        val band = 64.dp.toPx().coerceAtMost(edgeX)
                        if (band > 0f) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to colors.ink.copy(alpha = 0f),
                                    1f to colors.ink.copy(alpha = 0.16f * (1f - s)),
                                    startX = edgeX - band,
                                    endX = edgeX,
                                ),
                                topLeft = Offset(edgeX - band, 0f),
                                size = Size(band, size.height),
                            )
                        }
                    }
                }
        )

        // ── 浮层卡 ──（自身跟手位移在 OverlayLayer 里；这层只管被上层盖时的视差）
        overlays.forEachIndexed { index, overlay ->
            val above = overlays.getOrNull(index + 1)?.slide?.value
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val cover = 1f - (above ?: 1f)
                        translationX = -0.10f * size.width * cover
                        val sc = 0.97f + 0.03f * (above ?: 1f)
                        scaleX = sc
                        scaleY = sc
                    }
            ) {
                OverlayLayer(
                    state = overlay,
                    onGone = { overlays.remove(overlay) },
                ) { close ->
                    saveableStateHolder.SaveableStateProvider(key = overlay.dest.route) {
                        when (overlay.dest) {
                            AstelleDestination.Home -> Unit // 不会发生：首页永生
                            AstelleDestination.Diary -> DiaryScreen(onOpenDrawer = openDrawerThenHome)
                            AstelleDestination.Plans -> PlansScreen(onOpenDrawer = openDrawerThenHome)
                            AstelleDestination.Settings -> SettingsScreen(
                                onBack = close,
                                onOpenDrawer = openDrawerThenHome,
                            )
                        }
                    }
                }
            }
        }
    }
}
