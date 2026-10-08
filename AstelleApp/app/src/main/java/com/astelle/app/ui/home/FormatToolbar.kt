package com.astelle.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
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
import androidx.compose.material.icons.outlined.InsertLink
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.astelle.app.ui.components.MenuRow
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.PressGlow
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Muted
import com.astelle.app.ui.theme.SurfaceFloat

/** 工具栏动作：全是纯数据，落到 MarkdownEditing 里执行 */
sealed interface FormatAction {
    data class Wrap(val open: String, val close: String = open) : FormatAction
    data class LinePrefix(val prefix: String) : FormatAction
    data class Insert(val snippet: String, val caret: Int) : FormatAction
    data object ToggleTask : FormatAction
    data object Indent : FormatAction
    data object Outdent : FormatAction
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
 * 横着的感叹号：一条胶囊（感叹号的竖） + 右边一颗圆球（感叹号的点），球和条隔 10dp。
 *
 * - **条**：文字格式，分组 —— B/I/U/删除线 一组、H1/H2/H3 一组、列表/有序/任务/引用 一组
 * - **球**：插入型内容入口（链接/图片/音频/代码块/表格/分割线），单独一套 UI
 *
 * 材质（用户 2026-10-08 晚定稿，按外观图）：
 * - **胶囊有底有描边**：`SurfaceFloat` 暖底 + `Divider` 发丝描边 + 24dp 圆角，
 *   **宽度贴着按钮内容走**（⚠️ 不拉满整行 —— 拉满就成了用户不要的「矩形底」）；
 *   组间竖线保留在胶囊内；**胶囊周边没有任何图层**，直接悬在正文上
 * - **球有圆底**：同款 `SurfaceFloat` + 描边
 * - 位置不变：贴键盘上沿、和输入法留一小段悬空气（悬空感）
 * 按压反馈：`AccentMist` 暖底（颜色只表达状态）。
 * 内容超出可用宽度时胶囊内横向滚动，左缘 `SurfaceFloat` 渐隐提示「右边还有」。
 */
@Composable
internal fun FormatToolbar(
    groups: List<ToolbarGroup>,
    insertTools: List<ToolbarTool>,
    onAction: (FormatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 对齐（用户 2026-10-08 晚定）：常规**居中**；大屏（侧栏地盘）**靠右**；
    // **从不靠左** —— 左边是抽屉/侧栏（3️⃣ 平板常驻侧栏）的家。
    // 大屏的尺和 3️⃣ 同一把：宽>高 且 ≥1100dp（RikkaHub 规则），侧栏上线后不用改这里
    val config = LocalConfiguration.current
    val largeScreen =
        config.screenWidthDp > config.screenHeightDp && config.screenWidthDp >= 1100
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = if (largeScreen) Arrangement.End else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── 感叹号的「竖」：胶囊条（贴内容宽，超出则胶囊内滚动） ──
        val scrollState = rememberScrollState()
        val canScrollBack by remember { derivedStateOf { scrollState.value > 0 } }
        var pressedLabel by remember { mutableStateOf<String?>(null) }
        Box(
            modifier = Modifier
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceFloat)
                .border(1.dp, Divider, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) ToolSeparator()
                    group.tools.forEach { tool ->
                        ToolButton(
                            tool.label,
                            tool.icon,
                            pressed = pressedLabel == tool.label,
                            onPress = { pressedLabel = tool.label },
                            onRelease = { pressedLabel = null },
                        ) { onAction(tool.action) }
                    }
                }
            }
            // 左缘渐隐 = 「右边还有」（只在真能往回滚时出现）；渐隐到胶囊底色，不出胶囊
            if (canScrollBack) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(16.dp)
                        .height(48.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(SurfaceFloat, Color.Transparent),
                            ),
                        ),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // ── 感叹号的「点」：＋ 球，插入型内容的入口（和胶囊同款底色描边） ──
        var insertOpen by remember { mutableStateOf(false) }
        // 竞态闸门（和 ⋯ 菜单同款 bug）：弹层「点外面收起」+ 按钮点击会把菜单又打开；
        // 「刚被收起」250ms 内的点击不重开 —— 再点球 = 收回
        var insertDismissedAt by remember { mutableStateOf(0L) }
        Box {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            // 点一下亮起、松手 120ms 淡出（用户定：只要瞬间动效，不要常亮）
            val ballBg by animateColorAsState(
                targetValue = if (pressed) PressGlow else SurfaceFloat,
                animationSpec = tween(durationMillis = 120),
                label = "ballBg",
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ballBg)
                    .border(1.dp, Divider, CircleShape)
                    // 再点一次要**收回**，不是反复打开（用户提的；竞态用闸门挡）
                    .clickable(interactionSource = interaction, indication = null) {
                        val now = System.currentTimeMillis()
                        if (insertOpen) {
                            insertOpen = false
                        } else if (now - insertDismissedAt > 250) {
                            insertOpen = true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    // 链接形状的符号（用户：别用加号）；要和菜单里的「链接」**不一样** ——
                    // 那边是 Icons.Outlined.Link，这边用 InsertLink（链条带插件形状）
                    Icons.Outlined.InsertLink,
                    contentDescription = "插入",
                    tint = Accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(
                expanded = insertOpen,
                onDismissRequest = {
                    insertOpen = false
                    insertDismissedAt = System.currentTimeMillis()
                },
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

/** 组间竖线：胶囊内的分界（用户定稿：竖线保留） */
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

/**
 * 44dp 触控、20dp 图标、静音色、按压暖底 —— 和顶栏按钮同一套手感。
 * 按压反馈是「按下时 `AccentMist` 圆角块」：用 pressed 入参而不是
 * `collectIsPressedAsState`，拖动/滚动取消时能可靠熄灭。
 * 动效（用户 2026-10-08 深夜定）：**点一下亮起、松手 120ms 淡出** —— 只要这一个瞬间反馈，
 * 不做常亮状态（格式状态高亮已归档到 `archive/format-highlight-20261008`）
 */
@Composable
private fun ToolButton(
    label: String,
    icon: ImageVector?,
    pressed: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onClick: () -> Unit,
) {
    // ⚠️ 透明态用 PressGlow.copy(alpha = 0f) 而不是 Color.Transparent（透明黑）——
    // 否则淡出时颜色插值穿过灰色中间帧，用户看到「闪一下灰的」（翻过车）
    val bg by animateColorAsState(
        targetValue = if (pressed) PressGlow else PressGlow.copy(alpha = 0f),
        animationSpec = tween(durationMillis = 120),
        label = "toolBg",
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        onPress()
                        tryAwaitRelease()
                        onRelease()
                    },
                    onTap = { onClick() },
                )
            },
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

/** 第四类：列表层级（缩进/反缩进）—— 手机上的 Tab / Shift+Tab */
internal fun indentGroup() = ToolbarGroup(
    listOf(
        ToolbarTool("缩进", Icons.AutoMirrored.Outlined.FormatIndentIncrease, FormatAction.Indent),
        ToolbarTool("反缩进", Icons.AutoMirrored.Outlined.FormatIndentDecrease, FormatAction.Outdent),
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
