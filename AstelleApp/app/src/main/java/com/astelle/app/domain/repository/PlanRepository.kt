package com.astelle.app.domain.repository

import com.astelle.app.domain.model.PlanDay
import com.astelle.app.domain.model.Todo
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun observeDays(): Flow<List<PlanDay>>
    fun observeDay(id: String): Flow<PlanDay?>
    suspend fun upsertDay(day: PlanDay)
    suspend fun deleteDay(id: String)

    fun observeTodos(): Flow<List<Todo>>
    fun observeTodosForDay(planDayId: String): Flow<List<Todo>>
    fun observeTodosDueOn(date: LocalDate): Flow<List<Todo>>
    suspend fun upsertTodo(todo: Todo)
    suspend fun deleteTodo(id: String)
    suspend fun setTodoDone(id: String, done: Boolean)
}
