package com.example.android_demo_ceiling_container_1.domain.policy

import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.data.model.VisibilityConfig
import com.example.android_demo_ceiling_container_1.domain.model.PageState
import com.example.android_demo_ceiling_container_1.domain.model.ScrollDirection
import com.example.android_demo_ceiling_container_1.domain.model.ViewState
import com.example.android_demo_ceiling_container_1.util.Logger

/**
 * 显隐规则引擎
 *
 * 负责判断组件是否应该显示
 */
class VisibilityPolicy {

    private val logger = Logger.getInstance("VisibilityPolicy")

    /**
     * 检查组件是否应该显示
     *
     * @param component 组件
     * @param viewState 当前视图状态
     * @return true 表示应该显示
     */
    fun shouldShow(component: Component, viewState: ViewState): Boolean {
        // 1. 检查组件基础属性
        if (!component.visible || !component.enable) {
            logger.d("${component.id}: 组件已禁用", "VisibilityPolicy")
            return false
        }

        // 2. 检查显隐策略配置
        val visibility = component.visibility
        return checkVisibilityRules(visibility, viewState)
    }

    /**
     * 检查显隐规则
     */
    private fun checkVisibilityRules(
        config: VisibilityConfig,
        viewState: ViewState
    ): Boolean {
        // 规则1: 滚动阈值检查
        if (!checkScrollThreshold(config.scrollThreshold, viewState.scrollOffset)) {
            logger.d("滚动阈值未满足: 需要${config.scrollThreshold}, 当前${viewState.scrollOffset}", "VisibilityPolicy")
            return false
        }

        // 规则2: 滚动方向检查
        if (!checkScrollDirection(config.scrollDirection, viewState)) {
            logger.d("滚动方向不匹配: ${config.scrollDirection}", "VisibilityPolicy")
            return false
        }

        // 规则3: 页面状态检查
        if (!checkPageState(config.pageStates, viewState)) {
            logger.d("页面状态不匹配", "VisibilityPolicy")
            return false
        }

        // 规则4: 最小停留时长检查
        if (!checkMinDuration(config.minDuration, viewState)) {
            logger.d("停留时长不足", "VisibilityPolicy")
            return false
        }

        return true
    }

    /**
     * 滚动阈值检查
     *
     * scrollThreshold = 0 表示始终显示
     */
    private fun checkScrollThreshold(threshold: Int, currentOffset: Int): Boolean {
        return if (threshold > 0) {
            currentOffset >= threshold
        } else {
            true // 阈值为0表示始终可见
        }
    }

    /**
     * 滚动方向检查
     */
    private fun checkScrollDirection(direction: String, viewState: ViewState): Boolean {
        return when (direction.lowercase()) {
            "up" -> !viewState.isAtTop // 向上滚动时显示（不在顶部）
            "down" -> true // 简化：向下滚动时始终显示
            "both" -> true // 双向都显示
            else -> true
        }
    }

    /**
     * 页面状态检查
     */
    private fun checkPageState(requiredStates: List<String>, viewState: ViewState): Boolean {
        if (requiredStates.isEmpty()) {
            return true
        }

        val currentState = if (viewState.isVisible) {
            PageState.FOREGROUND.name.lowercase()
        } else {
            PageState.BACKGROUND.name.lowercase()
        }

        return requiredStates.any { it.lowercase() == currentState }
    }

    /**
     * 最小停留时长检查
     *
     * 注意：此检查需要在组件显示后一段时间内持续满足
     * 当前简化处理，实际应该记录组件首次可见时间
     */
    private fun checkMinDuration(minDuration: Long, viewState: ViewState): Boolean {
        // 简化：如果最小时长为0，始终满足
        // 实际应该比较组件首次可见时间与当前时间
        return minDuration <= 0
    }

    /**
     * 批量检查多个组件的显隐状态
     */
    fun filterVisibleComponents(
        components: List<Component>,
        viewState: ViewState
    ): List<Component> {
        return components.filter { shouldShow(it, viewState) }
    }

    companion object {
        @Volatile
        private var instance: VisibilityPolicy? = null

        fun getInstance(): VisibilityPolicy {
            return instance ?: synchronized(this) {
                instance ?: VisibilityPolicy().also { instance = it }
            }
        }
    }
}
