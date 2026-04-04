package com.example.androidcrashanalysis.model

/**
 * 场景数据类
 * 包含每个稳定性问题场景的元数据
 *
 * @param id 唯一标识符，用于路由跳转
 * @param title 场景标题（中文）
 * @param description 简短描述
 * @param category 所属分类
 * @param dangerLevel 危险等级 1-5
 * @param explanationText 问题原理说明（中文）
 * @param investigationMethod 排查方法（命令行/工具操作步骤）
 * @param fixDescription 修复方案说明
 */
data class Scenario(
    val id: String,
    val title: String,
    val description: String,
    val category: ScenarioCategory,
    val dangerLevel: Int,          // 1-5, 5 为最危险
    val explanationText: String,
    val investigationMethod: String,
    val fixDescription: String
)
