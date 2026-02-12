package com.example.android_demo_ceiling_container_1.data.model

/**
 * 组件类型枚举
 */
enum class ComponentType(val value: String) {
    COUPON("coupon"),
    BANNER("banner"),
    FLOATING_WIDGET("floating_widget"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): ComponentType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
