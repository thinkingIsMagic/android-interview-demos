package core.data.repository

import core.domain.model.Activity
import core.domain.model.ActivityStatus
import core.domain.model.ActivityType
import core.domain.repository.ActivityRepository

/**
 * 活动仓储实现
 * 
 * 使用委托模式实现接口（Kotlin 原生支持）
 * 
 * 好处：
 * - 不用手动写代理代码
 * - 可以组合多个行为（日志、缓存等）
 */
class ActivityRepositoryImpl(
    // 可以注入其他依赖（如缓存、数据库）
    private val localSource: core.data.source.ActivityLocalSource = core.data.source.ActivityLocalSource()
) : ActivityRepository by localSource {
    
    // 可以添加额外方法或覆盖方法
    
    /**
     * 获取指定类型的活动
     */
    fun getByType(type: ActivityType): List<Activity> {
        return getAll().filter { it.type::class == type::class }
    }
}
