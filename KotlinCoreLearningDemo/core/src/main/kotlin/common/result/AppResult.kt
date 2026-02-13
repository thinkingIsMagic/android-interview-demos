package core.common.result

/**
 * Result 封装 - 工程实践
 * 
 * 为什么封装 Result？
 * - Kotlin 标准库有 Result，但主要用于协程
 * - 自定义 Result 更语义化
 * - 可扩展错误处理（错误码、错误信息）
 * 
 * 好处：
 * - 强制处理错误（不能忽略）
 * - 避免使用 null 表示错误
 * - 链式调用（map/flatMap）
 */

/**
 * 业务操作结果封装
 * 
 * @param T 成功时的数据类型
 * @property isSuccess 是否成功
 * @property data 成功时的数据
 * @property error 失败时的错误信息
 */
sealed class AppResult<out T> {
    
    /**
     * 成功结果
     */
    data class Success<T>(val data: T) : AppResult<T>()
    
    /**
     * 失败结果
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : AppResult<Nothing>()
    
    /**
     * 是否成功
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * 是否失败
     */
    val isError: Boolean get() = this is Error
    
    /**
     * 获取成功数据，失败则返回默认值
     */
    fun getOrDefault(default: @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            is Error -> default
        }
    }
    
    /**
     * 获取成功数据，失败则抛异常
     */
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw exception ?: IllegalStateException(message)
        }
    }
    
    /**
     * 获取成功数据，失败则 null
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            is Error -> null
        }
    }
    
    /**
     * 映射成功数据
     */
    inline fun <R> map(transform: (T) -> R): AppResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
    
    /**
     * 扁平化映射（处理嵌套 Result）
     */
    inline fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
        }
    }
    
    /**
     * 执行成功时的操作
     */
    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * 执行失败时的操作
     */
    inline fun onError(action: (Error) -> Unit): AppResult<T> {
        if (this is Error) {
            action(this)
        }
        return this
    }
    
    companion object {
        /**
         * 创建成功结果
         */
        fun <T> success(data: T): AppResult<T> = Success(data)
        
        /**
         * 创建失败结果
         */
        fun error(message: String, exception: Throwable? = null): AppResult<Nothing> =
            Error(message, exception)
        
        /**
         * 捕获异常并封装
         */
        inline fun <T> runCatching(block: () -> T): AppResult<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e.message ?: "Unknown error", e)
            }
        }
    }
}
