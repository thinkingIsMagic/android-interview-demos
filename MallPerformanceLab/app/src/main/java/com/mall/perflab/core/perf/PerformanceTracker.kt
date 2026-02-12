package com.mall.perflab.core.perf

import android.os.Handler
import android.os.Looper
import android.os.Trace
import com.mall.perflab.core.config.FeatureToggle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 性能打点器 - Mall Performance Lab 核心可观测性组件
 *
 * 功能：
 * 1. 关键链路打点：冷启动→首帧→首屏数据→首屏渲染→可交互
 * 2. 模式分流：Baseline/Optimized 分开统计
 * 3. 结构化输出：日志 + 简单统计
 *
 * 使用方式：
 * - PerformanceTracker.begin("cold_start")  // 开始打点
 * - PerformanceTracker.end("cold_start")    // 结束打点
 * - PerformanceTracker.dump()               // 输出全部记录
 */
object PerformanceTracker {

    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    // ==================== 数据结构 ====================

    /**
     * 单次打点记录
     */
    data class TraceRecord(
        val name: String,           // 打点名称
        val startTime: Long,        // 开始时间（相对于进程启动）
        val endTime: Long,          // 结束时间
        val mode: FeatureToggle.Mode, // 当时的运行模式
        val threadName: String      // 所在线程
    ) {
        val duration: Long get() = endTime - startTime
    }

    /**
     * 统计摘要（同名打点的聚合）
     */
    data class TraceSummary(
        val name: String,
        val count: Int,
        val avgMs: Long,
        val minMs: Long,
        val maxMs: Long,
        val lastMs: Long,
        val mode: FeatureToggle.Mode
    )

    // ==================== 存储 ====================

    // 所有打点记录（线程安全）
    private val records = CopyOnWriteArrayList<TraceRecord>()

    // 同名打点的聚合统计（快速查询）
    private val summaryMap = ConcurrentHashMap<String, MutableList<TraceSummary>>()

    // 正在进行的打点（支持嵌套）
    private val activeTraces = ConcurrentHashMap<String, Long>()

    // ==================== 核心API ====================

    /**
     * 开始一个打点
     * @param name 打点名称，建议用下划线命名：module_action
     */
    fun begin(name: String) {
        // 系统Trace（可用于systrace/perfetto分析）
        if (FeatureToggle.isOptimized) {
            Trace.beginSection("opt_$name")
        } else {
            Trace.beginSection("base_$name")
        }

        activeTraces[name] = System.nanoTime()
    }

    /**
     * 结束一个打点并记录
     * @param name 打点名称
     * @param tags 附加标签，逗号分隔，如"critical,first_screen"
     */
    fun end(name: String, tags: String = ""): TraceRecord? {
        val startTime = activeTraces.remove(name) ?: return null
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000

        // 结束系统Trace
        Trace.endSection()

        // 记录
        val record = TraceRecord(
            name = name,
            startTime = startTime,
            endTime = endTime,
            mode = FeatureToggle.currentMode,
            threadName = Thread.currentThread().name
        )
        records.add(record)

        // 打印日志（便于实时观察）
        val modeTag = if (FeatureToggle.isOptimized) "[OPT]" else "[BAS]"
        val tagInfo = if (tags.isNotEmpty()) " [$tags]" else ""
        println("$modeTag TRACE_END: $name = ${durationMs}ms$tagInfo")

        return record
    }

    /**
     * 便捷方法：打点并直接返回耗时（毫秒）
     */
    inline fun <T> trace(name: String, tags: String = "", block: () -> T): T {
        begin(name)
        return try {
            block()
        } finally {
            end(name, tags)
        }
    }

    /**
     * 异步打点（不阻塞调用线程）
     */
    fun beginAsync(name: String, delayMs: Long = 0) {
        if (delayMs <= 0) {
            handler.post { begin(name) }
        } else {
            handler.postDelayed({ begin(name) }, delayMs)
        }
    }

    /**
     * 异步结束打点
     */
    fun endAsync(name: String) {
        handler.post { end(name) }
    }

    // ==================== 统计与输出 ====================

    /**
     * 获取所有记录
     */
    fun getAllRecords(): List<TraceRecord> = records.toList()

    /**
     * 获取指定名称的记录
     */
    fun getRecords(name: String): List<TraceRecord> =
        records.filter { it.name == name }

    /**
     * 清空所有记录（用于重新测试）
     */
    fun clear() {
        records.clear()
        activeTraces.clear()
        summaryMap.clear()
    }

    /**
     * 输出一份结构化报告
     */
    fun dump(): String {
        val sb = StringBuilder()
        sb.appendLine("\n╔══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║            Mall Performance Lab - 性能报告                    ║")
        sb.appendLine("╚══════════════════════════════════════════════════════════════╝")

        // 按模式分组
        val baselineRecords = records.filter { it.mode == FeatureToggle.Mode.BASELINE }
        val optimizedRecords = records.filter { it.mode == FeatureToggle.Mode.OPTIMIZED }

        sb.appendLine("\n【运行模式】: ${FeatureToggle.currentMode}")
        sb.appendLine("【总打点数】: ${records.size}")
        sb.appendLine("  - Baseline: ${baselineRecords.size} 次")
        sb.appendLine("  - Optimized: ${optimizedRecords.size} 次")

        if (records.isEmpty()) {
            sb.appendLine("\n暂无打点数据，请先触发页面加载")
            return sb.toString()
        }

        // 按名称聚合统计
        sb.appendLine("\n┌─────────────────────────────────────┬────────┬─────────┬────────┬────────┐")
        sb.appendLine("│ 打点名称                           │  次数  │  平均   │  最短  │  最长  │")
        sb.appendLine("├─────────────────────────────────────┼────────┼─────────┼────────┼────────┤")

        records.groupBy { it.name }.forEach { (name, list) ->
            if (list.size >= 1) {
                val durations = list.map { it.duration / 1_000_000 }
                val avg = durations.average().toLong()
                val min = durations.minOrNull() ?: 0
                val max = durations.maxOrNull() ?: 0

                val displayName = if (name.length > 35) name.take(32) + "..." else name
                sb.appendLine(String.format("│ %-35s │ %6d │ %6dms │ %6dms │ %6dms │",
                    displayName, list.size, avg, min, max))
            }
        }
        sb.appendLine("└─────────────────────────────────────┴────────┴─────────┴────────┴────────┘")

        // 关键指标对比（如果两种模式都有数据）
        if (baselineRecords.isNotEmpty() && optimizedRecords.isNotEmpty()) {
            sb.appendLine("\n【关键指标对比】")
            sb.appendLine("┌─────────────────────────────────────┬──────────────┬──────────────┬──────────┐")
            sb.appendLine("│ 指标                                 │ Baseline     │ Optimized    │ 提升     │")
            sb.appendLine("├─────────────────────────────────────┼──────────────┼──────────────┼──────────┤")

            val keyMetrics = listOf(
                "perf_mall_cold_start",
                "perf_mall_first_data",
                "perf_mall_first_content",
                "perf_mall_interactive"
            )

            keyMetrics.forEach { metric ->
                val base = baselineRecords.filter { it.name == metric }
                val opt = optimizedRecords.filter { it.name == metric }

                if (base.isNotEmpty() && opt.isNotEmpty()) {
                    val baseAvg = base.map { it.duration / 1_000_000 }.average().toLong()
                    val optAvg = opt.map { it.duration / 1_000_000 }.average().toLong()
                    val improvement = if (baseAvg > 0) ((baseAvg - optAvg) * 100 / baseAvg) else 0

                    val displayName = if (metric.length > 35) metric.take(32) + "..." else metric
                    sb.appendLine(String.format("│ %-35s │ %12dms │ %12dms │ %6d%%  │",
                        displayName, baseAvg, optAvg, improvement))
                }
            }
            sb.appendLine("└─────────────────────────────────────┴──────────────┴──────────────┴──────────┘")
        }

        sb.appendLine("\n【输出时间】: ${dateFormat.format(Date())}")
        return sb.toString()
    }

    /**
     * 打印报告到Logcat
     */
    fun report() {
        println(dump())
    }
}
