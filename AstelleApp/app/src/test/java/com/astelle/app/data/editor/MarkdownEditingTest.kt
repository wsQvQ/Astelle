package com.astelle.app.data.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    /* ---------- 包住 / 摘掉（状态点亮时再按 = 取消） ---------- */

    @Test
    fun `已经在粗体里再按就是取消`() {
        val r = MarkdownEditing.toggleWrap("**粗体**", 3, 3, "**")
        assertEquals("粗体", r.text)
        assertEquals(1, r.selectStart)
        assertEquals(1, r.selectEnd)
    }

    @Test
    fun `取消时保住选区`() {
        val r = MarkdownEditing.toggleWrap("**粗体**", 2, 4, "**")
        assertEquals("粗体", r.text)
        assertEquals(0, r.selectStart)
        assertEquals(2, r.selectEnd)
    }

    @Test
    fun `粗体里的字加斜体不会误判成取消`() {
        val r = MarkdownEditing.toggleWrap("**粗体**", 2, 4, "*")
        assertEquals("***粗体***", r.text)
    }

    @Test
    fun `下划线标签也能摘掉`() {
        val r = MarkdownEditing.toggleWrap("<u>字</u>", 4, 4, "<u>", "</u>")
        assertEquals("字", r.text)
        assertEquals(1, r.selectStart)
    }

    /* ---------- 状态检测（工具栏点亮） ---------- */

    @Test
    fun `光标在粗体里点亮加粗`() {
        val s = MarkdownEditing.detectFormatStates("**粗**", 3, 3)
        assertTrue(s.bold)
        assertFalse(s.italic)
    }

    @Test
    fun `斜体和删除线各自点亮`() {
        assertTrue(MarkdownEditing.detectFormatStates("*斜*", 2, 2).italic)
        assertTrue(MarkdownEditing.detectFormatStates("~~删~~", 3, 3).strike)
        assertTrue(MarkdownEditing.detectFormatStates("<u>下</u>", 4, 4).underline)
    }

    @Test
    fun `标题级别点亮对应键`() {
        assertEquals(1, MarkdownEditing.detectFormatStates("# 大标题", 3, 3).heading)
        assertEquals(2, MarkdownEditing.detectFormatStates("  ## 二", 5, 5).heading)
        assertEquals(0, MarkdownEditing.detectFormatStates("正文", 1, 1).heading)
        assertEquals(0, MarkdownEditing.detectFormatStates("#### 四", 3, 3).heading)
    }

    @Test
    fun `列表状态分开点亮`() {
        assertTrue(MarkdownEditing.detectFormatStates("- 一", 0, 0).bullet)
        assertFalse(MarkdownEditing.detectFormatStates("- [ ] 待办", 3, 3).bullet)
        assertTrue(MarkdownEditing.detectFormatStates("- [x] 待办", 3, 3).task)
        assertTrue(MarkdownEditing.detectFormatStates("1. 一", 2, 2).ordered)
        assertTrue(MarkdownEditing.detectFormatStates("> 引", 2, 2).quote)
    }

    @Test
    fun `混合选区不算亮`() {
        val s = MarkdownEditing.detectFormatStates("- 一\n正文", 0, 5)
        assertFalse(s.bullet)
    }

    /* ---------- 缩进 / 反缩进 ---------- */

    @Test
    fun `列表缩进两空格并推走选区`() {
        val r = MarkdownEditing.indent("- 一", 0, 3)
        assertEquals("  - 一", r.text)
        assertEquals(2, r.selectStart)
        assertEquals(5, r.selectEnd)
    }

    @Test
    fun `有序列表按内容列缩进`() {
        val r = MarkdownEditing.indent("1. 一", 2, 2)
        assertEquals("   1. 一", r.text)
        assertEquals(5, r.selectStart)
    }

    @Test
    fun `多行一起缩进`() {
        val r = MarkdownEditing.indent("- 一\n- 二", 0, 7)
        assertEquals("  - 一\n  - 二", r.text)
    }

    @Test
    fun `反缩进去掉一级`() {
        val r = MarkdownEditing.outdent("    - 一", 4, 4)
        assertEquals("  - 一", r.text)
        assertEquals(2, r.selectStart)
    }

    @Test
    fun `反缩进到底就停`() {
        val r = MarkdownEditing.outdent("- 一", 0, 0)
        assertEquals("- 一", r.text)
    }

    @Test
    fun `普通行缩进也是两空格`() {
        val r = MarkdownEditing.indent("你好", 2, 2)
        assertEquals("  你好", r.text)
        assertEquals(4, r.selectStart)
    }
}
