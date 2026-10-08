package com.astelle.app.data.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownEditingTest {

    /* ---------- 包住 ---------- */

    @Test
    fun `有选区就包住它`() {
        val r = MarkdownEditing.wrap("你好世界", 1, 3, "**", "**")
        assertEquals("你**好世**界", r.text)
        assertEquals(3, r.selectStart)
        assertEquals(5, r.selectEnd)
    }

    @Test
    fun `没选区就插一对符号，光标落中间`() {
        val r = MarkdownEditing.wrap("你好", 1, 1, "**", "**")
        assertEquals("你****好", r.text)
        assertEquals(3, r.selectStart)
        assertEquals(3, r.selectEnd)
    }

    @Test
    fun `开闭符号可以不一样`() {
        val r = MarkdownEditing.wrap("", 0, 0, "`", "`")
        assertEquals("``", r.text)
    }

    /* ---------- 行级前缀三态 ---------- */

    @Test
    fun `加上前缀`() {
        val r = MarkdownEditing.toggleLinePrefix("标题", 1, "# ")
        assertEquals("# 标题", r.text)
    }

    @Test
    fun `再点一次取消`() {
        val r = MarkdownEditing.toggleLinePrefix("# 标题", 3, "# ")
        assertEquals("标题", r.text)
    }

    @Test
    fun `换一种前缀`() {
        val r = MarkdownEditing.toggleLinePrefix("# 标题", 3, "## ")
        assertEquals("## 标题", r.text)
    }

    @Test
    fun `连点三次不会堆叠`() {
        var text = "标题"
        repeat(3) { text = MarkdownEditing.toggleLinePrefix(text, 1, "# ").text }
        assertEquals("# 标题", text)
    }

    @Test
    fun `列表前缀也一样`() {
        assertEquals("- 牛奶", MarkdownEditing.toggleLinePrefix("牛奶", 0, "- ").text)
        assertEquals("1. 牛奶", MarkdownEditing.toggleLinePrefix("- 牛奶", 3, "1. ").text)
    }

    @Test
    fun `只改光标所在行`() {
        val r = MarkdownEditing.toggleLinePrefix("一行\n二行", 4, "- ")
        assertEquals("一行\n- 二行", r.text)
    }

    /* ---------- 待办打勾 ---------- */

    @Test
    fun `未勾选变成勾选`() {
        val r = MarkdownEditing.toggleTask("- [ ] 买菜", 6)
        assertEquals("- [x] 买菜", r.text)
    }

    @Test
    fun `勾选变回未勾选`() {
        val r = MarkdownEditing.toggleTask("- [x] 买菜", 6)
        assertEquals("- [ ] 买菜", r.text)
    }

    @Test
    fun `普通行变成待办`() {
        val r = MarkdownEditing.toggleTask("买菜", 2)
        assertEquals("- [ ] 买菜", r.text)
    }

    /* ---------- 回车续号 ---------- */

    private fun enter(old: String, caret: Int): EditResult? {
        // 模拟：在 caret 处插入一个换行
        val new = old.substring(0, caret) + "\n" + old.substring(caret)
        return MarkdownEditing.autoContinue(old, new, caret + 1)
    }

    @Test
    fun `无序列表续无序`() {
        val r = enter("- 买菜", 4)
        assertEquals("- 买菜\n- ", r!!.text)
    }

    @Test
    fun `有序列表递增`() {
        val r = enter("1. 第一", 5)
        assertEquals("1. 第一\n2. ", r!!.text)
        val r2 = enter("1. 第一\n2. 第二", 11)
        assertEquals("1. 第一\n2. 第二\n3. ", r2!!.text)
    }

    @Test
    fun `待办续出未勾选`() {
        val line = "- [x] 已完成"
        val r = enter(line, line.length)
        assertEquals("- [x] 已完成\n- [ ] ", r!!.text)
    }

    @Test
    fun `引用续引用`() {
        val line = "> 引用"
        val r = enter(line, line.length)
        assertEquals("> 引用\n> ", r!!.text)
    }

    @Test
    fun `空项回车结束列表`() {
        val text = "- 买菜\n- "
        val r = enter(text, text.length)
        assertEquals("- 买菜\n", r!!.text)
    }

    @Test
    fun `空待办回车也结束`() {
        val text = "- [ ] 买菜\n- [ ] "
        val r = enter(text, text.length)
        assertEquals("- [ ] 买菜\n", r!!.text)
    }

    @Test
    fun `普通段落不接管`() {
        val text = "普通一段文字"
        assertNull(enter(text, text.length))
    }

    @Test
    fun `粘贴一大段不接管`() {
        val old = "- 一"
        val new = old + "\n\n好多\n好多"
        assertNull(MarkdownEditing.autoContinue(old, new, new.length))
    }

    /* ---------- 插入模板 ---------- */

    @Test
    fun `插模板后光标落在指定位置`() {
        val r = MarkdownEditing.insert("", 0, "[]()", 1)
        assertEquals("[]()", r.text)
        assertEquals(1, r.selectStart)
    }

    @Test
    fun `在光标处插入`() {
        val r = MarkdownEditing.insert("你好", 1, "，世界", 3)
        assertEquals("你，世界好", r.text)
    }
}
