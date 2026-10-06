package com.astelle.app.ui.home

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 抽屉顶上那行问候。看起来是小事，但节日倒数算错一整天，用户是会注意到的。
 */
class CleanGreetingTest {

    /** 固定到某天的中午，好让「时段」部分稳定 */
    private fun noon(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun `节日当天直接报节日`() {
        assertEquals("国庆节 · 中午好", cleanGreeting(noon(2026, 10, 1)))
    }

    @Test
    fun `平常日子数到下一个节日`() {
        val text = cleanGreeting(noon(2026, 9, 28))
        assertTrue("9-28 到国庆还有 3 天，实际：$text", text.startsWith("距离国庆节还有 3 天"))
    }

    @Test
    fun `隔天就是节日时说「明天」`() {
        assertEquals("明天国庆节 · 中午好", cleanGreeting(noon(2026, 9, 30)))
    }

    @Test
    fun `闰年最后一天仍然数得到元旦`() {
        // 2028 是闰年。写死 365 的话，12-31 → 元旦 的差是 (1 - 366 + 365) % 365 = 0，
        // 会被当成「今天就是元旦」而整条丢掉，问候只剩一句「中午好」
        assertEquals("明天元旦 · 中午好", cleanGreeting(noon(2028, 12, 31)))
    }

    @Test
    fun `平年最后一天同样数得到元旦`() {
        assertEquals("明天元旦 · 中午好", cleanGreeting(noon(2026, 12, 31)))
    }
}
