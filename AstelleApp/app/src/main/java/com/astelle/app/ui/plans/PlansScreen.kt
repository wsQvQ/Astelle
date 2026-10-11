package com.astelle.app.ui.plans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astelle.app.domain.logic.TaskLogic
import com.astelle.app.domain.model.History
import com.astelle.app.domain.model.Todo
import com.astelle.app.ui.components.GlassMenu
import com.astelle.app.ui.components.MenuRow
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.CenteredLineHeight
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Ghost
import com.astelle.app.ui.theme.Ink
import com.astelle.app.ui.theme.InkSoft
import com.astelle.app.ui.theme.LocalAstelleColors
import com.astelle.app.ui.theme.Muted
import com.astelle.app.ui.theme.Paper
import com.astelle.app.ui.theme.PaperWarm
import com.astelle.app.ui.theme.PressGlow
import com.astelle.app.ui.theme.SurfaceFloat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(14.dp)
private val HeroShape = RoundedCornerShape(18.dp)
private val DateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * 待办页（14 号计划 · v5 定稿）：**待办 / 历史** 左右分屏。
 * 顶栏同设置页语言（返回圆钮 + 大标题 34→18sp 上滑收缩 + 柔洗底）；
 * 底部＝首页工具栏语法：胶囊居中切换两区 + 加号球钉右下（随当前区直达新建）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlansScreen(
    onBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel(),
) {
    val colors = LocalAstelleColors.current
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val histories by viewModel.histories.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val scope = rememberCoroutineScope()

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // 编辑表单：null = 关；data = 编辑中的对象（null = 新建）
    var todoEditor by remember { mutableStateOf<Todo?>(null) }
    var showTodoEditor by remember { mutableStateOf(false) }
    var childParent by remember { mutableStateOf<Todo?>(null) }
    var historyEditor by remember { mutableStateOf<History?>(null) }
    var showHistoryEditor by remember { mutableStateOf(false) }
    var doneExpanded by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paperWarm)
            .navigationBarsPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        // ── 顶栏（同设置页：柔洗底 + 收缩 + 返回常驻） ──
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val t = scrollBehavior.state.collapsedFraction
                    if (t > 0f) {
                        val eased = t * t
                        val fade = (28.dp.toPx() / size.height).coerceIn(0f, 1f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to colors.paper.copy(alpha = eased),
                                (1f - fade) to colors.paper.copy(alpha = eased),
                                1f to colors.paper.copy(alpha = 0f),
                                startY = 0f,
                                endY = size.height,
                            ),
                        )
                        val y = size.height - 0.5.dp.toPx()
                        drawLine(
                            color = Divider.copy(alpha = eased * 0.45f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
        ) {
            val collapsed = scrollBehavior.state.collapsedFraction
            LargeTopAppBar(
                title = {
                    Text(
                        if (viewModel.pane == TaskPane.TODO) "待办" else "历史",
                        style = TextStyle(
                            fontSize = (34f - 16f * collapsed).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            lineHeightStyle = CenteredLineHeight,
                        ),
                        modifier = Modifier.padding(start = 2.dp),
                    )
                },
                navigationIcon = {
                    val interaction = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .padding(start = 8.dp)
                            .pressScale(interaction)
                            .size(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.paper)
                            .clickableNoRipple(interaction, onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = InkSoft)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = Ink,
                    navigationIconContentColor = InkSoft,
                ),
                scrollBehavior = scrollBehavior,
            )
        }

        Box(Modifier.weight(1f)) {
            // ── 左右滑切页（10-11）：两区一个 Pager，胶囊联动；边缘起滑让给系统返回手势 ──
            val pagerState = rememberPagerState(
                initialPage = if (viewModel.pane == TaskPane.TODO) 0 else 1,
            ) { 2 }
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    viewModel.pane = if (page == 0) TaskPane.TODO else TaskPane.HISTORY
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) {
                    TodoPane(
                        todos = todos,
                        today = today,
                        filter = viewModel.filter,
                        doneExpanded = doneExpanded,
                        onFilter = { viewModel.filter = it },
                        onToggleDoneExpanded = { doneExpanded = !doneExpanded },
                        onToggle = { id, done -> viewModel.toggleTodo(id, done) },
                        onEdit = { todoEditor = it; childParent = null; showTodoEditor = true },
                        onDelete = { viewModel.deleteTodo(it) },
                        onAddChild = { parent ->
                            childParent = parent
                            todoEditor = null
                            showTodoEditor = true
                        },
                    )
                } else {
                    HistoryPane(
                        histories = histories,
                        today = today,
                        onEdit = { historyEditor = it; showHistoryEditor = true },
                        onReorder = { viewModel.reorderHistories(it) },
                    )
                }
            }

            // ── 底部双件套（首页工具栏语法：胶囊 + 加号球**整组居中**，相对位置不变） ──
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TaskPill(
                    pane = viewModel.pane,
                    todoCount = todos.count { !it.isDone && it.parentId == null },
                    historyCount = histories.size,
                    onPane = { pane ->
                        scope.launch {
                            pagerState.animateScrollToPage(if (pane == TaskPane.TODO) 0 else 1)
                        }
                    },
                )
                Spacer(Modifier.width(10.dp))
                Ball(
                    onClick = {
                        when (viewModel.pane) {
                            TaskPane.TODO -> showQuickAdd = true
                            TaskPane.HISTORY -> {
                                historyEditor = null
                                showHistoryEditor = true
                            }
                        }
                    },
                )
            }
        }
    }

    if (showQuickAdd) {
        QuickAddDialog(
            onDismiss = { showQuickAdd = false },
            onDone = { title, dueDate, items ->
                viewModel.addTodoGroup(title, dueDate, items)
                showQuickAdd = false
            },
        )
    }

    if (showTodoEditor) {
        val parentId = todoEditor?.parentId ?: childParent?.id
        val parentTitle = parentId?.let { pid -> todos.firstOrNull { it.id == pid }?.title }
        TodoEditorDialog(
            initial = todoEditor,
            parentId = parentId,
            parentTitle = parentTitle,
            onDismiss = { showTodoEditor = false },
            onSave = { title, due, pid ->
                val t = todoEditor
                if (t == null) viewModel.addTodo(title, due, pid)
                else viewModel.editTodo(t.id, title, due, pid)
                showTodoEditor = false
            },
            onDelete = {
                todoEditor?.let { viewModel.deleteTodo(it.id) }
                showTodoEditor = false
            },
        )
    }

    if (showHistoryEditor) {
        HistoryEditorDialog(
            initial = historyEditor,
            onDismiss = { showHistoryEditor = false },
            onSave = { title, start, pinned ->
                val h = historyEditor
                if (h == null) viewModel.addHistory(title, start, pinned)
                else viewModel.editHistory(h.id, title, start, pinned)
                showHistoryEditor = false
            },
            onDelete = {
                historyEditor?.let { viewModel.deleteHistory(it.id) }
                showHistoryEditor = false
            },
        )
    }
}

// ═════════════════════════ 待办面板 ═════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoPane(
    todos: List<Todo>,
    today: LocalDate,
    filter: TaskLogic.Filter,
    doneExpanded: Boolean,
    onFilter: (TaskLogic.Filter) -> Unit,
    onToggleDoneExpanded: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (Todo) -> Unit,
    onDelete: (String) -> Unit,
    onAddChild: (Todo) -> Unit,
) {
    val colors = LocalAstelleColors.current
    val activeParents = todos.filter { it.parentId == null && !it.isDone }
    val parentIds = activeParents.map { it.id }.toSet()
    val shownParents = activeParents.filter { TaskLogic.matchesFilter(it, today, filter) }
    val treeInput = todos.filter { it.id in parentIds || it.parentId in parentIds }
        .filter { it.parentId == null && TaskLogic.matchesFilter(it, today, filter) || it.parentId in shownParents.map { p -> p.id }.toSet() }
    val flat = TaskLogic.flatten(treeInput, today)
    val doneTop = todos.filter { it.isDone && it.parentId == null }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        // 筛选 chips（同侧栏筛选语言）
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            FilterChip("全部", activeParents.size, filter == TaskLogic.Filter.ALL) { onFilter(TaskLogic.Filter.ALL) }
            FilterChip(
                "今天",
                activeParents.count { TaskLogic.isDueToday(today, it.dueDate) },
                filter == TaskLogic.Filter.TODAY,
            ) { onFilter(TaskLogic.Filter.TODAY) }
            FilterChip(
                "已过期",
                activeParents.count { TaskLogic.isOverdue(today, it.dueDate) },
                filter == TaskLogic.Filter.OVERDUE,
            ) { onFilter(TaskLogic.Filter.OVERDUE) }
        }

        if (flat.isEmpty()) {
            EmptyHint("暂无待办")
        }

        flat.forEach { item ->
            val isSub = item.parentId != null
            val progress = if (!isSub) TaskLogic.parentProgress(todos, item.id) else null
            TodoRow(
                todo = item,
                today = today,
                isSub = isSub,
                progress = progress,
                onToggle = { onToggle(item.id, it) },
                onEdit = { onEdit(item) },
                onDelete = { onDelete(item.id) },
                onAddChild = if (isSub) null else ({ onAddChild(item) }),
            )
        }

        // 已完成折叠
        if (doneTop.isNotEmpty()) {
            DoneFold(
                done = doneTop,
                expanded = doneExpanded,
                onToggleExpand = onToggleDoneExpanded,
                onToggle = onToggle,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAstelleColors.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.accentMist else Color.Transparent)
            .border(
                1.dp,
                if (selected) Accent else colors.divider,
                RoundedCornerShape(999.dp),
            )
            .clickableNoRipple(interaction, onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
    ) {
        Text(
            "$label $count",
            fontSize = 11.5.sp,
            color = if (selected) Accent else colors.inkSoft,
            style = TextStyle(letterSpacing = 0.12.sp, lineHeightStyle = CenteredLineHeight),
        )
    }
}

/** 勾选圈：未勾=描边圆 · 勾了=Accent 填充打勾 · 母项带子=进度环 */
@Composable
private fun TaskCheckbox(done: Boolean, progress: Pair<Int, Int>?, small: Boolean) {
    val colors = LocalAstelleColors.current
    val sizeDp = if (small) 17.dp else 22.dp
    val stroke = if (small) 1.5.dp else 1.7.dp
    Canvas(Modifier.size(sizeDp)) {
        val r = this.size.minDimension / 2f - stroke.toPx() / 2f
        when {
            done -> {
                drawCircle(color = Accent, radius = r + stroke.toPx() / 2f)
                val a = r * 0.52f
                drawLine(
                    color = colors.paper,
                    start = Offset(size.width / 2f - a, size.height / 2f + a * 0.12f),
                    end = Offset(size.width / 2f - a * 0.18f, size.height / 2f + a * 0.85f),
                    strokeWidth = stroke.toPx() * 1.25f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.paper,
                    start = Offset(size.width / 2f - a * 0.18f, size.height / 2f + a * 0.85f),
                    end = Offset(size.width / 2f + a * 1.05f, size.height / 2f - a * 0.72f),
                    strokeWidth = stroke.toPx() * 1.25f,
                    cap = StrokeCap.Round,
                )
            }
            progress != null -> {
                val (doneN, total) = progress
                drawCircle(
                    color = colors.divider,
                    radius = r,
                    style = Stroke(width = stroke.toPx() * 1.35f),
                )
                if (doneN > 0) {
                    drawArc(
                        color = Accent,
                        startAngle = -90f,
                        sweepAngle = 360f * doneN / total,
                        useCenter = false,
                        style = Stroke(width = stroke.toPx() * 1.35f, cap = StrokeCap.Round),
                    )
                }
            }
            else -> {
                drawCircle(
                    color = InkSoft,
                    radius = r,
                    style = Stroke(width = stroke.toPx()),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoRow(
    todo: Todo,
    today: LocalDate,
    isSub: Boolean,
    progress: Pair<Int, Int>?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddChild: (() -> Unit)? = null,
) {
    val colors = LocalAstelleColors.current
    val overdue = TaskLogic.isOverdue(today, todo.dueDate)
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Box {
        Row(
            Modifier
                .padding(
                    start = if (isSub) 34.dp else 14.dp,
                    end = 14.dp,
                    top = 4.dp,
                    bottom = 8.dp,
                )
                .fillMaxWidth()
                .clip(CardShape)
                .background(if (overdue && !isSub) colors.dangerBg else colors.paper)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onEdit,
                    onLongClick = { menuOpen = true },
                )
                .padding(horizontal = 16.dp, vertical = if (isSub) 9.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tapInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .pressScale(tapInteraction)
                    .clickableNoRipple(tapInteraction) { onToggle(!todo.isDone) },
            ) {
                TaskCheckbox(done = todo.isDone, progress = progress, small = isSub)
            }
            Spacer(Modifier.width(if (isSub) 12.dp else 13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    todo.title,
                    fontSize = if (isSub) 14.5.sp else 16.5.sp,
                    color = if (todo.isDone) colors.muted else colors.ink,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                    style = TextStyle(lineHeightStyle = CenteredLineHeight),
                )
                if (!isSub) {
                    val meta = buildList {
                        if (overdue) add("已过期 · ${todo.dueDate!!.format(DateFmt)}")
                        else if (TaskLogic.isDueToday(today, todo.dueDate)) add("今天到期")
                        else if (todo.dueDate != null) add("截止 ${todo.dueDate.format(DateFmt)}")
                        else add("无截止")
                        val p = progress
                        if (p != null) add("${p.first} / ${p.second} 已完成")
                    }
                    Row(
                        Modifier.padding(top = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        meta.forEach { m ->
                            val danger = m.startsWith("已过期")
                            val todayChip = m == "今天到期"
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (todayChip) colors.accentMist else Color.Transparent)
                                    .border(
                                        1.dp,
                                        when {
                                            danger -> colors.danger.copy(alpha = 0.35f)
                                            todayChip -> Accent.copy(alpha = 0.35f)
                                            else -> colors.divider
                                        },
                                        RoundedCornerShape(999.dp),
                                    )
                                    .padding(horizontal = 9.dp, vertical = 2.5.dp),
                            ) {
                                Text(
                                    m,
                                    fontSize = 11.sp,
                                    color = if (danger) colors.danger else if (todayChip) Accent else colors.muted,
                                    style = TextStyle(letterSpacing = 0.1.sp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            // 右侧天数位
            val right = countdownText(today, todo.dueDate)
            Text(
                right.first,
                style = numeralStyle(if (isSub) 17f else 24f, FontWeight.Medium),
                color = when {
                    todo.isDone -> colors.muted
                    right.second -> colors.danger
                    else -> Accent
                },
            )
            if (right.first != "今天" && right.first != "—") {
                Spacer(Modifier.width(3.dp))
                Text("天", fontSize = 10.5.sp, color = colors.muted, style = TextStyle(lineHeightStyle = CenteredLineHeight))
            }
        }

        // 长按菜单（编辑 / 添加子项 / 删除 —— 子项从卡片派生，表单里零选择器）
        GlassMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            MenuRow(label = "编辑", icon = Icons.Outlined.Add) { menuOpen = false; onEdit() }
            if (onAddChild != null) {
                MenuRow(label = "添加子项", icon = Icons.Outlined.Add) { menuOpen = false; onAddChild() }
            }
            MenuRow(label = "删除", icon = Icons.Outlined.Add) { menuOpen = false; onDelete() }
        }
    }
}

@Composable
private fun DoneFold(
    done: List<Todo>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (Todo) -> Unit,
    onDelete: (String) -> Unit,
) {
    val colors = LocalAstelleColors.current
    val interaction = remember { MutableInteractionSource() }
    Column {
        Row(
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.divider.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .clickableNoRipple(interaction, onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TaskCheckbox(done = true, progress = null, small = true)
            Spacer(Modifier.width(10.dp))
            Text("已完成 · ${done.size}", fontSize = 12.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.12.sp))
            Spacer(Modifier.width(10.dp))
            Text(
                done.first().title,
                fontSize = 13.sp,
                color = colors.muted,
                textDecoration = TextDecoration.LineThrough,
            )
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "▴" else "▾", fontSize = 11.sp, color = colors.muted)
        }
        if (expanded) {
            done.forEach { item ->
                TodoRow(
                    todo = item,
                    today = LocalDate.now(),
                    isSub = false,
                    progress = null,
                    onToggle = { onToggle(item.id, it) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item.id) },
                )
            }
        }
    }
}

// ═════════════════════════ 历史面板 ═════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryPane(
    histories: List<History>,
    today: LocalDate,
    onEdit: (History) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    // 排序模式（10-11：长按出汉堡手柄，拖动调序；松手落库退出）
    var order by remember { mutableStateOf<List<String>?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    val pitchPx = with(LocalDensity.current) { 76.dp.toPx() }

    val shown = if (order != null) {
        order.orEmpty().mapNotNull { id -> histories.firstOrNull { it.id == id } }
    } else {
        TaskLogic.sortHistories(histories, today)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        if (shown.isEmpty()) {
            EmptyHint("暂无历史")
            return@Column
        }

        if (order == null) {
            // 正常态：置顶/第一项 → 头条巨型卡，其余为行；长按任意一条进入排序
            val hero = shown.firstOrNull { it.isPinned } ?: shown.first()
            HeroHistoryCard(
                history = hero,
                today = today,
                onEdit = { onEdit(hero) },
                onStartReorder = { order = shown.map { it.id } },
            )
            shown.filter { it.id != hero.id }.forEach { h ->
                HistoryRow(
                    history = h,
                    today = today,
                    reorder = false,
                    dragging = false,
                    dragDelta = 0f,
                    onEdit = { onEdit(h) },
                    onStartReorder = { order = shown.map { it.id } },
                    onDragStart = {},
                    onDrag = {},
                    onDragEnd = {},
                )
            }
        } else {
            // 排序模式：全员统一行 + 左缘汉堡手柄（≈），拖动换位
            shown.forEach { h ->
                HistoryRow(
                    history = h,
                    today = today,
                    reorder = true,
                    dragging = draggingId == h.id,
                    dragDelta = if (draggingId == h.id) dragDelta else 0f,
                    onEdit = { order = null; onEdit(h) },
                    onStartReorder = {},
                    onDragStart = { draggingId = h.id; dragDelta = 0f },
                    onDrag = { delta ->
                        dragDelta += delta
                        val current = order.orEmpty().toMutableList()
                        val i = current.indexOf(h.id)
                        if (i >= 0 && dragDelta > pitchPx && i < current.lastIndex) {
                            current[i] = current[i + 1]
                            current[i + 1] = h.id
                            order = current
                            dragDelta -= pitchPx
                        } else if (i > 0 && dragDelta < -pitchPx) {
                            current[i] = current[i - 1]
                            current[i - 1] = h.id
                            order = current
                            dragDelta += pitchPx
                        }
                    },
                    onDragEnd = {
                        val finalOrder = order.orEmpty()
                        draggingId = null
                        dragDelta = 0f
                        order = null
                        onReorder(finalOrder)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroHistoryCard(
    history: History,
    today: LocalDate,
    onEdit: () -> Unit,
    onStartReorder: () -> Unit,
) {
    val colors = LocalAstelleColors.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(HeroShape)
            .background(colors.paper)
            .border(1.dp, colors.divider, HeroShape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onEdit,
                onLongClick = onStartReorder, // 长按 = 进入拖拽排序
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (history.isPinned) {
                Text("置顶", fontSize = 10.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.25.sp))
            }
            Text(history.title, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = colors.ink)
            Text(
                "起始 ${history.startDate.format(DateFmt)}" +
                    if (history.note.isNotEmpty()) " · ${history.note}" else "",
                fontSize = 11.sp,
                color = colors.muted,
                style = TextStyle(letterSpacing = 0.05.sp),
                modifier = Modifier.padding(top = 8.dp),
            )
            val anniv = TaskLogic.daysToNextAnniversary(today, history.startDate)
            if (anniv in 1..365) {
                Text(
                    "距下个周年 · $anniv 天",
                    fontSize = 10.5.sp,
                    color = Accent,
                    style = TextStyle(letterSpacing = 0.08.sp),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                TaskLogic.daysSince(today, history.startDate).toString(),
                style = numeralStyle(68f, FontWeight.Light, tracking = -0.035f),
                color = colors.inkSoft,
            )
            Text("天", fontSize = 10.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.25.sp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    history: History,
    today: LocalDate,
    reorder: Boolean,
    dragging: Boolean,
    dragDelta: Float,
    onEdit: () -> Unit,
    onStartReorder: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colors = LocalAstelleColors.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .padding(horizontal = 14.dp, vertical = 0.dp)
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .graphicsLayer { translationY = dragDelta }
            .clip(CardShape)
            .background(colors.paper)
            .border(1.dp, colors.divider, CardShape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onEdit,
                onLongClick = onStartReorder,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (reorder) {
            // 汉堡手柄（≡）：只在排序模式出现，拖它换位
            Box(
                Modifier
                    .size(36.dp)
                    .pointerInput(history.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { _, amount -> onDrag(amount.y) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = "拖动排序",
                    tint = colors.muted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(history.title, fontSize = 16.5.sp, color = colors.ink)
                if (history.isPinned) {
                    Spacer(Modifier.width(8.dp))
                    Text("置顶", fontSize = 10.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.2.sp))
                }
            }
            Text(
                "起始 ${history.startDate.format(DateFmt)}" +
                    if (history.note.isNotEmpty()) " · ${history.note}" else "",
                fontSize = 11.sp,
                color = colors.muted,
                style = TextStyle(letterSpacing = 0.05.sp),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(92.dp)) {
            Text(
                TaskLogic.daysSince(today, history.startDate).toString(),
                style = numeralStyle(32f, FontWeight.Medium, tracking = -0.025f),
                color = colors.inkSoft,
            )
            Text("天", fontSize = 10.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.22.sp), modifier = Modifier.padding(top = 5.dp))
        }
    }
}

// ═════════════════════════ 底部双件套 ═════════════════════════

@Composable
private fun TaskPill(
    pane: TaskPane,
    todoCount: Int,
    historyCount: Int,
    onPane: (TaskPane) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAstelleColors.current
    val thumb by animateFloatAsState(
        targetValue = if (pane == TaskPane.TODO) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "taskPill",
    )
    Box(
        modifier
            .width(236.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.paper)
            .border(1.dp, colors.divider, RoundedCornerShape(999.dp)),
    ) {
        // 滑块
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(4.dp)
                .graphicsLayer { translationX = (size.width) * thumb }
                .fillMaxWidth(0.5f)
                .height(38.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.accentMist),
        )
        Row(Modifier.fillMaxSize()) {
            PillSegment("待办", todoCount, pane == TaskPane.TODO, Modifier.weight(1f)) { onPane(TaskPane.TODO) }
            PillSegment("历史", historyCount, pane == TaskPane.HISTORY, Modifier.weight(1f)) { onPane(TaskPane.HISTORY) }
        }
    }
}

@Composable
private fun PillSegment(
    label: String,
    count: Int,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAstelleColors.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxSize()
            .clickableNoRipple(interaction, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 13.5.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) Accent else colors.muted,
                style = TextStyle(letterSpacing = 0.12.sp, lineHeightStyle = CenteredLineHeight),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                count.toString(),
                fontSize = 12.sp,
                color = if (active) Accent else colors.muted,
                style = TextStyle(lineHeightStyle = CenteredLineHeight),
            )
        }
    }
}

@Composable
private fun Ball(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAstelleColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .pressScale(interaction, pressedScale = 0.92f)
            .size(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (pressed) PressGlow else colors.paper)
            .border(1.dp, colors.divider, RoundedCornerShape(999.dp))
            .clickableNoRipple(interaction, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Add, "新建", tint = Accent, modifier = Modifier.size(20.dp))
    }
}

// ═════════════════════════ 表单 ═════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoEditorDialog(
    initial: Todo?,
    parentId: String?,
    parentTitle: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, due: LocalDate?, parentId: String?) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var due by remember { mutableStateOf(initial?.dueDate) }
    // 子项不设下拉：从卡片派生（长按→添加子项），表单里只留上下文横幅，✕ 可脱离
    var detached by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val effectiveParent = if (detached) null else parentId

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建待办" else "编辑待办", fontSize = 16.sp) },
        text = {
            Column {
                TaskTextField(value = title, onValueChange = { title = it }, hint = "标题…")
                Spacer(Modifier.height(12.dp))
                DateRow(label = "截止", date = due, onPick = { showDatePicker = true }, onClear = { due = null })
                if (parentTitle != null && !detached) {
                    Spacer(Modifier.height(10.dp))
                    ContextBanner(title = parentTitle, onDetach = { detached = true })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, due, effectiveParent) }) {
                Text("保存", color = Accent)
            }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = onDelete) { Text("删除", color = Muted) }
                }
                TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
            }
        },
        containerColor = SurfaceFloat,
        titleContentColor = Ink,
        textContentColor = Muted,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = due?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消", color = Muted) }
            },
            shape = RoundedCornerShape(20.dp),
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryEditorDialog(
    initial: History?,
    onDismiss: () -> Unit,
    onSave: (title: String, start: LocalDate, pinned: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var start by remember { mutableStateOf(initial?.startDate ?: LocalDate.now()) }
    var pinned by remember { mutableStateOf(initial?.isPinned ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新建历史" else "编辑历史", fontSize = 16.sp) },
        text = {
            Column {
                TaskTextField(value = title, onValueChange = { title = it }, hint = "标题…")
                Spacer(Modifier.height(12.dp))
                DateRow(label = "起始", date = start, onPick = { showDatePicker = true }, onClear = null)
                Spacer(Modifier.height(10.dp))
                PinRow(pinned = pinned, onToggle = { pinned = !pinned })
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, start, pinned) }) { Text("保存", color = Accent) }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = onDelete) { Text("删除", color = Muted) }
                }
                TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
            }
        },
        containerColor = SurfaceFloat,
        titleContentColor = Ink,
        textContentColor = Muted,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        start = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消", color = Muted) }
            },
            shape = RoundedCornerShape(20.dp),
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun TaskTextField(value: String, onValueChange: (String) -> Unit, hint: String) {
    val colors = LocalAstelleColors.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = Ink),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.paperWarm.copy(alpha = 0.8f))
            .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = Ghost.copy(alpha = 0.7f))
                inner()
            }
        },
    )
}

@Composable
private fun DateRow(label: String, date: LocalDate?, onPick: () -> Unit, onClear: (() -> Unit)?) {
    val colors = LocalAstelleColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.1.sp))
        Spacer(Modifier.width(12.dp))
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(999.dp))
                .clickableNoRipple(interaction, onPick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                date?.format(DateFmt) ?: "无截止",
                fontSize = 12.5.sp,
                color = if (date == null) colors.muted else Ink,
            )
        }
        if (date != null && onClear != null) {
            Spacer(Modifier.width(8.dp))
            val clearInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickableNoRipple(clearInteraction, onClear)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text("清除", fontSize = 12.sp, color = colors.muted)
            }
        }
    }
}

/** 置顶切换（长按菜单退役后，编辑表单接管置顶/删除） */
@Composable
private fun PinRow(pinned: Boolean, onToggle: () -> Unit) {
    val colors = LocalAstelleColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("置顶", fontSize = 12.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.1.sp))
        Spacer(Modifier.width(12.dp))
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (pinned) colors.accentMist else Color.Transparent)
                .border(1.dp, if (pinned) Accent.copy(alpha = 0.35f) else colors.divider, RoundedCornerShape(999.dp))
                .clickableNoRipple(interaction, onToggle)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                if (pinned) "已置顶" else "未置顶",
                fontSize = 12.5.sp,
                color = if (pinned) Accent else colors.muted,
            )
        }
    }
}

/**
 * 上下文横幅：「加入「冰岛行程准备」」+ ✕ 脱离。
 * 子项从卡片派生（长按→添加子项），表单里**零选择器零行话**——
 * 「母项」是我们的专业词，不上生产文案（10-11 用户点名）。
 */
@Composable
private fun ContextBanner(title: String, onDetach: () -> Unit) {
    val colors = LocalAstelleColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accentMist.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "加入「$title」",
                fontSize = 12.sp,
                color = colors.inkSoft,
                style = TextStyle(letterSpacing = 0.05.sp),
            )
        }
        Spacer(Modifier.width(8.dp))
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickableNoRipple(interaction, onDetach)
                .padding(6.dp),
        ) {
            Text("✕", fontSize = 12.sp, color = colors.muted)
        }
    }
}

/**
 * 快记框（10-11 用户亲自设计的交互）：加号弹出 →
 * 每行「勾选框图标 + 输入栏」；**回车 = 换行继续打**（勾选框只是图标，无实际功能，
 * 提示"你正在添加待办"）；第二行出现的瞬间顶部弹「**待办清单**」（默认标题，可自定义，
 * 弹出不抢焦点）；左下「设置截止」开日历；右下「完成」封存。
 * 一行 = 独立待办；多行 = 清单母项 + 子项。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun QuickAddDialog(
    onDismiss: () -> Unit,
    onDone: (title: String, due: LocalDate?, items: List<String>) -> Unit,
) {
    val colors = LocalAstelleColors.current
    var rows by remember { mutableStateOf(listOf("")) }
    var title by remember { mutableStateOf("待办清单") }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val lastFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val anyText = rows.any { it.isNotBlank() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(colors.paper)
                .padding(horizontal = 22.dp, vertical = 24.dp),
        ) {
            // ── 待办清单标题（≥2 行时弹出；默认「待办清单」可自定义） ──
            AnimatedVisibility(visible = rows.size > 1) {
                Column {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            fontFamily = FontFamily.Serif,
                        ),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    )
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ── 待办行（勾选框 = 纯图标） ──
            Column(Modifier.heightIn(max = 270.dp).verticalScroll(rememberScrollState())) {
                rows.forEachIndexed { index, value ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 装饰性勾选框（无实际功能，提示"正在添加待办"）
                        Box(
                            Modifier
                                .size(27.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .border(2.dp, colors.muted, RoundedCornerShape(9.dp)),
                        )
                        Spacer(Modifier.width(16.dp))
                        BasicTextField(
                            value = value,
                            onValueChange = { text -> rows = rows.toMutableList().also { it[index] = text } },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 20.sp, color = Ink),
                            cursorBrush = SolidColor(Accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                rows = rows + ""
                                lastFocus.requestFocus()
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (index == rows.lastIndex) {
                                        Modifier.focusRequester(lastFocus)
                                    } else {
                                        Modifier
                                    }
                                )
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown &&
                                        e.key == Key.Backspace &&
                                        value.isEmpty() &&
                                        rows.size > 1
                                    ) {
                                        rows = rows.toMutableList().also { it.removeAt(index) }
                                        true
                                    } else {
                                        false
                                    }
                                },
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    if (value.isEmpty()) {
                                        Text(
                                            "回车即可连续添加待办",
                                            fontSize = 20.sp,
                                            color = Ghost.copy(alpha = 0.7f),
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                }
            }

            // ── 底部：设置截止 + 完成 ──
            Row(
                Modifier.fillMaxWidth().padding(top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val dueInteraction = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accentMist)
                        .clickableNoRipple(dueInteraction) { showDatePicker = true }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⏱", fontSize = 15.sp, color = Ink)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        due?.format(DateFmt)?.let { "截止 $it" } ?: "设置截止",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        style = TextStyle(lineHeightStyle = CenteredLineHeight),
                    )
                }
                val doneInteraction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (anyText) Accent else colors.divider.copy(alpha = 0.5f))
                        .clickableNoRipple(doneInteraction) {
                            if (anyText) {
                                onDone(
                                    title,
                                    due,
                                    rows.map { it.trim() }.filter { it.isNotEmpty() },
                                )
                            }
                        }
                        .padding(horizontal = 26.dp, vertical = 13.dp),
                ) {
                    Text(
                        "完成",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (anyText) colors.paper else colors.muted,
                        style = TextStyle(lineHeightStyle = CenteredLineHeight),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = due?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        due = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消", color = Muted) }
            },
            shape = RoundedCornerShape(20.dp),
        ) { DatePicker(state = state) }
    }
}

// ═════════════════════════ 小件 ═════════════════════════

@Composable
private fun EmptyHint(text: String) {
    val colors = LocalAstelleColors.current
    Box(
        Modifier
            .padding(horizontal = 14.dp, vertical = 18.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.5.dp, colors.divider, CardShape)
            .padding(vertical = 34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 12.5.sp, color = colors.muted, style = TextStyle(letterSpacing = 0.25.sp))
    }
}

/**
 * 数字专用样式（10-11 用户定调）：衬线标题 × **轻量无衬线数字** + tabular 等宽 ——
 * 编辑感对比，比一路衬线更高级。天数/倒计时全走这套。
 */
private fun numeralStyle(size: Float, weight: FontWeight, tracking: Float = -0.02f): TextStyle =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = tracking.sp,
        fontFeatureSettings = "tnum",
        lineHeightStyle = CenteredLineHeight,
    )

/** 右侧倒计时文字 + 是否告急（砖红） */
private fun countdownText(today: LocalDate, due: LocalDate?): Pair<String, Boolean> {
    if (due == null) return "—" to false
    val d = TaskLogic.daysUntil(today, due)
    return when {
        d < 0 -> (-d).toString() to true
        d == 0 -> "今天" to false
        d <= 3 -> d.toString() to true
        else -> d.toString() to false
    }
}

// ── 手感小件（与设置页 pressTactile 同口径） ──

@Composable
private fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    pressedScale: Float = 0.94f,
): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.clickableNoRipple(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.then(
    Modifier.combinedClickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
    )
)
