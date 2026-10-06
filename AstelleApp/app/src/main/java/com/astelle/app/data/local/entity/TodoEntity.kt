package com.astelle.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = PlanDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planDayId"), Index("dueEpochDay"), Index("isDone")],
)
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isDone: Boolean = false,
    val dueEpochDay: Long? = null,
    val planDayId: String? = null,
    val createdAt: Long = 0L,
    val doneAt: Long? = null,
    val sortOrder: Int = 0,
)
