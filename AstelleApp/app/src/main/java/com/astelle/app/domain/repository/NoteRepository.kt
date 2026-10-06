package com.astelle.app.domain.repository

import com.astelle.app.domain.model.Note
import com.astelle.app.domain.model.NoteSummary
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    /**
     * 抽屉列表的数据源。只返回摘要投影，**不含正文全文** ——
     * 正文由 [getNote] 在真正打开某篇时才取，避免列表把超大行读进
     * SQLite 的 CursorWindow（约 2MB）直接崩溃。
     *
     * [query] 为空表示不过滤，否则标题或正文命中即算匹配（在 SQL 里做）。
     */
    fun observeSummaries(query: String = ""): Flow<List<NoteSummary>>
    suspend fun getNote(id: String): Note?
    suspend fun upsert(note: Note)
    suspend fun delete(id: String)

    /** 翻转置顶 / 收藏。不读全文，一条 UPDATE 搞定 */
    suspend fun togglePinned(id: String)
    suspend fun toggleFavorite(id: String)

    /** 移入 / 移出分类；[folderId] 传 null 即回到「未分类」。同样只走一条 UPDATE */
    suspend fun moveToFolder(id: String, folderId: String?)
}
