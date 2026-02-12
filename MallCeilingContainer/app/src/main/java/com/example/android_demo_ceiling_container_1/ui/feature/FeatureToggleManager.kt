package com.example.android_demo_ceiling_container_1.ui.feature

import com.example.android_demo_ceiling_container_1.domain.policy.FeatureToggle
import com.example.android_demo_ceiling_container_1.domain.policy.FeatureToggleManager
import com.example.android_demo_ceiling_container_1.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 开关 UI 状态
 */
data class FeatureToggleState(
    val toggles: Map<String, FeatureToggle> = emptyMap(),
    val enableAll: Boolean = true
)

/**
 * 开关管理 Composable 用的状态Holder
 */
class FeatureToggleManagerUI {

    private val logger = Logger.getInstance("FeatureToggleManagerUI")

    private val manager = FeatureToggleManager.getInstance()

    private val _state = MutableStateFlow(FeatureToggleState())
    val state: StateFlow<FeatureToggleState> = _state.asStateFlow()

    init {
        // 注册变更监听
        manager.addListener { featureId, enabled ->
            updateState()
        }
        updateState()
    }

    private fun updateState() {
        _state.value = FeatureToggleState(
            toggles = manager.getAllToggles(),
            enableAll = true // 从 manager 获取
        )
    }

    /**
     * 检查功能是否启用
     */
    fun isEnabled(featureId: String): Boolean {
        return manager.isEnabled(featureId)
    }

    /**
     * 切换功能开关
     */
    fun toggle(featureId: String) {
        val current = manager.isEnabled(featureId)
        manager.updateToggle(featureId, !current)
        logger.d("开关切换: $featureId = ${!current}", "FeatureToggleManagerUI")
    }

    /**
     * 批量设置开关
     */
    fun setToggles(toggles: Map<String, Boolean>) {
        toggles.forEach { (id, enabled) ->
            manager.updateToggle(id, enabled)
        }
    }

    /**
     * 重置所有开关
     */
    fun reset() {
        manager.reset()
        updateState()
        logger.d("所有开关已重置", "FeatureToggleManagerUI")
    }

    companion object {
        @Volatile
        private var instance: FeatureToggleManagerUI? = null

        fun getInstance(): FeatureToggleManagerUI {
            return instance ?: synchronized(this) {
                instance ?: FeatureToggleManagerUI().also { instance = it }
            }
        }
    }
}
