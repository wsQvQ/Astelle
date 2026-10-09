package com.astelle.app.domain.repository

import com.astelle.app.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>

    suspend fun upsert(folder: Folder)

    /** 改名。只走 UPDATE，不整行回写 */
    suspend fun rename(id: String, name: String)

    /** 置顶切换（④）。不碰 updatedAt —— 置顶不是内容修改 */
    suspend fun togglePinned(id: String)

    /** 下一个排序位；新分类排在末尾 */
    suspend fun nextSortOrder(): Int

    /** 删除分类；同时把它名下的笔记放回未分类，不留悬空引用 */
    suspend fun delete(id: String)
}
