package com.astelle.app.domain.repository

import com.astelle.app.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>

    suspend fun upsert(folder: Folder)

    /** 删除分类；同时把它名下的笔记放回未分类，不留悬空引用 */
    suspend fun delete(id: String)
}
