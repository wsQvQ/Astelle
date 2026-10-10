package com.astelle.app.domain.logic

import com.astelle.app.domain.model.Todo
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLogicTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 11)

    private fun todo(
        id: String,
        title: String = id,
        done: Boolean = false,
        due: LocalDate? = null,
        parent: String? = null,
        sort: Int = 0,
    ) = Todo(
        id = id,
        title = title,
        isDone = done,
        dueDate = due,
        parentId = parent,
        createdAt = sort.toLong(),
        sortOrder = sort,
    )

    // ── 天数口径（14 号计划 §2：当天=0，过期=负） ──

    @Test fun `daysUntil 当天为零`() {
        assertEquals(0, TaskLogic.daysUntil(today, today))
    }

    @Test fun `daysUntil 未来为正 过去为负`() {
        assertEquals(3, TaskLogic.daysUntil(today, today.plusDays(3)))
        assertEquals(-2, TaskLogic.daysUntil(today, today.minusDays(2)))
    }

    @Test fun `daysSince 从起始到今天`() {
        assertEquals(0, TaskLogic.daysSince(today, today))
        assertEquals(10, TaskLogic.daysSince(today, today.minusDays(10)))
    }

    @Test fun `isOverdue 与 isDueToday 口径`() {
        assertTrue(TaskLogic.isOverdue(today, today.minusDays(1)))
        assertFalse(TaskLogic.isOverdue(today, today))
        assertFalse(TaskLogic.isOverdue(today, null))
        assertTrue(TaskLogic.isDueToday(today, today))
        assertFalse(TaskLogic.isDueToday(today, null))
    }

    @Test fun `daysToNextAnniversary 跨年滚动`() {
        val start = LocalDate.of(2023, 8, 14)
        // 今年生日还没到（10-11 之前是 8-14，已过）→ 明年 8-14
        assertEquals(
            TaskLogic.daysUntil(today, LocalDate.of(2027, 8, 14)),
            TaskLogic.daysToNextAnniversary(today, start),
        )
        // 今天就是周年
        assertEquals(0, TaskLogic.daysToNextAnniversary(start.withYear(today.year), start))
    }

    // ── 排序：过期 → 今天 → 未来 → 无截止沉底 ──

    @Test fun `sortParents 过期最前 无截止沉底`() {
        val none = todo("none")
        val future = todo("future", due = today.plusDays(5))
        val over = todo("over", due = today.minusDays(1))
        val todayDue = todo("today", due = today)
        val sorted = TaskLogic.sortParents(listOf(none, future, over, todayDue), today)
        assertEquals(listOf("over", "today", "future", "none"), sorted.map { it.id })
    }

    // ── 母子级联（14 号计划 §2.4） ──

    @Test fun `勾母项子项全跟`() {
        val p = todo("p")
        val c1 = todo("c1", parent = "p")
        val c2 = todo("c2", parent = "p", done = true)
        val changes = TaskLogic.toggleCascade(listOf(p, c1, c2), "p", true)
        // c2 已经是 done，不重复写
        assertEquals(setOf("p" to true, "c1" to true), changes.toSet())
    }

    @Test fun `子项全勾母项自动勾`() {
        val p = todo("p")
        val c1 = todo("c1", parent = "p")
        val c2 = todo("c2", parent = "p", done = true)
        val changes = TaskLogic.toggleCascade(listOf(p, c1, c2), "c1", true)
        assertEquals(setOf("c1" to true, "p" to true), changes.toSet())
    }

    @Test fun `子项没全勾母项须取消勾`() {
        val p = todo("p", done = true)
        val c1 = todo("c1", parent = "p", done = true)
        val c2 = todo("c2", parent = "p", done = true)
        val changes = TaskLogic.toggleCascade(listOf(p, c1, c2), "c1", false)
        assertEquals(setOf("c1" to false, "p" to false), changes.toSet())
    }

    @Test fun `parentProgress 无子项为 null`() {
        assertNull(TaskLogic.parentProgress(listOf(todo("solo")), "solo"))
        assertEquals(
            1 to 2,
            TaskLogic.parentProgress(
                listOf(todo("p"), todo("c1", parent = "p", done = true), todo("c2", parent = "p")),
                "p",
            ),
        )
    }

    // ── flatten：母项后紧跟子项 ──

    @Test fun `flatten 母项带子项按序展开`() {
        val p2 = todo("p2", due = today)
        val p1 = todo("p1", due = today.minusDays(1))
        val c2 = todo("c2", parent = "p1", sort = 2)
        val c1 = todo("c1", parent = "p1", sort = 1)
        val flat = TaskLogic.flatten(listOf(p2, p1, c2, c1), today)
        assertEquals(listOf("p1", "c1", "c2", "p2"), flat.map { it.id })
    }

    // ── 筛选 ──

    @Test fun `filter 今天与过期`() {
        val t = todo("t", due = today)
        val o = todo("o", due = today.minusDays(2))
        val f = todo("f", due = today.plusDays(9))
        val n = todo("n")
        val all = listOf(t, o, f, n)
        assertEquals(1, all.count { TaskLogic.matchesFilter(it, today, TaskLogic.Filter.TODAY) })
        assertEquals(1, all.count { TaskLogic.matchesFilter(it, today, TaskLogic.Filter.OVERDUE) })
        assertEquals(4, all.count { TaskLogic.matchesFilter(it, today, TaskLogic.Filter.ALL) })
    }
}
