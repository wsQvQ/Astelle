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

    /* ---------------- groupNotes ---------------- */

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

    /* ---------------- buildRows ---------------- */

    @Test
    fun `只有真正的分类才包成容器，未分类保持平铺卡片`() {
        val groups = groupNotes(
            listOf(note("a"), note("b", "f1")),
            listOf(folder("f1", "工作")),
        )
        val rows = buildRows(groups, collapsed = emptySet())

        assertTrue(
            "收件箱是收件箱，不是你自己建的分组，不该套一个文件夹壳",
            rows.any { it is DrawerRow.FlatNote && it.summary.id == "a" },
        )
        val container = rows.filterIsInstance<DrawerRow.FolderGroup>().single()
        assertEquals("f1", container.group.folder?.id)
        assertTrue(
            "分类里的笔记住进容器了，不该再单独占一行",
            rows.none { it is DrawerRow.FlatNote && it.summary.id == "b" },
        )
    }

    @Test
    fun `折叠的组仍然占一行，只是把 collapsed 传下去`() {
        val groups = groupNotes(
            listOf(note("b", "f1"), note("c", "f2")),
            listOf(folder("f1", "工作"), folder("f2", "生活")),
        )
        val rows = buildRows(groups, collapsed = setOf("f1"))
        val containers = rows.filterIsInstance<DrawerRow.FolderGroup>()

        assertTrue(
            "折叠态要传给容器去收起内容",
            containers.first { it.group.key == "f1" }.collapsed,
        )
        assertTrue(
            "没折叠的组不该被连累",
            !containers.first { it.group.key == "f2" }.collapsed,
        )
        // 笔记始终挂在容器上，收不收起由容器自己的动画决定
        assertEquals(1, containers.first { it.group.key == "f1" }.group.notes.size)
    }

    @Test
    fun `搜索态下全部压平，不分段`() {
        val groups = groupNotes(
            listOf(note("a"), note("b", "f1")),
            listOf(folder("f1", "工作")),
        )
        val rows = buildRows(groups, collapsed = emptySet(), flat = true)

        assertTrue("搜索是「我要那一篇」，不该再冒出分类容器", rows.none { it is DrawerRow.FolderGroup })
        assertEquals(
            listOf("a", "b"),
            rows.filterIsInstance<DrawerRow.FlatNote>().map { it.summary.id },
        )
    }

    @Test
    fun `每行的 key 唯一且稳定`() {
        val groups = groupNotes(
            listOf(note("a"), note("b", "f1")),
            listOf(folder("f1", "工作"), folder("f2", "生活")),
        )
        val keys = buildRows(groups, emptySet()).map { it.key }

        assertEquals("key 撞车会让 LazyColumn 直接崩", keys.size, keys.toSet().size)
        assertTrue("空分类也要有 key", keys.contains("g:f2"))
        assertTrue(keys.contains("g:f1"))
        assertTrue(keys.contains("n:a"))
    }
}
