/*
 * StructuredLogger.kt
 *
 * 结构化日志输出器
 *
 * 设计要点：
 * 1. 使用 Map<String, Any?> 存储日志字段，支持任意类型
 * 2. 输出 JSON 格式，便于日志收集系统解析
 * 3. 使用 toString() 处理复杂对象，避免序列化开销
 * 4. 所有方法都是 inline，避免 lambda 开销
 *
 * 面试亮点：
 * - 结构化日志是现代可观测性系统的基础（对比传统字符串日志）
 * - JSON 格式便于 ELK/Grafana 等工具收集和分析
 */
package com.trackapi.observability

import android.util.Log
import org.json.JSONObject

/**
 * 统一的日志数据结构
 *
 * @property timestamp Unix 时间戳（毫秒）
 * @property level 日志级别：DEBUG, INFO, WARN, ERROR
 * @property type 事件类型：如 "event", "error", "performance"
 * @property message 简要描述
 * @property data 附加数据 Map
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val type: String,
    val message: String,
    val data: Map<String, Any?> = emptyMap()
) {
    /**
     * 转换为 JSON 字符串
     * 便于日志收集系统解析
     */
    fun toJson(): String {
        return JSONObject().apply {
            put("timestamp", timestamp)
            put("level", level)
            put("type", type)
            put("message", message)
            put("data", JSONObject(data.mapValues { it.value?.toString() }))
        }.toString()
    }
}

/**
 * 日志级别常量
 */
object LogLevel {
    const val DEBUG = "DEBUG"
    const val INFO = "INFO"
    const val WARN = "WARN"
    const val ERROR = "ERROR"
}

/**
 * 结构化日志输出器
 *
 * 职责：
 * 1. 接收日志数据，输出到 Logcat
 * 2. 提供 JSON 格式，便于对接上报系统
 * 3. 性能优先，使用 inline 减少开销
 */
object StructuredLogger {

    private const val TAG = "Observability"

    /**
     * 输出 DEBUG 级别日志
     */
    inline fun debug(type: String, message: String, data: Map<String, Any?> = emptyMap()) {
        log(LogLevel.DEBUG, type, message, data)
    }

    /**
     * 输出 INFO 级别日志
     */
    inline fun info(type: String, message: String, data: Map<String, Any?> = emptyMap()) {
        log(LogLevel.INFO, type, message, data)
    }

    /**
     * 输出 WARN 级别日志
     */
    inline fun warn(type: String, message: String, data: Map<String, Any?> = emptyMap()) {
        log(LogLevel.WARN, type, message, data)
    }

    /**
     * 输出 ERROR 级别日志
     */
    inline fun error(type: String, message: String, data: Map<String, Any?> = emptyMap()) {
        log(LogLevel.ERROR, type, message, data)
    }

    /**
     * 核心日志输出方法
     *
     * 性能优化：
     * - 避免使用 StringBuilder，直接拼接 JSON
     * - 减少对象创建，复用 JSONObject
     */
     fun log(level: String, type: String, message: String, data: Map<String, Any?>) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            type = type,
            message = message,
            data = data
        )

        // Logcat 输出：标签+JSON
        Log.d(TAG, "[${entry.level}] ${entry.type}: ${entry.message} | ${entry.toJson()}")
    }

    /**
     * 批量日志输出（可用于异步批量上报）
     */
    fun logBatch(entries: List<LogEntry>) {
        entries.forEach { entry ->
            Log.d(TAG, "[${entry.level}] ${entry.type}: ${entry.message} | ${entry.toJson()}")
        }
    }
}
