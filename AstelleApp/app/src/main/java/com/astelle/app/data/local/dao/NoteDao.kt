package com.astelle.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.astelle.app.data.local.NoteSummaryRow
import com.astelle.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /**
     * 抽屉列表用的轻量投影。
     *
     * 这里**故意不写 `SELECT *`**：正文可能几十万字，把全文读进
     * CursorWindow（约 2MB）会抛 SQLiteBlobTooBigException，打开应用即崩。
     * 列表只需要「标题 + 一小段摘要 + 字数」，所以用 substr / length 在
     * SQL 里就裁掉，全文只在真正打开某篇笔记时才取。
     *
     * 搜索也下推到 SQL：`:query` 为空时不过滤，否则标题或正文命中。
     * （SQLite 的 LIKE 对 ASCII 默认不区分大小写，与原先的
     * `contains(ignoreCase = true)` 行为一致。）
     */
    @Query(
        """
        SELECT id,
               title,
               substr(content, 1, 280) AS snippet,
               length(content) AS charCount,
               createdAt,
               updatedAt,
               mood,
               isPinned,
               isFavorite,
               isArchived,
               folderId
        FROM notes
        WHERE isArchived = 0
          AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun observeSummaries(query: String): Flow<List<NoteSummaryRow>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteEntity)

    @Update
    suspend fun update(entity: NoteEntity)

    @Delete
    suspend fun delete(entity: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE notes SET isArchived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, updatedAt: Long)

    /** 移入 / 移出分类：folderId 传 null 即移出到「未分类」 */
    @Query("UPDATE notes SET folderId = :folderId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun moveToFolder(id: String, folderId: String?, updatedAt: Long)
}
