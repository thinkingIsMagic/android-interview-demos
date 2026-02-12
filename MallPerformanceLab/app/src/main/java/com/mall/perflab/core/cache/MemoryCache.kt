package com.mall.perflab.core.cache

import android.util.LruCache
import com.mall.perflab.core.perf.TraceLogger
import java.lang.ref.WeakReference

/**
 * LRU内存缓存
 *
 * 使用Android系统的LruCache实现
 *
 * 优化点：缓存热点数据，减少重复计算/网络请求
 */
class MemoryCache<K : Any, V : Any>(
    /**
     * 最大缓存条目数
     */
    maxSize: Int = DEFAULT_MAX_SIZE
) {
    companion object {
        private const val DEFAULT_MAX_SIZE = 100
    }

    // LruCache内部会自动淘汰最近最少使用的条目
    private val cache: LruCache<K, V> = object : LruCache<K, V>(maxSize) {
        /**
         * 计算每个条目的大小（默认都算1）
         */
        override fun sizeOf(key: K, value: V): Int = 1

        /**
         * 条目被淘汰时回调
         */
        override fun entryRemoved(
            evicted: Boolean,
            key: K,
            oldValue: V,
            newValue: V?
        ) {
            if (evicted) {
                TraceLogger.Cache.evict(key.toString())
            }
        }
    }

    // 弱引用缓存（用于存储不可序列化的大对象）
    private val weakCache = mutableMapOf<K, WeakReference<V>>()

    /**
     * 存入缓存
     */
    fun put(key: K, value: V) {
        cache.put(key, value)
        // 同时放入弱引用缓存
        weakCache[key] = WeakReference(value)
        TraceLogger.Cache.save(key.toString())
    }

    /**
     * 从缓存读取
     * @return 缓存的值，若不存在返回null
     */
    fun get(key: K): V? {
        val value = cache.get(key)
        if (value != null) {
            TraceLogger.Cache.hit(key.toString())
            // 移动到最近使用（LRU特性自动处理）
            return value
        }

        // 尝试从弱引用获取
        val ref = weakCache[key]
        val weakValue = ref?.get()
        if (weakValue != null) {
            TraceLogger.Cache.hit("$key (weak)")
            // 升级到强引用
            put(key, weakValue)
            return weakValue
        }

        TraceLogger.Cache.miss(key.toString())
        return null
    }

    /**
     * 检查缓存是否存在
     */
    fun contains(key: K): Boolean = cache.get(key) != null || weakCache.containsKey(key)

    /**
     * 移除指定缓存
     */
    fun remove(key: K): V? {
        weakCache.remove(key)
        return cache.remove(key)
    }

    /**
     * 清空所有缓存
     */
    fun clear() {
        cache.evictAll()
        weakCache.clear()
        TraceLogger.Cache.evict("all")
    }

    /**
     * 获取缓存大小
     */
    fun size(): Int = cache.size()

    /**
     * 获取最大容量
     */
    fun maxSize(): Int = cache.maxSize()
}
