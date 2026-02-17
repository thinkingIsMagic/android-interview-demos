package com.mall.perflab.core.prewarm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mall.perflab.R
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 优化版View预创建管理器 - Mall Performance Lab
 *
 * ================================================================
 * 【什么是View预创建？】
 *
 * Android中，View是通过XML布局文件" inflate"（膨胀）创建的。
 * 这个过程很慢，因为需要：
 * 1. 读取XML文件（IO操作）
 * 2. 解析XML标签
 * 3. 创建Java对象（内存分配）
 * 4. 测量布局（计算位置大小）
 * 5. 绘制到屏幕
 *
 * 通常一次inflate需要10-50ms！
 * 列表滚动时频繁inflate会导致卡顿。
 *
 * ================================================================
 * 【预创建的原理】
 *
 * 预先在后台把View创建好，放在"池子"里。
 * 需要用的时候直接从池子取，不需要inflate。
 *
 * 对比：
 * - Baseline：显示时才创建 → 第一次慢
 * - Optimized：启动时就创建 → 每次都快
 *
 * ================================================================
 * 【View复用池原理】
 *
 * RecyclerView的原理：
 * 1. 屏幕显示10个Item，创建10个View
 * 2. 滚动到第11个Item时
 * 3. RecyclerView不会创建新View
 * 4. 而是复用第1个Item的View
 * 5. 只需要绑定新数据，不需要inflate
 *
 * 本项目的预创建就是手动实现这个池子：
 * - 启动App时，在后台预创建View
 * - 首次展示列表时，直接从池子取
 * - 避免首次inflate的开销
 *
 * ================================================================
 * 【预创建的时机】
 *
 * 1. App启动时（onCreate）
 *    - 用户等待时间最长
 *    - 可以做更多预创建
 *
 * 2. 列表滚动时
 *    - 预加载下一个屏幕的Item
 *    - RecyclerView自动处理
 *
 * 3. 页面跳转前
 *    - 预创建下一个页面的View
 *
 * ================================================================
 * 【预创建的注意事项】
 *
 * 1. 内存占用
 *    - 预创建越多，内存占用越大
 *    - 需要限制池子大小
 *
 * 2. 并行创建
 *    - 使用Coroutine多线程并行创建
 *    - 加快速度
 *
 * 3. 状态清理
 *    - 复用View时要清空旧数据
 *    - 否则会显示错误的内容
 *
 * 4. 首屏优先
 *    - 先创建首屏的View
 *    - 非首屏可以延迟创建
 *
 * 【优化点】
 * 1. 并行预创建：多线程同时inflate
 * 2. 分类预创建：按ViewType分类管理
 * 3. 状态重置：复用时自动清理状态
 * 4. 懒加载策略：首屏关键View优先
 *
 * 预创建策略对比：
 * - Baseline: 随用随创建
 * - Optimized: 提前创建+复用池
 */
object OptimizedViewPreWarmer {

    // ==================== 配置 ====================

    // 预创建View数量限制
    private const val BANNER_LIMIT = 3
    private const val COUPON_LIMIT = 5
    private const val GRID_LIMIT = 6
    private const val FEED_LIMIT = 10

    // 并行度
    private const val PARALLELISM = 2

    // ==================== 组件 ====================

    private var context: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 预创建View缓存池（ViewType -> View列表）
    private val viewPools = ConcurrentHashMap<String, MutableList<View>>()

    // 预创建状态
    @Volatile
    private var isPreWarmed = false

    // 预创建进度回调
    private var onProgressCallback: ((Int, Int) -> Unit)? = null

    // ==================== 核心API ====================

    /**
     * 并行预创建View
     *
     * @param context Context
     * @param onProgress 进度回调 (current, total)
     */
    fun preWarm(context: Context, onProgress: ((Int, Int) -> Unit)? = null) {
        if (!FeatureToggle.useViewPreWarm()) {
            TraceLogger.PreWarm.start("disabled")
            return
        }

        if (isPreWarmed) {
            TraceLogger.d("PREWARM", "已预热，跳过")
            return
        }

        onProgressCallback = onProgress

        PerformanceTracker.trace("view_prewarm_all", "precreate") {
            scope.launch {
                // 1. 并行预创建不同类型的View
                val jobs = listOf(
                    async(Dispatchers.IO) { preWarmBannerPool(context) },
                    async(Dispatchers.IO) { preWarmCouponPool(context) },
                    async(Dispatchers.IO) { preWarmGridPool(context) },
                    async(Dispatchers.IO) { preWarmFeedPool(context) }
                )

                // 等待全部完成
                jobs.awaitAll()

                isPreWarmed = true
                onProgressCallback = null

                val totalCount = viewPools.values.sumOf { it.size }
                TraceLogger.PreWarm.done("all", totalCount)
            }
        }
    }

    /**
     * 获取预创建View
     *
     * @param viewType View类型
     * @return View实例
     */
    fun acquire(viewType: String): View? {
        val pool = viewPools[viewType] ?: return null
        return if (pool.isNotEmpty()) pool.removeAt(0) else null
    }

    /**
     * 归还View到池中
     *
     * @param viewType View类型
     * @param view View实例
     */
    fun release(viewType: String, view: View) {
        // 重置View状态
        resetView(view)

        // 放回池中
        val pool = viewPools.getOrPut(viewType) { mutableListOf() }

        // 限制池大小
        val limit = getLimit(viewType)
        if (pool.size < limit) {
            pool.add(view)
        }
    }

    /**
     * 批量获取View
     */
    fun acquireMultiple(viewType: String, count: Int): List<View> {
        val pool = viewPools[viewType] ?: return emptyList()
        val result = mutableListOf<View>()

        repeat(minOf(count, pool.size)) {
            result.add(pool.removeAt(0))
        }

        return result
    }

    /**
     * 检查是否已预热
     */
    fun isReady(): Boolean = isPreWarmed

    /**
     * 清除预热缓存
     */
    fun clear() {
        viewPools.values.forEach { pool ->
            pool.clear()
        }
        viewPools.clear()
        isPreWarmed = false
        TraceLogger.d("PREWARM", "预热缓存已清除")
    }

    /**
     * 获取预热统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "isReady" to isPreWarmed,
            "totalViews" to viewPools.values.sumOf { it.size },
            "byType" to viewPools.mapValues { it.value.size }
        )
    }

    // ==================== 内部实现 ====================

    private suspend fun preWarmBannerPool(context: Context) {
        val pool = withContext(Dispatchers.IO) {
            val poolList = mutableListOf<View>()
            repeat(BANNER_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_banner_floor, null)
                poolList.add(view)
                onProgressCallback?.invoke(it + 1, BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT)
            }
            poolList
        }
        viewPools["BannerFloor"] = pool
    }

    private suspend fun preWarmCouponPool(context: Context) {
        val pool = withContext(Dispatchers.IO) {
            val poolList = mutableListOf<View>()
            repeat(COUPON_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_coupon_floor, null)
                poolList.add(view)
                onProgressCallback?.invoke(
                    BANNER_LIMIT + it + 1,
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT
                )
            }
            poolList
        }
        viewPools["CouponFloor"] = pool
    }

    private suspend fun preWarmGridPool(context: Context) {
        val pool = withContext(Dispatchers.IO) {
            val poolList = mutableListOf<View>()
            repeat(GRID_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_grid_floor, null)
                poolList.add(view)
                onProgressCallback?.invoke(
                    BANNER_LIMIT + COUPON_LIMIT + it + 1,
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT
                )
            }
            poolList
        }
        viewPools["GridFloor"] = pool
    }

    private suspend fun preWarmFeedPool(context: Context) {
        val pool = withContext(Dispatchers.IO) {
            val poolList = mutableListOf<View>()
            repeat(FEED_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_feed_product, null)
                poolList.add(view)
                onProgressCallback?.invoke(
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + it + 1,
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT
                )
            }
            poolList
        }
        viewPools["ProductFeed"] = pool
    }

    /**
     * 重置View状态
     * 确保复用时不携带旧数据
     */
    private fun resetView(view: View) {
        // 清除所有tag
        view.tag = null

        // 递归清理子View
        when (view) {
            is TextView -> {
                view.text = ""
            }
            is ImageView -> {
                view.setImageDrawable(null)
            }
            is ViewGroup -> {
                (0 until view.childCount).forEach { index ->
                    resetView(view.getChildAt(index))
                }
            }
        }
    }

    private fun getLimit(viewType: String): Int {
        return when (viewType) {
            "BannerFloor" -> BANNER_LIMIT
            "CouponFloor" -> COUPON_LIMIT
            "GridFloor" -> GRID_LIMIT
            "ProductFeed" -> FEED_LIMIT
            else -> 10
        }
    }
}
