/*
 * SamplingManager.kt
 *
 * 采样决策管理器
 *
 * 面试亮点：
 * 1. 采样是控制数据量的关键手段
 * 2. 随机采样 vs 哈希采样
 * 3. 简单高效的实现
 */
package com.trackapi.observability

import kotlin.random.Random

/**
 * 采样管理器
 *
 * 设计要点：
 * 1. 使用 Random.nextFloat() 生成 0~1 的随机数
 * 2. 简单高效，无额外内存开销
 * 3. 支持按类型分别设置采样率
 */
class SamplingManager {

    // 默认采样率配置
    private val defaultSamplingRate = 1.0f

    // 按类型的采样率覆盖
    private val typeSamplingRates = mutableMapOf<String, Float>()

    /**
     * 决策是否采样
     *
     * 面试亮点：
     * - 简单高效：一次随机比较
     * - 概率准确：长期运行符合采样率期望
     *
     * @param type 事件类型
     * @return true=采集, false=丢弃
     */
    fun shouldSample(type: String): Boolean {
        val rate = typeSamplingRates[type] ?: defaultSamplingRate
        return Random.nextFloat() < rate
    }

    /**
     * 设置特定类型的采样率
     */
    fun setSamplingRate(type: String, rate: Float) {
        require(rate in 0.0f..1.0f) { "采样率必须在 0.0 ~ 1.0 之间" }
        typeSamplingRates[type] = rate
    }

    /**
     * 清除自定义采样率配置
     */
    fun reset() {
        typeSamplingRates.clear()
    }
}
