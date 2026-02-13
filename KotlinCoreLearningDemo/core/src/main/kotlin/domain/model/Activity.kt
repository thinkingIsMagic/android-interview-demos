package core.domain.model

/**
 * 电商活动引擎 - 领域模型
 * 
 * 本文件展示 Kotlin 在领域建模中的最佳实践：
 * - sealed class 表示有限状态/类型
 * - data class 表示值对象
 * - 非空类型默认安全
 */

/**
 * 活动类型 - 密封类
 * 
 * 为什么用 sealed class？
 * - 子类型在编译期确定
 * - when 表达式可以覆盖所有情况，编译器检查完整性
 * - 适合状态机、有限集合类型
 * 
 * 本项目中的活动类型：
 * - MoneyOff: 满减活动
 * - Discount: 折扣活动
 * - Seckill: 秒杀活动
 * - FreeShipping: 免邮活动
 */
sealed class ActivityType {
    
    /**
     * 满减活动
     * @property threshold 门槛金额
     * @property discount 优惠金额
     */
    data class MoneyOff(
        val threshold: Double,
        val discount: Double
    ) : ActivityType()
    
    /**
     * 折扣活动
     * @property rate 折扣率（如 0.8 表示 8 折）
     * @property maxDiscount 最大优惠金额（可选）
     */
    data class Discount(
        val rate: Double,
        val maxDiscount: Double? = null
    ) : ActivityType()
    
    /**
     * 秒杀活动
     * @property stock 库存数量
     * @property startTime 开始时间戳
     */
    data class Seckill(
        val stock: Int,
        val startTime: Long
    ) : ActivityType()
    
    /**
     * 免邮活动
     * 无额外参数
     */
    object FreeShipping : ActivityType()
}

/**
 * 活动实体
 * 
 * data class 自动生成：
 * - equals/hashCode：用于 Set/Map 去重
 * - toString：便于调试日志
 * - copy：创建修改后的副本
 * - componentN：解构声明
 */
data class Activity(
    /** 活动 ID */
    val id: String,
    /** 活动名称 */
    val name: String,
    /** 活动类型（密封类） */
    val type: ActivityType,
    /** 活动状态 */
    val status: ActivityStatus = ActivityStatus.ACTIVE,
    /** 开始时间 */
    val startTime: Long = System.currentTimeMillis(),
    /** 结束时间 */
    val endTime: Long? = null
)

/**
 * 活动状态枚举
 */
enum class ActivityStatus {
    PENDING,   // 待生效
    ACTIVE,    // 生效中
    EXPIRED,   // 已过期
    DISABLED   // 已禁用
}

/**
 * 促销活动计算结果
 */
data class PromotionResult(
    val originalAmount: Double,
    val discountedAmount: Double,
    val savedAmount: Double,
    val appliedPromotion: String,
    val isSuccess: Boolean,
    val message: String = if (isSuccess) "优惠计算成功" else "无优惠适用"
) {
    /**
     * 是否节省了费用
     */
    val hasSavings: Boolean get() = savedAmount > 0
    
    /**
     * 折扣比例（0-1）
     */
    val discountRate: Double get() = if (originalAmount > 0) {
        (originalAmount - savedAmount) / originalAmount
    } else {
        0.0
    }
}
