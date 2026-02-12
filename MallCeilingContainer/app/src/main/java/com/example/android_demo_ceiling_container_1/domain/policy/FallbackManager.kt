package com.example.android_demo_ceiling_container_1.domain.policy

import com.example.android_demo_ceiling_container_1.data.model.BottomConfig
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.data.model.ComponentConfig
import com.example.android_demo_ceiling_container_1.data.model.ComponentType
import com.example.android_demo_ceiling_container_1.util.Logger

/**
 * 兜底策略管理器
 *
 * 职责：
 * 1. 解析失败时提供默认配置
 * 2. 字段缺失时使用默认值
 * 3. 组件渲染失败时降级
 * 4. 全局开关关闭时提供默认组件
 */
class FallbackManager {

    private val logger = Logger.getInstance("FallbackManager")

    /**
     * 获取兜底配置
     *
     * 当解析失败或配置无效时调用
     */
    fun getFallbackConfig(): BottomConfig {
        logger.w("使用兜底配置", "FallbackManager")
        return BottomConfig(
            version = "1.0.0-fallback",
            enable = true,
            components = listOf(getDefaultCouponComponent())
        )
    }

    /**
     * 获取默认 Coupon 组件
     */
    fun getDefaultCouponComponent(): Component {
        return Component(
            id = "fallback_coupon",
            type = ComponentType.COUPON,
            priority = 1,
            scene = "default",
            visible = true,
            enable = true,
            config = ComponentConfig.CouponConfig(
                title = "默认优惠券",
                amount = 10,
                threshold = 0,
                buttonText = "领取",
                bgColor = "#FF5722"
            ),
            tracker = com.example.android_demo_ceiling_container_1.data.model.TrackerConfig(
                exposureEvent = "fallback_coupon_exposure",
                clickEvent = "fallback_coupon_click"
            )
        )
    }

    /**
     * 获取默认 Banner 组件
     */
    fun getDefaultBannerComponent(): Component {
        return Component(
            id = "fallback_banner",
            type = ComponentType.BANNER,
            priority = 1,
            scene = "default",
            visible = true,
            enable = true,
            config = ComponentConfig.BannerConfig(
                title = "默认Banner",
                link = "",
                height = 120
            )
        )
    }

    /**
     * 组件降级处理
     *
     * 当组件渲染失败时，降级为更简单的组件
     */
    fun downgradeComponent(component: Component): Component {
        logger.w("组件降级: ${component.id}", "FallbackManager")

        return when (component.type) {
            ComponentType.FLOATING_WIDGET -> {
                // Floating 降级为 Coupon
                getDefaultCouponComponent().copy(
                    id = "${component.id}_downgraded",
                    scene = component.scene
                )
            }
            ComponentType.BANNER -> {
                // Banner 降级为 Coupon
                getDefaultCouponComponent().copy(
                    id = "${component.id}_downgraded",
                    scene = component.scene
                )
            }
            ComponentType.COUPON -> {
                // Coupon 保持不变，或可以降级为更简单的占位
                component.copy(
                    config = ComponentConfig.CouponConfig(
                        title = "优惠券（简化版）",
                        amount = 0,
                        buttonText = "领取"
                    )
                )
            }
            else -> getDefaultCouponComponent()
        }
    }

    /**
     * 字段缺失时的默认值
     */
    fun getDefaultValueForField(fieldName: String): Any? {
        return when (fieldName) {
            "priority" -> 0
            "scene" -> "default"
            "visible" -> true
            "enable" -> true
            "title" -> ""
            "amount" -> 0
            "threshold" -> 0
            "scrollThreshold" -> 0
            "minDuration" -> 0L
            else -> null
        }
    }

    /**
     * 检查是否需要使用兜底
     */
    fun shouldUseFallback(config: BottomConfig?): Boolean {
        return config == null || !config.isValid()
    }

    /**
     * 处理异常配置
     */
    fun handleInvalidConfig(invalidConfig: BottomConfig?): BottomConfig {
        if (shouldUseFallback(invalidConfig)) {
            return getFallbackConfig()
        }
        // 即使配置无效，也尝试返回清理后的版本
        return invalidConfig ?: getFallbackConfig()
    }

    companion object {
        @Volatile
        private var instance: FallbackManager? = null

        fun getInstance(): FallbackManager {
            return instance ?: synchronized(this) {
                instance ?: FallbackManager().also { instance = it }
            }
        }
    }
}
