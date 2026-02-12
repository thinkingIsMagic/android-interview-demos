package com.example.android_demo_ceiling_container_1.ui.bottom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_demo_ceiling_container_1.performance.PerformanceMonitor
import com.example.android_demo_ceiling_container_1.util.Logger

/**
 * BottomHost - 统一底部容器
 *
 * 核心职责：
 * 1. 接收配置和状态
 * 2. 展示单个组件（互斥）
 * 3. 统一的曝光/点击处理
 * 4. 动画显隐
 */
@Composable
fun BottomHost(
    modifier: Modifier = Modifier,
    viewModel: BottomViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val logger = Logger.getInstance("BottomHost")

    // 动画显隐
    AnimatedVisibility(
        visible = state.shouldShow,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // 渲染选中的组件
            state.selectedComponent?.let { component ->
                logger.d("BottomHost 渲染组件: ${component.id}", "BottomHost")
                BottomComponentRenderer.getInstance().Render(
                    component = component,
                    onClick = {
                        viewModel.processEvent(BottomViewEvent.ComponentClicked(component))
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * BottomHost 简单版本（无 ViewModel）
 */
@Composable
fun SimpleBottomHost(
    modifier: Modifier = Modifier,
    shouldShow: Boolean = true,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = shouldShow,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            content()
        }
    }
}
