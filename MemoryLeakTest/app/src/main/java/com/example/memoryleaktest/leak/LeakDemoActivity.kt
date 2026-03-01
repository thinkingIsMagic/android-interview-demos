package com.example.memoryleaktest.leak

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.memoryleaktest.R

/**
 * 用于演示 Handler 内存泄漏的 Activity
 *
 * 使用方法：
 * 1. 点击 "启动泄漏场景" 按钮
 * 2. 立即按返回键退出
 * 3. 等待 10 秒，观察 Logcat
 * 4. 如果使用了 LeakCanary，会收到泄漏通知
 */
class LeakDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LeakDemoActivity"
        private const val MSG_DELAYED_TASK = 1
        private const val DELAY_TIME = 30000L
    }

    private lateinit var statusText: TextView

    // ====== 泄漏代码：非静态内部类 Handler ======
    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_DELAYED_TASK -> {
                    Log.d(TAG, "延迟任务执行了！")
                    runOnUiThread {
                        statusText.text = "延迟任务执行完毕！"
                        Toast.makeText(this@LeakDemoActivity, "延迟任务执行了！", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leak_demo)

        statusText = findViewById(R.id.statusText)
        val startButton = findViewById<Button>(R.id.btnStartTask)
        val finishButton = findViewById<Button>(R.id.btnFinish)

        startButton.setOnClickListener {
            startDelayedTask()
        }

        finishButton.setOnClickListener {
            finish() // 直接退出
        }

        Log.d(TAG, "Activity 创建: $this")
    }

    /**
     * 发送延迟消息 - 触发泄漏的操作
     */
    private fun startDelayedTask() {
        val message = Message.obtain()
        message.what = MSG_DELAYED_TASK
        message.obj = "任务数据"

        // 发送延迟消息，10秒后执行
        mHandler.sendMessageDelayed(message, DELAY_TIME)

        statusText.text = "已发送延迟任务（${DELAY_TIME/1000}秒后执行）\n请立即按返回键退出，观察泄漏"

        // 模拟外部回调
        simulateExternalCallback()
    }

    /**
     * 模拟外部库的回调
     */
    private fun simulateExternalCallback() {
        val imageLoader = ImageLoader()
        imageLoader.loadImage("https://example.com/test.jpg", object : ImageLoader.LoadCallback {
            override fun onImageLoaded(imageBytes: ByteArray?) {
                Log.d(TAG, "图片加载完成: ${imageBytes?.size} bytes")
            }

            override fun onError(error: String?) {
                Log.d(TAG, "图片加载失败: $error")
            }
        })
    }

    /**
     * ====== 关键：这里应该清理消息 ======
     * 但如果不调用这个方法，就会泄漏
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity onDestroy: $this")

        // ✅ 正确做法：移除所有消息（如果没有这行，就会泄漏）
        // mHandler.removeCallbacksAndMessages(null)

        // 为了演示泄漏，我们故意不清理
        Log.w(TAG, "注意：没有清理 Handler 消息，Activity 会泄漏！")
    }
}

/**
 * 修复后的版本 - 对比学习
 */
class LeakFixedDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LeakFixedDemo"
        private const val MSG_DELAYED_TASK = 1
    }

    private lateinit var statusText: TextView

    // ====== 修复：使用静态内部类 + WeakReference ======
    private class SafeHandler(activity: LeakFixedDemoActivity) : Handler(Looper.getMainLooper()) {
        private val activityRef = java.lang.ref.WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = activityRef.get()
            if (activity != null) {
                when (msg.what) {
                    MSG_DELAYED_TASK -> {
                        Log.d(TAG, "延迟任务执行了（安全）")
                        activity.runOnUiThread {
                            activity.updateStatus("延迟任务执行完毕！（安全）")
                        }
                    }
                }
            } else {
                Log.w(TAG, "Activity 已被回收，跳过任务执行")
            }
        }
    }

    private val mHandler = SafeHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leak_demo)

        statusText = findViewById(R.id.statusText)
        val titleText = findViewById<TextView>(R.id.titleText)
        titleText.text = "Handler 修复演示"

        val startButton = findViewById<Button>(R.id.btnStartTask)
        val finishButton = findViewById<Button>(R.id.btnFinish)

        startButton.text = "启动安全任务"
        startButton.setOnClickListener {
            startDelayedTask()
        }

        finishButton.text = "返回"
        finishButton.setOnClickListener {
            finish()
        }

        statusText.text = "点击按钮开始演示\n使用 WeakReference 保护"

        Log.d(TAG, "Fixed Activity 创建: $this")
    }

    private fun startDelayedTask() {
        val message = Message.obtain()
        message.what = MSG_DELAYED_TASK
        mHandler.sendMessageDelayed(message, 10000)

        statusText.text = "已发送延迟任务（10秒）\n使用 WeakReference 保护"
    }

    fun updateStatus(text: String) {
        if (!isDestroyed) {
            statusText.text = text
        }
    }

    /**
     * ====== 修复：在 onDestroy 中清理 ======
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Fixed Activity onDestroy: $this")

        // ✅ 移除所有消息，防止泄漏
        mHandler.removeCallbacksAndMessages(null)
    }
}
