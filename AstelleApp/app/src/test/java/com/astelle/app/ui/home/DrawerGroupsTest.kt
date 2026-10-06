package com.astelle.app.ui.home

import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.model.NoteSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 抽屉分组是纯逻辑，不该埋在 Composable 里靠肉眼验证。
 * 这里把「哪篇进哪组、组按什么顺序、折叠后还剩什么」全部钉死。
 */
class DrawerGroupsTest {

    private fun note(id: String, folderId: String? = null) = NoteSummary(
        id = id,
        title = id,
        createdAt = 0L,
        updatedAt = 0L,
        folderId = folderId,
    )

    private fun folder(id: String, name: String = id, sortOrder: Int = 0) =
        Folder(id = id, name = name, sortOrder = sortOrder, createdAt = 0L)

    @Test
    fun `没有分类时只有收件箱一组`() {
        val groups = groupNotes(listOf(note("a"), note("b")), emptyList())

        assertEquals(1, groups.size)
        assertNull("收件箱的 folder 必须是 null", groups[0].folder)
        assertEquals("未分类", groups[0].name)
        assertEquals(INBOX_KEY, groups[0].key)
        assertEquals(listOf("a", "b"), groups[0].notes.map { it.id })
    }

    @Test
    fun `收件箱排在最前`() {
        val groups = groupNotes(
            listOf(note("a"), note("b", "f1")),
            listOf(folder("f1", "工作")),
        )

        assertNull("第一组应是收件箱", groups[0].folder)
        assertEquals("f1", groups[1].folder?.id)
    }

    @Test
    fun `指向不存在分类的笔记归入收件箱`() {
        // 删分类会 detach 干净，所以这是数据被外部改坏时的兜底：
        // 宁可让它出现在未分类里，也不能从界面上凭空消失
        val groups = groupNotes(listOf(note("a", "ghost")), listOf(folder("f1", "工作")))

        assertEquals("收件箱 + 空的 f1", 2, groups.size)
        assertNull(groups[0].folder)
        assertEquals(listOf("a"), groups[0].notes.map { it.id })
        assertTrue(
            "不该凭空多出一个幽灵分类",
            groups.none { it.folder?.id == "ghost" },
        )
    }

    @Test
    fun `空分类默认出现，hideEmpty 时不出现`() {
        val folders = listOf(folder("f1", "工作"), folder("f2", "生活"))

        val shown = groupNotes(listOf(note("a")), folders)
        assertEquals("刚建的空分类得看得见，否则像没建成", 3, shown.size)

        val hidden = groupNotes(listOf(note("a")), folders, hideEmpty = true)
        assertEquals("筛选态下空组只是噪音", 1, hidden.size)
        assertNull(hidden[0].folder)
    }

    @Test
    fun `空收件箱始终不出现`() {
        val groups = groupNotes(listOf(note("a", "f1")), listOf(folder("f1", "工作")))

        assertEquals(1, groups.size)
        assertEquals("f1", groups[0].folder?.id)
    }

    @Test
    fun `一篇文章都没有时分组为空`() {
        assertTrue(groupNotes(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `折叠的组只吐组头，笔记不进列表`() {
        val folders = listOf(folder("f1", "工作"), folder("f2", "生活"))
        val groups = groupNotes(
            listOf(note("a"), note("b", "f1"), note("c", "f2")),
            folders,
        )

        val expanded = buildRows(groups, collapsed = emptySet())
        assertEquals(6, expanded.size) // 收件箱头+a + f1头+b + f2头+c

        val collapsed = buildRows(groups, collapsed = setOf("f1"))
        assertEquals(5, collapsed.size)
        assertTrue(
            "f1 里的 b 不该出现",
            collapsed.none { it is DrawerRow.Note && it.summary.id == "b" },
        )
        assertTrue(
            "但 f2 的 c 还得在",
            collapsed.any { it is DrawerRow.Note && it.summary.id == "c" },
        )
    }

    @Test
    fun `分类下的笔记标为 nested，收件箱的不标`() {
        val groups = groupNotes(
            listOf(note("a"), note("b", "f1")),
            listOf(folder("f1", "工作")),
        )
        val rows = buildRows(groups, emptySet())

        fun nestedOf(id: String) =
            rows.filterIsInstance<DrawerRow.Note>().first { it.summary.id == id }.nested

        assertTrue("收件箱是平铺的", !nestedOf("a"))
        assertTrue("分类下的要缩进一档", nestedOf("b"))
    }

    @Test
    fun `每行的 key 唯一且稳定`() {
        val groups = groupNotes(
            listOf(note("a"), note("b", "f1")),
            listOf(folder("f1", "工作"), folder("f2", "生活")),
        )
        val keys = buildRows(groups, emptySet()).map { it.key }

        assertEquals("key 撞车会让 LazyColumn 直接崩", keys.size, keys.toSet().size)
        // 空分类也要有 key
        assertTrue(keys.contains("h:f2"))
        assertTrue(keys.contains("h:$INBOX_KEY"))
        assertTrue(keys.contains("n:b"))
    }
}
