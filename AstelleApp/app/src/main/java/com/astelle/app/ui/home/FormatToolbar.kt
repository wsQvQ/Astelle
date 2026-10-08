package com.astelle.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.ui.components.MenuRow
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.AccentMist
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Muted
import com.astelle.app.ui.theme.SurfaceFloat

/** 工具栏动作：包住 / 行前缀 / 插模板 / 待办打勾。全是纯数据，落到 MarkdownEditing 里执行 */
sealed interface FormatAction {
    data class Wrap(val open: String, val close: String = open) : FormatAction
    data class LinePrefix(val prefix: String) : FormatAction
    data class Insert(val snippet: String, val caret: Int = snippet.length) : FormatAction
    data object ToggleTask : FormatAction
}

private data class Tool(
    val label: String,
    val icon: ImageVector? = null,   // 有 icon 用 icon，没有用 label 当文字按钮（H1/H2/H3）
    val action: FormatAction,
)

/**
 * 键盘上沿的格式工具栏。
 *
 * 形态取自小米笔记那条（浮在键盘上沿的圆角胶囊），**格式不藏二级**（Markdown
 * 编辑器里 80% 的操作就是格式）；颜色走我们自己的纸色系：暖米底 + 发丝线 + 静音图标。
 *
 * 一行横向滚动 + 分组竖线；「⋯」收着 5 个低频项。平板宽度够时一行全展开不滚。
 */
@Composable
internal fun FormatToolbar(
    onAction: (FormatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember {
        listOf(
            // 高频 8 个：标题 + 强调 + 块 + 插入
            listOf(
                Tool("H1", action = FormatAction.LinePrefix("# ")),
                Tool("B", Icons.Outlined.FormatBold, FormatAction.Wrap("**")),
                Tool("I", Icons.Outlined.FormatItalic, FormatAction.Wrap("*")),
                Tool("列表", Icons.AutoMirrored.Outlined.FormatListBulleted, FormatAction.LinePrefix("- ")),
                Tool("任务", Icons.Outlined.TaskAlt, FormatAction.ToggleTask),
                Tool("引用", Icons.Outlined.FormatQuote, FormatAction.LinePrefix("> ")),
                Tool("代码", Icons.Outlined.Code, FormatAction.Wrap("`")),
                Tool("链接", Icons.Outlined.Link, FormatAction.Insert("[]()", 1)),
            ),
        )
    }
    val overflow = remember {
        listOf(
            Tool("H2", action = FormatAction.LinePrefix("## ")),
            Tool("H3", action = FormatAction.LinePrefix("### ")),
            Tool("有序列表", Icons.Outlined.FormatListNumbered, FormatAction.LinePrefix("1. ")),
            Tool("删除线", Icons.Outlined.FormatStrikethrough, FormatAction.Wrap("~~")),
            Tool("代码块", Icons.Outlined.DataObject, FormatAction.Insert("\n```\n\n```\n", 6)),
            Tool("表格", Icons.Outlined.TableChart, FormatAction.Insert(TABLE_TEMPLATE, TABLE_CARET)),
            Tool("分割线", Icons.Outlined.HorizontalRule, FormatAction.Insert("\n---\n", 4)),
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceFloat)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            groups.forEachIndexed { index, group ->
                if (index > 0) item(key = "sep$index") { ToolSeparator() }
                items(group, key = { it.label }) { tool ->
                    ToolButton(tool.label, tool.icon) { onAction(tool.action) }
                }
            }
        }
        ToolSeparator()
        var open by remember { mutableStateOf(false) }
        Box {
            ToolButton("⋯", Icons.Outlined.MoreHoriz) { open = true }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = SurfaceFloat,
                border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                overflow.forEach { tool ->
                    MenuRow(
                        label = tool.label,
                        icon = tool.icon ?: Icons.Outlined.Code,
                    ) {
                        open = false
                        onAction(tool.action)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolSeparator() {
    Box(
        Modifier
            .padding(vertical = 12.dp)
            .width(1.dp)
            .height(24.dp)
            .background(Divider),
    )
}

/** 44dp 触控、20dp 图标、静音色、按压暖底 —— 和顶栏按钮同一套手感 */
@Composable
private fun ToolButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) AccentMist else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, tint = Muted, modifier = Modifier.size(20.dp))
        } else {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Accent)
        }
    }
}

/** 2×2 表格骨架：表头是占位文字，光标落在第一个空单元格里，打字即填 */
internal val TABLE_TEMPLATE = "\n| 列1 | 列2 |\n|---|---|\n|  |  |\n"

internal val TABLE_CARET: Int = TABLE_TEMPLATE.indexOf("\n|  ") + 3
