import org.junit.Test
import org.junit.Assert.*
import demo.*

/**
 * Kotlin 核心特性单元测试
 * 
 * 覆盖范围：
 * - 基础语法（val/var、空安全、数据类）
 * - 函数与高阶（lambda、inline、扩展函数）
 * - 面向对象（密封类、委托、泛型）
 * - 集合操作
 * 
 * 面试提示：
 * - 能在面试中解释这些测试的原理
 * - 能说出每种写法的适用场景
 */
class CoreTest {
    
    // ===== 基础语法测试 =====
    
    @Test
    fun `val 不可重新赋值`() {
        val name: String = "Kotlin"
        // name = "Java" // 编译错误
        assertEquals("Kotlin", name)
    }
    
    @Test
    fun `var 可重新赋值`() {
        var count = 0
        count = 1
        assertEquals(1, count)
    }
    
    @Test
    fun `空安全 - 安全调用`() {
        val nullable: String? = null
        val length = nullable?.length
        assertNull(length)
        
        val nonNull: String? = "hello"
        assertEquals(5, nonNull?.length)
    }
    
    @Test
    fun `空安全 - Elvis 运算符`() {
        val nullable: String? = null
        val result = nullable?.length ?: 0
        assertEquals(0, result)
    }
    
    @Test
    fun `数据类自动生成方法`() {
        data class Activity(val id: String, val name: String)
        
        val a1 = Activity("A001", "满减")
        val a2 = Activity("A001", "满减")
        
        // equals
        assertEquals(a1, a2)
        
        // hashCode
        assertEquals(a1.hashCode(), a2.hashCode())
        
        // toString
        assertTrue(a1.toString().contains("Activity"))
        
        // copy
        val a3 = a1.copy(name = "折扣")
        assertEquals("折扣", a3.name)
        assertEquals(a1.id, a3.id)
        
        // componentN / 解构
        val (id, name) = a1
        assertEquals("A001", id)
        assertEquals("满减", name)
    }
    
    // ===== 密封类测试 =====
    
    @Test
    fun `密封类 when 必须覆盖所有情况`() {
        sealed class Result {
            data class Success(val data: String) : Result()
            data class Error(val message: String) : Result()
            object Loading : Result()
        }
        
        fun handle(result: Result): String {
            return when (result) {
                is Result.Success -> result.data
                is Result.Error -> result.message
                is Result.Loading -> "Loading..."
            }
        }
        
        assertEquals("data", handle(Result.Success("data")))
        assertEquals("error", handle(Result.Error("error")))
        assertEquals("Loading...", handle(Result.Loading))
    }
    
    // ===== 集合测试 =====
    
    @Test
    fun `List map 转换`() {
        val numbers = listOf(1, 2, 3, 4, 5)
        val doubled = numbers.map { it * 2 }
        assertEquals(listOf(2, 4, 6, 8, 10), doubled)
    }
    
    @Test
    fun `List filter 筛选`() {
        val numbers = listOf(1, 2, 3, 4, 5)
        val evens = numbers.filter { it % 2 == 0 }
        assertEquals(listOf(2, 4), evens)
    }
    
    @Test
    fun `Map getOrElse`() {
        val map = mapOf("a" to 1, "b" to 2)
        assertEquals(1, map.getOrElse("a") { 0 })
        assertEquals(0, map.getOrElse("c") { 0 })
    }
    
    @Test
    fun `List to Set 去重`() {
        val numbers = listOf(1, 2, 2, 3, 3, 3)
        assertEquals(3, numbers.toSet().size)
    }
    
    @Test
    fun `associateBy 转 Map`() {
        data class User(val id: Int, val name: String)
        val users = listOf(User(1, "张三"), User(2, "李四"))
        val map = users.associateBy { it.id }
        
        assertEquals("张三", map[1]?.name)
        assertEquals("李四", map[2]?.name)
    }
    
    // ===== 扩展函数测试 =====
    
    @Test
    fun `扩展 String 首字母大写`() {
        fun String.capitalizeFirst(): String {
            return if (isNotEmpty()) this[0].uppercase() + substring(1) else this
        }
        
        assertEquals("Hello", "hello".capitalizeFirst())
        assertEquals("World", "World".capitalizeFirst())
    }
    
    @Test
    fun `扩展 Double 货币格式化`() {
        fun Double.toCurrency(): String {
            return String.format("¥%.2f", this)
        }
        
        assertEquals("¥99.90", 99.9.toCurrency())
    }
    
    // ===== 作用域函数测试 =====
    
    @Test
    fun `apply 返回对象本身`() {
        data class Config(var host: String = "", var port: Int = 0)
        
        val config = Config().apply {
            host = "localhost"
            port = 8080
        }
        
        assertEquals("localhost", config.host)
        assertEquals(8080, config.port)
    }
    
    @Test
    fun `let 返回 lambda 结果`() {
        val nullable: String? = "hello"
        val length = nullable?.let { it.length * 2 }
        assertEquals(10, length)
        
        val nullResult = nullable?.let { null }
        assertNull(nullResult)
    }
    
    // ===== lambda 与高阶函数测试 =====
    
    @Test
    fun `lambda 闭包捕获外部变量`() {
        var counter = 0
        val increment = { counter++ }
        
        increment()
        increment()
        
        assertEquals(2, counter)
    }
    
    @Test
    fun `高阶函数参数`() {
        fun calculate(a: Int, b: Int, operation: (Int, Int) -> Int): Int {
            return operation(a, b)
        }
        
        val add = { x: Int, y: Int -> x + y }
        val multiply = { x: Int, y: Int -> x * y }
        
        assertEquals(5, calculate(2, 3, add))
        assertEquals(6, calculate(2, 3, multiply))
    }
    
    // ===== 泛型测试 =====
    
    @Test
    fun `泛型函数`() {
        fun <T> firstOrNull(list: List<T>): T? {
            return list.firstOrNull()
        }
        
        assertEquals(1, firstOrNull(listOf(1, 2, 3)))
        assertNull(firstOrNull(emptyList<Int>()))
    }
    
    @Test
    fun `泛型约束`() {
        fun <T : Number> sum(list: List<T>): Double {
            return list.sumOf { it.toDouble() }
        }
        
        assertEquals(6.0, sum(listOf(1, 2, 3)), 0.01)
        assertEquals(6.5, sum(listOf(1.5, 2.0, 3.0)), 0.01)
    }
    
    @Test
    fun `协变 out`() {
        open class Activity
        class MoneyOff : Activity()
        
        // Producer<out T>：子类型可以赋值给父类型
        val moneyOffProducer: () -> MoneyOff = { MoneyOff() }
        val activityProducer: () -> Activity = moneyOffProducer
        
        assertTrue(activityProducer() is Activity)
    }
    
    // ===== AppResult 测试 =====
    
    @Test
    fun `AppResult Success`() {
        val result = com.learn.kotlin.core.common.result.AppResult.success("data")
        
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals("data", result.getOrNull())
        assertEquals("data", result.getOrDefault("default"))
    }
    
    @Test
    fun `AppResult Error`() {
        val result = com.learn.kotlin.core.common.result.AppResult.error("error message")
        
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
        assertEquals("default", result.getOrDefault("default"))
    }
    
    @Test
    fun `AppResult map`() {
        val result = com.learn.kotlin.core.common.result.AppResult.success(5)
        val mapped = result.map { it * 2 }
        
        assertEquals(10, mapped.getOrNull())
    }
    
    @Test
    fun `AppResult onSuccess onError`() {
        var successCalled = false
        var errorCalled = false
        
        com.learn.kotlin.core.common.result.AppResult.success("data")
            .onSuccess { successCalled = true }
            .onError { errorCalled = true }
        
        assertTrue(successCalled)
        assertFalse(errorCalled)
    }
}
