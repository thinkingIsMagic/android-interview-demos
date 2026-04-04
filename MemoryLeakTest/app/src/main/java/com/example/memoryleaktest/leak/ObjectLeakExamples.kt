package com.example.memoryleaktest.leak

import android.util.Log
import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask

/**
 * ========== 普通对象内存泄漏示例 ==========
 *
 * 本文件展示非 Activity 对象的内存泄漏场景：
 * 1. 静态集合持有对象
 * 2. 未关闭的线程/定时器
 * 3. 单例模式持有短生命周期对象
 * 4. 未注销的监听器
 *
 * 使用 LeakCanary 的 AppWatcher.objectWatcher.watch() 手动观察这些对象
 */

// ==================== 场景 1：静态集合泄漏 ====================

/**
 * 静态集合导致内存泄漏
 *
 * 泄漏原因：
 * - static List/Map/Set 的生命周期等同于 Application
 * - 对象被添加后永远不会被移除
 * - 即使外部已经不需要这些对象，它们仍然被集合持有
 */
object StaticCacheManager {
    private val TAG = "StaticCacheManager"

    // 模拟一个静态缓存 List
    // 注意：这是导致泄漏的典型案例
    val leakyCache = mutableListOf<Any>()

    // 修复：使用弱引用或限制缓存大小
    // val fixedCache = LruCache<String, Any>(100)

    /**
     * 添加数据到缓存（会导致泄漏）
     */
    fun addToCache(data: Any) {
        leakyCache.add(data)
        Log.d(TAG, "添加缓存，当前大小: ${leakyCache.size}")
    }

    /**
     * 模拟业务场景：用户数据被缓存但从未清理
     * 每次刷新用户资料，新数据被添加，旧数据永不释放
     */
    fun cacheUserData(userData: UserData) {
        // 问题：每次调用都会添加，列表无限增长
        leakyCache.add(userData)
    }

    /**
     * 正确的做法：清理缓存
     */
    fun clearCache() {
        leakyCache.clear()
        Log.d(TAG, "缓存已清理")
    }
}

/**
 * 用户数据类（模拟业务对象）
 */
data class UserData(
    val userId: String,
    val userName: String,
    val avatarUrl: String,
    val friends: List<String>
)

// ==================== 场景 2：未关闭的线程/定时器泄漏 ====================

/**
 * 线程/定时器导致内存泄漏
 *
 * 泄漏原因：
 * - 匿名 Thread 或 TimerTask 会持有外部类的隐式引用
 * - 如果线程一直在运行，它引用的对象就无法被 GC
 * - 定时器任务如果没有取消，会持续持有引用
 */
class NetworkManager {
    private val TAG = "NetworkManager"

    // 模拟的线程（会持有外部类引用）
    private var leakyThread: Thread? = null

    // 模拟的定时器
    private var leakyTimer: Timer? = null

    /**
     * 泄漏场景：启动后台线程轮询
     */
    fun startPolling() {
        // 问题：匿名 Thread 持有 NetworkManager 引用
        leakyThread = Thread {
            while (true) {
                try {
                    Thread.sleep(5000) // 每 5 秒轮询一次
                    Log.d(TAG, "轮询中...")
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        leakyThread?.start()
    }

    /**
     * 修复：在不需要时停止线程
     */
    fun stopPolling() {
        leakyThread?.interrupt()
        leakyThread = null
    }

    /**
     * 泄漏场景：启动定时任务
     */
    fun startPeriodicTask() {
        leakyTimer = Timer()
        // 问题：TimerTask 持有 NetworkManager 引用
        leakyTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "定时任务执行中...")
            }
        }, 0, 3000)
    }

    /**
     * 修复：取消定时任务
     */
    fun stopPeriodicTask() {
        leakyTimer?.cancel()
        leakyTimer = null
    }
}

/**
 * 使用静态内部类 + WeakReference 修复线程泄漏
 */
class SafeNetworkManager {
    private val TAG = "SafeNetworkManager"

    private var safeThread: Thread? = null

    /**
     * 修复：使用静态内部类，避免持有外部类引用
     */
    fun startPollingSafely() {
        safeThread = Thread(SafePollingTask(WeakReference(this)))
        safeThread?.start()
    }

    fun stopPolling() {
        safeThread?.interrupt()
        safeThread = null
    }

    fun doWork() {
        Log.d(TAG, "执行实际工作...")
    }

    // 静态内部类，不持有外部类引用
    private class SafePollingTask(
        private val managerRef: WeakReference<SafeNetworkManager>
    ) : Runnable {
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(5000)
                    // 使用前检查引用是否有效
                    managerRef.get()?.doWork()
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }
}

// ==================== 场景 3：单例模式泄漏 ====================

/**
 * 单例模式导致的内存泄漏
 *
 * 泄漏原因：
 * - 单例生命周期 = Application 生命周期
 * - 如果将短生命周期对象传递给单例且没有清理
 * - 该对象就会"永生"
 */

/**
 * 泄漏的单例：持有 Context 或业务对象
 */
object LeakySingleton {
    private val TAG = "LeakySingleton"

    // 模拟持有 Activity/Fragment 上下文
    private var leakyContext: Any? = null

    // 模拟持有业务对象
    private var leakyData: Any? = null

    /**
     * 泄漏：保存了 Activity 引用
     */
    fun setContext(context: Any) {
        leakyContext = context
        Log.d(TAG, "保存了 Context")
    }

    /**
     * 泄漏：保存了业务对象
     */
    fun setData(data: Any) {
        leakyData = data
        Log.d(TAG, "保存了数据")
    }

    /**
     * 修复：清理引用
     */
    fun clear() {
        leakyContext = null
        leakyData = null
        Log.d(TAG, "已清理引用")
    }
}

/**
 * 修复：使用弱引用的单例
 */
object SafeSingleton {
    private val TAG = "SafeSingleton"

    // 使用弱引用，允许 GC 回收
    private var safeContext: WeakReference<Any>? = null
    private var safeData: WeakReference<Any>? = null

    fun setContext(context: Any) {
        safeContext = WeakReference(context)
    }

    fun setData(data: Any) {
        safeData = WeakReference(data)
    }

    fun getContext(): Any? = safeContext?.get()

    fun clear() {
        safeContext?.clear()
        safeData?.clear()
    }
}

// ==================== 场景 4：监听器/回调未注销 ====================

/**
 * 监听器/回调未注销导致的泄漏
 *
 * 泄漏原因：
 * - 注册了全局监听器（如 EventBus、系统服务）
 * - 但忘记在不需要时注销
 * - 监听器持有对象引用，导致泄漏
 */
class LocationTracker {
    private val TAG = "LocationTracker"

    // 模拟的监听器列表
    private val listeners = mutableListOf<LocationListener>()

    // 模拟的系统服务引用
    private var systemService: Any? = null

    interface LocationListener {
        fun onLocationUpdate(lat: Double, lng: Double)
    }

    /**
     * 泄漏：添加监听器但没有对应的移除方法
     */
    fun registerListener(listener: LocationListener) {
        listeners.add(listener)
        Log.d(TAG, "注册监听器，当前数量: ${listeners.size}")
    }

    /**
     * 修复：移除监听器
     */
    fun unregisterListener(listener: LocationListener) {
        listeners.remove(listener)
        Log.d(TAG, "移除监听器，当前数量: ${listeners.size}")
    }

    /**
     * 修复：移除所有监听器
     */
    fun unregisterAll() {
        listeners.clear()
        Log.d(TAG, "已移除所有监听器")
    }

    /**
     * 模拟系统服务注册（常见泄漏场景）
     */
    fun registerSystemService(service: Any) {
        systemService = service
        Log.d(TAG, "注册系统服务")
    }

    /**
     * 修复：注销系统服务
     */
    fun unregisterSystemService() {
        systemService = null
        Log.d(TAG, "注销系统服务")
    }
}

/**
 * 使用回调的泄漏示例
 */
class EventBusExample {
    private val TAG = "EventBusExample"

    // 模拟的订阅者列表
    private val subscribers = mutableListOf<Any>()

    /**
     * 模拟 EventBus.subscribe()
     */
    fun subscribe(subscriber: Any) {
        subscribers.add(subscriber)
    }

    /**
     * 模拟 EventBus.unubscribe()
     */
    fun unsubscribe(subscriber: Any) {
        subscribers.remove(subscriber)
    }
}

// ==================== 场景 5：Handler 在普通类中的泄漏 ====================

/**
 * 非 Activity/Fragment 类中使用 Handler 的泄漏
 */
class MessageProcessor {
    private val TAG = "MessageProcessor"

    // 问题：非静态 Handler 会持有 MessageProcessor 引用
    private val handler = android.os.Handler(android.os.Looper.getMainLooper()) {
        // 这个 lambda 持有 MessageProcessor 引用
        Log.d(TAG, "处理消息: ${it.what}")
        true
    }

    fun sendDelayedMessage(msg: Int, delayMs: Long = 10000) {
        val message = android.os.Message.obtain()
        message.what = msg
        handler.sendMessageDelayed(message, delayMs)
    }

    /**
     * 修复：清理消息
     */
    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
    }
}

// ==================== 手动使用 LeakCanary 观察对象 ====================

/**
 * 使用 LeakCanary 手动观察对象的示例
 *
 * LeakCanary 默认只观察 Activity 和 Fragment
 * 但可以通过 AppWatcher.objectWatcher.watch() 观察任意对象
 *
 * 使用方法：
 * 1. 在 debugImplementation 中添加 leakcanary
 * 2. 调用 AppWatcher.objectWatcher.watch(yourObject)
 * 3. 如果对象无法被回收，LeakCanary 会显示泄漏路径
 */
object LeakCanaryHelper {
    private val TAG = "LeakCanaryHelper"

    /**
     * 手动观察对象的示例代码
     *
     * 注意：这个方法需要在 LeakCanary 可用时调用
     * 实际使用时请添加依赖并正确导入
     */
    fun watchObject(obj: Any, description: String) {
        // LeakCanary 的 API（需要导入 leakcanary 包）
        // import leakcanary.AppWatcher
        // AppWatcher.objectWatcher.expectWeaklyReachable(obj, description)

        Log.d(TAG, "观察对象: $description")
        Log.d(TAG, "如果对象泄漏，LeakCanary 会显示通知")
        Log.d(TAG, "查看泄漏路径: $description 的 GC Root 引用链")
    }

    /**
     * 示例：在缓存场景中观察对象
     */
    fun watchCachedObject(userData: UserData) {
        // 观察被缓存的用户数据
        watchObject(userData, "缓存的用户数据: ${userData.userId}")
    }

    /**
     * 示例：在线程场景中观察对象
     */
    fun watchThreadObject(obj: Any) {
        watchObject(obj, "线程中使用的对象")
    }
}

// ==================== 总结：修复方案 ====================

/**
 * 常见泄漏的修复方案总结
 */
object LeakFixSummary {

    /**
     * | 泄露原因 | 修复方案 |
     * |---------|---------|
     * | 静态变量持有实例 | 尽量避免 static 持有实例；或置为 null |
     * | 匿名内部类线程 | 改为 static 内部类 + WeakReference |
     * | Handler 消息未处理 | onDestroy 中调用 removeCallbacksAndMessages |
     * | 静态集合缓存 | 使用 LruCache 或定期清理 |
     * | 单例持有长引用 | 使用 WeakReference |
     * | 监听器未注销 | 在 onDestroy/onDestroyView 中 unregister |
     */
    fun printSummary() {
        Log.d("LeakFixSummary", "详见上方表格")
    }
}
