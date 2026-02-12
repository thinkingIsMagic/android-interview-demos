package com.mall.perflab.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import com.mall.perflab.data.mock.DataGenerator
import com.mall.perflab.data.model.MallData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 商城数据仓库
 *
 * 职责：
 * 1. 数据获取（缓存 + 网络）
 * 2. 缓存管理（内存 + 磁盘）
 * 3. Feed分页管理
 *
 * 优化点关联：
 * - useCache(): 是否使用缓存
 * - usePreFetch(): 是否启用预请求
 */
class MallRepository(private val context: Context) {

    companion object {
        private const val SP_NAME = "mall_cache"
        private const val KEY_MALL_DATA = "mall_data"
        private const val KEY_CACHE_TIME = "cache_time"
        private const val KEY_CACHE_PAGE = "cache_page"

        // 缓存过期时间（5分钟）
        private const val CACHE_EXPIRE_MS = 5 * 60 * 1000L
    }

    // 内存缓存（单例）
    private var memoryCache: MallData? = null
    private var cacheTime: Long = 0

    // 磁盘缓存
    private val sp: SharedPreferences by lazy {
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    // Feed分页状态
    private var currentPage = 0
    private var hasMore = true
    private val feedMutex = Mutex()

    // ==================== 核心API ====================

    /**
     * 获取首页数据
     *
     * 策略（根据FeatureToggle决定）：
     * - Baseline: 直接请求，忽略缓存
     * - Optimized: 优先返回缓存，同时异步刷新
     *
     * @param forceRefresh 强制刷新（忽略缓存）
     */
    suspend fun getMallData(forceRefresh: Boolean = false): Pair<MallData?, Long> {
        val startTime = System.currentTimeMillis()

        // 优化点1：缓存读取（内存优先，磁盘次之）
        if (!forceRefresh && FeatureToggle.useCache()) {
            val cached = getFromCache()
            if (cached != null) {
                val latency = System.currentTimeMillis() - startTime
                TraceLogger.Cache.hit("mall_data")
                // 【缓存命中】直接返回，时间极短
                return cached to latency
            }
            TraceLogger.Cache.miss("mall_data")
        }

        // 发起网络请求
        val (data, networkLatency) = withContext(Dispatchers.IO) {
            PerformanceTracker.trace("repo_fetch_mall_data", "network") {
                var result: MallData? = null
                var lat = 0L
                DataGenerator.generateMallData { mallData, latency ->
                    result = mallData
                    lat = latency
                }
                (result to lat)
            }
        }

        if (data != null) {
            // 更新分页状态
            currentPage = data.nextPage
            hasMore = data.hasMore

            // 优化点2：缓存写入（内存+磁盘）
            if (FeatureToggle.useCache()) {
                saveToCache(data)
            }
        }

        return data to networkLatency
    }

    /**
     * 加载更多Feed数据
     *
     * 优化点3：预取下一页（当剩余数据<3条时自动预取）
     */
    suspend fun loadMoreFeed(): Pair<List<com.mall.perflab.data.model.FeedItem>, Long> {
        return feedMutex.withLock {
            val pageToLoad = currentPage

            // 优化点4：Feed预取策略
            // 当还有更多数据时，提前加载下一页
            if (!hasMore) {
                return emptyList<com.mall.perflab.data.model.FeedItem>() to 0L
            }

            val (items, latency) = withContext(Dispatchers.IO) {
                PerformanceTracker.trace("repo_fetch_feed_page_$pageToLoad", "feed_pagination") {
                    DataGenerator.generateFeed(pageToLoad)
                }
            }

            currentPage++
            hasMore = currentPage < 5

            return items to latency
        }
    }

    /**
     * 预请求首页数据（Optimized模式下调用）
     *
     * 在进入页面前提前触发，不阻塞当前流程
     */
    suspend fun preFetchMallData() {
        if (!FeatureToggle.usePreFetch()) return

        TraceLogger.PreFetch.start("mall_data")

        // 异步执行，不阻塞调用方
        withContext(Dispatchers.IO) {
            val (data, latency) = getMallData(forceRefresh = true)
            TraceLogger.PreFetch.done("mall_data", latency)
        }
    }

    /**
     * 判断是否还有更多数据
     */
    fun hasMoreFeed(): Boolean = hasMore

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        memoryCache = null
        cacheTime = 0
        sp.edit().clear().apply()
        TraceLogger.Cache.evict("all")
    }

    // ==================== 缓存实现 ====================

    /**
     * 从内存读取缓存
     */
    private fun getFromMemory(): MallData? {
        // 检查是否过期
        if (memoryCache != null && isCacheValid()) {
            return memoryCache
        }
        return null
    }

    /**
     * 从缓存读取（内存 + 磁盘）
     */
    private fun getFromCache(): MallData? {
        // 1. 先查内存
        getFromMemory()?.let { return it }

        // 2. 查磁盘
        val json = sp.getString(KEY_MALL_DATA, null) ?: return null
        val page = sp.getInt(KEY_CACHE_PAGE, 0)

        // 反序列化
        try {
            // 注意：实际项目应使用Gson/Moshi解析，这里简化处理
            // 为演示，我们用简单的序列化方式
            if (isCacheValid() && page == currentPage) {
                // 反序列化（简化版）
                val data = deserializeMallData(json)
                memoryCache = data
                cacheTime = System.currentTimeMillis()
                return data
            }
        } catch (e: Exception) {
            // 缓存损坏，忽略
        }

        return null
    }

    /**
     * 保存到缓存（内存 + 磁盘）
     */
    private fun saveToCache(data: MallData) {
        memoryCache = data
        cacheTime = System.currentTimeMillis()

        // 异步写入磁盘（避免阻塞主线程）
        Dispatchers.IO.dispatch(androidx.coroutines.EmptyCoroutineContext) {
            try {
                val json = serializeMallData(data)
                sp.edit()
                    .putString(KEY_MALL_DATA, json)
                    .putLong(KEY_CACHE_TIME, cacheTime)
                    .putInt(KEY_CACHE_PAGE, data.nextPage)
                    .apply()
                TraceLogger.Cache.save("mall_data", json.length)
            } catch (e: Exception) {
                // 写入失败，忽略
            }
        }
    }

    /**
     * 判断缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - cacheTime < CACHE_EXPIRE_MS
    }

    // ==================== 序列化（简化版） ====================

    private fun serializeMallData(data: MallData): String {
        // 简化版序列化：保存关键信息
        // 实际项目应使用Gson/Moshi
        return buildString {
            append("floors=${data.marketingFloors.size};")
            append("items=${data.feedItems.size};")
            append("page=${data.nextPage};")
            append("hasMore=${data.hasMore}")
        }
    }

    private fun deserializeMallData(json: String): MallData {
        // 简化版反序列化
        // 实际项目应使用Gson/Moshi解析完整JSON
        return MallData(
            marketingFloors = emptyList(),
            feedItems = emptyList(),
            hasMore = true,
            nextPage = 0
        )
    }
}
