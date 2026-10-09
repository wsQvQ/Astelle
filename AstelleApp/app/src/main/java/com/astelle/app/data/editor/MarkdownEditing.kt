package com.astelle.app.data.editor

/**
 * Markdown 编辑的纯函数：加粗、标题、列表、回车续号、待办打勾。
 *
 * 零 Android 依赖，全部 JVM 可单测 —— 手感逻辑出 bug 时，
 * 先在这里补用例，比在真机上戳来戳去快十倍。
 */

/** 一次编辑的结果：新文本 + 新的选区（[selectStart], [selectEnd] 相等即光标） */
data class EditResult(val text: String, val selectStart: Int, val selectEnd: Int)

object MarkdownEditing {

    /** 行首语法：标题 / 无序列表 / 任务 / 有序列表 / 引用 */
    private val LINE_PREFIX = Regex("""^(#{1,6}\s+|[-*+]\s+\[[ xX]\]\s+|[-*+]\s+|\d+\.\s+|>\s+)""")

    /** 空的列表项/引用行：`- `、`1. `、`- [ ] `、`> `（后面没内容） */
    private val EMPTY_ITEM = Regex("""^(#{1,6}\s+|[-*+]\s+\[[ xX]\]\s+|[-*+]\s+|\d+\.\s+|>\s+)$""")

    private val ORDERED = Regex("""^(\d+)\.\s+""")
    private val TASK = Regex("""^[-*+]\s+\[([ xX])]\s+""")
    private val BULLET = Regex("""^([-*+])\s+""")
    private val QUOTE = Regex("""^>\s+""")

    /** 包住选区；没选区就插一对符号，光标落在中间 */
    fun wrap(text: String, start: Int, end: Int, open: String, close: String = open): EditResult {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        return if (s < e) {
            val out = text.substring(0, s) + open + text.substring(s, e) + close + text.substring(e)
            EditResult(out, s + open.length, e + open.length)
        } else {
            val out = text.substring(0, s) + open + close + text.substring(s)
            EditResult(out, s + open.length, s + open.length)
        }
    }

    /**
     * 行级前缀（标题/列表/引用）：**加上 → 换掉 → 取消** 三态循环。
     *
     * 光标在哪一行就改哪一行；已是这个前缀就取消，是别的前缀就换掉，
     * 都没有就加上 —— 这样同一个按钮点三次不会堆出 `# # # `。
     */
    fun toggleLinePrefix(text: String, cursor: Int, prefix: String): EditResult {
        val (lineStart, lineEnd) = lineRange(text, cursor)
        val line = text.substring(lineStart, lineEnd)
        val existing = LINE_PREFIX.find(line)?.value.orEmpty()
        val newLine = when {
            existing == prefix -> line.substring(existing.length)
            existing.isNotEmpty() -> prefix + line.substring(existing.length)
            else -> prefix + line
        }
        val out = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val caret = lineStart + newLine.length
        return EditResult(out, caret, caret)
    }

    /** 待办打勾 / 取消：`- [ ] x` ⇄ `- [x] x`；普通行则变成待办 */
    fun toggleTask(text: String, cursor: Int): EditResult {
        val (lineStart, lineEnd) = lineRange(text, cursor)
        val line = text.substring(lineStart, lineEnd)
        val task = TASK.find(line)
        val newLine = when {
            task == null -> "- [ ] " + line.removePrefix(LINE_PREFIX.find(line)?.value.orEmpty())
            // 换掉方括号里的那个字符 —— 用捕获组自己的 range，别用 match.range 去凑下标
            task.groupValues[1] == " " -> line.replaceRange(task.groups[1]!!.range, "x")
            else -> line.replaceRange(task.groups[1]!!.range, " ")
        }
        val out = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val caret = lineStart + newLine.length
        return EditResult(out, caret, caret)
    }

    /** 在光标处插入一段模板，光标落在 [caret]（相对模板开头） */
    fun insert(text: String, cursor: Int, snippet: String, caret: Int = snippet.length): EditResult {
        val s = cursor.coerceIn(0, text.length)
        val out = text.substring(0, s) + snippet + text.substring(s)
        val at = s + caret.coerceIn(0, snippet.length)
        return EditResult(out, at, at)
    }

    /**
     * 插入**独立成块**的一段（图片用，用户 10-09 拍板）：光标在行内就前后断行，
     * 让内容独占一段 —— 不把句子劈开。
     */
    fun insertBlock(text: String, cursor: Int, snippet: String): EditResult {
        val at = cursor.coerceIn(0, text.length)
        val before = text.substring(0, at)
        val after = text.substring(at)
        val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
        val trail = if (after.isEmpty() || after.startsWith("\n")) "" else "\n"
        val out = before + lead + snippet + trail + after
        val caret = at + lead.length + snippet.length
        return EditResult(out, caret, caret)
    }

    /**
     * 回车时自动续列表/引用。返回 null = 不接管（普通换行）。
     *
     * 只在「正好插入一个换行」时接管，其余情况（粘贴、撤销、输入法整段替换）一律放行。
     */
    fun autoContinue(oldText: String, newText: String, caret: Int): EditResult? {
        if (newText.length != oldText.length + 1) return null
        val nl = caret - 1
        if (nl < 0 || nl >= newText.length || newText[nl] != '\n') return null

        val lineStart = oldText.lastIndexOf('\n', nl - 1) + 1
        val line = oldText.substring(lineStart, nl)

        // 空项回车 → 撤掉前缀**和刚打的换行**，结束列表（手感的关键）
        if (EMPTY_ITEM.matches(line)) {
            val out = oldText.substring(0, lineStart) + oldText.substring(nl)
            return EditResult(out, lineStart, lineStart)
        }

        val next = nextPrefix(line) ?: return null
        val insertAt = nl + 1
        val out = newText.substring(0, insertAt) + next + newText.substring(insertAt)
        return EditResult(out, insertAt + next.length, insertAt + next.length)
    }

    /** 下一行该续什么前缀；不是列表/引用行返回 null */
    private fun nextPrefix(line: String): String? {
        ORDERED.find(line)?.let { return "${it.groupValues[1].toInt() + 1}. " }
        // 待办续出来的永远是未勾选
        TASK.find(line)?.let { return "${BULLET.find(line)?.groupValues?.get(1) ?: "-"} [ ] " }
        BULLET.find(line)?.let { return "${it.groupValues[1]} " }
        QUOTE.find(line)?.let { return "> " }
        return null
    }

    /* ==================== 缩进 / 反缩进（列表层级） ==================== */

    /** 缩进一级：列表行按「内容列」缩进（`- `→2 空格、`1. `→3 空格），普通行 2 空格 */
    fun indent(text: String, start: Int, end: Int): EditResult = shiftIndent(text, start, end, indent = true)

    /** 反缩进一级：去掉行首至多一级空格，到底就停 */
    fun outdent(text: String, start: Int, end: Int): EditResult = shiftIndent(text, start, end, indent = false)

    /** 一级缩进的宽度：有序列表要对齐内容列（`1. ` 是 3、`10. ` 是 4），其余 2 */
    private fun indentUnit(line: String): Int {
        val body = line.trimStart()
        return ORDERED.find(body)?.value?.length ?: 2
    }

    private fun shiftIndent(text: String, start: Int, end: Int, indent: Boolean): EditResult {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        val effEnd = if (e > s && e >= 1 && text[e - 1] == '\n') e - 1 else e
        val firstStart = text.lastIndexOf('\n', s - 1) + 1
        val lastEnd = text.indexOf('\n', effEnd).let { if (it < 0) text.length else it }
        if (lastEnd < firstStart) return EditResult(text, s, e)

        val lines = text.substring(firstStart, lastEnd).split('\n')
        val sb = StringBuilder()
        var newS = s
        var newE = e
        var pos = firstStart
        lines.forEachIndexed { index, line ->
            // 空行不动：缩进空行只会留下一串看不见的尾随空格
            if (line.isNotEmpty()) {
                val unit = indentUnit(line)
                if (indent) {
                    sb.append(" ".repeat(unit)).append(line)
                    if (pos <= s) newS += unit
                    if (pos <= e) newE += unit
                } else {
                    val removed = minOf(unit, line.takeWhile { it == ' ' }.length)
                    if (removed > 0) {
                        if (s >= pos + removed) newS -= removed else if (s > pos) newS = pos
                        if (e >= pos + removed) newE -= removed else if (e > pos) newE = pos
                    }
                    sb.append(line, removed, line.length)
                }
            } else {
                sb.append(line)
            }
            if (index < lines.size - 1) sb.append('\n')
            pos += line.length + 1
        }
        val out = text.substring(0, firstStart) + sb + text.substring(lastEnd)
        return EditResult(out, newS, newE.coerceAtLeast(newS))
    }

    private fun lineRange(text: String, cursor: Int): Pair<Int, Int> {
        val c = cursor.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', c - 1) + 1
        val end = text.indexOf('\n', c).let { if (it < 0) text.length else it }
        return start to end
    }
}
