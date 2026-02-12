package com.mall.perflab.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.util.LruCache
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.TraceLogger
import java.io.File
import java.io.FileOutputStream

/**
 * Bitmap优化处理器
 *
 * 【优化原理】
 * 1. 采样率缩放：用BitmapFactory.Options.inSampleSize降低分辨率
 * 2. 内存复用：Bitmap复用池，减少内存分配
 * 3. 格式选择：ARGB_8888(32bit) vs RGB_565(16bit)
 * 4. 缓存策略：内存缓存已解码Bitmap
 *
 * 【效果对比】
 * - 1080x1920原图：~8MB内存
 * - 1/2采样后：~2MB内存
 * - RGB_565格式：~1MB内存
 *
 * 【使用场景】
 * - 列表图片展示
 * - 缩略图加载
 * - 瀑布流图片
 */
object BitmapProcessor {

    // ==================== 配置 ====================

    companion object {
        // 内存缓存：最大10MB
        private const val MAX_MEMORY_CACHE = 10 * 1024 * 1024

        // 复用池最大数量
        private const val RECYCLE_POOL_MAX = 5

        // 默认采样率
        private const val DEFAULT_SAMPLE_SIZE = 2

        // 目标最大边（用于缩放）
        private const val MAX_DIMENSION = 1080
    }

    // ==================== 组件 ====================

    // 内存缓存（LruCache自动淘汰）
    private val memoryCache = object : LruCache<String, Bitmap>(MAX_MEMORY_CACHE) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount

        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap?,
            newValue: Bitmap?
        ) {
            if (evicted) {
                // 放入复用池
                if (oldValue != null && !oldValue.isRecycled) {
                    recyclePool.add(oldValue)
                    if (recyclePool.size > RECYCLE_POOL_MAX) {
                        recyclePool.removeAt(0).recycle()
                    }
                }
            }
        }
    }

    // Bitmap复用池（用于inBitmap）
    private val recyclePool = mutableListOf<Bitmap>()

    // ==================== 核心API ====================

    /**
     * 加载Bitmap（带优化）
     *
     * 优化策略：
     * 1. 先查缓存
     * 2. 缓存未命中则采样加载
     * 3. 尝试复用已有Bitmap
     */
    fun loadBitmap(context: Context, path: String): Bitmap? {
        return loadBitmap(context, path, null)
    }

    /**
     * 加载Bitmap（指定目标尺寸）
     */
    fun loadBitmap(
        context: Context,
        path: String,
        targetSize: Pair<Int, Int>? = null
    ): Bitmap? {
        // 1. 检查内存缓存
        val cacheKey = "${path}_${targetSize?.first ?: 0}x${targetSize?.second ?: 0}"
        memoryCache.get(cacheKey)?.let { cached ->
            TraceLogger.Cache.hit(cacheKey)
            return cached
        }
        TraceLogger.Cache.miss(cacheKey)

        // 2. 获取图片尺寸（不加载）
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        // 3. 计算采样率
        val sampleSize = calculateSampleSize(options, targetSize)

        // 4. 复用已有Bitmap（减少内存分配）
        val reuseBitmap = findReusableBitmap(sampleSize, options)

        // 5. 采样加载
        val loadOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = choosePixelFormat()
            inBitmap = reuseBitmap
        }

        val bitmap = BitmapFactory.decodeFile(path, loadOptions)

        // 6. 存入缓存
        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap)
            TraceLogger.Cache.save(cacheKey, bitmap.byteCount)
        }

        return bitmap
    }

    /**
     * 从资源加载Bitmap（带优化）
     */
    fun loadBitmap(context: Context, resId: Int): Bitmap? {
        return try {
            val cacheKey = "res_$resId"

            // 查缓存
            memoryCache.get(cacheKey)?.let { return it }

            // 采样加载
            val options = BitmapFactory.Options().apply {
                inSampleSize = DEFAULT_SAMPLE_SIZE
                inPreferredConfig = choosePixelFormat()
            }

            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)

            if (bitmap != null) {
                memoryCache.put(cacheKey, bitmap)
            }

            bitmap
        } catch (e: Exception) {
            TraceLogger.e("BITMAP", "资源加载失败: $resId", e)
            null
        }
    }

    /**
     * 缩放Bitmap
     */
    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val (newWidth, newHeight) = calculateScaleSize(bitmap.width, bitmap.height, maxWidth, maxHeight)

        if (newWidth == bitmap.width && newHeight == bitmap.height) {
            return bitmap
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 裁剪Bitmap
     */
    fun cropBitmap(
        source: Bitmap,
        srcRect: Rect,
        dstWidth: Int,
        dstHeight: Int
    ): Bitmap {
        val result = Bitmap.createBitmap(dstWidth, dstHeight, choosePixelFormat())
        val canvas = Canvas(result)
        val dstRect = Rect(0, 0, dstWidth, dstHeight)
        canvas.drawBitmap(source, srcRect, dstRect, null)
        return result
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        memoryCache.evictAll()
        recyclePool.forEach { it.recycle() }
        recyclePool.clear()
    }

    /**
     * 获取缓存统计
     */
    fun getStats(): Map<String, Any> {
        return mapOf(
            "cacheSize" to memoryCache.size(),
            "cacheMaxSize" to MAX_MEMORY_CACHE,
            "recyclePoolSize" to recyclePool.size,
            "hitRate" to "N/A" // 简化，实际应记录命中次数
        )
    }

    // ==================== 内部方法 ====================

    /**
     * 计算采样率
     *
     * 【原理】
     * inSampleSize = 2 表示长宽都变为原来的1/2
     * 内存占用变为原来的 1/4
     */
    private fun calculateSampleSize(
        options: BitmapFactory.Options,
        targetSize: Pair<Int, Int>?
    ): Int {
        val (width, height) = targetSize ?: Pair(MAX_DIMENSION, MAX_DIMENSION)

        var sampleSize = 1

        if (options.outWidth > width || options.outHeight > height) {
            val widthRatio = options.outWidth / width
            val heightRatio = options.outHeight / height
            sampleSize = maxOf(widthRatio, heightRatio)
        }

        // 采样率必须是2的幂
        var powerOfTwo = 1
        while (powerOfTwo < sampleSize) {
            powerOfTwo *= 2
        }

        return powerOfTwo.coerceAtLeast(1)
    }

    /**
     * 选择像素格式
     *
     * 【原理】
     * - ARGB_8888: 32位，透明度+RGB，每像素4字节
     * - RGB_565: 16位，无透明度，每像素2字节（内存减半）
     * - ALPHA_8: 8位，只有透明度
     */
    private fun choosePixelFormat(): Bitmap.Config {
        // Optimized模式下使用RGB_565省内存
        return if (FeatureToggle.isOptimized) {
            Bitmap.Config.RGB_565
        } else {
            Bitmap.Config.ARGB_8888
        }
    }

    /**
     * 查找可复用的Bitmap
     *
     * 【原理】Android 4.4+ 支持不同大小Bitmap复用
     * 复用条件：字节数 >= 新图，且不是正在使用
     */
    private fun findReusableBitmap(
        sampleSize: Int,
        options: BitmapFactory.Options
    ): Bitmap? {
        if (!FeatureToggle.isOptimized) return null

        return recyclePool.find { bitmap ->
            // 检查字节数是否足够
            val requiredSize = (options.outWidth / sampleSize) *
                    (options.outHeight / sampleSize) *
                    getBytesPerPixel(choosePixelFormat())

            bitmap.byteCount >= requiredSize && !bitmap.isRecycled
        }
    }

    private fun getBytesPerPixel(config: Bitmap.Config): Int {
        return when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            else -> 4
        }
    }

    /**
     * 计算缩放尺寸
     */
    private fun calculateScaleSize(
        srcWidth: Int,
        srcHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        val ratio = minOf(
            maxWidth.toFloat() / srcWidth,
            maxHeight.toFloat() / srcHeight
        )

        return Pair(
            (srcWidth * ratio).toInt(),
            (srcHeight * ratio).toInt()
        )
    }
}
