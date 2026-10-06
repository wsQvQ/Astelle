package com.astelle.app.ui.home

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.ui.components.AstelleIcons
import com.astelle.app.ui.navigation.AstelleDestination
// 色板统一取自 ui/theme —— 本文件不再自己抄一份
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.AccentMist
import com.astelle.app.ui.theme.Danger
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DrawerSheet(
    notes: List<NoteSummary>,
    folders: List<Folder>,
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
    onAddFolder: (String) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onMoveNoteToFolder: (String, String?) -> Unit,
    onNavigate: (AstelleDestination) -> Unit,
) {
    // 长按分类组头弹出的重命名 / 删除确认
    var renaming by remember { mutableStateOf<Folder?>(null) }
    var deleting by remember { mutableStateOf<Folder?>(null) }

    if (renaming != null) {
        val target = renaming!!
        var draft by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("重命名分类", fontSize = 16.sp) },
            text = {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Ink),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaperWarm.copy(alpha = 0.8f))
                        .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onRenameFolder(target.id, draft)
                        renaming = null
                    }),
                    decorationBox = { innerTextField: @Composable () -> Unit ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (draft.isEmpty()) {
                                Text("分类名…", fontSize = 14.sp, color = Ghost.copy(alpha = 0.7f))
                            }
                            innerTextField()
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameFolder(target.id, draft)
                    renaming = null
                }) { Text("保存", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("取消", color = Muted) }
            },
            containerColor = SurfaceFloat,
            titleContentColor = Ink,
            textContentColor = Muted,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 0.dp,
        )
    }

    if (deleting != null) {
        val target = deleting!!
        val count = notes.count { it.folderId == target.id }
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除分类「${target.name}」？", fontSize = 16.sp) },
            text = {
                Text(
                    // 说清笔记的去向，否则用户会以为连笔记一起删了
                    if (count == 0) "这个分类里没有笔记。" else "里面的 $count 篇笔记会回到「未分类」，不会被删除。",
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteFolder(target.id)
                    deleting = null
                }) { Text("删除分类", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消", color = Muted) }
            },
            containerColor = SurfaceFloat,
            titleContentColor = Ink,
            textContentColor = Muted,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 0.dp,
        )
    }

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
                            // 输入条已经开着且写了字 → 这一下就是提交（设计稿 §3「再点加号 = 提交」）
                            if (showCatInput && catName.isNotBlank()) {
                                onAddFolder(catName)
                                catName = ""
                                showCatInput = false
                            } else {
                                showCatInput = true
                            }
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
                        if (catName.isNotBlank()) onAddFolder(catName)
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
            // 折叠态。默认全展开，所以只记「被折叠的」
            val collapsed = remember { mutableStateMapOf<String, Boolean>() }
            val collapsedKeys = collapsed.filterValues { it }.keys
            // 搜索时压平列表：搜索是「我要那一篇」，此时还按分类铺开会把结果
            // 埋在一串组头里。筛选（置顶/收藏）则保留分组，只是空组不再占位
            val flat = searchQuery.isNotBlank()
            val groups = groupNotes(notes, folders, hideEmpty = filter != NoteFilter.All)
            val rows = if (flat) notes.map { DrawerRow.Note(it) } else buildRows(groups, collapsedKeys)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                ) {
                    // 逐行发射而不是 items(rows)：只有这样才能对组头用 stickyHeader，
                    // 让章节标签在滚动时停在眼前（设计稿 §3）
                    rows.forEach { row ->
                        when (row) {
                            is DrawerRow.Header -> stickyHeader(key = row.key) {
                                // 不透明底衬：不然卡片会从组头背后透出来
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        // horizontal 4dp 与卡片同级，让组头和它下面的卡片左缘对齐
                                        .padding(horizontal = 4.dp, vertical = 3.dp)
                                        .background(DrawerBg),
                                ) {
                                    FolderHeader(
                                        name = row.group.name,
                                        count = row.group.notes.size,
                                        collapsed = collapsed[row.group.key] == true,
                                        isInbox = row.group.folder == null,
                                        onToggle = {
                                            collapsed[row.group.key] =
                                                collapsed[row.group.key] != true
                                        },
                                        onRename = { renaming = row.group.folder },
                                        onDelete = { deleting = row.group.folder },
                                    )
                                }
                            }
                            is DrawerRow.Note -> item(key = row.key) {
                                NoteCard(
                                    note = row.summary,
                                    selected = row.summary.id == currentNoteId,
                                    nested = row.nested,
                                    folders = folders,
                                    onClick = { onOpenNote(row.summary.id) },
                                    onTogglePin = { onTogglePin(row.summary.id) },
                                    onToggleFavorite = { onToggleFavorite(row.summary.id) },
                                    onRequestDelete = { onRequestDelete(row.summary.id) },
                                    onMoveToFolder = { onMoveNoteToFolder(row.summary.id, it) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                    if (rows.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillParentMaxSize().padding(bottom = 72.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    emptyHint(searchQuery, filter),
                                    fontSize = 12.sp,
                                    color = Ghost,
                                )
                            }
                        }
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

/**
 * 分类组头。38dp / 圆角 10dp —— 折叠时是纸白，展开时透出一点暖橙，
 * 让「这一组是开着的」不只靠那个小箭头承担。
 *
 * 长按 → 重命名 / 删除。「未分类」是收件箱、不是一个真的分类，所以不给菜单。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderHeader(
    name: String,
    count: Int,
    collapsed: Boolean,
    isInbox: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 设计稿 §3：折叠展开 250ms，同一条品牌曲线
    val arrow by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = tween(250, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "folderArrow",
    )

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        pressed -> AccentMist
                        collapsed -> Paper.copy(alpha = 0.65f)
                        else -> AccentMist.copy(alpha = 0.75f)
                    }
                )
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onToggle,
                    onLongClick = if (isInbox) null else { { menuOpen = true } },
                )
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AstelleIcons.Folder,
                contentDescription = null,
                tint = if (collapsed) Muted else Accent,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (collapsed) Muted else InkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("$count", fontFamily = mono, fontSize = 11.sp, color = Ghost)
            Spacer(Modifier.width(4.dp))
            Icon(
                AstelleIcons.Chevron,
                contentDescription = if (collapsed) "展开" else "折叠",
                tint = Ghost,
                modifier = Modifier.size(16.dp).rotate(arrow),
            )
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceFloat,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = { menuOpen = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text("删除分类", color = Danger) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}

/** 列表空的时候说清楚「为什么空」，比一片留白好 */
private fun emptyHint(query: String, filter: NoteFilter): String = when {
    query.isNotBlank() -> "没有匹配「$query」的笔记"
    filter == NoteFilter.Pinned -> "还没有置顶的笔记"
    filter == NoteFilter.Favorite -> "还没有收藏的笔记"
    else -> "还没有笔记，去写第一条吧"
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteSummary,
    selected: Boolean,
    nested: Boolean,
    folders: List<Folder>,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestDelete: () -> Unit,
    onMoveToFolder: (String?) -> Unit,
    modifier: Modifier = Modifier,
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
    // 菜单的第二层：选要移进哪个分类。用同一个菜单换内容，而不是再叠一层
    // DropdownMenu —— 嵌套弹窗的位置很难控制，在抽屉这种窄容器里尤其明显
    var moving by remember { mutableStateOf(false) }

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 分类下的卡片缩进一档：不缩的话整组就是一堆飘着的卡片，
                // 上面那个组头白画了
                .padding(
                    start = if (nested) 12.dp else 4.dp,
                    end = 4.dp,
                    top = 3.dp,
                    bottom = 3.dp,
                )
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
            onDismissRequest = { menuOpen = false; moving = false },
            // 与 meta 行那枚 ⋯ 菜单保持同一套外观
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceFloat,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            if (moving) {
                DropdownMenuItem(
                    text = { Text("← 选择分类", color = Muted) },
                    onClick = { moving = false },
                )
                DropdownMenuItem(
                    text = { Text(if (note.folderId == null) "未分类 ✓" else "未分类") },
                    onClick = {
                        menuOpen = false; moving = false
                        // 已经在那组里就别白写一次库（会白白刷新 updatedAt）
                        if (note.folderId != null) onMoveToFolder(null)
                    },
                )
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = {
                            Text(if (note.folderId == folder.id) "${folder.name} ✓" else folder.name)
                        },
                        onClick = {
                            menuOpen = false; moving = false
                            if (note.folderId != folder.id) onMoveToFolder(folder.id)
                        },
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text(if (note.isPinned) "取消置顶" else "置顶") },
                    onClick = { menuOpen = false; onTogglePin() },
                )
                DropdownMenuItem(
                    text = { Text(if (note.isFavorite) "取消收藏" else "收藏") },
                    onClick = { menuOpen = false; onToggleFavorite() },
                )
                // 一个分类都没有时不显示这项 —— 点进去只有「未分类」可选，纯属绕路
                if (folders.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("移动到分类") },
                        onClick = { moving = true },
                    )
                }
                DropdownMenuItem(
                    text = { Text("删除", color = Danger) },
                    onClick = { menuOpen = false; onRequestDelete() },
                )
            }
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
