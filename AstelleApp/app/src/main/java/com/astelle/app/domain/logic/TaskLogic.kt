package com.astelle.app.domain.logic

import com.astelle.app.domain.model.History
import com.astelle.app.domain.model.Todo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 待办/历史的业务规则（纯函数，单测主场 —— UI 只管画）。
 *
 * 铁律出处（14 号计划）：
 *  - 倒计时口径：≤3 天 砖红、当天=「今天」、过期=「已过 x 天」、无截止=「—」
 *  - 母子级联：子项全勾 ⇒ 母项自动勾；勾母项 ⇒ 子项全跟
 *  - 排序：过期→今天→按截止升序→无截止沉底；历史：置顶优先、天数大者在前
 */
object TaskLogic {

    // ── 天数 ──────────────────────────────────────────────

    /** 距截止还有多少天（负数 = 已过期天数）；当天 = 0 */
    fun daysUntil(today: LocalDate, due: LocalDate): Int =
        ChronoUnit.DAYS.between(today, due).toInt()

    /** 起始至今多少天（当天 = 0） */
    fun daysSince(today: LocalDate, start: LocalDate): Int =
        ChronoUnit.DAYS.between(start, today).toInt()

    fun isOverdue(today: LocalDate, due: LocalDate?): Boolean =
        due != null && due.isBefore(today)

    fun isDueToday(today: LocalDate, due: LocalDate?): Boolean = due == today

    /** 距下个周年还有多少天（0 = 今天就是周年） */
    fun daysToNextAnniversary(today: LocalDate, start: LocalDate): Int {
        var next = start.withYear(today.year)
        if (next.isBefore(today)) next = next.withYear(today.year + 1)
        return daysUntil(today, next)
    }

    // ── 过滤 / 排序 ────────────────────────────────────────

    enum class Filter { ALL, TODAY, OVERDUE }

    fun matchesFilter(todo: Todo, today: LocalDate, filter: Filter): Boolean = when (filter) {
        Filter.ALL -> true
        Filter.TODAY -> isDueToday(today, todo.dueDate)
        Filter.OVERDUE -> isOverdue(today, todo.dueDate)
    }

    /**
     * 待办排序（只排**母项**；子项挂在母项下按 sortOrder）：
     * 过期 → 今天 → 未来按日近者先 → 无截止沉底；同档 sortOrder/createdAt 定序。
     */
    fun sortParents(list: List<Todo>, today: LocalDate): List<Todo> =
        list.sortedWith(
            compareBy<Todo> { if (isOverdue(today, it.dueDate)) 0 else if (isDueToday(today, it.dueDate)) 1 else if (it.dueDate != null) 2 else 3 }
                .thenBy { it.dueDate ?: LocalDate.MAX }
                .thenBy { it.sortOrder }
                .thenBy { it.createdAt }
        )

    /** 历史排序：置顶 → 天数大者在前 → sortOrder */
    fun sortHistories(list: List<History>, today: LocalDate): List<History> =
        list.sortedWith(
            compareByDescending<History> { it.isPinned }
                .thenByDescending { daysSince(today, it.startDate) }
                .thenBy { it.sortOrder }
        )

    // ── 母子结构 ──────────────────────────────────────────

    /** 展平成「母项 + 其子项」的渲染序：无父的按 [sortParents]，各母项后紧跟其子项 */
    fun flatten(list: List<Todo>, today: LocalDate): List<Todo> {
        val parents = list.filter { it.parentId == null }.let { sortParents(it, today) }
        val byParent = list.filter { it.parentId != null }.groupBy { it.parentId }
        return parents.flatMap { p -> listOf(p) + (byParent[p.id].orEmpty().sortedBy { it.sortOrder }) }
    }

    /** 母项进度：已完成 / 总数（无子项返回 null） */
    fun parentProgress(list: List<Todo>, parentId: String): Pair<Int, Int>? {
        val children = list.filter { it.parentId == parentId }
        if (children.isEmpty()) return null
        return children.count { it.isDone } to children.size
    }

    /**
     * 勾选级联（返回需要落库的新状态）：
     *  - 勾母项 ⇒ 全部子项跟勾；取消母项 ⇒ 全部子项跟取消
     *  - 勾任一子项 ⇒ 若兄弟全勾，母项自动勾；未全勾则母项取消勾
     * 返回 (id → shouldDone) 的变更清单（不含状态本就一致的）。
     */
    fun toggleCascade(list: List<Todo>, id: String, done: Boolean): List<Pair<String, Boolean>> {
        val item = list.firstOrNull { it.id == id } ?: return emptyList()
        val changes = mutableListOf<Pair<String, Boolean>>()
        if (item.isDone != done) changes += item.id to done
        val children = list.filter { it.parentId == id }
        if (item.parentId == null && children.isNotEmpty()) {
            // 母项：子项全跟
            children.forEach { if (it.isDone != done) changes += it.id to done }
        } else if (item.parentId != null) {
            // 子项：看兄弟
            val siblings = list.filter { it.parentId == item.parentId }
            val allDone = siblings.all { if (it.id == id) done else it.isDone }
            val parent = list.firstOrNull { it.id == item.parentId }
            if (parent != null && parent.isDone != allDone) changes += parent.id to allDone
        }
        return changes
    }
}
