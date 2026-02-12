package com.example.android_demo_ceiling_container_1.data.model

/**
 * 开关配置数据模型
 */
data class FeatureConfig(
    val enableAll: Boolean = true,
    val featureFlags: Map<String, FeatureFlag> = emptyMap()
)

/**
 * 单个功能开关
 */
data class FeatureFlag(
    val id: String,
    val enabled: Boolean = true,
    val config: Map<String, Any?> = emptyMap()
)
