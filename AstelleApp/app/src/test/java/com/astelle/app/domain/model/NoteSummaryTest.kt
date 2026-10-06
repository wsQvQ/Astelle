package com.astelle.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSummaryTest {

    private fun summary(
        title: String = "",
        snippet: String = "",
        charCount: Int = 0,
    ) = NoteSummary(
        id = "1",
        title = title,
        snippet = snippet,
        charCount = charCount,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `有标题时标题位用标题`() {
        assertEquals("我的标题", summary(title = "我的标题", snippet = "正文首行").displayTitle)
    }

    @Test
    fun `标题只有空白时退回正文首行`() {
        assertEquals("正文首行", summary(title = "   ", snippet = "正文首行\n第二行").displayTitle)
    }

    @Test
    fun `退回正文首行时会截断到 24 字`() {
        val long = "一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十"
        assertEquals(24, summary(snippet = long).displayTitle.length)
    }

    @Test
    fun `摘要取正文首行，不带后面的行`() {
        assertEquals("第一行", summary(snippet = "第一行\n第二行\n第三行").preview)
    }

    @Test
    fun `摘要两侧空白会被裁掉`() {
        assertEquals("正文", summary(snippet = "  \n  正文  \n  ").preview)
    }

    @Test
    fun `摘要为空时两个展示位都不炸`() {
        val s = summary()
        assertEquals("", s.preview)
        assertEquals("", s.displayTitle)
    }

    @Test
    fun `字数取的是正文字数而不是摘要长度`() {
        // 摘要被 substr 截到 280，字数必须是 SQL length() 的全文长度
        val s = summary(snippet = "短摘要", charCount = 12345)
        assertEquals(12345, s.charCount)
        assertEquals("短摘要", s.preview)
    }
}
