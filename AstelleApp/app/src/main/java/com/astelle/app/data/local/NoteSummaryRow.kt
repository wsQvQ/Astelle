package com.astelle.app.data.local

/**
 * [com.astelle.app.data.local.dao.NoteDao.observeSummaries] 的查询结果行。
 *
 * 不是实体，只是 DAO 的投影容器 —— 字段名必须和 SQL 里的列别名一致。
 * 留意这里**没有 content 全文**，只有 `snippet`（substr 截出来的开头）
 * 和 `charCount`（length() 算出来的字数）。
 */
data class NoteSummaryRow(
    val id: String,
    val title: String,
    val snippet: String,
    val charCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val mood: String?,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val isArchived: Boolean,
)
