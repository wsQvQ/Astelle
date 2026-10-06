package com.astelle.app.data.repository

import com.astelle.app.data.local.dao.NoteDao
import com.astelle.app.data.local.toDomain
import com.astelle.app.data.local.toEntity
import com.astelle.app.domain.model.Note
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.domain.repository.NoteRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
) : NoteRepository {

    override fun observeSummaries(query: String): Flow<List<NoteSummary>> =
        noteDao.observeSummaries(query).map { list -> list.map { it.toDomain() } }

    override fun observeNote(id: String): Flow<Note?> =
        noteDao.observeById(id).map { it?.toDomain() }

    override suspend fun getNote(id: String): Note? = noteDao.getById(id)?.toDomain()

    override suspend fun upsert(note: Note) {
        noteDao.upsert(note.toEntity())
    }

    override suspend fun delete(id: String) {
        noteDao.deleteById(id)
    }

    override suspend fun togglePinned(id: String) {
        noteDao.togglePinned(id, updatedAt = System.currentTimeMillis())
    }

    override suspend fun toggleFavorite(id: String) {
        noteDao.toggleFavorite(id, updatedAt = System.currentTimeMillis())
    }

    override suspend fun moveToFolder(id: String, folderId: String?) {
        // 顺带刷新 updatedAt：移进新分类后笔记会浮到那一组的最前，
        // 用户能立刻看见「它确实过去了」——否则卡片纹丝不动，像没生效
        noteDao.moveToFolder(id, folderId, updatedAt = System.currentTimeMillis())
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        noteDao.setArchived(id, archived, updatedAt = System.currentTimeMillis())
    }
}
