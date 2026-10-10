package com.astelle.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 历史表（v6 新增）：正计时器的起始日 + 置顶 */
@Entity(tableName = "histories")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    val startEpochDay: Long,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
)
