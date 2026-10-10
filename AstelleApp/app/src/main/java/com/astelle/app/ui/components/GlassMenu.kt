package com.astelle.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.astelle.app.data.settings.GlassModeHolder
import com.astelle.app.ui.theme.LocalAstelleColors
import com.astelle.app.ui.theme.SurfaceFloat

private val MenuShape = RoundedCornerShape(14.dp)
private val DialogShape = RoundedCornerShape(20.dp)

/** 开弹层瞬间拍一张主窗口快照（RGB_565：糊 30dp 后 565 色带不可见，省内存） */
private fun snapshotOf(view: android.view.View): ImageBitmap? =
    runCatching { view.drawToBitmap(Bitmap.Config.RGB_565).asImageBitmap() }.getOrNull()

/**
 * 玻璃菜单（10-11 自研轻玻璃）——所有浮层菜单的玻璃替身，配方见 [GlassPanel]。
 *
 * ⚠️ 菜单是独立窗口，拿不到主窗口的实时图层——走**开瞬快照**（瞬态浮层，
 * 打开几百毫秒内背景静止，快照与真背景无感）；同窗口的悬浮工具栏走实时图层。
 * [GlassModeHolder] 关 = 原纸感浮层，零额外开销。
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
            border = BorderStroke(1.dp, com.astelle.app.ui.theme.Divider),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            properties = properties,
            content = content,
        )
        return
    }

    val view = LocalView.current
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(expanded) {
        snapshot = if (expanded) snapshotOf(view) else null
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
        GlassPanel(
            shape = MenuShape,
            source = snapshot?.let { GlassSource.Snapshot(it) },
            // 铺满 Surface 的 8dp 内边距：不铺满就是"卡中卡"割裂（用户实拍）
            bleed = 8.dp,
        ) {
            Column { content() }
        }
    }
}

/**
 * 玻璃对话框（10-11）——AlertDialog 的玻璃替身，布局沿用 M3 口径
 * （标题 / 正文 / 按钮组右对齐），配方见 [GlassPanel]。
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
    // ⚠️ 主窗口的 view：必须在 Dialog 外取
    val hostView = LocalView.current
    var snapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        // 只在玻璃开着时拍（10-11 审计：以前连关着也拍，白烧 10-50ms）
        snapshot = if (glassOn) snapshotOf(hostView) else null
    }

    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties()) {
        if (glassOn) {
            GlassPanel(
                shape = DialogShape,
                source = snapshot?.let { GlassSource.Snapshot(it) },
            ) {
                DialogBody(title, text, confirmButton, dismissButton)
            }
        } else {
            Box(
                Modifier
                    .background(SurfaceFloat, DialogShape)
                    .padding(24.dp),
            ) {
                DialogBody(title, text, confirmButton, dismissButton)
            }
        }
    }
}

@Composable
private fun DialogBody(
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)?,
) {
    val colors = LocalAstelleColors.current
    Column(Modifier.padding(24.dp)) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) { title() }
        Spacer(Modifier.height(12.dp))
        CompositionLocalProvider(LocalContentColor provides colors.muted) { text() }
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            if (dismissButton != null) {
                dismissButton()
                Spacer(Modifier.width(4.dp))
            }
            confirmButton()
        }
    }
}
