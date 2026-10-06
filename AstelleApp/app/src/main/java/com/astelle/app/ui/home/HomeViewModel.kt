package com.astelle.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astelle.app.BuildConfig
import com.astelle.app.data.seed.SampleNote
import com.astelle.app.domain.model.Note
import com.astelle.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首屏编辑器：双层顶栏 + 标题 + 正文。变更去抖后自动落库。
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val filteredNotes: StateFlow<List<Note>> = combine(
        noteRepository.observeNotes(),
        _uiState.map { it.searchQuery }.distinctUntilChanged(),
        _uiState.map { it.filter }.distinctUntilChanged(),
    ) { notes, query, filter ->
        notes
            .asSequence()
            .filter { note ->
                when (filter) {
                    NoteFilter.All -> true
                    NoteFilter.Pinned -> note.isPinned
                    NoteFilter.Favorite -> note.isFavorite
                }
            }
            .filter { note ->
                query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var activeNote: Note? = null
    private var saveJob: Job? = null

    /** 撤销 / 重做。纯内存，不落库；切换笔记时整条重置 */
    private val history = EditHistory()

    init {
        seedSampleNote()
        _uiState
            .map { EditSnapshot(it.title, it.content) }
            .distinctUntilChanged()
            .debounce(SAVE_DEBOUNCE_MS)
            .onEach { snapshot ->
                persist(snapshot.title, snapshot.content)
                recordHistory(snapshot)
            }
            .launchIn(viewModelScope)
    }

    /**
     * 测试期便利功能：把示例笔记刷新成当前构建里的最新内容。
     *
     * 固定 ID + upsert，所以不会越塞越多；只在 debug 构建里跑，release 包不会有这篇。
     * 直接写库而不经过 _uiState，因此不会污染用户正在编辑的那份空白草稿。
     */
    private fun seedSampleNote() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            val existing = noteRepository.getNote(SampleNote.ID)
            val now = System.currentTimeMillis()
            noteRepository.upsert(
                Note(
                    id = SampleNote.ID,
                    title = SampleNote.TITLE,
                    content = SampleNote.CONTENT,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            )
        }
    }

    /** 撤销 / 重做把快照写回界面。历史游标已由 EditHistory 内部移动完毕 */
    private fun applySnapshot(snapshot: EditSnapshot) {
        _uiState.update {
            it.copy(
                title = snapshot.title,
                content = snapshot.content,
                // 与手动输入一致：先置为「保存中」，让 500ms 后的去抖落库把状态收干净
                isDirty = true,
                isSaving = true,
                canUndo = history.canUndo,
                canRedo = history.canRedo,
            )
        }
    }

    /** 编辑停稳后记一笔历史；与游标相同的快照会被 EditHistory 自行忽略 */
    private fun recordHistory(snapshot: EditSnapshot) {
        history.record(snapshot)
        _uiState.update { it.copy(canUndo = history.canUndo, canRedo = history.canRedo) }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.title, isDirty = true, isSaving = true) }
            is HomeUiEvent.ContentChanged ->
                _uiState.update { it.copy(content = event.content, isDirty = true, isSaving = true) }
            is HomeUiEvent.SearchChanged ->
                _uiState.update { it.copy(searchQuery = event.query) }
            is HomeUiEvent.SetFilter ->
                _uiState.update { it.copy(filter = event.filter) }
            HomeUiEvent.SaveNow -> {
                saveJob?.cancel()
                persist(_uiState.value.title, _uiState.value.content)
            }
            HomeUiEvent.Undo -> history.undo()?.let(::applySnapshot)
            HomeUiEvent.Redo -> history.redo()?.let(::applySnapshot)
            HomeUiEvent.NewNote -> {
                saveJob?.cancel()
                persist(_uiState.value.title, _uiState.value.content)
                activeNote = null
                history.reset(EditSnapshot())
                _uiState.update {
                    it.copy(
                        currentNoteId = null,
                        title = "",
                        content = "",
                        savedAt = null,
                        isDirty = false,
                        isSaving = false,
                        canUndo = false,
                        canRedo = false,
                    )
                }
            }
            is HomeUiEvent.OpenNote -> {
                saveJob?.cancel()
                persist(_uiState.value.title, _uiState.value.content)
                viewModelScope.launch {
                    val note = noteRepository.getNote(event.id) ?: return@launch
                    activeNote = note
                    // 换了一篇笔记，旧的历史对它没有意义
                    history.reset(EditSnapshot(note.title, note.content))
                    _uiState.update {
                        it.copy(
                            currentNoteId = note.id,
                            title = note.title,
                            content = note.content,
                            savedAt = note.updatedAt,
                            isDirty = false,
                            isSaving = false,
                            canUndo = false,
                            canRedo = false,
                        )
                    }
                }
            }
            is HomeUiEvent.RequestDelete ->
                _uiState.update { it.copy(pendingDeleteId = event.id) }
            HomeUiEvent.CancelDelete ->
                _uiState.update { it.copy(pendingDeleteId = null) }
            HomeUiEvent.ConfirmDelete -> {
                val id = _uiState.value.pendingDeleteId ?: return
                _uiState.update { it.copy(pendingDeleteId = null) }
                viewModelScope.launch {
                    noteRepository.delete(id)
                    if (_uiState.value.currentNoteId == id) {
                        activeNote = null
                        _uiState.update {
                            it.copy(
                                currentNoteId = null,
                                title = "",
                                content = "",
                                savedAt = null,
                                isDirty = false,
                            )
                        }
                    }
                }
            }
            is HomeUiEvent.TogglePin -> {
                viewModelScope.launch {
                    val note = noteRepository.getNote(event.id) ?: return@launch
                    noteRepository.setPinned(event.id, !note.isPinned)
                }
            }
            is HomeUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    val note = noteRepository.getNote(event.id) ?: return@launch
                    noteRepository.setFavorite(event.id, !note.isFavorite)
                }
            }
            is HomeUiEvent.SetMode ->
                _uiState.update { it.copy(mode = event.mode) }
        }
    }

    private fun persist(title: String, content: String) {
        val blank = title.isBlank() && content.isBlank()
        val currentId = _uiState.value.currentNoteId
        if (blank && currentId == null) {
            _uiState.update { it.copy(isSaving = false, isDirty = false) }
            return
        }
        saveJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = activeNote
            if (existing != null) {
                if (blank) {
                    noteRepository.delete(existing.id)
                    activeNote = null
                    _uiState.update {
                        it.copy(currentNoteId = null, savedAt = null, isSaving = false, isDirty = false)
                    }
                } else {
                    val updated = existing.copy(
                        title = title.trim(),
                        content = content,
                        updatedAt = now,
                    )
                    noteRepository.upsert(updated)
                    activeNote = updated
                    _uiState.update {
                        it.copy(savedAt = now, isSaving = false, isDirty = false)
                    }
                }
            } else {
                if (blank) {
                    _uiState.update { it.copy(isSaving = false, isDirty = false) }
                    return@launch
                }
                val note = Note(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    content = content,
                    createdAt = now,
                    updatedAt = now,
                )
                noteRepository.upsert(note)
                activeNote = note
                _uiState.update {
                    it.copy(currentNoteId = note.id, savedAt = now, isSaving = false, isDirty = false)
                }
            }
        }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 500L
    }
}
