package com.mall.perflab.core.network

import android.util.Base64
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPOutputStream

/**
 * 网络请求优化器
 *
 * 【优化原理】
 * 1. 请求合并：多个小请求合并为一个，减少连接开销
 * 2. 数据压缩：GZIP压缩减少传输量（通常减少60-70%）
 * 3. 增量数据：只请求变化的部分
 * 4. 协议优化：HTTP/2多路复用
 *
 * 【效果对比】
 * - 单个请求：3个请求 = 3次RTT + 3次握手
 * - 合并请求：1次RTT + 1次握手
 * - GZIP：100KB → 30KB
 */
object NetworkOptimizer {

    // ==================== 配置 ====================

    companion object {
        // 合并请求的最大等待时间（毫秒）
        private const val BATCH_DELAY_MS = 50L

        // 合并请求的最大数量
        private const val BATCH_MAX_SIZE = 10

        // GZIP压缩阈值（字节）
        private const val GZIP_THRESHOLD = 512
    }

    // ==================== 组件 ====================

    // 待合并的请求队列
    private val pendingRequests = ConcurrentHashMap<String, MutableList<BatchRequest>>()

    // 请求计数器
    private val requestCounter = java.util.concurrent.atomic.AtomicInteger(0)

    // ==================== 核心API ====================

    /**
     * 执行GET请求（带优化）
     */
    suspend fun get(url: String): String {
        return withContext(Dispatchers.IO) {
            if (FeatureToggle.isOptimized) {
                optimizedGet(url)
            } else {
                basicGet(url)
            }
        }
    }

    /**
     * 执行POST请求（带优化）
     */
    suspend fun post(url: String, body: String): String {
        return withContext(Dispatchers.IO) {
            if (FeatureToggle.isOptimized) {
                optimizedPost(url, body)
            } else {
                basicPost(url, body)
            }
        }
    }

    /**
     * 合并请求 - 将多个请求合并为一个
     *
     * @param key 合并key，相同的key会合并
     * @param request 请求信息
     */
    suspend fun batchRequest(
        key: String,
        request: suspend () -> String
    ): String {
        val requestId = requestCounter.incrementAndGet()

        // 添加到待合并队列
        val batchList = pendingRequests.getOrPut(key) { mutableListOf() }
        val wrapper = BatchRequest(requestId, request)
        batchList.add(wrapper)

        // 触发合并执行
        if (batchList.size >= BATCH_MAX_SIZE) {
            executeBatch(key, batchList)
        } else {
            // 延迟执行，等待更多请求加入
            kotlinx.coroutines.delay(BATCH_DELAY_MS)
            if (batchList.isNotEmpty() && batchList.first().id == requestId) {
                executeBatch(key, batchList)
            }
        }

        return wrapper.result ?: ""
    }

    /**
     * 获取请求统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "pendingBatches" to pendingRequests.size,
            "totalRequests" to requestCounter.get()
        )
    }

    // ==================== 内部实现 ====================

    /**
     * 基础GET请求（无优化）
     */
    private fun basicGet(url: String): String {
        PerformanceTracker.trace("network_get_$url", "basic") {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                connection.inputStream.bufferedReader().readText()
            } finally {
                connection.disconnect()
            }
        }
        return ""
    }

    /**
     * 基础POST请求（无优化）
     */
    private fun basicPost(url: String, body: String): String {
        PerformanceTracker.trace("network_post_$url", "basic") {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            try {
                connection.outputStream.write(body.toByteArray())
                connection.inputStream.bufferedReader().readText()
            } finally {
                connection.disconnect()
            }
        }
        return ""
    }

    /**
     * 优化版GET请求（带压缩）
     */
    private fun optimizedGet(url: String): String {
        return PerformanceTracker.trace("network_get_opt_$url", "optimized") {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            // 优化点1：请求压缩
            connection.setRequestProperty("Accept-Encoding", "gzip")

            try {
                val encoding = connection.contentEncoding
                val inputStream = connection.inputStream

                // 优化点2：响应解压
                if ("gzip" == encoding) {
                    GZIPInputStream(inputStream).bufferedReader().readText()
                } else {
                    inputStream.bufferedReader().readText()
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * 优化版POST请求（带压缩）
     */
    private fun optimizedPost(url: String, body: String): String {
        return PerformanceTracker.trace("network_post_opt_$url", "optimized") {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            // 优化点：请求压缩
            if (body.length > GZIP_THRESHOLD) {
                connection.setRequestProperty("Content-Encoding", "gzip")
            }

            try {
                val outputStream = connection.outputStream

                // 优化点：GZIP压缩
                if (body.length > GZIP_THRESHOLD) {
                    GZIPOutputStream(outputStream).use { gzip ->
                        gzip.write(body.toByteArray())
                    }
                } else {
                    outputStream.write(body.toByteArray())
                }

                connection.inputStream.bufferedReader().readText()
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * 执行合并请求
     */
    private suspend fun executeBatch(
        key: String,
        requests: MutableList<BatchRequest>
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 清空队列
                pendingRequests.remove(key)

                // 模拟合并请求（实际项目中发送到批量API）
                val startTime = System.currentTimeMillis()

                // 这里模拟合并执行
                requests.forEach { req ->
                    val result = req.request()
                    req.result = result
                }

                val latency = System.currentTimeMillis() - startTime
                TraceLogger.i("NETWORK", "批量请求完成: ${requests.size}个, 耗时=${latency}ms")
            } catch (e: Exception) {
                TraceLogger.e("NETWORK", "批量请求失败: $key", e)
            }
        }
    }

    /**
     * 压缩字符串（GZIP）
     */
    fun compress(data: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(data.toByteArray()) }
        return output.toByteArray()
    }

    /**
     * 解压字符串
     */
    fun decompress(bytes: ByteArray): String {
        return GZIPInputStream(bytes.inputStream()).bufferedReader().readText()
    }

    /**
     * Base64编码
     */
    fun encodeBase64(data: String): String {
        return Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP)
    }

    // ==================== 数据类 ====================

    /**
     * 批量请求包装
     */
    data class BatchRequest(
        val id: Int,
        val request: suspend () -> String
    ) {
        @Volatile
        var result: String? = null
    }
}

/**
 * GZIP输入流包装
 */
private class GZIPInputStream(
    input: java.io.InputStream
) : java.util.zip.GZIPInputStream(input)
