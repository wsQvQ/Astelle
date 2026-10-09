package com.astelle.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import io.noties.markwon.image.AsyncDrawable
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Ink
import com.astelle.app.ui.theme.Paper

/**
 * 导出图片用的「屏幕外画布」。
 *
 * 图片宽度**固定 1440 像素**（用户定的，不是 @2x，也不是 2160）：
 * 不管在哪台设备上导出，出来的图都一模一样。做法是给这块子树一份写死的
 * `Density(2f)`，于是 720dp 宽的画布正好 1440px，正文 16sp 也正好 32px。
 * 屏幕真实密度、系统字体缩放都不参与 —— 否则同一份笔记在手机和平板上
 * 导出的比例不同，「1440px」这个承诺就没法兑现。
 *
 * 完整用法见 HomeScreen 里的导出流程，要点：
 *  1. **只在导出那一瞬挂载**（`if (exportCanvasShown)`）。常驻的话，
 *     每敲一个字都要多渲染一遍 Markdown，长笔记会明显卡手；
 *  2. `drawWithContent` 把自己 record 进 [graphicsLayer]；
 *  3. 等两帧再 `toImageBitmap()`：第一帧完成排版 + 绘制（record），
 *     到第二帧时第一帧才真正落定。库文档明确要求 record 先于 toImageBitmap，
 *     只等一帧抓到的会是上一帧甚至一张空图。
 */
@Composable
internal fun ExportImageCanvas(
    title: String,
    content: String,
    graphicsLayer: GraphicsLayer,
    /** 当前切片顶部（px）。整张图不高时传 0 */
    sliceOffsetPx: Float = 0f,
    /** 当前切片高度（px）。0 = 整张 */
    sliceHeightPx: Int = 0,
    /** 画布实际高度（px），抓图方靠它知道要拼多少片 */
    onHeightChanged: (Int) -> Unit = {},
    /** 图片 drawable 的就绪状态出口（⑫）：抓图前要等全部 hasResult */
    onImageDrawables: (List<AsyncDrawable>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalDensity provides EXPORT_DENSITY) {
        Column(
            modifier = modifier
                // requiredWidth 而不是 width：宿主是个 1px 的 Layout，
                // 普通 width 会被父约束压成 0，图就没了
                .requiredWidth(EXPORT_PAGE_WIDTH)
                .onSizeChanged { onHeightChanged(it.height) }
                .drawWithContent {
                    // 按切片 record：translate 把内容挪进切片窗口，每次只录一小片。
                    // 整张录完一次读回会撞 GPU 纹理上限（见 MAX_SLICE_PX）
                    val full = size.height.toInt()
                    val h = if (sliceHeightPx > 0) {
                        minOf(sliceHeightPx, full - sliceOffsetPx.toInt())
                    } else {
                        full
                    }
                    graphicsLayer.record(size = IntSize(size.width.toInt(), h.coerceAtLeast(1))) {
                        translate(0f, -sliceOffsetPx) { this@drawWithContent.drawContent() }
                    }
                    drawLayer(graphicsLayer)
                }
                .background(Paper)
                .padding(horizontal = 72.dp, vertical = 64.dp),
        ) {
            if (title.isNotBlank()) {
                // 26sp = 52px，比正文（32px）大一档半：图片是要拿去分享的页面，
                // 笔记标题得是页面上最重的一行；但也不追到正文 # 一级标题（约 64px）
                // 那么大 —— 那是 Markwon 的2em 规则，抢了页标题会倒挂
                Text(
                    title.trim(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    lineHeight = 36.sp,
                )
                // 标题与正文之间这道发丝线，与编辑器 meta 行下的分隔同一套语言
                Box(
                    Modifier
                        .padding(top = 20.dp, bottom = 24.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Divider.copy(alpha = 0.55f)),
                )
            }
            MarkdownBody(
                markdown = content,
                modifier = Modifier.fillMaxWidth(),
                onImageDrawables = onImageDrawables,
            )
        }
    }
}

/** 导出画布的固定密度：1dp = 2px。改它等于改图片里所有字号 */
private val EXPORT_DENSITY = Density(2f, 1f)

/** 720dp × Density(2f) = 1440px。要改图片宽度只改这里 */
private val EXPORT_PAGE_WIDTH = 720.dp

/** 图片宽度（px），抓图方建输出位图时用 */
internal const val EXPORT_IMAGE_WIDTH_PX = 1440

/**
 * 每次只抓这么高的一片。
 *
 * 长图整张读回内存会撞设备的 GPU 纹理上限（手机常见 8192，平板旗舰 16384）——
 * 真机上 4450 字的图（约 12000px）在手机上 `@copy` 环节抛 NullPointerException，
 * 800 字（约 2500px）没事，就是这个原因。分片后每片都在安全区内，再拼成整图。
 */
internal const val MAX_SLICE_PX = 2048

/**
 * 屏幕外宿主：孩子照常排版、绘制、record，只是被裁在 1px² 里画不出来。
 *
 * 两个不显眼但会炸的点（真机自检时踩过）：
 *  1. **不能给孩子任何尺寸约束**。普通 Box/requiredSize 会把孩子压成宿主的
 *     尺寸，图片高度直接塌掉（实测塌成 1440×2px）。所以这里用自定义 Layout，
 *     以完全无界的约束量孩子。
 *  2. **自身报 1×1px 而不是 0×0**：零面积节点会不会被跳过绘制属于实现细节，
 *     赌不起。这 1px 落在 Paper 底色上，肉眼看不见。
 */
@Composable
internal fun OffscreenCanvasHost(content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = Modifier.clipToBounds(),
    ) { measurables, _ ->
        val placeables = measurables.map { it.measure(Constraints()) }
        layout(1, 1) {
            placeables.forEach { it.place(0, 0) }
        }
    }
}
