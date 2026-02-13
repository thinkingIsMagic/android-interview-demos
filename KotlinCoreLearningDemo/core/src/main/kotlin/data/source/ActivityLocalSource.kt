package core.data.source

import core.domain.model.Activity
import core.domain.model.ActivityStatus
import core.domain.model.ActivityType
import core.domain.repository.ActivityRepository

/**
 * 活动本地数据源
 * 
 * 实现仓储接口
 * 
 * 实际工程中：
 * - 这里可能是 Room 数据库、网络 API
 * - 可以添加缓存逻辑
 * - 可以添加数据转换
 */
class ActivityLocalSource : ActivityRepository {
    
    /**
     * 模拟本地数据存储
     */
    private val storage = mutableMapOf<String, Activity>()
    
    init {
        // 初始化一些示例数据
        storage["A001"] = Activity(
            id = "A001",
            name = "满100减10",
            type = ActivityType.MoneyOff(100.0, 10.0),
            status = ActivityStatus.ACTIVE
        )
        storage["A002"] = Activity(
            id = "A002",
            name = "8折优惠",
            type = ActivityType.Discount(0.8, 50.0),
            status = ActivityStatus.ACTIVE
        )
        storage["A003"] = Activity(
            id = "A003",
            name = "限时秒杀",
            type = ActivityType.Seckill(100, System.currentTimeMillis()),
            status = ActivityStatus.ACTIVE
        )
        storage["A004"] = Activity(
            id = "A004",
            name = "全场免邮",
            type = ActivityType.FreeShipping,
            status = ActivityStatus.ACTIVE
        )
    }
    
    override fun getById(id: String): Activity? {
        return storage[id]
    }
    
    override fun getAll(): List<Activity> {
        return storage.values.toList()
    }
    
    override fun getByStatus(status: ActivityStatus): List<Activity> {
        return storage.values.filter { it.status == status }
    }
    
    override fun save(activity: Activity): Boolean {
        storage[activity.id] = activity
        return true
    }
}
