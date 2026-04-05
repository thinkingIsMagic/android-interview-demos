package com.example.androidcrashanalysis.model

/**
 * 场景注册表
 * 包含项目中所有 12 个场景的定义
 */
object ScenarioRegistry {

    private val allScenarios = listOf(
        // ========== OOM 模块 (5个场景) ==========
        Scenario(
            id = "oom_static_ref",
            title = "静态引用泄漏",
            description = "static 变量持有 Activity 引用，旋转屏幕后无法回收",
            category = ScenarioCategory.OOM,
            dangerLevel = 4,
            explanationText = """
                【问题原理】
                在 Kotlin 中，companion object 里的 static 变量生命周期等同于进程生命周期。
                如果在 onCreate 中执行: companion object { var activity: Activity? = null }
                activity = this

                当用户旋转屏幕时，Activity 会 onDestroy 后重建。但 static 变量持有的是
                旧 Activity 实例的引用，导致 GC Root 仍能访问该对象，Activity 无法被回收。

                多次旋转后，大量旧 Activity 堆积，堆内存耗尽，最终触发 OOM。

                【为什么危险】
                静态变量是最强的 GC Root 之一，一旦持有引用，对象几乎不可能被回收。
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发泄漏"按钮，然后旋转屏幕 3-5 次
                2. 点击"手动 GC"，观察 usedMemory 是否下降
                   - 修复关闭时：内存不下降（Activity 无法回收）
                   - 修复开启时：内存下降（WeakReference 允许 GC 回收）

                【LeakCanary 使用】
                Debug 构建下，LeakCanary 会自动检测 Activity 泄漏。
                触发泄漏后，约 5 秒后通知栏会弹出"LeakCanary: Activity leaked"通知。
                点击通知可查看引用链，直指 companion object.staticActivityRef。

                【MAT 分析步骤】
                1. Android Studio Profiler -> Heap Dump -> 导出 hprof
                2. 转换格式: hprof-conv original.hprof converted.hprof
                3. 打开 MAT，File -> Open Heap Dump -> converted.hprof
                4. Histogram 视图，输入 "MainActivity" 过滤
                5. 查看对象数量（旋转后应该只有 1 个，多个说明泄漏）
                6. 右键 -> Path To GC Roots -> exclude weak/soft references
                   就能看到谁持有引用
            """.trimIndent(),
            fixDescription = """
                【修复方案】
                使用 WeakReference 包裹 Activity 引用：
                    companion object {
                        // 修复前：强引用，会导致泄漏
                        // var activity: Activity? = null

                        // 修复后：弱引用，不阻碍 GC
                        var activityRef: WeakReference<Activity>? = null
                    }

                关键点：
                - WeakReference 不阻止 GC 回收对象
                - 使用前需判空: activityRef?.get() ?: return
                - 或者直接避免在 static 中持有 Activity 引用
            """.trimIndent()
        ),
        Scenario(
            id = "oom_handler_leak",
            title = "Handler 泄漏",
            description = "匿名 Handler 发送 delayed message，Activity 销毁后 message 仍持有引用",
            category = ScenarioCategory.OOM,
            dangerLevel = 4,
            explanationText = """
                【问题原理】
                匿名内部类 Handler 会隐式持有外部类（Activity）的引用。

                代码示例：
                    val handler = object : Handler(Looper.getMainLooper()) {
                        override fun handleMessage(msg: Message) { ... }
                    }
                    handler.postDelayed({ /* 引用了外部 Activity */ }, 60000)

                当 Activity 调用 finish() 时：
                - Handler 中的 runnable 仍然在 MessageQueue 中
                - Message.target 指向 Handler
                - Handler 持有 Activity 引用（匿名内部类特性）
                - MessageQueue -> Message -> Handler -> Activity 形成引用链

                即使设置了 60 秒延迟，在延迟到达前，Activity 无法被回收。

                【LeakCanary 如何检测】
                会发现 MessageQueue 中存在持有 Activity 引用的 PendingRunnable/Message。
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发泄漏"（会发送一个 60 秒延迟的 message）
                2. 立即按返回键退出页面
                3. 点击"手动 GC"，观察 usedMemory
                4. 等待 60 秒后，再次 GC 观察

                【验证方法】
                - 修复关闭时：退出后 Activity 仍存在于内存中（GC 无法回收）
                - 修复开启时：退出后 Activity 立即可被 GC 回收

                【代码层面的关键点】
                在 MessageQueue 中检查：mMessages 链表是否包含引用当前 Activity 的 Message
            """.trimIndent(),
            fixDescription = """
                【修复方案】三步走

                Step 1: 将 Handler 改为 static 内部类
                    // 匿名内部类会持有外部类引用 -> 改为 static
                    class SafeHandler(looper: Looper) : Handler(looper) {
                        // 如果需要操作 UI，用 WeakReference
                    }

                Step 2: 在 Activity onDestroy 中清理消息队列
                    override fun onDestroy() {
                        super.onDestroy()
                        // 移除所有与该 Handler 关联的回调和消息
                        handler.removeCallbacksAndMessages(null)
                    }

                Step 3: 如果 Handler 需要回调 UI，用 WeakReference
                    class SafeHandler(
                        looper: Looper,
                        val activityRef: WeakReference<Activity>
                    ) : Handler(looper) { ... }
            """.trimIndent()
        ),
        Scenario(
            id = "oom_unregistered_listener",
            title = "未反注册 Listener",
            description = "注册广播/传感器监听后未在 onDestroy 反注册",
            category = ScenarioCategory.OOM,
            dangerLevel = 3,
            explanationText = """
                【问题原理】
                Android 中很多系统服务（广播接收器、传感器、加速度计等）采用
                观察者模式，需要手动反注册。

                泄漏链条：
                Context -> 系统服务注册表 -> BroadcastReceiver/Listener
                        -> 匿名内部类/lambda -> Activity

                以 BroadcastReceiver 为例：
                    // 在 onCreate 中注册
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) { ... }
                    }
                    registerReceiver(receiver, filter)
                    // 缺失: unregisterReceiver(receiver)

                Activity 销毁后，系统仍在向 receiver 发送事件，
                receiver 引用着 Activity，导致泄漏。

                【传感器泄漏同理】
                SensorManager.registerListener() -> Listener 匿名类 -> Activity
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发泄漏"（注册广播接收器）
                2. 按返回键退出页面
                3. 点击"手动 GC"，观察内存

                【LeakCanary】
                检测到 BroadcastReceiver 未反注册。
                堆中会存在：(Context -> Activity) <- receiver

                【修复验证】
                开启修复开关后退出，Activity 应立即被回收
            """.trimIndent(),
            fixDescription = """
                【修复方案】使用 DisposableEffect 管理生命周期（Compose 最佳实践）

                fun MyScreen() {
                    val context = LocalContext.current

                    // 修复版本：onDispose 中执行反注册
                    DisposableEffect(Unit) {
                        val receiver = object : BroadcastReceiver() {
                            override fun onReceive(ctx: Context, intent: Intent) {
                                // 处理广播
                            }
                        }
                        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                        context.registerReceiver(receiver, filter)

                        onDispose {
                            // 关键：在 onDispose 中反注册
                            context.unregisterReceiver(receiver)
                        }
                    }
                }

                【通用原则】
                - register 配对 unregister
                - 在 onDestroy / onDispose 中调用
                - 使用 try-catch 防止重复反注册抛异常
            """.trimIndent()
        ),
        Scenario(
            id = "oom_bitmap",
            title = "Native 内存耗尽",
            description = "循环创建大 Bitmap 耗尽 Native 堆，触发 LMK 杀进程",
            category = ScenarioCategory.NATIVE_MEMORY,
            dangerLevel = 5,
            explanationText = """
                【重要区分】
                本场景演示的是 Native 内存耗尽导致 LMK 杀进程，
                而非 Java 堆耗尽导致的 OutOfMemoryError。

                两者的本质区别：

                | | Java OOM | LMK 杀进程 |
                |---|---|---|
                | 堆类型 | Java 堆 | Native 堆 |
                | 触发结果 | OutOfMemoryError | SIGKILL（进程消失）|
                | 可捕获性 | ✅ 可 catch | ❌ 无法捕获 |
                | 日志特征 | java.lang.OutOfMemoryError | 无堆栈，进程突然消失 |

                【问题原理】
                Bitmap 的像素数据分配在 Native 堆，而非 Java 堆。
                当 Native 堆耗尽时，系统会触发 Low Memory Killer（LMK），
                直接向进程发送 SIGKILL，进程无法做任何处理直接消失。

                计算公式：宽 × 高 × 每像素字节数
                例如：4096 × 4096 × 4 bytes (ARGB_8888) = 64 MB

                Native 堆大小通常远大于 Java 堆，
                但仍有限制（取决于设备配置）。

                【为什么会触发 LMK】
                Native 堆持续增长 → 系统检测到内存压力
                → LMK 优先杀掉高内存占用的进程 → 进程被 SIGKILL
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 LMK"，观察 app 可能直接消失
                2. 如果没有消失，点击"查看内存"观察 native 堆变化
                3. app 消失后，logcat 中无 Java 堆栈

                【关键命令】
                # 观察进程内存使用
                adb shell dumpsys meminfo com.example.androidcrashanalysis

                # 观察 Native 堆分配
                adb shell dumpsys meminfo -n com.example.androidcrashanalysis | grep "Native"

                # LMK 杀进程时，logcat 会打印：
                # "进程被 low memory killer 杀掉"

                【如何区分 LMK 和 Java OOM】
                - Java OOM: logcat 有 "java.lang.OutOfMemoryError" 堆栈
                - LMK: 进程突然消失，logcat 无异常堆栈，只有 "Sending signal. PID: xxx SIG: 9"
            """.trimIndent(),
            fixDescription = """
                【修复方案】Native 内存优化策略

                1. 减小 Bitmap 尺寸
                   - 使用 inSampleSize 压缩：inSampleSize=2 → 内存 1/4
                   - 使用 RGB_565 替代 ARGB_8888：内存减半

                2. 及时释放
                   - 不再使用后调用 bitmap.recycle()
                   - 注意：recycle 后不能再访问

                3. 加载前检查
                   - 计算目标 Bitmap 所需内存
                   - 检查当前可用内存是否足够
                   - 不足时先释放旧的 Bitmap

                4. 使用 BitmapPool
                   - BitmapFactory.Options.inBitmap 复用已有 Bitmap
                   - 减少反复分配/释放带来的内存碎片
            """.trimIndent()
        ),
        Scenario(
            id = "oom_collection",
            title = "集合泄漏",
            description = "全局单例的 List 不断 add 大对象，不清理导致内存耗尽",
            category = ScenarioCategory.OOM,
            dangerLevel = 3,
            explanationText = """
                【问题原理】
                全局单例（如 object : Singleton 或 static 变量）的生命周期
                等同于进程生命周期。

                如果单例中维护一个集合（如 mutableListOf），
                并在每次操作时 add 大对象（如 1MB 的 ByteArray），但不清理：

                    object LeakStore {
                        val leakList = mutableListOf<ByteArray>()

                        fun store(data: ByteArray) {
                            leakList.add(data) // 持续追加，从不清理
                        }
                    }

                问题：
                - 每次 store() 增加 1MB
                - Activity 对象被 add 进 list（引用泄漏）
                - LeakStore.leakList 持有所有 Activity/ByteArray 引用
                - 堆内存持续增长，最终 OOM

                【常见的泄漏场景】
                - 缓存单例：只加不删
                - 全局 Map 存储 Context
                - Log 收集器：日志列表无限增长
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发泄漏"多次（每次增加约 1MB）
                2. 观察内存增长
                3. 点击"手动 GC"，修复关闭时内存不下降

                【MAT 分析重点】
                1. Histogram 视图 -> Byte[] 过滤
                2. 按数量排序，观察是否存在大量 1MB 左右的数组
                3. Path To GC Roots -> exclude weak/soft
                   找到 LeakStore.leakList 作为 GC Root

                【修复验证】
                开启修复后：
                - 退出页面时调用 clearList()
                - GC 后内存明显下降
            """.trimIndent(),
            fixDescription = """
                【修复方案】进入时清理 + 设置容量上限

                object LeakStoreFixed {
                    // 修复1：限制集合最大容量，防止无限增长
                    private val MAX_SIZE = 100
                    private val leakList = mutableListOf<ByteArray>()

                    fun store(data: ByteArray) {
                        // 修复2：超出容量时移除最老的元素（FIFO）
                        if (leakList.size >= MAX_SIZE) {
                            leakList.removeAt(0)
                        }
                        leakList.add(data)
                    }

                    // 修复3：提供清理方法
                    fun clearAll() {
                        leakList.clear()
                    }
                }

                // 最佳实践：使用 LRU Cache 替代简单 List
                // private val cache = LruCache<String, ByteArray>(maxSize)
            """.trimIndent()
        ),

        // ========== ANR 模块 (4个场景) ==========
        Scenario(
            id = "anr_main_thread_sleep",
            title = "主线程 Sleep",
            description = "在主线程执行 Thread.sleep(15000)，阻塞 UI 线程触发 ANR",
            category = ScenarioCategory.ANR,
            dangerLevel = 4,
            explanationText = """
                【问题原理】
                Android 的 UI 更新运行在主线程（Looper / UI Thread）。

                当主线程执行耗时操作（如 sleep）时：
                - 无法响应用户输入事件
                - 无法处理 View 绘制
                - 系统等待超过 5 秒无响应，弹出 ANR 对话框

                代码示例：
                    fun onClick(v: View) {
                        // 在主线程执行 sleep，阻塞了 UI 线程
                        Thread.sleep(15000) // ← 15 秒无响应，系统判定 ANR
                    }

                系统 ANR 超时阈值：
                - 广播接收器前台：10 秒
                - 前台 Activity 输入事件：5 秒
                - 后台 Activity / Service：整个生命周期

                【面试要点】
                ANR 不是异常，是系统的一种保护机制。
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 ANR"
                2. 在按钮点击后的 5-10 秒内，尝试点击页面其他区域
                3. 系统弹出 "App Not Responding" 对话框

                【查看 traces.txt】
                adb pull /data/anr/traces.txt .

                traces 文件关键信息：
                ----- pid 1234 at 2024-01-01 12:00:00 -----
                Cmd line: com.example.androidcrashanalysis

                "main" prio=5 tid=1 Runnable        <-- 主线程
                  | group="main" sCount=0 dsCount=0
                  at java.lang.Thread.sleep!(Native method)
                  at java.lang.Thread.sleep(Thread.java:-1)
                  at MainThreadSleepScenarioKt.triggerAnrBuggy  <-- 罪魁祸首

                【如何判断根因】
                找到 "main" 线程的状态：
                - Runnable: 正在执行 CPU 密集型任务（如 sleep、计算）
                - Blocked: 等待锁
                - Native: 正在执行 native 代码
                - Waiting / TimedWaiting: 等待某个条件

                【CPU usage 高/低】
                - CPU 高 + Runnable：可能是死循环或 sleep
                - CPU 低 + Blocked/Waiting：可能是等待锁或 IO
            """.trimIndent(),
            fixDescription = """
                【修复方案】将耗时操作移到子线程

                // 修复前：在主线程执行（会阻塞 UI）
                fun triggerAnrBuggy() {
                    Thread.sleep(15000) // ❌ 绝对不要在主线程
                }

                // 修复后：使用 Kotlin 协程
                fun triggerAnrFixed() {
                    // Dispatchers.IO: 适合 IO 密集型操作
                    // Dispatchers.Default: 适合 CPU 密集型操作
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(15000) // 不会阻塞主线程
                    }
                }

                // 或者使用 Handler/AsyncTask
                // Handler(Looper.getMainLooper()).postDelayed({ ... }, 15000) // 这是安全的！
                // Handler.postDelayed 不占用 CPU，只注册一个延时任务
            """.trimIndent()
        ),
        Scenario(
            id = "anr_deadlock",
            title = "主线程死锁",
            description = "主线程和子线程互相等待对方持有的锁，形成死锁",
            category = ScenarioCategory.ANR,
            dangerLevel = 5,
            explanationText = """
                【问题原理】
                死锁的四个必要条件（缺一不可）：
                1. 互斥：资源只能被一个线程持有
                2. 持有并等待：线程持有资源的同时等待其他资源
                3. 不可抢占：资源不能被强制释放
                4. 循环等待：形成线程-资源环形等待链

                本场景代码：
                    val lockA = Object()
                    val lockB = Object()

                    // 主线程：先拿 A，再等 B
                    synchronized(lockA) {
                        synchronized(lockB) { ... } // ← 等 B
                    }

                    // 子线程：先拿 B，再等 A
                    thread {
                        synchronized(lockB) {
                            synchronized(lockA) { ... } // ← 等 A
                        }
                    }

                结果：主线程持有 A 等 B，子线程持有 B 等 A → 互相等待 → 死锁

                【危险程度】
                死锁是最严重的 ANR 原因之一：
                - 100% 导致 ANR
                - 难以复现（取决于 CPU 调度时序）
                - 在高并发场景下更容易触发
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 ANR"（先加锁 A）
                2. 子线程获得锁 B，等待 A（死锁形成）
                3. 主线程获得 A，等待 B（死锁形成）
                4. 5 秒后出现 ANR 对话框

                【traces.txt 关键信息】
                看到两个线程都在 BLOCKED 状态，且互相等待对方的锁：

                "main" prio=5 tid=1 Blocked
                  - waiting to lock <0x12345678> (Object A) held by thread 2
                  at ...

                "Thread-1" prio=5 tid=2 Blocked
                  - waiting to lock <0x87654321> (Object B) held by thread 1
                  at ...

                【如何判断是死锁】
                - 多个线程同时处于 BLOCKED 状态
                - 等待的锁形成环路（A 等 B，B 等 A）
                - 找到持有锁的线程，它们也在等待其他锁
            """.trimIndent(),
            fixDescription = """
                【修复方案】统一加锁顺序

                // 核心原则：所有线程必须按相同的顺序获取锁
                // 规定：永远先获取 lockA，再获取 lockB

                // 线程安全的资源管理器
                object ResourceManager {
                    private val lockA = Object()
                    private val lockB = Object()

                    // 修复：无论哪个线程调用，都遵守 A→B 顺序
                    fun doWork() {
                        synchronized(lockA) { // Step 1: 先拿 A
                            synchronized(lockB) { // Step 2: 再拿 B
                                // 业务逻辑
                            }
                        }
                    }
                }

                // 额外建议：减少锁的粒度，能不用 synchronized就不用
                // 使用 ReentrantLock 可以设置超时，避免无限等待：
                // lock.tryLock(5, TimeUnit.SECONDS)
            """.trimIndent()
        ),
        Scenario(
            id = "anr_large_file_io",
            title = "主线程大文件 IO",
            description = "在主线程同步读写大文件，阻塞 UI 线程",
            category = ScenarioCategory.ANR,
            dangerLevel = 3,
            explanationText = """
                【问题原理】
                文件 IO（磁盘读写、网络请求）属于 IO 密集型操作，速度远慢于 CPU。

                问题代码：
                    fun loadData() {
                        // 在主线程执行同步 IO，阻塞 UI
                        val bytes = File("large_file.bin").readBytes() // 100MB 文件
                    }

                大文件 IO 时间估算：
                - SD 卡读取：约 10-30 MB/s
                - 手机内部存储：约 100-300 MB/s
                - 100MB 文件：可能需要 1-10 秒 → 触发 ANR

                【为什么 Android 禁止主线程 IO】
                - 主线程 = UI 线程 = 用户交互响应
                - IO 期间主线程被挂起，无法响应点击、滑动
                - 超时即 ANR

                【常见误用场景】
                - SharedPreferences.getString() 第一次加载（XML 解析）
                - Asset 文件读取
                - 数据库 Query
                - 网络请求（OkHttp / Retrofit 同步调用）
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 ANR"（主线程读取大文件）
                2. 约 5-10 秒后出现 ANR

                【traces.txt 分析】
                主线程处于 Runnable 或 Native 状态，
                堆栈中能看到 FileInputStream / readBytes 等 IO 调用。

                【CPU 使用率判断】
                - IO 操作时 CPU 使用率通常较低（等待磁盘/网络）
                - 如果 CPU 高，可能是加密/压缩操作（也应该移到子线程）

                【Android 官方限制】
                Android 4.0+ 开始禁止主线程网络请求
                Android 3.0+ 开始限制主线程 IO（StrictMode）
            """.trimIndent(),
            fixDescription = """
                【修复方案】使用协程 + Dispatchers.IO

                // 修复前：主线程 IO
                fun loadDataBuggy() {
                    val bytes = File("file.bin").readBytes() // ❌ 阻塞主线程
                }

                // 修复后：IO 调度器
                fun loadDataFixed(onLoaded: (ByteArray) -> Unit) {
                    // Dispatchers.IO: 专为 IO 密集型任务设计
                    // 底层使用共享的线程池，最大并发数约为 CPU 核心数 * 2
                    CoroutineScope(Dispatchers.IO).launch {
                        val bytes = withContext(Dispatchers.IO) {
                            File("file.bin").readBytes() // ✅ 子线程
                        }
                        withContext(Dispatchers.Main) {
                            onLoaded(bytes) // ✅ 回到主线程更新 UI
                        }
                    }
                }

                // 旧代码迁移：HandlerThread / AsyncTask
                // 新代码：Kotlin Coroutine（推荐）
            """.trimIndent()
        ),
        Scenario(
            id = "anr_sp_commit",
            title = "SharedPreferences commit 卡顿",
            description = "主线程大量使用 commit() 同步写入，每次都触发 ANR",
            category = ScenarioCategory.ANR,
            dangerLevel = 2,
            explanationText = """
                【问题原理】
                SharedPreferences 有两种写入模式：

                1. commit() — 同步写入，立即返回 boolean
                   - 写完返回 true/false
                   - 在主线程调用会阻塞等待写入完成
                   - 底层调用 DataOutputStream.writeBytes() + fsync()

                2. apply() — 异步写入，立即返回
                   - 写入到内存后立即返回
                   - 后台线程负责写入磁盘
                   - 无 ANR 风险

                问题代码：
                    repeat(1000) {
                        sp.edit()
                            .putString("key_" + i, "value_" + i)
                            .commit() // ❌ 每次都同步等待写入
                    }

                1000 次 commit 每次约 5-10ms，累计 5-10 秒 → 触发 ANR

                【apply vs commit 选择】
                - 绝大多数场景用 apply()（异步、安全）
                - 仅在需要知道写入结果（success/fail）时用 commit()
                  （如登录状态必须持久化成功才能进入主页）
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 ANR"（执行 1000 次 commit）
                2. 约 5-10 秒后出现 ANR

                【traces.txt 分析】
                主线程堆栈：
                    at android.app.SharedPreferencesImpl.EditorImpl.commit
                    at (your code)
                    at ...

                【如何区分 commit 和 apply】
                traces 中看到 commit：
                    at android.app.SharedPreferencesImpl.EditorImpl.commit(Native)
                    at android.app.SharedPreferencesImpl.EditorImpl.commit
                → 说明在主线程同步写入

                如果是 apply，调用会立即返回，不会出现在 traces 中阻塞。
            """.trimIndent(),
            fixDescription = """
                【修复方案】99% 场景用 apply()，commit() 仅在必要时使用

                // 修复版本：使用 apply 替代 commit
                repeat(1000) {
                    sp.edit()
                        .putString("key_" + i, "value_" + i)
                        .apply() // ✅ 异步写入，不阻塞主线程
                }

                // 如果必须等待写入完成（极少情况），用以下方案：
                fun saveAndWait(sp: SharedPreferences, key: String, value: String) {
                    val latch = CountDownLatch(1)
                    sp.edit().putString(key, value)
                        .apply {
                            registerOnSharedPreferenceChangeListener { _, _ ->
                                latch.countDown()
                            }
                        }
                    latch.await(5, TimeUnit.SECONDS) // 最多等待 5 秒
                }

                // 最推荐的通用写法：
                fun saveAsync(sp: SharedPreferences, key: String, value: String) {
                    sp.edit().putString(key, value).apply() // 简单、安全
                }
            """.trimIndent()
        ),

        // ========== Native Crash 模块 (3个场景) ==========
        Scenario(
            id = "native_nullptr",
            title = "空指针解引用",
            description = "C++ 中解引用空指针，触发 SIGSEGV (Signal 11)",
            category = ScenarioCategory.NATIVE_CRASH,
            dangerLevel = 5,
            explanationText = """
                【问题原理】
                在 C/C++ 中，空指针解引用是未定义行为（UB, Undefined Behavior）。

                C++ 代码：
                    int* ptr = nullptr;
                    *ptr = 42; // ← 解引用空指针

                在 Linux/Android 中，这会触发 SIGSEGV（段错误）信号。

                【Android 中的信号类型】
                - SIGSEGV (11): 段错误 - 访问了无效内存地址
                  （空指针解引用、非法地址访问、栈溢出）
                - SIGABRT (6): 中止信号 - 通常由 assert() 失败或 abort() 调用触发
                  （双重 free、缓冲区溢出检测）
                - SIGBUS (10): 总线错误 - 地址对齐问题或物理地址无效
                  （访问非对齐的内存）

                【SIGSEGV vs SIGABRT 的区别】
                - SIGSEGV: 内存访问违规（你访问了不该访问的地址）
                - SIGABRT: 程序主动调用 abort()（通常因为检测到逻辑错误）
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 Crash"（会弹出确认对话框）
                2. App 崩溃，查看 logcat

                【logcat 关键信息】
                FATAL SIGNAL 11 (SIGSEGV) ...  at NativeCrash.onNativeNullPointerDereference

                【tombstone 文件】
                adb logcat | grep -A 100 "tombstone"
                或
                adb shell cat /data/tombstones/tombstone_00

                tombstone 关键字段：
                - signal: 11 (SIGSEGV)
                - fault addr: 0x0 (空指针地址)
                - backtrace: 崩溃时的调用栈

                【addr2line 还原堆栈】
                1. 获取 so 文件路径和偏移地址
                2. adb pull /data/app/.../lib/arm64-v8a/libnative-crash-lib.so
                3. /path/to/ndk/21.4.7075529/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-addr2line \\
                    -e libnative-crash-lib.so 0x偏移地址
            """.trimIndent(),
            fixDescription = """
                【修复方案】防御性编程 + 空指针检查

                // 修复前：直接解引用
                void triggerBuggy() {
                    int* ptr = nullptr;
                    *ptr = 42; // ❌ 未定义行为，必然 SIGSEGV
                }

                // 修复后：空指针检查
                void triggerFixed() {
                    int* ptr = nullptr;

                    // ✅ 使用前检查
                    if (ptr != nullptr) {
                        *ptr = 42;
                    } else {
                        // 处理空指针情况：记录日志，返回错误码
                        __android_log_print(ANDROID_LOG_ERROR, "Native",
                            "Null pointer detected!");
                    }
                }

                // 或者使用引用替代指针（更安全）
                // int& ref = *(int*)nullptr; // 编译期就会失败
            """.trimIndent()
        ),
        Scenario(
            id = "native_buffer_overflow",
            title = "数组越界写入",
            description = "C++ 中 memcpy 写入超出栈缓冲区边界，触发 SIGABRT/SIGSEGV",
            category = ScenarioCategory.NATIVE_CRASH,
            dangerLevel = 5,
            explanationText = """
                【问题原理】
                栈缓冲区溢出（Stack Buffer Overflow）：

                C++ 代码：
                    char buffer[10];
                    const char* src = "This string is longer than 10 characters!!!";
                    memcpy(buffer, src, strlen(src)); // ← 写入超过 buffer 容量

                后果：
                - 覆盖 buffer 之后的栈内存（return address、saved registers）
                - 破坏函数调用栈帧
                - 可能触发 SIGSEGV 或 SIGABRT

                【Android 的缓解措施】
                - Stack Canaries：gcc -fstack-protector 会在栈上插入 canary 值，
                  被覆盖时触发 SIGABRT
                - PIE (Position Independent Executable)：地址空间随机化
                - DEP/NX：栈不可执行

                【SIGABRT vs SIGSEGV】
                - 栈溢出通常触发 SIGABRT（因为触发了 canary 检查）
                - 简单越界可能触发 SIGSEGV（访问到不可映射地址）
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 Crash"（确认对话框）
                2. App 崩溃，查看 logcat

                【logcat 信息】
                // 触发 canary 检查：
                FATAL SIGNAL 6 (SIGABRT) ...
                "NativeCrash" prio=5 tid=1 Abort
                  at abort (abort.cpp:+74)

                // 直接崩溃：
                FATAL SIGNAL 11 (SIGSEGV) ...

                【tombstone 分析】
                查看 fault addr 是否在栈地址范围内（通常 0x7fff_xxxx 格式）
                backtrace 中能看到 memcpy 调用

                【ASAN（AddressSanitizer）】
                在 CMakeLists.txt 中添加：
                    add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
                    link_libraries(-fsanitize=address)
                ASAN 能在开发阶段更精确地检测越界访问
            """.trimIndent(),
            fixDescription = """
                【修复方案】使用安全的内存拷贝函数

                // 修复前：不检查长度
                void triggerBuggy() {
                    char buffer[10];
                    const char* src = "Very long string...";
                    memcpy(buffer, src, strlen(src)); // ❌ 无边界检查
                }

                // 修复方案1：使用 strncpy / snprintf 限制长度
                void triggerFixed1() {
                    char buffer[10];
                    const char* src = "Very long string...";
                    strncpy(buffer, src, sizeof(buffer) - 1);
                    buffer[sizeof(buffer) - 1] = '\\0'; // 确保 null-terminated
                }

                // 修复方案2：使用 std::string（自动管理内存）
                void triggerFixed2() {
                    std::string buffer(10, '\\0');
                    std::string src = "Very long string...";
                    std::copy_n(src.begin(), std::min(src.size(), buffer.size()), buffer.begin());
                }

                // 修复方案3：使用 memcpy_s（C11，安全版本）
                // memcpy_s(dest, destSize, src, count) // 越界返回错误码
            """.trimIndent()
        ),
        Scenario(
            id = "native_uaf",
            title = "Use-After-Free",
            description = "free() 后继续使用指针，触发 SIGSEGV",
            category = ScenarioCategory.NATIVE_CRASH,
            dangerLevel = 5,
            explanationText = """
                【问题原理】
                Use-After-Free（UAF，释放后使用）是内存安全的经典漏洞：

                C++ 代码：
                    int* ptr = (int*)malloc(sizeof(int));
                    *ptr = 42;
                    free(ptr);      // ← 释放内存
                    *ptr = 100;     // ← 继续使用已释放的指针 ❌

                危险之处：
                1. free 后内存内容可能未清零（取决于实现）
                2. 被 free 的内存可能被其他代码重新 malloc 分配
                3. 两次操作之间存在竞态条件

                攻击利用：
                - 攻击者可以控制重新分配的内容
                - 可以构造任意代码执行（经典的 CTF/PWN 技术）

                【其他 UAF 变种】
                - Double Free：free 两次同一指针
                - Dangling Pointer：指针被 delete 后未置空
                - 返回栈上变量地址（返回局部变量引用）
            """.trimIndent(),
            investigationMethod = """
                【排查步骤】
                1. 点击"触发 Crash"
                2. App 崩溃

                【logcat】
                FATAL SIGNAL 11 (SIGSEGV) ...
                fault addr: 某个非零地址（不是 0x0）
                ← 说明访问了一个已释放的地址（而不是真正的空指针）

                【tombstone 分析】
                - SIGSEGV fault addr 不是 0x0（这是和空指针的关键区别）
                - backtrace 中能看到 free/malloc 调用

                【如何区分 UAF 和空指针】
                - 空指针解引用：fault addr = 0x0
                - UAF：fault addr = 某个有效的（但已无效的）地址

                【ASAN 检测 UAF】
                ASAN 能检测到 UAF 并给出精确的错误信息：
                "heap-use-after-free, address 0x... was freed here:"
            """.trimIndent(),
            fixDescription = """
                【修复方案】释放后置空 + 智能指针

                // 修复前：free 后不置空
                void triggerBuggy() {
                    int* ptr = (int*)malloc(sizeof(int));
                    *ptr = 42;
                    free(ptr);
                    // ptr 仍指向已释放的内存，但不为 nullptr
                    *ptr = 100; // ❌ UAF
                }

                // 修复1：free 后立即置空
                void triggerFixed1() {
                    int* ptr = (int*)malloc(sizeof(int));
                    *ptr = 42;
                    free(ptr);
                    ptr = nullptr; // ✅ 置空，后续使用会触发明确的空指针异常
                    if (ptr != nullptr) {
                        *ptr = 100;
                    }
                }

                // 修复2：使用智能指针（C++11，现代方案）
                void triggerFixed2() {
                    // unique_ptr: 独占所有权，作用域结束时自动 free
                    auto ptr = std::make_unique<int>(42);

                    // 作用域结束时自动释放，无需手动 free
                    // 如果需要 shared ownership，用 std::shared_ptr
                }
            """.trimIndent()
        )
    )

    /** 按分类分组获取所有场景 */
    fun getScenariosByCategory(): Map<ScenarioCategory, List<Scenario>> {
        return allScenarios.groupBy { it.category }
    }

    /** 根据 ID 查找场景 */
    fun getScenarioById(id: String): Scenario? {
        return allScenarios.find { it.id == id }
    }

    /** 获取所有场景列表 */
    fun getAllScenarios(): List<Scenario> = allScenarios
}
