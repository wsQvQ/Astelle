package com.astelle.app.data.repository

import android.content.Context
import com.astelle.app.data.image.ImageLinks
import com.astelle.app.data.image.ImageStore
import com.astelle.app.data.local.dao.NoteDao
import com.astelle.app.data.local.escapeLikePattern
import com.astelle.app.data.local.toDomain
import com.astelle.app.data.local.toEntity
import com.astelle.app.domain.model.Note
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.domain.repository.NoteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    // 删笔记清图要用私有目录（⑫）；Context 住在数据层，ViewModel 不碰 Android
    @ApplicationContext private val appContext: Context,
) : NoteRepository {

    override fun observeSummaries(query: String): Flow<List<NoteSummary>> =
        // 转义放在这里：DAO 的 SQL 只管拿一个安全的模式去 LIKE
        noteDao.observeSummaries(escapeLikePattern(query)).map { list -> list.map { it.toDomain() } }

    override suspend fun getNote(id: String): Note? = noteDao.getById(id)?.toDomain()

    override suspend fun upsert(note: Note) {
        noteDao.upsert(note.toEntity())
    }

    override suspend fun delete(id: String) {
        // 删笔记连带清图（⑫）：引用的图片在删除**前**拿，删完就取不到了。
        // **只删 app 私有副本**，绝不动系统相册（产品红线）。
        // ⚠️ 已知边界：两篇笔记粘贴共用同一张图时，删一篇会带走图
        val images = noteDao.getById(id)?.toDomain()
            ?.let { ImageLinks.referencedPaths(it.content) }
            .orEmpty()
        noteDao.deleteById(id)
        if (images.isNotEmpty()) {
            withContext(Dispatchers.IO) { ImageStore.deleteImages(appContext, images) }
        }
    }

    override suspend fun togglePinned(id: String) {
        noteDao.togglePinned(id)
    }

    override suspend fun toggleFavorite(id: String) {
        noteDao.toggleFavorite(id)
    }

    override suspend fun moveToFolder(id: String, folderId: String?) {
        // 不碰 updatedAt：它是「内容最后修改时间」。挪个位置不算改内容，
        // 刷了它笔记会凭空跳到那一组最前，看着像被置顶了。
        // 「移到哪儿了」的反馈交给 UI 层的 Toast
        noteDao.moveToFolder(id, folderId)
    }
}
