package com.astelle.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.astelle.app.data.local.dao.FolderDao
import com.astelle.app.data.local.dao.NoteDao
import com.astelle.app.data.local.dao.PlanDayDao
import com.astelle.app.data.local.dao.TodoDao
import com.astelle.app.data.local.entity.FolderEntity
import com.astelle.app.data.local.entity.NoteEntity
import com.astelle.app.data.local.entity.PlanDayEntity
import com.astelle.app.data.local.entity.TodoEntity

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        PlanDayEntity::class,
        TodoEntity::class,
    ],
    // v4：新增 folders 表 + notes.folderId。
    // v5：folders.isPinned（文件夹置顶，④）—— **真 Migration**，从这版起不再允许清库。
    version = 5,
    exportSchema = false,
)
abstract class AstelleDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun planDayDao(): PlanDayDao
    abstract fun todoDao(): TodoDao

    companion object {
        const val NAME = "astelle.db"

        /** v4→5：folders 加 isPinned。只加列不动数据 —— 数据是用户的（产品红线） */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
