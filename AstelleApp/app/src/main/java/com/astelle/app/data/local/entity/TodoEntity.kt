package com.astelle.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 待办表（v6 重建：去掉 planDayId 外键 + 新增 note/parentId，见 MIGRATION_5_6）。
 * 母子项 = parentId 自关联；级联规则在 TaskLogic（业务层），不在 SQL。
 */
@Entity(
    tableName = "todos",
    indices = [Index("dueEpochDay"), Index("isDone"), Index("parentId")],
)
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    val isDone: Boolean = false,
    val dueEpochDay: Long? = null,
    val parentId: String? = null,
    val createdAt: Long = 0L,
    val doneAt: Long? = null,
    val sortOrder: Int = 0,
)
