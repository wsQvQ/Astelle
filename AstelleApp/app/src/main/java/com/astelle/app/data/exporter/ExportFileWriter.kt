package com.astelle.app.data.exporter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import java.io.File
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
            val tmp = File(context.cacheDir, "astelle-export.png")
            try {
                // ① 先编码进私有临时文件：把「编码器」和「文档提供方的流」两件事拆开，
                //    谁出错一眼看出来（@encode / @write）
                if (!encodePng(bitmap, tmp)) {
                    return@withContext ExportResult.Failed("图片编码失败@encode")
                }
                // ② 再分块拷进 SAF 流。分块比编码器直写温和，部分提供方的流
                //    扛不住大块写入（手机上抛过 NullPointerException）
                tmp.inputStream().use { input ->
                    stream.use { out -> input.copyTo(out, bufferSize = 8 * 1024) }
                }
                ExportResult.Ok
            } catch (e: Throwable) {
                Log.w(TAG, "写入失败", e)
                ExportResult.Failed("图片写入失败：${e.javaClass.simpleName}@write")
            } finally {
                tmp.delete()
            }
        }

    /**
     * 编码 PNG 到 [target]，失败自动换配置再试。
     *
     * 配置选择：短图用 **ARGB_8888**（无损、内嵌照片不掉色）；
     * 长图（>4096px）先试 **RGB_565** —— 1440×12000 的图 34MB vs 69MB，手机吃不消后者，
     * 纸色底 + 文字看不出差别。另一种配置作为兜底：RGB_565→PNG 在不同 Skia 版本上
     * 行为不一致，而 ARGB 占内存大，各有各的坑，互相补。
     * 两条路都不行才认输，报 @encode。
     */
    private fun encodePng(bitmap: Bitmap, target: File): Boolean {
        val preferred =
            if (bitmap.height > TALL_IMAGE_PX) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        val fallback =
            if (preferred == Bitmap.Config.RGB_565) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        for (config in listOf(preferred, fallback)) {
            try {
                val software = toSoftware(bitmap, config)
                val ok = target.outputStream().use { software.compress(Bitmap.CompressFormat.PNG, 100, it) }
                software.recycle()
                if (ok) return true
            } catch (e: Throwable) {
                Log.w(TAG, "encode($config) 失败", e)
            }
        }
        return false
    }

    /**
     * 硬件位图转成软件位图。
     *
     * `GraphicsLayer.toImageBitmap()` 产出的是 **HARDWARE 配置**位图（compose 内部
     * `Bitmap.createBitmap(Picture)` 造的），而 `Bitmap.compress()` 要锁像素，
     * 硬件位图锁不了、直接抛异常 —— 真机上「md 导得出、图导不出」就栽在这。
     *
     * 转换走 **`Canvas.drawBitmap`** 而不是 `Bitmap.copy`：copy 在部分机型上对
     * 硬件位图行为不一致，画一遍是更老更稳的路子（真机验过这步没问题）。
     * 源位图**不在这里 recycle** —— 编码可能要试两次，用完再放。
     */
    private fun toSoftware(bitmap: Bitmap, config: Bitmap.Config): Bitmap {
        val copy = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
        Canvas(copy).drawBitmap(bitmap, 0f, 0f, null)
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
