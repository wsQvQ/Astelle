package com.astelle.app.domain.model

/**
 * 笔记的**轻量投影**，只给抽屉列表用。
 *
 * 为什么不让列表直接用 [Note]：正文可能有几十万字。列表如果把全文都读出来，
 * 一旦某一行超过 CursorWindow（约 2MB），SQLite 会抛
 * `SQLiteBlobTooBigException`，打开应用即崩。
 *
 * 所以列表只要「标题 + 一小段摘要 + 字数」这三样能画出卡片的料，
 * **全文只在真正打开某篇笔记时才取**。
 */
data class NoteSummary(
    val id: String,
    val title: String = "",
    /** 正文开头的一小段，不是全文 */
    val snippet: String = "",
    /** 正文字数（SQL length()，不是摘要长度） */
    val charCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val mood: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    /** 所属分类；null = 未分类。抽屉据此把笔记分到各个分组里 */
    val folderId: String? = null,
) {
    /** 卡片第一行：标题为空时退回正文首行 */
    val displayTitle: String
        get() = title.trim().ifBlank { firstLine().take(24) }

    /** 卡片第二行：正文首行 */
    val preview: String
        get() = firstLine()

    private fun firstLine(): String =
        snippet.trim().lineSequence().firstOrNull().orEmpty()
}
