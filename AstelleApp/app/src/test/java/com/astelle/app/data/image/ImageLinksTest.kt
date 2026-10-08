package com.astelle.app.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageLinksTest {

    @Test
    fun `显示解析把相对路径换成绝对文件地址`() {
        val md = "前面\n![](images/abc.jpg)\n后面"
        val out = ImageLinks.resolveForDisplay(md, "/data/user/0/app/files")
        assertEquals(
            "前面\n![](file:///data/user/0/app/files/images/abc.jpg)\n后面",
            out,
        )
    }

    @Test
    fun `显示解析不动普通链接和网络图片`() {
        val md = "[站](https://a.com) ![网](https://a.com/x.png) ![](images/1.png)"
        val out = ImageLinks.resolveForDisplay(md, "/base")
        assertEquals(
            "[站](https://a.com) ![网](https://a.com/x.png) ![](file:///base/images/1.png)",
            out,
        )
    }

    @Test
    fun `md导出把图片换成占位`() {
        val md = "标题\n![](images/abc.jpg)\n完"
        assertEquals("标题\n[图片]\n完", ImageLinks.toMarkdownExport(md))
    }

    @Test
    fun `清点引用的图片路径`() {
        val md = "![](images/a.jpg)\n![](img x) ![x](images/b.png)\n![](images/a.jpg)"
        assertEquals(listOf("images/a.jpg", "images/b.png"), ImageLinks.referencedPaths(md))
    }

    @Test
    fun `没有图片就原样返回`() {
        val md = "普通一段，没有图"
        assertEquals(md, ImageLinks.toMarkdownExport(md))
        assertEquals(md, ImageLinks.resolveForDisplay(md, "/base"))
        assertEquals(emptyList<String>(), ImageLinks.referencedPaths(md))
    }
}
