package com.astelle.app.ui.home

import com.astelle.app.data.importer.ImportedNote

enum class NoteFilter { All, Pinned, Favorite }

data class HomeUiState(
    val currentNoteId: String? = null,
    val title: String = "",
    val content: String = "",
    val savedAt: Long? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    /** 当前笔记的开关态。`⋯` 菜单据此显示「置顶 / 取消置顶」而不是死文案 */
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    /** 编辑历史里是否还有更早的快照可回退 */
    val canUndo: Boolean = false,
    /** 回退之后是否还能再前进 */
    val canRedo: Boolean = false,
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
    data object Undo : HomeUiEvent
    data object Redo : HomeUiEvent
    data object NewNote : HomeUiEvent
    data class OpenNote(val id: String) : HomeUiEvent
    /** 从 .md 文件导入：解析在 UI 层完成，这里只接收纯数据，ViewModel 不碰 Android */
    data class ImportNote(val note: ImportedNote) : HomeUiEvent
    data class RequestDelete(val id: String) : HomeUiEvent
    data object ConfirmDelete : HomeUiEvent
    data object CancelDelete : HomeUiEvent
    data class TogglePin(val id: String) : HomeUiEvent
    data class ToggleFavorite(val id: String) : HomeUiEvent
    data class SetMode(val mode: EditorMode) : HomeUiEvent

    /** 在某个分类下新建一篇空白笔记，直接进编辑器 */
    data class NewNoteInFolder(val folderId: String) : HomeUiEvent

    /* ---------- 分类 ---------- */

    data class AddFolder(val name: String) : HomeUiEvent
    data class RenameFolder(val id: String, val name: String) : HomeUiEvent
    data class DeleteFolder(val id: String) : HomeUiEvent

    /** 文件夹置顶切换（④）：不碰 updatedAt */
    data class ToggleFolderPin(val id: String) : HomeUiEvent

    /** [folderId] 传 null = 移回「未分类」 */
    data class MoveNoteToFolder(val noteId: String, val folderId: String?) : HomeUiEvent
}
