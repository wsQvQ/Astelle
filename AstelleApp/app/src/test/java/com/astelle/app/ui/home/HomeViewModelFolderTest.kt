package com.astelle.app.ui.home

import com.astelle.app.domain.model.Folder
import com.astelle.app.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 分类的集成验证。
 *
 * 重点不是「能不能建分类」，而是**删除 / 移动之后，内存里的 activeNote
 * 有没有跟着改**。不同步的话，下一次 500ms 自动保存会整行回写，
 * 把刚做的操作悄悄抹掉 —— 不报错、不崩溃，只是数据不对。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFolderTest {

    private val dispatcher = StandardTestDispatcher()
    private val settle = 700L
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `新建分类会落库并按创建顺序排在后头`() = runTest(dispatcher) {
        val folders = FakeFolderRepository()
        val vm = HomeViewModel(FakeNoteRepository(), folders)
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.AddFolder("工作")); runCurrent()
        vm.onEvent(HomeUiEvent.AddFolder("生活")); runCurrent()

        val names = folders.stored.values.sortedBy { it.sortOrder }.map { it.name }
        assertEquals(listOf("工作", "生活"), names)
        assertTrue("排序不应撞车", folders.stored.values.map { it.sortOrder }.toSet().size == 2)
    }

    @Test
    fun `空白分类名会被忽略`() = runTest(dispatcher) {
        val folders = FakeFolderRepository()
        val vm = HomeViewModel(FakeNoteRepository(), folders)
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.AddFolder("   ")); runCurrent()

        assertTrue("空白名不该建出分类", folders.stored.isEmpty())
    }

    @Test
    fun `重命名分类`() = runTest(dispatcher) {
        val folders = FakeFolderRepository()
        folders.upsert(Folder(id = "f1", name = "工作", createdAt = now))
        val vm = HomeViewModel(FakeNoteRepository(), folders)
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.RenameFolder("f1", "项目")); runCurrent()

        assertEquals("项目", folders.stored["f1"]?.name)
    }

    @Test
    fun `删除分类后自动保存不会把悬空 folderId 写回去`() = runTest(dispatcher) {
        val notes = FakeNoteRepository()
        val folders = FakeFolderRepository()
        notes.upsert(
            Note(
                id = "n1", title = "会议纪要", content = "内容",
                createdAt = now, updatedAt = now, folderId = "f1",
            )
        )
        folders.upsert(Folder(id = "f1", name = "工作", createdAt = now))

        val vm = HomeViewModel(notes, folders)
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.OpenNote("n1")); runCurrent()
        assertEquals("f1", notes.getNote("n1")?.folderId)

        vm.onEvent(HomeUiEvent.DeleteFolder("f1")); runCurrent()
        assertTrue("分类本身应已被删除", folders.stored.isEmpty())

        // 改一个字，触发一次真实的去抖落库（persist 会拿 activeNote 整行回写）
        vm.onEvent(HomeUiEvent.ContentChanged("内容2"))
        advanceTimeBy(settle); runCurrent()

        assertNull(
            "分类已经没了，笔记不该还挂着一个不存在的 folderId",
            notes.getNote("n1")?.folderId,
        )
    }

    @Test
    fun `移动笔记后自动保存不会把它挪回去`() = runTest(dispatcher) {
        val notes = FakeNoteRepository()
        val folders = FakeFolderRepository()
        notes.upsert(
            Note(id = "n1", title = "随笔", content = "a", createdAt = now, updatedAt = now)
        )
        folders.upsert(Folder(id = "f1", name = "工作", createdAt = now))

        val vm = HomeViewModel(notes, folders)
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.OpenNote("n1")); runCurrent()
        vm.onEvent(HomeUiEvent.MoveNoteToFolder("n1", "f1")); runCurrent()
        assertEquals("f1", notes.getNote("n1")?.folderId)

        vm.onEvent(HomeUiEvent.ContentChanged("b"))
        advanceTimeBy(settle); runCurrent()

        assertEquals(
            "自动保存整行回写时，不能被内存里的旧 folderId 抹掉",
            "f1",
            notes.getNote("n1")?.folderId,
        )
    }

    @Test
    fun `移出分类回到未分类`() = runTest(dispatcher) {
        val notes = FakeNoteRepository()
        notes.upsert(
            Note(
                id = "n1", title = "随笔", content = "a",
                createdAt = now, updatedAt = now, folderId = "f1",
            )
        )
        val vm = HomeViewModel(notes, FakeFolderRepository())
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.OpenNote("n1")); runCurrent()
        vm.onEvent(HomeUiEvent.MoveNoteToFolder("n1", null)); runCurrent()

        assertNull(notes.getNote("n1")?.folderId)
    }
}
