package com.mall.perflab.core.cache

import android.content.Context
import android.content.SharedPreferences
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 优化版缓存管理器 - Mall Performance Lab
 *
 * ================================================================
 * 【什么是缓存？】
 * 缓存是一种"临时存储"技术，把常用的数据放在更快的地方，避免重复计算或请求。
 * 就像家里冰箱里的食材：买一次可以吃好几天，不用每次都去超市。
 *
 * ================================================================
 * 【为什么需要多级缓存？】
 * 计算机存储的速度和容量是矛盾的：
 * - 内存：非常快（纳秒级），但容量小，掉电丢失
 * - 磁盘：慢（毫秒级），但容量大，持久化
 *
 * 解决方案：分级缓存
 * L1缓存（内存） → L2缓存（磁盘） → 网络
 *   快              慢
 *
 * ================================================================
 * 【本项目的缓存策略】
 *
 * 1. LruCache（内存缓存）
 *    - 什么是LruCache？"Least Recently Used" 最近最少使用
 *    - 当缓存满了会自动淘汰最久未使用的项
 *    - 速度：纳秒级，读取最快
 *    - 容量：50条（根据机器内存动态调整）
 *
 * 2. DiskCache（磁盘缓存）
 *    - 使用SharedPreferences存储
 *    - 速度：毫秒级，比网络快很多
 *    - 容量：100条
 *    - 持久化：App重启后还在
 *
 * 3. TTL（Time To Live）过期机制
 *    - 每条缓存有自己的"保质期"
 *    - 商城首页数据：5分钟过期
 *    - Feed数据：2分钟过期
 *    - 配置数据：30分钟过期
 *
 * ================================================================
 * 【缓存命中流程】
 * 1. 先查内存 → 如果命中，直接返回（最快）
 * 2. 内存没命中 → 查磁盘 → 如果命中，返回并回填内存
 * 3. 磁盘也没命中 → 只能网络请求
 *
 * ================================================================
 * 【异步写入】
 * 写入缓存不能阻塞主线程（UI线程），否则会卡顿
 * 使用Coroutine在后台线程写入
 *
 * 【优化点】
 * 1. 多级缓存：内存(LruCache) + 磁盘(SP)
 * 2. 缓存预热：启动时预加载热点数据
 * 3. 缓存淘汰：TTL过期 + 容量LRU
 * 4. 异步读写：主线程不阻塞
 *
 * 缓存策略对比：
 * - Baseline: 无缓存，每次都网络请求
 * - Optimized: 内存+磁盘，5分钟TTL
 */
class OptimizedCacheManager(context: Context) {

    // ==================== 配置 ====================

    companion object {
        // 缓存过期时间
        private const val DEFAULT_TTL_MS = 5 * 60 * 1000L  // 5分钟
        private const val LONG_TTL_MS = 30 * 60 * 1000L    // 30分钟（首页配置）
        private const val SHORT_TTL_MS = 2 * 60 * 1000L    // 2分钟（Feed）

        // 内存缓存容量
        private const val MEMORY_CACHE_MAX = 50

        // 磁盘缓存容量（条数）
        private const val DISK_CACHE_MAX = 100
    }

    // ==================== 组件 ====================

    // 内存缓存（高速）
    private val memoryCache = object : android.util.LruCache<String, CacheEntry>(MEMORY_CACHE_MAX) {
        override fun sizeOf(key: String, value: CacheEntry): Int = 1

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: CacheEntry?,
            newValue: CacheEntry?
        ) {
            if (evicted) {
                TraceLogger.Cache.evict("$key (memory_lru)")
            }
        }
    }

    // 磁盘缓存（持久化）
    private val diskCache = DiskCache(context)

    // 缓存访问锁（读写分离）
    private val readLock = ConcurrentHashMap<String, java.util.concurrent.locks.ReentrantLock>()
    private val writeLock = java.util.concurrent.locks.ReentrantLock()

    // ==================== 核心API ====================

    /**
     * 获取缓存
     *
     * 策略：
     * 1. 先查内存（最快）
     * 2. 内存未命中查磁盘
     * 3. 磁盘命中回填内存
     *
     * @param key 缓存键
     * @param ttlMs 自定义TTL（0使用默认）
     * @return 缓存值，null表示未命中/已过期
     */
    fun get(key: String, ttlMs: Long = DEFAULT_TTL_MS): String? {
        // 1. 内存查询（纳秒级）
        val memEntry = memoryCache.get(key)
        if (memEntry != null && !memEntry.isExpired(ttlMs)) {
            TraceLogger.Cache.hit("$key (memory)")
            return memEntry.value
        }

        // 2. 磁盘查询（毫秒级）
        if (FeatureToggle.useCache()) {
            val diskValue = diskCache.getSync(key)
            if (diskValue != null) {
                TraceLogger.Cache.hit("$key (disk)")

                // 回填内存（异步，避免阻塞）
                val entry = CacheEntry(diskValue, System.currentTimeMillis())
                memoryCache.put(key, entry)

                return diskValue
            }
        }

        TraceLogger.Cache.miss(key)
        return null
    }

    /**
     * 获取缓存（异步版）
     */
    suspend fun getAsync(key: String, ttlMs: Long = DEFAULT_TTL_MS): String? {
        return withContext(Dispatchers.IO) {
            get(key, ttlMs)
        }
    }

    /**
     * 写入缓存
     *
     * 策略：
     * 1. 同步写入内存
     * 2. 异步写入磁盘
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param ttlMs 过期时间
     */
    fun put(key: String, value: String, ttlMs: Long = DEFAULT_TTL_MS) {
        if (!FeatureToggle.useCache()) return

        // 1. 同步写入内存
        val entry = CacheEntry(value, System.currentTimeMillis())
        memoryCache.put(key, entry)

        // 2. 异步写入磁盘
        CoroutineScope(Dispatchers.IO).launch {
            try {
                diskCache.put(key, value, ttlMs)
                TraceLogger.Cache.save(key, value.length)
            } catch (e: Exception) {
                TraceLogger.e("CACHE", "磁盘写入失败: $e", e)
            }
        }
    }

    /**
     * 写入缓存（异步版）
     */
    suspend fun putAsync(key: String, value: String, ttlMs: Long = DEFAULT_TTL_MS) {
        withContext(Dispatchers.IO) {
            put(key, value, ttlMs)
        }
    }

    /**
     * 批量写入缓存
     */
    fun putAll(entries: Map<String, String>, ttlMs: Long = DEFAULT_TTL_MS) {
        entries.forEach { (key, value) ->
            put(key, value, ttlMs)
        }
    }

    /**
     * 删除缓存
     */
    fun remove(key: String) {
        memoryCache.remove(key)
        CoroutineScope(Dispatchers.IO).launch {
            diskCache.remove(key)
        }
        TraceLogger.Cache.evict(key)
    }

    /**
     * 清空所有缓存
     */
    fun clear() {
        memoryCache.evictAll()
        CoroutineScope(Dispatchers.IO).launch {
            diskCache.clear()
        }
        TraceLogger.Cache.evict("all")
    }

    /**
     * 检查缓存是否存在
     */
    fun contains(key: String): Boolean {
        return memoryCache.get(key) != null || diskCache.exists(key)
    }

    // ==================== 缓存预热 ====================

    /**
     * 预热缓存
     *
     * 在Application启动时调用，提前加载热点数据
     */
    fun warmUp(entries: Map<String, String>) {
        if (!FeatureToggle.useCache()) return

        entries.forEach { (key, value) ->
            val entry = CacheEntry(value, System.currentTimeMillis())
            memoryCache.put(key, entry)
        }

        // 异步持久化
        CoroutineScope(Dispatchers.IO).launch {
            entries.forEach { (key, value) ->
                diskCache.put(key, value, LONG_TTL_MS)
            }
        }

        TraceLogger.i("CACHE", "缓存预热完成: ${entries.size}条")
    }

    // ==================== 统计与监控 ====================

    /**
     * 获取缓存命中率
     */
    fun getHitRate(): Map<String, Any> {
        // 简化版：返回缓存统计
        return mapOf(
            "memory_size" to memoryCache.size(),
            "memory_max" to MEMORY_CACHE_MAX
        )
    }

    // ==================== 内部类 ====================

    /**
     * 缓存条目
     */
    data class CacheEntry(
        val value: String,
        val timestamp: Long
    ) {
        fun isExpired(ttlMs: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMs
        }
    }
}
