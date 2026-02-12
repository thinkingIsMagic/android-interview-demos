package com.example.android_demo_ceiling_container_1.data

import com.example.android_demo_ceiling_container_1.data.mock.MockConfigDataSource
import com.example.android_demo_ceiling_container_1.data.model.BottomConfig
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.data.parser.BottomConfigParser
import com.example.android_demo_ceiling_container_1.util.Logger
import com.example.android_demo_ceiling_container_1.util.Result

/**
 * 配置仓库
 *
 * 负责配置数据的获取、解析和缓存
 */
class BottomConfigRepository(
    private val parser: BottomConfigParser = BottomConfigParser()
) {

    private val logger = Logger.getInstance("BottomConfigRepository")

    // 内存缓存
    private var cachedConfig: BottomConfig? = null

    /**
     * 获取底部配置
     *
     * @param useMock 是否使用 Mock 数据
     * @return 解析后的配置结果
     */
    fun getConfig(useMock: Boolean = true): Result<BottomConfig> {
        // 返回缓存
        cachedConfig?.let {
            logger.d("返回缓存的配置", "BottomConfigRepository")
            return Result.success(it)
        }

        val jsonString = if (useMock) {
            logger.d("使用 Mock 配置数据", "BottomConfigRepository")
            MockConfigDataSource.getDefaultConfig()
        } else {
            // TODO: 从网络获取
            logger.w("网络配置未实现，使用 Mock", "BottomConfigRepository")
            MockConfigDataSource.getDefaultConfig()
        }

        return parseAndCache(jsonString)
    }

    /**
     * 获取指定场景的配置
     */
    fun getConfigForScene(scene: String): Result<List<Component>> {
        return getConfig().map { config ->
            config.getSortedComponents().filter { it.scene == scene }
        }
    }

    /**
     * 解析并缓存配置
     */
    private fun parseAndCache(jsonString: String): Result<BottomConfig> {
        val result = parser.parse(jsonString)
        result.onSuccess { config ->
            cachedConfig = config
            logger.d("配置已缓存: version=${config.version}, components=${config.components.size}", "BottomConfigRepository")
        }
        return result
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cachedConfig = null
        logger.d("配置缓存已清除", "BottomConfigRepository")
    }

    /**
     * 刷新配置
     */
    fun refreshConfig(): Result<BottomConfig> {
        clearCache()
        return getConfig()
    }

    companion object {
        @Volatile
        private var instance: BottomConfigRepository? = null

        fun getInstance(): BottomConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: BottomConfigRepository().also { instance = it }
            }
        }
    }
}
