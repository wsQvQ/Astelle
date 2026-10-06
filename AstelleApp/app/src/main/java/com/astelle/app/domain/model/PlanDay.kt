package com.astelle.app.domain.model

import java.time.LocalDate

/**
 * 日子：倒数或正数的一天。纪念并入计划，不做独立模块。
 */
data class PlanDay(
    val id: String,
    val name: String,
    val date: LocalDate,
    val isYearly: Boolean = false,
    val intention: String? = null,
    val createdAt: Long = 0L,
) {
    /** 距离今天的天数：未来为正，今天为 0，过去为负。 */
    fun daysFrom(today: LocalDate): Long = date.toEpochDay() - today.toEpochDay()

    fun isPast(today: LocalDate): Boolean = date.isBefore(today)

    fun isFuture(today: LocalDate): Boolean = date.isAfter(today)
}
