package com.astelle.app.ui.home

import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.model.NoteSummary

/**
 * 抽屉里的一组：一个分类 + 它名下的笔记。
 *
 * [folder] 为 null 表示「未分类」—— 也就是收件箱，新写的笔记先落这里。
 */
internal data class NoteGroup(
    val folder: Folder?,
    val notes: List<NoteSummary>,
) {
    /** 折叠状态的键。分类 id 是 UUID，不会撞上收件箱这个哨兵值 */
    val key: String get() = folder?.id ?: INBOX_KEY

    val name: String get() = folder?.name ?: "未分类"
}

internal const val INBOX_KEY = "\u0000inbox"

/**
 * 把扁平的笔记列表按分类切成组。
 *
 * 四条规则，每条都有理由：
 *
 * 1. **收件箱排最前**。它是新笔记的落点，也是「还有什么没整理」的入口；
 *    排在末尾就等于藏起来了。
 * 2. **指向不存在分类的笔记也归入收件箱**。删分类时 [FolderRepository] 会把
 *    笔记 detach 干净，所以这是兜底 —— 万一数据被外部改坏，那篇笔记至少还能
 *    看见，而不是从界面上凭空消失。
 * 3. **空分类默认照样出现**（计数 0）。用户刚建完分类却看不到它，会以为没建成。
 *    [hideEmpty] 为 true 时相反：筛选态下空组只是噪音。
 * 4. **空收件箱始终不出现**。它不需要被「展示」，它是默认值。
 */
internal fun groupNotes(
    notes: List<NoteSummary>,
    folders: List<Folder>,
    hideEmpty: Boolean = false,
): List<NoteGroup> {
    val known = folders.mapTo(HashSet()) { it.id }
    val byFolder = notes.groupBy { it.folderId?.takeIf(known::contains) }

    return buildList {
        val inbox = byFolder[null].orEmpty()
        if (inbox.isNotEmpty()) add(NoteGroup(folder = null, notes = inbox))
        folders.forEach { folder ->
            val owned = byFolder[folder.id].orEmpty()
            if (owned.isNotEmpty() || !hideEmpty) add(NoteGroup(folder, owned))
        }
    }
}

/**
 * 抽屉列表要画的一行。
 *
 * LazyColumn 要一串扁平的行，而分组是树状的 —— 这个 sealed 就是那层压平。
 */
internal sealed interface DrawerRow {
    val key: String

    /** 一个分类容器：组头 + 组内条目，两张共用一条圆角边框（见 `FolderCard`） */
    data class FolderGroup(val group: NoteGroup, val collapsed: Boolean) : DrawerRow {
        override val key: String get() = "g:${group.key}"
    }

    /** 平铺的卡片：搜索态，以及「未分类」收件箱 */
    data class FlatNote(val summary: NoteSummary) : DrawerRow {
        override val key: String get() = "n:${summary.id}"
    }
}

/**
 * 把分组摊平成列表要的行。
 *
 * 只有**真正的分类**才包成容器；「未分类」保持平铺卡片 —— 它是收件箱，
 * 不是你自己建的分组，给它套一个文件夹壳反而像多了一层组织。
 *
 * [flat] 为 true（搜索态）时整个不分段，就是一张平铺的匹配结果。
 */
internal fun buildRows(
    groups: List<NoteGroup>,
    collapsed: Set<String>,
    flat: Boolean = false,
): List<DrawerRow> = buildList {
    groups.forEach { group ->
        if (flat || group.folder == null) {
            group.notes.forEach { add(DrawerRow.FlatNote(it)) }
        } else {
            add(DrawerRow.FolderGroup(group, collapsed = group.key in collapsed))
        }
    }
}
