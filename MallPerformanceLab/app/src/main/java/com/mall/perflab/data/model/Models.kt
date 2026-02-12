package com.mall.perflab.data.model

/**
 * 商城首页数据模型
 *
 * 结构：
 * - marketingFloors: 营销楼层列表（Banner/Coupon/Grid等）
 * - feedItems: Feed流列表
 * - hasMore: 是否有更多数据
 * - nextPage: 下一页页码
 */
data class MallData(
    val marketingFloors: List<MarketingFloor>,
    val feedItems: List<FeedItem>,
    val hasMore: Boolean = true,
    val nextPage: Int = 1
)

/**
 * 营销楼层基类
 * 支持不同类型楼层（Banner/Coupon/Grid等）
 */
sealed class MarketingFloor {

    abstract val id: String
    abstract val type: FloorType
    abstract val priority: Int  // 渲染优先级

    /**
     * 楼层类型枚举
     */
    enum class FloorType(val displayName: String) {
        BANNER("轮播Banner"),
        COUPON("优惠券"),
        GRID("商品网格"),
        FLASH_SALE("限时秒杀"),
        NAV("导航入口")
    }
}

/**
 * Banner楼层
 */
data class BannerFloor(
    override val id: String,
    override val priority: Int,
    val banners: List<BannerItem>
) : MarketingFloor() {
    override val type: FloorType = FloorType.BANNER
}

/**
 * 单个Banner项
 */
data class BannerItem(
    val imageUrl: String,
    val title: String,
    val link: String
)

/**
 * Coupon楼层
 */
data class CouponFloor(
    override val id: String,
    override val priority: Int,
    val coupons: List<CouponItem>
) : MarketingFloor() {
    override val type: FloorType = FloorType.COUPON
}

/**
 * 单个优惠券项
 */
data class CouponItem(
    val title: String,
    val discountAmount: Double,
    val condition: String,
    val expireTime: String
)

/**
 * Grid楼层（商品网格）
 */
data class GridFloor(
    override val id: String,
    override val priority: Int,
    val title: String,
    val products: List<ProductItem>
) : MarketingFloor() {
    override val type: FloorType = FloorType.GRID
}

/**
 * 单个商品项
 */
data class ProductItem(
    val id: String,
    val name: String,
    val price: Double,
    val originalPrice: Double,
    val imageUrl: String
)

/**
 * Feed列表项
 * 支持多种类型混排
 */
sealed class FeedItem {
    abstract val id: String
    abstract val position: Int  // 在列表中的位置
    abstract val feedType: FeedType  // Feed类型

    /**
     * Feed类型
     */
    enum class FeedType(val displayName: String) {
        PRODUCT("商品"),
        BANNER("广告Banner"),
        ACTIVITY("活动入口")
    }
}

/**
 * Feed商品项
 */
data class ProductFeedItem(
    override val id: String,
    override val position: Int,
    val product: ProductItem,
    val likeCount: Int = 0,
    val commentCount: Int = 0
) : FeedItem() {
    override val feedType: FeedType = FeedType.PRODUCT
}

/**
 * Feed广告Banner项
 */
data class BannerFeedItem(
    override val id: String,
    override val position: Int,
    val imageUrl: String,
    val title: String
) : FeedItem() {
    override val feedType: FeedType = FeedType.BANNER
}
