package com.example.android_demo_ceiling_container_1.domain.model

import com.example.android_demo_ceiling_container_1.data.model.Component

/**
 * 视图状态
 */
data class ViewState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val scrollOffset: Int = 0,
    val isAtTop: Boolean = true
)

/**
 * 视图策略
 */
data class ViewPolicy(
    val scrollThreshold: Int = 0,
    val scrollDirection: ScrollDirection = ScrollDirection.DOWN,
    val minDurationMs: Long = 0,
    val pageStates: List<PageState> = listOf(PageState.FOREGROUND)
)

enum class ScrollDirection {
    UP, DOWN, BOTH
}

enum class PageState {
    FOREGROUND, BACKGROUND, STOPPED
}

/**
 * 裁决结果
 */
data class ExclusiveResult(
    val selectedComponent: Component?,
    val rejectedComponents: List<Component>,
    val reason: String
)
