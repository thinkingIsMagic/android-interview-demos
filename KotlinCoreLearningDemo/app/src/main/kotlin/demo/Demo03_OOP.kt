package demo

/**
 * Kotlin 面向对象与设计模式演示
 * 
 * 覆盖考点：
 * 1. interface 与 abstract class
 * 2. 委托（by）实现组合优于继承
 * 3. object / companion object
 * 4. 可见性修饰符
 * 5. 泛型（out/in）协变逆变
 */
object Demo03_OOP {
    
    fun run() {
        println(">>> Demo03: 面向对象与设计")
        println()
        
        // 密封类 vs 枚举
        demoSealedClass()
        
        // 接口与委托
        demoInterfaceAndDelegation()
        
        // 泛型协变逆变
        demoGenerics()
        
        // object 与 companion
        demoObject()
        
        println()
    }
    
    /**
     * 密封类 vs 枚举（面试高频）
     * 
     * 密封类（Sealed Class）：
     * - 子类型有限且已知（编译期确定）
     * - 子类可以是 data class、object、普通 class
     * - 适合：状态机、多分支返回类型
     * 
     * 枚举（Enum）：
     * - 每个枚举常量是单例
     * - 所有枚举类型相同（实现相同接口）
     * - 适合：有限且固定的常量集合
     */
    private fun demoSealedClass() {
        println("--- 密封类与枚举示例 ---")
        
        // ===== 密封类：电商活动类型（支持不同数据结构）=====
        // 使用 sealed class 定义活动类型，子类可以携带不同数据
        sealed class ActivityType {
            // 满减活动：携带门槛和折扣
            data class MoneyOff(val threshold: Double, val discount: Double) : ActivityType()
            
            // 折扣活动：携带折扣比例
            data class Discount(val rate: Double, val maxDiscount: Double? = null) : ActivityType()
            
            // 秒杀活动：携带限量信息
            data class Seckill(val stock: Int, val startTime: Long) : ActivityType()
            
            // 免邮活动：无额外数据
            object FreeShipping : ActivityType()
        }
        
        // when 表达式可以覆盖所有分支，编译器会检查完整性
        fun calculate(activity: ActivityType, amount: Double): Double {
            return when (activity) {
                is ActivityType.MoneyOff -> {
                    // 智能转换：activity 自动是 MoneyOff 类型
                    if (amount >= activity.threshold) {
                        amount - activity.discount
                    } else {
                        amount
                    }
                }
                is ActivityType.Discount -> {
                    val discounted = amount * activity.rate
                    activity.maxDiscount?.let { minOf(discounted, it) } ?: discounted
                }
                is ActivityType.Seckill -> {
                    // 秒杀库存检查
                    if (activity.stock > 0) {
                        amount * 0.9 // 9折
                    } else {
                        amount // 没库存，原价
                    }
                }
                is ActivityType.FreeShipping -> amount
            }
        }
        
        // 测试各种活动
        val moneyOff = ActivityType.MoneyOff(100.0, 10.0)
        val discount = ActivityType.Discount(0.8, 50.0)
        val seckill = ActivityType.Seckill(10, System.currentTimeMillis())
        
        println("  满减(150) = ${calculate(moneyOff, 150.0)}")  // 140.0
        println("  8折(200) = ${calculate(discount, 200.0)}")  // 160.0
        println("  秒杀 = ${calculate(seckill, 100.0)}")        // 90.0
        println("  免邮 = ${calculate(ActivityType.FreeShipping, 50.0)}") // 50.0
        
        // ===== 枚举：订单状态（有限且固定）=====
        enum class OrderStatus {
            PENDING,    // 待支付
            PAID,      // 已支付
            SHIPPED,   // 已发货
            COMPLETED, // 已完成
            CANCELLED  // 已取消
        }
        
        fun getNextStatus(status: OrderStatus): OrderStatus? {
            return when (status) {
                OrderStatus.PENDING -> OrderStatus.PAID
                OrderStatus.PAID -> OrderStatus.SHIPPED
                OrderStatus.SHIPPED -> OrderStatus.COMPLETED
                else -> null // 已完成或取消，无下一步
            }
        }
        
        println("  PENDING -> ${getNextStatus(OrderStatus.PENDING)}")
        println()
    }
    
    /**
     * 委托（Delegation）实现组合优于继承
     * 
     * Kotlin 原生支持 by 委托
     * 
     * 好处：
     * - 避免继承带来的强耦合
     * - 灵活组合多个行为
     * - 比手动写代理代码更简洁
     * 
     * 常见场景：日志记录、缓存、事务
     */
    private fun demoInterfaceAndDelegation() {
        println("--- 接口与委托示例 ---")
        
        // ===== 仓储接口（依赖倒置）=====
        // 定义接口，不依赖具体实现
        interface ActivityRepository {
            fun getById(id: String): Activity?
            fun getAll(): List<Activity>
        }
        
        // ===== 实现类 =====
        class InMemoryActivityRepository : ActivityRepository {
            private val storage = mutableMapOf<String, Activity>()
            
            override fun getById(id: String): Activity? = storage[id]
            
            override fun getAll(): List<Activity> = storage.values.toList()
            
            fun add(activity: Activity) { storage[activity.id] = activity }
        }
        
        // ===== 带缓存的仓储实现（委托模式）=====
        // 构造函数参数是接口，具体实现可替换
        class CachedActivityRepository(
            private val delegate: ActivityRepository
        ) : ActivityRepository by delegate {
            
            private val cache = mutableMapOf<String, Activity?>()
            
            override fun getById(id: String): Activity? {
                return cache.getOrPut(id) { delegate.getById(id) }
            }
            
            // getAll 不缓存，保持原有行为
        }
        
        // ===== 带日志的仓储实现（装饰器模式）=====
        class LoggingActivityRepository(
            private val delegate: ActivityRepository
        ) : ActivityRepository by delegate {
            
            override fun getById(id: String): Activity? {
                println("  [日志] 查询活动: $id")
                return delegate.getById(id)
            }
        }
        
        // 测试
        val baseRepo = InMemoryActivityRepository().apply {
            add(Activity("A001", "满100减10", ActivityType.MoneyOff(100.0, 10.0)))
            add(Activity("A002", "8折优惠", ActivityType.Discount(0.8)))
        }
        
        val loggingRepo = LoggingActivityRepository(baseRepo)
        val cachedRepo = CachedActivityRepository(loggingRepo)
        
        println("  首次查询: ${cachedRepo.getById("A001")?.name}")
        println("  二次查询（缓存）: ${cachedRepo.getById("A001")?.name}")
        println()
    }
    
    /**
     * 泛型协变（out）与逆变（in）
     * 
     * 协变（out T）：Producer，只读返回
     * - List<String> 可以赋值给 List<Any>
     * 
     * 逆变（in T）：Consumer，只写参数
     * - Comparator<String> 可以赋值给 Comparator<Any>
     * 
     * 为什么需要？
     * - Java 用 ? extends / ? super
     * - Kotlin 用 in/out 更语义化
     * 
     * 规则：
     * - out 位置：返回值（不能是函数参数）
     * - in 位置：函数参数（不能是返回值）
     */
    private fun demoGenerics() {
        println("--- 泛型协变逆变示例 ---")
        
        // ===== 协变（out）=====
        // Producer<out T>：只生产 T，不消费 T
        // MoneyOffProducer 可以返回 ActivityType.MoneyOff
        class ActivityProducer<out T : ActivityType> {
            // 只能返回 T，不能接受 T 作为参数
            fun produce(): T? = null
            // fun consume(item: T) {} // ❌ 编译错误
        }
        
        // String 是 Any 的子类型，所以 Producer<String> 也是 Producer<Any> 的子类型
        val stringProducer: ActivityProducer<ActivityType.MoneyOff> = ActivityProducer()
        val anyProducer: ActivityProducer<ActivityType> = stringProducer // 协变：子类型可以赋值给父类型
        println("  协变测试通过")
        
        // ===== 逆变（in）=====
        // Consumer<in T>：只消费 T，不生产 T
        interface ActivityConsumer<in T : ActivityType> {
            // 可以接受 T 作为参数
            fun consume(item: T)
            // fun produce(): T // ❌ 编译错误
        }
        
        // Consumer<Any> 可以接受 String，因为 String 是 Any 的子类型
        // 逆变：父类型可以赋值给子类型
        val anyConsumer: ActivityConsumer<ActivityType> = object : ActivityConsumer<Any> {
            override fun consume(item: Any) {
                println("  消费: $item")
            }
        }
        
        // MoneyOff 是 ActivityType 的子类型
        val moneyOffConsumer: ActivityConsumer<ActivityType.MoneyOff> = anyConsumer
        moneyOffConsumer.consume(ActivityType.MoneyOff(100.0, 10.0))
        
        // ===== @UnsafeVariance 注解（实际工程常见）=====
        // 用于打破规则的情况，需要开发者确保类型安全
        class SafeList<T> {
            // contains 接受 T 但返回 T（协变位置），需要 @UnsafeVariance
            fun contains(element: @UnsafeVariance T): Boolean = true
        }
        println("  @UnsafeVariance 演示通过")
        println()
    }
    
    /**
     * object 与 companion object
     * 
     * object：
     * - 单例类（惰性初始化，线程安全）
     * - 匿名内部类
     * - 伴生对象（ companion object）
     * 
     * companion object：
     * - 静态成员（Kotlin 没有 static）
     * - 工厂方法
     * - 模拟静态常量
     */
    private fun demoObject() {
        println("--- object 与 companion 示例 ---")
        
        // ===== object 单例 =====
        object ActivityManager {
            // 惰性初始化，线程安全
            private val activities = mutableMapOf<String, Activity>()
            
            fun register(activity: Activity) {
                activities[activity.id] = activity
            }
            
            fun get(id: String): Activity? = activities[id]
        }
        
        ActivityManager.register(Activity("A001", "测试", ActivityType.FreeShipping))
        println("  单例获取: ${ActivityManager.get("A001")?.name}")
        
        // ===== companion object（静态成员）=====
        class User private constructor(
            val id: String,
            val name: String
        ) {
            // companion object 类似 Java 静态方法
            companion object {
                // 工厂方法（比构造函数更语义化）
                fun create(name: String): User {
                    return User("USER_${System.currentTimeMillis()}", name)
                }
                
                // 模拟静态常量
                const val MAX_NAME_LENGTH = 50
                
                // 扩展函数绑定到 companion
                fun User.greet() = "Hello, ${this.name}"
            }
            
            fun greet() = "Hello, $name"
        }
        
        val user = User.create("张三")
        println("  创建用户: ${user.id}, ${user.name}")
        println("  静态常量: ${User.MAX_NAME_LENGTH}")
        println()
    }
    
    // ===== 领域模型定义 =====
    
    data class Activity(
        val id: String,
        val name: String,
        val type: ActivityType
    )
    
    // 密封类定义（重复声明，实际项目中应放在同一文件）
    sealed class ActivityType {
        data class MoneyOff(val threshold: Double, val discount: Double) : ActivityType()
        data class Discount(val rate: Double, val maxDiscount: Double? = null) : ActivityType()
        data class Seckill(val stock: Int, val startTime: Long) : ActivityType()
        object FreeShipping : ActivityType()
    }
}
