package com.astelle.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.astelle.app.data.local.dao.FolderDao
import com.astelle.app.data.local.dao.HistoryDao
import com.astelle.app.data.local.dao.NoteDao
import com.astelle.app.data.local.dao.TodoDao
import com.astelle.app.data.local.entity.FolderEntity
import com.astelle.app.data.local.entity.HistoryEntity
import com.astelle.app.data.local.entity.NoteEntity
import com.astelle.app.data.local.entity.TodoEntity

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TodoEntity::class,
        HistoryEntity::class,
    ],
    // v4：新增 folders 表 + notes.folderId。
    // v5：folders.isPinned（文件夹置顶，④）。
    // v6：todos 重建（去 planDayId 外键，加 note/parentId 母子项）+ 新增 histories（历史正计时）
    //     —— 14 号计划。**真 Migration**，从 v5 起就不允许清库（数据是用户的）。
    version = 6,
    exportSchema = false,
)
abstract class AstelleDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun todoDao(): TodoDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "astelle.db"

        /** v4→5：folders 加 isPinned。只加列不动数据 —— 数据是用户的（产品红线） */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v5→v6：todos 换代（去 planDayId 外键、加 note/parentId），新增 histories，
         * 顺手清掉从没露出过 UI 的 plan_days 旧概念。
         * ⚠️ 换表用「建新表 + 搬数据 + 换名」三步走 —— SQLite 的 DROP COLUMN 老版本不支持。
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `todos_new` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `isDone` INTEGER NOT NULL DEFAULT 0,
                        `dueEpochDay` INTEGER,
                        `parentId` TEXT,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `doneAt` INTEGER,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO todos_new (id, title, note, isDone, dueEpochDay, parentId, createdAt, doneAt, sortOrder)
                    SELECT id, title, '', isDone, dueEpochDay, NULL, createdAt, doneAt, sortOrder FROM todos
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE todos")
                db.execSQL("ALTER TABLE todos_new RENAME TO todos")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todos_dueEpochDay` ON `todos` (`dueEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todos_isDone` ON `todos` (`isDone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todos_parentId` ON `todos` (`parentId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `histories` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `startEpochDay` INTEGER NOT NULL,
                        `isPinned` INTEGER NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // plan_days：从 v4 存在但从未有 UI 入口（无用户数据可言），随 v6 退场
                db.execSQL("DROP TABLE IF EXISTS plan_days")
            }
        }
    }
}
