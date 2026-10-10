package com.astelle.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.heightIn
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
import com.astelle.app.ui.components.GlassAlertDialog
import com.astelle.app.ui.components.GlassMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.domain.model.Folder
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
import com.astelle.app.ui.theme.FolderBody
import com.astelle.app.ui.theme.FolderHead
import com.astelle.app.ui.theme.Ghost
import com.astelle.app.ui.theme.Ink
import com.astelle.app.ui.theme.CenteredLineHeight
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
    /** 文件夹置顶切换（④） */
    onToggleFolderPin: (String) -> Unit,
    onMoveNoteToFolder: (String, String?) -> Unit,
    onNewNoteInFolder: (String) -> Unit,
    onNavigate: (AstelleDestination) -> Unit,
    /** 卡片默认密度（设置页）：true = 默认收起 */
    initialCompactCards: Boolean = false,
) {
    // 长按分类组头弹出的重命名 / 删除确认
    var renaming by remember { mutableStateOf<Folder?>(null) }
    var deleting by remember { mutableStateOf<Folder?>(null) }
    // 卡片密度（用户 10-09 C）：收起 = 只留「标题 + 日期」，展开 = 全卡。
    // 默认值由设置页「卡片默认密度」供给（10-10）
    var compactCards by remember { mutableStateOf(initialCompactCards) }

    if (renaming != null) {
        val target = renaming!!
        var draft by remember(target.id) { mutableStateOf(target.name) }
        GlassAlertDialog(
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
        )
    }

    if (deleting != null) {
        val target = deleting!!
        val count = notes.count { it.folderId == target.id }
        GlassAlertDialog(
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
        )
    }

    /* ---------- 新建分类的内联输入条 ---------- */

    var showCatInput by remember { mutableStateOf(false) }
    var catName by remember { mutableStateOf("") }

    fun submitCategory() {
        if (catName.isNotBlank()) onAddFolder(catName)
        catName = ""
        showCatInput = false
    }

    fun closeCategoryInput() {
        catName = ""
        showCatInput = false
    }

    // 自绘容器：不用 ModalDrawerSheet（避免多余阴影/内边距）
    // 内容避开状态栏（背景仍可铺满到顶，边到边）
    //
    // 输入条开着时，点空白处也收起来。用「只看不拿」的手势检测实现：
    // 全程不 consume 任何事件，所以滚动、点卡片都不受影响；而一旦有人
    // 消费过事件（拖动滚动、点到按钮），waitForUpOrCancellation 会返回
    // null，这里也就不会误关。
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pointerInput(showCatInput) {
                if (!showCatInput) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = true)
                    if (waitForUpOrCancellation() != null) closeCategoryInput()
                }
            },
    ) {
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
                            // 和密度钮一般高（28dp）—— 还原到原来的体量并一起做大（用户 10-10）
                            .height(28.dp)
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${notes.size} 篇",
                            fontFamily = mono,
                            fontSize = 11.sp,
                            color = Muted,
                            style = TextStyle(lineHeightStyle = CenteredLineHeight),
                        )
                    }
                    // 「N 篇」右边：**卡片密度开关**（用户 10-09 C）—— 一下把所有
                    // 笔记卡收成「标题 + 日期」，再点展开。放这儿而不是新建按钮旁：
                    // 它是「列表怎么看」的视图开关，和统计同族；新建那边是「创造」，语义不掺和
                    Spacer(Modifier.width(6.dp))
                    val densityInteraction = remember { MutableInteractionSource() }
                    val densityPressed by densityInteraction.collectIsPressedAsState()
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (densityPressed) AccentMist else Paper.copy(alpha = 0.7f))
                            .clickable(interactionSource = densityInteraction, indication = null) {
                                compactCards = !compactCards
                            }
                            // 做大（用户 10-10：是把密度钮做大，不是把胶囊做小）—— 28dp 和「N 篇」齐平
                            .height(28.dp)
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (compactCards) Icons.Outlined.UnfoldMore else Icons.Outlined.UnfoldLess,
                            contentDescription = if (compactCards) "展开卡片" else "收起卡片",
                            tint = if (compactCards) Accent else Muted,
                            modifier = Modifier.size(17.dp),
                        )
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
                    .onFocusChanged {
                        searchFocused = it.isFocused
                        // 只能靠 isFocused == true 触发：这个回调在挂载时
                        // 会先以 false 跑一次，无条件关会把刚打开的输出条立刻收掉
                        if (it.isFocused) closeCategoryInput()
                    }
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
                    ) { closeCategoryInput(); onImportMarkdown() }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AstelleIcons.Import, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "导入 Markdown",
                        fontSize = 12.sp,
                        color = Muted,
                        // 不再「沉下去」：中文行盒裁剪后才真居中
                        style = TextStyle(lineHeightStyle = CenteredLineHeight),
                    )
                }
            }

            // 筛选 chip ＋ 新增分类
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Chip("全部", selected = filter == NoteFilter.All) {
                    closeCategoryInput(); onFilter(NoteFilter.All)
                }
                Spacer(Modifier.width(8.dp))
                Chip("文件夹", selected = filter == NoteFilter.Folders) {
                    closeCategoryInput(); onFilter(NoteFilter.Folders)
                }
                Spacer(Modifier.width(8.dp))
                Chip("收藏", selected = filter == NoteFilter.Favorite) {
                    closeCategoryInput(); onFilter(NoteFilter.Favorite)
                }
                Spacer(Modifier.weight(1f))
                // 新增分类入口。＋ 转 45° 就成了 ×，不用额外文案解释「再点一下能收起」。
                // 用 Material 的 Add 而不是文字「＋」：全角 ＋ 的字形在行盒里会偏下，
                // 用图标才保证几何居中
                val folderInteraction = remember { MutableInteractionSource() }
                val folderPressed by folderInteraction.collectIsPressedAsState()
                val plusRotation by animateFloatAsState(
                    targetValue = if (showCatInput) 45f else 0f,
                    animationSpec = tween(200, easing = BrandCurve),
                    label = "plusRotation",
                )
                Box(
                    modifier = Modifier
                        // 无底色（③ 拍板）：这颗是**新建文件夹**（文件夹图标 +「新建」二字），
                        // 和顶栏 ✎ 新建笔记区分；按压才亮暖底
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (folderPressed || showCatInput) AccentMist else Color.Transparent)
                        .clickable(interactionSource = folderInteraction, indication = null) {
                            when {
                                !showCatInput -> showCatInput = true
                                // 开着且有字 → 这一下就是提交；开着但空着 → 收起来
                                catName.isNotBlank() -> submitCategory()
                                else -> closeCategoryInput()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            // 文件夹带 ＋，细线自绘版（用户 10-10：大小不动、线做细）；
                            // 一眼是「新建文件夹」，和顶栏 ✎ 新建笔记区分
                            AstelleIcons.FolderPlus,
                            contentDescription = "新建分类",
                            tint = if (folderPressed || showCatInput) Accent else Ghost,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "新建",
                            fontSize = 14.sp,
                            // 窄栏/大字号下不许折成「新\n建」两行（10-10 实测翻车）
                            maxLines = 1,
                            softWrap = false,
                            style = TextStyle(lineHeightStyle = CenteredLineHeight),
                            color = if (folderPressed || showCatInput) Accent else Muted,
                        )
                    }
                }
            }

            // 内联新建分类输入条。Enter 提交；右边那个「新建」是给不知道
            // 回车能提交的人看的 —— 光靠键盘动作不算把功能做完整
            if (showCatInput) {
                val inputInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        // heightIn 而不是 height：写死高度的话，「新建」会被挤到
                        // 框外去（大字体下必然发生）。这里让整条随内容长高
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaperWarm.copy(alpha = 0.8f))
                        .border(1.dp, Accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Ink),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitCategory() }),
                        decorationBox = { innerTextField: @Composable () -> Unit ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (catName.isEmpty()) {
                                    Text("输入分类名…", fontSize = 12.sp, color = Ghost.copy(alpha = 0.7f))
                                }
                                innerTextField()
                            }
                        },
                    )
                    if (catName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                // 这一圈 padding 保证按钮永远比输入框矮一档，
                                // 不会顶到边框；曲率跟着同心走（外 10 − 内缩 4 = 6）
                                .padding(end = 5.dp, top = 4.dp, bottom = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Accent)
                                .clickable(interactionSource = inputInteraction, indication = null) {
                                    submitCategory()
                                }
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("新建", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
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
                animationSpec = tween(180, easing = BrandCurve),
                label = "drawerTopFade",
            )
            // 折叠态。默认全展开，所以只记「被折叠的」。
            // （花笺反过来：它默认全部收起。我们打开抽屉是为了看笔记，
            //   默认收起等于先甩你一张目录，多一步。）
            // 例外：「文件夹」视图默认**全收起** —— 它就是一张目录，点开才见文章（用户 10-09）
            val foldersOnly = filter == NoteFilter.Folders
            val collapsed = remember { mutableStateMapOf<String, Boolean>() }
            // 搜索时压平列表：搜索是「我要那一篇」，此时还按分类铺开会把结果
            // 埋在一串组头里。筛选保留分组，只是空组不再占位
            val flat = searchQuery.isNotBlank()
            // 文件夹视图要看空分类（分类本身就是内容）；收藏视图的空组只是噪音
            val groups = groupNotes(notes, folders, hideEmpty = filter == NoteFilter.Favorite)
            // 一个 map 两种语义：常规记「被折叠的」（默认展开）；
            // 文件夹视图记「被展开的」（默认收起）—— onToggle 两种都兼容
            val toggledOn = collapsed.filterValues { it }.keys
            val collapsedKeys =
                if (foldersOnly) (groups.map { it.key }.toSet() - toggledOn) else toggledOn
            val rows = buildRows(groups, collapsedKeys, flat = flat)
                // 文件夹视图：只留分类容器，收件箱的平铺卡不掺和
                .filter { !foldersOnly || it is DrawerRow.FolderGroup }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                ) {
                    items(rows, key = { it.key }) { row ->
                        when (row) {
                            is DrawerRow.FolderGroup -> FolderCard(
                                group = row.group,
                                collapsed = row.collapsed,
                                currentNoteId = currentNoteId,
                                allFolders = folders,
                                onToggle = {
                                    collapsed[row.group.key] = collapsed[row.group.key] != true
                                },
                                onRename = { renaming = row.group.folder },
                                onDelete = { deleting = row.group.folder },
                                onToggleFolderPin = { row.group.folder?.let { onToggleFolderPin(it.id) } },
                                onOpenNote = { closeCategoryInput(); onOpenNote(it) },
                                onAddNote = { closeCategoryInput(); onNewNoteInFolder(it) },
                                onTogglePin = onTogglePin,
                                onToggleFavorite = onToggleFavorite,
                                onRequestDelete = onRequestDelete,
                                onMoveToFolder = onMoveNoteToFolder,
                                compact = compactCards,
                            )
                            is DrawerRow.FlatNote -> NoteItem(
                                note = row.summary,
                                selected = row.summary.id == currentNoteId,
                                contained = false,
                                compact = compactCards,
                                folders = folders,
                                onClick = { closeCategoryInput(); onOpenNote(row.summary.id) },
                                onTogglePin = { onTogglePin(row.summary.id) },
                                onToggleFavorite = { onToggleFavorite(row.summary.id) },
                                onRequestDelete = { onRequestDelete(row.summary.id) },
                                onMoveToFolder = { onMoveNoteToFolder(row.summary.id, it) },
                                modifier = Modifier.animateItem(),
                            )
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
                DockBtn(AstelleIcons.Plan, "计划") { closeCategoryInput(); onNavigate(AstelleDestination.Plans) }
                Spacer(Modifier.width(14.dp))
                DockBtn(AstelleIcons.Diary, "日记") { closeCategoryInput(); onNavigate(AstelleDestination.Diary) }
                Spacer(Modifier.width(14.dp))
                DockBtn(Icons.Outlined.AutoAwesome, "AI") { closeCategoryInput(); onNavigate(AstelleDestination.Diary) }
                Spacer(Modifier.weight(1f))
                DockBtn(Icons.Outlined.Tune, "设置") { closeCategoryInput(); onNavigate(AstelleDestination.Settings) }
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
            style = TextStyle(lineHeightStyle = CenteredLineHeight),
        )
    }
}

/** 全项目动效共用这一条：cubic-bezier(.22, 1, .36, 1) */
private val BrandCurve = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * 一个分类容器：组头 + 组内条目拼成**一张**卡。
 *
 * 关键是那个「拼」—— 展开时组头下缘切直角、内容上缘也是直角，两者共用
 * 一条圆角边框，看上去就是一张卡分了标题栏和内容区；折叠时组头恢复四角圆，
 * 变回一张独立的小卡。（花笺的做法，比「一堆飘着的卡片 + 一个标题」清楚得多。）
 */
@Composable
private fun FolderCard(
    group: NoteGroup,
    collapsed: Boolean,
    currentNoteId: String?,
    allFolders: List<Folder>,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpenNote: (String) -> Unit,
    /** 文件夹置顶切换（④） */
    onToggleFolderPin: () -> Unit,
    onAddNote: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRequestDelete: (String) -> Unit,
    onMoveToFolder: (String, String?) -> Unit,
    /** 紧凑卡（用户 10-09 C）：透传给组内条目 */
    compact: Boolean = false,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            // 这圈描边才是把整组「立起来」的东西。之前只调填色，
            // 组头的亮度又正好撞上抽屉底，于是整块看起来就是背景的一部分
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Divider, RoundedCornerShape(10.dp)),
    ) {
        FolderHeader(
            name = group.name,
            count = group.notes.size,
            collapsed = collapsed,
            pinned = group.folder?.isPinned == true,
            onToggle = onToggle,
            onRename = onRename,
            onDelete = onDelete,
            onTogglePinFolder = onToggleFolderPin,
            onAddNote = group.folder?.let { folder -> { onAddNote(folder.id) } },
        )

        // 花笺的 `grid-template-rows: 0fr → 1fr`，Compose 版本就是它。
        // 好处一样：按内容的真实高度展开收起，不需要事先知道这一组有多高
        AnimatedVisibility(
            visible = !collapsed,
            enter = expandVertically(tween(250, easing = BrandCurve)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(250, easing = BrandCurve)) + fadeOut(tween(150)),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    // 圆角交给外层容器的 clip；这里只铺色
                    .background(FolderBody),
            ) {
                // 组头与内容之间的一道缝
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                // 四周都留 4dp：选中的高亮块不碰容器描边，像一块浮着的软块。
                // 之前只有 vertical、左右是 0，高亮就贴着线，看着脏
                Column(Modifier.padding(4.dp)) {
                if (group.notes.isEmpty()) {
                    Text(
                        "空文件夹",
                        fontSize = 11.sp,
                        color = Ghost.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    )
                } else {
                    group.notes.forEachIndexed { index, note ->
                        if (index > 0) HairLine()
                        NoteItem(
                            note = note,
                            selected = note.id == currentNoteId,
                            contained = true,
                            compact = compact,
                            folders = allFolders,
                            onClick = { onOpenNote(note.id) },
                            onTogglePin = { onTogglePin(note.id) },
                            onToggleFavorite = { onToggleFavorite(note.id) },
                            onRequestDelete = { onRequestDelete(note.id) },
                            onMoveToFolder = { onMoveToFolder(note.id, it) },
                        )
                    }
                }
                }
            }
        }
    }
}

/** 组内条目之间的发丝分割线 —— 比再画一张卡片轻，也让「同一组」看得出来 */
@Composable
private fun HairLine() {
    Box(
        Modifier
            .fillMaxWidth()
            // 和条目文字同一个左缘（4dp 外缩 + 9dp 内边距）
            .padding(horizontal = 9.dp)
            .height(1.dp)
            .background(Divider.copy(alpha = 0.7f)),
    )
}

/**
 * 分类组头。**展开时刻意把下缘切成直角**，好和下面的内容拼成一张卡；
 * 折叠时四角收圆，自己就是一张完整的小卡。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderHeader(
    name: String,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    /** 文件夹置顶（④）：置顶态要在名字旁亮出来，菜单里给切换入口 */
    pinned: Boolean = false,
    onTogglePinFolder: () -> Unit = {},
    /** 在这个分类里新建一篇。不传就不显示那枚按钮 */
    onAddNote: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // 箭头 200ms、内容 250ms —— 和花笺一致，箭头跟手一点
    val arrow by animateFloatAsState(
        targetValue = if (collapsed) -90f else 0f,
        animationSpec = tween(200, easing = BrandCurve),
        label = "folderArrow",
    )
    // 圆角由外面那层容器的 clip 统一管，这里只铺色 ——
    // 自己再切一次圆角的话，描边和圆角两套各画各的，接缝对不齐
    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                // 只有「按下去」才换色。折叠/展开颜色一致：
                // 收起只是「内容没了」，不是「换了个东西」
                .background(if (pressed) PaperWarm else FolderHead)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onToggle,
                    onLongClick = {
                        // 长按是个「藏起来」的入口，没有触觉反馈用户不知道自己触发了什么
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                // 13dp：和组内条目的文字同一个左缘（4dp 外缩 + 9dp 内边距）
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 箭头放最左，跟文件夹图标、名字、计数一起从左往右读
            Icon(
                AstelleIcons.Chevron,
                contentDescription = if (collapsed) "展开" else "折叠",
                tint = Ghost,
                modifier = Modifier.size(16.dp).rotate(arrow),
            )
            Spacer(Modifier.width(6.dp))
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
            // 置顶态亮在名字旁（颜色只表达状态）
            if (pinned) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.PushPin,
                    contentDescription = "已置顶",
                    tint = Accent,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text("$count", fontFamily = mono, fontSize = 11.sp, color = Ghost)
            // 「在这个分类里新建一篇」。用 Material 的 Add 图标，不用文字「＋」——
            // 全角 ＋ 的字形在行盒里偏下，和计数对不齐
            if (onAddNote != null) {
                val addInteraction = remember { MutableInteractionSource() }
                val addPressed by addInteraction.collectIsPressedAsState()
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(interactionSource = addInteraction, indication = null) {
                            onAddNote()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "在这个分类里新建笔记",
                        tint = if (addPressed) Accent else Ghost,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        GlassMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            MenuRow(
                label = if (pinned) "取消置顶" else "置顶",
                icon = Icons.Outlined.PushPin,
            ) { menuOpen = false; onTogglePinFolder() }
            MenuDivider()
            MenuRow(label = "重命名", icon = Icons.Outlined.Edit) { menuOpen = false; onRename() }
            MenuDivider()
            MenuRow(
                label = "删除分类",
                icon = Icons.Outlined.DeleteOutline,
                iconTint = Danger,
                textColor = Danger,
            ) { menuOpen = false; onDelete() }
        }
    }
}

/** 列表空的时候说清楚「为什么空」，比一片留白好 */
private fun emptyHint(query: String, filter: NoteFilter): String = when {
    query.isNotBlank() -> "没有匹配「$query」的笔记"
    filter == NoteFilter.Folders -> "还没有分类，右上角「新建」开一个"
    filter == NoteFilter.Favorite -> "还没有收藏的笔记"
    else -> "还没有笔记，去写第一条吧"
}

/**
 * 一条笔记。
 *
 * [contained] 为 true 时它住在某个分类容器里：自己没有背景和圆角，
 * 颜色交给外面那张卡，只负责在选中时铺一层淡淡的暖橙。
 * 为 false 时就是收件箱/搜索结果的独立卡片。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NoteItem(
    note: NoteSummary,
    selected: Boolean,
    contained: Boolean,
    /** 紧凑卡（用户 10-09 C）：只留标题 + 日期 */
    compact: Boolean = false,
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
    val haptics = LocalHapticFeedback.current
    // 菜单的第二层：选要移进哪个分类。用同一个菜单换内容，而不是再叠一层
    // DropdownMenu —— 嵌套弹窗的位置很难控制，在抽屉这种窄容器里尤其明显
    var moving by remember { mutableStateOf(false) }

    // 选中色条：0 → 20dp 展开，600ms。设计稿 §3 E 写了这条，花笺也是 600ms，
    // 之前一直没做，选中就成了「啪」一下变块颜色，看着生硬。
    // 色条**绝对定位**贴在最左缘：塞进 Row 内容里的话，选中时整块文字会横着跳一下
    val barHeight by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = tween(600, easing = BrandCurve),
        label = "selectBar",
    )
    val rowBg by animateColorAsState(
        targetValue = when {
            // 花笺是 bg-bamboo-mist/70 —— 只是「这块在被看」，不是高亮成一块色
            selected -> AccentMist.copy(alpha = 0.7f)
            pressed -> PaperWarm
            contained -> Color.Transparent
            else -> Paper
        },
        animationSpec = tween(600, easing = BrandCurve),
        label = "noteRowBg",
    )

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 容器里的条目贴着卡边；独立卡片自己留出卡片间的缝隙和左右缩进
                .padding(
                    start = if (contained) 0.dp else 4.dp,
                    end = if (contained) 0.dp else 4.dp,
                    top = if (contained) 0.dp else 3.dp,
                    bottom = if (contained) 0.dp else 3.dp,
                )
                .clip(
                    // 组内的条目也是圆角：选中的那条才是容器里的一块「软块」，
                    // 而不是一条横贯的色带（花笺 rounded-xl）
                    if (contained) RoundedCornerShape(6.dp) else RoundedCornerShape(14.dp)
                )
                .background(rowBg)
                // 独立卡片和分类容器共用同一条描边 —— 否则同一个列表里
                // 一半有边一半没边，是两种语言。组内的条目不再描，
                // 它们已经在描过边的容器里，再描就是双重边框
                .then(
                    if (contained) Modifier
                    else Modifier.border(1.dp, Divider, RoundedCornerShape(14.dp))
                )
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        // 长按是个「藏起来」的入口，没有触觉反馈用户不知道自己触发了什么
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                // 组内条目已经外缩 4dp，内容再退 9dp —— 文字仍落在距容器内缘 13dp，
                // 和组头齐平，高亮缩进时文字不动
                // 三行间距整体收了一档，位置和层级不变
                .padding(horizontal = if (contained) 9.dp else 13.dp, vertical = if (contained) 8.dp else 9.dp),
        ) {
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
                    // 状态徽标（②，用户 10-09 实测：图钉压根没画、星被挤成 8dp 隐形）——
                    // 置顶图钉 + 收藏星，13dp 起步，间距走 Spacer 不吃图标尺寸
                    if (note.isPinned) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = "已置顶",
                            tint = Accent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    if (note.isFavorite) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            AstelleIcons.Sparkle,
                            contentDescription = "已收藏",
                            tint = Accent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(dateLabel, fontFamily = mono, fontSize = 10.sp, color = Ghost)
                }
                // 摘要与字数都来自投影，不再从正文全文里现算
                val sum = note.preview
                // 紧凑卡只留标题 + 日期（用户 10-09 C）
                if (!compact) {
                    // sum != headline：标题为空时 headline 就是正文首行，再显示一遍纯属重复
                    if (sum.isNotBlank() && sum != headline) {
                        Text(
                            sum,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Text(
                        "$timeLabel · ${note.charCount} 字",
                        fontFamily = mono,
                        fontSize = 10.sp,
                        color = Ghost,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }

        // 选中色条。绝对定位贴在卡片最左缘 —— 不进 Row 的内容流，
        // 否则选中时整块文字会横着跳一下
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 3.dp)
                .width(3.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(topEnd = 99.dp, bottomEnd = 99.dp))
                .background(Accent.copy(alpha = 0.6f)),
        )

        GlassMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false; moving = false },
        ) {
            if (moving) {
                MenuRow(
                    label = "选择分类",
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    iconTint = Muted,
                    textColor = Muted,
                ) { moving = false }
                MenuRow(
                    label = if (note.folderId == null) "未分类 ✓" else "未分类",
                    icon = Icons.Outlined.Inbox,
                ) {
                    menuOpen = false; moving = false
                    // 已经在那组里就别白写一次库（会白白刷新 updatedAt）
                    if (note.folderId != null) onMoveToFolder(null)
                }
                folders.forEach { folder ->
                    MenuRow(
                        label = if (note.folderId == folder.id) "${folder.name} ✓" else folder.name,
                        icon = Icons.Outlined.FolderOpen,
                    ) {
                        menuOpen = false; moving = false
                        if (note.folderId != folder.id) onMoveToFolder(folder.id)
                    }
                }
            } else {
                MenuRow(
                    label = if (note.isPinned) "取消置顶" else "置顶",
                    icon = Icons.Outlined.PushPin,
                ) { menuOpen = false; onTogglePin() }
                MenuRow(
                    label = if (note.isFavorite) "取消收藏" else "收藏",
                    icon = Icons.Outlined.StarBorder,
                ) { menuOpen = false; onToggleFavorite() }
                // 一个分类都没有时不显示这项 —— 点进去只有「未分类」可选，纯属绕路
                if (folders.isNotEmpty()) {
                    MenuRow(
                        label = "移动到分类",
                        icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                    ) { moving = true }
                }
                // 危险操作单独隔一组
                MenuDivider()
                MenuRow(
                    label = "删除",
                    icon = Icons.Outlined.DeleteOutline,
                    iconTint = Danger,
                    textColor = Danger,
                ) { menuOpen = false; onRequestDelete() }
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
    // 用本年的真实天数，别写死 365：闰年里 12-31 → 元旦 的差会被算成 0，
    // 于是「明天元旦」整条文案凭空消失
    val yearDays = now.getActualMaximum(java.util.Calendar.DAY_OF_YEAR)
    val soonest = festivals.mapNotNull { (md, name) ->
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.MONTH, md / 100 - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, md % 100)
        val target = cal.get(java.util.Calendar.DAY_OF_YEAR)
        val delta = (target - nowDays + yearDays) % yearDays
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
