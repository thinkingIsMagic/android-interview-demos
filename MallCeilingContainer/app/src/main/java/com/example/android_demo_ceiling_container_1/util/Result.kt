package com.example.android_demo_ceiling_container_1.util

/**
 * 统一错误封装 Result<T>
 *
 * @property isSuccess 是否成功
 * @property data 成功时的数据
 * @property error 失败时的错误信息
 */
sealed class Result<out T> {

    data class Success<T>(val data: T) : Result<T>()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
        val code: Int = -1
    ) : Result<Nothing>()

    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success

    val isError: Boolean get() = this is Error

    val isLoading: Boolean get() = this is Loading

    /**
     * 获取成功数据，失败则返回默认值
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * 获取成功数据，失败则抛出异常
     */
    fun getOrThrow(): @UnsafeVariance T = when (this) {
        is Success -> data
        is Error -> throw exception ?: IllegalStateException(message)
        is Loading -> throw IllegalStateException("Result is still loading")
    }

    /**
     * 成功时的回调
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        // 判断当前是否成功
        if (this is Success) action(data)
        // 为了链式调用，返回 this
        return this
    }

    /**
     * 失败时的回调
     */
    inline fun onError(action: (Error) -> Unit): Result<T> {
        if (this is Error) action(this)
        return this
    }

    /**
     * map 转换成功数据
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    /**
     * flatMap 链式调用
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)

        fun error(message: String, exception: Throwable? = null, code: Int = -1): Result<Nothing> =
            Error(message, exception, code)

        fun loading(): Result<Nothing> = Loading
    }
}
