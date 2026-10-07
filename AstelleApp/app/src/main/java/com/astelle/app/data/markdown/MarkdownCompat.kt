package com.astelle.app.data.markdown

/**
 * 渲染前的兼容性修补。**只影响显示，绝不改动存储的原文** ——
 * 用户怎么写就怎么存，导出 .md 也是原样。
 *
 * 目前只有一条规则，加规则前先想清楚「会不会误伤正常文本」：
 *
 * **表格前补空行。** GFM 规定表格不能打断段落：表格紧跟上一行文字时
 * （中间没空行），整块会被当成那一段的续行，按纯文本显示 ——
 * 用户从别处粘贴的行程笔记就踩了这个，天气那张表整个没渲染。
 * 别的表前面恰好有空行，所以只有它露馅。
 *
 * 判据收紧到「像表头 + 下一行是分隔行」才补：普通的 `| a | b |` 文本
 * 后面没有 `|---|` 分隔行，不会被误判。
 */
object MarkdownCompat {

    fun render(text: String): String {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n')
        if (lines.size < 2) return normalized

        val out = StringBuilder(normalized.length + 16)
        for (i in lines.indices) {
            val line = lines[i]
            val prev = lines.getOrNull(i - 1).orEmpty()
            val next = lines.getOrNull(i + 1).orEmpty()
            if (prev.isNotBlank() && isTableRow(line) && isDelimiter(next)) {
                out.append('\n')
            }
            out.append(line)
            if (i != lines.lastIndex) out.append('\n')
        }
        return out.toString()
    }

    /** `| a | b |` 这种行：首尾都是竖线（表格的表头/数据行） */
    private fun isTableRow(line: String): Boolean {
        val t = line.trim()
        return t.length > 1 && t.startsWith("|") && t.endsWith("|")
    }

    /** `|---|:--:|` 这种分隔行：只由竖线、横线、冒号、空格组成 */
    private fun isDelimiter(line: String): Boolean {
        val t = line.trim()
        return t.length > 1 && t.startsWith("|") && t.endsWith("|") &&
            t.all { it == '|' || it == '-' || it == ':' || it == ' ' }
    }
}
