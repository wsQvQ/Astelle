package com.astelle.app.data.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownCompatTest {

    @Test
    fun `表格紧跟段落时补一个空行`() {
        val md = """
            **天气（实测预报）**
            | 日期 | 芒市 |
            |---|---|
            | 10/8 | 晴 |
        """.trimIndent()
        val out = MarkdownCompat.render(md)
        assertEquals(
            """
            **天气（实测预报）**

            | 日期 | 芒市 |
            |---|---|
            | 10/8 | 晴 |
            """.trimIndent(),
            out,
        )
    }

    @Test
    fun `本来就有空行的表格原样不动`() {
        val md = """
            标题

            | a | b |
            |---|---|
            | 1 | 2 |
        """.trimIndent()
        assertEquals(md, MarkdownCompat.render(md))
    }

    @Test
    fun `没有分隔行的竖线文本不补空行`() {
        // 普通的 `| a | b |` 行后面没有 |---|，不该被当成表格
        val md = """
            前一行
            | a | b |
            | c | d |
        """.trimIndent()
        assertEquals(md, MarkdownCompat.render(md))
    }

    @Test
    fun `文档开头的表格不补`() {
        val md = """
            | a | b |
            |---|---|
            | 1 | 2 |
        """.trimIndent()
        assertEquals(md, MarkdownCompat.render(md))
    }

    @Test
    fun `多张表各补各的`() {
        val md = """
            段落一
            | a |
            |---|
            | 1 |

            中间文字
            | b |
            |---|
            | 2 |
        """.trimIndent()
        val out = MarkdownCompat.render(md)
        assertEquals(
            """
            段落一

            | a |
            |---|
            | 1 |

            中间文字

            | b |
            |---|
            | 2 |
            """.trimIndent(),
            out,
        )
    }

    @Test
    fun `CRLF 先规范成 LF 再补`() {
        val md = "文字\r\n| a |\r\n|---|\r\n| 1 |"
        assertEquals("文字\n\n| a |\n|---|\n| 1 |", MarkdownCompat.render(md))
    }

    @Test
    fun `空串和单行原样返回`() {
        assertEquals("", MarkdownCompat.render(""))
        assertEquals("只有一行", MarkdownCompat.render("只有一行"))
    }
}
