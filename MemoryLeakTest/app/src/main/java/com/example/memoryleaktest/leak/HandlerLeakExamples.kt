package com.example.memoryleaktest.leak

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.ref.WeakReference

/**
 * 模拟的外部类（非 Activity/Fragment）
 * 这里模拟一个图片加载器或网络请求管理器
 */
class ImageLoader {
    interface LoadCallback {
        fun onImageLoaded(imageBytes: ByteArray?)
        fun onError(error: String?)
    }

    // 模拟异步加载图片
    fun loadImage(url: String, callback: LoadCallback) {
        // 模拟耗时操作
        Handler(Looper.getMainLooper()).postDelayed({
            // 模拟加载成功
            callback.onImageLoaded("image_data".toByteArray())
        }, 3000) // 3秒后回调
    }
}

/**
 * ========== 有内存泄漏的代码示例 ==========
 *
 * 这个示例展示了常见的 Handler 内存泄漏问题
 *
 * 泄漏原因：
 * 1. Handler 被声明为 Activity 的非静态内部类（或匿名内部类）
 * 2. 非静态内部类会持有外部类（Activity）的隐式引用
 * 3. 如果 Handler 还有未执行的 Message（延迟消息），Message.target 会持有 Handler 引用
 * 4. MessageQueue 中会保留这些 Message，导致 Activity 无法被 GC 回收
 *
 * 泄漏场景：
 * - 用户启动 Activity
 * - 点击按钮触发延迟消息（例如 10 秒后更新 UI）
 * - 用户立即按返回键退出 Activity
 * - 由于 MessageQueue 中仍有消息，Activity 被 Handler 引用，无法被回收
 * - 只有当消息执行完毕或超时，Activity 才能被回收
 */
class LeakyActivity {
    companion object {
        private const val TAG = "LeakyActivity"
    }

    // 模拟的 UI 更新 Handler
    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: android.os.Message) {
            when (msg.what) {
                1 -> {
                    // 更新 UI - 这里会引用 Activity
                    Log.d(TAG, "更新 UI: ${msg.obj}")
                }
            }
        }
    }

    // 模拟外部类的回调
    private val imageLoader = ImageLoader()

    fun startDelayedTask() {
        // 发送一个延迟 10 秒的消息
        val message = Message.obtain()
        message.what = 1
        message.obj = "延迟任务完成"
        mHandler.sendMessageDelayed(message, 10000) // 10 秒延迟

        // 同时发起一个图片加载请求
        imageLoader.loadImage("https://example.com/image.jpg", object : ImageLoader.LoadCallback {
            override fun onImageLoaded(imageBytes: ByteArray?) {
                // 回调中更新 UI - 隐式持有 Activity 引用
                Log.d(TAG, "图片加载完成: ${imageBytes?.size} bytes")
            }

            override fun onError(error: String?) {
                Log.d(TAG, "图片加载失败: $error")
            }
        })
    }

    // 如果在 Activity 中，这个方法应该被调用
    // 但如果用户在此之前退出，Activity 就会泄漏
    fun cleanup() {
        mHandler.removeCallbacksAndMessages(null)
    }
}

/**
 * ========== 修复后的代码示例 ==========
 *
 * 修复方法：
 * 1. 使用静态内部类 + WeakReference
 * 2. 在 onDestroy/onDestroyView 中移除所有消息
 * 3. 使用 Lifecycle-aware 组件
 */

/**
 * 使用弱引用保护的 Handler
 *
 * 原理：
 * - 静态内部类不持有外部类的引用
 * - WeakReference 允许 GC 在需要时回收 Activity
 * - 在访问 Activity 前检查引用是否还有效
 */
class SafeHandler(activity: LeakyFixedActivity) : Handler(Looper.getMainLooper()) {
    // 使用弱引用持有 Activity
    private val activityRef: WeakReference<LeakyFixedActivity> = WeakReference(activity)

    override fun handleMessage(msg: android.os.Message) {
        // 每次使用前检查 Activity 是否还存在
        val activity = activityRef.get()
        if (activity != null) {
            when (msg.what) {
                1 -> {
                    activity.updateUI(msg.obj as String)
                }
            }
        } else {
            Log.w("SafeHandler", "Activity 已被回收，跳过 UI 更新")
        }
    }
}

/**
 * 修复后的 Activity 示例
 */
class LeakyFixedActivity {
    companion object {
        private const val TAG = "LeakyFixedActivity"
    }

    // 使用自定义的 SafeHandler
    private val mHandler = SafeHandler(this)

    private val imageLoader = ImageLoader()

    fun startDelayedTask() {
        val message = Message.obtain()
        message.what = 1
        message.obj = "延迟任务完成"
        mHandler.sendMessageDelayed(message, 10000)

        imageLoader.loadImage("https://example.com/image.jpg", object : ImageLoader.LoadCallback {
            override fun onImageLoaded(imageBytes: ByteArray?) {
                // 在回调中也要检查 Activity 是否存在
                // 可以使用 WeakReference 或者在 callback 中传递弱引用
                Log.d(TAG, "图片加载完成: ${imageBytes?.size} bytes")
            }

            override fun onError(error: String?) {
                Log.d(TAG, "图片加载失败: $error")
            }
        })
    }

    fun updateUI(data: String) {
        Log.d(TAG, "安全更新 UI: $data")
    }

    // 关键：在组件销毁时移除所有消息
    fun cleanup() {
        mHandler.removeCallbacksAndMessages(null)
    }
}

/**
 * ========== 最佳实践：使用 Kotlin 特性 ==========
 *
 * Kotlin 提供了更优雅的解决方案：
 * 1. 使用 lambda 代替匿名 Handler
 * 2. 使用协程
 * 3. 使用 Lifecycle-aware 组件
 */

/**
 * Kotlin 协程版本（推荐）
 *
 * 优点：
 * - 自动管理生命周期
 * - 代码更简洁
 * - 不会内存泄漏
 */
/*
class KotlinCoroutineActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startDelayedTask() {
        scope.launch {
            delay(10000) // 协程自动挂起，不阻塞线程
            // 这个 lambda 会自动捕获 scope
            // 当 Activity 销毁时，scope 取消，协程也会自动取消
            Log.d("CoroutineActivity", "延迟任务完成")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // 取消所有协程
    }
}
*/

/**
 * 使用 Runnable 时的同样问题
 */
class RunnableLeakExample {
    // 同样的问题：Runnable 作为非静态内部类会持有外部类引用
    private val runnable = Runnable {
        // 这个 lambda/Runnable 会持有外部类引用
        Thread.sleep(10000)
        Log.d("RunnableLeak", "任务完成")
    }

    fun startTask() {
        // 如果没有及时移除，Activity 会泄漏
        // Handler(Looper.getMainLooper()).postDelayed(runnable, 10000)
    }

    fun cleanup() {
        // Handler(Looper.getMainLooper()).removeCallbacks(runnable)
    }
}
