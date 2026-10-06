package com.astelle.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlLikeTest {

    @Test
    fun `普通关键词原样通过`() {
        assertEquals("会议", escapeLikePattern("会议"))
        assertEquals("hello world", escapeLikePattern("hello world"))
    }

    @Test
    fun `百分号不再匹配一切`() {
        // 不转义的话 `%` 会命中所有笔记 —— 用户输入的是字面量，不是通配符
        assertEquals("\\%", escapeLikePattern("%"))
        assertEquals("100\\%", escapeLikePattern("100%"))
    }

    @Test
    fun `下划线不再匹配任意一个字符`() {
        assertEquals("\\_", escapeLikePattern("_"))
        assertEquals("a\\_b", escapeLikePattern("a_b"))
    }

    @Test
    fun `反斜杠先转义，不会被自己再转一遍`() {
        // 关键顺序：反斜杠必须第一个换，否则后面补进去的转义符会被二次转义
        assertEquals("\\\\", escapeLikePattern("\\"))
        assertEquals("\\\\\\%", escapeLikePattern("\\%"))
    }
}
