package com.astelle.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记分类（文件夹）。
 *
 * 笔记通过 [NoteEntity.folderId] 挂在分类下；folderId 为 null 表示「未分类」。
 * 刻意不做多级嵌套 —— 设计稿的定位是「随手收下，慢慢整理」，
 * 层级一深就变成文件管理器了。
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
)
