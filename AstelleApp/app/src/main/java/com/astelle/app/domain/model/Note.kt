package com.astelle.app.domain.model

/**
 * 随记：标题 + 正文。本地优先。
 */
data class Note(
    val id: String,
    val title: String = "",
    val content: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val mood: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
) {
    val displayTitle: String
        get() = title.trim().ifBlank {
            content.trim().lineSequence().firstOrNull().orEmpty().take(24)
        }

    val preview: String
        get() = content.trim().lineSequence().firstOrNull().orEmpty()
}
