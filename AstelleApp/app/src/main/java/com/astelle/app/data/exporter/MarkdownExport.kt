package com.astelle.app.data.exporter

import com.astelle.app.data.image.ImageLinks

import com.astelle.app.domain.model.Note

/**
 * 笔记 → .md 文本。纯函数、零 Android 依赖，可直接 JVM 单测。
 *
 * 与 [com.astelle.app.data.importer.MarkdownImport] 互为镜像：
 * 导出的文件再丢回导入，标题与正文能还原（见 MarkdownExportTest 的回环用例）。
 *
 * 规则（刻意保持简单，结果可预测）：
 *  1. 标题非空 → `# 标题` 单独成段；
 *  2. 正文原样接在后面，中间空一行；
 *  3. 两者都空 → 空串，调用方拿它判断「没东西可导」；
 *  4. 换行统一成 `\n`、首尾空白裁掉 —— 文件里不该拖着编辑器的行尾杂音。
 */
object MarkdownExport {

    const val DEFAULT_NAME = "笔记"

    /** 文件名长度上限（按字符算）。40 个汉字 = 120 字节 UTF-8，离 255 字节还很远 */
    const val MAX_NAME_LENGTH = 40

    /** 文件名里非法的字符：Windows 与多数文档提供方都会拒收 */
    private val ILLEGAL_NAME_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    /** 首尾要清掉的字符：空格、点（Windows 会吞掉末尾的点）、制表符 */
    private val EDGE_TRIM = charArrayOf(' ', '.', '\t')

    fun toMarkdown(title: String, content: String): String {
        val heading = title.trim()
        // .md 只导文本（⑫ 拍板）：图片换 [图片] 占位 —— 导出目录里没有 images/，
        // 留相对路径就是死链，占位符更诚实
        val body = ImageLinks.toMarkdownExport(normalize(content)).trim()
        return buildList {
            if (heading.isNotEmpty()) add("# $heading")
            if (body.isNotEmpty()) add(body)
        }.joinToString("\n\n")
    }

    /**
     * 导出文件名（不含扩展名）。
     *
     * 优先用标题；标题空白则退用正文第一行，和 [Note.displayTitle] 一个口径。
     * 非法字符换成下划线，首尾的点和空格清掉（Windows 会吞掉）；
     * 清完只剩符号的名字（`:::` 这种会洗成 `___`）认不出来也没意义，
     * 一并退回 [DEFAULT_NAME] —— 别给 SAF 一个空名字或一串下划线，
     * 保存框会不认，用户也不知道那是什么。
     */
    fun baseName(title: String, content: String): String {
        val source = title.trim().ifBlank {
            normalize(content).trim().lineSequence().firstOrNull().orEmpty()
        }
        val cleaned = ILLEGAL_NAME_CHARS.replace(source, "_")
            .trim(*EDGE_TRIM)
            .take(MAX_NAME_LENGTH)
            .trim(*EDGE_TRIM)
        return if (cleaned.any { it.isLetterOrDigit() }) cleaned else DEFAULT_NAME
    }

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n')
}

/** 供调用方按域模型直接取用；实现只有一处，在 [MarkdownExport] */
fun Note.toMarkdown(): String = MarkdownExport.toMarkdown(title, content)

/** 与 [Note.toMarkdown] 同口径的导出文件名 */
fun Note.exportBaseName(): String = MarkdownExport.baseName(title, content)
