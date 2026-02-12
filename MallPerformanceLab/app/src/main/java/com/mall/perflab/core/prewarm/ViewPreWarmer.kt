package com.mall.perflab.core.prewarm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mall.perflab.R
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger

/**
 * View预创建管理器
 *
 * 功能：
 * 1. 预创建首屏关键View（Banner/Grid等）
 * 2. 预渲染复杂布局
 *
 * 优化点：
 * - useViewPreWarm(): 控制是否启用
 * - 提前创建View对象，避免首帧时频繁inflate
 *
 * 使用方式：
 * - ViewPreWarmer.preWarm(context) // 在Application调用
 * - ViewPreWarmer.getPreWarmedView() // 页面中使用
 */
object ViewPreWarmer {

    // 预创建的View缓存（ViewType -> List<View>）
    private val preWarmedViews = HashMap<String, MutableList<View>>()
    private var isPreWarmed = false

    // ==================== 公共API ====================

    /**
     * 预创建首屏关键View
     *
     * 调用时机：
     * - Application.onCreate()（冷启动场景）
     * - 用户进入商城页前（热启动场景）
     *
     * 预创建的View类型：
     * - BannerFloor: 首页Banner
     * - CouponFloor: 优惠券
     * - GridFloor: 商品网格
     * - FeedItem: Feed列表项
     */
    fun preWarm(context: Context) {
        if (!FeatureToggle.useViewPreWarm()) {
            TraceLogger.PreWarm.start("disabled")
            return
        }

        if (isPreWarmed) {
            TraceLogger.d("PREWARM", "已预热，跳过")
            return
        }

        PerformanceTracker.trace("view_prewarm_all", "precreate") {
            val inflater = LayoutInflater.from(context)

            // 1. 预创建Banner楼层
            preWarmedViews["BannerFloor"] = mutableListOf<View>().apply {
                repeat(3) {
                    add(inflater.inflate(R.layout.item_banner_floor, null))
                }
            }

            // 2. 预创建Coupon楼层
            preWarmedViews["CouponFloor"] = mutableListOf<View>().apply {
                repeat(5) {
                    add(inflater.inflate(R.layout.item_coupon_floor, null))
                }
            }

            // 3. 预创建Grid楼层
            preWarmedViews["GridFloor"] = mutableListOf<View>().apply {
                repeat(6) {
                    add(inflater.inflate(R.layout.item_grid_floor, null))
                }
            }

            // 4. 预创建Feed Item（不同类型）
            preWarmedViews["ProductFeed"] = mutableListOf<View>().apply {
                repeat(5) {
                    add(inflater.inflate(R.layout.item_feed_product, null))
                }
            }

            isPreWarmed = true

            TraceLogger.PreWarm.done("all", preWarmedViews.values.sumOf { it.size })
        }
    }

    /**
     * 获取预创建的View
     *
     * @param viewType View类型
     * @return View对象，若无预创建返回null
     */
    fun getPreWarmedView(viewType: String): View? {
        val list = preWarmedViews[viewType] ?: return null
        return if (list.isNotEmpty()) list.removeAt(0) else null
    }

    /**
     * 归还View到缓存池（支持View复用）
     */
    fun returnView(viewType: String, view: View) {
        // 重置View状态（重要！避免状态残留）
        resetView(view)

        val list = preWarmedViews.getOrPut(viewType) { mutableListOf() }
        // 限制池大小，避免内存无限增长
        if (list.size < 20) {
            list.add(view)
        }
    }

    /**
     * 批量获取多个View
     */
    fun getPreWarmedViews(viewType: String, count: Int): List<View> {
        val list = preWarmedViews[viewType] ?: return emptyList()
        val result = mutableListOf<View>()
        repeat(minOf(count, list.size)) {
            result.add(list.removeAt(0))
        }
        return result
    }

    /**
     * 检查是否已预热
     */
    fun isPreWarmed(): Boolean = isPreWarmed

    /**
     * 清除预热缓存（释放内存）
     */
    fun clear() {
        preWarmedViews.values.forEach { list ->
            list.forEach { view ->
                // View不再使用时， detach from parent
            }
        }
        preWarmedViews.clear()
        isPreWarmed = false
        TraceLogger.d("PREWARM", "缓存已清除")
    }

    /**
     * 获取预热统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "isPreWarmed" to isPreWarmed,
            "totalViews" to preWarmedViews.values.sumOf { it.size },
            "byType" to preWarmedViews.mapValues { it.value.size }
        )
    }

    // ==================== 内部方法 ====================

    /**
     * 重置View状态
     * 确保复用时不会携带旧数据
     */
    private fun resetView(view: View) {
        // TextView内容清空
        if (view is TextView) {
            view.text = ""
            view.tag = null
        }

        // ImageView重置
        if (view is ImageView) {
            view.setImageDrawable(null)
            view.tag = null
        }

        // 递归处理子View
        if (view is LinearLayout || view is android.view.ViewGroup) {
            (0 until view.childCount).forEach { index ->
                resetView(view.getChildAt(index))
            }
        }
    }
}
