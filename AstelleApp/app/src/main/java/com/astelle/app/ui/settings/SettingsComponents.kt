package com.astelle.app.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astelle.app.ui.theme.Accent
import com.astelle.app.ui.theme.LocalAstelleColors

/**
 * 设置页的两块公共零件（「共享地基」，用户 10-10）——
 * 分组标题 + 卡片行。照 DSH 设置页的气质：暖底、独立圆角卡片、
 * 图标 + 标题 + 副标 + 尾部控件。三栏的「预览」小标等后续行目也吃它们。
 *
 * 10 号计划 1b：小标行距 24→20dp；可点行整行按压淡入 PaperWarm。
 */
@Composable
internal fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Accent,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp, start = 6.dp),
        )
        content()
    }
}

/**
 * 一行设置 = 一张独立圆角卡（照参考图：卡与卡之间是留白不是分隔线）。
 * [trailing] 放尾部控件（下拉胶囊 / 开关 / 什么都不放）。
 * [onClick] 给了整行就可点：按压整行淡入 PaperWarm（10 号 1b），无涟漪（全 app 手感口径）。
 */
@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = LocalAstelleColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // 按压淡入 120ms（透明黑陷阱：wash 走同色透明，别用 Color.Transparent 作插值端点）
    val wash by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "rowPress")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            // 深底浅卡（用户 10-10：照参考图）—— 页面用 paperWarm，卡用 paper
            .background(colors.paper)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .background(colors.paperWarm.copy(alpha = wash))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.inkSoft, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.ink)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = colors.muted, modifier = Modifier.padding(top = 3.dp))
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            Box(contentAlignment = Alignment.Center) { trailing() }
        }
    }
}
