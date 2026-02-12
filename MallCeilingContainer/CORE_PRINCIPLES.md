# 核心原理文档

## 1. 互斥裁决机制

### 1.1 问题背景

在电商场景中，底部区域可能同时存在多种组件：
- Coupon 优惠券
- Banner 活动图
- Floating Widget 悬浮按钮
- 消息提示
- 客服入口

**核心诉求**：同一时刻只展示一个组件，避免 UI 冲突。

### 1.2 解决方案：优先级队列

```kotlin
class ExclusivePolicy {
    fun selectComponent(components: List<Component>, state: ViewState): ExclusiveResult {
        // Step 1: 过滤满足显隐条件的组件
        val visible = components.filter { meetsVisibility(it, state) }

        // Step 2: 按 priority 降序排序
        val sorted = visible.sortedByDescending { it.priority }

        // Step 3: 选择最高优先级的组件
        return ExclusiveResult(
            selectedComponent = sorted.firstOrNull(),
            rejectedComponents = sorted.drop(1),
            reason = "优先级裁决: ${sorted.firstOrNull()?.id}"
        )
    }
}
```

### 1.3 优先级规则

| 优先级 | 组件类型 | 典型场景 |
|--------|----------|----------|
| 100 | Coupon | 商品详情页 |
| 80 | Banner | 活动推广 |
| 60 | Floating | 客服入口 |

**追问解答要点**：
- 优先级相同时：按配置顺序或组件 ID 二次排序
- 动态调整：服务端可随时修改 priority 实现 ABTest

---

## 2. 显隐规则引擎

### 2.1 规则维度

```kotlin
data class VisibilityConfig(
    scrollThreshold: Int = 0,      // 滚动阈值（像素）
    scrollDirection: String = "down", // 滚动方向
    minDuration: Long = 0,         // 最小停留时长
    pageStates: List<String> = []   // 页面状态
)
```

### 2.2 规则执行流程

```
组件是否可见?
    ├── visible=false → false
    ├── enable=false → false
    ├── scrollOffset < scrollThreshold → false
    ├── scrollDirection 不匹配 → false
    ├── pageState 不在白名单 → false
    └── minDuration 未满足 → false
    └── 以上全部通过 → true
```

### 2.3 面试要点

**Q: scrollThreshold = 0 意味着什么？**

A: 表示"始终显示"，不需要滚动触发。

**Q: 如何处理滚动方向判断？**

A: 记录上次滚动位置，与当前对比得出方向。简化版可只判断 isAtTop。

---

## 3. 容错兜底体系

### 3.1 兜底场景

| 场景 | 处理方式 |
|------|----------|
| JSON 解析失败 | 返回兜底配置 |
| 字段缺失 | 使用默认值 |
| 组件类型未知 | 过滤该组件 |
| 渲染异常 | 降级为默认 Coupon |
| 全局开关关闭 | 显示兜底组件 |

### 3.2 兜底配置

```kotlin
fun getFallbackConfig(): BottomConfig {
    return BottomConfig(
        version = "1.0.0-fallback",
        enable = true,
        components = listOf(
            Component(
                id = "fallback_coupon",
                type = ComponentType.COUPON,
                priority = 1,
                config = CouponConfig(title = "默认优惠券")
            )
        )
    )
}
```

### 3.3 追问解答

**Q: 如何确保兜底不影响正常流程？**

A:
- 兜底配置优先级最低
- 只有解析失败时才使用
- 记录兜底触发日志，便于排查

---

## 4. 配置下发与变更

### 4.1 配置生命周期

```
[网络/本地] → [BottomConfigParser] → [BottomConfig] → [ViewModel] → [UI]
     ↓              ↓                    ↓              ↓           ↓
  JSON 字符串    解析为领域模型        缓存/版本校验    状态管理     渲染
```

### 4.2 配置刷新

```kotlin
fun refreshConfig() {
    repository.clearCache()  // 清除内存缓存
    repository.getConfig()   // 重新加载
}
```

### 4.3 版本控制

```kotlin
data class BottomConfig(
    val version: String = "1.0",
    val enable: Boolean = true,
    val components: List<Component>
) {
    fun isCompatible(requiredVersion: String): Boolean {
        // 简单的版本比较
        return version >= requiredVersion
    }
}
```

---

## 5. 埋点体系

### 5.1 统一埋点入口

```kotlin
class Tracker {
    fun expose(component: Component) {
        logEvent(
            event = component.tracker.exposureEvent,
            params = mapOf(
                "component_id" to component.id,
                "component_type" to component.type.value,
                "priority" to component.priority
            )
        )
    }

    fun click(component: Component) {
        // 同上
    }
}
```

### 5.2 埋点时机

| 事件 | 触发时机 |
|------|----------|
| 曝光 | 组件首次进入可视区域 + 停留 > 100ms |
| 点击 | 用户点击组件 |
| 裁决 | 互斥裁决结果变更 |

### 5.3 追问解答

**Q: 如何避免重复曝光？**

A:
- 记录已曝光组件 ID Set
- 组件可见性从 false → true 时触发
- 页面销毁时清空 Set

---

## 6. 性能优化

### 6.1 性能指标

```kotlin
data class PerformanceStats(
    createViewCount: Int = 0,    // 组件创建次数
    composeCount: Int = 0,       // Compose 重组次数
    lastRenderTimeMs: Long = 0,  // 最近渲染耗时
    totalRenderTimeMs: Long = 0  // 累计渲染耗时
)
```

### 6.2 优化策略

| 策略 | 做法 |
|------|------|
| 避免重组 | `remember { component }` |
| 按需渲染 | `AnimatedVisibility` |
| 懒加载 | `LazyColumn` 滚动触发 |
| 缓存 | 配置本地缓存 |

### 6.3 面试要点

**Q: Compose 重组如何影响性能？**

A:
- 组件状态变更触发重组
- 使用 `remember` 缓存不可变数据
- 拆分组件粒度，减少重组范围

---

## 7. 面试追问清单（20题）

### 7.1 互斥与优先级

1. **多组件优先级相同时如何处理？**
   - 答：按配置顺序或 ID 字典序二次排序

2. **如何支持动态调整优先级？**
   - 答：服务端下发新配置，客户端刷新

3. **互斥裁决的性能开销？**
   - 答：O(n log n) 排序，组件数少可忽略

### 7.2 显隐规则

4. **滚动阈值如何动态获取？**
   - 答：`NestedScrollConnection` 监听滚动

5. **scrollThreshold = 0 有什么含义？**
   - 答：始终可见，无需滚动触发

6. **如何处理快速滚动场景？**
   - 答：使用 `scrollThreshold` + `minDuration` 双重校验

### 7.3 容错与兜底

7. **解析失败时如何确保有 UI 展示？**
   - 答：`FallbackManager.getFallbackConfig()`

8. **字段缺失时如何处理？**
   - 答：Parser 层使用 `optString(key, default)`

9. **如何区分"无配置"和"配置错误"？**
   - 答：前者返回空列表，后者返回兜底配置

### 7.4 性能与架构

10. **Compose 重组时如何避免组件重复创建？**
    - 答：`remember { }` 缓存组件实例

11. **首帧渲染如何优化？**
    - 答：配置预加载 + 懒加载组件

12. **ViewModel 重建时如何恢复状态？**
    - 答：`SavedStateHandle` 或磁盘缓存

13. **内存泄漏风险点？**
    - 答：`viewModelScope` 自动取消，`remember` 避免闭包捕获

### 7.5 埋点与监控

14. **曝光埋点的触发条件？**
    - 答：组件可见 + 停留 > 100ms

15. **如何避免重复曝光？**
    - 答：`exposedSet.contains(id)`

16. **埋点与业务解耦？**
    - 答：统一 `Tracker` 接口，支持 mock

### 7.6 扩展与维护

17. **新增组件类型需要改哪些代码？**
    - 答：`ComponentType` 枚举 + 对应 UI 组件

18. **服务端 JSON 字段变更如何兼容？**
    - 答：Parser 层 `optString` + 兜底默认值

19. **多页面共享配置？**
    - 答：`Repository` 单例 + 内存缓存

20. **与传统方案（BaseActivity）的优劣对比？**
   - 答：
   | 维度 | Config-Driven | 传统方案 |
   |------|---------------|----------|
   | 灵活性 | 高 - 配置下发 | 低 - 代码修改 |
   | 维护成本 | 低 - 统一管理 | 高 - 分散各处 |
   | 性能 | 好 - 按需加载 | 差 - 全量初始化 |
   | 可测试 | 高 - 可单元测试 | 低 - 依赖 Activity |

---

## 8. 2分钟演示讲稿

> 开场：30秒

"大家好，今天分享一个我做的 Config-Driven Bottom Host 项目。这是一个面试级的 Android 组件化实践，解决的是电商页面底部多组件的治理问题。"

> 核心功能：60秒

"项目核心是三部分：
第一，互斥裁决 - 通过 JSON 配置定义组件优先级，运行时自动选择最优组件展示。
第二，显隐规则 - 支持滚动阈值、页面状态等动态控制。
第三，容错兜底 - 任何解析失败或字段缺失，都会有默认配置兜底。

这是一个典型的 MVVM + Compose 项目，数据流向很清晰：服务端 JSON 经 Parser 解析为领域模型，ViewModel 管理状态，BottomHost 负责渲染。"

> 亮点：20秒

"项目亮点：完整的容错体系、统一埋点、性能监控，还有 15 个单测覆盖解析层。"

> 结尾：10秒

"以上就是项目核心设计，欢迎提问交流。谢谢！"

---

## 9. 面试加分项

### 9.1 代码质量

- [x] 单元测试覆盖率 > 80%
- [x] 错误处理完善（Result 封装）
- [x] 日志体系完整
- [x] 命名规范，注释清晰

### 9.2 架构设计

- [x] 分层清晰（data/domain/ui）
- [x] 依赖倒置（接口抽象）
- [x] 单一职责
- [x] 开闭原则

### 9.3 工程化

- [x] Gradle Kotlin DSL
- [x] 版本号管理
- [x] CI/CD 可接入
- [x] 文档完善

---

## 10. 扩展方向

1. **网络配置下发**：接入真实服务端
2. **组件热更新**：动态加载新组件
3. **A/B Test**：配置灰度发布
4. **多主题**：暗色模式适配
5. **辅助功能**：无障碍支持
