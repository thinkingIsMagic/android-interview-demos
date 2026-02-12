package com.mall.perflab.core.perf

import android.util.Log

/**
 * 结构化日志输出器
 *
 * 特点：
 * 1. 按类别前缀区分：性能/缓存/预加载/预创建
 * 2. 支持条件编译：Release版本可关闭详细日志
 * 3. 便于grep筛选
 *
 * 使用方式：
 * - PerfLog.i("CACHE", "hit率为30%")
 * - PerfLog.d("PRELOAD", "开始预加载")
 */
object TraceLogger {

    private const val TAG = "MallPerfLab"

    // 日志级别（可动态调整）
    @Volatile
    var logLevel: Int = LEVEL_INFO

    const val LEVEL_DEBUG = 0
    const val LEVEL_INFO = 1
    const val LEVEL_WARN = 2
    const val LEVEL_ERROR = 3

    // ==================== 统一入口 ====================

    /**
     * 输出DEBUG级别日志
     */
    fun d(category: String, message: String, throwable: Throwable? = null) {
        if (logLevel <= LEVEL_DEBUG) {
            logcat(Log.DEBUG, category, message, throwable)
        }
    }

    /**
     * 输出INFO级别日志
     */
    fun i(category: String, message: String, throwable: Throwable? = null) {
        if (logLevel <= LEVEL_INFO) {
            logcat(Log.INFO, category, message, throwable)
        }
    }

    /**
     * 输出WARN级别日志
     */
    fun w(category: String, message: String, throwable: Throwable? = null) {
        if (logLevel <= LEVEL_WARN) {
            logcat(Log.WARN, category, message, throwable)
        }
    }

    /**
     * 输出ERROR级别日志
     */
    fun e(category: String, message: String, throwable: Throwable? = null) {
        logcat(Log.ERROR, category, message, throwable)
    }

    // ==================== 分类便捷方法 ====================

    // 缓存相关
    object Cache {
        fun hit(key: String) = i("CACHE_HIT", "缓存命中: $key")
        fun miss(key: String) = i("CACHE_MISS", "缓存未命中: $key")
        fun save(key: String, size: Int = 0) = i("CACHE_SAVE", "缓存写入: $key (size=$size)")
        fun evict(key: String) = i("CACHE_EVICT", "缓存淘汰: $key")
    }

    // 预请求相关
    object PreFetch {
        fun start(url: String) = d("PREFETCH_START", "开始预请求: $url")
        fun done(url: String, latency: Long) = i("PREFETCH_DONE", "预请求完成: $url (${latency}ms)")
        fun cancel(url: String) = d("PREFETCH_CANCEL", "取消预请求: $url")
    }

    // 预加载相关
    object PreLoad {
        fun start(key: String) = d("PRELOAD_START", "开始预加载: $key")
        fun done(key: String, latency: Long) = i("PRELOAD_DONE", "预加载完成: $key (${latency}ms)")
    }

    // 预创建相关
    object PreWarm {
        fun start(viewType: String) = d("PREWARM_START", "开始预创建: $viewType")
        fun done(viewType: String, count: Int) = i("PREWARM_DONE", "预创建完成: $viewType (count=$count)")
    }

    // 性能相关
    object Perf {
        fun metric(name: String, value: Long, unit: String = "ms") =
            i("PERF_METRIC", "$name = $value $unit")
    }

    // ==================== 内部实现 ====================

    private fun logcat(level: Int, category: String, message: String, throwable: Throwable?) {
        val formatted = "[$category] $message"
        when (level) {
            Log.DEBUG -> {
                if (throwable != null) Log.d(TAG, formatted, throwable) else Log.d(TAG, formatted)
            }
            Log.INFO -> {
                if (throwable != null) Log.i(TAG, formatted, throwable) else Log.i(TAG, formatted)
            }
            Log.WARN -> {
                if (throwable != null) Log.w(TAG, formatted, throwable) else Log.w(TAG, formatted)
            }
            Log.ERROR -> {
                if (throwable != null) Log.e(TAG, formatted, throwable) else Log.e(TAG, formatted)
            }
        }
        // 同时打印到stdout（便于本地开发观察）
        println("${System.currentTimeMillis()} $formatted")
    }

    /**
     * 获取可复用的LogTag（用于标记特定模块）
     */
    fun createTag(prefix: String): String = "[$prefix]"
}
