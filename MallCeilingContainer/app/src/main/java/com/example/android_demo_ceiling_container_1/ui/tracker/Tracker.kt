package com.example.android_demo_ceiling_container_1.ui.tracker

import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.util.Logger

/**
 * 埋点统一入口
 *
 * 负责曝光/点击事件的统一处理
 */
class Tracker {

    private val logger = Logger.getInstance("Tracker")

    /**
     * 曝光事件
     */
    fun expose(component: Component) {
        val eventName = component.tracker.exposureEvent.ifBlank {
            "${component.type.value}_exposure"
        }

        val params = buildMap {
            put("component_id", component.id)
            put("component_type", component.type.value)
            put("priority", component.priority)
            put("scene", component.scene)
            put("timestamp", System.currentTimeMillis())
        }

        logger.event(eventName, params)
        logger.d("曝光: ${component.id}, event=$eventName", "Tracker")
    }

    /**
     * 点击事件
     */
    fun click(component: Component) {
        val eventName = component.tracker.clickEvent.ifBlank {
            "${component.type.value}_click"
        }

        val params = buildMap {
            put("component_id", component.id)
            put("component_type", component.type.value)
            put("priority", component.priority)
            put("scene", component.scene)
            put("timestamp", System.currentTimeMillis())
        }

        logger.event(eventName, params)
        logger.d("点击: ${component.id}, event=$eventName", "Tracker")
    }

    /**
     * 自定义事件
     */
    fun track(eventName: String, params: Map<String, Any?>) {
        logger.event(eventName, params)
        logger.d("自定义事件: $eventName, params=$params", "Tracker")
    }

    companion object {
        @Volatile
        private var instance: Tracker? = null

        fun getInstance(): Tracker {
            return instance ?: synchronized(this) {
                instance ?: Tracker().also { instance = it }
            }
        }
    }
}
