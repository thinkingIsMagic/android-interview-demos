/*
 * EventTracker.kt
 *
 * 事件埋点 Tracker
 *
 * 面试亮点：
 * 1. 轻量实现，零额外依赖
 * 2. 结构化输出，便于后续分析
 */
package com.trackapi.observability

/**
 * 事件埋点 Tracker
 *
 * 职责：
 * 1. 接收业务事件
 * 2. 转换为统一格式
 * 3. 输出到 StructuredLogger
 */
class EventTracker : ITracker {

    override fun onEvent(type: String, data: Map<String, Any?>) {
        // 结构化输出事件
        StructuredLogger.info(
            type = "event",
            message = type,
            data = data
        )
    }
}

/**
 * 事件 Builder
 *
 * 提供 DSL 风格的事件构建
 */
object EventBuilder {

    /**
     * 构建事件数据
     */
    fun build(
        name: String,
        params: Map<String, Any?> = emptyMap()
    ): Pair<String, Map<String, Any?>> {
        return "custom_event" to mapOf("eventName" to name) + params
    }
}
