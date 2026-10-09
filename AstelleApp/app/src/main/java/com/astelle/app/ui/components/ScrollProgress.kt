package com.astelle.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Ghost
import kotlin.math.roundToInt

/**
 * 阅读 / 滚动进度条（用户 10-10：「符合页风格且不影响阅读」）。
 *
 * 右缘一根 3dp 细轨 + 按比例的圆角拇指：轨道半透明发丝感，拇指安静灰，
 * 不写字不放图标。只在内容真的能滚时淡入，滚到底/短文自动淡出。
 * 手机平板通用 —— 它只依赖 [ScrollState]，贴在任何滚动容器边上都行。
 */
@Composable
internal fun ScrollProgress(
    state: ScrollState,
    modifier: Modifier = Modifier,
) {
    // 视口高度（px）：拇指长度 = 视口 / 内容，轨迹 = 余下行程
    var viewport by remember { mutableIntStateOf(0) }
    val scrollable = state.maxValue > 0
    val alpha by animateFloatAsState(targetValue = if (scrollable) 1f else 0f, label = "scrollProgress")

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(3.dp)
            .alpha(alpha)
            .onSizeChanged { viewport = it.height },
    ) {
        // 轨：发丝感的半透明线
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.dp))
                .background(Divider.copy(alpha = 0.35f)),
        )
        if (viewport > 0 && scrollable) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val thumbFraction = (viewport.toFloat() / (state.maxValue + viewport))
                .coerceIn(0.12f, 1f)
            val offsetFraction = state.value.toFloat() / state.maxValue
            val thumbPx = (viewport * thumbFraction).roundToInt()
            val travelPx = ((viewport - thumbPx) * offsetFraction).roundToInt()
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(with(density) { thumbPx.toDp() })
                    .offset { IntOffset(0, travelPx) }
                    .clip(RoundedCornerShape(2.dp))
                    .background(Ghost),
            )
        }
    }
}
