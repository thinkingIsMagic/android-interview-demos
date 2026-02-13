package demo

/**
 * Kotlin 函数与高阶函数演示
 * 
 * 覆盖考点：
 * 1. lambda 表达式
 * 2. inline 函数与 reified 泛型
 * 3. 扩展函数
 * 4. 作用域函数（let/apply/run/also/with）
 */
object Demo02_Functions {
    
    fun run() {
        println(">>> Demo02: 函数与高阶函数")
        println()
        
        // lambda 表达式
        demoLambda()
        
        // inline 函数
        demoInline()
        
        // 扩展函数
        demoExtension()
        
        // 作用域函数
        demoScopeFunctions()
        
        println()
    }
    
    /**
     * Lambda 表达式
     * 
     * 语法：{ 参数 -> 函数体 }
     * 省略写法：it（单参数时的默认名称）
     * 
     * 面试点：
     * - lambda 与匿名内部类的区别（语法更简洁、无需 SAM 转换）
     * - closure 闭包：lambda 可以访问外部变量
     */
    private fun demoLambda() {
        println("--- Lambda 表达式示例 ---")
        
        // 完整写法
        val square: (Int) -> Int = { x: Int -> x * x }
        println("  square(5) = ${square(5)}")
        
        // 推断类型
        val cube = { x: Int -> x * x * x }
        println("  cube(3) = ${cube(3)}")
        
        // 使用 it（单参数时的简写）
        val double = { it: Int -> it * 2 }
        val numbers = listOf(1, 2, 3, 4, 5)
        
        // 高阶函数：接受 lambda 参数
        val evens = numbers.filter { it % 2 == 0 }
        println("  偶数: $evens")
        
        val doubled = numbers.map { it * 2 }
        println("  翻倍: $doubled")
        
        // 闭包：lambda 捕获外部变量
        var counter = 0
        val increment = { counter++ }
        increment()
        increment()
        println("  闭包 counter = $counter")
        println()
    }
    
    /**
     * inline 函数原理
     * 
     * 什么是 inline？
     * - 编译器将函数体直接嵌入调用处
     * - 消除 lambda 的包装对象开销（避免额外对象分配）
     * 
     * 什么时候用？
     * - 高阶函数（性能敏感场景）
     * - 不适合：递归函数、大函数（代码膨胀）
     * 
     * reified 泛型：
     * - 普通泛型在运行时会被擦除（T -> Any）
     * - inline + reified 保留泛型信息
     */
    private fun demoInline() {
        println("--- Inline 函数示例 ---")
        
        // 内联函数：消除 lambda 开销
        inlineFun("hello") { name ->
            println("  内联中: $name")
        }
        
        // reified 泛型演示
        println("  Int 类型: ${getTypeName<Int>()}")
        println("  String 类型: ${getTypeName<String>()}")
        
        // 对比：非内联函数无法访问泛型类型
        // nonInlineFun("test") // ❌ 编译错误：Unresolved reference
        println()
    }
    
    // inline 函数：将 lambda 内联到调用处
    // 编译后相当于直接执行 lambda 内的代码，无额外对象分配
    private inline fun inlineFun(message: String, block: (String) -> Unit) {
        println("  [inline start]")
        block(message)
        println("  [inline end]")
    }
    
    /**
     * reified 泛型：保留运行时类型信息
     * 
     * 为什么普通泛型不行？
     * - Java/Kotlin 泛型有类型擦除（type erasure）
     * - JVM 运行时 T 被擦除为 Object 或具体边界
     * 
     * inline + reified 原理：
     * - 编译器内联函数体时，将 T 替换为具体类型
     * - 生成的代码是具体的类型检查
     */
    private inline fun <reified T> getTypeName(): String {
        // 这里可以直接使用 T，因为编译器内联时替换为具体类型
        return T::class.simpleName ?: "Unknown"
    }
    
    /**
     * 扩展函数
     * 
     * 作用：为已有类添加方法，无需继承
     * 原理：编译时生成静态方法，第一个参数是接收者对象
     * 常见用途：SDK 扩展、DSL 构建
     */
    private fun demoExtension() {
        println("--- 扩展函数示例 ---")
        
        // 字符串扩展：首字母大写
        fun String.capitalizeFirst(): String {
            return if (isNotEmpty()) this[0].uppercase() + substring(1) else this
        }
        
        println("  \"hello\".capitalizeFirst() = ${"hello".capitalizeFirst()}")
        
        // 扩展可空类型
        fun String?.orDefault(default: String = ""): String {
            return this ?: default
        }
        
        val nullable: String? = null
        println("  null.orDefault() = ${nullable.orDefault()}")
        
        // 电商场景：给 Double 添加货币格式化
        fun Double.toCurrency(): String {
            return String.format("¥%.2f", this)
        }
        
        println("  99.9.toCurrency() = ${99.9.toCurrency()}")
        println()
    }
    
    /**
     * 作用域函数（面试高频）
     * 
     * let: 返回 lambda 结果，it 访问对象
     * run: 返回 lambda 结果，this 访问对象（类似 let，但用 this）
     * with: 非扩展函数，this 访问对象，返回 lambda 结果
     * apply: 返回对象本身，this 访问对象（用于配置对象）
     * also: 返回对象本身，it 访问对象（用于附加操作）
     * 
     * 选择指南：
     * - 配置对象：apply
     * - 返回结果：let/run
     * - 附加操作（日志、打印）：also
     * - 替代 with：run（更安全， receiver 可能为 null）
     */
    private fun demoScopeFunctions() {
        println("--- 作用域函数示例 ---")
        
        data class User(var name: String, var age: Int)
        
        // apply：返回对象本身，用于配置
        val user = User("张三", 25).apply {
            // this 可省略
            name = "李四"
            age = 30
        }
        println("  apply 结果: $user")
        
        // let：返回结果，处理可空
        val nullable: String? = "hello"
        val length = nullable?.let {
            // it 是可空解包后的类型
            println("  let 中: $it")
            it.length
        } ?: 0
        println("  let 结果: $length")
        
        // run：返回结果，配置+计算
        val result = user.run {
            // this = user
            println("  run 中: $this")
            name.length * 2 // 返回计算结果
        }
        println("  run 结果: $result")
        
        // also：返回对象本身，附加操作
        val logUser = user.also {
            // it = user
            println("  also 记录: $it")
            // 返回 user 本身，可以链式调用
        }
        println("  also 结果: ${logUser === user}") // true，同一个对象
        
        // with：非扩展函数
        with(user) {
            println("  with 中: $name, $age")
            "$name - $age"
        }.also { println("  with 返回: $it") }
        
        println()
        
        // 电商场景：订单配置
        data class Order(var id: String, var items: List<String> = emptyList())
        
        val order = Order("ORDER_001")
            .apply {
                items = listOf("商品A", "商品B")
            }
            .also {
                // 打印日志
                println("  创建订单: ${it.id}, 商品数: ${it.items.size}")
            }
            .let {
                // 返回处理结果
                "订单 ${it.id} 已创建"
            }
        println("  链式调用结果: $order")
    }
}
