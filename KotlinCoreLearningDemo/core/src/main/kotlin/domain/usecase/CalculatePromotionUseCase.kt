package core.domain.usecase

import core.domain.model.*

/**
 * 计算促销优惠用例
 * 
 * 用例层（Use Case）职责：
 * - 编排业务逻辑
 * - 调用仓储获取数据
 * - 返回结果给表现层
 * 
 * 本类展示：
 * - when 表达式处理密封类
 * - 复杂的业务逻辑编排
 */
class CalculatePromotionUseCase {
    
    /**
     * 计算订单适用的优惠
     * 
     * 业务逻辑：
     * 1. 遍历所有可用活动
     * 2. 筛选适用的活动
     * 3. 计算最优优惠
     * 
     * @param order 订单
     * @param availableActivities 可用活动列表
     * @return 促销计算结果
     */
    fun execute(
        order: Order,
        availableActivities: List<Activity>
    ): PromotionResult {
        if (order.items.isEmpty()) {
            return PromotionResult(
                originalAmount = 0.0,
                discountedAmount = 0.0,
                savedAmount = 0.0,
                appliedPromotion = "",
                isSuccess = false,
                message = "订单为空"
            )
        }
        
        val originalAmount = order.totalAmount
        
        // 筛选适用的活动
        val applicablePromotions = availableActivities
            .filter { it.status == ActivityStatus.ACTIVE }
            .mapNotNull { activity ->
                calculatePromotion(activity, originalAmount)
            }
            .filter { it.savedAmount > 0 }
        
        if (applicablePromotions.isEmpty()) {
            return PromotionResult(
                originalAmount = originalAmount,
                discountedAmount = originalAmount,
                savedAmount = 0.0,
                appliedPromotion = "无",
                isSuccess = false,
                message = "暂无可用优惠"
            )
        }
        
        // 选择最优优惠（节省最多）
        val bestPromotion = applicablePromotions.maxByOrNull { it.savedAmount }
            ?: return PromotionResult(
                originalAmount = originalAmount,
                discountedAmount = originalAmount,
                savedAmount = 0.0,
                appliedPromotion = "无",
                isSuccess = false,
                message = "计算错误"
            )
        
        return PromotionResult(
            originalAmount = originalAmount,
            discountedAmount = bestPromotion.discountedAmount,
            savedAmount = bestPromotion.savedAmount,
            appliedPromotion = bestPromotion.appliedPromotion,
            isSuccess = true,
            message = "已享优惠"
        )
    }
    
    /**
     * 计算单个活动的优惠
     * 
     * 使用 when 表达式处理密封类的所有子类
     * 编译器确保覆盖所有情况
     */
    private fun calculatePromotion(
        activity: Activity,
        amount: Double
    ): PromotionResult? {
        return when (val type = activity.type) {
            // 满减活动
            is ActivityType.MoneyOff -> {
                if (amount >= type.threshold) {
                    PromotionResult(
                        originalAmount = amount,
                        discountedAmount = amount - type.discount,
                        savedAmount = type.discount,
                        appliedPromotion = "满${type.threshold}减${type.discount}",
                        isSuccess = true
                    )
                } else {
                    null // 未达门槛
                }
            }
            
            // 折扣活动
            is ActivityType.Discount -> {
                val discounted = amount * type.rate
                val finalDiscount = type.maxDiscount?.let { minOf(discounted, it) } ?: discounted
                val saved = amount - finalDiscount
                
                PromotionResult(
                    originalAmount = amount,
                    discountedAmount = finalDiscount,
                    savedAmount = saved,
                    appliedPromotion = "${(type.rate * 10).toInt()}折${if (type.maxDiscount != null) "（最高减${type.maxDiscount}）" else ""}",
                    isSuccess = true
                )
            }
            
            // 秒杀活动
            is ActivityType.Seckill -> {
                if (type.stock > 0 && System.currentTimeMillis() >= type.startTime) {
                    PromotionResult(
                        originalAmount = amount,
                        discountedAmount = amount * 0.9, // 秒杀固定 9 折
                        savedAmount = amount * 0.1,
                        appliedPromotion = "秒杀价（9折）",
                        isSuccess = true
                    )
                } else {
                    null
                }
            }
            
            // 免邮活动（不减免商品金额）
            is ActivityType.FreeShipping -> {
                null
            }
        }
    }
}
