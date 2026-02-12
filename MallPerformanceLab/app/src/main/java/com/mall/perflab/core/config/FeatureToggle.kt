package com.mall.perflab.core.config

/**
 * 性能优化开关控制器
 *
 * 用于Baseline vs Optimized版本对比，支持：
 * 1. 全局模式切换
 * 2. 各优化项独立控制
 *
 * 使用方式：
 * - FeatureToggle.isOptimized // 检查是否启用优化
 * - FeatureToggle.useCache     // 是否使用缓存
 */
object FeatureToggle {

    /**
     * 运行模式：基线版 vs 优化版
     * - BASELINE: 原始实现，无优化
     * - OPTIMIZED: 启用全部优化策略
     */
    enum class Mode {
        BASELINE,  // 基线版：对比组
        OPTIMIZED  // 优化版：实验组
    }

    // 当前运行模式（默认基线版，便于首次对比）
    @Volatile
    var currentMode: Mode = Mode.BASELINE

    /**
     * 是否处于优化模式
     * 性能打点时会根据此值分流记录
     */
    val isOptimized: Boolean
        get() = currentMode == Mode.OPTIMIZED

    // ==================== 独立优化开关（仅优化模式下生效） ====================

    object Optimized {
        /** 是否启用缓存（内存+磁盘） */
        var enableCache: Boolean = true

        /** 是否启用预请求（首屏数据提前获取） */
        var enablePreFetch: Boolean = true

        /** 是否启用预加载（图片、资源预加载） */
        var enablePreLoad: Boolean = true

        /** 是否启用View预创建（首屏关键View提前创建） */
        var enableViewPreWarm: Boolean = true

        /** 是否启用图片占位策略（避免白屏） */
        var enableImagePlaceholder: Boolean = true

        /** 是否启用并行初始化（减少启动阻塞） */
        var enableParallelInit: Boolean = true

        /** 是否启用主线程减负（IO到子线程） */
        var enableIoOffMain: Boolean = true
    }

    // ==================== 便捷判断方法 ====================

    /**
     * 判断某项优化是否启用
     * 规则：必须是优化模式 + 对应开关开启
     */
    fun useCache(): Boolean = isOptimized && Optimized.enableCache
    fun usePreFetch(): Boolean = isOptimized && Optimized.enablePreFetch
    fun usePreLoad(): Boolean = isOptimized && Optimized.enablePreLoad
    fun useViewPreWarm(): Boolean = isOptimized && Optimized.enableViewPreWarm
    fun useImagePlaceholder(): Boolean = isOptimized && Optimized.enableImagePlaceholder
    fun useParallelInit(): Boolean = isOptimized && Optimized.enableParallelInit
    fun useIoOffMain(): Boolean = isOptimized && Optimized.enableIoOffMain

    /**
     * 切换到优化模式（用于动态切换对比）
     */
    fun enableOptimized() {
        currentMode = Mode.OPTIMIZED
    }

    /**
     * 切换到基线模式（用于动态切换对比）
     */
    fun enableBaseline() {
        currentMode = Mode.BASELINE
    }

    /**
     * 切换模式并返回新状态
     */
    fun toggleMode(): Mode {
        currentMode = if (currentMode == Mode.BASELINE) Mode.OPTIMIZED else Mode.BASELINE
        return currentMode
    }
}
