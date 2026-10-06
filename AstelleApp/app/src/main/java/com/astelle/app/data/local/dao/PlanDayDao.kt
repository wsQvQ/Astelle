package com.astelle.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.astelle.app.data.local.entity.PlanDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDayDao {
    @Query("SELECT * FROM plan_days ORDER BY dateEpochDay ASC")
    fun observeAll(): Flow<List<PlanDayEntity>>

    @Query("SELECT * FROM plan_days WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<PlanDayEntity?>

    @Query("SELECT * FROM plan_days WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlanDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlanDayEntity)

    @Update
    suspend fun update(entity: PlanDayEntity)

    @Delete
    suspend fun delete(entity: PlanDayEntity)

    @Query("DELETE FROM plan_days WHERE id = :id")
    suspend fun deleteById(id: String)
}
