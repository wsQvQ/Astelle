package com.astelle.app.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astelle.app.R
import com.astelle.app.data.editor.MarkdownEditing
import com.astelle.app.data.exporter.ExportFileWriter
import com.astelle.app.data.exporter.ExportResult
import com.astelle.app.data.exporter.MarkdownExport
import com.astelle.app.data.importer.MarkdownFileReader
import com.astelle.app.data.importer.MarkdownImport
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.ui.components.AstelleIcons
import com.astelle.app.ui.components.MenuDivider
import com.astelle.app.ui.components.MenuRow
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
import com.astelle.app.ui.theme.SavedText
import com.astelle.app.ui.theme.SurfaceFloat
import com.astelle.app.ui.theme.display
import com.astelle.app.ui.theme.mono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * 导入 .md 时交给系统文件选择器的类型过滤。
 * 各家文件管理器对 .md 上报的 MIME 差异很大，这里把常见几种都列上；
 * 若真机上 .md 被置灰选不中，往这里补一个全通配的 MIME 即可。
 */
private val MARKDOWN_MIME_TYPES = arrayOf(
    "text/markdown",
    "text/x-markdown",
    "text/plain",
    "text/*",
    "application/octet-stream",
)

@Composable
fun HomeRoute(
    currentDestination: AstelleDestination,
    onNavigate: (AstelleDestination) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // 导入 .md：挑文件 → 读文本 → 纯函数解析 → 交给 ViewModel 落库。
    // 解析放在 UI 层是为了让 ViewModel 完全不碰 Android 的 ContentResolver
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val picked = MarkdownFileReader.read(context, uri)
            if (picked == null) {
                Toast.makeText(context, "导入失败：读不到这个文件", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val imported = MarkdownImport.parse(picked.displayName, picked.text)
            viewModel.onEvent(HomeUiEvent.ImportNote(imported))
            val message = if (picked.truncated) {
                "文件过大，只导入了前 ${MarkdownFileReader.MAX_BYTES / 1024} KB"
            } else {
                "已导入「${imported.title}」"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 与 RikkaHub 同一套：标准 ModalNavigationDrawer，手势交给 Material
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) keyboard?.hide()
    }

    // 打开侧边栏就清掉预览里的选区（用户：选中不该留着）—— tick 递增，下游自己清
    var selectionClearTick by remember { mutableStateOf(0) }
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) selectionClearTick++
    }

    // ⚠️ 抽屉手势（调研 RikkaHub + M3 源码的结论，见 docs/06 §3.30）：
    // 起手区域是**整个内容区**（不是屏幕边缘），M3 默认阈值 = 拖过 50% 抽屉宽（150dp）
    // 或 fling ≥400dp/s。RikkaHub 一行没改 —— 也就是说它同样容易被快速轻扫误触。
    // 我们加两道自己的闸：
    //  ① 方向锁：横向位移没到竖向两倍之前，横向残量一律不放给抽屉（滤掉斜滑）
    //  ② 打字（输入法弹出）时不响应抽屉手势 —— 打字时的横向抖动最容易误触
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !imeVisible || drawerState.isOpen,
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
                    folders = folders,
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
                    onImportMarkdown = {
                        scope.launch { drawerState.close() }
                        importLauncher.launch(MARKDOWN_MIME_TYPES)
                    },
                    onAddFolder = { viewModel.onEvent(HomeUiEvent.AddFolder(it)) },
                    onRenameFolder = { id, name ->
                        viewModel.onEvent(HomeUiEvent.RenameFolder(id, name))
                    },
                    onDeleteFolder = { viewModel.onEvent(HomeUiEvent.DeleteFolder(it)) },
                    onMoveNoteToFolder = { noteId, folderId ->
                        viewModel.onEvent(HomeUiEvent.MoveNoteToFolder(noteId, folderId))
                    },
                    onNewNoteInFolder = { folderId ->
                        scope.launch { drawerState.close() }
                        viewModel.onEvent(HomeUiEvent.NewNoteInFolder(folderId))
                    },
                    onNavigate = {
                        scope.launch { drawerState.close() }
                        onNavigate(it)
                    },
                )
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // 方向锁闸：只放「明显横向」的滑动给抽屉，斜滑/竖滑的横向残量在这儿吃掉
                // （子组件先消费，剩下的才到这儿，所以不伤点击、滚动、选中）
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var dx = 0f
                        var dy = 0f
                        while (true) {
                            val ev = awaitPointerEvent()
                            if (ev.changes.none { it.pressed }) break
                            val change = ev.changes.first()
                            dx += change.positionChange().x
                            dy += change.positionChange().y
                            if (abs(dx) < 2 * abs(dy)) {
                                ev.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
        ) {
            EditorScaffold(
                state = state,
                clearSelectionTick = selectionClearTick,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onTitle = { viewModel.onEvent(HomeUiEvent.TitleChanged(it)) },
                onContent = { viewModel.onEvent(HomeUiEvent.ContentChanged(it)) },
                onNewNote = { viewModel.onEvent(HomeUiEvent.NewNote) },
                onUndo = { viewModel.onEvent(HomeUiEvent.Undo) },
                onRedo = { viewModel.onEvent(HomeUiEvent.Redo) },
                onMode = { viewModel.onEvent(HomeUiEvent.SetMode(it)) },
                onTogglePin = { id -> viewModel.onEvent(HomeUiEvent.TogglePin(id)) },
                onToggleFavorite = { id -> viewModel.onEvent(HomeUiEvent.ToggleFavorite(id)) },
                onRequestDelete = { id -> viewModel.onEvent(HomeUiEvent.RequestDelete(id)) },
            )
        }
    }

    // 删除二次确认
    if (state.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeUiEvent.CancelDelete) },
            title = { Text("删除这篇笔记？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeUiEvent.ConfirmDelete) }) {
                    Text("删除", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(HomeUiEvent.CancelDelete) }) {
                    Text("取消", color = Muted)
                }
            },
            // 显式指定，不依赖 M3 默认容器色
            containerColor = SurfaceFloat,
            titleContentColor = Ink,
            textContentColor = Muted,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 0.dp,
        )
    }
}

/* ========================= 编辑器 · 方案 1 双层顶栏 ========================= */

@Composable
private fun EditorScaffold(
    state: HomeUiState,
    clearSelectionTick: Int = 0,
    onOpenDrawer: () -> Unit,
    onTitle: (String) -> Unit,
    onContent: (String) -> Unit,
    onNewNote: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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

    // ── 编辑框持有 TextFieldValue ──
    // 工具栏要拿光标/选区做「包住选中」「插模板光标落点」，String 拿不到这些。
    // 外部内容变化（切笔记、撤销、重做）时同步进来；打字以本地为准，不回灌
    var fieldValue by remember { mutableStateOf(TextFieldValue("")) }
    LaunchedEffect(state.currentNoteId, state.content) {
        if (fieldValue.text != state.content) {
            fieldValue = TextFieldValue(state.content, selection = TextRange(state.content.length))
        }
    }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0 // 保留：键盘态以后还要用

    /** 工具栏动作落到纯函数；一次操作 = 一次撤销（走 onContent 一条路） */
    val applyFormat: (FormatAction) -> Unit = { action ->
        val text = fieldValue.text
        val cursor = fieldValue.selection.min
        val end = fieldValue.selection.max
        val r = when (action) {
            is FormatAction.Wrap -> MarkdownEditing.wrap(text, cursor, end, action.open, action.close)
            is FormatAction.LinePrefix -> MarkdownEditing.toggleLinePrefix(text, cursor, action.prefix)
            is FormatAction.Insert -> MarkdownEditing.insert(text, cursor, action.snippet, action.caret)
            FormatAction.ToggleTask -> MarkdownEditing.toggleTask(text, cursor)
        }
        fieldValue = TextFieldValue(r.text, TextRange(r.selectStart, r.selectEnd))
        onContent(r.text)
    }

    // ── 导出 ──
    // Markdown 只是文本拼接，点保存框那一刻现生成就行；
    // 图片要临时挂一块 1440px 的屏幕外画布抓图（见 ExportImageCanvas）。
    // 整条链路只读内存里的 state，绝不落库 —— 导出是读操作，不该刷新 updatedAt
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    val exportLayer = rememberGraphicsLayer()
    var exportCanvasShown by remember { mutableStateOf(false) }
    var exportCanvasHeightPx by remember { mutableStateOf(0) }
    var sliceOffsetPx by remember { mutableStateOf(0f) }
    var sliceHeightPx by remember { mutableStateOf(0) }
    var pendingMarkdown by remember { mutableStateOf<String?>(null) }
    var pendingPng by remember { mutableStateOf<Bitmap?>(null) }
    var pendingBaseName by remember { mutableStateOf(MarkdownExport.DEFAULT_NAME) }

    val exportMdLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        val text = pendingMarkdown
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        exportScope.launch {
            val message = when (val result = ExportFileWriter.writeText(context, uri, text)) {
                is ExportResult.Ok -> "已导出 Markdown 文件"
                is ExportResult.Failed -> "导出失败：${result.reason}"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val exportPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        val bitmap = pendingPng
        if (uri == null || bitmap == null) return@rememberLauncherForActivityResult
        exportScope.launch {
            val message = when (val result = ExportFileWriter.writePng(context, uri, bitmap)) {
                is ExportResult.Ok -> "已导出图片"
                is ExportResult.Failed -> "导出失败：${result.reason}"
            }
            // 位图写完就撒手：长图占内存不小，别一直攥在手里
            pendingPng = null
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

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
            // 设计稿 §2.1：撤销 / 重做无历史时 32% 透明禁用
            IconBtn(onClick = onUndo, enabled = state.canUndo) {
                Icon(
                    AstelleIcons.Undo,
                    contentDescription = "撤销",
                    tint = Muted.copy(alpha = if (state.canUndo) 1f else 0.32f),
                    modifier = Modifier.size(22.dp),
                )
            }
            IconBtn(onClick = onRedo, enabled = state.canRedo) {
                Icon(
                    AstelleIcons.Redo,
                    contentDescription = "重做",
                    tint = Muted.copy(alpha = if (state.canRedo) 1f else 0.32f),
                    modifier = Modifier.size(22.dp),
                )
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
            isPinned = state.isPinned,
            isFavorite = state.isFavorite,
            onTogglePin = onTogglePin,
            onToggleFavorite = onToggleFavorite,
            onRequestDelete = onRequestDelete,
            onExportMarkdown = {
                val text = MarkdownExport.toMarkdown(state.title, state.content)
                if (text.isEmpty()) {
                    Toast.makeText(context, "还没有内容，写点什么再导出", Toast.LENGTH_SHORT).show()
                } else {
                    pendingMarkdown = text
                    pendingBaseName = MarkdownExport.baseName(state.title, state.content)
                    exportMdLauncher.launch("$pendingBaseName.md")
                }
            },
            onExportImage = {
                if (state.title.isBlank() && state.content.isBlank()) {
                    Toast.makeText(context, "还没有内容，写点什么再导出", Toast.LENGTH_SHORT).show()
                } else {
                    pendingBaseName = MarkdownExport.baseName(state.title, state.content)
                    exportScope.launch {
                        exportCanvasShown = true
                        // 等三帧：第一帧排版+绘制（record），第二帧才落定；
                        // 而 Markdown 表格的行高靠「首轮绘制后 requestLayout、
                        // 第二轮测量才正确」（见 fixTableRelayout），所以要多等一帧 ——
                        // 只等两帧抓到的是没修完的那张，表格会叠字（真机踩过）
                        withFrameNanos { }
                        withFrameNanos { }
                        withFrameNanos { }
                        val totalHeight = exportCanvasHeightPx
                        // 分片抓、拼整图：长图一次性读回内存会撞 GPU 纹理上限，
                        // 手机上 4450 字的图就是这么在 @copy 环节炸的（见 MAX_SLICE_PX）
                        val shot = if (totalHeight <= 0) {
                            Log.w("AstelleExport", "画布高度为 0，没东西可抓")
                            null
                        } else {
                            runCatching {
                                val out = Bitmap.createBitmap(
                                    EXPORT_IMAGE_WIDTH_PX,
                                    totalHeight,
                                    Bitmap.Config.RGB_565,
                                )
                                val canvas = Canvas(out)
                                var y = 0
                                while (y < totalHeight) {
                                    val h = minOf(MAX_SLICE_PX, totalHeight - y)
                                    sliceOffsetPx = y.toFloat()
                                    sliceHeightPx = h
                                    withFrameNanos { }
                                    withFrameNanos { }
                                    // toImageBitmap() 出来的是**硬件位图**，
                                    // 而软件画布不许直接画硬件位图（真机实测：
                                    // "Software rendering doesn't support hardware bitmaps"）——
                                    // 必须先 Bitmap.copy 成软件位图，这一步在所有机型都合法
                                    val part = exportLayer.toImageBitmap().asAndroidBitmap()
                                    val sw = part.copy(Bitmap.Config.RGB_565, false)
                                    canvas.drawBitmap(sw, 0f, y.toFloat(), null)
                                    sw.recycle()
                                    y += h
                                }
                                out
                            }.getOrElse { e ->
                                Log.w("AstelleExport", "抓图失败 totalHeight=$totalHeight", e)
                                null
                            }
                        }
                        sliceOffsetPx = 0f
                        sliceHeightPx = 0
                        exportCanvasShown = false
                        if (shot == null) {
                            Toast.makeText(context, "导出失败，没能生成图片", Toast.LENGTH_SHORT).show()
                        } else {
                            pendingPng = shot
                            exportPngLauncher.launch("$pendingBaseName.png")
                        }
                    }
                }
            },
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
                value = fieldValue,
                onValueChange = { new ->
                    // 回车续列表/引用：只在「正好插入一个换行」时接管
                    val continued = MarkdownEditing.autoContinue(fieldValue.text, new.text, new.selection.min)
                    if (continued != null) {
                        fieldValue = TextFieldValue(continued.text, TextRange(continued.selectStart, continued.selectEnd))
                        onContent(continued.text)
                    } else {
                        fieldValue = new
                        onContent(new.text)
                    }
                },
                // ⚠️ 正文**不加 imePadding**：工具栏已经用 imePadding 把自己顶到键盘上沿、
                // 也占掉了自己的高度；正文再按输入法高度内缩一次就是双重扣减 ——
                // 真机上表现为「打字区被顶上去一格，第一行看不见」（P0 bug A）
                modifier = Modifier.weight(1f),
            )
            // 格式工具栏**编辑模式常驻**（用户拍板）：收起键盘就消失会连带把它的
            // ⋯ 菜单一起拆掉 —— 菜单一打开输入法就收起，于是菜单秒开秒关、页面抽搐（P0 bug B）
            FormatToolbar(
                // 顺序（用户定的）：强调 → 标题 → 块
                groups = remember { listOf(emphasisGroup(), headingGroup(), blockGroup()) },
                insertTools = remember { insertTools() },
                onAction = { applyFormat(it) },
                modifier = Modifier.imePadding(),
            )
        } else {
            BodyPreview(
                content = state.content,
                modifier = Modifier.weight(1f),
                clearSelectionTick = clearSelectionTick,
            )
        }

        // ── 屏幕外的导出画布：只在抓图那几十毫秒里存在 ──
        if (exportCanvasShown) {
            OffscreenCanvasHost {
                ExportImageCanvas(
                    title = state.title,
                    content = state.content,
                    graphicsLayer = exportLayer,
                    sliceOffsetPx = sliceOffsetPx,
                    sliceHeightPx = sliceHeightPx,
                    onHeightChanged = { exportCanvasHeightPx = it },
                )
            }
        }
    }
}

@Composable
private fun IconBtn(
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    size: Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(size)
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
        // 高度交给文字自身决定（24sp 行高 + 上下各 2dp），不写死 32dp：
        // 写死的话用户把系统字体调大后，标题会被裁掉
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
            // 这里必须用 fillMaxWidth，不能用 fillMaxSize。
            // 外层已不再写死高度，fillMaxSize 会去撑满 Column 给的全部剩余空间，
            // 于是标题框变成整屏高、文字飘到屏幕正中（上一版就是这么炸的）。
            // fillMaxWidth 只锁宽度，高度由文字撑开。
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
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
    isPinned: Boolean,
    isFavorite: Boolean,
    onTogglePin: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onExportMarkdown: () -> Unit,
    onExportImage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 0.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(dateLabel, fontFamily = mono, fontSize = 11.sp, color = Ghost)
        MetaDot()
        Text("$charCount 字", fontFamily = mono, fontSize = 11.sp, color = Ghost)
        MetaDot()
        SavePill(isDirty = isDirty, isSaving = isSaving)
        Spacer(Modifier.weight(1f))
        Box {
            // 36dp：原型 index.html 里这枚 ⋯ 是 32px，但那是鼠标场景；
            // 手机上我留到 36dp 保触控。行内文字垂直居中，按钮越高，
            // 标题与 meta 之间凭空多出来的空白就越大
            IconBtn(onClick = onMore, size = 36.dp) {
                Icon(AstelleIcons.More, contentDescription = "更多", tint = Muted, modifier = Modifier.size(20.dp))
            }
            // 二级菜单沿用抽屉里「移动到分类」的做法：同一个 DropdownMenu 换内容，
            // 不叠第二个弹窗 —— 嵌套弹窗的位置在窄容器里根本控制不住
            var pickingExport by remember { mutableStateOf(false) }
            DropdownMenu(
                expanded = moreMenuOpen,
                onDismissRequest = { pickingExport = false; onDismissMore() },
                shape = RoundedCornerShape(14.dp),
                containerColor = SurfaceFloat,
                // 和卡片同一套语言：一圈描边 + 圆角 14dp，边界靠描边立
                border = BorderStroke(1.dp, Divider),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                if (pickingExport) {
                    MenuRow(
                        label = "返回",
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        iconTint = Muted,
                        textColor = Muted,
                    ) { pickingExport = false }
                    MenuRow(label = "Markdown 文件", icon = Icons.AutoMirrored.Outlined.Article) {
                        pickingExport = false
                        onDismissMore()
                        onExportMarkdown()
                    }
                    MenuRow(label = "图片", icon = Icons.Outlined.Photo) {
                        pickingExport = false
                        onDismissMore()
                        onExportImage()
                    }
                } else {
                    MenuRow(
                        label = if (isPinned) "取消置顶" else "置顶",
                        icon = Icons.Outlined.PushPin,
                        enabled = currentNoteId != null,
                    ) {
                        onDismissMore()
                        currentNoteId?.let(onTogglePin)
                    }
                    MenuRow(
                        label = if (isFavorite) "取消收藏" else "收藏",
                        icon = Icons.Outlined.StarBorder,
                        enabled = currentNoteId != null,
                    ) {
                        onDismissMore()
                        currentNoteId?.let(onToggleFavorite)
                    }
                    MenuRow(label = "导出", icon = Icons.Outlined.SaveAlt) { pickingExport = true }
                    // 危险操作单独隔一组
                    MenuDivider()
                    MenuRow(
                        label = "删除笔记",
                        icon = Icons.Outlined.DeleteOutline,
                        iconTint = Danger,
                        textColor = Danger,
                        enabled = currentNoteId != null,
                    ) {
                        onDismissMore()
                        currentNoteId?.let(onRequestDelete)
                    }
                }
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
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
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
                if (value.text.isEmpty()) {
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
private fun BodyPreview(
    content: String,
    modifier: Modifier = Modifier,
    clearSelectionTick: Int = 0,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        if (content.isBlank()) {
            Text("还没有内容，请切换到「编辑」输入文字", color = Ghost, fontSize = 14.sp)
        } else {
            // 真 Markdown 渲染。字号 / 行高 / 配色的三个刻意选择，
            // 全写在 MarkdownBody 里 —— 它和导出图片共用同一份配置。
            // selectable：预览里选中哪段复制哪段（用户要的），抄出来是渲染后的纯文本
            MarkdownBody(
                markdown = content,
                selectable = true,
                clearSelectionTick = clearSelectionTick,
            )
        }
    }
}

