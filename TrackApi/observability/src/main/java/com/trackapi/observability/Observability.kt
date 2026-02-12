/*
 * Observability.kt
 *
 * 可观测性框架统一入口（Facade 模式）
 *
 * 设计要点：
 * 1. 单例对象，提供全局访问点
 * 2. 内部持有 TrackerRegistry，统一管理所有 Tracker
 * 3. 对外暴露简洁 DSL 风格 API
 * 4. 初始化时自动注册默认 Tracker
 *
 * 面试亮点：
 * - Facade 模式：封装复杂子系统，提供统一接口
 * - 单例 vs 依赖注入：生产环境推荐注入，但单例便于快速演示
 * - DSL 风格 API：提升代码可读性和使用体验
 */
package com.trackapi.observability

/**
 * 可观测性框架入口
 *
 * 使用方式：
 * ```kotlin
 * // 埋点
 * Observability.logEvent("button_click", "submit_order")
 *
 * // 错误
 * Observability.logError(throwable, mapOf("user_id" to 123))
 *
 * // 性能
 * Observability.trackPerformance("api_request") {
 *     // 业务代码
 * }
 * ```
 */
object Observability {

    // 内部注册中心
    private val registry = TrackerRegistry()

    // 运行时配置
    @Volatile
    private var config: TrackerConfig = TrackerConfig()

    /**
     * 初始化框架
     *
     * 应在 Application.onCreate() 中调用
     * 面试亮点：延迟初始化，避免启动时性能损耗
     */
    fun init(config: TrackerConfig = TrackerConfig()) {
        this.config = config

        // 注册默认 Tracker
        registry.register(EventTracker())
        registry.register(ErrorTracker())
        registry.register(PerformanceTracker())

        StructuredLogger.info(
            type = "Observability",
            message = "Observability framework initialized",
            data = mapOf(
                "samplingRate" to config.samplingRate,
                "featureSwitches" to config.featureSwitches
            )
        )
    }

    /**
     * 更新配置（支持动态调整）
     */
    fun updateConfig(newConfig: TrackerConfig) {
        this.config = newConfig
        StructuredLogger.info(
            type = "Observability",
            message = "Config updated",
            data = mapOf("samplingRate" to newConfig.samplingRate)
        )
    }

    /**
     * 记录业务事件
     *
     * @param eventName 事件名称
     * @param params 附加参数
     */
    fun logEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        registry.notifyEvent("event", mapOf("eventName" to eventName) + params)
    }

    /**
     * 记录错误/异常
     *
     * @param throwable 异常对象
     * @param tags 标签信息
     * @param message 额外描述
     */
    fun logError(
        throwable: Throwable,
        tags: Map<String, Any?> = emptyMap(),
        message: String? = null
    ) {
        registry.notifyError(throwable, mapOf("message" to message) + tags)
    }

    /**
     * 记录性能耗时（自动计算）
     *
     * @param traceName 追踪名称
     * @param block 业务代码块
     * @return 性能数据 Result
     */
    inline fun <T> trackPerformance(
        traceName: String,
        tags: Map<String, Any?> = emptyMap(),
        block: () -> T
    ): PerformanceResult<T> {
        val tracker = PerformanceTracker()
        tracker.start(traceName)

        return try {
            val result = block()
            val duration = tracker.stop(traceName)
            PerformanceResult(
                data = result,
                traceName = traceName,
                durationMs = duration,
                success = true
            )
        } catch (e: Exception) {
            val duration = tracker.stop(traceName)
            // 同时记录错误
            logError(e, tags)
            PerformanceResult(
                data = null,
                traceName = traceName,
                durationMs = duration,
                success = false,
                error = e
            )
        }
    }

    /**
     * 手动记录性能耗时（start/stop 模式）
     *
     * 适用于无法用闭包包裹的场景
     */
    fun startTrace(traceName: String) {
        registry.notifyPerformanceStart(traceName)
    }

    fun stopTrace(traceName: String): Long? {
        return registry.notifyPerformanceStop(traceName)
    }
}

/**
 * 性能追踪结果
 */
data class PerformanceResult<T>(
    val data: T?,
    val traceName: String,
    val durationMs: Long,
    val success: Boolean,
    val error: Throwable? = null
)
