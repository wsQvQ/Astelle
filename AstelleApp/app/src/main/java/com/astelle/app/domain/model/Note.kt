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
    /**
     * 所属分类；null = 未分类。
     *
     * 这个字段必须一路带到 [Note.toEntity]，否则每次保存都会把归属写丢 ——
     * 用户把笔记放进分类、随手编辑一下，它就掉出来了。
     */
    val folderId: String? = null,
) {
    val displayTitle: String
        get() = title.trim().ifBlank {
            content.trim().lineSequence().firstOrNull().orEmpty().take(24)
        }

    val preview: String
        get() = content.trim().lineSequence().firstOrNull().orEmpty()
}
