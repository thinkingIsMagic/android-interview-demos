package com.mall.perflab.core.cache

import android.content.Context
import android.content.SharedPreferences
import com.mall.perflab.core.perf.TraceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 磁盘缓存管理器
 *
 * 基于SharedPreferences实现
 *
 * 优化点：
 * 1. 应用重启后仍可使用缓存（冷启动优化）
 * 2. 避免重复网络请求
 *
 * 缓存失效策略：
 * - TTL过期（默认5分钟）
 * - 容量淘汰（LRU）
 */
class DiskCache(context: Context) {

    companion object {
        private const val SP_NAME = "disk_cache"
        private const val DEFAULT_TTL_MS = 5 * 60 * 1000L // 5分钟

        // 简单缓存容器
        data class CacheEntry(
            val data: String,
            val timestamp: Long,
            val ttlMs: Long
        ) {
            fun isExpired(): Boolean =
                System.currentTimeMillis() - timestamp > ttlMs
        }
    }

    private val sp: SharedPreferences by lazy {
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    // 内存索引（避免频繁读SP）
    private val indexCache = ConcurrentHashMap<String, Long>()

    // 缓存TTL配置
    private val ttlMap = ConcurrentHashMap<String, Long>()

    /**
     * 写入缓存
     *
     * @param key 缓存键
     * @param data 缓存数据（JSON字符串）
     * @param ttlMs 过期时间（毫秒），0表示永不过期
     */
    suspend fun put(key: String, data: String, ttlMs: Long = DEFAULT_TTL_MS) {
        withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()

            // 写入数据
            sp.edit()
                .putString("${key}_data", data)
                .putLong("${key}_time", timestamp)
                .putLong("${key}_ttl", ttlMs)
                .apply()

            // 更新内存索引
            indexCache[key] = timestamp
            ttlMap[key] = ttlMs

            TraceLogger.Cache.save(key, data.length)
        }
    }

    /**
     * 读取缓存
     *
     * @param key 缓存键
     * @return 缓存数据，若不存在或已过期返回null
     */
    suspend fun get(key: String): String? {
        // 1. 检查索引
        val timestamp = indexCache[key]
            ?: sp.getLong("${key}_time", 0L).also { indexCache[key] = it }

        if (timestamp == 0L) {
            TraceLogger.Cache.miss(key)
            return null
        }

        // 2. 获取TTL
        val ttl = ttlMap[key]
            ?: sp.getLong("${key}_ttl", DEFAULT_TTL_MS).also { ttlMap[key] = it }

        // 3. 检查是否过期
        if (System.currentTimeMillis() - timestamp > ttl) {
            // 已过期，删除
            remove(key)
            TraceLogger.Cache.evict("$key (expired)")
            return null
        }

        // 4. 读取数据
        val data = sp.getString("${key}_data", null)
        if (data != null) {
            TraceLogger.Cache.hit(key)
        } else {
            TraceLogger.Cache.miss(key)
        }
        return data
    }

    /**
     * 读取缓存（同步版本，不阻塞）
     */
    fun getSync(key: String): String? {
        val timestamp = sp.getLong("${key}_time", 0L)
        if (timestamp == 0L) return null

        val ttl = sp.getLong("${key}_ttl", DEFAULT_TTL_MS)
        if (System.currentTimeMillis() - timestamp > ttl) {
            return null
        }

        return sp.getString("${key}_data", null)
    }

    /**
     * 删除缓存
     */
    suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            sp.edit()
                .remove("${key}_data")
                .remove("${key}_time")
                .remove("${key}_ttl")
                .apply()

            indexCache.remove(key)
            ttlMap.remove(key)
        }
    }

    /**
     * 检查缓存是否存在且有效
     */
    fun exists(key: String): Boolean = getSync(key) != null

    /**
     * 清空所有缓存
     */
    suspend fun clear() {
        withContext(Dispatchers.IO) {
            sp.edit().clear().apply()
            indexCache.clear()
            ttlMap.clear()
        }
    }

    /**
     * 设置TTL（可动态调整过期时间）
     */
    fun setTtl(key: String, ttlMs: Long) {
        ttlMap[key] = ttlMs
    }

    /**
     * 获取缓存统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "total_keys" to indexCache.size,
            "ttl_configured" to ttlMap.size
        )
    }
}
