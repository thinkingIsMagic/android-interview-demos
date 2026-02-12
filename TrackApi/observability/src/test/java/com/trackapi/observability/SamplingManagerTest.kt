/*
 * SamplingManagerTest.kt
 *
 * SamplingManager 单元测试
 *
 * 面试亮点：
 * - 测试采样算法的正确性
 * - 测试采样率的边界条件
 */
package com.trackapi.observability

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SamplingManager 单元测试
 */
class SamplingManagerTest {

    private lateinit var samplingManager: SamplingManager

    @Before
    fun setup() {
        samplingManager = SamplingManager()
    }

    @Test
    fun `zero sampling rate should never sample`() {
        samplingManager.setSamplingRate("test_event", 0.0f)

        // 运行多次，确保一致返回 false
        repeat(100) {
            assertFalse(
                "With 0.0 sampling rate, should never sample",
                samplingManager.shouldSample("test_event")
            )
        }
    }

    @Test
    fun `one sampling rate should always sample`() {
        samplingManager.setSamplingRate("test_event", 1.0f)

        repeat(100) {
            assertTrue(
                "With 1.0 sampling rate, should always sample",
                samplingManager.shouldSample("test_event")
            )
        }
    }

    @Test
    fun `unknown event type should use default sampling rate`() {
        // 默认采样率是 1.0，所以应该总是返回 true
        assertTrue(
            "Unknown event type should use default sampling rate (1.0)",
            samplingManager.shouldSample("unknown_event")
        )
    }

    @Test
    fun `reset should clear custom sampling rates`() {
        samplingManager.setSamplingRate("custom_event", 0.0f)
        samplingManager.reset()

        // reset 后应该恢复到默认 1.0
        assertTrue(
            "After reset, should use default sampling rate",
            samplingManager.shouldSample("custom_event")
        )
    }

    @Test
    fun `different event types can have different sampling rates`() {
        samplingManager.setSamplingRate("event_a", 0.0f)
        samplingManager.setSamplingRate("event_b", 1.0f)

        assertFalse("event_a should not be sampled", samplingManager.shouldSample("event_a"))
        assertTrue("event_b should always be sampled", samplingManager.shouldSample("event_b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sampling rate below zero should throw exception`() {
        samplingManager.setSamplingRate("test", -0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sampling rate above one should throw exception`() {
        samplingManager.setSamplingRate("test", 1.1f)
    }
}
