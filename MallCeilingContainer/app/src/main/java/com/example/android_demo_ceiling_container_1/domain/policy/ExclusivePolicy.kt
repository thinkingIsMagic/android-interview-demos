package com.example.android_demo_ceiling_container_1.domain.policy

import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.domain.model.ExclusiveResult
import com.example.android_demo_ceiling_container_1.domain.model.ViewState
import com.example.android_demo_ceiling_container_1.util.Logger

/**
 * 互斥裁决策略
 *
 * 负责在多个组件中选择最终展示的组件
 */
class ExclusivePolicy {

    private val logger = Logger.getInstance("ExclusivePolicy")

    /**
     * 从组件列表中选出最优组件
     *
     * @param components 可用组件列表
     * @param currentState 当前视图状态
     * @return 裁决结果
     */
    fun selectComponent(
        components: List<Component>,
        currentState: ViewState
    ): ExclusiveResult {
        if (components.isEmpty()) {
            return ExclusiveResult(
                selectedComponent = null,
                rejectedComponents = emptyList(),
                reason = "组件列表为空"
            )
        }

        logger.d("开始互斥裁决，可选组件数: ${components.size}", "ExclusivePolicy")

        // 过滤出满足可见性条件的组件
        val visibleComponents = filterByVisibility(components, currentState)
        logger.d("满足可见性条件的组件数: ${visibleComponents.size}", "ExclusivePolicy")

        if (visibleComponents.isEmpty()) {
            return ExclusiveResult(
                selectedComponent = null,
                rejectedComponents = components,
                reason = "无满足可见性条件的组件"
            )
        }

        // 按优先级排序，选择最高优先级的组件
        val sorted = visibleComponents.sortedByDescending { it.priority }
        val selected = sorted.firstOrNull()

        val rejected = if (selected != null) {
            sorted.drop(1)
        } else {
            emptyList()
        }

        val reason = if (selected != null) {
            "选中组件: ${selected.id}, 优先级: ${selected.priority}, 场景: ${selected.scene}"
        } else {
            "未选中任何组件"
        }

        logger.d("裁决完成: $reason", "ExclusivePolicy")

        return ExclusiveResult(
            selectedComponent = selected,
            rejectedComponents = rejected,
            reason = reason
        )
    }

    /**
     * 根据可见性策略过滤组件
     */
    private fun filterByVisibility(
        components: List<Component>,
        state: ViewState
    ): List<Component> {
        return components.filter { component ->
            val policy = component.visibility

            // 检查滚动阈值
            val scrollThreshold = policy.scrollThreshold
            val meetsScrollThreshold = if (scrollThreshold > 0) {
                state.scrollOffset >= scrollThreshold
            } else {
                true // 阈值为0表示始终可见
            }

            // 检查滚动方向（简化：仅检查是否向下滚动）
            val directionMatch = when (policy.scrollDirection.lowercase()) {
                "up" -> !state.isAtTop
                "down" -> true // 简化：向下滚动时显示
                else -> true
            }

            // 检查最小停留时长（简化：忽略）
            val durationMatch = true

            meetsScrollThreshold && directionMatch && durationMatch
        }
    }

    companion object {
        @Volatile
        private var instance: ExclusivePolicy? = null

        fun getInstance(): ExclusivePolicy {
            return instance ?: synchronized(this) {
                instance ?: ExclusivePolicy().also { instance = it }
            }
        }
    }
}
