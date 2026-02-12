package com.example.android_demo_ceiling_container_1.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统一日志系统
 *
 * 支持级别控制、Tag 自动生成、格式化输出、性能日志
 *
 * @property tag 日志 Tag 前缀
 * @property level 日志级别
 */
class Logger(
    private val tag: String = "BottomHost",
    private val level: LogLevel = LogLevel.DEBUG
) {

    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private fun getTag(clas
    sName: String): String = "$tag[$className]"

    private fun formatMessage(message: Any?, tag: String): String {
        val time = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        return "$time [$threadName] $tag: $message"
    }

    fun v(message: Any?, className: String = "") {
        if (level <= LogLevel.VERBOSE) {
            Log.v(getTag(className), formatMessage(message, getTag(className)))
        }
    }

    fun d(message: Any?, className: String = "") {
        if (level <= LogLevel.DEBUG) {
            Log.d(getTag(className), formatMessage(message, getTag(className)))
        }
    }

    fun i(message: Any?, className: String = "") {
        if (level <= LogLevel.INFO) {
            Log.i(getTag(className), formatMessage(message, getTag(className)))
        }
    }

    fun w(message: Any?, className: String = "") {
        if (level <= LogLevel.WARN) {
            Log.w(getTag(className), formatMessage(message, getTag(className)))
        }
    }

    fun e(message: Any?, throwable: Throwable? = null, className: String = "") {
        if (level <= LogLevel.ERROR) {
            val fullMessage = formatMessage(message, getTag(className))
            if (throwable != null) {
                Log.e(getTag(className), fullMessage, throwable)
            } else {
                Log.e(getTag(className), fullMessage)
            }
        }
    }

    /**
     * 性能日志标记
     */
    fun performance(tag: String, operation: String, durationMs: Long) {
        i("[$tag] $operation completed in ${durationMs}ms", tag)
    }

    /**
     * 结构性日志
     * 把事件信息格式化成统一结构，便于后续分析（如埋点平台）
     * Logger.event("button_click", mapOf(
            "button_name" to "submit_order",
            "page" to "CartActivity"
        ))
     */
    fun event(eventName: String, params: Map<String, Any?>) {
        val paramsStr = params.entries.joinToString(", ", "{", "}") {
            "${it.key}=${it.value}"
        }
        i("EVENT: $eventName $paramsStr", "Tracker")
    }

    companion object {
        private const val DEFAULT_TAG = "BottomHost"
        // 双重检查锁需要 volatile 关键字
        @Volatile
        private var instance: Logger? = null

        fun getInstance(tag: String = DEFAULT_TAG): Logger {
            return instance ?: synchronized(this) {
                instance ?: Logger(tag).also { instance = it }
            }
        }

        fun v(message: Any?, tag: String = DEFAULT_TAG) =
            getInstance(tag).v(message)

        fun d(message: Any?, tag: String = DEFAULT_TAG) =
            getInstance(tag).d(message)

        fun i(message: Any?, tag: String = DEFAULT_TAG) =
            getInstance(tag).i(message)

        fun w(message: Any?, tag: String = DEFAULT_TAG) =
            getInstance(tag).w(message)

        fun e(message: Any?, throwable: Throwable? = null, tag: String = DEFAULT_TAG) =
            getInstance(tag).e(message, throwable)

        fun performance(tag: String, operation: String, durationMs: Long) =
            getInstance(tag).performance(tag, operation, durationMs)

        fun event(eventName: String, params: Map<String, Any?>) =
            getInstance().event(eventName, params)
    }
}
