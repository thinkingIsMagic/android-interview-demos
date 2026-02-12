/*
 * MainActivity.kt
 *
 * Demo 主页面
 *
 * 演示 Observability 模块的所有功能：
 * 1. 事件埋点（点击按钮）
 * 2. 异常捕获
 * 3. 性能耗时追踪
 * 4. 采样率演示
 * 5. 降级开关演示
 *
 * 面试亮点：
 * - 展示如何将业务代码接入监控框架
 * - 简洁的 API 设计
 * - 完整的可观测性功能演示
 */
package com.demo.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.trackapi.observability.Observability
import com.trackapi.observability.TrackerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Demo 活动
 *
 * 包含演示区：
 * 1. 事件埋点演示
 * 2. 异常捕获演示
 * 3. 性能追踪演示
 * 4. 采样/降级控制
 * 5. 日志输出区
 */
class MainActivity : AppCompatActivity() {

    private val logTag = "MainActivity"
    private val logMessages = mutableListOf<String>()
    private val logCounter = AtomicInteger(0)

    // UI Components
    private lateinit var logContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var samplingSwitch: Switch
    private lateinit var perfSwitch: Switch
    private lateinit var errorSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建简单的 UI
        val rootView = createUI()
        setContentView(rootView)

        // 记录页面曝光
        Observability.logEvent(
            eventName = "page_view",
            params = mapOf("page_name" to "MainActivity")
        )

        logToScreen("Demo 已启动，观察 Logcat 查看完整日志")
    }

    /**
     * 创建演示 UI
     *
     * 使用原生 View，避免 Compose 依赖
     * 面试亮点：简洁的 UI 代码，专注于功能演示
     */
    private fun createUI(): View {
        val context = this
        val density = context.resources.displayMetrics.density
        val padding = (16 * density).toInt()

        // 根容器
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        // ===== 标题 =====
        val title = TextView(context).apply {
            text = "Observability Mini-Kit Demo"
            textSize = 20f
            setPadding(0, 0, 0, padding)
        }
        rootLayout.addView(title)

        // ===== 按钮区域 =====
        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. 事件埋点按钮
        val eventButton = createDemoButton("1. 事件埋点 (Log Event)") {
            logToScreen("触发事件埋点")
            Observability.logEvent(
                eventName = "button_click",
                params = mapOf(
                    "button_id" to "event_button",
                    "timestamp" to System.currentTimeMillis(),
                    "demo_version" to "1.0"
                )
            )
            logToScreen("  -> 事件已记录: button_click")
        }
        buttonLayout.addView(eventButton)

        // 2. 异常捕获按钮
        val errorButton = createDemoButton("2. 异常捕获 (Log Error)") {
            logToScreen("触发测试异常")
            try {
                throw RuntimeException("Demo 测试异常: ${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Observability.logError(
                    throwable = e,
                    tags = mapOf(
                        "scenario" to "manual_exception",
                        "user_id" to "demo_user_001"
                    )
                )
            }
            logToScreen("  -> 异常已记录到日志")
        }
        buttonLayout.addView(errorButton)

        // 3. 性能追踪按钮
        val perfButton = createDemoButton("3. 性能追踪 (Track Performance)") {
            logToScreen("开始性能追踪...")
            val result = Observability.trackPerformance("mock_network_request") {
                // 模拟网络请求耗时 (500ms)
                Thread.sleep(500)
                mapOf("status" to "success", "data" to listOf(1, 2, 3))
            }
            logToScreen("  -> 耗时: ${result.durationMs}ms, success=${result.success}")
        }
        buttonLayout.addView(perfButton)

        // 4. 协程性能追踪按钮
        val coroutineButton = createDemoButton("4. 协程性能追踪 (Coroutine)") {
            logToScreen("协程任务开始...")
            CoroutineScope(Dispatchers.IO).launch {
                val result = Observability.trackPerformance("coroutine_io_task") {
                    delay(300) // 模拟 IO 耗时
                    "协程任务完成"
                }
                runOnUiThread {
                    logToScreen("  -> 协程耗时: ${result.durationMs}ms")
                }
            }
        }
        buttonLayout.addView(coroutineButton)

        // 5. start/stop 手动追踪
        val manualButton = createDemoButton("5. 手动追踪 (Start/Stop)") {
            logToScreen("手动追踪: 开始")
            Observability.startTrace("manual_trace")

            // 模拟耗时操作
            Thread.sleep(200)

            val duration = Observability.stopTrace("manual_trace")
            logToScreen("  -> 手动追踪耗时: ${duration}ms")
        }
        buttonLayout.addView(manualButton)

        // 6. 展示列表加载
        val listButton = createDemoButton("6. 模拟列表加载") {
            logToScreen("加载模拟数据列表...")
            val result = Observability.trackPerformance("list_loading") {
                // 模拟从网络加载数据
                val data = (1..10).map { "Item $it" }
                delay(150)
                data
            }
            logToScreen("  -> 加载 ${result.data?.size ?: 0} 条数据, 耗时: ${result.durationMs}ms")
        }
        buttonLayout.addView(listButton)

        rootLayout.addView(buttonLayout)

        // ===== 控制开关区域 =====
        val controlTitle = TextView(context).apply {
            text = "\n--- 降级开关 (模拟远端配置) ---"
            textSize = 14f
            setPadding(0, (16 * density).toInt(), 0, (8 * density).toInt())
        }
        rootLayout.addView(controlTitle)

        val controlLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        samplingSwitch = Switch(context).apply {
            text = "采样 10%"
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                val config = TrackerConfig(
                    samplingRate = if (isChecked) 0.1f else 1.0f
                )
                Observability.updateConfig(config)
                logToScreen("采样率已设置为: ${if (isChecked) "10%" else "100%"}")
            }
        }
        controlLayout.addView(samplingSwitch)

        perfSwitch = Switch(context).apply {
            text = "性能追踪"
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                logToScreen("性能追踪开关: $isChecked")
            }
        }
        controlLayout.addView(perfSwitch)

        errorSwitch = Switch(context).apply {
            text = "错误捕获"
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                logToScreen("错误捕获开关: $isChecked")
            }
        }
        controlLayout.addView(errorSwitch)

        rootLayout.addView(controlLayout)

        // 7. 清空日志按钮
        val clearButton = Button(context).apply {
            text = "清空日志"
            setOnClickListener {
                logMessages.clear()
                logContainer.removeAllViews()
                logToScreen("日志已清空")
            }
        }
        rootLayout.addView(clearButton)

        // ===== 日志输出区域 =====
        val logTitle = TextView(context).apply {
            text = "\n--- 屏幕日志输出 ---"
            textSize = 14f
            setPadding(0, (16 * density).toInt(), 0, (8 * density).toInt())
        }
        rootLayout.addView(logTitle)

        // 日志容器
        scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (400 * density).toInt()
            )
        }

        logContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollView.addView(logContainer)
        rootLayout.addView(scrollView)

        return rootLayout
    }

    /**
     * 创建演示按钮
     */
    private fun createDemoButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener {
                try {
                    onClick()
                } catch (e: Exception) {
                    Log.e(logTag, "Demo error", e)
                }
            }
        }
    }

    /**
     * 日志输出到屏幕
     *
     * 面试亮点：实时日志展示，便于 Demo 演示
     */
    private fun logToScreen(message: String) {
        val logId = logCounter.incrementAndGet()
        val timestamp = System.currentTimeMillis()

        // 添加到内存列表
        logMessages.add("[$timestamp] $message")

        // 输出到 Logcat（便于查看完整结构化日志）
        Log.d(logTag, message)

        // 更新 UI
        runOnUiThread {
            val textView = TextView(this).apply {
                text = "[$logId] $message"
                textSize = 11f
                setPadding(0, 2, 0, 2)
            }
            logContainer.addView(textView)

            // 滚动到底部
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }

            // 限制日志数量
            if (logMessages.size > 100) {
                logMessages.removeAt(0)
                logContainer.removeViewAt(0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Observability.logEvent("page_view", mapOf("page" to "MainActivity"))
    }

    override fun onPause() {
        super.onPause()
        Observability.logEvent("page_exit", mapOf("page" to "MainActivity"))
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
