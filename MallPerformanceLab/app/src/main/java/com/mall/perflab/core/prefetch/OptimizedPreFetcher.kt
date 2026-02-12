package com.mall.perflab.core.prefetch

import android.os.Handler
import android.os.Looper
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 优化版预请求管理器 - Mall Performance Lab
 *
 * 【优化点】
 * 1. 智能预取：基于用户行为预测触发时机
 * 2. 请求去重：相同请求只发一次
 * 3. 优先级队列：首屏数据优先
 * 4. 带宽感知：避免与主请求争抢带宽
 *
 * 预取策略对比：
 * - Baseline: 用户触发时才请求
 * - Optimized: 预取+并行+去重
 */
class OptimizedPreFetcher {

    // ==================== 配置 ====================

    companion object {
        // 预取延迟（避免启动时带宽争抢）
        private const val PREFETCH_DELAY_MS = 500L

        // Feed预取阈值（剩余N条时触发）
        private const val FEED_PRELOAD_THRESHOLD = 3

        // 最大并行预取数
        private const val MAX_CONCURRENT_PREFETCH = 3
    }

    // ==================== 组件 ====================

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    // 已预取的去重集合
    private val preFetchedKeys = ConcurrentHashMap<String, Long>()

    // 预取任务状态
    private val activeTasks = ConcurrentHashMap<String, Job>()

    // 预取优先级队列
    private val priorityQueue = ConcurrentHashMap<Int, MutableList<String>>()

    // 并发控制
    private val runningCount = AtomicInteger(0)

    // 监听器
    private val listeners = ConcurrentHashMap<String, MutableList<(String, Boolean) -> Unit>>()

    // ==================== 核心API ====================

    /**
     * 预请求（智能版）
     *
     * @param key 请求标识
     * @param priority 优先级（数字越小优先级越高）
     * @param delayMs 延迟时间（毫秒）
     * @param fetcher 请求回调
     */
    fun preFetch(
        key: String,
        priority: Int = 5,
        delayMs: Long = PREFETCH_DELAY_MS,
        fetcher: suspend () -> String
    ) {
        if (!FeatureToggle.usePreFetch()) return

        // 1. 检查是否已预取（去重）
        if (isPreFetched(key)) {
            TraceLogger.d("PREFETCH", "跳过重复预取: $key")
            return
        }

        // 2. 检查是否正在预取
        if (activeTasks.containsKey(key)) {
            return
        }

        // 3. 加入优先级队列
        priorityQueue.getOrPut(priority) { mutableListOf() }.add(key)

        // 4. 延迟后执行（避免启动时争抢带宽）
        handler.postDelayed({
            executePreFetch(key, priority, fetcher)
        }, delayMs)
    }

    /**
     * Feed预取（当用户浏览到阈值时触发）
     */
    fun preFetchFeed(
        currentPage: Int,
        totalItems: Int,
        fetcher: suspend (Int) -> String
    ) {
        val remaining = totalItems - currentPage

        // 当剩余数据少于阈值时，预取下一页
        if (remaining <= FEED_PRELOAD_THRESHOLD) {
            val nextPage = currentPage + 1
            val key = "feed_page_$nextPage"

            preFetch(key, priority = 3, fetcher = {
                fetcher(nextPage)
            })
        }
    }

    /**
     * 取消预取
     */
    fun cancel(key: String) {
        activeTasks[key]?.cancel()
        activeTasks.remove(key)
        preFetchedKeys.remove(key)
        TraceLogger.PreFetch.cancel(key)
    }

    /**
     * 取消所有预取
     */
    fun cancelAll() {
        activeTasks.values.forEach { it.cancel() }
        activeTasks.clear()
        handler.removeCallbacksAndMessages(null)
        TraceLogger.d("PREFETCH", "取消所有预取任务")
    }

    /**
     * 检查是否已预取
     */
    fun isPreFetched(key: String): Boolean {
        val timestamp = preFetchedKeys[key] ?: return false
        // 5分钟内认为有效
        return System.currentTimeMillis() - timestamp < 5 * 60 * 1000L
    }

    /**
     * 获取预取统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "preFetchedCount" to preFetchedKeys.size,
            "activeTasks" to activeTasks.size,
            "runningCount" to runningCount.get()
        )
    }

    /**
     * 清理资源
     */
    fun destroy() {
        cancelAll()
        scope.cancel()
    }

    // ==================== 内部实现 ====================

    private fun executePreFetch(
        key: String,
        priority: Int,
        fetcher: suspend () -> String
    ) {
        // 并发控制
        if (runningCount.get() >= MAX_CONCURRENT_PREFETCH) {
            // 延迟重试
            handler.postDelayed({
                executePreFetch(key, priority, fetcher)
            }, 200)
            return
        }

        runningCount.incrementAndGet()
        activeTasks[key] = scope.launch {
            try {
                val startTime = System.currentTimeMillis()

                TraceLogger.PreFetch.start(key)

                val result = fetcher()

                val latency = System.currentTimeMillis() - startTime

                // 标记完成
                preFetchedKeys[key] = System.currentTimeMillis()

                TraceLogger.PreFetch.done(key, latency)

                // 通知监听器
                notifyListeners(key, true)

            } catch (e: Exception) {
                TraceLogger.e("PREFETCH", "预取失败: $key", e)
                notifyListeners(key, false)
            } finally {
                runningCount.decrementAndGet()
                activeTasks.remove(key)
            }
        }
    }

    private fun notifyListeners(key: String, success: Boolean) {
        listeners[key]?.forEach { callback ->
            handler.post { callback(key, success) }
        }
    }

    /**
     * 添加监听器
     */
    fun addListener(key: String, callback: (String, Boolean) -> Unit) {
        listeners.getOrPut(key) { mutableListOf() }.add(callback)
    }
}
