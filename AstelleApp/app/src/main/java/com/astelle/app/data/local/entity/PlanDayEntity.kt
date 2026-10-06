package com.astelle.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_days")
data class PlanDayEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** epoch day，兼容 minSdk 26 与 Room 无 LocalDate 内建转换。 */
    val dateEpochDay: Long,
    val isYearly: Boolean = false,
    val intention: String? = null,
    val createdAt: Long = 0L,
)
