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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BrightnessAuto
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.BuildConfig
import com.astelle.app.data.settings.ColorMode
import com.astelle.app.data.settings.SettingsStore
import com.astelle.app.ui.components.MenuRow
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

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶栏：返回（退出设置）+ 侧栏
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietIconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "返回", onBack)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
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
