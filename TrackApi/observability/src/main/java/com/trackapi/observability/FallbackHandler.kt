/*
 * FallbackHandler.kt
 *
 * 异常兜底处理器
 *
 * 设计要点：
 * 1. 监控代码本身不能影响业务
 * 2. catch 所有异常，吞掉并记录
 * 3. 提供上报自身异常的能力（监控的监控）
 *
 * 面试亮点：
 * - 防御性编程：监控框架必须具备自我保护能力
 * - 容错设计：永远不要让监控代码成为业务的问题
 */
package com.trackapi.observability

/**
 * 兜底处理器
 *
 * 职责：
 * 1. 捕获并处理监控代码中的异常
 * 2. 确保异常不会传播到业务代码
 * 3. 可选上报自身异常（监控的监控）
 */
object FallbackHandler {

    private var selfMonitoringEnabled = false

    /**
     * 安全执行代码块
     *
     * 面试亮点：
     * - 防御性编程的典范
     * - 即使监控代码崩溃，业务也不受影响
     */
    inline fun <T> safeExecute(block: () -> T): T? {
        return try {
            block()
        } catch (e: Throwable) {
            // 捕获所有异常，包括 Error
            handleException(e)
            null
        }
    }

    /**
     * 安全执行代码块（无返回值）
     */
    inline fun safeExecute(crossinline block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            handleException(e)
        }
    }

    /**
     * 处理异常
     *
     * 设计要点：
     * 1. 记录到 Logcat（便于调试）
     * 2. 可选上报到监控系统（监控的监控）
     */
     fun handleException(e: Throwable) {
        // 输出到 Logcat
        StructuredLogger.error(
            type = "FallbackHandler",
            message = "Observability internal error",
            data = mapOf(
                "exceptionClass" to e.javaClass.simpleName,
                "message" to e.message,
                "stackTrace" to e.stackTraceToString().take(500)
            )
        )

        // 如果开启了自我监控，可以上报到服务端
        // 生产环境可以接入，这里仅作演示
        if (selfMonitoringEnabled) {
            reportSelfException(e)
        }
    }

    /**
     * 上报自身异常（预留接口）
     */
    private fun reportSelfException(e: Throwable) {
        // 生产环境实现：上报到专门的异常监控系统
        // 例如：Bugly、Sentry
    }

    /**
     * 开启自我监控
     */
    fun enableSelfMonitoring() {
        selfMonitoringEnabled = true
    }

    /**
     * 关闭自我监控
     */
    fun disableSelfMonitoring() {
        selfMonitoringEnabled = false
    }
}
