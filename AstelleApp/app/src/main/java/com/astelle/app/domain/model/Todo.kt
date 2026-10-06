package com.astelle.app.domain.model

import java.time.LocalDate

/**
 * 待办：可独立存在，也可挂在某个日子下。
 */
data class Todo(
    val id: String,
    val title: String,
    val isDone: Boolean = false,
    val dueDate: LocalDate? = null,
    val planDayId: String? = null,
    val createdAt: Long = 0L,
    val doneAt: Long? = null,
    val sortOrder: Int = 0,
)
