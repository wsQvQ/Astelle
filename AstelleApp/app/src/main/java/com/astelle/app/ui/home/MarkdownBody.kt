package com.astelle.app.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.Ink
import com.astelle.app.ui.theme.PaperWarm
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.noties.markwon.ext.tables.TableRowSpan

/**
 * 正文的真 Markdown 渲染。编辑器预览与导出图片共用这一份配置 ——
 * 导出的图长得和预览一模一样，是「所见即所得」的一部分。
 *
 * 规格：正文 16sp / 行高 1.75（docs/ui/01-home-screen.md §2.2）
 *
 * 三个刻意的选择：
 *  - linkColor = Accent
 *      链接用品牌橙。下划线保留库默认的开启状态：只靠颜色区分链接，
 *      对色盲用户不友好。
 *  - syntaxHighlightColor
 *      这个参数名有误导性，它实际就是 codeBackgroundColor
 *      （见库的 MardownCorePlugin.configureTheme）。库默认浅灰，
 *      和暖纸色板打架，故换成 PaperWarm。
 *  - enableSoftBreakAddsNewLine
 *      保持库默认的 true。笔记里按一次回车就该换行；若为 false，
 *      多行正文会被 Markdown 规则并成一整段。
 */
@Composable
internal fun MarkdownBody(
    markdown: String,
    modifier: Modifier = Modifier,
    /** 预览模式下可选中复制（用户要的「选中哪段复制哪段」）；导出画布用不到，关掉省事 */
    selectable: Boolean = false,
) {
    MarkdownText(
        markdown = markdown,
        modifier = modifier.fillMaxWidth(),
        linkColor = Accent,
        style = TextStyle(
            color = Ink,
            fontSize = 16.sp,
            lineHeight = 28.sp,
        ),
        syntaxHighlightColor = PaperWarm,
        isTextSelectable = selectable,
        beforeSetMarkdown = { textView, spanned -> fixTableRelayout(textView, spanned) },
    )
}

/**
 * Markwon 表格（`markwon-ext-tables`）的行高 bug：`TableRowSpan` 的单元格排版
 * 在 `draw()` 里才创建，而行高在 `getSize()`（测量阶段）里取 —— 首轮测量时
 * `layouts` 还是空的，行高按**单行**算，单元格一换行就整行叠字（真机上
 * 「费用估算」那张表的说明列就是这么糊的）。
 *
 * 它自带的 `invalidator` 只调 `invalidate()`（重绘）而不 `requestLayout()`（重测），
 * 于是永远停在错误的行高上。这里把 invalidator 换成 `requestLayout()`：
 * 首轮绘制后触发第二轮测量，此时单元格排版已就绪，行高就正了；
 * 第二轮的 `height == maxHeight`，不会再触发，稳定不循环。
 */
private fun fixTableRelayout(textView: android.widget.TextView, spanned: android.text.Spanned) {
    spanned.getSpans(0, spanned.length, TableRowSpan::class.java).forEach { span ->
        span.invalidator { textView.requestLayout() }
    }
}
