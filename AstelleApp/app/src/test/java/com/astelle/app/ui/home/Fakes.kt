package com.astelle.app.ui.home

import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.model.Note
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.domain.repository.FolderRepository
import com.astelle.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/*
 * ViewModel 测试用的内存仓库。放在单独文件里，几个测试类共用，
 * 免得每加一处仓储方法就要在多个副本里同步改一遍。
 */

internal class FakeNoteRepository : NoteRepository {

    val stored = LinkedHashMap<String, Note>()
    private val summaries = MutableStateFlow<List<NoteSummary>>(emptyList())

    private fun publish() {
        summaries.value = stored.values.map { it.toSummary() }
    }

    override fun observeSummaries(query: String): Flow<List<NoteSummary>> =
        if (query.isBlank()) {
            summaries
        } else {
            summaries.map { list ->
                list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.snippet.contains(query, ignoreCase = true)
                }
            }
        }

    override suspend fun getNote(id: String): Note? = stored[id]

    override suspend fun upsert(note: Note) {
        stored[note.id] = note
        publish()
    }

    override suspend fun delete(id: String) {
        stored.remove(id)
        publish()
    }

    override suspend fun togglePinned(id: String) {
        stored[id]?.let { stored[id] = it.copy(isPinned = !it.isPinned); publish() }
    }

    override suspend fun toggleFavorite(id: String) {
        stored[id]?.let { stored[id] = it.copy(isFavorite = !it.isFavorite); publish() }
    }

    override suspend fun moveToFolder(id: String, folderId: String?) {
        stored[id]?.let { stored[id] = it.copy(folderId = folderId); publish() }
    }
}

internal class FakeFolderRepository : FolderRepository {

    val stored = LinkedHashMap<String, Folder>()
    private val flow = MutableStateFlow<List<Folder>>(emptyList())

    private fun publish() {
        // 和 DAO 同一把排序尺：置顶最前，其余按 sortOrder、createdAt
        flow.value = stored.values.sortedWith(
            compareByDescending<Folder> { it.isPinned }.thenBy { it.sortOrder }.thenBy { it.createdAt },
        )
    }

    override fun observeFolders(): Flow<List<Folder>> = flow

    override suspend fun upsert(folder: Folder) {
        stored[folder.id] = folder
        publish()
    }

    override suspend fun rename(id: String, name: String) {
        stored[id]?.let { stored[id] = it.copy(name = name); publish() }
    }

    override suspend fun togglePinned(id: String) {
        stored[id]?.let { stored[id] = it.copy(isPinned = !it.isPinned); publish() }
    }

    override suspend fun nextSortOrder(): Int =
        (stored.values.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun delete(id: String) {
        // 只删分类本身。真实实现还会顺手把笔记 detach 到未分类，
        // 但那是 NoteDao 的活 —— 这里故意不做，好让「ViewModel 有没有
        // 同步内存副本」这件事在测试里真的暴露出来
        stored.remove(id)
        publish()
    }
}

/** 与 NoteDao.observeSummaries 的投影保持一致，便于测试聚焦在 ViewModel 行为上 */
internal fun Note.toSummary() = NoteSummary(
    id = id,
    title = title,
    snippet = content.take(280),
    charCount = content.length,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mood = mood,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
    folderId = folderId,
)
