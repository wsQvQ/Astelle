package com.astelle.app.data.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownImportTest {

    @Test
    fun `文件名优先当标题，正文原样保留`() {
        val note = MarkdownImport.parse(
            "测试.md",
            """
            # Astelle

            这是正文第一段。
            """.trimIndent(),
        )
        assertEquals("测试", note.title)
        assertEquals("# Astelle\n\n这是正文第一段。", note.content)
    }

    @Test
    fun `文件名没有标题时也一样`() {
        val note = MarkdownImport.parse("购物清单.md", "牛奶\n鸡蛋")
        assertEquals("购物清单", note.title)
        assertEquals("牛奶\n鸡蛋", note.content)
    }

    @Test
    fun `文件名没有扩展名也照常处理`() {
        val note = MarkdownImport.parse("随手记", "内容")
        assertEquals("随手记", note.title)
        assertEquals("内容", note.content)
    }

    @Test
    fun `文件名只有扩展名时，退而用正文里的首个标题`() {
        val note = MarkdownImport.parse(".md", "# 从正文里来的标题\n\n正文")
        assertEquals("从正文里来的标题", note.title)
        // 标题是从正文抽出来的，那一行要去掉，否则重复
        assertEquals("正文", note.content)
    }

    @Test
    fun `文件名为空时，退而用正文里的首个标题`() {
        val note = MarkdownImport.parse(null, "## 二级也算\n\n正文")
        assertEquals("二级也算", note.title)
        assertEquals("正文", note.content)
    }

    @Test
    fun `文件名和标题都没有时用默认标题`() {
        val note = MarkdownImport.parse(".md", "就是一段普通文字")
        assertEquals(MarkdownImport.DEFAULT_TITLE, note.title)
        assertEquals("就是一段普通文字", note.content)
    }

    @Test
    fun `退而用标题时空行会被跳过`() {
        val note = MarkdownImport.parse(".md", "\n\n\n# 标题\n\n正文")
        assertEquals("标题", note.title)
        assertEquals("正文", note.content)
    }

    @Test
    fun `井号后没有空格不算标题`() {
        val note = MarkdownImport.parse(".md", "#这不是标题\n正文")
        assertEquals(MarkdownImport.DEFAULT_TITLE, note.title)
        assertEquals("#这不是标题\n正文", note.content)
    }

    @Test
    fun `七个井号不算标题`() {
        val note = MarkdownImport.parse(".md", "####### 太多\n正文")
        assertEquals(MarkdownImport.DEFAULT_TITLE, note.title)
    }

    @Test
    fun `正文里的井号行不会被误当成标题`() {
        val note = MarkdownImport.parse(".md", "正文开头\n\n# 后面才有标题")
        assertEquals(MarkdownImport.DEFAULT_TITLE, note.title)
        assertEquals("正文开头\n\n# 后面才有标题", note.content)
    }

    @Test
    fun `CRLF 会被规范化成 LF`() {
        val note = MarkdownImport.parse("x.md", "# 标题\r\n\r\n第一行\r\n第二行\r\n")
        assertEquals("x", note.title)
        assertEquals("# 标题\n\n第一行\n第二行", note.content)
    }

    @Test
    fun `标题两侧的空白会被裁掉`() {
        assertEquals("标题", MarkdownImport.parse(".md", "   #    标题   ").title)
        assertEquals("测试", MarkdownImport.parse("  测试  .md", "").title)
    }

    @Test
    fun `只有一行标题时正文为空`() {
        val note = MarkdownImport.parse(".md", "# 光杆标题")
        assertEquals("光杆标题", note.title)
        assertEquals("", note.content)
    }

    @Test
    fun `空文件用文件名当标题`() {
        val note = MarkdownImport.parse("空白.md", "   \n\n  ")
        assertEquals("空白", note.title)
        assertEquals("", note.content)
    }
}
