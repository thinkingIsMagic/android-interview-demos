# Config-Driven Bottom Host

> 统一底部容器 + 配置化渲染 - 面试级 Android 项目

## 项目概述

本项目实现了一个 **Config-Driven Bottom Host**（配置驱动底部宿主），用于统一管理底部业务组件。通过 JSON 配置驱动，实现组件的动态下发、互斥裁决、显隐规则、埋点和性能监控。

### 核心特性

- **统一容器**：单一 BottomHost 承载所有底部组件
- **配置驱动**：服务端 JSON 动态下发组件配置
- **互斥裁决**：按 priority 自动选择最优组件
- **显隐规则**：支持滚动阈值、页面状态等控制
- **容错兜底**：解析失败/字段缺失自动降级
- **统一埋点**：曝光/点击事件标准化
- **性能监控**：createView/compose 计数与耗时统计

---

## 架构设计

### 模块结构

```
app/
├── data/                      # 数据层
│   ├── model/                 # 数据模型
│   │   ├── BottomConfig.kt    # 配置根模型
│   │   ├── Component.kt      # 组件模型
│   │   ├── ComponentType.kt   # 组件类型枚举
│   │   └── FeatureConfig.kt  # 开关配置
│   ├── parser/                # 解析器
│   │   └── BottomConfigParser.kt
│   ├── mock/                  # Mock 数据
│   │   └── MockConfigDataSource.kt
│   └── BottomConfigRepository.kt
├── domain/                    # 业务层
│   ├── model/
│   │   └── ViewPolicy.kt     # 视图策略
│   └── policy/
│       ├── ExclusivePolicy.kt # 互斥裁决
│       ├── VisibilityPolicy.kt # 显隐规则
│       ├── FallbackManager.kt # 兜底策略
│       └── FeatureToggle.kt   # 远端开关
├── ui/                        # UI 层
│   ├── bottom/
│   │   ├── BottomHost.kt     # 统一容器
│   │   ├── BottomComponentRenderer.kt
│   │   ├── BottomViewModel.kt
│   │   └── components/       # 组件 UI
│   │       ├── CouponComponent.kt
│   │       ├── BannerComponent.kt
│   │       └── FloatingWidgetComponent.kt
│   ├── tracker/
│   │   └── Tracker.kt        # 埋点
│   └── theme/
├── performance/               # 性能层
│   └── PerformanceMonitor.kt
├── demo/                      # 演示页面
│   ├── DemoActivity.kt
│   └── DemoViewModel.kt
└── util/                       # 工具层
    ├── Result.kt
    └── Logger.kt
```

### 数据流

```
服务端 JSON
    ↓
BottomConfigParser (JSON → Domain Model)
    ↓
BottomConfigRepository (数据仓库)
    ↓
BottomViewModel (状态管理)
    ↓
ExclusivePolicy (互斥裁决)
    ↓
BottomHost (Compose 容器)
    ↓
BottomComponentRenderer (组件渲染)
```

---

## 核心设计

### 1. 互斥裁决

```kotlin
// ExclusivePolicy.kt
fun selectComponent(components, viewState): ExclusiveResult {
    // 1. 过滤满足可见性的组件
    val visible = filterByVisibility(components, viewState)
    // 2. 按 priority 降序排序
    val sorted = visible.sortedByDescending { it.priority }
    // 3. 选中的组件 = 最高优先级
    return ExclusiveResult(selected = sorted.firstOrNull())
}
```

### 2. 组件配置

```json
{
  "id": "coupon_001",
  "type": "coupon",
  "priority": 100,
  "scene": "product_detail",
  "visible": true,
  "enable": true,
  "config": {
    "title": "限时优惠券",
    "amount": 50,
    "threshold": 200
  },
  "tracker": {
    "exposureEvent": "coupon_exposure",
    "clickEvent": "coupon_click"
  },
  "visibility": {
    "scrollThreshold": 200,
    "scrollDirection": "down"
  }
}
```

### 3. 兜底机制

```kotlin
// FallbackManager.kt
fun getFallbackConfig(): BottomConfig {
    return BottomConfig(
        version = "1.0.0-fallback",
        enable = true,
        components = listOf(getDefaultCouponComponent())
    )
}
```

---

## 快速开始

### 环境要求

- Android Studio Hedgehog | 2023.1.1+
- JDK 17
- Gradle 8.4+
- Kotlin 2.0.21

### 构建

```bash
./gradlew assembleDebug
```

### 运行单测

```bash
./gradlew testDebugUnitTest
```

### 演示说明

1. 打开 App 进入 DemoActivity
2. 顶部展示当前配置状态和选中的底部组件
3. 使用 Slider 模拟滚动，触发组件显隐
4. 勾选/取消组件，测试互斥裁决
5. 查看性能统计和裁决结果

---

## 功能清单

### 已实现

| 功能 | 状态 | 说明 |
|------|------|------|
| JSON 配置解析 | ✅ | Gson 多态解析 |
| 组件类型 | ✅ | Coupon / Banner / Floating |
| 互斥裁决 | ✅ | 按 priority 排序 |
| 显隐规则 | ✅ | 滚动阈值/方向 |
| 容错兜底 | ✅ | 解析失败自动降级 |
| 统一埋点 | ✅ | 曝光/点击事件 |
| 性能监控 | ✅ | createView/compose 计数 |
| 远端开关 | ✅ | FeatureToggle |
| 单元测试 | ✅ | 13 个测试用例 |
| 演示页面 | ✅ | 完整交互 Demo |

---

## 面试亮点

1. **架构分层**：data/domain/ui 三层，职责清晰
2. **容错体系**：Parser 层容错 + FallbackManager 兜底
3. **性能优化**：remember 缓存 + 懒加载
4. **可测试性**：Parser 单测 + Repository 可 mock
5. **工程化**：Kotlin DSL + 版本号管理 + 完整文档

---

## 追问清单

详见 [CORE_PRINCIPLES.md](CORE_PRINCIPLES.md)

---

## License

MIT
