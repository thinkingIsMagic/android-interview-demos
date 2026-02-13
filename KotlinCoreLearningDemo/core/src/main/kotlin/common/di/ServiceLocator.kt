package core.common.di

/**
 * 简单 DI 容器 - 手写依赖注入
 * 
 * 为什么需要 DI？
 * - 解耦：依赖抽象而非具体
 * - 可测试：可替换 mock/fake 实现
 * - 灵活：运行时切换实现
 * 
 * 本实现：
 * - 简单 Service Locator 模式
 * - 适用于小型项目或演示
 * - 实际工程推荐 Hilt/Koin
 */

/**
 * DI 容器单例
 */
object ServiceLocator {
    
    /**
     * 注册表：类型 -> 实例提供器
     */
    private val registry = mutableMapOf<KClass<*>, () -> Any>()
    
    /**
     * 已解析的实例缓存（单例模式）
     */
    private val instances = mutableMapOf<KClass<*>, Any>()
    
    /**
     * 注册依赖
     * 
     * @param factory 实例创建工厂
     * @param T 类型
     */
    inline fun <reified T : Any> register(
        noinline factory: () -> T
    ) {
        registry[T::class] = factory
    }
    
    /**
     * 获取实例（每次创建新实例）
     */
    inline fun <reified T : Any> get(): T {
        val factory = registry[T::class]
            ?: throw IllegalStateException("未注册类型: ${T::class.simpleName}")
        @Suppress("UNCHECKED_CAST")
        return factory() as T
    }
    
    /**
     * 获取单例实例（同一实例）
     */
    inline fun <reified T : Any> getSingleton(): T {
        val cached = instances[T::class]
        if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
        
        val instance = get<T>()
        instances[T::class] = instance
        return instance
    }
    
    /**
     * 注册为单例
     */
    inline fun <reified T : Any> registerSingleton(
        noinline factory: () -> T
    ) {
        registry[T::class] = {
            getSingletonFromCacheOrCreate<T>(factory)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getSingletonFromCacheOrCreate(factory: () -> T): T {
        val clazz = T::class
        val cached = instances[clazz]
        if (cached != null) {
            return cached as T
        }
        val instance = factory()
        instances[clazz] = instance
        return instance
    }
    
    /**
     * 重置容器（用于测试）
     */
    fun reset() {
        registry.clear()
        instances.clear()
    }
}

/**
 * 便捷扩展函数
 */
inline fun <reified T : Any> di(): T = ServiceLocator.get()

inline fun <reified T : Any> diSingleton(): T = ServiceLocator.getSingleton()
