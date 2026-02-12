/*
 * DemoApplication.kt
 *
 * Demo 应用入口
 *
 * 面试亮点：
 * - 正确的初始化时机：Application.onCreate()
 * - 配置分离：便于调整参数
 */
package com.demo.app

import android.app.Application
import com.trackapi.observability.Observability
import com.trackapi.observability.TrackerConfig

/**
 * Demo 应用
 *
 * 职责：
 * 1. 初始化 Observability 框架
 * 2. 配置采样率和功能开关
 */
class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化可观测性框架
        val config = TrackerConfig(
            samplingRate = 1.0f, // 全量采样（演示用，线上建议 0.1~0.5）
            featureSwitches = mapOf(
                "event_tracking" to true,
                "error_tracking" to true,
                "performance_tracking" to true
            )
        )

        Observability.init(config)

        // 记录启动日志
        Observability.logEvent(
            eventName = "app_start",
            params = mapOf(
                "app_version" to "1.0.0",
                "os_version" to android.os.Build.VERSION.SDK_INT.toString()
            )
        )
    }
}
