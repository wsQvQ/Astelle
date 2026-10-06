package com.astelle.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astelle.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 删掉分类前，先把它名下的笔记放回「未分类」。
     *
     * 不做这一步的话，那些笔记的 folderId 会指向一个已经不存在的分类，
     * 在抽屉里就成了谁都认领不了的孤儿 —— 既不在任何分组里，也不算未分类。
     */
    @Query("UPDATE notes SET folderId = NULL, updatedAt = :updatedAt WHERE folderId = :id")
    suspend fun detachNotes(id: String, updatedAt: Long)
}
