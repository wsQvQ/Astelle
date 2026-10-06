package com.astelle.app.ui.home

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.ui.components.AstelleIcons
import com.astelle.app.ui.navigation.AstelleDestination
// 色板统一取自 ui/theme —— 本文件不再自己抄一份
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.AccentMist
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.DrawerBg
import com.astelle.app.ui.theme.Ghost
import com.astelle.app.ui.theme.Ink
import com.astelle.app.ui.theme.InkSoft
import com.astelle.app.ui.theme.Muted
import com.astelle.app.ui.theme.Paper
import com.astelle.app.ui.theme.PaperWarm
import com.astelle.app.ui.theme.SurfaceFloat
import com.astelle.app.ui.theme.display
import com.astelle.app.ui.theme.mono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ========================= 抽屉 ========================= */

@Composable
internal fun DrawerSheet(
    notes: List<NoteSummary>,
    currentNoteId: String?,
    searchQuery: String,
    filter: NoteFilter,
    onSearch: (String) -> Unit,
    onFilter: (NoteFilter) -> Unit,
    onOpenNote: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onImportMarkdown: () -> Unit,
    onAddFolder: () -> Unit,
    onNavigate: (AstelleDestination) -> Unit,
) {
    // 自绘容器：不用 ModalDrawerSheet（避免多余阴影/内边距）
    // 内容避开状态栏（背景仍可铺满到顶，边到边）
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
            var showCatInput by remember { mutableStateOf(false) }
            var catName by remember { mutableStateOf("") }
            // 品牌头：横线 + 名号 + 日期，加一点层次
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Accent.copy(alpha = 0.8f)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Astelle",
                        fontFamily = display,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Paper.copy(alpha = 0.7f))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text("${notes.size} 篇", fontFamily = mono, fontSize = 11.sp, color = Muted)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    remember { cleanGreeting() },
                    fontFamily = mono,
                    fontSize = 11.sp,
                    color = Ghost,
                    modifier = Modifier.padding(start = 38.dp),
                )
            }

            // 搜索（BasicTextField，避免 Material TextField 在 40dp 条里被压扁）
            var searchFocused by remember { mutableStateOf(false) }
            val searchInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (searchFocused) SurfaceFloat else Paper)
                    .border(
                        1.dp,
                        if (searchFocused) Accent.copy(alpha = 0.35f) else Divider.copy(alpha = 0.9f),
                        RoundedCornerShape(10.dp),
                    )
                    .onFocusChanged { searchFocused = it.isFocused }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AstelleIcons.Search, contentDescription = null, tint = Ghost, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = Ink, lineHeight = 16.sp),
                    cursorBrush = SolidColor(Accent),
                    interactionSource = searchInteraction,
                    decorationBox = { innerTextField: @Composable () -> Unit ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text("搜索笔记…", fontSize = 13.sp, color = Ghost)
                            }
                            innerTextField()
                        }
                    },
                )
            }

            // 导入
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Paper.copy(alpha = 0.6f))
                    .border(1.dp, Accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onImportMarkdown() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AstelleIcons.Import, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("导入 Markdown", fontSize = 12.sp, color = Muted)
                }
            }

            // 筛选 chip ＋ 新增分类
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Chip("全部", selected = filter == NoteFilter.All) { onFilter(NoteFilter.All) }
                Spacer(Modifier.width(8.dp))
                Chip("置顶", selected = filter == NoteFilter.Pinned) { onFilter(NoteFilter.Pinned) }
                Spacer(Modifier.width(8.dp))
                Chip("收藏", selected = filter == NoteFilter.Favorite) { onFilter(NoteFilter.Favorite) }
                Spacer(Modifier.weight(1f))
                // 新增分类入口：点 + 在下方展开输入条（花笺做法）
                val folderInteraction = remember { MutableInteractionSource() }
                val folderPressed by folderInteraction.collectIsPressedAsState()
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (folderPressed || showCatInput) AccentMist else Color.Transparent)
                        .clickable(interactionSource = folderInteraction, indication = null) {
                            showCatInput = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("＋", fontSize = 16.sp, color = if (folderPressed || showCatInput) Accent else Ghost)
                }
            }

            // 内联新建分类输入条（Enter 提交，Esc/失焦收起）
            if (showCatInput) {
                BasicTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Ink),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaperWarm.copy(alpha = 0.8f))
                        .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (catName.isNotBlank()) onAddFolder()
                        showCatInput = false
                        catName = ""
                    }),
                    decorationBox = { innerTextField: @Composable () -> Unit ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (catName.isEmpty()) {
                                Text("输入分类名…", fontSize = 12.sp, color = Ghost.copy(alpha = 0.7f))
                            }
                            innerTextField()
                        }
                    },
                )
            }

            // 列表 + 上下渐隐
            val listState = rememberLazyListState()
            // 顶部渐隐只在列表真的滚动过之后才出现 —— 它的语义是「卡片从这条边
            // 溶出去」。停在顶部时若也画，就会白白糊掉第一张卡的上沿
            val listScrolled by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }
            val topFadeAlpha by animateFloatAsState(
                targetValue = if (listScrolled) 1f else 0f,
                animationSpec = tween(180, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
                label = "drawerTopFade",
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            selected = note.id == currentNoteId,
                            onClick = { onOpenNote(note.id) },
                            onTogglePin = { onTogglePin(note.id) },
                            onToggleFavorite = { onToggleFavorite(note.id) },
                            onRequestDelete = { onRequestDelete(note.id) },
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
                // 上下渐隐，溶进抽屉底色
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(22.dp)
                        .graphicsLayer { alpha = topFadeAlpha }
                        .background(
                            Brush.verticalGradient(
                                listOf(DrawerBg, DrawerBg.copy(alpha = 0f))
                            )
                        ),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(DrawerBg.copy(alpha = 0f), DrawerBg)
                            )
                        ),
                )
            }

            // 底部图标坞：计划·日记·AI 抱左并留出呼吸，设置远置右
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(DrawerBg.copy(alpha = 0f), DrawerBg)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockBtn(AstelleIcons.Plan, "计划") { onNavigate(AstelleDestination.Plans) }
                Spacer(Modifier.width(14.dp))
                DockBtn(AstelleIcons.Diary, "日记") { onNavigate(AstelleDestination.Diary) }
                Spacer(Modifier.width(14.dp))
                DockBtn(Icons.Outlined.AutoAwesome, "AI") { onNavigate(AstelleDestination.Diary) }
                Spacer(Modifier.weight(1f))
                DockBtn(Icons.Outlined.Tune, "设置") { onNavigate(AstelleDestination.Settings) }
            }
        }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = selected || pressed
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) AccentMist else Paper.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = if (active) Accent.copy(alpha = 0.35f) else Divider.copy(alpha = 0.8f),
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            color = if (active) Accent else Muted,
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val dateLabel = remember(note.updatedAt) {
        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(note.updatedAt))
    }
    val timeLabel = remember(note.updatedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp, horizontal = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        selected -> AccentMist
                        pressed -> PaperWarm
                        else -> Paper
                    }
                )
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { menuOpen = true },
                )
                .padding(horizontal = 13.dp, vertical = 11.dp),
        ) {
            if (selected) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(topEnd = 99.dp, bottomEnd = 99.dp))
                        .background(Accent.copy(alpha = 0.65f)),
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                // 标题为空时 displayTitle 会回退成正文首行；
                // 若摘要再显示同一行，卡片上就会出现肉眼可见的重复，故抽出来比对一次
                val headline = note.displayTitle.ifBlank { "空白笔记" }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        headline,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) Accent else InkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.isFavorite) {
                        Icon(
                            AstelleIcons.Sparkle,
                            contentDescription = "已收藏",
                            tint = Accent,
                            modifier = Modifier.size(12.dp).padding(end = 4.dp),
                        )
                    }
                    Text(dateLabel, fontFamily = mono, fontSize = 10.sp, color = Ghost)
                }
                // 摘要与字数都来自投影，不再从正文全文里现算
                val sum = note.preview
                // sum != headline：标题为空时 headline 就是正文首行，再显示一遍纯属重复
                if (sum.isNotBlank() && sum != headline) {
                    Text(
                        sum,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Text(
                    "$timeLabel · ${note.charCount} 字",
                    fontFamily = mono,
                    fontSize = 10.sp,
                    color = Ghost,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            // 与 meta 行那枚 ⋯ 菜单保持同一套外观
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceFloat,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            DropdownMenuItem(
                text = { Text(if (note.isPinned) "取消置顶" else "置顶") },
                onClick = { menuOpen = false; onTogglePin() },
            )
            DropdownMenuItem(
                text = { Text(if (note.isFavorite) "取消收藏" else "收藏") },
                onClick = { menuOpen = false; onToggleFavorite() },
            )
            DropdownMenuItem(
                text = { Text("删除", color = Color(0xFFC45C4A)) },
                onClick = { menuOpen = false; onRequestDelete() },
            )
        }
    }
}

/**
 * 抽屉底部入口：圆底 + 纯图标，无文字（对齐 RikkaHub 坞形态）。
 */
@Composable
private fun DockBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (pressed) AccentMist else Paper.copy(alpha = 0.55f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Muted,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 干净文案：节日倒数 + 时段，不带抒情。
 * 例：「距离国庆还有 2 天 · 中午好」「国庆节 · 晚上好」「早上好」
 */
internal fun cleanGreeting(now: java.util.Calendar = java.util.Calendar.getInstance()): String {
    val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
    val part = when (hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }
    val month = now.get(java.util.Calendar.MONTH) + 1
    val day = now.get(java.util.Calendar.DAY_OF_MONTH)
    val today = month * 100 + day

    // 固定公历节点
    val festivals = listOf(
        101 to "元旦",
        214 to "情人节",
        308 to "妇女节",
        501 to "劳动节",
        504 to "青年节",
        601 to "儿童节",
        701 to "建党节",
        801 to "建军节",
        910 to "教师节",
        1001 to "国庆节",
        1225 to "圣诞节",
    )

    val todayFest = festivals.firstOrNull { it.first == today }?.second
    if (todayFest != null) return "$todayFest · $part"

    // 全年取最近一个（长期项目不截断 30 天）
    val nowDays = now.get(java.util.Calendar.DAY_OF_YEAR)
    val soonest = festivals.mapNotNull { (md, name) ->
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.MONTH, md / 100 - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, md % 100)
        val target = cal.get(java.util.Calendar.DAY_OF_YEAR)
        val delta = (target - nowDays + 365) % 365
        if (delta >= 1) delta to name else null
    }.minByOrNull { it.first }

    return if (soonest != null) {
        val (d, name) = soonest
        when (d) {
            1 -> "明天${name} · $part"
            2 -> "距离${name}还有两天 · $part"
            else -> "距离${name}还有 $d 天 · $part"
        }
    } else {
        part
    }
}
