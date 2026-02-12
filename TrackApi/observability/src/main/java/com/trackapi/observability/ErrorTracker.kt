/*
 * ErrorTracker.kt
 *
 * 异常捕获 Tracker
 *
 * 面试亮点：
 * 1. 统一异常捕获，避免遗漏
 * 2. 丰富的上下文信息
 * 3. 支持多种异常类型
 */
package com.trackapi.observability

/**
 * 异常捕获 Tracker
 *
 * 职责：
 * 1. 接收异常信息
 * 2. 收集堆栈和上下文
 * 3. 输出到日志
 */
class ErrorTracker : ITracker {

    override fun onError(throwable: Throwable, tags: Map<String, Any?>) {
        // 提取关键信息
        val exceptionInfo = extractExceptionInfo(throwable)

        // 构建结构化错误数据
        val errorData = mapOf(
            "exceptionClass" to exceptionInfo.className,
            "message" to exceptionInfo.message,
            "stackTrace" to exceptionInfo.stackTrace,
            "cause" to (exceptionInfo.causeClassName ?: "none"),
            "tags" to tags
        )

        // 输出错误日志
        StructuredLogger.error(
            type = "error",
            message = "${exceptionInfo.className}: ${exceptionInfo.message}",
            data = errorData
        )
    }

    /**
     * 提取异常关键信息
     *
     * 性能优化：
     * - 只取前 10 行堆栈，避免过长日志
     * - 不使用 Exception.toString()，手动拼接减少开销
     */
    private fun extractExceptionInfo(throwable: Throwable): ExceptionInfo {
        val stackTrace = throwable.stackTraceToString()
            .lines()
            .take(10)
            .joinToString("\n")

        return ExceptionInfo(
            className = throwable.javaClass.simpleName,
            message = throwable.message ?: "Unknown error",
            stackTrace = stackTrace,
            causeClassName = throwable.cause?.javaClass?.simpleName
        )
    }

    /**
     * 异常信息数据类
     */
    private data class ExceptionInfo(
        val className: String,
        val message: String,
        val stackTrace: String,
        val causeClassName: String?
    )
}

/**
 * 错误等级
 */
object ErrorLevel {
    const val FATAL = "FATAL"
    const val ERROR = "ERROR"
    const val WARN = "WARN"
}
