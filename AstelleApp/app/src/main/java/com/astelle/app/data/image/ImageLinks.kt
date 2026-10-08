package com.astelle.app.data.image

/**
 * 正文里图片引用的纯文本处理：显示解析、导出替换、清点。
 *
 * 图片在正文里**永远是相对路径** `![](images/<uuid>.jpg)`（⑫ 拍板：不存 content://，
 * 那是临时授权，重启就废）。渲染/导出各自按需要换算，原文永远不动。
 */
object ImageLinks {

    /** 图片引用：`![任意 alt](images/xxx.jpg)`；alt 不含 `]`（防止跨条目误吞），路径不含空格/右括号 */
    private val IMAGE_LINK = Regex("""!\[[^\]\n]*]\((images/[^)\s]+)\)""")

    /** 一篇笔记引用到的图片相对路径（删笔记清图用） */
    fun referencedPaths(markdown: String): List<String> =
        IMAGE_LINK.findAll(markdown).map { it.groupValues[1] }.distinct().toList()

    /**
     * 显示解析（预览 + 导出长图共用）：`images/xxx.jpg` → `file://<baseDir>/images/xxx.jpg`。
     * 只改喂给渲染器的文本，**绝不落库**。
     */
    fun resolveForDisplay(markdown: String, baseDir: String): String {
        val root = baseDir.trimEnd('/')
        return IMAGE_LINK.replace(markdown) { match ->
            val altPart = match.value.substringBefore("(images/")
            "$altPart(file://$root/${match.groupValues[1]})"
        }
    }

    /** .md 导出：只导文本，图片换 `[图片]` 占位（⑫ 拍板） */
    fun toMarkdownExport(markdown: String): String = IMAGE_LINK.replace(markdown, "[图片]")
}
