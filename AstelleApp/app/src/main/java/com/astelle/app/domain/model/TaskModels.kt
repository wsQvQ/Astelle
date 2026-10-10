package com.astelle.app.domain.model

import java.time.LocalDate

/**
 * 待办（14 号计划）：可独立存在，也可作为**母项**挂子项（parentId 自关联）。
 * 截止日可空：空 = 无截止（列表里显示「—」）。
 */
data class Todo(
    val id: String,
    val title: String,
    val note: String = "",
    val isDone: Boolean = false,
    val dueDate: LocalDate? = null,
    val parentId: String? = null,
    val createdAt: Long = 0L,
    val doneAt: Long? = null,
    val sortOrder: Int = 0,
)

/**
 * 历史（10-11 用户定名）：正计时器 —— 「已经 x 天」。
 * 它不是任务：没有勾选，只有起始日、备注与置顶。
 */
data class History(
    val id: String,
    val title: String,
    val note: String = "",
    val startDate: LocalDate,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
)
