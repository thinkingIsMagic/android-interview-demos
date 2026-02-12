package com.example.android_demo_ceiling_container_1.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_demo_ceiling_container_1.data.BottomConfigRepository
import com.example.android_demo_ceiling_container_1.data.model.BottomConfig
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.domain.model.ViewState
import com.example.android_demo_ceiling_container_1.domain.policy.ExclusivePolicy
import com.example.android_demo_ceiling_container_1.performance.PerformanceMonitor
import com.example.android_demo_ceiling_container_1.performance.PerformanceStats
import com.example.android_demo_ceiling_container_1.ui.bottom.BottomViewEvent
import com.example.android_demo_ceiling_container_1.ui.bottom.BottomViewState
import com.example.android_demo_ceiling_container_1.ui.bottom.BottomViewModel
import com.example.android_demo_ceiling_container_1.ui.tracker.Tracker
import com.example.android_demo_ceiling_container_1.util.Logger
import com.example.android_demo_ceiling_container_1.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Demo 页面 ViewModel
 *
 * 包含演示用的状态和操作
 */
class DemoViewModel(
    private val repository: BottomConfigRepository = BottomConfigRepository.getInstance(),
    private val exclusivePolicy: ExclusivePolicy = ExclusivePolicy.getInstance(),
    private val tracker: Tracker = Tracker.getInstance(),
    private val performanceMonitor: PerformanceMonitor = PerformanceMonitor.getInstance()
) : ViewModel() {

    private val logger = Logger.getInstance("DemoViewModel")

    private val _state = MutableStateFlow(DemoState())
    val state: StateFlow<DemoState> = _state.asStateFlow()

    private val bottomViewModel = BottomViewModel(repository, exclusivePolicy, tracker, performanceMonitor)
    val bottomState: StateFlow<BottomViewState> = bottomViewModel.state

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val result = repository.getConfig()
            when (result) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            config = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * 刷新配置
     */
    fun refreshConfig() {
        repository.clearCache()
        loadConfig()
    }

    /**
     * 更新滚动位置
     */
    fun updateScroll(offset: Int, atTop: Boolean) {
        bottomViewModel.processEvent(
            BottomViewEvent.UpdateScroll(offset, atTop)
        )
    }

    /**
     * 切换组件显示
     */
    fun toggleComponentVisibility(componentId: String) {
        _state.update { state ->
            val updatedComponents = state.config?.components?.map { component ->
                if (component.id == componentId) {
                    component.copy(visible = !component.visible)
                } else {
                    component
                }
            } ?: emptyList()

            state.copy(
                config = state.config?.copy(components = updatedComponents)
            )
        }
        // 重新裁决
        triggerExclusive裁决()
    }

    /**
     * 触发互斥裁决
     */
    private fun triggerExclusive裁决() {
        val config = _state.value.config ?: return
        val components = config.getEnabledComponents()
        val currentState = ViewState(isVisible = true)

        val result = exclusivePolicy.selectComponent(components, currentState)
        bottomViewModel.processEvent(BottomViewEvent.LoadConfig)
    }

    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): PerformanceStats {
        return performanceMonitor.getStats()
    }

    /**
     * 获取日志
     */
    fun getLogs(): List<String> {
        return _state.value.logs
    }

    /**
     * 添加日志
     */
    fun addLog(log: String) {
        _state.update {
            val newLogs = (it.logs + log).takeLast(100) // 保留最近100条
            it.copy(logs = newLogs)
        }
    }
}

/**
 * Demo 状态
 */
data class DemoState(
    val config: BottomConfig? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val logs: List<String> = emptyList()
)
