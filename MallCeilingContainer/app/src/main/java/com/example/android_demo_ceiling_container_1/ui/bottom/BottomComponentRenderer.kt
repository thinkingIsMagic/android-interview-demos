package com.example.android_demo_ceiling_container_1.ui.bottom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.data.model.ComponentConfig
import com.example.android_demo_ceiling_container_1.data.model.ComponentType
import com.example.android_demo_ceiling_container_1.performance.PerformanceMonitor
import com.example.android_demo_ceiling_container_1.ui.bottom.components.BannerComponent
import com.example.android_demo_ceiling_container_1.ui.bottom.components.CouponComponent
import com.example.android_demo_ceiling_container_1.ui.bottom.components.FloatingWidgetComponent
import com.example.android_demo_ceiling_container_1.util.Logger

/**
 * 组件渲染器
 *
 * 根据组件类型分发渲染
 */
class BottomComponentRenderer(
    private val performanceMonitor: PerformanceMonitor = PerformanceMonitor.getInstance()
) {

    private val logger = Logger.getInstance("BottomComponentRenderer")

    /**
     * 渲染组件
     *
     * @param component 组件数据
     * @param onClick 点击回调
     * @param modifier 修饰符
     */
    @Composable
    fun Render(
        component: Component,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val startTime = System.currentTimeMillis()
        performanceMonitor.incrementCreateViewCount()

        val content = when (component.type) {
            ComponentType.COUPON -> {
                val config = component.config as? ComponentConfig.CouponConfig
                    ?: ComponentConfig.CouponConfig()
                CouponComponent(
                    config = config,
                    onClick = onClick
                )
            }
            ComponentType.BANNER -> {
                val config = component.config as? ComponentConfig.BannerConfig
                    ?: ComponentConfig.BannerConfig()
                BannerComponent(
                    config = config,
                    onClick = onClick
                )
            }
            ComponentType.FLOATING_WIDGET -> {
                val config = component.config as? ComponentConfig.FloatingWidgetConfig
                    ?: ComponentConfig.FloatingWidgetConfig()
                FloatingWidgetComponent(
                    config = config,
                    onClick = onClick
                )
            }
            ComponentType.UNKNOWN -> {
                logger.w("未知组件类型: ${component.id}", "BottomComponentRenderer")
                null
            }
        }

        val duration = System.currentTimeMillis() - startTime
        performanceMonitor.recordOperation("compose_${component.type.value}", duration)

        // 使用 remember 避免重组时重新创建
        remember(component.id) {
            content
        }?.let { composable ->
            composable(modifier)
        }
    }

    companion object {
        @Volatile
        private var instance: BottomComponentRenderer? = null

        fun getInstance(): BottomComponentRenderer {
            return instance ?: synchronized(this) {
                instance ?: BottomComponentRenderer().also { instance = it }
            }
        }
    }
}
