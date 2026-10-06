package com.astelle.app.ui.home

import com.astelle.app.domain.model.Note

enum class NoteFilter { All, Pinned, Favorite }

data class HomeUiState(
    val currentNoteId: String? = null,
    val title: String = "",
    val content: String = "",
    val savedAt: Long? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val filter: NoteFilter = NoteFilter.All,
    val mode: EditorMode = EditorMode.Edit,
    /** 待二次确认删除的笔记 id */
    val pendingDeleteId: String? = null,
) {
    val charCount: Int get() = content.length
}

enum class EditorMode { Edit, Preview }

sealed interface HomeUiEvent {
    data class TitleChanged(val title: String) : HomeUiEvent
    data class ContentChanged(val content: String) : HomeUiEvent
    data class SearchChanged(val query: String) : HomeUiEvent
    data class SetFilter(val filter: NoteFilter) : HomeUiEvent
    data object SaveNow : HomeUiEvent
    data object NewNote : HomeUiEvent
    data class OpenNote(val id: String) : HomeUiEvent
    data class RequestDelete(val id: String) : HomeUiEvent
    data object ConfirmDelete : HomeUiEvent
    data object CancelDelete : HomeUiEvent
    data class TogglePin(val id: String) : HomeUiEvent
    data class ToggleFavorite(val id: String) : HomeUiEvent
    data class SetMode(val mode: EditorMode) : HomeUiEvent
}
