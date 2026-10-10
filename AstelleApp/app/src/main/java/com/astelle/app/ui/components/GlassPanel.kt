package com.astelle.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.LocalAstelleColors
import com.astelle.app.ui.theme.SurfaceFloat
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 玻璃背景源（10-11）：三类浮层共用一套配方，源只有两种 + 无源（背后是本窗平涂）。
 */
sealed interface GlassSource {
    /** 窗口快照（菜单/对话框这类独立弹窗用）：坐标原点 = 窗口 (0,0) */
    class Snapshot(val image: ImageBitmap) : GlassSource

    /** 实时图层（悬浮工具栏这类同窗浮层用）：坐标原点 = [origin]（窗口坐标） */
    class Layer(val layer: GraphicsLayer, val origin: Offset) : GlassSource
}

// ═══════════════════════════════════════════════════════════════════════════
// 「Astelle 磨砂纸玻璃」配方 —— 全 app 玻璃审美的**唯一权威**（10-11 重设计）。
//
// 设计语言：毛玻璃要「隐隐约约看到背景」，不能全透、也不能花到抢戏——
//   ① 厚糊：背景糊成抽象色块（30dp），上面的按钮文字才有主场
//   ② 80% 纸 tint：背景只留 20% 影子，可读性等同普通纸面
//   ③ 纸感噪点（3%）：磨砂颗粒 = 玻璃的"指纹"，也是 Astelle 纸感身份的延续
//   ④ 透镜两缘：顶部镜面高光 + 底部内阴影 = 玻璃片的物理厚度
//   ⑤ 边缘：发丝描边 + **顶缘一线高光**（玻璃接光那条线）
//
// ⚠️ 血泪坑（10-11 实测「模糊全部没生效」）：`Modifier.blur` 的
//   `BlurredEdgeTreatment.Unbounded` = TileMode.Decal —— 部分 Adreno 驱动上
//   Decal 的 RenderEffect 直接变恒等（糊了个寂寞）。**一律用默认 Rectangle
//   （Clamp）**；再配 1.12x 饱和放大画源，Clamp 边缘采样也不发灰。
// ═══════════════════════════════════════════════════════════════════════════
private val GlassBlurRadius = 30.dp
private const val GlassTintAlpha = 0.80f
private const val GlassGrainAlpha = 0.03f
private const val GlassSourceZoom = 1.12f

/** 128px 磨砂噪点瓦片（白噪 + 随机透明度），全局一张，别每次生成 */
private val GlassNoiseTile: ImageBitmap by lazy {
    val size = 128
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val rnd = Random(20261011)
    val pixels = IntArray(size * size) { i ->
        val v = rnd.nextInt(256)
        val a = rnd.nextInt(256)
        (a shl 24) or (v shl 16) or (v shl 8) or v
    }
    bmp.setPixels(pixels, 0, size, 0, 0, size, size)
    bmp.asImageBitmap()
}

/**
 * 玻璃面板：[source] 非空 = 背后有可糊的内容（快照/实时图层）；
 * null = 背后是本窗平涂（ViewPill 这类），只上 tint/颗粒/透镜缘。
 * [enabled] = false = 玻璃开关关着 → 原纸感（[flatColor] 实底 + 发丝描边），零花活。
 */
@Composable
fun GlassPanel(
    shape: Shape,
    modifier: Modifier = Modifier,
    source: GlassSource? = null,
    enabled: Boolean = true,
    flatColor: Color = SurfaceFloat,
    bleed: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalAstelleColors.current
    var panelPos by remember { mutableStateOf(Offset.Zero) }
    val bleedPx = with(LocalDensity.current) { bleed.toPx() }
    // bleed>0 = 玻璃要**溢出内容盒**铺满容器（菜单 Surface 有 8dp 内边距——
    // 不铺满就是"卡中卡"割裂，用户实拍的丑就是它）。圆角交给容器自己的 clip。
    val expand: Modifier =
        if (bleedPx > 0f) {
            Modifier.layout { measurable, constraints ->
                val p = measurable.measure(constraints)
                layout(p.width, (p.height + 2 * bleedPx).roundToInt()) {
                    p.place(0, -bleedPx.roundToInt())
                }
            }
        } else {
            Modifier
        }

    Box(
        modifier = modifier
            .then(if (bleedPx > 0f) Modifier else Modifier.clip(shape))
            .onGloballyPositioned { panelPos = it.positionInWindow() }
    ) {
        if (!enabled) {
            Box(
                Modifier
                    .matchParentSize()
                    .then(expand)
                    .background(flatColor)
                    .border(1.dp, Divider.copy(alpha = 0.8f), shape)
            )
            content()
            return@Box
        }
        if (source != null) {
            // ① 厚糊底（bounded = Clamp；源画 1.12x，边缘不发灰）
            Box(
                Modifier
                    .matchParentSize()
                    .then(expand)
                    .blur(GlassBlurRadius)
                    .drawBehind {
                        when (source) {
                            is GlassSource.Snapshot -> {
                                // 整图 1.12x 缩放、对准面板中心 = 等效"放大裁切"，
                                // 只用成员 drawImage（drawImageRect 这版解析不了，坑）
                                val img = source.image
                                val z = GlassSourceZoom
                                drawImage(
                                    img,
                                    dstOffset = IntOffset(
                                        (size.width / 2f - (panelPos.x + size.width / 2f) * z).roundToInt(),
                                        (size.height / 2f - (panelPos.y + size.height / 2f) * z).roundToInt(),
                                    ),
                                    dstSize = IntSize(
                                        (img.width * z).roundToInt(),
                                        (img.height * z).roundToInt(),
                                    ),
                                )
                            }
                            is GlassSource.Layer -> {
                                withTransform({
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    translate(cx, cy)
                                    scale(GlassSourceZoom, GlassSourceZoom)
                                    translate(
                                        -(panelPos.x - source.origin.x) - cx,
                                        -(panelPos.y - source.origin.y) - cy,
                                    )
                                }) {
                                    drawLayer(source.layer)
                                }
                            }
                        }
                    }
            )
        }
        // ② 纸 tint（背景只留 20% 影子）③ 磨砂颗粒 ④ 透镜两缘
        Box(
            Modifier
                .matchParentSize()
                .then(expand)
                .background(colors.paper.copy(alpha = GlassTintAlpha))
                .drawBehind {
                    // 纸感磨砂颗粒（平铺瓦片）
                    val tile = GlassNoiseTile.width
                    var y = 0f
                    while (y < size.height) {
                        var x = 0f
                        while (x < size.width) {
                            drawImage(
                                GlassNoiseTile,
                                dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
                                dstSize = IntSize(tile, tile),
                                alpha = GlassGrainAlpha,
                            )
                            x += tile
                        }
                        y += tile
                    }
                    // 顶部镜面高光（玻璃片上缘接光）
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.14f),
                            0.45f to Color.White.copy(alpha = 0f),
                        ),
                    )
                    // 底部内阴影（玻璃片的厚度感）
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.78f to Color.Black.copy(alpha = 0f),
                            1f to Color.Black.copy(alpha = 0.05f),
                        ),
                    )
                }
                .border(1.dp, Divider.copy(alpha = 0.7f), shape)
        )
        // ⑤ 顶缘一线高光（玻璃接光的那条线，玻璃感的灵魂）
        Box(
            Modifier
                .matchParentSize()
                .then(expand)
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.30f),
                        start = Offset(1.5f, 1f),
                        end = Offset(size.width - 1.5f, 1f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
        )
        content()
    }
}
