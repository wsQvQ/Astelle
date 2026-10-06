package com.astelle.app.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astelle.app.R
import com.astelle.app.domain.model.Note
import com.astelle.app.ui.components.AstelleIcons
import com.astelle.app.ui.navigation.AstelleDestination
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val Paper = Color(0xFFFAF5EF)
private val PaperWarm = Color(0xFFF3EBE0)
private val DrawerBg = Color(0xFFEAE0D0)
private val CardBg = Color(0xFFFAF5EF)
private val Divider = Color(0xFFE8DDD0)
private val Ink = Color(0xFF2C2418)
private val InkSoft = Color(0xFF4A4034)
private val Muted = Color(0xFF9C8B74)
private val Ghost = Color(0xFFB8A992)
private val Accent = Color(0xFFD4843A)
private val AccentMist = Color(0xFFF7E8D4)
private val SurfaceFloat = Color(0xFFFFFFFF)
private val SavedText = Color(0xFFA86428)

private val mono = FontFamily.Monospace
private val display = FontFamily.Serif

@Composable
fun HomeRoute(
    currentDestination: AstelleDestination,
    onNavigate: (AstelleDestination) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    // 与 RikkaHub 同一套：标准 ModalNavigationDrawer，手势交给 Material
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) keyboard?.hide()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Ink.copy(alpha = 0.18f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                drawerContainerColor = DrawerBg,
                drawerTonalElevation = 0.dp,
                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                DrawerSheet(
                    notes = notes,
                    currentNoteId = state.currentNoteId,
                    searchQuery = state.searchQuery,
                    filter = state.filter,
                    onSearch = { viewModel.onEvent(HomeUiEvent.SearchChanged(it)) },
                    onFilter = { viewModel.onEvent(HomeUiEvent.SetFilter(it)) },
                    onOpenNote = {
                        scope.launch { drawerState.close() }
                        viewModel.onEvent(HomeUiEvent.OpenNote(it))
                    },
                    onTogglePin = { viewModel.onEvent(HomeUiEvent.TogglePin(it)) },
                    onToggleFavorite = { viewModel.onEvent(HomeUiEvent.ToggleFavorite(it)) },
                    onRequestDelete = { viewModel.onEvent(HomeUiEvent.RequestDelete(it)) },
                    onAddFolder = { /* 分类落库下一轮 */ },
                    onNavigate = {
                        scope.launch { drawerState.close() }
                        onNavigate(it)
                    },
                )
            }
        },
    ) {
        EditorScaffold(
            state = state,
            onOpenDrawer = { scope.launch { drawerState.open() } },
            onTitle = { viewModel.onEvent(HomeUiEvent.TitleChanged(it)) },
            onContent = { viewModel.onEvent(HomeUiEvent.ContentChanged(it)) },
            onNewNote = { viewModel.onEvent(HomeUiEvent.NewNote) },
            onMode = { viewModel.onEvent(HomeUiEvent.SetMode(it)) },
            onTogglePin = { id -> viewModel.onEvent(HomeUiEvent.TogglePin(id)) },
            onToggleFavorite = { id -> viewModel.onEvent(HomeUiEvent.ToggleFavorite(id)) },
            onRequestDelete = { id -> viewModel.onEvent(HomeUiEvent.RequestDelete(id)) },
        )
    }

    // 删除二次确认
    if (state.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeUiEvent.CancelDelete) },
            title = { Text("删除这篇笔记？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeUiEvent.ConfirmDelete) }) {
                    Text("删除", color = Color(0xFFC45C4A))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(HomeUiEvent.CancelDelete) }) {
                    Text("取消")
                }
            },
        )
    }
}

/* ========================= 编辑器 · 方案 1 双层顶栏 ========================= */

@Composable
private fun EditorScaffold(
    state: HomeUiState,
    onOpenDrawer: () -> Unit,
    onTitle: (String) -> Unit,
    onContent: (String) -> Unit,
    onNewNote: () -> Unit,
    onMode: (EditorMode) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
) {
    val dateLabel = remember(state.savedAt, state.currentNoteId) {
        val t = state.savedAt ?: System.currentTimeMillis()
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(t))
    }
    var moreMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── 顶栏行 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 侧栏钮单独再圆一点，手感更软
            IconBtn(onClick = onOpenDrawer, shape = CircleShape) {
                Icon(AstelleIcons.Sidebar, contentDescription = "侧边栏", tint = Muted, modifier = Modifier.size(24.dp))
            }
            IconBtn(onClick = {}, enabled = false) {
                Icon(AstelleIcons.Undo, contentDescription = "撤销", tint = Muted.copy(alpha = 0.32f), modifier = Modifier.size(22.dp))
            }
            IconBtn(onClick = {}, enabled = false) {
                Icon(AstelleIcons.Redo, contentDescription = "重做", tint = Muted.copy(alpha = 0.32f), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            ViewPill(mode = state.mode, onMode = onMode)
            Spacer(Modifier.width(4.dp))
            IconBtn(onClick = onNewNote) {
                Image(
                    painter = painterResource(R.drawable.ic_new_note),
                    contentDescription = "新建笔记",
                    // 比其余 24dp 图标大约 8%
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // ── 标题 ──
        TitleField(
            value = state.title,
            onValueChange = onTitle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 10.dp),
        )

        // ── meta 行 ──
        MetaRow(
            dateLabel = dateLabel,
            charCount = state.charCount,
            isDirty = state.isDirty,
            isSaving = state.isSaving,
            moreMenuOpen = moreMenuOpen,
            onMore = { moreMenuOpen = true },
            onDismissMore = { moreMenuOpen = false },
            currentNoteId = state.currentNoteId,
            onTogglePin = onTogglePin,
            onToggleFavorite = onToggleFavorite,
            onRequestDelete = onRequestDelete,
        )

        // ── 分割 ──
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Divider.copy(alpha = 0.55f)),
        )

        // ── 正文 ──
        if (state.mode == EditorMode.Edit) {
            BodyEditor(
                value = state.content,
                onValueChange = onContent,
                modifier = Modifier
                    .weight(1f)
                    .imePadding(),
            )
        } else {
            BodyPreview(content = state.content, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun IconBtn(
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(if (pressed) PaperWarm else Color.Transparent)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ViewPill(mode: EditorMode, onMode: (EditorMode) -> Unit) {
    val target = if (mode == EditorMode.Edit) 0f else 1f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 250, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "viewPillThumb",
    )

    Box(
        modifier = Modifier
            .height(32.dp)
            .width(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PaperWarm.copy(alpha = 0.8f))
            .border(1.dp, Divider, RoundedCornerShape(10.dp))
            .padding(3.dp),
    ) {
        // 滑块：白底，用 graphicsLayer 平移（避免 offset + shadow 发灰）
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .graphicsLayer { translationX = size.width * animated }
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
        )
        Row(Modifier.fillMaxSize()) {
            listOf(EditorMode.Edit to "编辑", EditorMode.Preview to "预览").forEach { (m, label) ->
                val selected = mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onMode(m) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) Accent else Ghost,
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        singleLine = true,
        maxLines = 1,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            lineHeight = 24.sp,
        ),
        cursorBrush = SolidColor(Accent),
        decorationBox = { innerTextField: @Composable () -> Unit ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        "无标题",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ghost,
                        maxLines = 1,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun MetaRow(
    dateLabel: String,
    charCount: Int,
    isDirty: Boolean,
    isSaving: Boolean,
    moreMenuOpen: Boolean,
    onMore: () -> Unit,
    onDismissMore: () -> Unit,
    currentNoteId: String?,
    onTogglePin: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 0.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(dateLabel, fontFamily = mono, fontSize = 11.sp, color = Ghost)
        MetaDot()
        Text("$charCount 字", fontFamily = mono, fontSize = 11.sp, color = Ghost)
        MetaDot()
        SavePill(isDirty = isDirty, isSaving = isSaving)
        Spacer(Modifier.weight(1f))
        Box {
            IconBtn(onClick = onMore) {
                Icon(AstelleIcons.More, contentDescription = "更多", tint = Muted, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(
                expanded = moreMenuOpen,
                onDismissRequest = onDismissMore,
                shape = RoundedCornerShape(14.dp),
                containerColor = SurfaceFloat,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                DropdownMenuItem(
                    text = { Text("置顶") },
                    leadingIcon = { Icon(AstelleIcons.More, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onDismissMore()
                        currentNoteId?.let(onTogglePin)
                    },
                )
                DropdownMenuItem(
                    text = { Text("收藏") },
                    leadingIcon = { Icon(AstelleIcons.Sparkle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onDismissMore()
                        currentNoteId?.let(onToggleFavorite)
                    },
                )
                DropdownMenuItem(
                    text = { Text("删除笔记", color = Color(0xFFC45C4A)) },
                    leadingIcon = {
                        Icon(AstelleIcons.Import, contentDescription = null, tint = Color(0xFFC45C4A), modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        onDismissMore()
                        currentNoteId?.let(onRequestDelete)
                    },
                )
            }
        }
    }
}

@Composable
private fun MetaDot() {
    Box(
        Modifier
            .padding(horizontal = 8.dp)
            .size(3.dp)
            .clip(CircleShape)
            .background(Ghost.copy(alpha = 0.55f)),
    )
}

@Composable
private fun SavePill(isDirty: Boolean, isSaving: Boolean) {
    val saved = !isDirty && !isSaving
    val bg = if (saved) AccentMist else Color.Transparent
    val border = if (saved) Color.Transparent else Divider
    val text = if (saved) SavedText else Muted
    val led = if (saved) Accent else Color(0xFFC4B8A6)
    val label = when {
        isSaving && isDirty -> "保存中…"
        saved -> "已保存"
        else -> "未保存"
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(start = 6.dp, end = 8.dp)
            .height(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(led))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = text, lineHeight = 12.sp)
    }
}

@Composable
private fun BodyEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 16.sp,
            color = Ink,
            lineHeight = 26.sp,
        ),
        cursorBrush = SolidColor(Accent),
        decorationBox = { innerTextField: @Composable () -> Unit ->
            Box(Modifier.fillMaxSize()) {
                if (value.isEmpty()) {
                    Column {
                        Text(
                            "写下你的想法…",
                            fontSize = 16.sp,
                            color = Ghost.copy(alpha = 0.75f),
                            lineHeight = 26.sp,
                        )
                        Text(
                            "支持 Markdown",
                            fontSize = 11.sp,
                            color = Ghost.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun BodyPreview(content: String, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        if (content.isBlank()) {
            Text("还没有内容", color = Ghost, fontSize = 14.sp)
        } else {
            // 真 Markdown 渲染。规格：正文 16sp / 行高 1.75（docs/ui/01-home-screen.md §2.2）
            //
            // 三个刻意的选择：
            //  - linkColor = Accent
            //        链接用品牌橙。下划线保留库默认的开启状态：只靠颜色区分链接，
            //        对色盲用户不友好。
            //  - syntaxHighlightColor
            //        这个参数名有误导性，它实际就是 codeBackgroundColor
            //        （见库的 MardownCorePlugin.configureTheme）。库默认浅灰，
            //        和暖纸色板打架，故换成 PaperWarm。
            //  - enableSoftBreakAddsNewLine
            //        保持库默认的 true。笔记里按一次回车就该换行；若为 false，
            //        多行正文会被 Markdown 规则并成一整段。
            MarkdownText(
                markdown = content,
                modifier = Modifier.fillMaxWidth(),
                linkColor = Accent,
                style = TextStyle(
                    color = Ink,
                    fontSize = 16.sp,
                    lineHeight = 28.sp,
                ),
                syntaxHighlightColor = PaperWarm,
            )
        }
    }
}

/* ========================= 抽屉 ========================= */

@Composable
private fun DrawerSheet(
    notes: List<Note>,
    currentNoteId: String?,
    searchQuery: String,
    filter: NoteFilter,
    onSearch: (String) -> Unit,
    onFilter: (NoteFilter) -> Unit,
    onOpenNote: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
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
                    .background(if (searchFocused) SurfaceFloat else CardBg)
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
                    ) { }
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
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
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
                // 上下 22px 渐隐，溶进抽屉底色
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(22.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
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
                            androidx.compose.ui.graphics.Brush.verticalGradient(
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
                        androidx.compose.ui.graphics.Brush.verticalGradient(
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
    note: Note,
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
                        else -> CardBg
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
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        note.displayTitle.ifBlank { "空白笔记" },
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
                val sum = note.content.trim().lineSequence().firstOrNull().orEmpty()
                if (sum.isNotBlank()) {
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
                    "$timeLabel · ${note.content.length} 字",
                    fontFamily = mono,
                    fontSize = 10.sp,
                    color = Ghost,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
