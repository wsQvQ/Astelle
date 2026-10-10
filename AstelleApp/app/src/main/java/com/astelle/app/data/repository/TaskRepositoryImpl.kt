package com.astelle.app.data.repository

import com.astelle.app.data.local.dao.HistoryDao
import com.astelle.app.data.local.dao.TodoDao
import com.astelle.app.data.local.toDomain
import com.astelle.app.data.local.toEntity
import com.astelle.app.domain.model.History
import com.astelle.app.domain.model.Todo
import com.astelle.app.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao,
    private val historyDao: HistoryDao,
) : TaskRepository {

    override fun observeTodos(): Flow<List<Todo>> =
        todoDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeHistories(): Flow<List<History>> =
        historyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertTodo(todo: Todo) {
        todoDao.upsert(todo.toEntity())
    }

    override suspend fun deleteTodo(id: String) {
        // 删母项连子项（业务口径见 TaskLogic / 14 号计划）
        todoDao.deleteChildren(id)
        todoDao.deleteById(id)
    }

    override suspend fun setTodoDone(id: String, done: Boolean) {
        todoDao.setDone(id, done, if (done) System.currentTimeMillis() else null)
    }

    override suspend fun upsertHistory(history: History) {
        historyDao.upsert(history.toEntity())
    }

    override suspend fun setHistoryPinned(id: String, pinned: Boolean) {
        historyDao.setPinned(id, pinned)
    }

    override suspend fun deleteHistory(id: String) {
        historyDao.deleteById(id)
    }
}
