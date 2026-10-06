package com.astelle.app.data.repository

import com.astelle.app.data.local.dao.PlanDayDao
import com.astelle.app.data.local.dao.TodoDao
import com.astelle.app.data.local.toDomain
import com.astelle.app.data.local.toEntity
import com.astelle.app.domain.model.PlanDay
import com.astelle.app.domain.model.Todo
import com.astelle.app.domain.repository.PlanRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PlanRepositoryImpl @Inject constructor(
    private val planDayDao: PlanDayDao,
    private val todoDao: TodoDao,
) : PlanRepository {

    override fun observeDays(): Flow<List<PlanDay>> =
        planDayDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeDay(id: String): Flow<PlanDay?> =
        planDayDao.observeById(id).map { it?.toDomain() }

    override suspend fun upsertDay(day: PlanDay) {
        planDayDao.upsert(day.toEntity())
    }

    override suspend fun deleteDay(id: String) {
        planDayDao.deleteById(id)
    }

    override fun observeTodos(): Flow<List<Todo>> =
        todoDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTodosForDay(planDayId: String): Flow<List<Todo>> =
        todoDao.observeByPlanDay(planDayId).map { list -> list.map { it.toDomain() } }

    override fun observeTodosDueOn(date: LocalDate): Flow<List<Todo>> =
        todoDao.observeByDueDay(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertTodo(todo: Todo) {
        todoDao.upsert(todo.toEntity())
    }

    override suspend fun deleteTodo(id: String) {
        todoDao.deleteById(id)
    }

    override suspend fun setTodoDone(id: String, done: Boolean) {
        todoDao.setDone(id, done, doneAt = if (done) System.currentTimeMillis() else null)
    }
}
