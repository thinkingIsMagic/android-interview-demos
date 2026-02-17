package com.mall.perflab.core.cache

import android.util.LruCache
import com.mall.perflab.core.perf.TraceLogger
import java.lang.ref.WeakReference

/**
 * LRU内存缓存（双缓存设计）
 *
 * 使用Android系统的LruCache实现
 *
 * 优化点：缓存热点数据，减少重复计算/网络请求
 *
 * 双缓存设计：
 * 1. LruCache（一级缓存）：强引用，热点数据优先
 * 2. WeakReference（二级缓存）：LruCache淘汰时存入，内存紧张时自动回收
 *
 * 工作流程：
 * 1. put(key, value) → 存入LruCache
 * 2. get(key) → 先查LruCache，未命中查WeakReference
 * 3. LruCache满时 → entryRemoved被调用 → 存入WeakReference
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

    // LruCache（一级缓存）：强引用缓存，热点数据
    private val cache: LruCache<K, V> = object : LruCache<K, V>(maxSize) {
        /**
         * 计算每个条目的大小（默认都算1）
         */
        override fun sizeOf(key: K, value: V): Int = 1

        /**
         * 条目被淘汰时回调
         * 【关键】这里把被淘汰的数据存入弱引用缓存
         */
        override fun entryRemoved(
            evicted: Boolean,
            key: K,
            oldValue: V,
            newValue: V?
        ) {
            if (evicted && oldValue != null) {
                // 一级缓存满了，被淘汰的数据存入二级缓存（弱引用）
                // 这样即使强引用被清除，数据还在弱引用里，内存紧张时会被GC回收
                weakCache[key] = WeakReference(oldValue)
                TraceLogger.Cache.evict("$key (to_weak)")
            }
        }
    }

    // WeakReference（二级缓存）：当一级缓存满了，被淘汰的数据会存入这里
    // 弱引用在内存紧张时会被GC自动回收，起到兜底作用避免OOM
    private val weakCache = mutableMapOf<K, WeakReference<V>>()

    /**
     * 存入缓存（只存一级）
     */
    fun put(key: K, value: V) {
        cache.put(key, value)
        // 注意：WeakReference缓存是在 entryRemoved 中自动存入的，不是这里
        TraceLogger.Cache.save(key.toString())
    }

    /**
     * 从缓存读取
     * 优先查一级，一级没有查二级
     *
     * @return 缓存的值，若不存在返回null
     */
    fun get(key: K): V? {
        // 1. 先查一级缓存（强引用）
        val value = cache.get(key)
        if (value != null) {
            TraceLogger.Cache.hit(key.toString())
            return value
        }

        // 2. 一级没命中，查二级缓存（弱引用）
        val ref = weakCache[key]
        val weakValue = ref?.get()
        if (weakValue != null) {
            TraceLogger.Cache.hit("$key (weak)")
            // 从弱引用升级到强引用，重新存入一级缓存
            put(key, weakValue)
            // 清理二级缓存
            weakCache.remove(key)
            return weakValue
        }

        // 3. 都未命中
        TraceLogger.Cache.miss(key.toString())
        return null
    }

    /**
     * 检查缓存是否存在
     */
    fun contains(key: K): Boolean = cache.get(key) != null || weakCache[key]?.get() != null

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
     * 获取缓存大小（一级+二级）
     */
    fun size(): Int {
        val weakCount = weakCache.count { it.value.get() != null }
        return cache.size() + weakCount
    }

    /**
     * 获取一级缓存大小
     */
    fun firstCacheSize(): Int = cache.size()

    /**
     * 获取二级缓存大小
     */
    fun secondCacheSize(): Int = weakCache.count { it.value.get() != null }

    /**
     * 获取最大容量
     */
    fun maxSize(): Int = cache.maxSize()
}
