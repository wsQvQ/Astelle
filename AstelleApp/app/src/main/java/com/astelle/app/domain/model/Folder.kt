package com.astelle.app.domain.model

/**
 * 笔记分类。单层，不嵌套。
 *
 * 笔记的归属看 [NoteSummary.folderId]：为 null 即「未分类」。
 */
data class Folder(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
)
