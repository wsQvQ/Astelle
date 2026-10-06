package com.astelle.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自绘图标库：对齐 index.html 原型 SVG。
 */
object AstelleIcons {

    private val MutedColor = Color(0xFF9C8B74)
    private val GhostColor = Color(0xFFB8A992)

    val Sidebar: ImageVector by lazy {
        ImageVector.Builder("Sidebar", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8.5f, 3.2f)
                lineTo(18.5f, 3.2f)
                arcTo(3.2f, 3.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 21.7f, y1 = 6.4f)
                lineTo(21.7f, 17.6f)
                arcTo(3.2f, 3.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18.5f, y1 = 20.8f)
                lineTo(8.5f, 20.8f)
                arcTo(3.2f, 3.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5.3f, y1 = 17.6f)
                lineTo(5.3f, 6.4f)
                arcTo(3.2f, 3.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 8.5f, y1 = 3.2f)
                moveTo(10.2f, 3.2f)
                lineTo(10.2f, 20.8f)
            }
        }.build()
    }

    val NewNote: ImageVector by lazy {
        // Lucide FilePlus 官方 path，原样转换
        ImageVector.Builder("NewNote", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                // 文件 + 右上折角
                moveTo(15f, 2f)
                lineTo(6f, 2f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 4f, y1 = 4f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6f, y1 = 22f)
                lineTo(18f, 22f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 20f, y1 = 20f)
                lineTo(20f, 7f)
                close()
                moveTo(14f, 2f)
                lineTo(14f, 6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 16f, y1 = 8f)
                lineTo(20f, 8f)
                // 加号
                moveTo(12f, 18f)
                lineTo(12f, 12f)
                moveTo(9f, 15f)
                lineTo(15f, 15f)
            }
        }.build()
    }

    val Undo: ImageVector by lazy {
        ImageVector.Builder("Undo", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9f, 14f); lineTo(4f, 9f); lineTo(9f, 4f)
                moveTo(4f, 9f); lineTo(14f, 9f)
                arcTo(6f, 6f, 0f, false, true, 14f, 21f)
                lineTo(11f, 21f)
            }
        }.build()
    }

    val Redo: ImageVector by lazy {
        ImageVector.Builder("Redo", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(15f, 14f); lineTo(20f, 9f); lineTo(15f, 4f)
                moveTo(20f, 9f); lineTo(10f, 9f)
                arcTo(6f, 6f, 0f, false, false, 10f, 21f)
                lineTo(13f, 21f)
            }
        }.build()
    }

    val More: ImageVector by lazy {
        ImageVector.Builder("More", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(MutedColor)) {
                addOval(3.2f, 10.2f, 6.8f, 13.8f)
                addOval(10.2f, 10.2f, 13.8f, 13.8f)
                addOval(17.2f, 10.2f, 20.8f, 13.8f)
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(GhostColor), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(18f, 11f)
                arcTo(7f, 7f, 0f, true, true, 4f, 11f)
                arcTo(7f, 7f, 0f, true, true, 18f, 11f)
                moveTo(20f, 20f); lineTo(16.5f, 16.5f)
            }
        }.build()
    }

    val Import: ImageVector by lazy {
        ImageVector.Builder("Import", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3f); lineTo(12f, 14f)
                moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f)
                moveTo(5f, 20f); lineTo(19f, 20f)
            }
        }.build()
    }

    val Plan: ImageVector by lazy {
        ImageVector.Builder("Plan", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.65f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6.4f, 5.2f); lineTo(17.6f, 5.2f)
                arcTo(2.4f, 2.4f, 0f, false, true, 20f, 7.6f)
                lineTo(20f, 17.4f)
                arcTo(2.4f, 2.4f, 0f, false, true, 17.6f, 19.8f)
                lineTo(6.4f, 19.8f)
                arcTo(2.4f, 2.4f, 0f, false, true, 4f, 17.4f)
                lineTo(4f, 7.6f)
                arcTo(2.4f, 2.4f, 0f, false, true, 6.4f, 5.2f)
                moveTo(8f, 3.2f); lineTo(8f, 6.8f)
                moveTo(16f, 3.2f); lineTo(16f, 6.8f)
                moveTo(4f, 9.8f); lineTo(20f, 9.8f)
                moveTo(9f, 14.2f); lineTo(15f, 14.2f)
            }
        }.build()
    }

    val Diary: ImageVector by lazy {
        ImageVector.Builder("Diary", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.55f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(7.4f, 3.6f); lineTo(16.6f, 3.6f)
                arcTo(2.2f, 2.2f, 0f, false, true, 18.8f, 5.8f)
                lineTo(18.8f, 18.2f)
                arcTo(2.2f, 2.2f, 0f, false, true, 16.6f, 20.4f)
                lineTo(7.4f, 20.4f)
                arcTo(2.2f, 2.2f, 0f, false, true, 5.2f, 18.2f)
                lineTo(5.2f, 5.8f)
                arcTo(2.2f, 2.2f, 0f, false, true, 7.4f, 3.6f)
                moveTo(8.6f, 3.6f); lineTo(8.6f, 20.4f)
                moveTo(14.8f, 3.6f); lineTo(14.8f, 8.4f)
                lineTo(16.15f, 7.2f); lineTo(17.5f, 8.4f); lineTo(17.5f, 3.6f)
                moveTo(11.2f, 11f); lineTo(15.8f, 11f)
                moveTo(11.2f, 14.2f); lineTo(15.8f, 14.2f)
            }
        }.build()
    }

    /** AI：星群（一大一小，干净线稿） */
    val Sparkle: ImageVector by lazy {
        ImageVector.Builder("Sparkle", 24.dp, 24.dp, 24f, 24f).apply {
            // 主星
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.55f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(11f, 3.2f)
                lineTo(12.35f, 9.1f)
                lineTo(18.2f, 10.5f)
                lineTo(12.35f, 11.9f)
                lineTo(11f, 17.8f)
                lineTo(9.65f, 11.9f)
                lineTo(3.8f, 10.5f)
                lineTo(9.65f, 9.1f)
                close()
            }
            // 副星
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(18.2f, 14.2f)
                lineTo(18.85f, 16.55f)
                lineTo(21.2f, 17.2f)
                lineTo(18.85f, 17.85f)
                lineTo(18.2f, 20.2f)
                lineTo(17.55f, 17.85f)
                lineTo(15.2f, 17.2f)
                lineTo(17.55f, 16.55f)
                close()
            }
        }.build()
    }

    /** 设置：双滑杆（Lucide settings-2，干净不像太阳） */
    val Settings: ImageVector by lazy {
        ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(MutedColor), strokeLineWidth = 1.65f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                // 上滑杆
                moveTo(3.5f, 7.5f)
                lineTo(14.5f, 7.5f)
                moveTo(18.5f, 7.5f)
                lineTo(20.5f, 7.5f)
                // 下滑杆
                moveTo(3.5f, 16.5f)
                lineTo(8.5f, 16.5f)
                moveTo(12.5f, 16.5f)
                lineTo(20.5f, 16.5f)
                // 两个滑钮
                moveTo(16.5f, 7.5f)
                arcTo(1.7f, 1.7f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 13.1f, y1 = 7.5f)
                arcTo(1.7f, 1.7f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 16.5f, y1 = 7.5f)
                moveTo(10.5f, 16.5f)
                arcTo(1.7f, 1.7f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 7.1f, y1 = 16.5f)
                arcTo(1.7f, 1.7f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 10.5f, y1 = 16.5f)
            }
        }.build()
    }
}

private fun PathBuilder.addOval(l: Float, t: Float, r: Float, b: Float) {
    val cx = (l + r) / 2f
    val cy = (t + b) / 2f
    val rx = (r - l) / 2f
    val ry = (b - t) / 2f
    moveTo(cx + rx, cy)
    arcTo(rx, ry, 0f, true, false, cx - rx, cy)
    arcTo(rx, ry, 0f, true, false, cx + rx, cy)
    close()
}
