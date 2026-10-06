package com.astelle.app.ui.home

/**
 * 编辑器的一次快照：标题 + 正文。撤销 / 重做以它为最小单位。
 */
data class EditSnapshot(
    val title: String = "",
    val content: String = "",
)

private const val DEFAULT_MAX_ENTRIES = 100

/**
 * 编辑历史栈。纯逻辑、零 Android 依赖，因此可以直接用 JVM 单元测试覆盖。
 *
 * 模型：线性栈 + 游标。游标指向「当前所处的快照」。
 *
 * - [record] 把新快照追加到游标之后，并丢弃原有的 redo 分支（在历史中间继续编辑）
 * - [undo] / [redo] 只移动游标并返回目标快照，不改动栈本身
 * - 与游标处相同的快照会被忽略。这一条很关键：撤销/重做会把快照写回 UI，
 *   而 UI 的回流又会再走一遍记录流程，不挡住的话历史立刻被污染
 * - 超过 [maxEntries] 时丢弃最旧的一条，避免长时间编辑把内存撑爆
 */
class EditHistory(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    private val entries = ArrayList<EditSnapshot>(maxEntries)
    private var cursor = -1

    /** 是否还有更早的快照可回退 */
    val canUndo: Boolean get() = cursor > 0

    /** 回退之后是否还能再前进 */
    val canRedo: Boolean get() = cursor in 0 until entries.lastIndex

    /** 切换 / 新建笔记时调用：整条历史作废，以给定快照作为新的原点 */
    fun reset(snapshot: EditSnapshot) {
        entries.clear()
        entries.add(snapshot)
        cursor = 0
    }

    /** 记录一次「编辑停稳」之后的状态 */
    fun record(snapshot: EditSnapshot) {
        if (cursor >= 0 && entries[cursor] == snapshot) return

        // 站在历史中间继续编辑：后面的 redo 分支已经失效
        if (cursor < entries.lastIndex) {
            entries.subList(cursor + 1, entries.size).clear()
        }

        entries.add(snapshot)
        if (entries.size > maxEntries) {
            entries.removeAt(0)
        }
        cursor = entries.lastIndex
    }

    /** 返回上一个快照；已经在最早处则返回 null */
    fun undo(): EditSnapshot? {
        if (!canUndo) return null
        cursor--
        return entries[cursor]
    }

    /** 返回下一个快照；已经在最新处则返回 null */
    fun redo(): EditSnapshot? {
        if (!canRedo) return null
        cursor++
        return entries[cursor]
    }
}
