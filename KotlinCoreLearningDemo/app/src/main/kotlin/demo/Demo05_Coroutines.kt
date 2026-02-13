package demo

/**
 * Kotlin 协程核心演示
 * 
 * 覆盖考点：
 * 1. suspend 函数（挂起函数）
 * 2. CoroutineScope 与 Dispatcher
 * 3. 结构化并发
 * 4. 取消与超时
 * 5. 异常传播
 * 6. Flow（cold/hot）
 */
object Demo05_Coroutines {
    
    fun run() {
        println(">>> Demo05: 协程核心")
        println()
        
        // 基础挂起函数
        demoSuspendFunction()
        
        // CoroutineScope 与 Dispatcher
        demoCoroutineScope()
        
        // 结构化并发
        demoStructuredConcurrency()
        
        // 取消与超时
        demoCancellation()
        
        // 异常处理
        demoExceptionHandling()
        
        // Flow 演示
        demoFlow()
        
        println()
    }
    
    /**
     * suspend 函数（协程核心）
     * 
     * 什么是 suspend？
     * - 函数可以暂停执行（不阻塞线程）
     * - 恢复时从暂停点继续
     * 
     * 原理：
     * - 编译器将 suspend 函数转换为状态机
     * - 每次挂起都是一个状态切换
     * - 底层通过 Continuation 实现
     * 
     * 常见坑：
     * - suspend 函数只能在协程或其他 suspend 函数中调用
     * - 挂起不会阻塞线程，但会挂起协程
     */
    private fun demoSuspendFunction() {
        println("--- suspend 挂起函数 ---")
        
        // 定义 suspend 函数（模拟耗时操作）
        // 只能在协程或另一个 suspend 函数中调用
        suspend fun fetchActivity(id: String): String {
            // 模拟网络请求（实际会挂起协程，不阻塞线程）
            kotlinx.coroutines.delay(100) // 模拟异步等待
            return "Activity_$id"
        }
        
        // 运行协程
        kotlinx.coroutines.runBlocking {
            println("  开始获取活动...")
            val result = fetchActivity("A001")
            println("  获取结果: $result")
        }
        
        // 注意：runBlocking 会阻塞线程（主线程），实际工程中避免使用
        // 实际工程中使用 GlobalScope、ViewModelScope 或自定义 Scope
    }
    
    /**
     * CoroutineScope 与 Dispatcher
     * 
     * Dispatcher（调度器）：
     * - Dispatchers.Default：CPU 密集型（默认线程数 = CPU 核心数）
     * - Dispatchers.IO：IO 密集型（可更多线程，max(64, CPU数)）
     * - Dispatchers.Main：主线程（Android/UI 更新）
     * - Dispatchers.Unconfined：不指定（继承调用者线程）
     * 
     * withContext：切换调度器
     * 
     * 面试点：协程 vs 线程池？
     * - 协程更轻量（数千个 vs 数十个）
     * - 协程自动切换线程
     * - 结构化并发自动管理生命周期
     */
    private fun demoCoroutineScope() {
        println("--- CoroutineScope 与 Dispatcher ---")
        
        kotlinx.coroutines.runBlocking {
            // 使用不同调度器
            withContext(kotlinx.coroutines.Dispatchers.Default) {
                println("  Default 线程: ${Thread.currentThread().name}")
            }
            
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                println("  IO 线程: ${Thread.currentThread().name}")
            }
            
            // launch 不切换调度器
            kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.Default) {
                println("  launch 在: ${Thread.currentThread().name}")
            }
        }
        
        // 自定义 CoroutineScope（用于实际工程）
        val myScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        
        myScope.launch {
            println("  自定义 Scope 线程: ${Thread.currentThread().name}")
        }
        
        // 清理（实际工程中通常用 lifecycleScope 或 ViewModelScope）
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.delay(100)
            myScope.cancel()
        }
    }
    
    /**
     * 结构化并发
     * 
     * 结构化并发：
     * - 协程在 CoroutineScope 中启动
     * - 父协程等待所有子协程完成
     * - 子协程异常会传播给父协程
     * 
     * 好处：
     * - 自动管理生命周期
     * - 避免协程泄露
     * - 异常自动传播
     */
    private fun demoStructuredConcurrency() {
        println("--- 结构化并发 ---")
        
        kotlinx.coroutines.runBlocking {
            println("  启动父协程...")
            
            // 启动多个子协程
            val job1 = kotlinx.coroutines.launch {
                repeat(3) {
                    kotlinx.coroutines.delay(100)
                    println("    子协程1: $it")
                }
            }
            
            val job2 = kotlinx.coroutines.launch {
                repeat(3) {
                    kotlinx.coroutines.delay(150)
                    println("    子协程2: $it")
                }
            }
            
            println("  等待子协程完成...")
            
            // join 等待子协程完成
            job1.join()
            job2.join()
            
            println("  所有子协程完成!")
        }
        
        // async 并发计算（返回 Deferred，类似 Future）
        kotlinx.coroutines.runBlocking {
            println("\n  使用 async 并发计算...")
            
            val startTime = System.currentTimeMillis()
            
            val deferred1 = kotlinx.coroutines.async {
                kotlinx.coroutines.delay(100)
                1 + 1
            }
            
            val deferred2 = kotlinx.coroutines.async {
                kotlinx.coroutines.delay(200)
                2 + 2
            }
            
            // await 等待结果
            val result1 = deferred1.await()
            val result2 = deferred2.await()
            
            println("  结果: $result1, $result2")
            println("  耗时: ${System.currentTimeMillis() - startTime}ms (并发)")
        }
        
        // 注意：async 并发时两个 delay 同时执行，总耗时约 200ms
        // 串行执行需要 300ms
    }
    
    /**
     * 取消与超时
     * 
     * 取消机制：
     * - isActive 检查协程是否活跃
     * - throw CancellationException 取消协程
     * - finally 释放资源
     * 
     * 超时：withTimeout / withTimeoutOrNull
     */
    private fun demoCancellation() {
        println("--- 取消与超时 ---")
        
        kotlinx.coroutines.runBlocking {
            // 启动长任务
            val job = kotlinx.coroutines.launch {
                try {
                    repeat(10) { i ->
                        kotlinx.coroutines.delay(100)
                        println("    任务: $i")
                        
                        // 检查是否取消（重要！）
                        if (!isActive) {
                            println("  协程已取消")
                            return@launch
                        }
                    }
                } finally {
                    // finally 总会执行（用于资源清理）
                    println("  清理资源...")
                }
            }
            
            kotlinx.coroutines.delay(250) // 等待 250ms
            println("  取消协程...")
            job.cancel()
            job.join()
            println("  协程已取消")
            
            // 超时演示
            println("\n  超时演示...")
            try {
                kotlinx.coroutines.withTimeout(200) {
                    repeat(5) {
                        kotlinx.coroutines.delay(100)
                        println("    超时任务: $it")
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                println("  任务超时!")
            }
            
            // withTimeoutOrNull 超时返回 null
            val result = kotlinx.coroutines.withTimeoutOrNull(200) {
                "完成!"
            }
            println("  超时结果: $result")
        }
    }
    
    /**
     * 异常传播
     * 
     * 结构化并发中的异常传播：
     * - 子协程异常会取消父协程
     * - supervisorScope 隔离异常（不传播给父）
     * 
     * 常见模式：
     * - try/catch 捕获
     * - CoroutineExceptionHandler 全局处理
     */
    private fun demoExceptionHandling() {
        println("--- 异常处理 ---")
        
        kotlinx.coroutines.runBlocking {
            // launch 异常自动传播
            val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
                println("  全局异常处理: ${throwable.message}")
            }
            
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + handler)
            
            scope.launch {
                try {
                    throw RuntimeException("模拟异常")
                } catch (e: Exception) {
                    println("  捕获异常: ${e.message}")
                }
            }
            
            kotlinx.coroutines.delay(100)
            scope.cancel()
        }
        
        // supervisorScope：子协程异常不影响兄弟协程
        println("\n  supervisorScope 演示:")
        kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.supervisorScope {
                launch {
                    kotlinx.coroutines.delay(100)
                    println("  任务1 完成")
                }
                
                launch {
                    // 异常只影响这个协程
                    throw RuntimeException("任务2异常")
                }
                
                launch {
                    kotlinx.coroutines.delay(50)
                    println("  任务3 完成（不受影响）")
                }
            }
            println("  supervisorScope 完成")
        }
    }
    
    /**
     * Flow 冷流 vs 热流
     * 
     * Flow（冷流）：
     * - 惰性发布，只有收集时才执行
     * - 每次收集都会重新执行
     * - 类似 Sequence
     * 
     * SharedFlow（热流）：
     * - 发射后立即发送给所有订阅者
     * - 可配置重放策略
     * - 适合事件总线、状态更新
     * 
     * StateFlow（热流）：
     * - 始终有当前值
     * - 类似 LiveData
     */
    private fun demoFlow() {
        println("--- Flow 冷流与热流 ---")
        
        kotlinx.coroutines.runBlocking {
            // ===== Cold Flow =====
            println("--- Cold Flow ---")
            
            // 创建 Flow（惰性，不收集不执行）
            val coldFlow = kotlinx.coroutines.flow {
                println("    开始发射...")
                emit(1)
                kotlinx.coroutines.delay(100)
                emit(2)
                kotlinx.coroutines.delay(100)
                emit(3)
                println("    发射完成")
            }
            
            println("  Flow 已创建（未执行）")
            
            // 第一次收集
            println("  第一次收集:")
            coldFlow.collect { value ->
                println("    收到: $value")
            }
            
            // 第二次收集（重新执行）
            println("  第二次收集（重新执行）:")
            coldFlow.collect { value ->
                println("    收到: $value")
            }
            
            // ===== SharedFlow =====
            println("\n--- SharedFlow (热流) ---")
            
            // 创建 SharedFlow
            val sharedFlow = kotlinx.coroutines.flow.MutableSharedFlow<Int>(replay = 2)
            
            // 发射数据
            kotlinx.coroutines.launch {
                kotlinx.coroutines.delay(100)
                sharedFlow.emit(1)
                println("  发射: 1")
                kotlinx.coroutines.delay(100)
                sharedFlow.emit(2)
                println("  发射: 2")
            }
            
            // 收集（收到重放的 2 个值）
            println("  收集 SharedFlow:")
            kotlinx.coroutines.launch {
                sharedFlow.collect { value ->
                    println("    收到: $value")
                }
            }
            
            kotlinx.coroutines.delay(300)
            
            // ===== StateFlow =====
            println("\n--- StateFlow ---")
            
            // StateFlow 始终有当前值
            val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
            
            kotlinx.coroutines.launch {
                stateFlow.collect { value ->
                    println("    StateFlow 收到: $value")
                }
            }
            
            // 更新状态
            stateFlow.value = 1
            kotlinx.coroutines.delay(50)
            stateFlow.value = 2
            kotlinx.coroutines.delay(50)
            
            // ===== 电商场景：活动状态更新 =====
            println("\n--- 电商场景：实时活动状态 ---")
            
            data class ActivityState(
                val id: String,
                val name: String,
                val status: String,
                val stock: Int
            )
            
            val activityState = kotlinx.coroutines.flow.MutableStateFlow(
                ActivityState("A001", "秒杀活动", "未开始", 100)
            )
            
            kotlinx.coroutines.launch {
                activityState.collect { state ->
                    println("  [状态更新] ${state.name} - ${state.status} - 库存:${state.stock}")
                }
            }
            
            // 模拟状态更新
            activityState.value = activityState.value.copy(status = "进行中")
            kotlinx.coroutines.delay(50)
            activityState.value = activityState.value.copy(stock = 99)
            kotlinx.coroutines.delay(50)
            
            // 模拟 SharedFlow 事件（一次性）
            val events = kotlinx.coroutines.flow.MutableSharedFlow<String>()
            
            kotlinx.coroutines.launch {
                events.collect { event ->
                    println("  [事件] $event")
                }
            }
            
            events.emit("活动即将开始!")
            events.emit("库存告警!")
            kotlinx.coroutines.delay(100)
        }
    }
}
