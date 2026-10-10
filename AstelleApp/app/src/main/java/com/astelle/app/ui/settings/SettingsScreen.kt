package com.astelle.app.ui.settings

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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.BuildConfig
import com.astelle.app.data.settings.ColorMode
import com.astelle.app.data.settings.ImageQuality
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
import com.astelle.app.ui.theme.PaperWarm
import com.astelle.app.ui.theme.SurfaceFloat

/**
 * 设置页（10-10 用户指定里程碑：**可以打开、可以退出**，先立框架）。
 *
 * 框架照用户给的参考图：分组小标 + 独立圆角卡片行；
 * 「通用设置」先做**颜色模式**（浅色 / 深色 / 跟随系统，选完即存即生效）。
 * ⚠️ 深色的完整落地 = 静态色 token 收进 AstelleColors 的色板搬家（下一批）；
 * M3 层已随选择翻转，手绘区（抽屉/卡片）待色板收编。
 */
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

    Column(
        Modifier
            .fillMaxSize()
            // ⚠️ 状态栏/导航栏内缩（10-10 修：返回箭头撞时钟、侧栏撞电池）
            .statusBarsPadding()
            .navigationBarsPadding()
            // 深底浅卡：页面比卡片深一档（照参考图的层次）
            .background(colors.paperWarm)
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶栏：返回（退出设置）+ 侧栏
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(999.dp))
                    // 参考图的返回钮：一枚浅色圆钮，不裸奔
                    .background(colors.paper)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = InkSoft)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.paper)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenDrawer)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text("侧栏", fontSize = 12.sp, color = Muted)
            }
        }

        Text(
            "设置",
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
        )

        // ── 通用设置 ──
        SettingSection("通用设置") {
            SettingRow(
                icon = Icons.Outlined.DarkMode,
                title = "颜色模式",
                subtitle = mode.label(),
                trailing = { ModeDropdown(mode) { store.colorMode = it; mode = it } },
            )
            SettingRow(
                icon = Icons.Outlined.Palette,
                title = "动态取色",
                subtitle = if (android.os.Build.VERSION.SDK_INT >= 31) {
                    "跟随壁纸取色（Material You）"
                } else {
                    "需要 Android 12+"
                },
                trailing = {
                    TogglePill(
                        on = dynamic,
                        enabled = android.os.Build.VERSION.SDK_INT >= 31,
                    ) {
                        store.dynamicColor = !dynamic
                        dynamic = !dynamic
                    }
                },
            )
            SettingRow(
                icon = Icons.Outlined.List,
                title = "卡片默认密度",
                subtitle = if (compactDefault) "收起（标题 + 日期）" else "展开（全卡）",
                trailing = {
                    PillDropdown(if (compactDefault) "收起" else "展开") {
                        listOf(
                            "展开" to { store.compactCardsDefault = false; compactDefault = false },
                            "收起" to { store.compactCardsDefault = true; compactDefault = true },
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
                trailing = {
                    PillDropdown(imageQuality.label()) {
                        ImageQuality.entries.map { q ->
                            q.label() to { store.imageQuality = q; imageQuality = q }
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

/** 无涟漪圆钮（和抽屉/工具栏同一手感口径） */
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = InkSoft)
    }
}

/** 尾部下拉胶囊（照参考图「浅色 ∨」的样式），菜单沿用 MenuChrome 语言 */
@Composable
private fun ModeDropdown(current: ColorMode, onPick: (ColorMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PaperWarm)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = !open }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current.label(), fontSize = 13.sp, color = Ink)
            Spacer(Modifier.width(6.dp))
            Text("∨", fontSize = 11.sp, color = Muted)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceFloat,
            border = BorderStroke(1.dp, Divider),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            MenuRow(label = "浅色", icon = Icons.Outlined.LightMode) { open = false; onPick(ColorMode.LIGHT) }
            MenuRow(label = "深色", icon = Icons.Outlined.DarkMode) { open = false; onPick(ColorMode.DARK) }
            MenuRow(label = "跟随系统", icon = Icons.Outlined.BrightnessAuto) { open = false; onPick(ColorMode.SYSTEM) }
        }
    }
}

/** 通用「胶囊 + ∨ 下拉」：卡片密度、压缩档位等二/三选一都用它 */
@Composable
private fun PillDropdown(currentLabel: String, options: () -> List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PaperWarm)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = !open }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(currentLabel, fontSize = 13.sp, color = Ink, style = TextStyle(lineHeightStyle = CenteredLineHeight))
            Spacer(Modifier.width(6.dp))
            Text("∨", fontSize = 11.sp, color = Muted)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = SurfaceFloat,
            border = BorderStroke(1.dp, Divider),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            options().forEach { (label, action) ->
                MenuRow(label = label, icon = Icons.Outlined.Check) { open = false; action() }
            }
        }
    }
}

/** 「开 / 关」小胶囊（动态取色等开关） */
@Composable
private fun TogglePill(on: Boolean, enabled: Boolean = true, onToggle: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) AccentMist else PaperWarm)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) { onToggle() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (on) "开" else "关",
            fontSize = 13.sp,
            color = if (on) Accent else Muted,
            style = TextStyle(lineHeightStyle = CenteredLineHeight),
        )
    }
}
