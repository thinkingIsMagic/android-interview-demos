package com.example.memoryleaktest.leak

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.memoryleaktest.R

/**
 * 普通对象内存泄漏演示 Activity
 *
 * 展示非 Activity 对象的内存泄漏场景：
 * 1. 静态集合泄漏
 * 2. 线程/定时器泄漏
 * 3. 单例模式泄漏
 * 4. 监听器泄漏
 *
 * 配合 LeakCanary 和 Android Profiler 使用
 */
class ObjectLeakDemoActivity : AppCompatActivity() {
    private val TAG = "ObjectLeakDemo"

    private lateinit var tvStatus: TextView
    private lateinit var tvHint: TextView

    // 创建 Manager 实例（用于演示线程泄漏）
    private val networkManager = NetworkManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_leak_demo)

        tvStatus = findViewById(R.id.tvStatus)
        tvHint = findViewById(R.id.tvHint)

        setupButtons()
        updateStatus("演示页面已打开")
    }

    private fun setupButtons() {
        // 场景 1：静态集合泄漏
        findViewById<Button>(R.id.btnAddToStaticCache).setOnClickListener {
            // 模拟添加大量数据到静态缓存
            repeat(100) { index ->
                val userData = UserData(
                    userId = "user_$index",
                    userName = "用户$index",
                    avatarUrl = "https://example.com/avatar_$index.jpg",
                    friends = (0..10).map { "friend_$it" }
                )
                StaticCacheManager.cacheUserData(userData)

                // 手动通知 LeakCanary 观察其中一个对象
                if (index == 50) {
                    LeakCanaryHelper.watchCachedObject(userData)
                }
            }
            updateStatus("已添加 100 个用户数据到静态缓存\n当前缓存大小: ${StaticCacheManager.leakyCache.size}")
            showHint("问题：静态缓存会无限增长！\nGC Root -> StaticCacheManager.leakyCache -> List -> UserData")
        }

        // 场景 2：线程泄漏
        findViewById<Button>(R.id.btnStartThread).setOnClickListener {
            // 启动后台线程（会持有 NetworkManager 引用）
            networkManager.startPolling()
            networkManager.startPeriodicTask()

            // 观察 networkManager 对象
            LeakCanaryHelper.watchObject(networkManager, "NetworkManager")

            updateStatus("已启动后台线程和定时任务\n注意：如果直接退出页面，对象会泄漏")
            showHint("问题：线程持有 NetworkManager 引用\nGC Root -> Thread -> NetworkManager")
        }

        // 场景 3：单例泄漏
        findViewById<Button>(R.id.btnSingletonLeak).setOnClickListener {
            // 将 this（Activity）传递给单例
            LeakySingleton.setContext(this)
            LeakySingleton.setData(
                UserData(
                    userId = "current_user",
                    userName = "当前用户",
                    avatarUrl = "https://example.com/avatar.jpg",
                    friends = emptyList()
                )
            )

            // 观察 Activity 对象
            LeakCanaryHelper.watchObject(this, "ObjectLeakDemoActivity")

            updateStatus("Activity 已被单例持有\n如果现在退出，会导致泄漏")
            showHint("问题：单例生命周期 = Application\nGC Root -> LeakySingleton -> Activity")
        }

        // 场景 4：监听器泄漏（模拟）
        findViewById<Button>(R.id.btnListenerLeak).setOnClickListener {
            val locationTracker = LocationTracker()

            // 创建监听器（内部持有 Activity 引用）
            val listener = object : LocationTracker.LocationListener {
                override fun onLocationUpdate(lat: Double, lng: Double) {
                    Log.d(TAG, "位置更新: $lat, $lng")
                }
            }

            locationTracker.registerListener(listener)
            locationTracker.registerSystemService(Any())

            // 观察 tracker 对象
            LeakCanaryHelper.watchObject(locationTracker, "LocationTracker")

            // 注意：这里没有调用 unregister，会导致泄漏
            // 模拟：用户退出但监听器未注销
            updateStatus("已注册监听器和系统服务\n（未注销，模拟泄漏）")
            showHint("问题：监听器持有 Activity 引用\nGC Root -> LocationTracker -> Listener -> Activity")
        }

        // 手动触发 GC（帮助观察）
        findViewById<Button>(R.id.btnTriggerGC).setOnClickListener {
            System.gc()
            System.runFinalization()
            Toast.makeText(this, "已触发 GC", Toast.LENGTH_SHORT).show()
            updateStatus("已触发 GC\n如果 LeakCanary 没有报错，说明对象已回收")
            showHint("提示：观察 LeakCanary 通知或 Profiler")
        }

        // 清理所有资源（正确做法）
        findViewById<Button>(R.id.btnCleanup).setOnClickListener {
            cleanupAll()
            updateStatus("已清理所有资源\n对象应该可以被正常回收")
            showHint("修复方案：手动清理或使用 WeakReference")
        }
    }

    private fun cleanupAll() {
        // 清理静态缓存
        StaticCacheManager.clearCache()

        // 停止线程和定时器
        networkManager.stopPolling()
        networkManager.stopPeriodicTask()

        // 清理单例
        LeakySingleton.clear()

        // 提示：实际项目中需要跟踪 LocationTracker 实例并清理
    }

    private fun updateStatus(text: String) {
        tvStatus.text = text
    }

    private fun showHint(text: String) {
        tvHint.text = text
    }

    override fun onDestroy() {
        super.onDestroy()
        // 重要：在 onDestroy 中清理资源
        // 如果不清理，直接按返回键会导致泄漏
        Log.d(TAG, "onDestroy - 清理资源")
    }
}
