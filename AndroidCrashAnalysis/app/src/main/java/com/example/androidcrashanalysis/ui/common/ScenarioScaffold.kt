package com.example.androidcrashanalysis.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 场景页面骨架
 *
 * 所有 12 个场景页面都使用这个统一的骨架布局：
 * - 顶部导航栏（标题 + 危险等级标签 + 返回按钮）
 * - 修复版本开关
 * - 操作按钮区（可自定义）
 * - 内存信息区（OOM 场景专用，可选）
 * - 代码展示区（Native 场景专用，可选）
 * - 底部说明卡片
 *
 * @param title 场景标题
 * @param description 场景描述
 * @param dangerLevel 危险等级 1-5
 * @param categoryColor 分类主色调
 * @param explanationText 问题原理说明
 * @param investigationMethod 排查方法
 * @param fixDescription 修复方案说明
 * @param fixEnabled 当前修复开关状态
 * @param onFixToggle 修复开关回调
 * @param onBack 返回按钮回调
 * @param memoryInfo 内存信息槽（OOM 场景使用）
 * @param codeDisplay 代码展示槽（Native 场景使用）
 * @param actionButtons 操作按钮槽（触发按钮等）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioScaffold(
    title: String,
    description: String,
    dangerLevel: Int,
    categoryColor: Color,
    explanationText: String,
    investigationMethod: String,
    fixDescription: String,
    fixEnabled: Boolean,
    onFixToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    memoryInfo: (@Composable () -> Unit)? = null,
    codeDisplay: (@Composable () -> Unit)? = null,
    actionButtons: @Composable () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = categoryColor.copy(alpha = 0.1f),
                    titleContentColor = categoryColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 场景描述 + 危险等级标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                DangerLevelTag(
                    level = dangerLevel,
                    color = categoryColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // 修复版本开关卡片
            FixToggleCard(
                fixEnabled = fixEnabled,
                onFixToggle = onFixToggle,
                categoryColor = categoryColor
            )

            // 操作按钮区
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本（问题已修复）" else "⚠️ 问题版本（可触发问题）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (fixEnabled) Color(0xFF388E3C) else categoryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    actionButtons()
                }
            }

            // 内存信息槽（OOM 场景）
            memoryInfo?.invoke()

            // 代码展示槽（Native 场景）
            codeDisplay?.invoke()

            // 修复方案说明（当开关开启时显示）
            if (fixDescription.isNotBlank()) {
                FixDescriptionCard(
                    fixDescription = fixDescription,
                    showFix = fixEnabled,
                    categoryColor = categoryColor
                )
            }

            // 底部原理说明卡片
            ExplanationCard(
                explanationText = explanationText,
                investigationMethod = investigationMethod
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 修复开关卡片
 */
@Composable
private fun FixToggleCard(
    fixEnabled: Boolean,
    onFixToggle: (Boolean) -> Unit,
    categoryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (fixEnabled)
                Color(0xFF388E3C).copy(alpha = 0.1f)
            else
                categoryColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔄 修复版本",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (fixEnabled) "当前为修复后代码，不会触发问题" else "当前为原始问题代码，可触发问题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = fixEnabled,
                onCheckedChange = onFixToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF388E3C),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = categoryColor.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * 修复方案说明卡片（修复开关开启时显示）
 */
@Composable
private fun FixDescriptionCard(
    fixDescription: String,
    showFix: Boolean,
    categoryColor: Color
) {
    if (showFix) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF388E3C).copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🛠️ 修复方案",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                )
                Text(
                    text = fixDescription,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
