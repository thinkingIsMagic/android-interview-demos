/*
 * TrackerConfig.kt
 *
 * 运行时配置
 *
 * 面试亮点：
 * - 运行时配置 vs 构建时配置：灵活性 vs 性能
 * - 不可变配置（val）保证线程安全
 */
package com.trackapi.observability

/**
 * 运行时配置
 *
 * @property samplingRate 采样率 (0.0 ~ 1.0)，1.0 表示全量
 * @property featureSwitches 功能开关 Map
 */
data class TrackerConfig(
    /**
     * 采样率
     *
     * 面试亮点：采样是控制数据量的关键手段
     * - 0.01 = 1% 采样
     * - 1.0 = 全量
     */
    val samplingRate: Float = 1.0f,

    /**
     * 功能开关
     *
     * 面试亮点：降级策略 - 当监控本身影响性能时，可快速关闭
     * key: 功能名称
     * value: 是否开启
     */
    val featureSwitches: Map<String, Boolean> = mapOf(
        "event_tracking" to true,
        "error_tracking" to true,
        "performance_tracking" to true
    )
) {
    init {
        require(samplingRate in 0.0f..1.0f) { "采样率必须在 0.0 ~ 1.0 之间" }
    }
}

/**
 * 功能开关
 *
 * 简化版的开关访问
 */
object FeatureSwitch {

    private var config = TrackerConfig()

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: TrackerConfig) {
        config = newConfig
    }

    /**
     * 检查功能是否开启
     */
    fun isEnabled(feature: String): Boolean {
        return config.featureSwitches[feature] ?: true
    }

    /**
     * 开启功能
     */
    fun enable(feature: String) {
        config = config.copy(
            featureSwitches = config.featureSwitches.toMutableMap().apply {
                this[feature] = true
            }
        )
    }

    /**
     * 关闭功能
     */
    fun disable(feature: String) {
        config = config.copy(
            featureSwitches = config.featureSwitches.toMutableMap().apply {
                this[feature] = false
            }
        )
    }
}
