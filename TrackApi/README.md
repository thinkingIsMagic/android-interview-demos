# Observability Mini-Kit

> 轻量级 Android 可观测性框架 - 面试级 Demo 项目

## 项目简介

这是一个**可复用的轻量监控与稳定性框架**，用于演示 Android 工程化能力。适用于 3~5 年经验 Android 开发工程师的面试展示。

## 核心功能

| 功能 | 描述 | 面试关键词 |
|------|------|-----------|
| 统一埋点 API | `logEvent` / `logError` / `trackPerformance` | DSL 风格 API |
| 性能耗时统计 | start/stop 自动计算 duration | 性能监控 |
| 采样机制 | 支持简单采样率 | 数据量控制 |
| 错误兜底 | 统一异常捕获 + Fallback | 稳定性设计 |
| 降级开关 | 模拟远端配置 | 容错思维 |
| 结构化日志 | JSON 格式输出 | 可扩展性 |

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      Observability                           │
│              (Facade 模式 - 统一入口)                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   TrackerRegistry                            │
│            (注册表模式 - 管理所有 Tracker)                    │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │  Sampling   │ │FeatureSwitch│ │FallbackHandler│         │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
└─────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   EventTracker  │ │   ErrorTracker  │ │PerformanceTracker│
│   (事件埋点)     │ │   (异常捕获)     │ │  (性能追踪)      │
└─────────────────┘ └─────────────────┘ └─────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   StructuredLogger                           │
│              (JSON 结构化日志输出)                           │
└─────────────────────────────────────────────────────────────┘
```

## 模块结构

```
trackApi1/
├── app/                              # Demo 展示层
│   ├── src/main/java/com/demo/app/
│   │   ├── DemoApplication.kt        # 初始化入口
│   │   └── MainActivity.kt           # 演示页面
│   └── build.gradle.kts
│
├── observability/                    # ⭐ 核心监控模块
│   ├── src/main/java/com/trackapi/observability/
│   │   ├── Observability.kt          # Facade 入口
│   │   ├── TrackerRegistry.kt        # 注册中心
│   │   ├── TrackerConfig.kt          # 运行时配置
│   │   ├── ITracker.kt              # Tracker 接口
│   │   ├── EventTracker.kt          # 事件埋点
│   │   ├── ErrorTracker.kt          # 异常捕获
│   │   ├── PerformanceTracker.kt    # 性能追踪
│   │   ├── SamplingManager.kt       # 采样决策
│   │   ├── FeatureSwitch.kt         # 降级开关
│   │   ├── FallbackHandler.kt       # 异常兜底
│   │   └── StructuredLogger.kt       # 结构化日志
│   ├── src/test/                    # 单元测试
│   └── build.gradle.kts
│
├── build.gradle.kts                  # 根构建设置
├── settings.gradle.kts
└── README.md
```

## 快速开始

### 1. 初始化

```kotlin
// Application.onCreate()
class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = TrackerConfig(
            samplingRate = 1.0f,           // 采样率 0.0~1.0
            featureSwitches = mapOf(
                "event_tracking" to true,
                "error_tracking" to true,
                "performance_tracking" to true
            )
        )

        Observability.init(config)
    }
}
```

### 2. 事件埋点

```kotlin
// 简单事件
Observability.logEvent("button_click", mapOf("button_id" to "submit"))

// 页面曝光
Observability.logEvent("page_view", mapOf("page_name" to "HomeActivity"))
```

### 3. 异常捕获

```kotlin
try {
    // 业务代码
    throw RuntimeException("测试异常")
} catch (e: Exception) {
    Observability.logError(
        throwable = e,
        tags = mapOf("scenario" to "payment")
    )
}
```

### 4. 性能追踪

```kotlin
// 闭包形式（推荐）
val result = Observability.trackPerformance("api_request") {
    // 业务代码
    delay(500)
    "请求结果"
}
// result.durationMs, result.success

// start/stop 形式
Observability.startTrace("task_name")
// ... 业务代码 ...
val duration = Observability.stopTrace("task_name")
```

## 设计亮点

### 1. Facade 模式
统一入口，降低使用复杂度

### 2. 策略模式
采样、降级、兜底策略可替换

### 3. 注册表模式
Tracker 可动态注册/注销，扩展性强

### 4. 线程安全
- `CopyOnWriteArrayList` - Tracker 列表
- `ConcurrentHashMap` - 性能追踪记录

### 5. 防御性编程
- FallbackHandler 确保监控代码不影响业务
- 采样控制数据量

## 运行 Demo

1. 打开 Android Studio
2. Sync Project with Gradle Files
3. Run `app` module

查看 **Logcat** 过滤 `Observability` 标签，查看完整结构化日志。

## 单元测试

```bash
./gradlew :observability:test
```

测试覆盖：
- `PerformanceTrackerTest` - 性能追踪核心逻辑
- `SamplingManagerTest` - 采样算法
- `TrackerRegistryTest` - 注册表功能
- `FallbackHandlerTest` - 兜底机制

## 面试讲稿（2分钟）

> "这是一个我设计的轻量级可观测性框架，主要解决项目中的监控能力复用问题。"
>
> **核心设计思路**：
> - 采用 Facade 模式提供统一 API，内部通过 TrackerRegistry 管理多个 Tracker
> - 支持事件埋点、异常捕获、性能追踪三种能力
> - 采样率控制数据量，降级开关保障稳定性
>
> **技术亮点**：
> - 独立模块设计，可打包为 AAR 供其他项目复用
> - 所有 Tracker 通过接口管理，新增类型无需修改现有代码
> - 监控代码本身具备容错能力，不会影响业务
>
> **实际价值**：
> - 统一了团队的埋点规范
> - 性能损耗 < 1ms/次
> - 支持动态配置采样率，线上问题可快速止血

## 进阶方向

如果要在生产环境使用，可以考虑：

1. **上报系统对接**
   - 接入 Logcat → Kafka → Flink 链路
   - 实现批量上报，降低服务端压力

2. **配置中心**
   - 接入 Apollo/美团 Mconfig 实现动态配置
   - 支持按用户分群配置

3. **ANR 监控**
   - 通过 FileObserver 监控 ANR
   - 结合 Crash 上报体系

4. **卡顿检测**
   - Looper 消息插桩
   - 堆栈聚合分析

5. **APM 集成**
   - 接入 Sigar/Perfetto
   - 端到端性能分析

## License

MIT
