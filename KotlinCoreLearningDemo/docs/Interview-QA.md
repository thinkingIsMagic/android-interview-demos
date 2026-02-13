# Kotlin 面试高频问题

> 每题包含：1句话答案 + 关键点（最多3点）+ 本项目代码位置

## 1. 基础与语法

### Q1: val 和 var 有什么区别？

**1句话答案**: val 是只读引用（只能赋值一次），var 是可变引用。

**关键点**:
- val 类似 Java final，但不是不可变对象
- var 可以重新赋值
- val 编译期检查，IDE 友好

**项目代码**: `Demo01_Basics.kt:16-24`

---

### Q2: Kotlin 的空安全机制是什么？?. ?: !! 区别？

**1句话答案**: Kotlin 通过类型系统区分可空和非空类型，?. 安全调用返回 null，?: 提供默认值，!! 强制解包。

**关键点**:
- String? 表示可为 null
- ?. 链式安全调用，null 时返回 null
- ?: Elvis 运算符，null 时备选值
- !! 抛 NPE，只在确定不为 null 时用

**项目代码**: `Demo01_Basics.kt:36-53`

---

### Q3: 什么是智能类型转换（Smart Cast）？

**1句话答案**: Kotlin 编译器追踪 is 类型检查，自动插入强制转换代码，无需手动写 (obj as Type)。

**关键点**:
- 发生在 when/is 分支内
- val 变量自动转换，var 需要 val 引用
- 避免 Java 中繁琐的 instanceof + cast

**项目代码**: `Demo01_Basics.kt:55-66`

---

### Q4: when 表达式有什么优点？

**1句话答案**: when 是增强版 switch，支持任意类型、返回值、无需 break、编译器检查完整性。

**关键点**:
- 返回值（表达式体）
- 多条件合并 (1, 2, 3 -> ...)
- 区间匹配 (when { score >= 90 -> ... })
- 密封类时编译器检查所有分支

**项目代码**: `Demo01_Basics.kt:68-85`

---

### Q5: 数据类（data class）自动生成什么方法？

**1句话答案**: equals/hashCode/toString/copy/componentN（解构）。

**关键点**:
- 适合需要值相等性比较的场景
- copy 创建修改后的副本
- 解构声明 val (a, b) = pair
- 主构造函数至少一个参数

**项目代码**: `Demo01_Basics.kt:87-110`

## 2. 函数与高阶

### Q6: lambda 表达式和匿名内部类区别？

**1句话答案**: lambda 语法简洁、无需 SAM 转换、编译器自动优化为单方法接口。

**关键点**:
- { param -> body }
- 单参数可用 it
- 闭包捕获外部变量
- 无需 interface/override

**项目代码**: `Demo02_Functions.kt:18-35`

---

### Q7: inline 函数作用和原理？

**1句话答案**: inline 将函数体复制到调用处，消除 lambda 包装对象开销，但会增加代码体积。

**关键点**:
- 编译期嵌入，避免对象分配
- 适合高阶函数（性能敏感）
- 不适合大函数（代码膨胀）
- reified 保留泛型信息

**项目代码**: `Demo02_Functions.kt:38-60`

---

### Q8: 扩展函数原理是什么？

**1句话答案**: 编译为静态方法，第一个参数是接收者对象，本质不是修改原类。

**关键点**:
- fun String.ext(): ... 编译为 static ext(String this)
- 可扩展可空类型
- 常见于 SDK 扩展、DSL
- 不可访问 private/protected 成员

**项目代码**: `Demo02_Functions.kt:75-92`

---

### Q9: 作用域函数 let/apply/run/also/with 区别？

**1句话答案**: let/run 返回 lambda 结果，apply/also 返回对象本身，区别在于对象引用方式（it vs this）。

**关键点**:
- let/also: it 访问对象
- run/apply/with: this 访问对象
- 返回结果: let/run/with
- 返回对象: apply/also
- 场景：配置用 apply，空安全用 let

**项目代码**: `Demo02_Functions.kt:94-130`

## 3. 面向对象

### Q10: sealed class 和 enum class 区别？

**1句话答案**: 密封类子类可携带不同数据，枚举常量类型相同；密封类适合状态机，枚举适合固定集合。

**关键点**:
- 密封类子类可任意（data class/object/class）
- 枚举常量共享同一类型
- when 处理密封类需覆盖所有分支
- 密封类编译期已知子类

**项目代码**: `Demo03_OOP.kt:24-60`

---

### Q11: Kotlin 的委托（by）是什么？

**1句话答案**: by 关键字让一个类自动实现接口方法，无需手写代理代码，实现组合优于继承。

**关键点**:
- interface Foo { fun bar() }
- class A : Foo by Delegate()
- 可组合多个行为（日志、缓存）
- 避免继承带来的强耦合

**项目代码**: `Demo03_OOP.kt:68-90`

---

### Q12: 泛型协变（out）和逆变（in）区别？

**1句话答案**: out T 只读返回（Producer），in T 只写参数（Consumer），解决泛型子类型化问题。

**关键点**:
- out: List<String> -> List<Any>
- in: Consumer<Any> -> Consumer<String>
- @UnsafeVariance 打破规则
- 类似 Java ? extends / ? super

**项目代码**: `Demo03_OOP.kt:92-130`

---

### Q13: object 和 companion object 区别？

**1句话答案**: object 是单例类，companion object 是伴生对象（类似静态成员）。

**关键点**:
- object: 惰性单例，线程安全
- companion object: 绑定到类，类似 static
- companion 可实现接口
- 工厂方法放 companion

**项目代码**: `Demo03_OOP.kt:132-160`

## 4. 集合与序列

### Q14: List 和 Sequence 区别？什么时候用 Sequence？

**1句话答案**: List 立即求值（每个操作创建新集合），Sequence 惰性求值（终端操作才执行），大数据量用 Sequence。

**关键点**:
- Sequence 只有 toList/toSet 等终端操作才执行
- 中间操作返回新 Sequence
- 大数据量减少中间对象创建
- 链式操作只遍历一次

**项目代码**: `Demo04_Collections.kt:60-85`

---

### Q15: Kotlin 集合常用操作有哪些？

**1句话答案**: map/filter/flatMap/groupBy/associate/reduce/sum 等。

**关键点**:
- map: 转换元素
- filter: 筛选
- flatMap: 扁平化
- groupBy/associate: 转 Map
- sumOf: 求和

**项目代码**: `Demo04_Collections.kt:20-55`

## 5. 协程核心

### Q16: suspend 函数是什么？原理？

**1句话答案**: suspend 函数可以暂停执行（不阻塞线程），底层通过状态机实现。

**关键点**:
- suspend 修饰的函数可暂停/恢复
- 只能从协程或其他 suspend 调用
- 编译为 Continuation 状态机
- 每次挂起是状态切换

**项目代码**: `Demo05_Coroutines.kt:20-40`

---

### Q17: CoroutineScope 和 Dispatcher 区别？

**1句话答案**: CoroutineScope 管理协程生命周期，Dispatcher 决定协程在哪个线程执行。

**关键点**:
- Dispatchers.Default: CPU 密集
- Dispatchers.IO: IO 密集
- Dispatchers.Main: 主线程
- withContext 切换调度器

**项目代码**: `Demo05_Coroutines.kt:42-65`

---

### Q18: 结构化并发是什么？

**1句话答案**: 协程在 Scope 中启动，父协程等待所有子协程完成，异常自动传播。

**关键点**:
- 父协程取消，所有子取消
- 子协程异常，父也失败
- 自动管理生命周期
- 避免协程泄露

**项目代码**: `Demo05_Coroutines.kt:67-90`

---

### Q19: Flow 和 SharedFlow/StateFlow 区别？

**1句话答案**: Flow 冷流（收集时才执行），SharedFlow 热流（发射即广播），StateFlow 热流（有当前值）。

**关键点**:
- Cold Flow: 每次收集重新执行
- SharedFlow: replay 重放，emit 发射
- StateFlow: value 始终有值
- StateFlow 类似 LiveData

**项目代码**: `Demo05_Coroutines.kt:130-175`

---

### Q20: 协程怎么取消？

**1句话答案**: job.cancel() 取消，isActive 检查是否活跃，超时用 withTimeout。

**关键点**:
- cancel 发送取消信号
- 子协程需检查 isActive
- finally 释放资源
- supervisorScope 隔离异常

**项目代码**: `Demo05_Coroutines.kt:92-115`

## 6. 工程实践

### Q21: Kotlin 分层架构怎么组织？

**1句话答案**: domain（领域层）→ data（数据层）→ app（表现层），依赖倒置。

**关键点**:
- domain: model/repository/usecase
- data: repositoryImpl/dataSource
- app: UI/main 入口
- repository 接口在 domain

**项目代码**: `core/` 全模块

---

### Q22: 怎么在 Kotlin 中处理错误？

**1句话答案**: 用 Result/AppResult 封装，强制调用者处理，而非 try-catch 忽略。

**关键点**:
- sealed class Success/Error
- onSuccess/onError 回调
- getOrNull/getOrDefault
- runCatching 捕获异常

**项目代码**: `core/common/result/AppResult.kt`

---

### Q23: Kotlin 如何实现依赖注入？

**1句话答案**: 可手写 ServiceLocator，或用 Hilt/Koin/Dagger 框架。

**关键点**:
- 注册 factory
- 获取时调用 factory
- 可实现单例模式
- 框架自动管理生命周期

**项目代码**: `core/common/di/ServiceLocator.kt`

---

### Q24: Kotlin 有什么常见坑？

**1句话答案**: val 不等于不可变、?. 和 !! 误用、协程取消不检查、泛型擦除。

**关键点**:
- val list = mutableListOf() 内容可变
- !! 抛 NPE 要谨慎
- 协程循环要检查 isActive
- 反射才能获取泛型类型

**代码位置**: 各 Demo 注释中有说明
