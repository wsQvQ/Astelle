package com.astelle.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
    // 目前仍走 fallbackToDestructiveMigration，升级会清空本地数据；
    // 开发阶段可接受，正式发版前必须换成真实 Migration。
    version = 4,
    exportSchema = false,
)
abstract class AstelleDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun planDayDao(): PlanDayDao
    abstract fun todoDao(): TodoDao

    companion object {
        const val NAME = "astelle.db"
    }
}
