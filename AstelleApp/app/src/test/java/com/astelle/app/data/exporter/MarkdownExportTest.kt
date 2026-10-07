package com.astelle.app.data.exporter

import com.astelle.app.data.importer.MarkdownImport
import com.astelle.app.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 导出是纯函数层，测起来不需要 Android。
 *
 * 其中「导出的 md 丢回导入能还原」那条是回环测试：
 * [MarkdownImport] 与 [MarkdownExport] 是一对镜像，改任何一边都要过这一条。
 */
class MarkdownExportTest {

    private fun note(title: String, content: String) = Note(
        id = "n1",
        title = title,
        content = content,
        createdAt = 0L,
        updatedAt = 0L,
    )

    /* ---------- 正文拼接 ---------- */

    @Test
    fun `标题和正文各占一段`() {
        assertEquals("# 标题\n\n正文", MarkdownExport.toMarkdown("标题", "正文"))
    }

    @Test
    fun `标题空白就不写标题行`() {
        assertEquals("只有正文", MarkdownExport.toMarkdown("", "只有正文"))
        assertEquals("只有正文", MarkdownExport.toMarkdown("   ", "只有正文"))
    }

    @Test
    fun `正文空白就只留标题`() {
        assertEquals("# 标题", MarkdownExport.toMarkdown("标题", "  \n  "))
    }

    @Test
    fun `两个都空返回空串，调用方靠它判断没东西可导`() {
        assertEquals("", MarkdownExport.toMarkdown("", ""))
        assertEquals("", MarkdownExport.toMarkdown("   ", " \n "))
    }

    @Test
    fun `首尾空白裁掉，换行统一成 LF`() {
        assertEquals(
            "# 标题\n\n第一行\n第二行",
            MarkdownExport.toMarkdown("  标题  ", "\r\n第一行\r\n第二行\r\n"),
        )
    }

    @Test
    fun `域模型的 toMarkdown 与对象实现同口径`() {
        val n = note("标题", "正文")
        assertEquals(MarkdownExport.toMarkdown(n.title, n.content), n.toMarkdown())
        assertEquals(MarkdownExport.baseName(n.title, n.content), n.exportBaseName())
    }

    /* ---------- 回环 ---------- */

    @Test
    fun `导出的 md 丢回导入能还原`() {
        val md = note("测试笔记", "第一段\n\n第二段").toMarkdown()
        val back = MarkdownImport.parse(fileName = null, raw = md)
        assertEquals("测试笔记", back.title)
        assertEquals("第一段\n\n第二段", back.content)
    }

    @Test
    fun `无标题笔记回环时内容一点不丢，标题由导入方兜底`() {
        val md = note("", "只有正文\n第二行").toMarkdown()
        val back = MarkdownImport.parse(fileName = null, raw = md)
        // 没有 # 标题行可认，导入方退回默认标题 —— 这是导入的既定规则，
        // 导出这边不替它猜标题（把首行抬成标题反而会改动正文结构）
        assertEquals(MarkdownImport.DEFAULT_TITLE, back.title)
        assertEquals("只有正文\n第二行", back.content)
    }

    /* ---------- 文件名 ---------- */

    @Test
    fun `文件名优先用标题`() {
        assertEquals("我的笔记", MarkdownExport.baseName("我的笔记", "正文"))
    }

    @Test
    fun `标题空白时退用正文第一行`() {
        assertEquals("开头一行", MarkdownExport.baseName("", "开头一行\n第二行"))
    }

    @Test
    fun `非法字符换成下划线`() {
        assertEquals("a_b_c_d", MarkdownExport.baseName("a/b:c*d", "x"))
    }

    @Test
    fun `首尾的点和空格清掉，Windows 不认`() {
        assertEquals("笔记", MarkdownExport.baseName(" 笔记. ", "x"))
    }

    @Test
    fun `名字太长截断到四十个字符`() {
        assertEquals(
            MarkdownExport.MAX_NAME_LENGTH,
            MarkdownExport.baseName("好".repeat(100), "x").length,
        )
    }

    @Test
    fun `拿不到可用字符就退回默认名`() {
        assertEquals(MarkdownExport.DEFAULT_NAME, MarkdownExport.baseName("", ""))
        assertEquals(MarkdownExport.DEFAULT_NAME, MarkdownExport.baseName("   ", "  "))
        // 非法字符洗完只剩符号（`:::` → `___`），同样是没法认的名字
        assertEquals(MarkdownExport.DEFAULT_NAME, MarkdownExport.baseName(":::", "x"))
    }
}
