package com.example.android_demo_ceiling_container_1.data.parser

import com.example.android_demo_ceiling_container_1.data.model.BottomConfig
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.data.model.ComponentConfig
import com.example.android_demo_ceiling_container_1.data.model.ComponentType
import com.example.android_demo_ceiling_container_1.data.model.TrackerConfig
import com.example.android_demo_ceiling_container_1.data.model.VisibilityConfig
import com.example.android_demo_ceiling_container_1.util.Logger
import com.example.android_demo_ceiling_container_1.util.Result

/**
 * JSON 解析器：服务端 JSON 配置 -> 领域模型
 *
 * 使用 Gson 自动解析，支持多态
 */
class BottomConfigParser {

    private val logger = Logger.getInstance("BottomConfigParser")
    private val gson = ComponentConfig.createGson()

    fun parse(jsonString: String): Result<BottomConfig> {
        return try {
            logger.d("开始解析 BottomConfig JSON", "BottomConfigParser")
            val startTime = System.currentTimeMillis()

            val config = gson.fromJson(jsonString, BottomConfigDto::class.java).toDomain()

            val duration = System.currentTimeMillis() - startTime
            logger.performance("BottomConfigParser", "parse", duration)

            logger.d("解析成功: version=${config.version}, componentsCount=${config.components.size}", "BottomConfigParser")
            Result.success(config)
        } catch (e: Exception) {
            logger.e("JSON 解析失败: ${e.message}", e, "BottomConfigParser")
            Result.error("JSON 解析失败: ${e.message}", e)
        }
    }

    fun generateMockJson(): String {
        return """
        {
            "version": "1.0",
            "enable": true,
            "components": [
                {
                    "id": "coupon_001",
                    "type": "coupon",
                    "priority": 100,
                    "scene": "product_detail",
                    "visible": true,
                    "enable": true,
                    "config": {
                        "title": "限时优惠券",
                        "amount": 50,
                        "threshold": 200,
                        "buttonText": "领取",
                        "bgColor": "#FF5722",
                        "expireTime": "2026-02-28 23:59:59"
                    },
                    "tracker": {
                        "exposureEvent": "coupon_exposure",
                        "clickEvent": "coupon_click"
                    },
                    "visibility": {
                        "scrollThreshold": 200,
                        "scrollDirection": "down",
                        "minDuration": 1000,
                        "pageStates": ["foreground"]
                    }
                },
                {
                    "id": "banner_001",
                    "type": "banner",
                    "priority": 80,
                    "scene": "product_detail",
                    "visible": true,
                    "enable": true,
                    "config": {
                        "imageUrl": "https://example.com/banner.png",
                        "title": "新品首发",
                        "link": "https://example.com/activity",
                        "height": 120,
                        "cornerRadius": 8
                    },
                    "tracker": {
                        "exposureEvent": "banner_exposure",
                        "clickEvent": "banner_click"
                    }
                },
                {
                    "id": "float_001",
                    "type": "floating_widget",
                    "priority": 60,
                    "scene": "product_detail",
                    "visible": true,
                    "enable": true,
                    "config": {
                        "iconUrl": "https://example.com/service.png",
                        "text": "客服",
                        "position": "right_bottom",
                        "actions": ["chat", "phone"]
                    },
                    "tracker": {
                        "exposureEvent": "float_exposure",
                        "clickEvent": "float_click"
                    }
                }
            ]
        }
        """.trimIndent()
    }
}

data class BottomConfigDto(
    val version: String = "1.0",
    val enable: Boolean = true,
    val components: List<ComponentDto> = emptyList()
) {
    fun toDomain(): BottomConfig {
        return BottomConfig(
            version = version,
            enable = enable,
            components = components.mapNotNull { it.toDomain() }
        )
    }
}

data class ComponentDto(
    val id: String = "",
    val type: String = "unknown",
    val priority: Int = 0,
    val scene: String = "default",
    val visible: Boolean = true,
    val enable: Boolean = true,
    val config: Map<String, Any?>? = null,
    val tracker: TrackerConfigDto? = null,
    val visibility: VisibilityConfigDto? = null
) {
    fun toDomain(): Component? {
        val componentType = ComponentType.fromValue(type)

        val componentConfig = try {
            val configJson = gson.toJsonTree(config).toString()
            if (configJson == "null") {
                ComponentConfig.EmptyConfig
            } else {
                gson.fromJson(configJson, ComponentConfig::class.java)
            }
        } catch (e: Exception) {
            ComponentConfig.EmptyConfig
        }

        return Component(
            id = id.ifBlank { "component_${System.currentTimeMillis()}" },
            type = componentType,
            priority = priority,
            scene = scene,
            visible = visible,
            enable = enable,
            config = componentConfig,
            tracker = tracker?.toDomain() ?: TrackerConfig(),
            visibility = visibility?.toDomain() ?: VisibilityConfig()
        )
    }
}

data class TrackerConfigDto(
    val exposureEvent: String = "",
    val clickEvent: String = ""
) {
    fun toDomain(): TrackerConfig {
        return TrackerConfig(
            exposureEvent = exposureEvent,
            clickEvent = clickEvent
        )
    }
}

data class VisibilityConfigDto(
    val scrollThreshold: Int = 0,
    val scrollDirection: String = "down",
    val minDuration: Long = 0,
    val pageStates: List<String> = emptyList()
) {
    fun toDomain(): VisibilityConfig {
        return VisibilityConfig(
            scrollThreshold = scrollThreshold,
            scrollDirection = scrollDirection,
            minDuration = minDuration,
            pageStates = pageStates
        )
    }
}
