package com.mall.perflab.core.memory

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 内存监控器
 *
 * 【优化原理】
 * 1. 实时监控：跟踪内存使用趋势
 * 2. 泄漏检测：Activity/Fragment泄漏发现
 * 3. 自动GC：低内存时主动释放
 * 4. OOM防护：内存紧张时触发清理
 *
 * 【关键指标】
 * - Java Heap: 普通对象内存
 * - Native Heap: C/C++内存（Bitmap等）
 * - RSS: 常驻内存（实际占用）
 * - Threshold: 触发GC的阈值
 */
class MemoryMonitor(private val context: Context) {

    // ==================== 配置 ====================

    companion object {
        // 内存警告阈值（百分比）
        private const val WARNING_THRESHOLD = 80

        // 内存危险阈值（百分比）
        private const val DANGER_THRESHOLD = 90

        // 采样间隔（毫秒）
        private const val SAMPLE_INTERVAL_MS = 5000L
    }

    // ==================== 组件 ====================

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    // 内存信息缓存
    private var memInfo: ActivityManager.MemoryInfo? = null

    // 内存记录
    private val memoryRecords = ConcurrentHashMap<String, AtomicLong>()

    // 泄漏检测（Activity class -> 创建时间）
    private val activityTrackers = ConcurrentHashMap<Class<*>, Long>()

    // 监控状态
    @Volatile
    private var isMonitoring = false

    // ==================== 核心API ====================

    /**
     * 开始监控内存
     */
    fun start() {
        if (isMonitoring) return

        isMonitoring = true
        TraceLogger.i("MEMORY", "内存监控已启动")

        // 定时采样
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (isMonitoring) {
                sample()
                kotlinx.coroutines.delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    /**
     * 停止监控
     */
    fun stop() {
        isMonitoring = false
        TraceLogger.i("MEMORY", "内存监控已停止")
    }

    /**
     * 获取当前内存状态
     */
    fun getMemoryStatus(): MemoryStatus {
        updateMemInfo()

        val info = memInfo ?: return MemoryStatus.unknown()

        val totalMem = info.totalMem
        val availMem = info.availMem
        val usedMem = totalMem - availMem
        val usedPercent = (usedMem * 100) / totalMem

        // Java Heap
        val runtime = Runtime.getRuntime()
        val javaHeapUsed = runtime.totalMemory() - runtime.freeMemory()
        val javaHeapMax = runtime.maxMemory()

        // Native Heap
        val nativeHeap = Debug.getNativeHeapAllocatedSize()

        // RSS
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val rss = memoryInfo.totalMem - memoryInfo.availMem

        return MemoryStatus(
            usedMemory = usedMem,
            totalMemory = totalMem,
            usedPercent = usedPercent,
            javaHeapUsed = javaHeapUsed,
            javaHeapMax = javaHeapMax,
            nativeHeapAllocated = nativeHeap,
            rss = rss,
            isLowMemory = info.lowMemory,
            canGetMoreMemory = !info.lowMemory
        )
    }

    /**
     * 获取内存趋势
     */
    fun getMemoryTrend(): List<Long> {
        return memoryRecords.values.map { it.get() }
    }

    /**
     * 主动触发GC（谨慎使用）
     */
    fun requestGC() {
        if (!FeatureToggle.isOptimized) return

        TraceLogger.w("MEMORY", "主动触发GC")
        System.gc()

        // 记录GC后的内存
        val status = getMemoryStatus()
        memoryRecords["gc"]?.addAndGet(status.usedMemory)
    }

    /**
     * 记录内存快照
     */
    fun recordSnapshot(tag: String) {
        val status = getMemoryStatus()
        memoryRecords[tag] = AtomicLong(status.usedMemory)

        // 内存警告
        if (status.usedPercent >= WARNING_THRESHOLD) {
            TraceLogger.w("MEMORY", "内存警告: ${status.usedPercent}%")
        }

        // 内存危险
        if (status.usedPercent >= DANGER_THRESHOLD) {
            TraceLogger.e("MEMORY", "内存危险: ${status.usedPercent}%")
            requestGC()
        }

        TraceLogger.Perf.metric(tag, status.usedMemory, "bytes")
    }

    /**
     * 检查Activity泄漏
     */
    fun checkForLeak(activityClass: Class<*>): Boolean {
        val createdTime = activityTrackers[activityClass] ?: return false

        // 超过30秒未销毁，疑似泄漏
        val duration = System.currentTimeMillis() - createdTime
        if (duration > 30_000) {
            TraceLogger.e("MEMORY", "Activity泄漏检测: ${activityClass.simpleName}, 时长=${duration}ms")
            return true
        }

        return false
    }

    /**
     * 标记Activity创建
     */
    fun markActivityCreated(activityClass: Class<*>) {
        activityTrackers[activityClass] = System.currentTimeMillis()
    }

    /**
     * 标记Activity销毁
     */
    fun markActivityDestroyed(activityClass: Class<*>) {
        activityTrackers.remove(activityClass)
    }

    /**
     * 获取最大可用内存
     */
    fun getMaxAvailableMemory(): Long {
        return Runtime.getRuntime().maxMemory()
    }

    /**
     * 计算当前可用内存百分比
     */
    fun getAvailableMemoryPercent(): Float {
        val status = getMemoryStatus()
        return 1f - (status.usedMemory.toFloat() / status.totalMemory)
    }

    /**
     * 安全获取内存值（防止OOM）
     */
    fun tryAllocate(safeSize: Long, allocator: () -> Any?): Any? {
        val status = getMemoryStatus()

        // 内存紧张时先GC
        if (status.usedPercent > DANGER_THRESHOLD) {
            requestGC()
        }

        // 内存不够分配
        if (status.usedMemory + safeSize > status.totalMemory * 0.9) {
            TraceLogger.e("MEMORY", "拒绝分配: 内存不足, 需要=${safeSize}")
            return null
        }

        return allocator()
    }

    // ==================== 内部方法 ====================

    private fun updateMemInfo() {
        if (memInfo == null) {
            memInfo = ActivityManager.MemoryInfo()
        }
        activityManager.getMemoryInfo(memInfo)
    }

    private fun sample() {
        val status = getMemoryStatus()
        val timestamp = System.currentTimeMillis()

        memoryRecords["sample_$timestamp"] = AtomicLong(status.usedMemory)

        // 记录关键指标
        PerformanceTracker.trace("memory_sample", "monitor") {
            mapOf(
                "used_percent" to status.usedPercent,
                "java_heap" to status.javaHeapUsed,
                "native" to status.nativeHeapAllocated
            )
        }
    }

    // ==================== 数据类 ====================

    /**
     * 内存状态
     */
    data class MemoryStatus(
        val usedMemory: Long,
        val totalMemory: Long,
        val usedPercent: Int,
        val javaHeapUsed: Long,
        val javaHeapMax: Long,
        val nativeHeapAllocated: Long,
        val rss: Long,
        val isLowMemory: Boolean,
        val canGetMoreMemory: Boolean
    ) {
        companion object {
            fun unknown(): MemoryStatus {
                return MemoryStatus(0, 0, 0, 0, 0, 0, 0, false, true)
            }
        }
    }
}
