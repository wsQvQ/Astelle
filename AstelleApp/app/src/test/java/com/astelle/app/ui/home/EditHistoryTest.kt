package com.astelle.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditHistoryTest {

    private val a = EditSnapshot("a", "1")
    private val b = EditSnapshot("b", "2")
    private val c = EditSnapshot("c", "3")

    @Test
    fun `刚 reset 之后既不能撤销也不能重做`() {
        val h = EditHistory()
        h.reset(a)
        assertFalse(h.canUndo)
        assertFalse(h.canRedo)
        assertNull(h.undo())
        assertNull(h.redo())
    }

    @Test
    fun `输入之后可以撤销回原点`() {
        val h = EditHistory()
        h.reset(a)
        h.record(b)
        assertTrue(h.canUndo)
        assertFalse(h.canRedo)
        assertEquals(a, h.undo())
        assertFalse(h.canUndo)
        assertTrue(h.canRedo)
    }

    @Test
    fun `撤销之后可以重做回来`() {
        val h = EditHistory()
        h.reset(a)
        h.record(b)
        assertEquals(a, h.undo())
        assertEquals(b, h.redo())
        assertTrue(h.canUndo)
        assertFalse(h.canRedo)
    }

    @Test
    fun `撤销之后再编辑会丢弃 redo 分支`() {
        val h = EditHistory()
        h.reset(a)
        h.record(b)
        h.undo()
        h.record(c)
        assertFalse(h.canRedo)
        assertEquals(a, h.undo())
        assertEquals(c, h.redo())
    }

    @Test
    fun `与当前游标相同的快照不会被重复记录`() {
        // 这条对应真实场景：撤销后状态回流 UI，会再触发一次记录
        val h = EditHistory()
        h.reset(a)
        h.record(a)
        h.record(a)
        assertFalse(h.canUndo)

        h.record(b)
        h.record(b)
        assertTrue(h.canUndo)
        assertEquals(a, h.undo())
        assertFalse(h.canUndo)
    }

    @Test
    fun `撤销到最早之后继续撤销返回 null`() {
        val h = EditHistory()
        h.reset(a)
        h.record(b)
        assertEquals(a, h.undo())
        assertNull(h.undo())
    }

    @Test
    fun `多步之间来回移动`() {
        val h = EditHistory()
        h.reset(a)
        h.record(b)
        h.record(c)
        assertEquals(b, h.undo())
        assertEquals(a, h.undo())
        assertNull(h.undo())
        assertEquals(b, h.redo())
        assertEquals(c, h.redo())
        assertNull(h.redo())
    }

    @Test
    fun `超过上限时丢弃最旧的一条`() {
        val h = EditHistory(maxEntries = 3)
        h.reset(EditSnapshot("0", "0"))
        for (i in 1..5) h.record(EditSnapshot("$i", "$i"))
        // 只剩最后三条：3 / 4 / 5，游标停在 5
        assertEquals("4", h.undo()?.title)
        assertEquals("3", h.undo()?.title)
        assertFalse(h.canUndo)
    }

    @Test
    fun `标题与正文任意一个变化都算新快照`() {
        val h = EditHistory()
        h.reset(EditSnapshot("title", "body"))
        h.record(EditSnapshot("title", "body!"))
        h.record(EditSnapshot("title!", "body!"))
        assertTrue(h.canUndo)
        assertEquals(EditSnapshot("title", "body!"), h.undo())
        assertEquals(EditSnapshot("title", "body"), h.undo())
    }
}
