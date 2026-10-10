package com.astelle.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.BuildConfig
import com.astelle.app.data.settings.ColorMode
import com.astelle.app.data.settings.ImageQuality
import com.astelle.app.data.settings.MaterialStyle
import com.astelle.app.data.settings.SettingsStore
import com.astelle.app.ui.components.MenuRow
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.AccentMist
import com.astelle.app.ui.theme.CenteredLineHeight
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Ink
import com.astelle.app.ui.theme.InkSoft
import com.astelle.app.ui.theme.LocalAstelleColors
import com.astelle.app.ui.theme.Muted
import com.astelle.app.ui.theme.Paper
import com.astelle.app.ui.theme.PaperWarm
import com.astelle.app.ui.theme.SurfaceFloat

/**
 * 设置页（10-10 用户指定里程碑：**可以打开、可以退出**；10 号计划 RikkaHub 化）。
 *
 * 顶栏 = M3 LargeTopAppBar + exitUntilCollapsed：大标题「设置」34sp 上滑平滑收缩为
 * 18sp 钉顶，返回钮常驻 navigationIcon 槽；收缩后落一层 paper + 发丝线。
 * 组件口径：下拉 = 胶囊 + ExpandMore（点开旋转 180°）；开关 = M3 Switch；
 * 可点行整行按压淡入 PaperWarm；菜单沿用 MenuChrome 语言。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val colors = LocalAstelleColors.current
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }
    // 本地镜像一份：选完立刻反映在副标上（持久化走 store）
    var mode by remember { mutableStateOf(store.colorMode) }
    var dynamic by remember { mutableStateOf(store.dynamicColor) }
    var compactDefault by remember { mutableStateOf(store.compactCardsDefault) }
    var imageQuality by remember { mutableStateOf(store.imageQuality) }
    // 下拉菜单开关（提出来给「整行点击」共用）
    var modeOpen by remember { mutableStateOf(false) }
    var densityOpen by remember { mutableStateOf(false) }
    var qualityOpen by remember { mutableStateOf(false) }
    // 外观材质（10-11：预留「材质切换」二级菜单接口）+ 当前二级页
    var materialStyle by remember { mutableStateOf(store.materialStyle) }
    var subPage by remember { mutableStateOf<SettingsSubPage?>(null) }

    // ⚠️ 菜单/二级页开着时返回必须**只收浮层**（真机揪出的 bug：PredictiveBackHandler
    // 常开，把弹层的返回也吃了 → 直接退页）。本回调注册晚于它，dispatcher 里优先级更高。
    val anyMenuOpen = modeOpen || densityOpen || qualityOpen
    BackHandler(enabled = anyMenuOpen || subPage != null) {
        modeOpen = false
        densityOpen = false
        qualityOpen = false
        subPage = null
    }

    // ── 二级页：外观材质（10-11 预留材质切换接口；玻璃占位未开放）──
    if (subPage == SettingsSubPage.MATERIAL) {
        MaterialSubPage(
            current = materialStyle,
            onPick = {
                store.materialStyle = it
                materialStyle = it
            },
            onBack = { subPage = null },
        )
        return
    }

    // 1a：大标题上滑收缩（内容滚 → 顶栏折叠），返回钮常驻
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Column(
        Modifier
            .fillMaxSize()
            // ⚠️ 背景必须在 inset **外面**（10-10 修：状态栏那条露父级底色、和页面割裂）
            .background(colors.paperWarm)
            .navigationBarsPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        // 顶栏外套（10-11 用户嫌整块变色丑 → 自绘柔和洗底）：
        // 收缩度**平方缓入**——大半程通透、收尾才落定；纸色洗底只在底缘 28dp 渐隐，
        // 边界交给 45% 克制发丝线。draw 里读状态，不脏 composition。
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
            LargeTopAppBar(
                title = {
                    // 34sp 平滑收缩为 18sp（大小两行共用本 lambda、交叉淡入；
                    // 状态读在本 lambda 的重组域里，不牵连整页）
                    Text(
                        "设置",
                        style = TextStyle(
                            fontSize = (34f - 16f * scrollBehavior.state.collapsedFraction).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            lineHeightStyle = CenteredLineHeight,
                        ),
                        // M3 大标题基准 x=16dp，卡片沿在 18dp —— 补 2dp 对齐卡沿
                        modifier = Modifier.padding(start = 2.dp),
                    )
                },
                navigationIcon = {
                    // 1c：返回常驻（保留参考图的浅色圆钮口径，不裸奔）
                    QuietIconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "返回", onBack)
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    // 底色全交给外层自绘洗底（M3 自己的整块变色已废，10-11）
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = Ink,
                    navigationIconContentColor = InkSoft,
                ),
                scrollBehavior = scrollBehavior,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 通用设置 ──
            SettingSection("通用设置") {
                SettingRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "颜色模式",
                    subtitle = mode.label(),
                    onClick = { modeOpen = true },
                    trailing = {
                        ModeDropdown(mode, modeOpen, { modeOpen = it }) {
                            store.colorMode = it
                            mode = it
                        }
                    },
                )
                val sdkOk = android.os.Build.VERSION.SDK_INT >= 31
                SettingRow(
                    icon = Icons.Outlined.Palette,
                    title = "动态取色",
                    subtitle = if (sdkOk) {
                        "跟随壁纸取色（Material You）"
                    } else {
                        "需要 Android 12+"
                    },
                    enabled = sdkOk,
                    onClick = {
                        store.dynamicColor = !dynamic
                        dynamic = !dynamic
                    },
                    trailing = {
                        SettingsSwitch(
                            on = dynamic,
                            enabled = sdkOk,
                        ) { checked ->
                            store.dynamicColor = checked
                            dynamic = checked
                        }
                    },
                )
                SettingRow(
                    icon = Icons.Outlined.Layers,
                    title = "外观材质",
                    subtitle = materialStyle.label,
                    onClick = { subPage = SettingsSubPage.MATERIAL },
                )
                SettingRow(
                    icon = Icons.AutoMirrored.Outlined.List,
                    title = "卡片默认密度",
                    subtitle = if (compactDefault) "收起（标题 + 日期）" else "展开（全卡）",
                    onClick = { densityOpen = true },
                    trailing = {
                        PillDropdown(
                            if (compactDefault) "收起" else "展开",
                            densityOpen,
                            { densityOpen = it },
                        ) {
                            listOf(
                                "展开" to {
                                    store.compactCardsDefault = false
                                    compactDefault = false
                                },
                                "收起" to {
                                    store.compactCardsDefault = true
                                    compactDefault = true
                                },
                            )
                        }
                    },
                )
            }

            // ── 编辑与数据 ──
            SettingSection("编辑与数据") {
                SettingRow(
                    icon = Icons.Outlined.Image,
                    title = "图片压缩档位",
                    subtitle = imageQuality.subtitle(),
                    onClick = { qualityOpen = true },
                    trailing = {
                        PillDropdown(imageQuality.label(), qualityOpen, { qualityOpen = it }) {
                            ImageQuality.entries.map { q ->
                                q.label() to {
                                    store.imageQuality = q
                                    imageQuality = q
                                }
                            }
                        }
                    },
                )
            }

            // ── 关于 ──
            SettingSection("关于") {
                SettingRow(
                    icon = Icons.Outlined.Info,
                    title = "版本",
                    subtitle = BuildConfig.VERSION_NAME,
                )
                SettingRow(
                    icon = Icons.Outlined.Lock,
                    title = "数据说明",
                    subtitle = "本地优先，数据都在你的设备里",
                )
                SettingRow(
                    icon = Icons.Outlined.Code,
                    title = "开源许可",
                    subtitle = "Markwon · Compose · Coil 等",
                )
            }

            Spacer(Modifier.size(32.dp))
        }
    }
}

private fun ColorMode.label(): String = when (this) {
    ColorMode.LIGHT -> "浅色"
    ColorMode.DARK -> "深色"
    ColorMode.SYSTEM -> "跟随系统"
}

private fun ImageQuality.label(): String = when (this) {
    ImageQuality.HIGH -> "高质量"
    ImageQuality.STANDARD -> "标准"
    ImageQuality.SAVING -> "省空间"
}

private fun ImageQuality.subtitle(): String = when (this) {
    ImageQuality.HIGH -> "2400px / 92%，几乎无损"
    ImageQuality.STANDARD -> "1600px / 85%，均衡（默认）"
    ImageQuality.SAVING -> "1280px / 78%，最省空间"
}

/** 无涟漪圆钮（和抽屉/工具栏同一手感口径），浅色圆钮不裸奔 */
@Composable
private fun QuietIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Paper)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = InkSoft)
    }
}

/** 尾部下拉胶囊的箭头：ExpandMore，展开时旋转 180°（150ms 缓动，10 号 1b） */
@Composable
private fun Chevron(open: Boolean) {
    val rotation by animateFloatAsState(
        if (open) 180f else 0f,
        tween(150),
        label = "chevron",
    )
    Icon(
        Icons.Outlined.ExpandMore,
        contentDescription = null,
        tint = Muted,
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer { rotationZ = rotation },
    )
}

/** 尾部下拉胶囊（照参考图「浅色 ∨」的样式），菜单沿用 MenuChrome 语言 */
@Composable
private fun ModeDropdown(
    current: ColorMode,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onPick: (ColorMode) -> Unit,
) {
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PaperWarm)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onOpenChange(!open) }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current.label(),
                fontSize = 13.sp,
                color = Ink,
                style = TextStyle(lineHeightStyle = CenteredLineHeight),
            )
            Spacer(Modifier.width(5.dp))
            Chevron(open)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { onOpenChange(false) },
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceFloat,
            border = BorderStroke(1.dp, Divider),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            MenuRow(
                label = "浅色",
                icon = Icons.Outlined.LightMode,
                selected = current == ColorMode.LIGHT,
            ) {
                onOpenChange(false)
                onPick(ColorMode.LIGHT)
            }
            MenuRow(
                label = "深色",
                icon = Icons.Outlined.DarkMode,
                selected = current == ColorMode.DARK,
            ) {
                onOpenChange(false)
                onPick(ColorMode.DARK)
            }
            MenuRow(
                label = "跟随系统",
                icon = Icons.Outlined.BrightnessAuto,
                selected = current == ColorMode.SYSTEM,
            ) {
                onOpenChange(false)
                onPick(ColorMode.SYSTEM)
            }
        }
    }
}

/** 通用「胶囊 + 箭头下拉」：卡片密度、压缩档位等二/三选一都用它 */
@Composable
private fun PillDropdown(
    currentLabel: String,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    options: () -> List<Pair<String, () -> Unit>>,
) {
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PaperWarm)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onOpenChange(!open) }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                currentLabel,
                fontSize = 13.sp,
                color = Ink,
                style = TextStyle(lineHeightStyle = CenteredLineHeight),
            )
            Spacer(Modifier.width(5.dp))
            Chevron(open)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { onOpenChange(false) },
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceFloat,
            border = BorderStroke(1.dp, Divider),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            options().forEach { (label, action) ->
                // 勾只标当前项（10-11：以前每项挂勾 = 误导成全都选了）
                MenuRow(label = label, selected = label == currentLabel) {
                    onOpenChange(false)
                    action()
                }
            }
        }
    }
}

/** 开关：M3 Switch（单色 thumb/track，Accent/AccentMist 上色，10 号 1b） */
@Composable
private fun SettingsSwitch(
    on: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = on,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Accent,
            checkedTrackColor = AccentMist,
            checkedBorderColor = AccentMist,
            uncheckedThumbColor = Paper,
            uncheckedTrackColor = PaperWarm,
            uncheckedBorderColor = Divider,
            disabledCheckedThumbColor = Accent.copy(alpha = 0.45f),
            disabledCheckedTrackColor = AccentMist.copy(alpha = 0.45f),
            disabledCheckedBorderColor = AccentMist.copy(alpha = 0.45f),
            disabledUncheckedThumbColor = Paper.copy(alpha = 0.6f),
            disabledUncheckedTrackColor = PaperWarm,
            disabledUncheckedBorderColor = Divider.copy(alpha = 0.5f),
        ),
    )
}

/** 二级页枚举（10-11 预留：材质切换是第一个，后续二级菜单往这加） */
private enum class SettingsSubPage { MATERIAL }

/**
 * 二级页：外观材质（10-11 预留「材质切换」接口）。
 * 纸感 = 现行；液态玻璃 = 占位（候选 Kyant0/AndroidLiquidGlass，调研见
 * `docs/ui/11-liquid-glass-research.md`）。玻璃真正接入时在此处分叉材质管线。
 */
@Composable
private fun MaterialSubPage(
    current: MaterialStyle,
    onPick: (MaterialStyle) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalAstelleColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paperWarm)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietIconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "返回", onBack)
            Spacer(Modifier.width(6.dp))
            Text(
                "外观材质",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                style = TextStyle(lineHeightStyle = CenteredLineHeight),
            )
        }
        SettingSection("材质") {
            MaterialStyle.entries.forEach { style ->
                val isCurrent = style == current
                SettingRow(
                    icon = if (style == MaterialStyle.PAPER) {
                        Icons.Outlined.Layers
                    } else {
                        Icons.Outlined.Opacity
                    },
                    title = style.label,
                    subtitle = when {
                        !style.available -> "规划中 · 敬请期待"
                        isCurrent -> "当前使用"
                        else -> "点击切换"
                    },
                    onClick = if (style.available) ({ onPick(style) }) else null,
                    trailing = {
                        if (isCurrent) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Accent)
                        }
                    },
                )
            }
            Spacer(Modifier.size(32.dp))
        }
    }
}
