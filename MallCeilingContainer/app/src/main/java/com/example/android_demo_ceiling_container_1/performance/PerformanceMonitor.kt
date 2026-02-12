package com.example.android_demo_ceiling_container_1.performance

/**
 * 性能监控
 *
 * 记录 createView/compose 次数、渲染耗时
 */
class PerformanceMonitor {

    private var createViewCount = 0
    private var composeCount = 0
    private var lastOperationName = ""
    private var lastRenderTimeMs = 0L
    private val operationTimes = mutableMapOf<String, MutableList<Long>>()

    /**
     * 记录 createView 次数
     */
    fun incrementCreateViewCount() {
        createViewCount++
    }

    /**
     * 记录 compose 次数
     */
    fun incrementComposeCount() {
        composeCount++
    }

    /**
     * 记录操作耗时
     */
    fun recordOperation(operation: String, durationMs: Long) {
        lastOperationName = operation
        lastRenderTimeMs = durationMs

        val times = operationTimes.getOrPut(operation) { mutableListOf() }
        times.add(durationMs)
    }

    /**
     * 获取统计信息
     */
    fun getStats(): PerformanceStats {
        val avgTimes = operationTimes.mapValues { (_, times) ->
            if (times.isNotEmpty()) times.average().toLong() else 0L
        }

        return PerformanceStats(
            createViewCount = createViewCount,
            composeCount = composeCount,
            lastRenderTimeMs = lastRenderTimeMs,
            totalRenderTimeMs = operationTimes.values.flatten().sum()
        )
    }

    /**
     * 重置统计
     */
    fun reset() {
        createViewCount = 0
        composeCount = 0
        lastRenderTimeMs = 0
        operationTimes.clear()
    }

    /**
     * 打印统计日志
     */
    fun logStats(tag: String = "Performance") {
        val stats = getStats()
        val avgTime = if (operationTimes.isNotEmpty()) {
            operationTimes.values.flatten().average().toLong()
        } else 0L

        println("""
            |=== Performance Stats ===
            |createViewCount: ${stats.createViewCount}
            |composeCount: ${stats.composeCount}
            |lastRenderTimeMs: ${stats.lastRenderTimeMs}
            |avgRenderTimeMs: $avgTime
            |totalRenderTimeMs: ${stats.totalRenderTimeMs}
            |==========================
        """.trimMargin())
    }

    companion object {
        @Volatile
        private var instance: PerformanceMonitor? = null

        fun getInstance(): PerformanceMonitor {
            return instance ?: synchronized(this) {
                instance ?: PerformanceMonitor().also { instance = it }
            }
        }
    }
}

data class PerformanceStats(
    val createViewCount: Int = 0,
    val composeCount: Int = 0,
    val lastRenderTimeMs: Long = 0,
    val totalRenderTimeMs: Long = 0
)
