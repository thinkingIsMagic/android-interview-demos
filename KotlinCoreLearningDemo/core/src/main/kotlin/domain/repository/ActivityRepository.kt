package core.domain.repository

import core.domain.model.Activity

/**
 * 活动仓储接口
 * 
 * 接口隔离原则（ISP）：
 * - 定义领域层需要的抽象接口
 * - 数据层实现此接口
 * - 上层依赖抽象，不依赖具体实现
 * 
 * Kotlin 技巧：
 * - 使用 by 委托实现接口（见 data/repository/）
 * - 可空返回使用 ?
 */
interface ActivityRepository {
    
    /**
     * 根据 ID 获取活动
     * @param id 活动 ID
     * @return 活动实体，不存在返回 null
     */
    fun getById(id: String): Activity?
    
    /**
     * 获取所有活动
     * @return 活动列表
     */
    fun getAll(): List<Activity>
    
    /**
     * 获取指定状态的活动
     * @param status 活动状态
     * @return 符合条件的活动列表
     */
    fun getByStatus(status: core.domain.model.ActivityStatus): List<Activity>
    
    /**
     * 保存活动
     * @param activity 活动实体
     * @return 是否保存成功
     */
    fun save(activity: Activity): Boolean
}
