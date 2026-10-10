package com.astelle.app.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.theme.LocalAstelleColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 预测返回跟手外衣（10 号计划 §2）——包在日记/计划/设置三个子页外面。
 *
 * 手感参数（起步值，真机再调）：收缩 5% · 跟手右移 24dp · 圆角 18dp · 退后压暗 18%。
 * 取消时 150ms 弹回原位。
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
    val progress = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = true) { events ->
        try {
            // 跟手阶段：snapTo 直贴手指（别用 animateTo，会拖泥带水）
            events.collect { progress.snapTo(it.progress) }
            onBack()
        } catch (e: CancellationException) {
            uiScope.launch { progress.animateTo(0f, tween(150)) }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.paperWarm)
            // 背景退后一层：ink 明暗双相（亮色=墨压暗、暗色=纸提亮），两头通吃
            .drawBehind { drawRect(colors.ink.copy(alpha = progress.value * 0.18f)) }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value
                    val scale = 1f - p * 0.05f
                    scaleX = scale
                    scaleY = scale
                    translationX = p * 24.dp.toPx()
                    shape = RoundedCornerShape(18.dp * p)
                    clip = true
                    shadowElevation = p * 12.dp.toPx()
                }
        ) {
            content()
        }
    }
}
