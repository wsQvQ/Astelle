package com.astelle.app.data.local

import com.astelle.app.data.local.entity.NoteEntity
import com.astelle.app.data.local.entity.PlanDayEntity
import com.astelle.app.data.local.entity.TodoEntity
import com.astelle.app.domain.model.Note
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.domain.model.PlanDay
import com.astelle.app.domain.model.Todo
import java.time.LocalDate

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mood = mood,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mood = mood,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
)

fun NoteSummaryRow.toDomain(): NoteSummary = NoteSummary(
    id = id,
    title = title,
    snippet = snippet,
    charCount = charCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mood = mood,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
)

fun PlanDayEntity.toDomain(): PlanDay = PlanDay(
    id = id,
    name = name,
    date = LocalDate.ofEpochDay(dateEpochDay),
    isYearly = isYearly,
    intention = intention,
    createdAt = createdAt,
)

fun PlanDay.toEntity(): PlanDayEntity = PlanDayEntity(
    id = id,
    name = name,
    dateEpochDay = date.toEpochDay(),
    isYearly = isYearly,
    intention = intention,
    createdAt = createdAt,
)

fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    title = title,
    isDone = isDone,
    dueDate = dueEpochDay?.let(LocalDate::ofEpochDay),
    planDayId = planDayId,
    createdAt = createdAt,
    doneAt = doneAt,
    sortOrder = sortOrder,
)

fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    isDone = isDone,
    dueEpochDay = dueDate?.toEpochDay(),
    planDayId = planDayId,
    createdAt = createdAt,
    doneAt = doneAt,
    sortOrder = sortOrder,
)
