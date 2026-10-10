package com.astelle.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.astelle.app.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isDone ASC, sortOrder ASC, createdAt ASC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE parentId = :parentId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeChildren(parentId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TodoEntity)

    @Update
    suspend fun update(entity: TodoEntity)

    @Query("UPDATE todos SET isDone = :done, doneAt = :doneAt WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, doneAt: Long?)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: String)

    /** 删母项连子项（业务口径：母项没了子项没意义） */
    @Query("DELETE FROM todos WHERE parentId = :parentId")
    suspend fun deleteChildren(parentId: String)
}
