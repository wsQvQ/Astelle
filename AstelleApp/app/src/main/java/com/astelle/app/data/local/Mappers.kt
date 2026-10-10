package com.astelle.app.data.local

import com.astelle.app.data.local.entity.FolderEntity
import com.astelle.app.data.local.entity.HistoryEntity
import com.astelle.app.data.local.entity.NoteEntity
import com.astelle.app.data.local.entity.TodoEntity
import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.model.History
import com.astelle.app.domain.model.Note
import com.astelle.app.domain.model.NoteSummary
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
    folderId = folderId,
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
    folderId = folderId,
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
    folderId = folderId,
)

fun TodoEntity.toDomain(): Todo = Todo(
    id = id,
    title = title,
    note = note,
    isDone = isDone,
    dueDate = dueEpochDay?.let(LocalDate::ofEpochDay),
    parentId = parentId,
    createdAt = createdAt,
    doneAt = doneAt,
    sortOrder = sortOrder,
)

fun Todo.toEntity(): TodoEntity = TodoEntity(
    id = id,
    title = title,
    note = note,
    isDone = isDone,
    dueEpochDay = dueDate?.toEpochDay(),
    parentId = parentId,
    createdAt = createdAt,
    doneAt = doneAt,
    sortOrder = sortOrder,
)

fun HistoryEntity.toDomain(): History = History(
    id = id,
    title = title,
    note = note,
    startDate = LocalDate.ofEpochDay(startEpochDay),
    isPinned = isPinned,
    sortOrder = sortOrder,
)

fun History.toEntity(): HistoryEntity = HistoryEntity(
    id = id,
    title = title,
    note = note,
    startEpochDay = startDate.toEpochDay(),
    isPinned = isPinned,
    sortOrder = sortOrder,
)

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    sortOrder = sortOrder,
    isPinned = isPinned,
    createdAt = createdAt,
)

fun Folder.toEntity(): FolderEntity = FolderEntity(
    id = id,
    name = name,
    sortOrder = sortOrder,
    isPinned = isPinned,
    createdAt = createdAt,
)
