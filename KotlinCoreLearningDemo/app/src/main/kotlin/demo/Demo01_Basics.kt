package demo

/**
 * Kotlin 基础语法演示
 * 
 * 覆盖考点：
 * 1. val/var 区别与常量/变量
 * 2. 空安全（? ?: !!）
 * 3. 智能类型转换（smart cast）
 * 4. when 表达式（增强版 switch）
 * 5. 数据类（data class）
 */
object Demo01_Basics {
    
    /**
     * val vs var（面试高频）
     * 
     * val = value，编译期常量（类似 Java final），只能赋值一次
     * var = variable，可变变量
     * 
     * 常见坑：val 并不意味着不可变，只是引用不可变
     * val list = mutableListOf() // 引用不可变，但列表内容可修改
     */
    fun run() {
        println(">>> Demo01: 基础语法")
        println()
        
        // val 声明常量
        val name: String = "活动引擎"
        // name = "新名字" // ❌ 编译错误：Val cannot be reassigned
        
        // var 声明变量
        var count: Int = 0
        count = 1 // ✅ 可以修改
        
        // 类型推断（省略类型声明）
        val inferred = "Kotlin" // 编译器自动推断 String 类型
        
        println("  val name = $name")
        println("  var count = $count")
        println("  类型推断: $inferred")
        println()
        
        // 空安全演示
        demoNullSafety()
        
        // 智能类型转换
        demoSmartCast()
        
        // when 表达式
        demoWhen()
        
        // 数据类
        demoDataClass()
        
        println()
    }
    
    /**
     * 空安全（Kotlin 核心特性）
     * 
     * ? 可空类型：String? 表示可为 null
     * ?: Elvis 运算符：null 时默认值
     * !! 强制解包：不为 null 时使用，null 时抛异常
     * 
     * 面试官可能问：?. 和 !! 区别？什么情况用 !!？
     * - ?. 安全调用，null 时返回 null
     * - !! 断言非空，null 时抛 NPE（用于开发者确定不为 null 的场景）
     */
    private fun demoNullSafety() {
        println("--- 空安全示例 ---")
        
        val nullable: String? = null
        val nonNull: String = "有值"
        
        // 安全调用 ?. （推荐，避免 NPE）
        val length1: Int? = nullable?.length
        println("  nullable?.length = $length1") // null
        
        // Elvis ?: 运算符（null 时提供默认值）
        val length2: Int = nullable?.length ?: 0
        println("  nullable?.length ?: 0 = $length2") // 0
        
        // 强制解包 !! （需确保不为 null，否则 NPE）
        // val length3: Int = nullable!!.length // ❌ 运行时异常
        
        // 场景：电商场景中，用户已登录则一定 有 userId
        val userId: String? = getUserId()
        val safeUserId = userId ?: "GUEST" // 空时用默认值
        println("  安全获取 userId: $safeUserId")
        println()
    }
    
    /**
     * 智能类型转换（Smart Cast）
     * 
     * Kotlin 编译器会追踪类型检查，自动进行转换
     * 常见坑：在 Java 中需要手动 instanceof + 强制转换
     */
    private fun demoSmartCast() {
        println("--- 智能类型转换示例 ---")
        
        fun printLength(obj: Any) {
            // obj 是 Any，但我们可以用 when 进行类型检查
            when (obj) {
                is String -> {
                    // 编译器自动将 obj 转换为 String
                    // 不需要手动: (obj as String)
                    println("  String 长度: ${obj.length}")
                }
                is Int -> {
                    // 自动转换 Int
                    println("  Int 值: $obj")
                }
                else -> {
                    println("  未知类型")
                }
            }
        }
        
        printLength("Kotlin")
        printLength(42)
        println()
    }
    
    /**
     * when 表达式（增强版 switch）
     * 
     * 优点：
     * - 返回值（表达式体）
     * - 无需 break
     * - 任意类型匹配
     * - 区间匹配、类型匹配、条件匹配
     */
    private fun demoWhen() {
        println("--- when 表达式示例 ---")
        
        // 基本用法
        val x = 2
        val result = when (x) {
            1 -> "one"
            2 -> "two"
            else -> "other" // 必须（除非覆盖所有情况）
        }
        println("  when 结果: $result")
        
        // 多条件合并
        val day = 3
        val dayName = when (day) {
            1, 2, 3, 4, 5 -> "工作日"
            6, 7 -> "周末"
            else -> "无效日期"
        }
        println("  星期 $day = $dayName")
        
        // 区间匹配
        val score = 85
        val grade = when {
            score >= 90 -> "优秀"
            score >= 80 -> "良好"
            score >= 60 -> "及格"
            else -> "不及格"
        }
        println("  分数 $score = $grade")
        println()
    }
    
    /**
     * 数据类（Data Class）
     * 
     * 自动生成：equals/hashCode/toString/copy/componentN
     * 
     * 面试点：
     * - 什么时候用 data class？需要相等性比较、打印、拷贝时
     * - 解构声明：val (a, b) = pair
     * - 限制：必须有至少一个参数的主构造函数
     */
    private fun demoDataClass() {
        println("--- 数据类示例 ---")
        
        // 定义数据类（只需一行）
        data class ActivityRule(
            val id: String,
            val threshold: Double,      // 满减门槛
            val discount: Double,       // 折扣金额
            val maxUsage: Int? = null   // 可选，最大使用次数
        )
        
        // 自动生成 toString（便于调试）
        val rule = ActivityRule("MONEY_OFF_100", 100.0, 10.0)
        println("  toString: $rule")
        
        // 自动生成 equals/hashCode（用于 Set/Map）
        val rule2 = ActivityRule("MONEY_OFF_100", 100.0, 10.0)
        println("  equals: ${rule == rule2}") // true
        println("  hashCode: ${rule.hashCode()}")
        
        // 自动生成 copy（创建修改后的副本，原始对象不变）
        val rule3 = rule.copy(discount = 20.0)
        println("  copy 副本: $rule3")
        
        // 解构声明（自动生成 component1, component2...）
        val (id, threshold, discount, _) = rule
        println("  解构: id=$id, threshold=$threshold, discount=$discount")
        println()
    }
    
    // 模拟获取用户 ID（可能为 null）
    private fun getUserId(): String? = if (Math.random() > 0.5) "USER_123" else null
}
