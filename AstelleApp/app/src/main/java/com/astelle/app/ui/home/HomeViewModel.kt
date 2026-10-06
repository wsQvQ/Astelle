package com.astelle.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astelle.app.BuildConfig
import com.astelle.app.data.seed.SampleNote
import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.model.Note
import com.astelle.app.domain.model.NoteSummary
import com.astelle.app.domain.repository.FolderRepository
import com.astelle.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首屏编辑器：双层顶栏 + 标题 + 正文。变更去抖后自动落库。
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * 抽屉列表。搜索已下推给 SQL：换关键词时重新订阅一个新的查询流，
     * 因此这里拿到的是轻量摘要（不含正文全文），筛选仍在内存里做。
     */
    val filteredNotes: StateFlow<List<NoteSummary>> = combine(
        _uiState.map { it.searchQuery }.distinctUntilChanged()
            .flatMapLatest { noteRepository.observeSummaries(it) },
        _uiState.map { it.filter }.distinctUntilChanged(),
    ) { notes, filter ->
        when (filter) {
            NoteFilter.All -> notes
            NoteFilter.Pinned -> notes.filter { it.isPinned }
            NoteFilter.Favorite -> notes.filter { it.isFavorite }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 全部分类。分组渲染在 UI 层做（纯函数，见 DrawerGroups.kt） */
    val folders: StateFlow<List<Folder>> = folderRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /**
     * 把一篇笔记装进编辑器：绑定当前笔记、重置历史、清干净保存态。
     * 切换笔记与导入 .md 都走这里，避免同一段状态拼装写两遍。
     */
    private fun openInEditor(note: Note, savedAt: Long) {
        activeNote = note
        history.reset(EditSnapshot(note.title, note.content))
        _uiState.update {
            it.copy(
                currentNoteId = note.id,
                title = note.title,
                content = note.content,
                savedAt = savedAt,
                isDirty = false,
                isSaving = false,
                canUndo = false,
                canRedo = false,
            )
        }
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
                    openInEditor(note, savedAt = note.updatedAt)
                }
            }
            is HomeUiEvent.ImportNote -> {
                saveJob?.cancel()
                persist(_uiState.value.title, _uiState.value.content)
                viewModelScope.launch {
                    val now = System.currentTimeMillis()
                    val note = Note(
                        id = UUID.randomUUID().toString(),
                        title = event.note.title,
                        content = event.note.content,
                        createdAt = now,
                        updatedAt = now,
                    )
                    noteRepository.upsert(note)
                    openInEditor(note, savedAt = now)
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
                        // 历史必须跟着清空：否则在清空的编辑器上按一下撤销，
                        // 会把刚删掉的笔记内容「复活」成一篇新笔记
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
                }
            }
            is HomeUiEvent.TogglePin -> {
                viewModelScope.launch {
                    noteRepository.togglePinned(event.id)
                    syncActiveNoteFlag(event.id) { it.copy(isPinned = !it.isPinned) }
                }
            }
            is HomeUiEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    noteRepository.toggleFavorite(event.id)
                    syncActiveNoteFlag(event.id) { it.copy(isFavorite = !it.isFavorite) }
                }
            }
            is HomeUiEvent.SetMode ->
                _uiState.update { it.copy(mode = event.mode) }

            /* ---------- 分类 ---------- */

            is HomeUiEvent.AddFolder -> {
                val name = event.name.trim()
                if (name.isEmpty()) return
                viewModelScope.launch {
                    folderRepository.upsert(
                        Folder(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            // 由 SQL 取 max+1：抽屉关着时 folders 流无人订阅，
                            // 在内存里取 max 会让连点两次新建拿到同一个排序
                            sortOrder = folderRepository.nextSortOrder(),
                            createdAt = System.currentTimeMillis(),
                        )
                    )
                }
            }

            is HomeUiEvent.RenameFolder -> {
                val name = event.name.trim()
                // 改名成空 = 把分类变成无名条目，界面上一行空白，不如不响应
                if (name.isEmpty()) return
                viewModelScope.launch { folderRepository.rename(event.id, name) }
            }

            is HomeUiEvent.DeleteFolder -> viewModelScope.launch {
                folderRepository.delete(event.id)
                // 关键：库里 detachNotes 只改了数据库，内存里的 activeNote
                // 还指着那个已删除的分类。不同步的话，下一次自动保存整行回写，
                // 又会把这个悬空 folderId 写回去 —— 分类没了，笔记也再也找不到
                val current = activeNote
                if (current != null && current.folderId == event.id) {
                    activeNote = current.copy(folderId = null)
                }
            }

            is HomeUiEvent.MoveNoteToFolder -> viewModelScope.launch {
                noteRepository.moveToFolder(event.noteId, event.folderId)
                // 同上：不跟着改内存副本，下次 persist 会把 folderId 抹回去
                val current = activeNote
                if (current != null && current.id == event.noteId) {
                    activeNote = current.copy(folderId = event.folderId)
                }
            }
        }
    }

    /**
     * 翻转标记之后同步内存里的 activeNote。
     *
     * 不同步的话，下一次 persist 会拿旧的 isPinned / isFavorite 整行回写，
     * 把用户刚刚做的切换悄悄覆盖掉 —— 而且不会有任何报错。
     */
    private fun syncActiveNoteFlag(id: String, transform: (Note) -> Note) {
        val current = activeNote ?: return
        if (current.id == id) activeNote = transform(current)
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
                    val trimmed = title.trim()
                    // 内容没变就别写库。
                    // persist 会被「打开笔记」触发的防抖流水线再调一次 ——
                    // 若无条件 upsert，updatedAt 会被刷新，笔记平白跳到抽屉最前，
                    // 用户只是看了一眼却像改过一样。
                    if (existing.title == trimmed && existing.content == content) {
                        _uiState.update {
                            it.copy(savedAt = existing.updatedAt, isSaving = false, isDirty = false)
                        }
                        return@launch
                    }
                    val updated = existing.copy(
                        title = trimmed,
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
