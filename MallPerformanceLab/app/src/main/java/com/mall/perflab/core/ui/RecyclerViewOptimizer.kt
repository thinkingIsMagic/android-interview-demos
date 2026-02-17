package com.mall.perflab.core.ui

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import kotlinx.coroutines.*

/**
 * RecyclerView优化器
 *
 * 【优化原理】
 * 1. Prefetch：提前加载即将进入可视区域的item
 * 2. 布局预计算：提前计算布局，减少setText/测量次数
 * 3. 增量绑定：只更新变化的数据
 *
 * 【效果对比】
 * - 无Prefetch：滚动时临时加载，可能掉帧
 * - 有Prefetch：提前加载，滚动更流畅
 * - 增量绑定：减少setText调用（10ms → 1ms）
 */
object RecyclerViewOptimizer {

    /**
     * RecyclerView配置
     */
    fun configure(recyclerView: RecyclerView) {
        if (FeatureToggle.isOptimized) {
            // 优化点1：启用预取
            recyclerView.setItemViewCacheSize(20)

            // 优化点2：设置DrawingCache
            recyclerView.isDrawingCacheEnabled = true
            recyclerView.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH

            // 优化点3：设置HasFixedSize（如果item大小固定）
            // recyclerView.setHasFixedSize(true)

            // 优化点4：设置LayoutManager优化
            (recyclerView.layoutManager as? LinearLayoutManager)?.apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 4
            }
        }
    }

    /**
     * 创建优化的LayoutManager
     */
    fun createOptimizedLayoutManager(): LinearLayoutManager {
        return object : LinearLayoutManager(null) {
            init {
                // 开启预取
                isItemPrefetchEnabled = true
                // 初始预取数量
                initialPrefetchItemCount = 4
            }

            /**
             * 预取策略
             *
             * 【原理】在布局计算时，提前加载接下来N个item
             * 减少滚动时的临时加载
             */
            override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
                return if (FeatureToggle.isOptimized) {
                    // 多分配200px空间用于预布局
                    200
                } else {
                    super.getExtraLayoutSpace(state)
                }
            }
        }
    }

    /**
     * 滚动状态监听
     */
    fun createScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                when (newState) {
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // 滚动中：暂停图片加载（可选优化）
                        // imageLoader.pause()
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // 停止滚动：恢复图片加载
                        // imageLoader.resume()
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 记录滚动偏移量
                // TraceLogger.d("SCROLL", "dx=$dx, dy=$dy")
            }
        }
    }

    /**
     * LayoutAnimation（首次进入动画）
     *
     * 【原理】用动画替代冷启动时的突兀展示
     */
    fun applyLayoutAnimation(recyclerView: RecyclerView) {
        if (!FeatureToggle.isOptimized) return

        // 示例代码（实际使用需要定义animation资源）
        // recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(
        //     recyclerView.context,
        //     R.anim.layout_animation_fall_down
        // )
    }
}

/**
 * DiffUtil优化
 *
 * ================================================================
 * 【什么是DiffUtil？】
 *
 * DiffUtil是Android RecyclerView库提供的一个工具类。
 * 它用于比较两个列表的差异，计算出：
 * - 哪些item是新增的
 * - 哪些item是删除的
 * - 哪些item是移动位置的
 * - 哪些item内容变化了
 *
 * ================================================================
 * 【为什么需要DiffUtil？】
 *
 * 场景：商品列表数据更新了
 * - 原来的数据：["商品A", "商品B", "商品C"]
 * - 新的数据：["商品A", "商品D", "商品B"]
 *
 * 传统做法：notifyDataSetChanged()
 * - 整个列表全部重绘
 * - 每个Item都执行onBindViewHolder
 * - 性能差，有闪烁感
 *
 * 优化做法：DiffUtil
 * - 自动计算差异：
 *   - "商品C"被删除
 *   - "商品D"是新增
 *   - "商品A"和"商品B"位置变了
 * - 只更新变化的部分
 * - 高效且无闪烁
 *
 * ================================================================
 * 【DiffUtil的工作原理】
 *
 * DiffUtil会调用Callback的4个方法：
 *
 * 1. getOldListSize() - 旧列表长度
 * 2. getNewListSize() - 新列表长度
 *
 * 3. areItemsTheSame(oldPos, newPos)
 *    判断两个位置是否是同一个"Item"
 *    例子：id相同就认为是同一个item
 *
 * 4. areContentsTheSame(oldPos, newPos)
 *    判断同一个item的内容是否变化
 *    例子：比较字段值
 *
 * DiffUtil会找出最小更新序列，生成DiffResult
 * RecyclerView根据DiffResult执行局部更新
 *
 * ================================================================
 * 【使用示例】
 *
 * // 旧数据
 * val oldList = listOf(Product(1, "A"), Product(2, "B"))
 *
 * // 新数据
 * val newList = listOf(Product(1, "A"), Product(3, "C"), Product(2, "B"))
 *
 * // 计算差异
 * val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
 *     override fun getOldListSize() = oldList.size
 *     override fun getNewListSize() = newList.size
 *
 *     override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
 *         return oldList[oldPos].id == newList[newPos].id  // 比较id
 *     }
 *
 *     override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
 *         return oldList[oldPos] == newList[newPos]  // 比较内容
 *     }
 * })
 *
 * // 应用差异
 * adapter.diffDispatchUpdates(result)
 *
 * ================================================================
 * 【注意事项】
 *
 * 1. DiffUtil计算本身是O(n)的，大数据量可能慢
 *    建议在后台线程计算（使用calculateDiffAsync）
 *
 * 2. DiffUtil是"最小更新"，不是"最优移动"
 *    有些场景下移动动画可能不完美
 *
 * 3. 列表很大时，可以考虑只Diff可见区域
 *
 * 【原理】
 * - 全量刷新：notifyDataSetChanged() 重绘所有item
 * - 增量刷新：DiffUtil 只更新变化的部分
 *
 * 【效果】
 * - 10个item全量刷新：~100ms
 * - 1个item变化 DiffUtil：~10ms
 */
object DiffUtilOptimizer {

    /**
     * 计算Diff（主线程版本，用于简单场景）
     */
    fun <T> calculateDiff(
        oldList: List<T>,
        newList: List<T>,
        areItemsTheSame: (T, T) -> Boolean,
        areContentsTheSame: (T, T) -> Boolean
    ): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldList.size

                override fun getNewListSize(): Int = newList.size

                override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                    return areItemsTheSame(oldList[oldPos], newList[newPos])
                }

                override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                    return areContentsTheSame(oldList[oldPos], newList[newPos])
                }
            }
        )
    }

    /**
     * 异步计算Diff（大数据量时使用）
     */
    suspend fun <T> calculateDiffAsync(
        oldList: List<T>,
        newList: List<T>,
        areItemsTheSame: (T, T) -> Boolean,
        areContentsTheSame: (T, T) -> Boolean,
        callback: (DiffUtil.DiffResult) -> Unit
    ) {
        withContext(Dispatchers.Default) {
            val result = calculateDiff(oldList, newList, areItemsTheSame, areContentsTheSame)
            callback(result)
        }
    }
}

/**
 * ViewHolder复用优化
 *
 * 【原则】
 * 1. onBindViewHolder 中只更新必要的数据
 * 2. 不要在onBindViewHolder中做耗时操作
 * 3. 使用 View.setTag() + View.getTag() 传递数据
 */
object ViewHolderOptimizer {

    /**
     * 安全的View绑定（带null检查）
     */
    fun <T> bindView(
        view: android.view.View?,
        data: T?,
        binder: (android.view.View, T) -> Unit
    ) {
        if (view == null) return
        if (data == null) return
        binder(view, data)
    }

    /**
     * ViewType分发
     */
    inline fun <reified T> dispatch(
        item: Any?,
        binder: (T) -> Unit
    ) {
        if (item is T) {
            binder(item)
        }
    }
}

/**
 * 增量更新工具
 *
 * 【原理】计算oldList和newList的差异，只更新变化的position
 */
object PayloadOptimizer {

    /**
     * Payload类型
     */
    object PayloadType {
        const val UPDATE_TEXT = "payload_text"
        const val UPDATE_IMAGE = "payload_image"
        const val UPDATE_STATE = "payload_state"
    }

    /**
     * 构建Payload
     */
    fun buildUpdatePayload(
        type: String,
        data: android.os.Bundle
    ): android.os.Bundle {
        return android.os.Bundle().apply {
            putString("type", type)
            putAll(data)
        }
    }

    /**
     * 应用Payload
     */
    fun applyPayload(
        bundle: android.os.Bundle?,
        applier: (String, android.os.Bundle) -> Unit
    ) {
        if (bundle == null) return
        val type = bundle.getString("type") ?: return
        applier(type, bundle)
    }
}
