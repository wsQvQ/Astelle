package com.astelle.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.astelle.app.data.local.dao.NoteDao
import com.astelle.app.data.local.dao.PlanDayDao
import com.astelle.app.data.local.dao.TodoDao
import com.astelle.app.data.local.entity.NoteEntity
import com.astelle.app.data.local.entity.PlanDayEntity
import com.astelle.app.data.local.entity.TodoEntity

@Database(
    entities = [NoteEntity::class, PlanDayEntity::class, TodoEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AstelleDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun planDayDao(): PlanDayDao
    abstract fun todoDao(): TodoDao

    companion object {
        const val NAME = "astelle.db"
    }
}
