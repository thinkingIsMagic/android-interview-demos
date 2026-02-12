package com.example.android_demo_ceiling_container_1.data.mock

import com.example.android_demo_ceiling_container_1.data.parser.BottomConfigParser

/**
 * Mock 配置数据源
 *
 * 模拟服务端下发的 JSON 配置
 */
object MockConfigDataSource {

    private val parser = BottomConfigParser()

    /**
     * 获取默认 Mock 配置
     */
    fun getDefaultConfig(): String {
        return parser.generateMockJson()
    }

    /**
     * 获取包含无效组件的 Mock 配置（测试兜底）
     */
    fun getInvalidConfig(): String {
        return """
        {
            "version": "1.0",
            "enable": true,
            "components": [
                {
                    "id": "invalid_component",
                    "type": "unknown_type",
                    "priority": 50,
                    "enable": false,
                    "config": null
                }
            ]
        }
        """.trimIndent()
    }

    /**
     * 获取空组件列表的配置
     */
    fun getEmptyComponentsConfig(): String {
        return """
        {
            "version": "1.0",
            "enable": true,
            "components": []
        }
        """.trimIndent()
    }

    /**
     * 获取停用的配置
     */
    fun getDisabledConfig(): String {
        return """
        {
            "version": "1.0",
            "enable": false,
            "components": [
                {
                    "id": "coupon_001",
                    "type": "coupon",
                    "priority": 100,
                    "scene": "product_detail",
                    "visible": true,
                    "enable": true,
                    "config": {
                        "title": "优惠券",
                        "amount": 30
                    }
                }
            ]
        }
        """.trimIndent()
    }

    /**
     * 获取滚动阈值配置的 Mock
     */
    fun getScrollThresholdConfig(): String {
        return """
        {
            "version": "1.0",
            "enable": true,
            "components": [
                {
                    "id": "scroll_coupon",
                    "type": "coupon",
                    "priority": 100,
                    "scene": "product_detail",
                    "visible": true,
                    "enable": true,
                    "config": {
                        "title": "滚动显示优惠券",
                        "amount": 100
                    },
                    "visibility": {
                        "scrollThreshold": 150,
                        "scrollDirection": "down",
                        "minDuration": 500
                    }
                }
            ]
        }
        """.trimIndent()
    }
}
