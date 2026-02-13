package core.domain.model

/**
 * 用户领域模型
 * 
 * 展示 Kotlin 数据类的使用：
 * - 不可变属性（val）
 * - 默认参数
 * - 解构声明
 */
data class User(
    val id: String,
    val name: String,
    val level: Int = 1,
    val tags: List<String> = emptyList()
) {
    /**
     * 是否是 VIP 用户
     */
    val isVip: Boolean get() = level >= 3
    
    /**
     * 用户等级名称
     */
    val levelName: String get() = when (level) {
        1 -> "普通会员"
        2 -> "银牌会员"
        3 -> "金牌会员"
        4 -> "铂金会员"
        5 -> "钻石会员"
        else -> "未知"
    }
}

/**
 * 订单领域模型
 */
data class Order(
    val id: String,
    val userId: String,
    val items: List<OrderItem>,
    val status: OrderStatus = OrderStatus.PENDING
) {
    /**
     * 订单总金额
     */
    val totalAmount: Double get() = items.sumOf { it.price * it.quantity }
    
    /**
     * 商品总数
     */
    val itemCount: Int get() = items.sumOf { it.quantity }
}

/**
 * 订单商品
 */
data class OrderItem(
    val productId: String,
    val name: String,
    val price: Double,
    val quantity: Int
)

/**
 * 订单状态
 */
enum class OrderStatus {
    PENDING,    // 待支付
    PAID,       // 已支付
    SHIPPED,    // 已发货
    DELIVERED,  // 已送达
    COMPLETED,  // 已完成
    CANCELLED   // 已取消
}
