package com.astelle.app.data.exporter

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通过 SAF 把导出结果写进用户选中的位置。
 *
 * 与 [com.astelle.app.data.importer.MarkdownFileReader] 对称：
 * 这里只碰 ContentResolver，任何失败都返回 false，由调用方给提示，
 * 不把异常抛回 UI 层。**全程只写用户指定的那个文件，绝不碰数据库** ——
 * 导出是读操作，不能连带把笔记的 updatedAt 刷新掉。
 */
object ExportFileWriter {

    suspend fun writeText(context: Context, uri: Uri, text: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                open(context, uri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                    out.flush()
                } != null
            }.getOrDefault(false)
        }

    suspend fun writePng(context: Context, uri: Uri, bitmap: Bitmap): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                open(context, uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } == true
            }.getOrDefault(false)
        }

    /**
     * mode 用 "wt" 而不是默认的 "w"：覆盖同名文件时，"w" 在部分文档提供方上
     * 并不清空原有内容，新写入的短文本后面会拖着上一版的尾巴。
     */
    private fun open(context: Context, uri: Uri) =
        context.contentResolver.openOutputStream(uri, "wt")
}
