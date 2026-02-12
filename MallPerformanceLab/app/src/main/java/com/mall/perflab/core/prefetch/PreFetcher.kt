package com.mall.perflab.core.prefetch

import android.os.Handler
import android.os.Looper
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 预请求管理器
 *
 * 功能：
 * 1. 首屏数据预请求（应用启动时/进入页面前）
 * 2. Feed下一页预取
 * 3. 请求去重（相同key只发一次）
 *
 * 优化点：
 * - usePreFetch(): 控制是否启用
 * - 提前触发网络请求，缩短首屏等待时间
 */
class PreFetcher {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    // 已预请求的key集合（用于去重）
    private val preFetchedKeys = ConcurrentHashMap<String, Long>()

    // 预取状态回调
    private val listeners = ConcurrentHashMap<String, MutableList<(String, Boolean) -> Unit>>()

    // ==================== 公共API ====================

    /**
     * 预请求首屏数据
     *
     * 调用时机：
     * - Application.onCreate()
     * - 用户即将进入商城页之前
     *
     * @param key 预请求标识
     * @param fetcher 实际执行请求的回调
     */
    fun preFetch(
        key: String,
        fetcher: suspend () -> String
    ) {
        if (!FeatureToggle.usePreFetch()) {
            TraceLogger.PreFetch.cancel("$key (disabled)")
            return
        }

        // 1. 检查是否已预取（去重）
        if (preFetchedKeys.containsKey(key)) {
            TraceLogger.d("PREFETCH", "跳过重复预取: $key")
            return
        }

        // 2. 检查是否已有缓存
        // （可选：如果缓存有效，可以跳过预取）
        // if (hasValidCache(key)) return

        TraceLogger.PreFetch.start(key)

        // 3. 异步执行预请求
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val result = fetcher()
                val latency = System.currentTimeMillis() - startTime

                // 4. 标记预取完成
                preFetchedKeys[key] = System.currentTimeMillis()

                TraceLogger.PreFetch.done(key, latency)

                // 5. 通知监听器
                notifyListeners(key, true)
            } catch (e: Exception) {
                TraceLogger.e("PREFETCH", "预取失败: $key", e)
                notifyListeners(key, false)
            }
        }
    }

    /**
     * 预取Feed下一页
     *
     * 策略：
     * - 当用户浏览到倒数N条时触发
     * - 或用户停留超过X秒时触发
     */
    fun preFetchNextPage(
        page: Int,
        fetcher: suspend (Int) -> String
    ) {
        val key = "feed_page_$page"
        preFetch(key) {
            fetcher(page)
        }
    }

    /**
     * 延迟预请求（指定毫秒后执行）
     *
     * 用于：进入页面时延迟N秒触发，避免与页面初始化争抢带宽
     */
    fun preFetchDelayed(
        key: String,
        delayMs: Long,
        fetcher: suspend () -> String
    ) {
        handler.postDelayed({
            preFetch(key, fetcher)
        }, delayMs)
    }

    /**
     * 取消预请求
     */
    fun cancel(key: String) {
        preFetchedKeys.remove(key)
        TraceLogger.PreFetch.cancel(key)
    }

    /**
     * 清除所有预取记录
     */
    fun clear() {
        preFetchedKeys.clear()
    }

    /**
     * 获取预取状态
     */
    fun isPreFetched(key: String): Boolean = preFetchedKeys.containsKey(key)

    /**
     * 获取预取数量
     */
    fun preFetchedCount(): Int = preFetchedKeys.size

    // ==================== 监听器 ====================

    /**
     * 添加预取状态监听
     */
    fun addListener(key: String, callback: (key: String, success: Boolean) -> Unit) {
        listeners.getOrPut(key) { mutableListOf() }.add(callback)
    }

    /**
     * 移除监听器
     */
    fun removeListener(key: String) {
        listeners.remove(key)
    }

    private fun notifyListeners(key: String, success: Boolean) {
        listeners[key]?.forEach { callback ->
            handler.post { callback(key, success) }
        }
    }

    /**
     * 清理资源
     */
    fun destroy() {
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        listeners.clear()
    }
}
