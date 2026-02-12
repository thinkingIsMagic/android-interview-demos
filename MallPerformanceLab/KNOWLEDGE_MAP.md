# 性能优化知识地图

> Mall Performance Lab 对应的实战知识点，6大类别，每类1-2个可运行的例子

---

## 类别一：缓存策略

### 核心原理
> 用空间换时间，把「计算/网络结果」存下来，避免重复劳动

### 本项目例子

| 例子 | 文件 | 关键代码 | 面试怎么说 |
|------|------|----------|-----------|
| 内存缓存 | `MemoryCache.kt` | `LruCache<K,V>` | "用系统LruCache，自动LRU淘汰" |
| 磁盘缓存 | `DiskCache.kt` | `SharedPreferences` | "SP做简单kv缓存，生产用Room" |
| TTL过期 | `DiskCache.kt:isExpired()` | `System.currentTimeMillis()` | "设置TTL，数据不会永远stale" |

### 延伸知识点
- 多级缓存架构
- 缓存穿透/击穿/雪崩
- 一致性哈希（分布式缓存）

---

## 类别二：预请求策略

### 核心原理
> 把串行变并行，把「等要用」变成「提前准备」

### 本项目例子

| 例子 | 文件 | 关键代码 | 面试怎么说 |
|------|------|----------|-----------|
| 预请求首屏 | `PreFetcher.kt:preFetch()` | 延迟500ms触发 | "进入页面前提前发，不阻塞首帧" |
| 请求去重 | `PreFetcher.kt:preFetchedKeys` | `ConcurrentHashMap` | "相同key只发一次，防重复" |
| Feed预取 | `PreFetcher.kt:preFetchFeed()` | 剩余3条时触发 | "滚动到底部前提前加载下一页" |

### 延伸知识点
- 预热/预连接
- 请求合并（Batching）
- 带宽感知调度

---

## 类别三：预创建策略

### 核心原理
> 提前创建昂贵对象，消除主线程关键路径耗时

### 本项目例子

| 例子 | 文件 | 关键代码 | 面试怎么说 |
|------|------|----------|-----------|
| View预创建 | `ViewPreWarmer.kt:preWarm()` | `LayoutInflater.inflate()` | "Application时就创建好" |
| ViewPool | `ViewPreWarmer.kt:acquire/release` | 池化复用 | "用完放回池子，减少GC" |
| 状态重置 | `ViewPreWarmer.kt:resetView()` | 递归清理tag/text | "防止复用时数据污染" |

### 延伸知识点
- ObjectPool模式
- 享元模式（Flyweight）
- 启动优化（ Splash -> Main）

---

## 类别四：异步与并发

### 核心原理
> 充分利用多核CPU，IO不阻塞计算

### 本项目例子

| 例子 | 文件 | 关键代码 | 面试怎么说 |
|------|------|----------|-----------|
| Coroutine | `MallRepository.kt` | `Dispatchers.IO` | "IO操作扔子线程，主线程不卡" |
| 并发控制 | `OptimizedPreFetcher.kt` | `AtomicInteger` | "控制最大并行数，防崩溃" |
| 协程作用域 | `MainActivity.kt:scope` | `SupervisorJob` | "子协程失败不影响父协程" |

### 延伸知识点
- 线程池原理
- Kotlin Flow
- Mutex vs synchronized

---

## 类别五：列表优化

### 核心原理
> RecyclerView的「三板斧」：ViewHolder + DiffUtil + Payload

### 本项目例子

| 例子 | 文件 | 关键代码 | 面试怎么说 |
|------|------|----------|-----------|
| DiffUtil | `MarketingFloorAdapter.kt` | `ListAdapter<>` | "只更新变化的item，不全量刷新" |
| ViewType | `FeedAdapter.kt:getItemViewType()` | 多类型支持 | "不同布局走不同ViewHolder" |
| 骨架屏 | `item_feed_product.xml` | `tvImagePlaceholder` | "加载时显示占位，用户感知更快" |

### 延伸知识点
- RecyclerView预取（Prefetch）
- 嵌套滑动冲突
- 列表秒开方案

---

## 类别六：可观测性

### 核心原理
> 不能测量就不能优化，埋点要结构化

### 本项目例子

| 例子 | 文件 | 关键代码 | 面试怎么说 |
|------|------|----------|-----------|
| 性能打点 | `PerformanceTracker.kt` | `begin/end` | "关键链路打点：冷启动→首帧→首屏→可交互" |
| 结构化日志 | `TraceLogger.kt` | `[CACHE] tag` | "按类别前缀，grep好筛选" |
| 模式切换 | `FeatureToggle.kt` | `Baseline/Optimized` | "开关控制，对比验证" |

### 延伸知识点
- Systrace/Perfetto
- APM平台架构
- 指标设计（分位值/P90/P99）

---

## 知识地图总览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        性能优化知识地图                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   缓存策略 ────→ 预请求 ────→ 预创建 ────→ 异步并发 ────→ 列表优化       │
│      │             │             │              │              │        │
│      ▼             ▼             ▼              ▼              ▼        │
│   LruCache      延迟触发      ViewPool      Dispatchers    DiffUtil    │
│   TTL过期       请求去重      acquire/release IO线程        ViewHolder  │
│   多级查询      优先级队列     状态重持       并发控制       骨架屏      │
│                                                                          │
│                              │                                         │
│                              ▼                                         │
│                    ┌─────────────────┐                                │
│                    │   可观测性        │                                │
│                    │   打点+日志+报告  │                                │
│                    └─────────────────┘                                │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 推荐学习路径

1. **入门**: 先搞懂缓存+异步（最常用，效果最明显）
2. **进阶**: 预创建+列表优化（进阶优化，面试加分项）
3. **高阶**: 可观测性+预请求策略（架构思维）

---

## 面试金句

> "性能优化不是玄学，是**可测量、可验证**的工程。我做每个优化都会打点，对比Baseline和Optimized的效果。"
>
> "我不是盲目加缓存，而是根据**数据访问模式**决定：热点数据进内存，冷数据不进。"
>
> "预创建的精髓是**把耗时操作移出首帧关键路径**，让用户感觉更快。"
