package com.astelle.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.astelle.app.ui.theme.Divider
import com.astelle.app.ui.theme.Ink
import com.astelle.app.ui.theme.Muted
import com.astelle.app.ui.theme.SurfaceFloat

private val MenuShape = RoundedCornerShape(14.dp)
private val DialogShape = RoundedCornerShape(20.dp)

/**
 * 浮层菜单（MenuChrome 一贯口径：纸白 + 一圈描边 + 圆角 14dp）。
 *
 * ⚠️ 10-11：玻璃材质整条链**归档退役**（实验代码见分支
 * `archive/glass-material-20261011`，还有 bug，以后有空慢慢修）。
 * 这里只保留同名壳子——调用点零改动，产品里只剩原有材质。
 */
@Composable
fun GlassMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
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
}

/**
 * 确认对话框（M3 AlertDialog 原味）。玻璃版同在
 * `archive/glass-material-20261011`。
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton ?: {},
        containerColor = SurfaceFloat,
        titleContentColor = Ink,
        textContentColor = Muted,
        shape = DialogShape,
        tonalElevation = 0.dp,
    )
}
