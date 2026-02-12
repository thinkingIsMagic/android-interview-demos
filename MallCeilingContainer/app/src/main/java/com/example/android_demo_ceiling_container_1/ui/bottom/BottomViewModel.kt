package com.example.android_demo_ceiling_container_1.ui.bottom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_demo_ceiling_container_1.data.BottomConfigRepository
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.domain.model.ViewState
import com.example.android_demo_ceiling_container_1.domain.policy.ExclusivePolicy
import com.example.android_demo_ceiling_container_1.performance.PerformanceMonitor
import com.example.android_demo_ceiling_container_1.performance.PerformanceStats
import com.example.android_demo_ceiling_container_1.ui.tracker.Tracker
import com.example.android_demo_ceiling_container_1.util.Logger
import com.example.android_demo_ceiling_container_1.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * BottomHost ViewModel
 */
class BottomViewModel(
    private val repository: BottomConfigRepository = BottomConfigRepository.getInstance(),
    private val exclusivePolicy: ExclusivePolicy = ExclusivePolicy.getInstance(),
    private val tracker: Tracker = Tracker.getInstance(),
    private val performanceMonitor: PerformanceMonitor = PerformanceMonitor.getInstance()
) : ViewModel() {

    private val logger = Logger.getInstance("BottomViewModel")

    private val _state = MutableStateFlow(BottomViewState())
    val state: StateFlow<BottomViewState> = _state.asStateFlow()

    private var currentScrollOffset = 0
    private var isAtTop = true

    init {
        processEvent(BottomViewEvent.LoadConfig)
    }

    /**
     * 处理事件
     */
    fun processEvent(event: BottomViewEvent) {
        when (event) {
            is BottomViewEvent.LoadConfig -> loadConfig()
            is BottomViewEvent.RefreshConfig -> refreshConfig()
            is BottomViewEvent.UpdateScroll -> updateScroll(event.offset, event.isAtTop)
            is BottomViewEvent.ComponentClicked -> handleComponentClick(event.component)
            is BottomViewEvent.ComponentExposed -> handleComponentExpose(event.component)
            is BottomViewEvent.Dismiss -> dismiss()
        }
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _state.update { it.copy(isConfigLoaded = false, errorMessage = null) }

            val startTime = System.currentTimeMillis()
            val result = repository.getConfig()
            val duration = System.currentTimeMillis() - startTime
            performanceMonitor.recordOperation("loadConfig", duration)

            when (result) {
                is Result.Success -> {
                    val config = result.data
                    _state.update { it.copy(config = config, isConfigLoaded = true) }
                    logger.d("配置加载成功: ${config.components.size} 个组件", "BottomViewModel")
                    performExclusive裁决()
                }
                is Result.Error -> {
                    val message = "配置加载失败: ${result.message}"
                    _state.update { it.copy(errorMessage = message) }
                    logger.e(message, null, "BottomViewModel")
                }
                is Result.Loading -> {
                    _state.update { it.copy(isConfigLoaded = false) }
                }
            }
        }
    }

    private fun refreshConfig() {
        repository.clearCache()
        loadConfig()
    }

    private fun updateScroll(offset: Int, atTop: Boolean) {
        currentScrollOffset = offset
        isAtTop = atTop

        val newViewState = ViewState(
            isVisible = true,
            isLoading = false,
            scrollOffset = offset,
            isAtTop = atTop
        )
        _state.update { it.copy(viewState = newViewState) }

        // 重新执行裁决
        performExclusive裁决()
    }

    private fun performExclusive裁决() {
        val config = _state.value.config ?: return

        val components = config.getEnabledComponents()
        val currentState = _state.value.viewState.copy(
            scrollOffset = currentScrollOffset,
            isAtTop = isAtTop
        )

        val result = exclusivePolicy.selectComponent(components, currentState)

        _state.update {
            it.copy(
                selectedComponent = result.selectedComponent,
                exclusiveResult = result
            )
        }

        // 如果选中了组件，触发曝光
        result.selectedComponent?.let { component ->
            handleComponentExpose(component)
        }
    }

    private fun handleComponentClick(component: Component) {
        logger.d("组件点击: ${component.id}", "BottomViewModel")
        tracker.click(component)
    }

    private fun handleComponentExpose(component: Component) {
        logger.d("组件曝光: ${component.id}", "BottomViewModel")
        tracker.expose(component)
    }

    private fun dismiss() {
        _state.update {
            it.copy(
                selectedComponent = null,
                viewState = it.viewState.copy(isVisible = false)
            )
        }
    }

    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): PerformanceStats {
        return performanceMonitor.getStats()
    }

    /**
     * 重置性能统计
     */
    fun resetPerformanceStats() {
        performanceMonitor.reset()
    }
}
