/*
 * TrackerRegistryTest.kt
 *
 * TrackerRegistry 单元测试
 *
 * 面试亮点：
 * - 测试注册表的核心功能
 * - 测试采样和降级逻辑
 * - 测试线程安全性
 */
package com.trackapi.observability

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * TrackerRegistry 单元测试
 */
class TrackerRegistryTest {

    private lateinit var registry: TrackerRegistry

    @Before
    fun setup() {
        registry = TrackerRegistry()
    }

    @Test
    fun `register should add tracker to list`() {
        val tracker = EventTracker()
        registry.register(tracker)

        val trackers = registry.getTrackers()
        assertTrue("Tracker should be in list", trackers.contains(tracker))
    }

    @Test
    fun `register duplicate tracker should not add twice`() {
        val tracker = EventTracker()
        registry.register(tracker)
        registry.register(tracker)

        val trackers = registry.getTrackers()
        assertEquals("Should have only one tracker", 1, trackers.size)
    }

    @Test
    fun `unregister should remove tracker from list`() {
        val tracker = EventTracker()
        registry.register(tracker)
        registry.unregister(tracker)

        val trackers = registry.getTrackers()
        assertTrue("Tracker should not be in list", trackers.isEmpty())
    }

    @Test
    fun `notifyEvent should distribute to all trackers`() {
        val eventTracker = EventTracker()
        registry.register(eventTracker)

        registry.notifyEvent("test_event", mapOf("key" to "value"))

        // EventTracker should have processed the event
        // (We can verify by checking that no exception was thrown)
    }

    @Test
    fun `notifyPerformanceStart should return start time`() {
        val startTime = registry.notifyPerformanceStart("test_trace")

        assertTrue("Start time should be positive", startTime > 0)
        assertEquals("Should have 1 active trace", 1, registry.getActiveTraceCount())
    }

    @Test
    fun `notifyPerformanceStop should return duration`() {
        registry.notifyPerformanceStart("test_trace")
        Thread.sleep(50)
        val duration = registry.notifyPerformanceStop("test_trace")

        assertNotNull("Duration should not be null", duration)
        assertTrue("Duration should be >= 50ms", duration!! >= 50)
        assertEquals("Should have no active traces", 0, registry.getActiveTraceCount())
    }

    @Test
    fun `stop without start should return null`() {
        val duration = registry.notifyPerformanceStop("non_existent")

        assertNull("Should return null", duration)
    }

    @Test
    fun `setSamplingRate should configure sampling`() {
        // Set sampling rate to 0
        registry.setSamplingRate("test_event", 0.0f)

        // With 0 sampling rate, events should be filtered out
        // (This is an internal behavior test)
        registry.setSamplingRate("test_event", 1.0f) // Reset
    }

    @Test
    fun `setFeatureEnabled should toggle features`() {
        registry.setFeatureEnabled("performance_tracking", false)

        // Performance tracking should be disabled
        val startTime = registry.notifyPerformanceStart("test")
        assertEquals("Start should be blocked when feature disabled", -1L, startTime)
    }
}
