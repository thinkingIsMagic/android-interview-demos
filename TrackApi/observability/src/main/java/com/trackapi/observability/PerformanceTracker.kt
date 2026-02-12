/*
 * PerformanceTracker.kt
 *
 * 性能耗时 Tracker
 *
 * 面试亮点：
 * 1. start/stop 模式，自动计算耗时
 * 2. 支持闭包形式，更优雅
 * 3. 可扩展：可以后续添加阈值告警
 */
package com.trackapi.observability

import java.util.concurrent.ConcurrentHashMap

/**
 * 性能耗时 Tracker
 *
 * 支持两种使用方式：
 * 1. start/stop 手动控制
 * 2. 闭包形式，自动管理
 *
 * 内部使用 ConcurrentHashMap 保证线程安全
 */
class PerformanceTracker : ITracker {

    // 正在进行的追踪
    private val activeTraces = ConcurrentHashMap<String, TraceRecord>()

    /**
     * 开始追踪
     *
     * @param traceName 追踪名称
     * @return 开始时间戳
     */
    fun start(traceName: String): Long {
        val startTime = System.currentTimeMillis()
        activeTraces[traceName] = TraceRecord(
            name = traceName,
            startTime = startTime
        )
        return startTime
    }

    /**
     * 结束追踪
     *
     * @param traceName 追踪名称
     * @return 耗时（毫秒），未找到返回 null
     */
    fun stop(traceName: String): Long? {
        val record = activeTraces.remove(traceName) ?: return null
        val duration = System.currentTimeMillis() - record.startTime

        // 输出性能日志
        StructuredLogger.info(
            type = "performance",
            message = "Trace completed: $traceName",
            data = mapOf(
                "traceName" to traceName,
                "durationMs" to duration
            )
        )

        return duration
    }

    /**
     * 获取当前正在追踪的耗时（不删除记录）
     */
    fun getCurrentDuration(traceName: String): Long? {
        val record = activeTraces[traceName] ?: return null
        return System.currentTimeMillis() - record.startTime
    }

    /**
     * 检查是否正在追踪
     */
    fun isActive(traceName: String): Boolean {
        return activeTraces.containsKey(traceName)
    }

    override fun onPerformanceStart(traceName: String, startTime: Long) {
        start(traceName)
    }

    override fun onPerformanceStop(traceName: String, startTime: Long, duration: Long) {
        // 这里 duration 可能是估算的，stop 会重新计算
        stop(traceName)
    }

    /**
     * 清理所有正在追踪的记录（用于测试）
     */
    fun clear() {
        activeTraces.clear()
    }

    /**
     * 追踪记录数据类
     */
    private data class TraceRecord(
        val name: String,
        val startTime: Long,
        val tags: Map<String, Any?> = emptyMap()
    )
}

/**
 * 性能追踪 Builder
 *
 * 提供闭包形式的性能追踪
 */
inline fun <T> measurePerformance(
    tag: String,
    block: () -> T
): PerformanceMeasurement {
    val tracker = PerformanceTracker()
    tracker.start(tag)

    return try {
        val result = block()
        val duration = tracker.stop(tag)
        PerformanceMeasurement(
            tag = tag,
            durationMs = duration ?: -1,
            success = true
        )
    } catch (e: Exception) {
        val duration = tracker.stop(tag)
        PerformanceMeasurement(
            tag = tag,
            durationMs = duration ?: -1,
            success = false,
            error = e
        )
    }
}

/**
 * 性能测量结果
 */
data class PerformanceMeasurement(
    val tag: String,
    val durationMs: Long,
    val success: Boolean,
    val error: Throwable? = null
)
