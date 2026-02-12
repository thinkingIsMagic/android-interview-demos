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

/**
 * 优化版View预创建管理器 - Mall Performance Lab
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

    companion object {
        // 预创建View数量限制
        private const val BANNER_LIMIT = 3
        private const val COUPON_LIMIT = 5
        private const val GRID_LIMIT = 6
        private const val FEED_LIMIT = 10

        // 并行度
        private const val PARALLELISM = 2
    }

    // ==================== 组件 ====================

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val inflater: LayoutInflater by lazy {
        LayoutInflater.from(android.app.ApplicationContext())
    }

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
        val count = withContext(Dispatchers.IO) {
            val pool = mutableListOf<View>()
            repeat(BANNER_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_banner_floor, null)
                pool.add(view)
                onProgressCallback?.invoke(it + 1, BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT)
            }
            pool
        }
        viewPools["BannerFloor"] = count
    }

    private suspend fun preWarmCouponPool(context: Context) {
        val count = withContext(Dispatchers.IO) {
            val pool = mutableListOf<View>()
            repeat(COUPON_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_coupon_floor, null)
                pool.add(view)
                onProgressCallback?.invoke(
                    BANNER_LIMIT + it + 1,
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT
                )
            }
            pool
        }
        viewPools["CouponFloor"] = count
    }

    private suspend fun preWarmGridPool(context: Context) {
        val count = withContext(Dispatchers.IO) {
            val pool = mutableListOf<View>()
            repeat(GRID_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_grid_floor, null)
                pool.add(view)
                onProgressCallback?.invoke(
                    BANNER_LIMIT + COUPON_LIMIT + it + 1,
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT
                )
            }
            pool
        }
        viewPools["GridFloor"] = count
    }

    private suspend fun preWarmFeedPool(context: Context) {
        val count = withContext(Dispatchers.IO) {
            val pool = mutableListOf<View>()
            repeat(FEED_LIMIT) {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_feed_product, null)
                pool.add(view)
                onProgressCallback?.invoke(
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + it + 1,
                    BANNER_LIMIT + COUPON_LIMIT + GRID_LIMIT + FEED_LIMIT
                )
            }
            pool
        }
        viewPools["ProductFeed"] = count
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
