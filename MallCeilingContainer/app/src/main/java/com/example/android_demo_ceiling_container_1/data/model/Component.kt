package com.example.android_demo_ceiling_container_1.data.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

/**
 * 组件追踪配置
 */
data class TrackerConfig(
    val exposureEvent: String = "",
    val clickEvent: String = ""
)

/**
 * 组件可见性策略配置
 */
data class VisibilityConfig(
    val scrollThreshold: Int = 0,
    val scrollDirection: String = "down",
    val minDuration: Long = 0,
    val pageStates: List<String> = emptyList()
)

/**
 * 单个底部组件的数据模型
 */
data class Component(
    val id: String,
    val type: ComponentType,
    val priority: Int,
    val scene: String,
    val visible: Boolean = true,
    val enable: Boolean = true,
    val config: ComponentConfig = ComponentConfig.EmptyConfig,
    val tracker: TrackerConfig = TrackerConfig(),
    val visibility: VisibilityConfig = VisibilityConfig()
) {
    fun isValid(): Boolean {
        return id.isNotBlank() && type != ComponentType.UNKNOWN && enable
    }
}

/**
 * 组件特定配置（泛型适配）
 * 使用 @JsonAdapter 配合 GsonTypeAdapter 实现自动多态解析
 */
@JsonAdapter(ComponentConfig.GsonTypeAdapter::class)
sealed class ComponentConfig {
    data class CouponConfig(
        val title: String = "",
        val amount: Int = 0,
        val threshold: Int = 0,
        val buttonText: String = "领取",
        val bgColor: String = "#FF5722",
        val expireTime: String = ""
    ) : ComponentConfig()

    data class BannerConfig(
        val imageUrl: String = "",
        val title: String = "",
        val link: String = "",
        val height: Int = 120,
        val cornerRadius: Int = 8
    ) : ComponentConfig()

    data class FloatingWidgetConfig(
        val iconUrl: String = "",
        val text: String = "",
        val position: String = "right_bottom",
        val actions: List<String> = emptyList()
    ) : ComponentConfig()

    data object EmptyConfig : ComponentConfig()

    class GsonTypeAdapter : JsonSerializer<ComponentConfig>, JsonDeserializer<ComponentConfig> {

        override fun serialize(
            src: ComponentConfig?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            if (src == null) return JsonPrimitive("")
            val type = when (src) {
                is CouponConfig -> "coupon"
                is BannerConfig -> "banner"
                is FloatingWidgetConfig -> "floating_widget"
                is EmptyConfig -> "unknown"
            }
            val json = context?.serialize(src) ?: JsonPrimitive("")
            val result = json.asJsonObject
            result.addProperty("type", type)
            return result
        }

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): ComponentConfig {
            if (json == null || !json.isJsonObject) return EmptyConfig

            val jsonObject = json.asJsonObject
            val type = jsonObject.get("type")?.asString ?: "unknown"

            return when (type) {
                "coupon" -> context?.deserialize(json, CouponConfig::class.java) ?: EmptyConfig
                "banner" -> context?.deserialize(json, BannerConfig::class.java) ?: EmptyConfig
                "floating_widget" -> context?.deserialize(json, FloatingWidgetConfig::class.java) ?: EmptyConfig
                else -> EmptyConfig
            }
        }
    }

    companion object {
        fun createGson(): Gson {
            return GsonBuilder()
                .registerTypeAdapter(ComponentConfig::class.java, GsonTypeAdapter())
                .create()
        }
    }
}
