# Mall Performance Lab 代码学习指南

> 本指南手把手教你如何阅读和学习这个项目的代码。

---

## 一、学习顺序建议

**推荐顺序**: FeatureToggle → PerformanceTracker → MemoryCache → MallRepository → MainActivity

这个顺序是从基础设施到业务逻辑的递进。FeatureToggle 是开关，理解它才能知道优化是如何控制的。PerformanceTracker 是打点系统，理解它才能看懂日志。MemoryCache 是缓存实现，是最经典的优化案例。MallRepository 整合了各种优化策略。MainActivity 是页面入口，展示所有优化的效果。

---

## 二、逐个文件详细学习

### 2.1 FeatureToggle.kt （必须首先阅读）

**文件路径**: `core/config/FeatureToggle.kt`

**核心作用**: 整个项目的"开关"，控制优化策略的开启和关闭。

**学习方法**:

第1步：理解 enum Mode。阅读第21-24行，Mode 有两个值 BASELINE 和 OPTIMIZED，这是 AB 测试的基础。

```kotlin
enum class Mode {
    BASELINE,  // 对照组：无优化
    OPTIMIZED  // 实验组：启用优化
}
```

第2步：理解 currentMode。阅读第28行，`@Volatile var currentMode` 保证多线程可见性。理解为什么用 @Volatile（可见性）。

第3步：理解独立开关。阅读第39-60行的 object Optimized，这里定义了7个独立开关：
- enableCache: 缓存开关
- enablePreFetch: 预请求开关
- enableViewPreWarm: View预创建开关
- 其他...

第4步：理解便捷方法。阅读第68-74行，每个 useXxx() 方法的判断逻辑是：`isOptimized && 对应开关`。这意味着只有同时满足"优化模式+开关开启"才会生效。

**关键设计模式**: 组合模式 + 开关模式。多个独立开关组合成一个大的优化模式。

**面试考点**:
1. @Volatile 的作用是什么？（可见性）
2. 为什么用 object 而不是 class？（单例）
3. 如何动态切换开关？（toggleMode() 方法）

---

### 2.2 PerformanceTracker.kt （必须阅读）

**文件路径**: `core/perf/PerformanceTracker.kt`

**核心作用**: 性能打点系统，采集和报告性能数据。

**学习方法**:

第1步：理解两个数据类。阅读第36-44行 TraceRecord（单次打点记录）和第49-57行 TraceSummary（聚合统计）。

```kotlin
// 单次记录：记录每一次打点的详细信息
data class TraceRecord(
    val name: String,           // 打点名称
    val startTime: Long,        // 开始时间
    val endTime: Long,          // 结束时间
    val mode: FeatureToggle.Mode, // 当时的运行模式
    val threadName: String      // 所在线程
)

// 聚合统计：同名打点的汇总
data class TraceSummary(
    val name: String,
    val count: Int,      // 调用次数
    val avgMs: Long,      // 平均耗时
    val minMs: Long,      // 最短耗时
    val maxMs: Long,      // 最长耗时
    val lastMs: Long,     // 最近一次耗时
    val mode: FeatureToggle.Mode
)
```

第2步：理解三个存储结构。阅读第62-68行：
- records: 所有打点记录（CopyOnWriteArrayList 线程安全）
- summaryMap: 同名打点聚合统计（ConcurrentHashMap）
- activeTraces: 正在进行的打点（支持嵌套打点）

第3步：理解核心API。阅读第76-116行：
- begin(name): 开始打点，同时调用系统 Trace API
- end(name): 结束打点，计算耗时，打印日志
- trace(name) { }: 便捷方法，用法示例：

```kotlin
// 用法1：手动 begin/end
PerformanceTracker.begin("my_operation")
doSomething()
PerformanceTracker.end("my_operation")

// 用法2：inline 写法（推荐）
PerformanceTracker.trace("my_operation") {
    doSomething()
}
```

第4步：理解输出报告。阅读第173-245行 dump() 方法：
- 按模式分组统计（BASELINE vs OPTIMIZED）
- 按名称聚合计算 avg/min/max
- 关键指标对比（冷启动、首屏数据、首屏渲染、可交互）

**关键技术点**:
- System.nanoTime() 比 System.currentTimeMillis() 更精确
- CopyOnWriteArrayList 适合读多写少场景
- ConcurrentHashMap 支持高并发访问
- Trace API 可用于 systrace/perfetto 分析

**面试考点**:
1. CopyOnWriteArrayList 有什么特点？为什么适合这个场景？
2. ConcurrentHashMap 和 HashMap 的区别？
3. data class 自动生成了哪些方法？
4. 如何用这个系统做 AB 测试？

---

### 2.3 MemoryCache.kt （推荐阅读）

**文件路径**: `core/cache/MemoryCache.kt`

**核心作用**: 基于 LruCache 的内存缓存实现。

**学习方法**:

第1步：理解构造函数。阅读第14-22行：
- 默认最大100条
- companion object 定义常量

第2步：理解 LruCache 继承。阅读第25-44行：
- sizeOf() 默认返回1（每条算1）
- entryRemoved() 淘汰时打印日志

```kotlin
private val cache: LruCache<K, V> = object : LruCache<K, V>(maxSize) {
    override fun sizeOf(key: K, value: V): Int = 1
    override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
        if (evicted) TraceLogger.Cache.evict(key.toString())
    }
}
```

第3步：理解双缓存设计。阅读第47行和第52-83行：
- cache: LruCache 强引用缓存
- weakCache: WeakReference 弱引用缓存

```kotlin
// 弱引用缓存，用于存放大对象
private val weakCache = mutableMapOf<K, WeakReference<V>>()

fun put(key: K, value: V) {
    cache.put(key, value)              // 放入强引用
    weakCache[key] = WeakReference(value)  // 同时放入弱引用
}

fun get(key: K): V? {
    // 1. 先查强引用
    cache.get(key)?.let { return it }

    // 2. 查弱引用，找到后升级到强引用
    weakCache[key]?.get()?.let {
        put(key, it)  // 升级
        return it
    }

    return null
}
```

第4步：理解淘汰机制。LruCache 自动淘汰最近最少使用的条目，WeakReference 在内存紧张时自动回收。

**双缓存设计的目的**: 强引用保证热点数据不丢失，弱引用作为兜底避免 OOM。

**面试考点**:
1. LruCache 的淘汰算法是什么？（最近最少使用）
2. WeakReference 和 StrongReference 的区别？
3. 双重缓存的目的是什么？
4. entryRemoved 回调有什么用？

---

### 2.4 MallRepository.kt （核心业务，必须阅读）

**文件路径**: `data/repository/MallRepository.kt`

**核心作用**: 数据仓库，整合缓存和网络请求。

**学习方法**:

第1步：理解成员变量。阅读第30-51行：
- memoryCache: MallData? 内存缓存
- sp: SharedPreferences 磁盘缓存
- CACHE_EXPIRE_MS = 5分钟 过期时间
- currentPage/hasMore: 分页状态
- feedMutex: Mutex 并发控制

```kotlin
private var memoryCache: MallData? = null
private var cacheTime: Long = 0

private val sp: SharedPreferences by lazy {
    context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
}

private val feedMutex = Mutex()  // Feed加载的并发控制
```

第2步：理解 getMallData() 流程。阅读第64-104行，这是核心方法：

```kotlin
suspend fun getMallData(forceRefresh: Boolean = false): Pair<MallData?, Long> {
    val startTime = System.currentTimeMillis()

    // 【优化1】缓存读取：优先返回缓存，同时异步刷新
    if (!forceRefresh && FeatureToggle.useCache()) {
        val cached = getFromCache()
        if (cached != null) {
            return cached to latency  // 缓存命中，极快返回
        }
    }

    // 【优化2】IO 到子线程
    val (data, networkLatency) = withContext(Dispatchers.IO) {
        // 网络请求（模拟）
    }

    // 【优化3】缓存写入
    if (FeatureToggle.useCache()) {
        saveToCache(data)
    }

    return data to networkLatency
}
```

第3步：理解多级缓存查询。阅读第171-206行 getFromCache()：

```kotlin
private fun getFromCache(): MallData? {
    // 1. 先查内存
    getFromMemory()?.let { return it }

    // 2. 查磁盘
    val json = sp.getString(KEY_MALL_DATA, null) ?: return null

    // 3. 检查过期
    if (isCacheValid()) {
        val data = deserializeMallData(json)
        memoryCache = data  // 写入内存
        return data
    }

    return null
}

private fun isCacheValid(): Boolean {
    return System.currentTimeMillis() - cacheTime < CACHE_EXPIRE_MS
}
```

第4步：理解 Feed 预取。阅读第111-132行 loadMoreFeed()：
- 使用 Mutex.withLock 保证并发安全
- 页码管理 currentPage++

```kotlin
suspend fun loadMoreFeed(): Pair<List<FeedItem>, Long> {
    return feedMutex.withLock {
        val pageToLoad = currentPage
        // 网络请求...
        currentPage++
        hasMore = currentPage < 5
        return items to latency
    }
}
```

第5步：理解预请求。阅读第139-149行 preFetchMallData()：

```kotlin
suspend fun preFetchMallData() {
    if (!FeatureToggle.usePreFetch()) return

    // 异步执行，不阻塞
    withContext(Dispatchers.IO) {
        getMallData(forceRefresh = true)
    }
}
```

**关键技术点**:
- withContext(Dispatchers.IO) 切换到 IO 线程
- Mutex.withLock 保证协程并发安全
- 缓存写入是"先内存后磁盘异步"
- 缓存读取是"内存优先，磁盘兜底"

**面试考点**:
1. 为什么 getMallData 返回 Pair 而不是直接返回数据？
2. Mutex 和 synchronized 的区别？
3. 为什么磁盘缓存要异步写入？
4. isCacheValid() 的设计有什么问题？（时区问题）

---

### 2.5 MainActivity.kt （理解整体流程）

**文件路径**: `ui/MainActivity.kt`

**核心作用**: 商城首页，集成所有优化策略的入口。

**学习方法**:

第1步：理解性能链路打点。阅读第31-35行注释，定义4个关键指标：

```kotlin
// 性能链路打点：
// 1. page_onCreate -> 页面创建
// 2. perf_mall_first_data -> 首屏数据到达
// 3. perf_mall_first_content -> 首屏渲染完成
// 4. perf_mall_interactive -> 首屏可交互
```

第2步：理解数据加载流程。阅读第157-196行 loadData()：

```kotlin
private fun loadData() {
    PerformanceTracker.begin("page_onCreate")

    PerformanceTracker.begin("perf_mall_first_data")

    scope.launch {
        val (data, latency) = repository.getMallData()

        PerformanceTracker.end("perf_mall_first_data")

        if (data != null) {
            updateMallData(data)

            PerformanceTracker.begin("perf_mall_first_content")
            rvMall.post {
                PerformanceTracker.end("perf_mall_first_content")

                PerformanceTracker.begin("perf_mall_interactive")
                rvMall.postDelayed({
                    PerformanceTracker.end("perf_mall_interactive")
                }, 100)
            }
        }
    }
}
```

第3步：理解滚动加载更多。阅读第114-127行：

```kotlin
rvMall.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = rv.layoutManager as LinearLayoutManager
        val totalItemCount = layoutManager.itemCount
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        // 滚动到剩余3条时触发加载
        if (lastVisible >= totalItemCount - 3 && repository.hasMoreFeed()) {
            loadMoreFeed()
        }
    }
})
```

第4步：理解图片异步加载。阅读第288-308行：

```kotlin
private fun loadImageAsync(imageView: ImageView, url: String) {
    scope.launch(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeStream(URL(url).openStream())

        withContext(Dispatchers.Main) {
            imageView.setImageBitmap(bitmap)
        }
    }
}
```

**关键技术点**:
- rvMall.post 和 postDelayed 的区别
- CoroutineScope(Dispatchers.Main + SupervisorJob) 的含义
- RecyclerView.OnScrollListener 的使用
- submitList() 会触发 DiffUtil

**面试考点**:
1. post 和 postDelayed 的区别？
2. SupervisorJob 有什么作用？
3. addOnScrollListener 和 setOnScrollListener 的区别？
4. 为什么图片加载用 Dispatchers.IO？

---

## 三、辅助文件学习

### 3.1 TraceLogger.kt

**文件路径**: `core/perf/TraceLogger.kt`

**核心作用**: 结构化日志输出，便于在 Logcat 中筛选。

**学习方法**:

阅读第1-80行，理解日志分类：
- [CACHE] 缓存相关
- [NETWORK] 网络相关
- [PERF] 性能打点
- [PREFETCH] 预请求
- [PREWARM] 预创建

```kotlin
object Cache {
    fun hit(key: String) = println("[CACHE_HIT] $key")
    fun miss(key: String) = println("[CACHE_MISS] $key")
    fun save(key: String) = println("[CACHE_SAVE] $key")
}
```

**面试考点**:
1. 为什么日志要加前缀？（便于 grep 筛选）
2. object 单例的特点？

---

### 3.2 DiskCache.kt

**文件路径**: `core/cache/DiskCache.kt`

**核心作用**: 基于 SharedPreferences 的磁盘缓存。

**学习方法**:

阅读第1-80行，理解：
- getOrCreate() 懒加载
- isExpired() 过期判断
- save() / get() / remove() API

```kotlin
fun save(key: String, value: String, ttlMinutes: Int = 60) {
    val expiresAt = System.currentTimeMillis() + ttlMinutes * 60 * 1000
    sp.edit().putString(key, value).putLong("${key}_expires", expiresAt).apply()
}
```

**注意**: SharedPreferences 不适合存大量数据，生产环境应用 DataStore 或 MMKV。

---

### 3.3 DataGenerator.kt

**文件路径**: `data/mock/DataGenerator.kt`

**核心作用**: 模拟网络请求，生成测试数据。

**学习方法**:

阅读第1-80行，理解：
- generateMallData() 生成首页数据
- generateFeed() 生成 Feed 数据
- randomDelay() 模拟网络延迟

```kotlin
suspend fun generateMallData(callback: (MallData, Long) -> Unit) {
    randomDelay(500, 1500)  // 模拟 500-1500ms 延迟
    val data = createMallData()
    callback(data, 1000)
}
```

---

## 四、按功能分类学习

### 4.1 缓存相关文件

| 文件 | 作用 | 关键代码行 | 学习重点 |
|------|------|-----------|---------|
| MemoryCache.kt | 内存缓存 | 52-83 | LruCache + WeakReference 双缓存 |
| DiskCache.kt | 磁盘缓存 | 40-60 | SharedPreferences + TTL |
| OptimizedCacheManager.kt | 缓存管理 | - | 缓存整合 |

### 4.2 性能相关文件

| 文件 | 作用 | 关键代码行 | 学习重点 |
|------|------|-----------|---------|
| PerformanceTracker.kt | 打点系统 | 76-116 | begin/end/trace API |
| TraceLogger.kt | 日志 | 全部 | 结构化日志设计 |
| MemoryMonitor.kt | 内存监控 | - | OOM 防护 |

### 4.3 UI 相关文件

| 文件 | 作用 | 关键代码行 | 学习重点 |
|------|------|-----------|---------|
| MainActivity.kt | 首页 | 65-196 | 性能链路 + 协程使用 |
| MarketingFloorAdapter.kt | 楼层Adapter | - | DiffUtil |
| FeedAdapter.kt | Feed Adapter | - | ListAdapter |

---

## 五、学习方法建议

### 5.1 本地运行测试

```bash
# 1. 用 Android Studio 打开项目
# 2. 连接真机或模拟器
# 3. 运行 app
# 4. 观察 Logcat 日志
adb logcat -s MallPerfLab
```

### 5.2 动手修改验证

**练习1**: 在 MemoryCache 中添加统计信息

```kotlin
// 在 MemoryCache 中添加
var hitCount = 0
var missCount = 0

fun get(key: K): V? {
    val value = cache.get(key)
    if (value != null) hitCount++ else missCount++
    // ...
}
```

**练习2**: 在 MainActivity 中添加新的打点

```kotlin
// 在 loadData() 中添加
PerformanceTracker.begin("my_custom_operation")
// ... 你的代码
PerformanceTracker.end("my_custom_operation")
```

**练习3**: 模拟缓存失效

```kotlin
// 在 DiskCache.isExpired() 中修改 TTL
private const val CACHE_EXPIRE_MS = 1 * 60 * 1000L  // 改为1分钟
```

### 5.3 面试模拟问答

**Q1: 这个项目如何做 AB 测试？**

A: 通过 FeatureToggle.currentMode 切换 BASELINE 和 OPTIMIZED 两种模式。PerformanceTracker 会根据当前模式分开统计打点数据，dump() 方法会自动对比两种模式的效果。

**Q2: 缓存的过期策略如何设计？**

A: 项目使用 TTL（Time To Live）策略。写入时记录时间戳，读取时判断是否过期。这种策略简单有效，适合数据更新频率稳定的场景。

**Q3: 为什么用协程而不是线程池？**

A: 协程更轻量，一个线程可以运行多个协程。withContext(Dispatchers.IO) 会自动使用线程池，比手动管理线程池更简洁。

**Q4: 如何保证缓存的线程安全？**

A: LruCache 本身是线程安全的。MemoryCache 使用 CopyOnWriteArrayList 和 ConcurrentHashMap 保证并发安全。MallRepository 使用 Mutex 保证协程并发安全。

---

## 六、常见问题与解答

### Q1: SharedPreferences 有什么缺点？

A: 官方已不推荐在主线程使用 commit()，apply() 是异步但可能丢数据。不支持多进程。大量数据时性能差。建议生产环境用 DataStore 或 MMKV。

### Q2: 为什么用 CopyOnWriteArrayList？

A: 适合读多写少场景。打点记录是"写一次，之后只读"，所以适合。缺点是每次写入会复制整个数组，所以不适合频繁写入。

### Q3: data class 有什么限制？

A: 必须是 final，不能继承。自动生成 equals/hashCode/toString/copy/componentN。成员变量 val（只读）时，copy 会报错。

### Q4: MutableList += 操作有什么问题？

A: 在协程中直接修改 MutableList 不是线程安全的。FeedAdapter 中应该用 submitList() 触发 DiffUtil 更新。

---

**文档版本**: 1.1
**最后更新**: 2024年
