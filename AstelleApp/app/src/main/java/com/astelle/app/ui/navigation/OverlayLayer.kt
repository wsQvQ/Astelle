package com.astelle.app.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 一张浮层卡的运动状态（12 号计划：真页面垫底终局）。
 * `slide ∈ [0,1]`：0 = 盖好（满屏），1 = 完全滑出右缘。
 * 下层的视差/压暗都从**上一层的 slide** 推导，所以状态放在卡外面（根层持有）。
 */
class OverlayState(val dest: AstelleDestination) {
    val slide = Animatable(1f)
}

/**
 * 浮层卡本体（A 方案：1:1 真推卡）——盖在永生的首页上面。
 *
 * 手感配方（"最好看最高级"版）：
 *  - **跟手 1:1**：左缘手势直贴 `touchX`，右缘手势镜像 `width - touchX`，两条边都真跟
 *  - 推开时卡缩 6% + 圆角 22dp×s（iOS 推卡感）
 *  - 松手提交：160ms FastOutSlowIn 收尾滑出 → [onGone]（父层弹栈）
 *  - 中途取消：0.75 阻尼弹簧**活弹回**
 *  - 程序性关闭（返回钮/三键）：临界阻尼弹簧滑出，与开页同手感
 *
 * ⚠️ 坑（PredictiveBackHandler 1.9.3 源码验过，手册坑 24 族）：手势取消时
 * `job.cancel()` 连坐——`catch (CancellationException)` 里再挂起会立刻再抛，
 * 弹回动画必须丢回外层 scope。提交路径则相反：collect 结束后协程还活着，
 * 收尾动画可以放心就地做。
 */
@Composable
fun OverlayLayer(
    state: OverlayState,
    onGone: () -> Unit,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    val uiScope = rememberCoroutineScope()
    var widthPx by remember { mutableIntStateOf(0) }
    var reboundJob by remember { mutableStateOf<Job?>(null) }
    val slide = state.slide

    // 入场：从右缘滑入盖上活首页（临界阻尼弹簧：顺滑无回弹，iOS 推入质感）
    LaunchedEffect(Unit) {
        slide.snapTo(1f)
        slide.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
    }

    // 程序性关闭（设置页的返回圆钮等）
    val close: () -> Unit = {
        uiScope.launch {
            slide.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
            onGone()
        }
    }

    PredictiveBackHandler(enabled = true) { events ->
        reboundJob?.cancel()
        try {
            events.collect { e ->
                val w = widthPx.toFloat().coerceAtLeast(1f)
                // 1:1 真跟手：左缘取指尖 x，右缘取镜像（往左划也推得出卡）
                val follow =
                    if (e.swipeEdge == BackEventCompat.EDGE_RIGHT) w - e.touchX else e.touchX
                slide.snapTo((follow / w).coerceIn(0f, 1f))
            }
            // 提交：从指尖位置收尾滑出，再卸卡
            slide.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
            onGone()
        } catch (e: CancellationException) {
            reboundJob = uiScope.launch {
                slide.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { widthPx = it.width }
            .graphicsLayer {
                val s = slide.value
                translationX = s * widthPx
                val scale = 1f - s * 0.06f
                scaleX = scale
                scaleY = scale
                if (s > 0f) {
                    shape = RoundedCornerShape(22.dp * s)
                    clip = true
                }
            }
    ) {
        content(close)
    }
}
