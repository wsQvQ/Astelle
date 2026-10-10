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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
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
// 「Astelle 磨砂纸玻璃」配方 —— 全 app 玻璃审美的**唯一权威**。
//
//   ① 厚糊 30dp：背景糊成抽象色块，按钮文字才有主场（毛玻璃=隐约见背景）
//   ② 80% 纸 tint：背景只留 20% 影子，可读性等同普通纸面
//   ③ 纸感噪点 3%：磨砂颗粒 = 玻璃的"指纹"
//   ④ 透镜两缘：顶部镜面高光 + 底部内阴影 = 玻璃片的物理厚度
//   ⑤ 描边 + 顶缘一线高光（玻璃接光那条线）
//
// ⚠️ 血泪坑两则：
//   a) `Modifier.blur` 的 Unbounded = TileMode.Decal，部分 Adreno 驱动上效果
//      变恒等（糊了个寂寞）—— 一律 bounded（Clamp）+ 源 1.12x 饱和放大画。
//   b) 铺满容器（bleed）**只能画笔越界画**，别动布局：matchParentSize 后面再叠
//      布局撑高会被外层强制尺寸吃掉负位放置 = 画层错位、下半截溢出（翻过车）。
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
 * null = 背后是本窗平涂（ViewPill 这类）。
 * [enabled] = false = 玻璃开关关着 → 原纸感（[flatColor] 实底 + 描边）。
 * [bleed] = 玻璃画**出界**多少去铺满容器（菜单 Surface 的 8dp 内边距）——
 * 出界区靠容器自己的 clip 收圆角；bleed>0 时本面板自己不裁。
 * [radius] = 面板圆角（描边/高光/渐变共用；与 [shape] 的圆角保持一致，显式传）。
 */
@Composable
fun GlassPanel(
    shape: Shape,
    modifier: Modifier = Modifier,
    source: GlassSource? = null,
    enabled: Boolean = true,
    flatColor: Color = SurfaceFloat,
    bleed: Dp = 0.dp,
    radius: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalAstelleColors.current
    var panelPos by remember { mutableStateOf(Offset.Zero) }
    val bleedPx = with(LocalDensity.current) { bleed.toPx() }

    Box(
        modifier = modifier
            .then(if (bleedPx > 0f) Modifier else Modifier.clip(shape))
            .onGloballyPositioned { panelPos = it.positionInWindow() }
    ) {
        if (!enabled) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(flatColor)
                    .border(1.dp, Divider.copy(alpha = 0.8f), shape)
            )
            content()
            return@Box
        }

        // ① 厚糊底（bounded=Clamp + 1.12x 饱和放大；只糊内容盒，bleed 带由 tint 盖住）
        if (source != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .blur(GlassBlurRadius)
                    .drawBehind {
                        when (source) {
                            is GlassSource.Snapshot -> {
                                // 整图 1.12x 缩放对准面板中心 = 等效"放大裁切"，
                                // 只用成员 drawImage（drawImageRect 这版解析不了）
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

        // ②③④⑤ tint + 颗粒 + 透镜两缘 + 描边 + 顶缘高光 —— 一个"画出界"的层全包。
        // 圆角矩形画（不是 rect）：bleed 时根不裁，靠画笔自身的圆角兜住四角。
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val b = bleedPx
                    val top = -b
                    val h = size.height + 2f * b
                    val w = size.width
                    val rect = Size(w, h)
                    val rpx = radius.toPx()
                    // ② 纸 tint（含 bleed 带 = 铺满容器）
                    drawRoundRect(
                        color = colors.paper.copy(alpha = GlassTintAlpha),
                        topLeft = Offset(0f, top),
                        size = rect,
                        cornerRadius = CornerRadius(rpx),
                    )
                    // ③ 磨砂颗粒（平铺到 bleed 带；3% 噪点在圆角外溢可忽略）
                    val tile = GlassNoiseTile.width
                    var y = top
                    while (y < top + h) {
                        var x = 0f
                        while (x < w) {
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
                    // ④ 顶部镜面高光（玻璃片上缘接光）
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.14f),
                            0.45f to Color.White.copy(alpha = 0f),
                        ),
                        topLeft = Offset(0f, top),
                        size = rect,
                        cornerRadius = CornerRadius(rpx),
                    )
                    // ④ 底部内阴影（玻璃片的厚度感）
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0.78f to Color.Black.copy(alpha = 0f),
                            1f to Color.Black.copy(alpha = 0.05f),
                        ),
                        topLeft = Offset(0f, top),
                        size = rect,
                        cornerRadius = CornerRadius(rpx),
                    )
                    // ⑤ 发丝描边
                    drawRoundRect(
                        color = Divider.copy(alpha = 0.7f),
                        topLeft = Offset(0f, top),
                        size = rect,
                        cornerRadius = CornerRadius(rpx),
                        style = Stroke(1.dp.toPx()),
                    )
                    // ⑤ 顶缘一线高光（玻璃接光的那条线；两端内缩躲开圆角）
                    val inset = 1.5f + rpx * 0.4f
                    if (w > 2 * inset) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.30f),
                            start = Offset(inset, top + 1f),
                            end = Offset(w - inset, top + 1f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
        )
        content()
    }
}
