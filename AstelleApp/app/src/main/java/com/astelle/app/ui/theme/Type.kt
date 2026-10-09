package com.astelle.app.ui.theme

import androidx.compose.ui.text.font.FontFamily

// 完整的 Typography 定义在 Theme.kt 的 AstelleTypography 里。
// 这里只放两个跨文件共用的字体族快捷方式。

/** 等宽：日期、字数、篇数这类元信息 */
val mono = FontFamily.Monospace

/** 衬线：抽屉头部「Astelle」的品牌字 */
val display = FontFamily.Serif

/**
 * 小胶囊里的文字居中（10-10 修：编辑/预览滑块、1篇、导入、新建、chips
 * 在部分机型/字号缩放下「视觉下沉/上浮」）。
 *
 * 病根：中文字形在行盒里带大 descent，`Alignment.Center` 居中的是**行盒**不是**字**。
 * `Trim.Both + Alignment.Center` 把行盒裁到字面高度再居中 —— 字就真的正了。
 * 所有「固定高度胶囊里的小字」都该带它。
 */
val CenteredLineHeight = androidx.compose.ui.text.style.LineHeightStyle(
    trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
)
