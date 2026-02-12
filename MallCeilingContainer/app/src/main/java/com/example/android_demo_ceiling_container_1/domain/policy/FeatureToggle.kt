package com.example.android_demo_ceiling_container_1.domain.policy

import com.example.android_demo_ceiling_container_1.util.Logger

/**
 * 远端开关配置
 *
 * 支持动态下发开关，控制功能启用/禁用
 */
data class FeatureToggle(
    val featureId: String,
    val enabled: Boolean = true,
    val config: Map<String, Any?> = emptyMap()
)

/**
 * 开关配置 DTO（用于 JSON 解析）
 */
data class FeatureToggleDto(
    val featureId: String,
    val enabled: Boolean = true,
    val config: Map<String, Any?>? = null
)

/**
 * 开关配置容器
 */
data class FeatureConfig(
    val version: String = "1.0",
    val enableAll: Boolean = true,
    val toggles: Map<String, FeatureToggle> = emptyMap()
)

/**
 * 开关管理器
 */
class FeatureToggleManager {

    private val logger = Logger.getInstance("FeatureToggleManager")

    // 本地开关配置
    private var localConfig: FeatureConfig = FeatureConfig()

    // 开关变更监听器
    private val listeners = mutableListOf<(String, Boolean) -> Unit>()

    /**
     * 解析并应用开关配置
     */
    fun parseAndApply(configJson: String): Boolean {
        return try {
            // 简化：直接解析开关列表
            val toggles = parseConfig(configJson)
            applyConfig(toggles)
            true
        } catch (e: Exception) {
            logger.e("开关配置解析失败: ${e.message}", e, "FeatureToggleManager")
            false
        }
    }

    /**
     * 解析开关配置（简化实现）
     */
    private fun parseConfig(json: String): Map<String, FeatureToggle> {
        // 实际应该用 JSON 解析库
        // 这里简化处理，返回默认开关
        return mapOf(
            "bottom_host" to FeatureToggle("bottom_host", true),
            "coupon_component" to FeatureToggle("coupon_component", true),
            "banner_component" to FeatureToggle("banner_component", true),
            "floating_widget_component" to FeatureToggle("floating_widget_component", true),
            "performance_monitor" to FeatureToggle("performance_monitor", true),
            "tracker" to FeatureToggle("tracker", true)
        )
    }

    /**
     * 应用开关配置
     */
    private fun applyConfig(toggles: Map<String, FeatureToggle>) {
        localConfig = FeatureConfig(
            version = localConfig.version,
            enableAll = true,
            toggles = toggles
        )
        logger.d("开关配置已应用: ${toggles.size} 个开关", "FeatureToggleManager")
    }

    /**
     * 检查开关是否启用
     */
    fun isEnabled(featureId: String): Boolean {
        // 检查全局开关
        if (!localConfig.enableAll) {
            logger.d("全局开关已关闭", "FeatureToggleManager")
            return false
        }

        // 检查具体开关
        val toggle = localConfig.toggles[featureId]
        return toggle?.enabled ?: true
    }

    /**
     * 获取开关配置
     */
    fun getToggle(featureId: String): FeatureToggle? {
        return localConfig.toggles[featureId]
    }

    /**
     * 获取所有开关
     */
    fun getAllToggles(): Map<String, FeatureToggle> {
        return localConfig.toggles.toMap()
    }

    /**
     * 动态更新开关
     */
    fun updateToggle(featureId: String, enabled: Boolean) {
        val toggles = localConfig.toggles.toMutableMap()
        val oldToggle = toggles[featureId]
        if (oldToggle != null) {
            toggles[featureId] = oldToggle.copy(enabled = enabled)
        } else {
            toggles[featureId] = FeatureToggle(featureId, enabled)
        }

        localConfig = localConfig.copy(toggles = toggles)

        // 通知监听器
        notifyListeners(featureId, enabled)

        logger.d("开关更新: $featureId = $enabled", "FeatureToggleManager")
    }

    /**
     * 注册开关变更监听
     */
    fun addListener(listener: (String, Boolean) -> Unit) {
        listeners.add(listener)
    }

    /**
     * 移除监听
     */
    fun removeListener(listener: (String, Boolean) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * 通知监听器
     */
    private fun notifyListeners(featureId: String, enabled: Boolean) {
        listeners.forEach { it(featureId, enabled) }
    }

    /**
     * 重置所有开关
     */
    fun reset() {
        localConfig = FeatureConfig()
        logger.d("所有开关已重置", "FeatureToggleManager")
    }

    /**
     * 获取开关版本
     */
    fun getVersion(): String = localConfig.version

    companion object {
        @Volatile
        private var instance: FeatureToggleManager? = null

        fun getInstance(): FeatureToggleManager {
            return instance ?: synchronized(this) {
                instance ?: FeatureToggleManager().also { instance = it }
            }
        }
    }
}
