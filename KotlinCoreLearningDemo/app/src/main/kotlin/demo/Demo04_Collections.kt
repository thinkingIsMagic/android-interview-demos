package demo

/**
 * Kotlin 集合与序列演示
 * 
 * 覆盖考点：
 * 1. List/Set/Map 及其可变/不可变版本
 * 2. Sequence（惰性求值）vs List（立即求值）
 * 3. 性能对比
 * 4. 常用操作（map/filter/reduce/groupBy等）
 */
object Demo04_Collections {
    
    fun run() {
        println(">>> Demo04: 集合与序列")
        println()
        
        // 可变与不可变集合
        demoMutability()
        
        // List 操作
        demoListOperations()
        
        // Map 操作
        demoMapOperations()
        
        // Sequence 性能
        demoSequence()
        
        // 电商场景：订单处理
        demoOrderProcessing()
        
        println()
    }
    
    /**
     * 可变与不可变集合
     * 
     * Kotlin 区分不可变（List）和可变（MutableList）
     * 
     * 不可变 List：编译期安全，线程安全
     * 可变 MutableList：允许修改、增删
     * 
     * 注意：不可变不等于不可修改引用
     * val list = listOf(1,2,3) // list 引用不可变，但元素如果是可变对象仍可修改
     */
    private fun demoMutability() {
        println("--- 可变与不可变集合 ---")
        
        // 不可变集合（创建后不能修改大小）
        val immutableList: List<String> = listOf("A", "B", "C")
        // immutableList.add("D") // ❌ 编译错误
        
        // 可变集合
        val mutableList: MutableList<String> = mutableListOf("A", "B", "C")
        mutableList.add("D")
        println("  MutableList: $mutableList")
        
        // 不可变引用 + 可变对象（陷阱）
        val list: List<Int> = mutableListOf(1, 2, 3) // 引用不可变，但底层是可变
        // list[0] = 100 // ❌ 编译错误（智能检查）
        // 但通过 mutableListOf 创建的仍然可以通过原引用修改
        mutableList[0] = 100
        println("  底层修改: $mutableList")
        
        // 创建快照（复制为不可变）
        val snapshot = mutableList.toList()
        println("  快照: $snapshot")
        
        // Set 去重
        val unique = listOf(1, 2, 2, 3, 3, 3).toSet()
        println("  去重 Set: $unique")
        
        // Map
        val map = mapOf("a" to 1, "b" to 2)
        println("  Map: $map")
        println()
    }
    
    /**
     * List 常用操作（面试高频）
     * 
     * map：转换每个元素
     * filter：筛选元素
     * flatMap：扁平化
     * associate：转 Map
     * groupBy：分组
     */
    private fun demoListOperations() {
        println("--- List 操作 ---")
        
        val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        
        // map：转换
        val doubled = numbers.map { it * 2 }
        println("  map { *2 }: $doubled")
        
        // filter：筛选
        val evens = numbers.filter { it % 2 == 0 }
        println("  filter 偶数: $evens")
        
        // map + filter 链式
        val evenDoubled = numbers.filter { it % 2 == 0 }.map { it * 2 }
        println("  筛选后翻倍: $evenDoubled")
        
        // flatMap：扁平化（每个元素展开为多个）
        val words = listOf("hello", "world")
        val chars = words.flatMap { it.toList() }
        println("  flatMap 转字符: $chars")
        
        // associate / associateBy：转 Map
        data class Person(val id: Int, val name: String)
        val people = listOf(Person(1, "张三"), Person(2, "李四"))
        val idMap = people.associateBy { it.id }
        val nameMap = people.associate { it.id to it.name }
        println("  associateBy: $idMap")
        
        // groupBy：分组
        val grouped = numbers.groupBy { if (it % 2 == 0) "偶数" else "奇数" }
        println("  groupBy 奇偶: $grouped")
        
        // partition：拆分
        val (odd, even) = numbers.partition { it % 2 == 1 }
        println("  partition: 奇=$odd, 偶=$even")
        
        // take/skip：截取
        println("  前3: ${numbers.take(3)}, 跳过3: ${numbers.drop(3)}")
        
        // chunked：分块
        println("  分块(3): ${numbers.chunked(3)}")
        
        // zip：合并
        val zipped = listOf("a", "b", "c").zip(listOf(1, 2, 3))
        println("  zip: $zipped")
        println()
    }
    
    /**
     * Map 操作
     */
    private fun demoMapOperations() {
        println("--- Map 操作 ---")
        
        val prices = mapOf(
            "apple" to 5.0,
            "banana" to 3.0,
            "orange" to 4.0
        )
        
        // getOrElse：安全获取
        val applePrice = prices.getOrElse("apple") { 0.0 }
        println("  apple 价格: $applePrice")
        
        // getOrDefault（Kotlin 1.6+）
        val unknownPrice = prices["unknown"] ?: 0.0
        println("  unknown 价格: $unknownPrice")
        
        // mapValues / mapKeys：转换
        val doubledPrices = prices.mapValues { (_, v) -> v * 2 }
        println("  价格翻倍: $doubledPrices")
        
        // filter 筛选
        val expensive = prices.filter { (_, v) -> v > 3.5 }
        println("  贵的水果: $expensive")
        
        // forEach 遍历
        println("  遍历:")
        prices.forEach { (k, v) -> println("    $k = $v") }
        
        // += -= 操作符
        val mutablePrices = prices.toMutableMap()
        mutablePrices["grape"] = 6.0
        mutablePrices.remove("banana")
        println("  修改后: $mutablePrices")
        println()
    }
    
    /**
     * Sequence 惰性求值 vs List 立即求值
     * 
     * Sequence：
     * - 惰性求值，中间操作不立即执行
     * - 只有终端操作才触发计算
     * - 避免中间集合创建
     * - 大数据量时性能更优
     * 
     * List：
     * - 立即求值，每个操作都返回新 List
     * - 每个中间操作都会创建新集合
     * - 小数据量时更直观
     */
    private fun demoSequence() {
        println("--- Sequence 惰性求值 ---")
        
        val numbers = (1..100).toList()
        
        // List：每个操作创建新 List
        println("--- List 操作（立即求值）---")
        val startTime = System.currentTimeMillis()
        val resultList = numbers
            .map { 
                // 每次 map 都被执行
                println("    [List] map: $it")
                it * 2 
            }
            .filter { 
                println("    [List] filter: $it")
                it > 50 
            }
            .take(5)
        val listTime = System.currentTimeMillis() - startTime
        println("  结果: $resultList, 耗时: ${listTime}ms")
        
        // Sequence：只有终端操作才触发
        println("\n--- Sequence 操作（惰性求值）---")
        val seqStartTime = System.currentTimeMillis()
        val resultSeq = numbers
            .asSequence()
            .map { 
                println("    [Seq] map: $it")
                it * 2 
            }
            .filter { 
                println("    [Seq] filter: $it")
                it > 50 
            }
            .take(5)
            .toList() // 终端操作：触发所有计算
        val seqTime = System.currentTimeMillis() - seqStartTime
        println("  结果: $resultSeq, 耗时: ${seqTime}ms")
        
        // 验证：Seq 打印更少（只处理需要的元素）
        // 但在本例中因为每个元素都符合条件，打印数相同
        
        // 性能对比：处理大数据量
        val bigNumbers = (1..1000000).toList()
        
        val bigListTime = measureTime {
            bigNumbers.map { it * 2 }.filter { it > 1000000 }.take(10)
        }
        
        val bigSeqTime = measureTime {
            bigNumbers.asSequence().map { it * 2 }.filter { it > 1000000 }.take(10).toList()
        }
        
        println("\n  大数据量对比:")
        println("  List: ${bigListTime}ms")
        println("  Sequence: ${bigSeqTime}ms")
        println()
    }
    
    /**
     * 电商场景：订单处理流水线
     */
    private fun demoOrderProcessing() {
        println("--- 电商订单处理示例 ---")
        
        data class Order(
            val id: String,
            val amount: Double,
            val status: String,
            val items: List<String>
        )
        
        val orders = listOf(
            Order("O001", 150.0, "PENDING", listOf("A", "B")),
            Order("O002", 50.0, "PAID", listOf("C")),
            Order("O003", 200.0, "PENDING", listOf("D", "E", "F")),
            Order("O004", 80.0, "COMPLETED", listOf("G")),
            Order("O005", 300.0, "PAID", listOf("H", "I"))
        )
        
        // 1. 筛选已支付订单
        val paidOrders = orders.filter { it.status == "PAID" }
        println("  已支付订单: ${paidOrders.map { it.id }}")
        
        // 2. 计算总金额
        val totalAmount = orders.sumOf { it.amount }
        println("  总金额: ¥${totalAmount}")
        
        // 3. 按状态分组
        val byStatus = orders.groupBy { it.status }
        println("  按状态分组: ${byStatus.keys}")
        
        // 4. 扁平化所有商品
        val allItems = orders.flatMap { it.items }
        println("  所有商品: $allItems")
        
        // 5. 统计商品出现次数
        val itemCounts = allItems.groupingBy { it }.eachCount()
        println("  商品统计: $itemCounts")
        
        // 6. 大额订单（Sequence 优化）
        val bigOrders = orders.asSequence()
            .filter { it.amount > 100 }
            .map { it.copy(amount = it.amount * 0.95) } // 95折
            .toList()
        println("  大额订单(95折): ${bigOrders.map { "${it.id}:¥${it.amount}" }}")
        println()
    }
    
    // 简单耗时测量
    private fun measureTime(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }
}
