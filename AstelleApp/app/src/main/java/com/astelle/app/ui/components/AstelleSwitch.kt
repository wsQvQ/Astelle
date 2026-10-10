package com.astelle.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.theme.LocalAstelleColors
import kotlin.math.roundToInt

private val TrackWidth = 44.dp
private val TrackHeight = 26.dp
private val ThumbSize = 20.dp
private val ThumbInset = 3.dp

/**
 * Astelle 自绘开关（10-11 用户：M3 默认开关"很生硬"，抄高审美那挂的手感）。
 *
 * 动效配方：
 *  - 滑块**弹簧滑行**（阻尼 0.62，略带过冲的"活"劲），位置与轨道换色同一个进度驱动
 *  - 拨动瞬间滑块 **pop 一下**（1.18 → 1.0 高频弹簧）——iOS 拨杆那个"啵"
 *  - 按住时滑块微缩 0.88（触觉反馈的视觉版），松手回弹
 *  - 边界仍走描边（产品口径）：轨道发丝线，滑块 Paper 圆片带高光边；
 *    拨到开态滑块染成 Accent、描边淡出
 */
@Composable
fun AstelleSwitch(
    on: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalAstelleColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // 拨杆进度（0/1）：位置+颜色一根轴
    val progress = remember { Animatable(if (on) 1f else 0f) }
    // pop：只在"真的被拨动"时跳一下，首次登场不闪
    val pop = remember { Animatable(1f) }
    var firstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(on) {
        progress.animateTo(
            if (on) 1f else 0f,
            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        )
    }
    LaunchedEffect(on) {
        if (firstComposition) {
            firstComposition = false
        } else {
            pop.snapTo(1.18f)
            pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessHigh))
        }
    }

    val pressScale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh),
        label = "switchPress",
    )
    val travelPx = with(LocalDensity.current) {
        (TrackWidth - ThumbSize - ThumbInset * 2).toPx()
    }
    val shape = RoundedCornerShape(999.dp)

    Box(
        Modifier
            .size(width = TrackWidth, height = TrackHeight)
            .toggleable(
                value = on,
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onValueChange = onCheckedChange,
            )
    ) {
        // 轨道：PaperWarm ↔ AccentMist 随进度换血 + 发丝描边
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(lerp(colors.paperWarm, colors.accentMist, progress.value))
                .border(
                    1.dp,
                    lerp(colors.divider, colors.accent.copy(alpha = 0.35f), progress.value),
                    shape,
                )
        )
        // 滑块：弹簧位移 + pop + 按压微缩 + 2dp 轻投影
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = ThumbInset)
                .offset { IntOffset((progress.value * travelPx).roundToInt(), 0) }
                .size(ThumbSize)
                .graphicsLayer {
                    val s = pop.value * pressScale
                    scaleX = s
                    scaleY = s
                    shadowElevation = 2.dp.toPx()
                    this.shape = shape
                    clip = true
                }
                .background(lerp(colors.paper, colors.accent, progress.value))
                .border(1.dp, colors.divider.copy(alpha = 1f - progress.value), shape)
        )
    }
}
