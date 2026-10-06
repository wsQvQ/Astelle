package com.astelle.app.data.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 从系统文件选择器选中的文件 */
data class PickedFile(
    val displayName: String?,
    val text: String,
    /** 文件超过上限，只读了前面一段 */
    val truncated: Boolean,
)

/**
 * 通过 SAF 读取用户选中的文件。
 *
 * 与 [MarkdownImport] 分开是刻意的：解析是纯函数、可单测；
 * 这里只负责碰 Android 的 ContentResolver，任何失败都返回 null，
 * 由调用方给出提示，不把异常抛到 UI 层。
 */
object MarkdownFileReader {

    /**
     * 单次导入的字节上限。
     *
     * 这道闸不是随便设的：笔记列表用 `SELECT *` 把全文读进 CursorWindow，
     * 而 CursorWindow 上限约 2MB，**单行超过就会抛
     * SQLiteBlobTooBigException 直接崩溃**。文件选择器的 MIME 过滤挡不住
     * 用户选中一个几十 MB 的二进制文件，所以必须在读的时候截断。
     * 512KB 对纯文本笔记已经非常宽裕。
     */
    const val MAX_BYTES = 512 * 1024

    suspend fun read(context: Context, uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
        runCatching {
            val name = queryDisplayName(context, uri)
            val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val (text, truncated) = stream.use { readAtMost(it) }
            PickedFile(displayName = name, text = text, truncated = truncated)
        }.getOrNull()
    }

    /** 最多读 [MAX_BYTES] 字节，返回文本与「是否被截断」 */
    private fun readAtMost(input: InputStream): Pair<String, Boolean> {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var truncated = false

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            val room = MAX_BYTES - out.size()
            if (read > room) {
                if (room > 0) out.write(buffer, 0, room)
                truncated = true
                break
            }
            out.write(buffer, 0, read)
        }

        return String(out.toByteArray(), Charsets.UTF_8) to truncated
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
}
