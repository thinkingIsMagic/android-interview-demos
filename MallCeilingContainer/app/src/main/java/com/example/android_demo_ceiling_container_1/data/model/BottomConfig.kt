package com.example.android_demo_ceiling_container_1.data.model

/**
 * 底部配置根模型
 */
data class BottomConfig(
    val version: String = "1.0",
    val enable: Boolean = true,
    val components: List<Component> = emptyList()
) {
    /**
     * 获取启用的组件列表
     */
    fun getEnabledComponents(): List<Component> {
        return components.filter { it.isValid() && it.visible }
    }

    /**
     * 按优先级排序（高优先级在前）
     */
    fun getSortedComponents(): List<Component> {
        return getEnabledComponents().sortedByDescending { it.priority }
    }

    /**
     * 检查配置是否有效
     */
    fun isValid(): Boolean {
        return enable && components.isNotEmpty()
    }
}
