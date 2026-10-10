package com.astelle.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Muted

/**
 * 三处浮层菜单（`⋯` 菜单 / 导出二级 / 抽屉长按菜单）共用的一套零件。
 *
 * 收在这里的理由：菜单的「胖瘦」是全局手感，散在三个文件里就只能一处一处抠，
 * 抠着抠着三处就不一样了。
 */

/** 菜单横向留白。M3 默认 12dp 太抠，字几乎贴着描边 —— 用户：「左右两边太窄了」 */
private val MenuHorizontalPadding = 24.dp

/** 危险组上面那道发丝线，两端与菜单项的文字起点对齐 */
@Composable
fun MenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = MenuHorizontalPadding, vertical = 5.dp)
            .height(1.dp)
            .background(Divider),
    )
}

/** 菜单项的前导图标：18dp、默认静音色。图标只负责辨认，不跟字抢戏 */
@Composable
fun MenuIcon(
    icon: ImageVector,
    tint: Color = Muted,
    modifier: Modifier = Modifier,
) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(18.dp))
}

/**
 * 一行菜单项：前导图标 + 文字，行高沿用 M3 的 48dp（用户嫌 44dp 矮过，别再动它）。
 *
 * [onClick] 必须是**最后一个**参数，调用处才能写成 `MenuRow(...) { ... }` ——
 * 尾随 lambda 只绑最后一个参数，放中间会全部编译不过。
 * [textColor] 默认 `Color.Unspecified` = 交给 M3 自己的启用/禁用配色；
 * 危险项显式传 `Danger`。[iconTint] 同理，默认静音色。
 * [selected] = 当前选中项才画尾随对勾（10-11 用户：每项都挂勾会误导成"全都选了"）。
 * [icon] 可空：没有自然图标的选项（下拉二选一那种）干脆不放前导位，勾只走尾随位。
 */
@Composable
fun MenuRow(
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconTint: Color = Muted,
    textColor: Color = Color.Unspecified,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, color = textColor) },
        leadingIcon = if (icon != null) {
            { MenuIcon(icon, iconTint) }
        } else {
            null
        },
        trailingIcon = if (selected) {
            { MenuIcon(Icons.Outlined.Check, Accent) }
        } else {
            null
        },
        contentPadding = PaddingValues(horizontal = MenuHorizontalPadding, vertical = 0.dp),
        enabled = enabled,
        onClick = onClick,
    )
}
