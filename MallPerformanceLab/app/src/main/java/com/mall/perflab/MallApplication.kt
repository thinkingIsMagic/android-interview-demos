package com.mall.perflab

import android.app.Application
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.prewarm.ViewPreWarmer

/**
 * Mall Performance Lab Application
 *
 * 初始化时机：
 * - 冷启动时最早执行
 *
 * 初始化内容：
 * 1. 性能监控初始化
 * 2. 预创建View（useViewPreWarm时）
 */
class MallApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 【打点】应用启动开始
        PerformanceTracker.begin("app_cold_start")

        // 优化点：View预创建（在后台线程执行）
        if (FeatureToggle.useViewPreWarm()) {
            // 延迟执行，避免阻塞主线程太长时间
            PerformanceTracker.begin("app_view_prewarm")
            ViewPreWarmer.preWarm(this)
            PerformanceTracker.end("app_view_prewarm", "precreate")
        }

        // 【打点】应用启动完成
        PerformanceTracker.end("app_cold_start", "cold_start")

        // 输出启动性能
        PerformanceTracker.report()
    }
}
