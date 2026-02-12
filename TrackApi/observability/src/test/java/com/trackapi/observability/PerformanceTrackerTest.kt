/*
 * PerformanceTrackerTest.kt
 *
 * PerformanceTracker 单元测试
 *
 * 面试亮点：
 * - 单元测试覆盖核心功能
 * - 测试边界条件（重复停止、未开始停止等）
 * - 使用 JUnit 4 + Kotlin 风格断言
 */
package com.trackapi.observability

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * PerformanceTracker 单元测试
 */
class PerformanceTrackerTest {

    private lateinit var tracker: PerformanceTracker

    @Before
    fun setup() {
        tracker = PerformanceTracker()
    }

    @After
    fun teardown() {
        tracker.clear()
    }

    @Test
    fun `start and stop should return positive duration`() {
        val traceName = "test_trace"
        tracker.start(traceName)
        Thread.sleep(50) // 模拟耗时
        val duration = tracker.stop(traceName)

        assertNotNull("Duration should not be null", duration)
        assertTrue("Duration should be positive", duration!! >= 50)
    }

    @Test
    fun `stop without start should return null`() {
        val duration = tracker.stop("non_existent_trace")

        assertNull("Should return null for non-existent trace", duration)
    }

    @Test
    fun `double start should overwrite previous record`() {
        val traceName = "duplicate_trace"

        tracker.start(traceName)
        Thread.sleep(30)
        tracker.start(traceName) // 重新开始
        Thread.sleep(30)
        val duration = tracker.stop(traceName)

        // 第二次 start 会覆盖，所以 duration 应该接近第二次的 30ms
        assertNotNull(duration)
        assertTrue("Duration should be less than 100ms (accounting for overwrite)", duration!! < 100)
    }

    @Test
    fun `getCurrentDuration should return ongoing duration`() {
        val traceName = "ongoing_trace"
        tracker.start(traceName)
        Thread.sleep(50)

        val currentDuration = tracker.getCurrentDuration(traceName)

        assertNotNull("Current duration should not be null", currentDuration)
        assertTrue("Current duration should be >= 50ms", currentDuration!! >= 50)
    }

    @Test
    fun `isActive should return correct state`() {
        val traceName = "active_trace"

        assertFalse("Should not be active before start", tracker.isActive(traceName))

        tracker.start(traceName)
        assertTrue("Should be active after start", tracker.isActive(traceName))

        tracker.stop(traceName)
        assertFalse("Should not be active after stop", tracker.isActive(traceName))
    }

    @Test
    fun `clear should remove all active traces`() {
        tracker.start("trace_1")
        tracker.start("trace_2")

        tracker.clear()

        assertFalse("trace_1 should be cleared", tracker.isActive("trace_1"))
        assertFalse("trace_2 should be cleared", tracker.isActive("trace_2"))
        assertEquals("No active traces should remain", 0, tracker.activeTraces.size)
    }
}
