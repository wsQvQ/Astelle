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

    @Query("SELECT * FROM todos WHERE planDayId = :planDayId ORDER BY isDone ASC, sortOrder ASC, createdAt ASC")
    fun observeByPlanDay(planDayId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE dueEpochDay = :epochDay ORDER BY isDone ASC, sortOrder ASC, createdAt ASC")
    fun observeByDueDay(epochDay: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TodoEntity)

    @Update
    suspend fun update(entity: TodoEntity)

    @Delete
    suspend fun delete(entity: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE todos SET isDone = :done, doneAt = :doneAt WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean, doneAt: Long?)
}
