package com.astelle.app.data.exporter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 写入结果：成功，或一句能直接给用户看的失败原因 */
sealed interface ExportResult {
    data object Ok : ExportResult
    data class Failed(val reason: String) : ExportResult
}

/**
 * 通过 SAF 把导出结果写进用户选中的位置。
 *
 * 与 [com.astelle.app.data.importer.MarkdownFileReader] 对称：
 * 这里只碰 ContentResolver。**全程只写用户指定的那个文件，绝不碰数据库** ——
 * 导出是读操作，不能连带把笔记的 updatedAt 刷新掉。
 */
object ExportFileWriter {

    private const val TAG = "ExportFileWriter"

    /** 超过这个高度算「长图」，拷贝改用 RGB_565 省内存 */
    private const val TALL_IMAGE_PX = 4096

    suspend fun writeText(context: Context, uri: Uri, text: String): ExportResult =
        withContext(Dispatchers.IO) {
            val stream = open(context, uri)
                ?: return@withContext ExportResult.Failed("打不开目标文件")
            try {
                stream.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                ExportResult.Ok
            } catch (e: Exception) {
                Log.w(TAG, "writeText 失败", e)
                ExportResult.Failed(describe(e))
            }
        }

    suspend fun writePng(context: Context, uri: Uri, bitmap: Bitmap): ExportResult =
        withContext(Dispatchers.IO) {
            val stream = open(context, uri)
                ?: return@withContext ExportResult.Failed("打不开目标文件")
            val software = try {
                softwareCopy(bitmap)
            } catch (e: Throwable) {
                Log.w(TAG, "位图转换失败", e)
                return@withContext ExportResult.Failed("图片转换失败：${e.javaClass.simpleName}@copy")
            }
            try {
                val ok = stream.use { software.compress(Bitmap.CompressFormat.PNG, 100, it) }
                if (ok) ExportResult.Ok else ExportResult.Failed("图片压缩失败@compress")
            } catch (e: Throwable) {
                // ⚠️ 出错环节写进文案（@copy / @compress）：手机上抛过 NullPointerException，
                // 光看异常名定位不到，必须知道卡在哪一步
                Log.w(TAG, "压缩写入失败", e)
                ExportResult.Failed("图片压缩失败：${e.javaClass.simpleName}@compress")
            }
        }

    /**
     * 硬件位图先转成软件位图。
     *
     * `GraphicsLayer.toImageBitmap()` 产出的是 **HARDWARE 配置**位图（compose 内部
     * `Bitmap.createBitmap(Picture)` 造的），而 `Bitmap.compress()` 要锁像素，
     * 硬件位图锁不了、直接抛异常 —— 真机上「md 导得出、图导不出」就栽在这。
     *
     * 转换走 **`Canvas.drawBitmap`** 而不是 `Bitmap.copy`：copy 在部分机型上对
     * 硬件位图行为不一致（手机上抛过 NullPointerException），画一遍是更老更稳的路子。
     *
     * **长图用 RGB_565**：1440×12000 的图 ARGB 要 69MB，转换期间新旧两张并存
     * 接近 140MB，手机上吃不消（实测 800 字能导、4450 字不能）。RGB_565 只要一半，
     * 纸色底 + 文字看不出差别，只有内嵌照片会有轻微色带。
     * 转完立刻 recycle 源位图，后面压缩阶段只留一张。
     */
    private fun softwareCopy(bitmap: Bitmap): Bitmap {
        val config =
            if (bitmap.height > TALL_IMAGE_PX) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        val copy = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
        Canvas(copy).drawBitmap(bitmap, 0f, 0f, null)
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            runCatching { bitmap.recycle() }
        }
        return copy
    }

    private fun describe(e: Exception): String = when (e) {
        is IOException -> "写入出错（可能是存储空间不够）"
        else -> "写入出错：${e.javaClass.simpleName}"
    }

    /**
     * mode 用 "wt" 而不是默认的 "w"：覆盖同名文件时，"w" 在部分文档提供方上
     * 并不清空原有内容，新写入的短文本后面会拖着上一版的尾巴。
     */
    private fun open(context: Context, uri: Uri) = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")
    }.getOrNull()
}
