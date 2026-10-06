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

    /** 改名走原地 UPDATE：不必先读出来再整行回写，也就没有读写之间的竞态 */
    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    /**
     * 新分类排到末尾。
     *
     * 由 SQL 算而不是在 ViewModel 里读列表取 max：列表在抽屉关着时无人订阅
     * （WhileSubscribed），连点两次新建会拿到同一个值，两个分类排序撞车。
     */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM folders")
    suspend fun nextSortOrder(): Int

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
