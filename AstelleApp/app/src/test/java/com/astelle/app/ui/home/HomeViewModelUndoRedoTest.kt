package com.astelle.app.ui.home

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 撤销 / 重做的集成验证：不是只测 EditHistory 这个纯类，
 * 而是驱动真实的 HomeViewModel —— 包含 500ms 去抖那条数据流。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelUndoRedoTest {

    private val dispatcher = StandardTestDispatcher()

    /** 去抖窗口是 500ms，这里统一推进 700ms 确保它一定触发过 */
    private val settle = 700L

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `输入停稳后可以撤销，并且能重做回来`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeNoteRepository(), FakeFolderRepository())
        advanceTimeBy(settle); runCurrent()

        // 刚打开时没有历史，撤销钮应是禁用的
        assertFalse("初始不该可撤销", vm.uiState.value.canUndo)

        vm.onEvent(HomeUiEvent.ContentChanged("hello"))
        advanceTimeBy(settle); runCurrent()
        assertTrue("输入停稳后应可撤销", vm.uiState.value.canUndo)

        vm.onEvent(HomeUiEvent.Undo)
        assertEquals("撤销应回到空正文", "", vm.uiState.value.content)
        assertTrue("撤销后应可重做", vm.uiState.value.canRedo)

        vm.onEvent(HomeUiEvent.Redo)
        assertEquals("重做应回到 hello", "hello", vm.uiState.value.content)
        assertFalse("回到最新后不该还能重做", vm.uiState.value.canRedo)
    }

    @Test
    fun `连续多次编辑后能一步步退回`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeNoteRepository(), FakeFolderRepository())
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.TitleChanged("标题"))
        advanceTimeBy(settle); runCurrent()
        vm.onEvent(HomeUiEvent.ContentChanged("正文"))
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.Undo)
        assertEquals("标题", vm.uiState.value.title)
        assertEquals("", vm.uiState.value.content)

        vm.onEvent(HomeUiEvent.Undo)
        assertEquals("", vm.uiState.value.title)
        assertFalse(vm.uiState.value.canUndo)
    }

    @Test
    fun `新建笔记会清空历史`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeNoteRepository(), FakeFolderRepository())
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.ContentChanged("hello"))
        advanceTimeBy(settle); runCurrent()
        assertTrue(vm.uiState.value.canUndo)

        vm.onEvent(HomeUiEvent.NewNote)
        runCurrent()

        assertFalse("新建后不该残留可撤销", vm.uiState.value.canUndo)
        assertFalse(vm.uiState.value.canRedo)
        assertEquals("", vm.uiState.value.content)
    }

    @Test
    fun `撤销本身不会污染历史`() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeNoteRepository(), FakeFolderRepository())
        advanceTimeBy(settle); runCurrent()

        vm.onEvent(HomeUiEvent.ContentChanged("hello"))
        advanceTimeBy(settle); runCurrent()

        // 撤销后让状态回流（去抖再跑一轮），历史长度不应增长
        vm.onEvent(HomeUiEvent.Undo)
        advanceTimeBy(settle); runCurrent()

        // 此时只该有「空」和「hello」两条：撤销一次就到底
        assertFalse("撤销后不应还能继续撤销", vm.uiState.value.canUndo)
        assertTrue("hello 应仍可重做", vm.uiState.value.canRedo)
    }
}
