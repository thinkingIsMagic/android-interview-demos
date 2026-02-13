# Kotlin 核心知识与原理

> 本文档以"我在项目里怎么用到"为主线，解释 Kotlin 核心特性。

## 1. 基础与语法

### 1.1 val vs var

**项目中哪里用到**：`Main.kt`、`Activity.kt`

```kotlin
// val = value，编译期常量，只能赋值一次
val name: String = "活动引擎"  // 类似 Java final

// var = variable，可变变量  
var count: Int = 0
count = 1  // 可以修改
```

**为什么区分**：
- `val` 让编译器帮助检查不可变，减少 bug
- 不可变对象是函数式编程的核心

**常见坑**：
```kotlin
val list = mutableListOf(1, 2, 3)
list.add(4)  // ✅ 编译通过：val 是引用不可变，不是内容不可变
```

### 1.2 空安全 (? ?: !!)

**项目中哪里用到**：`Demo01_Basics.kt` 空安全示例

```kotlin
// String? 可空类型
val nullable: String? = null

// ?. 安全调用，null 返回 null
val length: Int? = nullable?.length

// ?: Elvis 运算符，null 时默认值
val len = nullable?.length ?: 0

// !! 强制解包，null 抛 NPE
val len2 = nullable!!.length  // 不推荐！
```

**面试官问什么**：
- `?.` 和 `!!` 区别？
- 什么时候用 `!!`？答：开发者确定不为 null 时（如 lateinit var 初始化后）

### 1.3 智能类型转换 (Smart Cast)

**项目中哪里用到**：`Demo01_Basics.kt` 智能类型转换示例

```kotlin
fun printLength(obj: Any) {
    when (obj) {
        is String -> {
            // 编译器自动转换 obj 为 String
            println(obj.length)  // 无需 (obj as String)
        }
        is Int -> println(obj * 2)
    }
}
```

**原理**：Kotlin 编译器追踪 `is` 检查，自动插入类型转换代码。

### 1.4 when 表达式

**项目中哪里用到**：`Demo03_OOP.kt` 密封类处理

```kotlin
val result = when (x) {
    1 -> "one"
    2 -> "two"
    else -> "other"
}

// 多条件合并
when (day) {
    1, 2, 3 -> "工作日"
    6, 7 -> "周末"
}

// 区间匹配
when {
    score >= 90 -> "优秀"
    score >= 80 -> "良好"
}
```

**优势**：
- 返回值（表达式体）
- 无需 break
- 任意类型匹配

## 2. 函数与高阶

### 2.1 Lambda 表达式

**项目中哪里用到**：`Demo02_Functions.kt` Lambda 示例

```kotlin
// 完整写法
val square: (Int) -> Int = { x: Int -> x * x }

// 推断类型
val cube = { x: Int -> x * x * x }

// 单参数简写 it
val doubled = numbers.map { it * 2 }

// 高阶函数
fun calculate(a: Int, b: Int, op: (Int, Int) -> Int): Int {
    return op(a, b)
}
```

**闭包**：lambda 可以捕获外部变量
```kotlin
var counter = 0
val increment = { counter++ }
increment()  // counter = 1
```

### 2.2 inline 函数

**项目中哪里用到**：`Demo02_Functions.kt` inline 示例

```kotlin
// inline：将函数体嵌入调用处
inline fun inlineFun(message: String, block: (String) -> Unit) {
    block(message)
}

// 编译后等价于直接执行 block 内的代码
```

**为什么用**：消除 lambda 的对象分配开销

**reified 泛型**：
```kotlin
inline fun <reified T> getTypeName(): String {
    return T::class.simpleName!!  // inline 保留类型信息
}
```

### 2.3 扩展函数

**项目中哪里用到**：`Demo02_Functions.kt` 扩展函数示例

```kotlin
// 为 String 添加方法
fun String.capitalizeFirst(): String {
    return if (isNotEmpty()) this[0].uppercase() + substring(1) else this
}

"hello".capitalizeFirst()  // "Hello"

// 扩展可空类型
fun String?.orDefault(default: String = ""): String = this ?: default
```

**原理**：编译为静态方法，第一个参数是接收者对象

### 2.4 作用域函数（面试高频）

**项目中哪里用到**：`Demo02_Functions.kt` 作用域函数示例

| 函数 | 返回值 | 对象引用 | 典型用途 |
|-----|-------|---------|---------|
| `let` | lambda 结果 | `it` | 空安全链式调用 |
| `run` | lambda 结果 | `this` | 配置 + 返回结果 |
| `with` | lambda 结果 | `this` | 非扩展版 run |
| `apply` | 对象本身 | `this` | 对象配置 |
| `also` | 对象本身 | `it` | 附加操作（日志） |

**选择指南**：
```kotlin
// 配置对象 → apply
val config = Config().apply {
    host = "localhost"
    println("初始化: $this")  // this 可省略
}

// 空安全 + 返回结果 → let
val name = nullable?.let {
    process(it)
} ?: return

// 替代 with → run（更安全）
user.run {
    println(name)  // this.name
    computeScore()
}
```

## 3. 面向对象与设计

### 3.1 数据类 (data class)

**项目中哪里用到**：`Activity.kt`、`User.kt`

```kotlin
data class Activity(
    val id: String,
    val name: String,
    val type: ActivityType
)
```

**自动生成**：
- `equals/hashCode` - 用于 Set/Map
- `toString` - 便于调试
- `copy` - 创建副本
- `componentN` - 解构声明

**限制**：
- 必须有主构造函数
- 不能继承（final）

### 3.2 密封类 (sealed class)

**项目中哪里用到**：`Activity.kt` 活动类型

```kotlin
sealed class ActivityType {
    data class MoneyOff(val threshold: Double, val discount: Double) : ActivityType()
    data class Discount(val rate: Double) : ActivityType()
    object FreeShipping : ActivityType()
}
```

**vs 枚举**：
- 密封类：子类可携带不同数据
- 枚举：所有常量类型相同

**when 编译器检查**：
```kotlin
fun calculate(activity: ActivityType, amount: Double): Double {
    return when (activity) {
        is ActivityType.MoneyOff -> { ... }
        is ActivityType.Discount -> { ... }
        // 编译器确保覆盖所有情况
    }
}
```

### 3.3 委托 (by)

**项目中哪里用到**：`ActivityRepositoryImpl.kt`

```kotlin
// 接口实现委托
class CachedActivityRepository(
    private val delegate: ActivityRepository
) : ActivityRepository by delegate {
    
    override fun getById(id: String): Activity? {
        // 缓存逻辑
        return cache.getOrPut(id) { delegate.getById(id) }
    }
}
```

**好处**：组合优于继承，少写样板代码

### 3.4 泛型协变逆变

**项目中哪里用到**：`Demo03_OOP.kt` 泛型示例

**协变 (out)**：Producer
```kotlin
class Producer<out T> {
    fun produce(): T? = null
    // 不能 fun consume(item: T)  // ❌
}
val stringProducer: Producer<String> = Producer()
val anyProducer: Producer<Any> = stringProducer  // ✅ 协变
```

**逆变 (in)**：Consumer
```kotlin
class Consumer<in T> {
    fun consume(item: T) {}
}
val anyConsumer: Consumer<Any> = Consumer()
val stringConsumer: Consumer<String> = anyConsumer  // ✅ 逆变
```

**规则**：
- `out T`：返回值类型，只读
- `in T`：函数参数类型，只写

## 4. 集合与序列

### 4.1 List/Map 操作

**项目中哪里用到**：`Demo04_Collections.kt`

```kotlin
// map 转换
val doubled = numbers.map { it * 2 }

// filter 筛选
val evens = numbers.filter { it % 2 == 0 }

// flatMap 扁平化
val chars = words.flatMap { it.toList() }

// groupBy 分组
val byStatus = orders.groupBy { it.status }

// associate 转 Map
val idMap = users.associateBy { it.id }
```

### 4.2 Sequence 惰性求值

**项目中哪里用到**：`Demo04_Collections.kt` Sequence 对比

```kotlin
// List：立即求值，每个操作创建新 List
val listResult = numbers
    .map { ... }  // 创建新 List
    .filter { ... }  // 再创建新 List
    .take(5)

// Sequence：只有终端操作才执行
val seqResult = numbers
    .asSequence()
    .map { ... }    // 不执行
    .filter { ... } // 不执行
    .take(5)
    .toList()       // 终端操作：触发计算
```

**性能对比**：
```
数据量 1000:
- List: 每个操作 O(n)，总 O(n²)
- Sequence: 只处理需要的元素，O(n)
```

## 5. 协程核心

### 5.1 suspend 函数

**项目中哪里用到**：`Demo05_Coroutines.kt`

```kotlin
// suspend 修饰的函数可以暂停执行
suspend fun fetchActivity(id: String): String {
    delay(100)  // 挂起，不阻塞线程
    return "Activity_$id"
}

// 只能在协程或其他 suspend 中调用
runBlocking {
    val result = fetchActivity("A001")  // 等待恢复
}
```

**原理**：
- 编译器将 suspend 函数转换为状态机
- 每次挂起是状态切换
- 底层通过 Continuation 实现

### 5.2 CoroutineScope 与 Dispatcher

```kotlin
// Dispatcher 决定协程在哪个线程执行
withContext(Dispatchers.IO) {
    // IO 操作
}

withContext(Dispatchers.Default) {
    // CPU 密集型计算
}

// launch 启动协程（不阻塞）
launch(Dispatchers.Default) {
    // 协程代码
}
```

**线程数**：
- Default: CPU 核心数
- IO: max(64, CPU数)

### 5.3 结构化并发

```kotlin
runBlocking {
    // 父协程
    val job1 = launch { ... }  // 子协程1
    val job2 = launch { ... }  // 子协程2
    
    job1.join()  // 等待
    job2.join()
    
    // 子协程异常会传播给父协程
    // 父协程自动取消所有子协程
}
```

### 5.4 取消与超时

```kotlin
val job = launch {
    repeat(10) { i ->
        delay(100)
        if (!isActive) return@launch  // 检查取消
    }
}

job.cancel()      // 取消
job.join()        // 等待完成

// 超时
withTimeout(1000) {
    // 超过 1 秒抛 TimeoutCancellationException
}

withTimeoutOrNull(1000) {
    // 超时返回 null
}
```

### 5.5 Flow

**Cold Flow**：
```kotlin
// 惰性，只有收集时才执行
val coldFlow = flow {
    emit(1)
    emit(2)
}

coldFlow.collect { value -> }  // 开始执行
coldFlow.collect { value -> }  // 重新执行
```

**Hot Flow (SharedFlow)**：
```kotlin
// 发射后立即发送给所有订阅者
val sharedFlow = MutableSharedFlow<Int>(replay = 2)

sharedFlow.emit(1)  // 立即发送
sharedFlow.emit(2)
```

**StateFlow**：
```kotlin
// 始终有当前值，类似 LiveData
val stateFlow = MutableStateFlow(0)
stateFlow.value = 1  // 更新
```

## 6. 工程实践

### 6.1 分层架构

```
app/           # 表现层，UI/入口
core/
  domain/      # 领域层，核心业务
    model/     # 实体
    repository/# 接口
    usecase/   # 用例
  data/        # 数据层
  common/      # 公共组件
```

### 6.2 Result 封装

```kotlin
sealed class AppResult<out T> {
    data class Success(val data: T) : AppResult<T>()
    data class Error(val message: String) : AppResult<Nothing>()
}

val result = AppResult.runCatching { riskyOperation() }
result.onSuccess { ... }
result.onError { ... }
```

### 6.3 简单 DI 容器

```kotlin
// 注册
ServiceLocator.register { MyRepository() }

// 获取
val repo: MyRepository = ServiceLocator.get()
```
