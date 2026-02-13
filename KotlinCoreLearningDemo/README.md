# KotlinCoreLearningDemo

## 项目概述

**主题**：电商活动引擎（Activity Engine）

**目标**：通过真实工程代码系统学习 Kotlin，覆盖面试核心考点

**特点**：
- 纯 Kotlin 项目（JVM Console），无需 Android 环境
- 真实工程分层架构（domain/data/app）
- 每个特性都有详细注释，回答"是什么、为什么、怎么用"
- 包含单元测试和文档

## 快速开始

### 环境要求
- JDK 17+
- Gradle 8.x

### 运行项目

```bash
# 克隆项目
git clone <repo-url>
cd KotlinCoreLearningDemo

# 运行所有示例
./gradlew run

# 运行测试
./gradlew test

# 查看测试报告
open app/build/reports/tests/test/index.html
```

## 项目结构

```
KotlinCoreLearningDemo/
├── app/                              # 应用入口模块
│   ├── src/main/kotlin/
│   │   ├── Main.kt                   # 主入口，一键运行所有示例
│   │   └── demo/                     # 按主题组织的示例
│   │       ├── Demo01_Basics.kt      # 基础语法
│   │       ├── Demo02_Functions.kt   # 函数与高阶
│   │       ├── Demo03_OOP.kt         # 面向对象
│   │       ├── Demo04_Collections.kt # 集合与序列
│   │       └── Demo05_Coroutines.kt  # 协程
│   └── src/test/kotlin/              # 单元测试
│       └── CoreTest.kt               # 12+ 测试用例
│
├── core/                             # 核心领域模块
│   └── src/main/kotlin/
│       ├── domain/                   # 领域层
│       │   ├── model/               # 领域实体
│       │   │   ├── Activity.kt     # 活动模型
│       │   │   └── User.kt          # 用户模型
│       │   ├── repository/          # 仓储接口
│       │   └── usecase/             # 业务用例
│       ├── data/                    # 数据层
│       │   ├── repository/         # 仓储实现
│       │   └── source/              # 数据源
│       └── common/                  # 公共组件
│           ├── result/              # Result 封装
│           └── di/                  # DI 容器
│
├── docs/                             # 文档
│   ├── Kotlin-Core.md               # Kotlin 核心原理
│   └── Interview-QA.md              # 面试问答
│
├── build.gradle.kts                  # 根构建配置
└── README.md                         # 本文件
```

## 学习路线（建议顺序）

### 第一阶段：基础语法
1. 运行 `Demo01_Basics.kt` → 理解 val/var、空安全、when、数据类
2. 阅读 `docs/Kotlin-Core.md` 基础部分
3. 完成 `CoreTest.kt` 前 6 个测试

### 第二阶段：函数与集合
1. 运行 `Demo02_Functions.kt` → lambda、inline、扩展函数、作用域函数
2. 运行 `Demo04_Collections.kt` → List/Map/Sequence
3. 对比 Sequence vs List 性能

### 第三阶段：面向对象
1. 运行 `Demo03_OOP.kt` → 密封类、委托、泛型协变逆变
2. 理解分层架构（domain/data/common）

### 第四阶段：协程核心
1. 运行 `Demo05_Coroutines.kt` → suspend、Scope、Flow
2. 理解结构化并发与取消机制

### 第五阶段：工程实践
1. 阅读 `core/common/result/AppResult.kt` → 错误处理封装
2. 阅读 `core/common/di/ServiceLocator.kt` → 简单 DI
3. 阅读 `core/domain/usecase/` → 用例层设计

## 亮点展示（面试可说）

### 1. 密封类处理多状态
```kotlin
sealed class ActivityType {
    data class MoneyOff(...) : ActivityType()
    data class Discount(...) : ActivityType()
    object FreeShipping : ActivityType()
}
// when 覆盖所有情况，编译器检查完整性
```

### 2. 协程结构化并发
```kotlin
// 父协程等待所有子协程完成
val job1 = launch { ... }
val job2 = launch { ... }
job1.join()
job2.join()
```

### 3. Result 封装强制错误处理
```kotlin
val result = AppResult.runCatching { riskyOperation() }
result.onSuccess { data -> handle(data) }
result.onError { error -> log(error) }
```

### 4. 集合操作链式调用
```kotlin
val orders = listOf(...)
    .filter { it.status == PAID }
    .groupBy { it.userId }
    .mapValues { (_, orders) -> orders.sumOf { it.amount } }
```

## 如何扩展

### 添加新的活动类型
1. 在 `core/domain/model/Activity.kt` 添加密封类子类
2. 在 `CalculatePromotionUseCase.kt` 添加 when 分支
3. 编译器确保覆盖所有情况

### 添加新的数据源
1. 在 `core/domain/repository/` 定义接口
2. 在 `core/data/` 实现接口
3. 使用 `ServiceLocator.register()` 注册

### 添加新功能
1. 在 `app/src/main/kotlin/demo/` 添加 Demo
2. 在 `Main.kt` 调用新 Demo
3. 添加对应的单元测试

## 常见问题

### Q: 为什么不用 Android？
A: 本项目聚焦 Kotlin 语言本身，避免 Android SDK 复杂度。2 天内可跑通。

### Q: 单元测试不够多怎么办？
A: 建议自己添加更多测试，覆盖边界条件和异常场景。

### Q: 如何在实际项目中使用？
A: 参考 core 模块的分层架构，直接复用 domain/data/common 的设计模式。

## 参考资源

- [Kotlin 官方文档](https://kotlinlang.org/docs/home.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- 《Kotlin 实战》- 经典教材

---

**项目维护**：本项目用于学习目的，欢迎提出改进建议。
