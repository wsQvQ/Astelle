package com.astelle.app.data.repository

import com.astelle.app.data.local.dao.FolderDao
import com.astelle.app.data.local.toDomain
import com.astelle.app.data.local.toEntity
import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.repository.FolderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
) : FolderRepository {

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(folder: Folder) {
        folderDao.upsert(folder.toEntity())
    }

    override suspend fun rename(id: String, name: String) {
        folderDao.rename(id, name)
    }

    override suspend fun nextSortOrder(): Int = folderDao.nextSortOrder()

    override suspend fun delete(id: String) {
        // 顺序要紧：先把笔记放出来，再删分类
        folderDao.detachNotes(id)
        folderDao.deleteById(id)
    }
}
