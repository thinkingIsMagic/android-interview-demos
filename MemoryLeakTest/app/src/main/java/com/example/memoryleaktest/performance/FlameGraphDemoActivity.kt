package com.example.memoryleaktest.performance

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.memoryleaktest.R

/**
 * 演示主线程卡顿与火焰图分析
 *
 * 火焰图（Flame Graph）是一种性能分析可视化工具
 * - 横向表示调用栈的占比
 * - 纵向表示调用深度
 * - 颜色通常表示类别（如 CPU 时间、内存分配等）
 *
 * 本示例演示：
 * 1. 在主线程执行耗时操作导致卡顿
 * 2. 使用 Android Profiler 的 CPU Profiler 录制并查看火焰图
 */
class FlameGraphDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FlameGraphDemo"
    }

    private lateinit var statusText: TextView

    // 使用 Handler 模拟异步任务
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flame_graph_demo)

        statusText = findViewById(R.id.statusText)
        val btnHeavyTask = findViewById<Button>(R.id.btnHeavyTask)
        val btnMultipleTasks = findViewById<Button>(R.id.btnMultipleTasks)
        val btnFinish = findViewById<Button>(R.id.btnFinish)

        // 单个耗时任务
        btnHeavyTask.setOnClickListener {
            statusText.text = "执行耗时任务中...\n点击 Profiler -> CPU 录制查看火焰图"
            performHeavyTask()
        }

        // 多个嵌套任务
        btnMultipleTasks.setOnClickListener {
            statusText.text = "执行嵌套任务中...\n观察火焰图的层级结构"
            performNestedTasks()
        }

        btnFinish.setOnClickListener {
            finish()
        }

        Log.d(TAG, "Activity 创建: $this")
    }

    /**
     * 模拟耗时操作 - 模拟主线程阻塞
     *
     * 在真实场景中，这可能是：
     * - 文件 I/O 操作
     * - 复杂计算
     * - 同步网络请求
     */
    private fun performHeavyTask() {
        Log.d(TAG, "开始执行耗时任务...")
        statusText.text = "执行耗时任务中...\n观察 Logcat 和 Profiler"

        // 让 CPU 真正忙起来（不要 sleep）
        val startTime = System.currentTimeMillis()

        // 模拟大量 CPU 计算，持续 2 秒
        var count = 0.0
        while (System.currentTimeMillis() - startTime < 2000) {
            count += Math.sqrt(Math.random()) // 模拟复杂计算
        }

        val endTime = System.currentTimeMillis()
        Log.d(TAG, "CPU 任务完成，耗时: ${endTime - startTime}ms")

        runOnUiThread {
            statusText.text = "任务完成！\n耗时: ${endTime - startTime}ms"
        }
    }

    /**
     * 模拟嵌套调用 - 产生深层火焰图
     *
     * 火焰图会显示完整的调用栈：
     * main()
     *   -> performNestedTasks()
     *      -> taskLevel1()
     *         -> taskLevel2()
     *            -> taskLevel3()
     *               -> doWork()
     */
    private fun performNestedTasks() {
        Log.d(TAG, "开始执行嵌套任务...")

        taskLevel1()

        runOnUiThread {
            statusText.text = "嵌套任务完成！\n查看火焰图的层级结构"
        }
    }

    private fun taskLevel1() {
        Log.d(TAG, "Level 1")
        taskLevel2()
    }

    private fun taskLevel2() {
        Log.d(TAG, "Level 2")
        taskLevel3()
    }

    private fun taskLevel3() {
        Log.d(TAG, "Level 3")
        doWork()
    }

    /**
     * 实际工作的地方 - 火焰图底部
     */
    private fun doWork() {
        Log.d(TAG, "Doing work...")
        // 模拟一些计算
        var sum = 0
        for (i in 1..1000000) {
            sum += i
        }
        Log.d(TAG, "Work done, sum = $sum")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理 Handler 消息
        mainHandler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Activity 销毁")
    }
}
