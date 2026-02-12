/*
 * TrackerRegistry.kt
 *
 * Tracker 注册中心
 *
 * 设计要点：
 * 1. 持有所有 Tracker 的引用，支持动态注册/注销
 * 2. 统一分发事件到所有 Tracker
 * 3. 集成采样和降级逻辑
 *
 * 面试亮点：
 * - 注册表模式（Registry Pattern）：管理一组可扩展的对象
 * - 观察者模式变体：事件发布到多个订阅者
 */
package com.trackapi.observability

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Tracker 注册中心
 *
 * 职责：
 * 1. 管理 Tracker 列表
 * 2. 统一分发事件
 * 3. 应用采样和降级策略
 *
 * 面试亮点：
 * - CopyOnWriteArrayList：线程安全的注册表实现
 * - 策略模式：采样、降级、兜底策略可替换
 */
class TrackerRegistry {

    // 使用 CopyOnWriteArrayList 保证线程安全
    // 读多写少场景下性能优秀
    private val trackers = CopyOnWriteArrayList<ITracker>()

    // 采样管理器
    private val samplingManager = SamplingManager()

    // 降级开关
    private val featureSwitch = FeatureSwitch()

    // Fallback 处理器
    private val fallbackHandler = FallbackHandler()

    // 正在进行的性能追踪
    private val activeTraces = ConcurrentHashMap<String, Long>()

    /**
     * 注册 Tracker
     *
     * @param tracker Tracker 实例
     */
    fun register(tracker: ITracker) {
        if (!trackers.contains(tracker)) {
            trackers.add(tracker)
            StructuredLogger.debug("TrackerRegistry", "Tracker registered", mapOf(
                "tracker" to tracker.javaClass.simpleName
            ))
        }
    }

    /**
     * 注销 Tracker
     *
     * @param tracker Tracker 实例
     */
    fun unregister(tracker: ITracker) {
        trackers.remove(tracker)
    }

    /**
     * 通知所有 Tracker（事件）
     *
     * @param type 事件类型
     * @param data 事件数据
     */
    fun notifyEvent(type: String, data: Map<String, Any?>) {
        // 采样决策
        if (!samplingManager.shouldSample(type)) {
            return
        }

        // 降级检查
        if (!featureSwitch.isEnabled(type)) {
            return
        }

        // Fallback 保护
        fallbackHandler.safeExecute {
            trackers.forEach { tracker ->
                tracker.onEvent(type, data)
            }
        }
    }

    /**
     * 通知所有 Tracker（错误）
     *
     * @param throwable 异常对象
     * @param tags 标签信息
     */
    fun notifyError(throwable: Throwable, tags: Map<String, Any?>) {
        // 降级检查 - 错误追踪默认开启
        if (!featureSwitch.isEnabled("error_tracking")) {
            return
        }

        // Fallback 保护
        fallbackHandler.safeExecute {
            trackers.forEach { tracker ->
                tracker.onError(throwable, tags)
            }
        }
    }

    /**
     * 通知性能开始
     *
     * @param traceName 追踪名称
     * @return 开始时间戳
     */
    fun notifyPerformanceStart(traceName: String): Long {
        if (!featureSwitch.isEnabled("performance_tracking")) {
            return -1
        }

        val startTime = System.currentTimeMillis()
        activeTraces[traceName] = startTime

        fallbackHandler.safeExecute {
            trackers.forEach { tracker ->
                tracker.onPerformanceStart(traceName, startTime)
            }
        }

        return startTime
    }

    /**
     * 通知性能结束
     *
     * @param traceName 追踪名称
     * @return 耗时（毫秒），未找到返回 null
     */
    fun notifyPerformanceStop(traceName: String): Long? {
        if (!featureSwitch.isEnabled("performance_tracking")) {
            return null
        }

        val startTime = activeTraces.remove(traceName) ?: return null
        val duration = System.currentTimeMillis() - startTime

        fallbackHandler.safeExecute {
            trackers.forEach { tracker ->
                tracker.onPerformanceStop(traceName, startTime, duration)
            }
        }

        return duration
    }

    /**
     * 获取正在追踪的记录数
     */
    fun getActiveTraceCount(): Int = activeTraces.size

    /**
     * 获取所有 Tracker（用于测试）
     */
    fun getTrackers(): List<ITracker> = trackers.toList()

    /**
     * 更新采样率
     */
    fun setSamplingRate(type: String, rate: Float) {
        samplingManager.setSamplingRate(type, rate)
    }

    /**
     * 设置功能开关
     */
    fun setFeatureEnabled(feature: String, enabled: Boolean) {
        if (enabled) {
            featureSwitch.enable(feature)
        } else {
            featureSwitch.disable(feature)
        }
    }
}
