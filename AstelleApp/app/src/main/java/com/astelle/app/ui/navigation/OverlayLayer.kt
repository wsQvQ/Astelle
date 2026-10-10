package com.astelle.app.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 一张浮层卡的运动状态（12 号计划：真页面垫底终局）。
 * `slide ∈ [0,1]`：0 = 盖好（满屏），1 = 完全滑出右缘。
 * 下层的视差/压暗都从**上一层的 slide** 推导，所以状态放在卡外面（根层持有）。
 * [initialSlide]：进程恢复（旋转等）时重建的卡应该"已在位"（0），不重放入场动画。
 */
class OverlayState(val dest: AstelleDestination, initialSlide: Float = 1f) {
    val slide = Animatable(initialSlide)
}

/**
 * 浮层卡本体（A 方案：1:1 真推卡）——盖在永生的首页上面。
 *
 * 手感配方（"最好看最高级"版）：
 *  - **跟手 1:1**：左缘手势直贴 `touchX`，右缘手势镜像 `width - touchX`，两条边都真跟
 *  - 推开时卡缩 6% + 圆角 22dp×s（iOS 推卡感）
 *  - 松手提交/取消：**速度继承**的弹簧（把指尖末速度直接喂给动画）——
 *    没有"松手先顿一下再动"的断档（10-11 用户反馈的弹回延迟就是零初速弹簧的慢起步）
 *  - 弹回启动走 `CoroutineStart.UNDISPATCHED`（少等一帧派发）
 *
 * ⚠️ 坑（PredictiveBackHandler 1.9.3 源码验过，手册坑 24/30）：手势**取消**时
 * `job.cancel()` 连坐——`catch (CancellationException)` 里再挂起会立刻再抛，
 * 弹回动画必须丢回外层 scope；**提交**路径协程还活着，收尾动画可就地做。
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
    // 指尖末速度（slide 分数/秒）：松手瞬间喂给弹簧，动作零断档
    var lastTimeNanos by remember { mutableLongStateOf(0L) }
    var lastS by remember { mutableFloatStateOf(0f) }
    var lastVel by remember { mutableFloatStateOf(0f) }
    val slide = state.slide

    // 入场：从右缘滑入盖上活首页（临界阻尼弹簧：顺滑无回弹，iOS 推入质感）；
    // 进程恢复重建的卡（slide 已在 0）不重放
    LaunchedEffect(Unit) {
        if (slide.value > 0.99f) {
            slide.snapTo(1f)
            slide.animateTo(0f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    // 程序性关闭（设置页的返回圆钮等）
    val close: () -> Unit = {
        uiScope.launch {
            slide.animateTo(
                1f,
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
                initialVelocity = lastVel.coerceAtLeast(1f), // 起个去势，别肉
            )
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
                val s = (follow / w).coerceIn(0f, 1f)
                val now = System.nanoTime()
                val dt = (now - lastTimeNanos) / 1_000_000_000f
                if (lastTimeNanos != 0L && dt > 0.0005f) {
                    lastVel = ((s - lastS) / dt).coerceIn(-8f, 8f)
                }
                lastTimeNanos = now
                lastS = s
                slide.snapTo(s)
            }
            // 提交：带着指尖末速度收尾滑出（弹簧接力，不回踢不顿挫）
            slide.animateTo(
                1f,
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
                initialVelocity = lastVel,
            )
            onGone()
        } catch (e: CancellationException) {
            // UNDISPATCHED：当前帧立刻起步（省掉一帧调度等待 = 消灭"顿一下才弹回"）
            reboundJob = uiScope.launch(start = CoroutineStart.UNDISPATCHED) {
                slide.animateTo(
                    0f,
                    spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                    initialVelocity = lastVel,
                )
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
                // clip 下限 0.03：零/微半径 clip 的 AA 边会画出细白线（白线嫌疑二）
                if (s > 0.03f) {
                    shape = RoundedCornerShape(22.dp * s)
                    clip = true
                }
            }
    ) {
        content(close)
    }
}
