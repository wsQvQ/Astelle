package com.astelle.app.ui.plans

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astelle.app.domain.logic.TaskLogic
import com.astelle.app.domain.model.History
import com.astelle.app.domain.model.Todo
import com.astelle.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 底部胶囊所在区（10-11：左右分屏定稿） */
enum class TaskPane { TODO, HISTORY }

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    val todos: StateFlow<List<Todo>> = repo.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val histories: StateFlow<List<History>> = repo.observeHistories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 胶囊所在区 + 待办筛选（配置变更都活着） */
    var pane by mutableStateOf(TaskPane.TODO)
    var filter by mutableStateOf(TaskLogic.Filter.ALL)

    // ── 待办 ──

    fun toggleTodo(id: String, done: Boolean) {
        val changes = TaskLogic.toggleCascade(todos.value, id, done)
        viewModelScope.launch {
            changes.forEach { (tid, d) -> repo.setTodoDone(tid, d) }
        }
    }

    fun addTodo(title: String, dueDate: LocalDate?, parentId: String?) {
        viewModelScope.launch {
            repo.upsertTodo(
                Todo(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    dueDate = dueDate,
                    parentId = parentId,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun editTodo(id: String, title: String, dueDate: LocalDate?) {
        val old = todos.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repo.upsertTodo(old.copy(title = title.trim(), dueDate = dueDate))
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch { repo.deleteTodo(id) }
    }

    // ── 历史 ──

    fun addHistory(title: String, startDate: LocalDate) {
        viewModelScope.launch {
            repo.upsertHistory(
                History(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    startDate = startDate,
                )
            )
        }
    }

    fun editHistory(id: String, title: String, startDate: LocalDate) {
        val old = histories.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repo.upsertHistory(old.copy(title = title.trim(), startDate = startDate))
        }
    }

    fun togglePinHistory(id: String, pinned: Boolean) {
        viewModelScope.launch { repo.setHistoryPinned(id, pinned) }
    }

    fun deleteHistory(id: String) {
        viewModelScope.launch { repo.deleteHistory(id) }
    }
}
