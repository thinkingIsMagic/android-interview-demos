# Mall Performance Lab - 商城页性能优化实验场

> 一个用于学习、理解、验证商城页性能优化策略的Android Demo项目

## 项目简介

本项目通过**Baseline vs Optimized**双版本对照，帮助理解电商App首页/详情页常用的性能优化手段。所有优化均可观测、可测量。

## 架构设计

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Mall Performance Lab                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐│
│  │   Application   │    │    MainActivity │    │     PerformanceTracker  ││
│  │   (冷启动入口)   │    │   (页面渲染)     │    │     (全链路打点)        ││
│  └────────┬────────┘    └────────┬────────┘    └─────────────────────────┘│
│           │                        │                                               │
│           ▼                        ▼                                               │
│  ┌─────────────────┐    ┌─────────────────┐                                      │
│  │ ViewPreWarmer    │    │   MallRepository│                                      │
│  │  (View预创建)     │    │   (数据层)       │                                      │
│  └────────┬────────┘    └────────┬────────┘                                      │
│           │                        │                                               │
│           └────────────┬───────────┘                                               │
│                        ▼                                                           │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                          优化策略层                                           ││
│  ├────────────────┬────────────────┬────────────────┬───────────────────────────┤│
│  │   缓存策略      │   预请求策略     │   图片策略      │   内存策略               ││
│  │  MemoryCache   │   PreFetcher    │ BitmapProcessor│   MemoryMonitor          ││
│  │  DiskCache     │   NetworkOpt    │ 采样/复用      │   OOM防护                ││
│  └────────────────┴────────────────┴────────────────┴───────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                          UI优化层                                            ││
│  │     RecyclerViewOptimizer (Prefetch/DiffUtil)                               ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │                          可观测性层                                          ││
│  │     TraceLogger (结构化日志) + PerformanceTracker (性能打点)                  ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 数据流

```
用户启动/进入页面
        │
        ▼
┌───────────────────┐
│  1. ViewPreWarmer │───预创建首屏View─────┐
│    (冷启动时)      │                      │
└───────────────────┘                      │
                                          ▼
┌───────────────────┐              ┌───────────────┐
│  2. PreFetcher    │──预请求首屏数据──▶│   缓存读取    │
│  (进入页面前)      │                  │ (Memory+Disk)│
└───────────────────┘                  └───────────────┘
        │                                      │
        │◀────缓存命中──────返回缓存数据───────┘
        │          │
        │          ▼
        │  ┌───────────────────┐
        └──│  3. 数据到达渲染    │───UI渲染─────▶ 首屏展示
           │  (Performance)     │
           └───────────────────┘
```

## 优化点详解

### 1. 缓存策略 (Cache)

**怎么做：**
- 内存缓存：`MemoryCache.kt` 使用 LruCache 实现
- 磁盘缓存：`DiskCache.kt` 使用 SharedPreferences 实现
- 多级查询：内存 → 磁盘
- TTL策略：首页5分钟、Feed2分钟、配置30分钟

**为什么有效：**
- 内存缓存命中耗时 < 1ms
- 磁盘缓存命中耗时 < 10ms
- 网络请求耗时 300-1500ms
- 缓存可减少 90%+ 的网络请求

**风险与副作用：**
- 内存占用增加（可控，LruCache自动淘汰）
- 数据可能 stale（设置合理TTL）
- 复杂度增加（需处理缓存失效）

**验证方法：**
```bash
# 查看日志
adb logcat -s MallPerfLab

# 观察缓存命中/未命中
# [CACHE_HIT] mall_data
# [CACHE_MISS] mall_data
```

### 2. 预请求策略 (PreFetch)

**怎么做：**
- `PreFetcher.kt`: 提前触发网络请求
- 延迟执行：启动后500ms，避免带宽争抢
- 请求去重：相同key只发一次
- 优先级：首屏数据优先级最高

**为什么有效：**
- 用户进入页面时，数据已就绪
- 与页面初始化并行执行
- 减少用户等待感

**风险与副作用：**
- 可能浪费流量（用户未进入页面）
- 预取数据可能过期
- 增加服务端压力

**验证方法：**
```bash
# 观察预请求日志
adb logcat -s MallPerfLab | grep PREFETCH
# [PREFETCH_START] mall_data
# [PREFETCH_DONE] mall_data = 320ms
```

### 3. 预加载策略 (PreLoad)

**怎么做：**
- 图片预加载：异步下载+解码
- View预创建：`ViewPreWarmer.kt`
- Feed预取：剩余3条时预取下一页

**为什么有效：**
- inflate是耗时操作（5-20ms/次）
- 图片解码是CPU密集操作
- 预创建可消除首帧卡顿

**风险与副作用：**
- 内存占用增加
- 预加载未使用=浪费
- 复杂度增加

**验证方法：**
```bash
# 查看预创建统计
adb logcat -s MallPerfLab | grep PREWARM
# [PREWARM_DONE] all (count=24)
```

### 4. 预创建策略 (ViewPreWarm)

**怎么做：**
- Application启动时预创建首屏View
- ViewType分类池管理
- acquire/release复用模式
- 状态重置防止数据污染

**为什么有效：**
- inflate耗时移出首帧关键路径
- ViewPool复用减少GC
- 减少卡顿和掉帧

**风险与副作用：**
- 启动时间增加（但首帧更快）
- 内存占用（可控）
- View状态管理复杂度

**验证方法：**
```bash
# 对比Trace
# Baseline: inflate耗时20ms
# Optimized: 预创建0ms
```

### 5. Bitmap优化策略

**怎么做：**
- `BitmapProcessor.kt`: 采样率加载、格式选择、内存复用
- inSampleSize: 长宽变为1/2，内存变为1/4
- RGB_565 vs ARGB_8888: 16bit vs 32bit，省50%内存
- inBitmap: 复用已有Bitmap对象

**为什么有效：**
- 1080x1920 ARGB_8888: ~8MB
- 1080x1920 RGB_565: ~4MB
- 1/2采样: ~2MB

**风险与副作用：**
- 采样可能导致图片模糊
- 复用需要相同大小
- Android版本兼容（4.4+支持）

**验证方法：**
```bash
# 查看内存
adb shell dumpsys meminfo <package>
# 观察Bitmap内存占用
```

### 6. 网络优化策略

**怎么做：**
- `NetworkOptimizer.kt`: GZIP压缩、请求合并
- Accept-Encoding: gzip
- 批量请求：多个小请求合并为一个

**为什么有效：**
- GZIP压缩：100KB → 30KB（节省70%）
- 合并请求：减少3次RTT

**风险与副作用：**
- 压缩/解压消耗CPU
- 合并增加响应大小

**验证方法：**
```bash
# 查看网络请求大小
adb logcat | grep Network
```

### 7. RecyclerView优化

**怎么做：**
- `RecyclerViewOptimizer.kt`: Prefetch、DiffUtil
- isItemPrefetchEnabled: 提前加载
- DiffUtil: 增量更新

**为什么有效：**
- 无Prefetch: 滚动时临时加载，可能掉帧
- 有Prefetch: 提前加载，滚动更流畅

**风险与副作用：**
- Prefetch占用更多内存
- DiffUtil计算消耗CPU

**验证方法：**
```bash
# 观察FPS
adb shell dumpsys gfxinfo <package>
```

## 性能指标

Mall Performance Lab 关注以下关键指标：

| 指标 | 含义 | Baseline目标 | Optimized目标 |
|------|------|--------------|---------------|
| perf_mall_cold_start | 冷启动到首帧 | < 500ms | < 300ms |
| perf_mall_first_data | 首屏数据到达 | 500-1500ms | < 100ms (缓存) |
| perf_mall_first_content | 首屏渲染完成 | 100-200ms | 50-100ms |
| perf_mall_interactive | 首屏可交互 | 200-300ms | 100-150ms |

## 使用指南

### 切换模式

```kotlin
// 代码切换
FeatureToggle.enableBaseline()   // 基线模式
FeatureToggle.enableOptimized()  // 优化模式

// 界面切换
// 点击"切换模式"按钮
```

### 查看性能报告

```kotlin
// 点击"报告"按钮
// 输出所有打点统计

# 或查看Logcat
adb logcat -s MallPerfLab
```

### 本地验证

1. **清除缓存**：点击"清空"按钮
2. **切换到Baseline**：观察无缓存时的性能
3. **切换到Optimized**：观察缓存/预取效果
4. **对比报告**：点击"报告"查看差异

## 技术栈

- **语言**: Kotlin
- **UI**: RecyclerView + ViewBinding
- **并发**: Coroutines
- **架构**: Repository Pattern
- **测试**: 手动对比（暂无自动化）

## 目录结构

```
app/src/main/java/com/mall/perflab/
├── core/
│   ├── cache/           # 缓存层
│   │   ├── MemoryCache.kt
│   │   ├── DiskCache.kt
│   │   └── OptimizedCacheManager.kt
│   ├── config/
│   │   └── FeatureToggle.kt
│   ├── image/           # 图片优化
│   │   └── BitmapProcessor.kt
│   ├── memory/          # 内存监控
│   │   └── MemoryMonitor.kt
│   ├── network/         # 网络优化
│   │   └── NetworkOptimizer.kt
│   ├── perf/
│   │   ├── PerformanceTracker.kt
│   │   └── TraceLogger.kt
│   ├── prefetch/
│   │   ├── PreFetcher.kt
│   │   └── OptimizedPreFetcher.kt
│   ├── prewarm/
│   │   ├── ViewPreWarmer.kt
│   │   └── OptimizedViewPreWarmer.kt
│   └── ui/              # UI优化
│       └── RecyclerViewOptimizer.kt
├── data/
│   ├── model/
│   │   └── Models.kt
│   ├── mock/
│   │   └── DataGenerator.kt
│   └── repository/
│       └── MallRepository.kt
└── ui/
    ├── MainActivity.kt
    └── adapter/
        ├── MarketingFloorAdapter.kt
        └── FeedAdapter.kt
```

## 文档体系

| 文档 | 说明 |
|------|------|
| `README.md` | 项目架构、代码结构、快速开始 |
| `PERFORMANCE_PRINCIPLES.md` | 性能优化原理与思路（核心文档） |
| `SPEECH_2MIN.md` | 2分钟面试讲稿 |
| `KNOWLEDGE_MAP.md` | 性能优化知识地图 |

## 扩展思考

1. **更高级的缓存**: 可以使用 Room/CacheMaxSize
2. **图片优化**: 使用 Fresco/Glide + WebP
3. **网络优化**: HTTP2 + QUIC
4. **渲染优化**: ViewHolder + DiffUtil
5. **启动优化**: SplashActivity + 懒加载

## 后续计划

- [ ] 添加自动化性能测试
- [ ] 集成 BatteryCanary/FPSMonitor
- [ ] 支持Compose版本对比
- [ ] 添加网络请求追踪（Chucker）

---

**Author**: Claude Code
**Version**: 1.0.0
