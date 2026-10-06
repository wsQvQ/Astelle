package com.astelle.app.data.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownImportTest {

    @Test
    fun `首个 H1 变成标题，并从正文里去掉`() {
        val note = MarkdownImport.parse(
            "readme.md",
            """
            # Astelle

            这是正文第一段。
            这是第二段。
            """.trimIndent(),
        )
        assertEquals("Astelle", note.title)
        assertEquals("这是正文第一段。\n这是第二段。", note.content)
    }

    @Test
    fun `没有标题时用文件名`() {
        val note = MarkdownImport.parse("购物清单.md", "牛奶\n鸡蛋")
        assertEquals("购物清单", note.title)
        assertEquals("牛奶\n鸡蛋", note.content)
    }

    @Test
    fun `文件名没有扩展名也照常处理`() {
        val note = MarkdownImport.parse("随手记", "内容")
        assertEquals("随手记", note.title)
    }

    @Test
    fun `文件名只有扩展名时回退到默认标题`() {
        val note = MarkdownImport.parse(".md", "")
        assertEquals(MarkdownImport.DEFAULT_TITLE, note.title)
        assertEquals("", note.content)
    }

    @Test
    fun `文件名为空时回退到默认标题`() {
        assertEquals(MarkdownImport.DEFAULT_TITLE, MarkdownImport.parse(null, "").title)
    }

    @Test
    fun `标题前的空行会被跳过`() {
        val note = MarkdownImport.parse("x.md", "\n\n\n## 二级也算\n\n正文")
        assertEquals("二级也算", note.title)
        assertEquals("正文", note.content)
    }

    @Test
    fun `井号后没有空格不算标题`() {
        val note = MarkdownImport.parse("x.md", "#这不是标题\n正文")
        assertEquals("x", note.title)
        assertEquals("#这不是标题\n正文", note.content)
    }

    @Test
    fun `七个井号不算标题`() {
        val note = MarkdownImport.parse("x.md", "####### 太多\n正文")
        assertEquals("x", note.title)
    }

    @Test
    fun `CRLF 会被规范化成 LF`() {
        val note = MarkdownImport.parse("x.md", "# 标题\r\n\r\n第一行\r\n第二行\r\n")
        assertEquals("标题", note.title)
        assertEquals("第一行\n第二行", note.content)
    }

    @Test
    fun `标题两侧的空白会被裁掉`() {
        assertEquals("标题", MarkdownImport.parse("x.md", "   #    标题   ").title)
    }

    @Test
    fun `只有一行标题时正文为空`() {
        val note = MarkdownImport.parse("x.md", "# 光杆标题")
        assertEquals("光杆标题", note.title)
        assertEquals("", note.content)
    }

    @Test
    fun `空文件用文件名当标题`() {
        val note = MarkdownImport.parse("空白.md", "   \n\n  ")
        assertEquals("空白", note.title)
        assertEquals("", note.content)
    }

    @Test
    fun `正文里的井号行不会被误当成标题`() {
        val note = MarkdownImport.parse("x.md", "正文开头\n\n# 后面才有标题")
        assertEquals("x", note.title)
        assertEquals("正文开头\n\n# 后面才有标题", note.content)
    }
}
