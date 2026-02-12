package com.mall.perflab.data.mock

import android.os.Handler
import android.os.Looper
import com.mall.perflab.data.model.*
import kotlin.random.Random

/**
 * Mock数据生成器
 *
 * 用于生成测试数据，模拟网络请求延迟
 *
 * 使用方式：
 * - DataGenerator.generateMallData() // 生成首页数据
 * - DataGenerator.generateFeed(page) // 生成分页数据
 */
object DataGenerator {

    private val handler = Handler(Looper.getMainLooper())
    private val random = Random(System.currentTimeMillis())

    // 模拟网络延迟范围（毫秒）
    private const val MIN_LATENCY = 300L
    private const val MAX_LATENCY = 1200L

    // ==================== 延迟模拟 ====================

    /**
     * 模拟网络延迟
     * @param onComplete 延迟后回调
     */
    fun simulateNetworkDelay(onComplete: () -> Unit) {
        val latency = random.nextLong(MIN_LATENCY, MAX_LATENCY + 1)
        handler.postDelayed(onComplete, latency)
    }

    /**
     * 同步模拟延迟（阻塞线程）
     */
    fun syncSimulateDelay(): Long {
        val latency = random.nextLong(MIN_LATENCY, MAX_LATENCY + 1)
        Thread.sleep(latency)
        return latency
    }

    // ==================== 首页数据生成 ====================

    /**
     * 生成首页完整数据（营销区 + Feed）
     */
    fun generateMallData(
        page: Int = 0,
        callback: (MallData, Long) -> Unit
    ) {
        val latency = syncSimulateDelay()
        val data = MallData(
            marketingFloors = generateMarketingFloors(),
            feedItems = generateFeedItems(page),
            hasMore = page < 5, // 最多5页
            nextPage = page + 1
        )
        callback(data, latency)
    }

    /**
     * 生成首页数据（带延迟回调）
     */
    fun fetchMallData(
        page: Int = 0,
        onComplete: (MallData, Long) -> Unit
    ) {
        simulateNetworkDelay {
            val data = generateMallData(page) { d, _ -> onComplete(d, 0L) }
        }
    }

    // ==================== 营销楼层生成 ====================

    /**
     * 生成营销楼层列表
     */
    private fun generateMarketingFloors(): List<MarketingFloor> {
        return listOf(
            generateBannerFloor(),
            generateCouponFloor(),
            generateGridFloor()
        )
    }

    /**
     * 生成Banner楼层
     */
    private fun generateBannerFloor(): BannerFloor {
        return BannerFloor(
            id = "floor_banner_${System.currentTimeMillis()}",
            priority = 1,
            banners = (1..3).map { index ->
                BannerItem(
                    imageUrl = "https://picsum.photos/seed/banner$index/375/160",
                    title = "618大促 $index",
                    link = "https://mall.com/activity/$index"
                )
            }
        )
    }

    /**
     * 生成Coupon楼层
     */
    private fun generateCouponFloor(): CouponFloor {
        return CouponFloor(
            id = "floor_coupon_${System.currentTimeMillis()}",
            priority = 2,
            coupons = (1..5).map { index ->
                CouponItem(
                    title = "满${index * 100}减${index * 20}",
                    discountAmount = (index * 20).toDouble(),
                    condition = "满${index * 100}可用",
                    expireTime = "2026-12-31"
                )
            }
        )
    }

    /**
     * 生成Grid楼层
     */
    private fun generateGridFloor(): GridFloor {
        return GridFloor(
            id = "floor_grid_${System.currentTimeMillis()}",
            priority = 3,
            title = "热门推荐",
            products = (1..6).map { index ->
                generateProduct("grid_${index}")
            }
        )
    }

    // ==================== Feed数据生成 ====================

    /**
     * 生成分页Feed数据
     */
    private fun generateFeedItems(page: Int, pageSize: Int = 10): List<FeedItem> {
        val startIndex = page * pageSize
        return (0 until pageSize).map { offset ->
            val position = startIndex + offset
            if (position % 5 == 0) {
                // 每5个插入一个广告Banner
                generateBannerFeed("feed_banner_$position", position)
            } else {
                generateProductFeed("feed_product_$position", position)
            }
        }
    }

    /**
     * 生成单个ProductFeedItem
     */
    private fun generateProductFeed(id: String, position: Int): ProductFeedItem {
        return ProductFeedItem(
            id = id,
            position = position,
            product = generateProduct(id),
            likeCount = random.nextInt(0, 10000),
            commentCount = random.nextInt(0, 500)
        )
    }

    /**
     * 生成单个BannerFeedItem
     */
    private fun generateBannerFeed(id: String, position: Int): BannerFeedItem {
        return BannerFeedItem(
            id = id,
            position = position,
            imageUrl = "https://picsum.photos/seed/$id/375/200",
            title = "广告位 $position"
        )
    }

    /**
     * 生成单个ProductItem
     */
    private fun generateProduct(seed: String): ProductItem {
        val price = (20.0 + random.nextDouble() * 980).let { it - (it % 0.01) }
        return ProductItem(
            id = "product_$seed",
            name = "商品 ${seed.takeLast(8)}",
            price = price,
            originalPrice = price * (1.1 + random.nextDouble() * 0.5),
            imageUrl = "https://picsum.photos/seed/$seed/200/200"
        )
    }

    // ==================== 便捷方法 ====================

    /**
     * 生成单页Feed（供外部调用）
     */
    fun generateFeed(page: Int = 0): Pair<List<FeedItem>, Long> {
        val latency = syncSimulateDelay()
        return generateFeedItems(page) to latency
    }

    /**
     * 生成指定数量的随机字符串
     */
    fun randomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
