package com.example.androidcrashanalysis.navigation

/**
 * 应用路由定义
 * 使用单参数化路由 designPattern: scenario/{scenarioId}
 */
object Routes {
    const val HOME = "home"
    const val SCENARIO_DETAIL = "scenario/{scenarioId}"

    fun scenarioDetail(scenarioId: String) = "scenario/$scenarioId"
}
