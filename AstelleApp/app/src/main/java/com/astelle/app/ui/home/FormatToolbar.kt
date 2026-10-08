package com.astelle.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
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
import androidx.compose.ui.window.PopupProperties
import com.astelle.app.ui.components.MenuRow
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.AccentMist
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Muted
import com.astelle.app.ui.theme.Paper
import com.astelle.app.ui.theme.SurfaceFloat

/** 工具栏动作：全是纯数据，落到 MarkdownEditing 里执行 */
sealed interface FormatAction {
    data class Wrap(val open: String, val close: String = open) : FormatAction
    data class LinePrefix(val prefix: String) : FormatAction
    data class Insert(val snippet: String, val caret: Int) : FormatAction
    data object ToggleTask : FormatAction
}

/**
 * 工具栏的**接口层**：条上放什么、怎么分组，全是数据，UI 只负责画。
 *
 * 用户说了「后续将会有大变动」，所以别把按钮写死在 Row 里 ——
 * 要换排布/加组/换形态（横条、竖条、浮动球……）只改这里的数据和一两个布局函数。
 */
data class ToolbarTool(
    val label: String,
    val icon: ImageVector? = null,   // 没有 icon 就用 label 当文字键（H1/H2/H3）
    val action: FormatAction,
)

/** 一组工具，组与组之间用竖线分开 */
data class ToolbarGroup(val tools: List<ToolbarTool>)

/**
 * 横着的感叹号：一条横向工具条（感叹号的竖） + 右边一颗圆球（感叹号的点）。
 *
 * - **条**：文字格式，分组 + 竖线 —— H1/H2/H3 一组、B/I/U/删除线 一组
 * - **球**：`＋` = 插入型内容（链接/图片/音频/表格/分割线/代码块），单独一套 UI
 *
 * 材质：暖米底 `SurfaceFloat` + 发丝线 `Divider` + 静音图标，和菜单/卡片同一套语言。
 */
@Composable
internal fun FormatToolbar(
    groups: List<ToolbarGroup>,
    insertTools: List<ToolbarTool>,
    onAction: (FormatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── 感叹号的「竖」：横向工具条 ──
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceFloat)
                .border(1.dp, Divider, RoundedCornerShape(24.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LazyRow(verticalAlignment = Alignment.CenterVertically) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) item(key = "sep$index") { ToolSeparator() }
                    items(group.tools, key = { it.label }) { tool ->
                        ToolButton(tool.label, tool.icon) { onAction(tool.action) }
                    }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // ── 感叹号的「点」：＋ 球，插入型内容的入口 ──
        var insertOpen by remember { mutableStateOf(false) }
        Box {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (pressed) AccentMist else SurfaceFloat)
                    .border(1.dp, Divider, CircleShape)
                    .clickable(interactionSource = interaction, indication = null) { insertOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "插入",
                    tint = Accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(
                expanded = insertOpen,
                onDismissRequest = { insertOpen = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = SurfaceFloat,
                border = androidx.compose.foundation.BorderStroke(1.dp, Divider),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                // 不抢输入焦点，否则弹出时输入法被挤下去（P0 bug2）
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
            ) {
                insertTools.forEach { tool ->
                    MenuRow(label = tool.label, icon = tool.icon ?: Icons.Outlined.Add) {
                        insertOpen = false
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

/* ===================== 默认排布（可换，见 ToolbarGroup） ===================== */

/** 第一类：标题 */
internal fun headingGroup() = ToolbarGroup(
    listOf(
        ToolbarTool("H1", action = FormatAction.LinePrefix("# ")),
        ToolbarTool("H2", action = FormatAction.LinePrefix("## ")),
        ToolbarTool("H3", action = FormatAction.LinePrefix("### ")),
    ),
)

/** 第二类：行内强调 */
internal fun emphasisGroup() = ToolbarGroup(
    listOf(
        ToolbarTool("B", Icons.Outlined.FormatBold, FormatAction.Wrap("**")),
        ToolbarTool("I", Icons.Outlined.FormatItalic, FormatAction.Wrap("*")),
        ToolbarTool("U", Icons.Outlined.FormatUnderlined, FormatAction.Wrap("<u>", "</u>")),
        ToolbarTool("S", Icons.Outlined.FormatStrikethrough, FormatAction.Wrap("~~")),
    ),
)

/** 第三类：块 */
internal fun blockGroup() = ToolbarGroup(
    listOf(
        ToolbarTool("列表", Icons.AutoMirrored.Outlined.FormatListBulleted, FormatAction.LinePrefix("- ")),
        ToolbarTool("有序", Icons.Outlined.FormatListNumbered, FormatAction.LinePrefix("1. ")),
        ToolbarTool("任务", Icons.Outlined.TaskAlt, FormatAction.ToggleTask),
        ToolbarTool("引用", Icons.Outlined.FormatQuote, FormatAction.LinePrefix("> ")),
    ),
)

/** ＋球里：插入型内容 */
internal fun insertTools() = listOf(
    ToolbarTool("链接", Icons.Outlined.Link, FormatAction.Insert("[]()", 1)),
    ToolbarTool("图片", Icons.Outlined.Image, FormatAction.Insert("![]()", 2)),
    ToolbarTool("音频", Icons.Outlined.Mic, FormatAction.Insert("\n[音频]()\n", 5)),
    ToolbarTool("代码块", Icons.Outlined.DataObject, FormatAction.Insert("\n```\n\n```\n", 6)),
    ToolbarTool("表格", Icons.Outlined.TableChart, FormatAction.Insert(TABLE_TEMPLATE, TABLE_CARET)),
    ToolbarTool("分割线", Icons.Outlined.HorizontalRule, FormatAction.Insert("\n---\n", 4)),
)

/** 2×2 表格骨架：表头是占位文字，光标落在第一个空单元格里，打字即填 */
internal val TABLE_TEMPLATE = "\n| 列1 | 列2 |\n|---|---|\n|  |  |\n"

internal val TABLE_CARET: Int = TABLE_TEMPLATE.indexOf("\n|  ") + 3
