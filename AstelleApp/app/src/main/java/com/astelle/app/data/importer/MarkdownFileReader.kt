package com.astelle.app.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 从系统文件选择器选中的文件：显示名 + 文本内容 */
data class PickedFile(
    val displayName: String?,
    val text: String?,
)

/**
 * 通过 SAF 读取用户选中的文件。
 *
 * 与 [MarkdownImport] 分开是刻意的：解析是纯函数、可单测；
 * 这里只负责碰 Android 的 ContentResolver，任何失败都返回 null，
 * 由调用方给出提示，不把异常抛到 UI 层。
 */
object MarkdownFileReader {

    suspend fun read(context: Context, uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
        runCatching {
            val name = queryDisplayName(context, uri)
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
            PickedFile(displayName = name, text = text)
        }.getOrNull()
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
}
