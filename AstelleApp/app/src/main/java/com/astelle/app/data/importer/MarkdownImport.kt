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
 * 规则（有意保持简单：能少猜就少猜，结果可预测）：
 *  1. **文件名优先**。用户给文件起的名字就是他想要的笔记名，
 *     所以 `测试.md` 导入后标题就是「测试」，正文原样保留。
 *  2. 文件名不可用（没有名字、或只有 `.md` 这种纯扩展名）时，
 *     退而用正文里的首个 ATX 标题，并把那一行从正文中去掉 —— 因为
 *     此时标题是从正文里抽出来的，留着会重复显示。
 *  3. 两者都没有，用 [DEFAULT_TITLE]。
 *  4. 换行统一成 `\n`，首尾空白裁掉。
 */
object MarkdownImport {

    /** CommonMark 的 ATX 标题：# 后面必须跟空格，最多六级 */
    private val ATX_HEADING = Regex("""^#{1,6}\s+(.*\S)\s*$""")

    const val DEFAULT_TITLE = "导入的笔记"

    fun parse(fileName: String?, raw: String): ImportedNote {
        val text = raw.replace("\r\n", "\n").replace('\r', '\n').trim()

        val fromFileName = fileName?.substringBeforeLast('.')?.trim().orEmpty()
        if (fromFileName.isNotEmpty()) {
            return ImportedNote(title = fromFileName, content = text)
        }

        // 文件名靠不住，才去正文里找标题
        val lines = text.lines()
        val firstIndex = lines.indexOfFirst { it.isNotBlank() }
        val heading = if (firstIndex >= 0) {
            ATX_HEADING.find(lines[firstIndex])?.groupValues?.get(1)
        } else {
            null
        }

        if (heading != null) {
            val body = lines
                .filterIndexed { index, _ -> index != firstIndex }
                .joinToString("\n")
                .trim()
            return ImportedNote(title = heading, content = body)
        }

        return ImportedNote(title = DEFAULT_TITLE, content = text)
    }
}
