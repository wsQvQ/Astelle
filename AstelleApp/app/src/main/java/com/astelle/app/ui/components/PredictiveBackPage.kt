package com.astelle.app.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.theme.LocalAstelleColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 返回途中垫在下面的「上一页快照」栈（10-11 用户：RikkaHub 预测返回能看到**返回后的页面**）。
 *
 * 做法＝**快照**而非实时组合下一页：导航离开时拍一张窗口图压栈（AstelleNavHost），
 * 返回时弹出。视觉上与"真页面"在几百毫秒的手势里无法区分，代价只是一次廉价
 * GPU blit —— 实时组合下一页 = 两棵组合树同帧渲染，那才是性能灾难。
 */
object NavBackdrops {
    private val stack: SnapshotStateList<ImageBitmap> = mutableStateListOf()

    /** 当前返回目标（= 栈顶）；没有快照时 PredictiveBackPage 走纯色兜底 */
    val top: ImageBitmap?
        get() = stack.lastOrNull()

    fun push(bitmap: ImageBitmap) {
        stack.add(bitmap)
        while (stack.size > 3) stack.removeAt(0) // 只留最近几层，防内存爬升
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun clear() = stack.clear()
}

/**
 * 预测返回跟手外衣（10 号计划 §2，10-11 二次调校）——包在日记/计划/设置外面。
 *
 * 手感参数：收缩 5% · 跟手右移 24dp · 圆角 18dp；取消 150ms 弹回。
 * 性能口径（10-11 用户反馈「返回有点卡」后复检）：
 *  - 跟手读数走裸 `mutableFloatStateOf`（Animatable 的 Mutex/snapTo 是逐帧开销）；
 *  - **拆掉逐帧 shadowElevation**（阴影 = 每帧额外渲染通道，卡顿头号嫌疑）；
 *  - 背后垫 NavBackdrops 快照（drawImage 硬件 blit），不再逐帧重画渐变蒙版。
 *
 * ⚠️ 坑（PredictiveBackHandler 1.9.3 源码验过）：手势取消时 `OnBackInstance.cancel()`
 * 会连 `job.cancel()` 一起按——`catch (CancellationException)` 里再挂起会立刻再抛，
 * 回弹动画必须丢回还活着的 `rememberCoroutineScope`。
 */
@Composable
fun PredictiveBackPage(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalAstelleColors.current
    val uiScope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var reboundJob by remember { mutableStateOf<Job?>(null) }

    PredictiveBackHandler(enabled = true) { events ->
        reboundJob?.cancel()
        try {
            // 跟手阶段：直接写状态贴手指（零中间层）
            events.collect { progress = it.progress }
            onBack()
        } catch (e: CancellationException) {
            reboundJob = uiScope.launch {
                animate(
                    initialValue = progress,
                    targetValue = 0f,
                    animationSpec = tween(150),
                ) { value, _ -> progress = value }
            }
        }
    }

    val backdrop = NavBackdrops.top
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.paperWarm)
            .drawBehind {
                if (backdrop != null) {
                    // 返回目标的真身快照（缩放铺满）
                    drawImage(backdrop, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                }
                // 叠一层随手指加深的薄压暗（ink 明暗双相：亮色=墨压、暗色=纸提，两头通吃）
                val dim = if (backdrop != null) 0.10f else 0.18f
                drawRect(colors.ink.copy(alpha = progress * dim))
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress
                    val scale = 1f - p * 0.05f
                    scaleX = scale
                    scaleY = scale
                    translationX = p * 24.dp.toPx()
                    if (p > 0f) {
                        shape = RoundedCornerShape(18.dp * p)
                        clip = true
                    }
                }
        ) {
            content()
        }
    }
}
