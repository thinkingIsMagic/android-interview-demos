# Handler 内存泄漏分析指南

## 目录
1. [内存泄漏原理](#1-内存泄漏原理)
2. [Handler 泄漏的详细机制](#2-handler-泄漏的详细机制)
3. [实战分析步骤](#3-实战分析步骤)
4. [代码示例解析](#4-代码示例解析)
5. [修复方案](#5-修复方案)
6. [检测工具推荐](#6-检测工具推荐)

---

## 1. 内存泄漏原理

### 1.1 什么是内存泄漏？

内存泄漏（Memory Leak）是指程序在申请内存后，无法释放已申请的内存空间，导致内存占用持续增长，最终可能导致 OOM（OutOfMemoryError）。

### 1.2 Android 中的内存管理

- Android 使用 **GC（垃圾回收）** 机制管理内存
- GC 会回收**不可达**的对象（即没有任何引用指向的对象）
- 内存泄漏的本质是：**本应被回收的对象因为被错误引用而无法回收**

### 1.3 常见的泄漏场景

| 类型 | 描述 |
|------|------|
| 静态引用 | 静态变量持有 Activity/Fragment 引用 |
| 非静态内部类 | 内部类持有外部类隐式引用 |
| 监听器未注销 | 监听器/回调未移除 |
| Handler 延迟消息 | MessageQueue 中仍有未执行的消息 |
| 单例持有 | 单例模式持有 Context |
| 资源未关闭 | Stream、Cursor 等未关闭 |

---

## 2. Handler 泄漏的详细机制

### 2.1 Handler 工作原理

```
┌─────────────────────────────────────────────────────────────┐
│                     MessageQueue                            │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐                   │
│  │ Message │──▶│ Message │──▶│ Message │──▶ ...            │
│  │ (next)  │   │ (next)  │   │ (next)  │                   │
│  └────┬────┘   └────┬────┘   └────┬────┘                   │
│       │             │             │                        │
│       ▼             ▼             ▼                        │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐                   │
│  │ Handler │   │ Handler │   │ Handler │                   │
│  │ (target)│   │ (target)│   │ (target)│                   │
│  └────┬────┘   └────┬────┘   └────┬────┘                   │
│       │             │             │                        │
│       └─────────────┴─────────────┘                         │
│                      │                                      │
│                      ▼                                      │
│            ┌─────────────────┐                               │
│            │   Activity      │ ◀── 泄漏点！                  │
│            │ (外部类)        │    持有隐式引用                │
│            └─────────────────┘                               │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 泄漏链条分析

```
用户点击按钮
    │
    ▼
Handler.sendMessageDelayed(message, 10000)
    │
    ▼
MessageQueue 中添加 Message
    │
    ▼
Message.target = Handler (引用)
    │
    ▼
Handler 是非静态内部类 → 持有 Activity 引用 (LeakyActivity.this)
    │
    ▼
用户按返回键退出 Activity
    │
    ▼
GC 尝试回收 Activity
    │
    ▼
❌ 失败！因为:
    Message → Handler → Activity (引用链存在)
    │
    ▼
Activity 无法被回收 → 内存泄漏！
```

### 2.3 关键代码位置

```kotlin
class LeakyActivity {
    // 问题1: 非静态内部类
    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            // 这个匿名内部类会持有外部 Activity 的引用
        }
    }

    fun startDelayedTask() {
        // 问题2: 发送延迟消息
        val message = Message.obtain()
        mHandler.sendMessageDelayed(message, 10000) // 10秒延迟

        // 问题3: 如果用户在这期间退出，Activity 会被泄漏
    }
}
```

---

## 3. 实战分析步骤

### 3.1 使用 Android Studio Profiler

#### 步骤 1: 打开 Profiler

```
Android Studio → Run → Profile 'app'
```

#### 步骤 2: 捕获堆转储（Heap Dump）

1. 点击 "Dump Java heap" 按钮
2. 等待分析完成
3. 在左侧选择 "Package" 或 "Class"

#### 步骤 3: 查找泄漏的 Activity

1. 在搜索框输入可能泄漏的 Activity 名称（如 `LeakyActivity`）
2. 如果发现多个实例（正常应该只有 1 个或 0 个），说明可能泄漏

#### 步骤 4: 分析引用链

```
右键点击泄漏的实例 → Path to GC Root → exclude weak/soft references
```

查看是什么引用持有该 Activity。

### 3.2 使用 LeakCanary（推荐）

LeakCanary 是 Square 开发的自动检测内存泄漏的工具。

#### 集成步骤

1. 添加依赖（`build.gradle`）:

```kotlin
dependencies {
    debugImplementation "com.squareup.leakcanary:leakcanary-android:2.12"
}
```

2. 正常编写代码，LeakCanary 会自动：
   - 监控 Activity/Fragment 的生命周期
   - 在 Activity 被销毁后，如果 5 秒内未被 GC 回收，自动触发堆转储
   - 在通知栏显示泄漏分析结果

3. 查看泄漏通知：
   - 下拉通知栏
   - 点击 LeakCanary 通知
   - 查看引用链

### 3.3 使用 adb 命令分析

```bash
# 1. 获取当前内存状态
adb shell dumpsys meminfo <package_name>

# 2. 触发 GC
adb shell dumpsys package <package_name>  # 包含 GC 信息

# 3. 导出堆转储文件
adb shell am dumpheap <package_name> /sdcard/heap.hprof

# 4. 拉取到本地
adb pull /sdcard/heap.hprof ./heap.hprof
```

### 3.4 MAT（Memory Analyzer Tool）分析

```bash
# 1. 下载 MAT: https://eclipse.dev/mat/downloads.php

# 2. 转换 hprof 格式（Android → 标准格式）
hprof-conv heap.hprof heap转换.hprof

# 3. 打开 MAT
# File → Open Heap Dump → 选择 heap转换.hprof

# 4. 使用 OQL 查找泄漏的 Activity
SELECT * FROM com.example.memoryleaktest.leak.LeakyActivity

# 5. 查看 GC Root 路径
# 右键 → Path to GC Roots → exclude weak/soft references
```

---

## 4. 代码示例解析

### 4.1 泄漏代码（LeakyActivity）

```kotlin
// 文件: HandlerLeakExamples.kt

class LeakyActivity {
    // ❌ 问题：非静态内部类，会持有外部类引用
    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                1 -> {
                    Log.d(TAG, "更新 UI: ${msg.obj}")
                }
            }
        }
    }

    fun startDelayedTask() {
        // ❌ 发送延迟消息，Message 会被放入 MessageQueue
        val message = Message.obtain()
        message.what = 1
        message.obj = "延迟任务完成"
        mHandler.sendMessageDelayed(message, 10000)
    }
}
```

**泄漏触发场景：**

```
1. 用户打开 LeakyActivity
2. 用户点击"开始延迟任务"按钮
3. Handler 发送一个 10 秒后的消息
4. 用户立即按返回键退出
5. Activity.onDestroy() 被调用
6. 但 MessageQueue 中仍有未执行的 Message
7. Message.target → Handler → Activity
8. GC 无法回收 Activity
9. 只有等到 10 秒后消息被执行，Activity 才能被回收
```

### 4.2 修复代码（LeakyFixedActivity）

```kotlin
// 使用静态内部类 + WeakReference

class SafeHandler(activity: LeakyFixedActivity) : Handler(Looper.getMainLooper()) {
    // ✅ 使用弱引用，不阻碍 GC
    private val activityRef: WeakReference<LeakyFixedActivity> = WeakReference(activity)

    override fun handleMessage(msg: Message) {
        // ✅ 每次使用前检查 Activity 是否存在
        val activity = activityRef.get()
        if (activity != null) {
            activity.updateUI(msg.obj as String)
        } else {
            Log.w("SafeHandler", "Activity 已被回收")
        }
    }
}

class LeakyFixedActivity {
    private val mHandler = SafeHandler(this)

    fun cleanup() {
        // ✅ 必须：在 onDestroy 中移除所有消息
        mHandler.removeCallbacksAndMessages(null)
    }
}
```

---

## 5. 修复方案

### 5.1 方案一：静态内部类 + WeakReference

```kotlin
class MainActivity : AppCompatActivity() {

    // 自定义静态 Handler
    private class SafeHandler(activity: MainActivity) : Handler(Looper.getMainLooper()) {
        private val activityRef = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            activityRef.get()?.let { activity ->
                // 安全使用 Activity
                when (msg.what) {
                    1 -> activity.updateUI(msg.obj.toString())
                }
            }
        }
    }

    private val mHandler = SafeHandler(this)

    override fun onDestroy() {
        super.onDestroy()
        // 关键：移除所有消息
        mHandler.removeCallbacksAndMessages(null)
    }
}
```

### 5.2 方案二：在 onDestroy 中清理

```kotlin
class MainActivity : AppCompatActivity() {
    private val mHandler = Handler(Looper.getMainLooper())

    fun someMethod() {
        mHandler.sendMessageDelayed(Message.obtain(), 10000)
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ 移除所有消息和回调
        mHandler.removeCallbacksAndMessages(null)
    }
}
```

### 5.3 方案三：使用 Kotlin 协程（推荐）

```kotlin
class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startDelayedTask() {
        scope.launch {
            delay(10000) // 不会泄漏！
            // Activity 销毁时，scope.cancel() 会取消这个协程
            updateUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // 取消所有协程
    }
}
```

### 5.4 方案四：使用 Lifecycle-aware 组件

```kotlin
class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 使用 lifecycleScope（推荐）
        lifecycleScope.launch {
            delay(10000)
            // 只有当 Activity 处于 STARTED 状态时才执行
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                updateUI()
            }
        }
    }
}
```

### 5.5 修复方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| 静态类 + WeakReference | 兼容性好 | 代码稍多，需要手动清理 |
| onDestroy 清理 | 简单直接 | 容易遗漏 |
| Kotlin 协程 | 自动管理生命周期，推荐 | 需要引入协程 |
| Lifecycle-aware | 与系统生命周期绑定 | 需要 Lifecycle 库 |

---

## 6. 检测工具推荐

### 6.1 LeakCanary（强烈推荐）

- 自动检测，无需手动操作
- 提供可视化泄漏路径
- 集成简单

```kotlin
// 只需要这一行（2.0+ 版本）
debugImplementation "com.squareup.leakcanary:leakcanary-android:2.12"
```

### 6.2 Android Profiler

- 官方工具，无需额外依赖
- 可以实时监控内存
- 支持手动触发堆转储

### 6.3 MAT (Memory Analyzer)

- 适合分析复杂泄漏
- 功能强大
- 需要手动分析

### 6.4 KtHook（可选）

- 针对 Kotlin 的泄漏检测
- 支持更多 Kotlin 特性

---

## 总结

### Handler 内存泄漏要点

1. **根本原因**：非静态内部类持有外部类引用 + 延迟消息
2. **泄漏条件**：消息未执行 + Activity 已销毁
3. **最佳修复**：使用 Kotlin 协程或 lifecycleScope
4. **必须操作**：在 onDestroy 中调用 `removeCallbacksAndMessages(null)`

### 快速检查清单

- [ ] Handler 是否使用静态内部类？
- [ ] 是否在 onDestroy 中清理了消息？
- [ ] 回调/监听器是否已注销？
- [ ] 是否使用了 LeakCanary 进行测试？

---

## 参考资料

- [Android 官方内存泄漏文档](https://developer.android.com/topic/libraries/support-library/leakcanary)
- [Understanding Android Memory Leaks](https://android-developers.googleblog.com/2011/03/memory-analysis-for-android.html)
- [LeakCanary GitHub](https://github.com/square/leakcanary)
