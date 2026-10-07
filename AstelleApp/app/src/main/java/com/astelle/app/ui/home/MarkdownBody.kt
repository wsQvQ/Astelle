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
internal fun MarkdownBody(markdown: String, modifier: Modifier = Modifier) {
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
    )
}
