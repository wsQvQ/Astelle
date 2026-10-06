package com.astelle.app.data.importer

/** 解析一个 .md 文件后得到的笔记内容 */
data class ImportedNote(
    val title: String,
    val content: String,
)

/**
 * 把 .md 文件文本转成 Astelle 的「标题 + 正文」。
 *
 * 纯函数、零 Android 依赖，因此可以直接用 JVM 单元测试覆盖。
 *
 * 规则（有意保持简单，能少猜就少猜）：
 *  1. 首个非空行是 ATX 标题（`# 标题`）→ 用它当标题，并从正文里去掉这一行。
 *     笔记模型本身就有独立标题字段，H1 再留一份在正文里会重复显示。
 *  2. 否则用文件名（去掉扩展名）当标题，正文保持原样。
 *  3. 换行统一成 `\n`，首尾空白裁掉。
 */
object MarkdownImport {

    /** CommonMark 的 ATX 标题：# 后面必须跟空格，最多六级 */
    private val ATX_HEADING = Regex("""^#{1,6}\s+(.*\S)\s*$""")

    const val DEFAULT_TITLE = "导入的笔记"

    fun parse(fileName: String?, raw: String): ImportedNote {
        val text = raw.replace("\r\n", "\n").replace('\r', '\n').trim()
        val lines = text.lines()

        val firstIndex = lines.indexOfFirst { it.isNotBlank() }
        val heading = if (firstIndex >= 0) {
            ATX_HEADING.find(lines[firstIndex])?.groupValues?.get(1)
        } else {
            null
        }

        return if (heading != null) {
            val body = lines
                .filterIndexed { index, _ -> index != firstIndex }
                .joinToString("\n")
                .trim()
            ImportedNote(title = heading, content = body)
        } else {
            ImportedNote(title = titleFromFileName(fileName), content = text)
        }
    }

    private fun titleFromFileName(fileName: String?): String {
        val base = fileName?.substringBeforeLast('.')?.trim().orEmpty()
        return base.ifBlank { DEFAULT_TITLE }
    }
}
