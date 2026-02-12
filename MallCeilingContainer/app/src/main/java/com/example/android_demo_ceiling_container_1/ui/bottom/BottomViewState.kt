package com.example.android_demo_ceiling_container_1.ui.bottom

import com.example.android_demo_ceiling_container_1.data.model.BottomConfig
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.domain.model.ExclusiveResult
import com.example.android_demo_ceiling_container_1.domain.model.ViewState

/**
 * BottomHost ViewState
 */
data class BottomViewState(
    val config: BottomConfig? = null,
    val selectedComponent: Component? = null,
    val exclusiveResult: ExclusiveResult? = null,
    val viewState: ViewState = ViewState(),
    val isConfigLoaded: Boolean = false,
    val errorMessage: String? = null
) {
    val shouldShow: Boolean
        get() = selectedComponent != null && viewState.isVisible

    val componentCount: Int
        get() = config?.components?.size ?: 0
}

/**
 * BottomHost 事件
 */
sealed class BottomViewEvent {
    data object LoadConfig : BottomViewEvent()
    data object RefreshConfig : BottomViewEvent()
    data class UpdateScroll(val offset: Int, val isAtTop: Boolean) : BottomViewEvent()
    data class ComponentClicked(val component: Component) : BottomViewEvent()
    data class ComponentExposed(val component: Component) : BottomViewEvent()
    data object Dismiss : BottomViewEvent()
}

/**
 * 性能统计
 */
data class PerformanceStats(
    val createViewCount: Int = 0,
    val composeCount: Int = 0,
    val lastRenderTimeMs: Long = 0,
    val totalRenderTimeMs: Long = 0
)
