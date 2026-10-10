package com.astelle.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.astelle.app.data.settings.GlassModeHolder
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.LocalAstelleColors
import com.astelle.app.ui.theme.SurfaceFloat

private val MenuShape = RoundedCornerShape(14.dp)

// ── 玻璃配方（10-11 二调：用户反馈「按钮看不清」——模糊翻倍让背景糊成抽象色块、
//    不跟前景抢戏，tint 加厚托住可读性。这两个旋钮就是"全透玻璃不适合本 app"的解法）──
private val GlassBlur = 42.dp
private const val GlassTintAlpha = 0.78f

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
            // ① 背景玻璃：快照对位 + **重**模糊（糊成色块，按钮文字立起来）
            Box(
                Modifier
                    .matchParentSize()
                    .blur(GlassBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .drawBehind {
                        val snap = snapshot ?: return@drawBehind
                        drawImage(snap, topLeft = Offset(-panelPos.x, -panelPos.y))
                    }
            )
            // ② 纸色 tint（可读性）+ ③ 顶部内高光（specular）+ ④ 发丝描边
            Box(
                Modifier
                    .matchParentSize()
                    .background(colors.paper.copy(alpha = GlassTintAlpha))
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

/**
 * 玻璃对话框（10-11 用户加 scope）——AlertDialog 的玻璃替身，布局沿用 M3 口径
 * （标题 / 正文 / 按钮组右对齐）。
 * ⚠️ 快照必须拍**主窗口**：LocalView 要在 Dialog 节点**外面**取——
 * 进了 Dialog 内部拿到的是弹层自己那扇窗，拍出来是空的（坑）。
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    val glassOn = GlassModeHolder.enabled.value
    val colors = LocalAstelleColors.current
    val hostView = LocalView.current
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var panelPos by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(Unit) {
        snapshot = runCatching {
            hostView.drawToBitmap(Bitmap.Config.RGB_565).asImageBitmap()
        }.getOrNull()
    }

    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties()) {
        val shape = RoundedCornerShape(20.dp)
        Box(
            Modifier
                .clip(shape)
                .onGloballyPositioned { panelPos = it.positionInWindow() }
        ) {
            val snap = snapshot
            if (glassOn && snap != null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .blur(GlassBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .drawBehind {
                            drawImage(snap, topLeft = Offset(-panelPos.x, -panelPos.y))
                        }
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(colors.paper.copy(alpha = GlassTintAlpha))
                        .drawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = 0.10f),
                                    0.5f to Color.White.copy(alpha = 0f),
                                ),
                            )
                        }
                        .border(1.dp, Divider.copy(alpha = 0.8f), shape)
                )
            } else {
                Box(Modifier.matchParentSize().background(SurfaceFloat))
            }
            Column(Modifier.padding(24.dp)) {
                CompositionLocalProvider(LocalContentColor provides colors.ink) { title() }
                Spacer(Modifier.height(12.dp))
                CompositionLocalProvider(LocalContentColor provides colors.muted) { text() }
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(Modifier.width(4.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}
