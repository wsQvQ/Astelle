package com.astelle.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.astelle.app.data.settings.GlassModeHolder
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.LocalAstelleColors
import com.astelle.app.ui.theme.SurfaceFloat

private val MenuShape = RoundedCornerShape(14.dp)

/**
 * 玻璃菜单（10-11 自研轻玻璃，用户："可以试试"）——所有浮层菜单的玻璃替身。
 *
 * 配方（零新依赖，离线安全）：
 *  1. 开启瞬间拍一张窗口快照（RGB_565，纸感平涂无压力）
 *  2. 快照**按菜单位置对位**平移进来 + RenderEffect 模糊 22dp
 *     （API<31 无 RenderEffect → 不糊，只留磨砂 tint，自动降级）
 *  3. 纸色半透明 tint（可读性）+ 顶部内高光（specular，玻璃的灵魂）+ 发丝描边
 *  4. [GlassModeHolder] 开关：关 = 原纸感浮层，零额外开销
 *
 * ⚠️ 菜单是独立窗口，拿不到主窗口的实时图层——所以走**快照**路线
 * （菜单是瞬态浮层，打开几百毫秒内背景静止，快照与真背景无感）；
 * 同窗口的悬浮工具栏才用实时图层（见 FormatToolbar 玻璃版）。
 */
@Composable
fun GlassMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val glassOn = GlassModeHolder.enabled.value

    if (!glassOn) {
        // 原纸感浮层（MenuChrome 一贯口径）
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            shape = MenuShape,
            containerColor = SurfaceFloat,
            border = BorderStroke(1.dp, Divider),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            properties = properties,
            content = content,
        )
        return
    }

    val colors = LocalAstelleColors.current
    val view = LocalView.current
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var panelPos by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(expanded) {
        if (expanded) {
            snapshot = runCatching {
                view.drawToBitmap(Bitmap.Config.RGB_565).asImageBitmap()
            }.getOrNull()
        } else {
            snapshot = null
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MenuShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        properties = properties,
    ) {
        Box(
            Modifier
                .clip(MenuShape)
                .onGloballyPositioned { panelPos = it.positionInWindow() }
        ) {
            // ① 背景玻璃：快照对位 + 模糊（边缘 Unbounded = 不糊出透明边）
            Box(
                Modifier
                    .matchParentSize()
                    .blur(22.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .drawBehind {
                        val snap = snapshot ?: return@drawBehind
                        drawImage(snap, topLeft = Offset(-panelPos.x, -panelPos.y))
                    }
            )
            // ② 纸色 tint（可读性）+ ③ 顶部内高光（specular）+ ④ 发丝描边
            Box(
                Modifier
                    .matchParentSize()
                    .background(colors.paper.copy(alpha = 0.66f))
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.12f),
                                0.5f to Color.White.copy(alpha = 0f),
                            ),
                        )
                    }
                    .border(1.dp, Divider.copy(alpha = 0.8f), MenuShape)
            )
            Column { content() }
        }
    }
}
