package com.astelle.app.domain.repository

import com.astelle.app.domain.model.History
import com.astelle.app.domain.model.Todo
import kotlinx.coroutines.flow.Flow

/**
 * 待办 / 历史仓储（14 号计划）。级联业务规则在 [com.astelle.app.domain.logic.TaskLogic]，
 * 仓储只管落库。
 */
interface TaskRepository {
    fun observeTodos(): Flow<List<Todo>>
    fun observeHistories(): Flow<List<History>>

    suspend fun upsertTodo(todo: Todo)
    suspend fun deleteTodo(id: String)
    suspend fun setTodoDone(id: String, done: Boolean)

    suspend fun upsertHistory(history: History)
    suspend fun setHistoryPinned(id: String, pinned: Boolean)
    suspend fun deleteHistory(id: String)
}
