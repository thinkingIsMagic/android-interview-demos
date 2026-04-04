package com.example.androidcrashanalysis.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.model.Scenario
import com.example.androidcrashanalysis.model.ScenarioCategory
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.theme.AnrOrange
import com.example.androidcrashanalysis.ui.theme.AnrOrangeBackground
import com.example.androidcrashanalysis.ui.theme.NativePurple
import com.example.androidcrashanalysis.ui.theme.NativePurpleBackground
import com.example.androidcrashanalysis.ui.theme.OomRed
import com.example.androidcrashanalysis.ui.theme.OomRedBackground

/**
 * 首页
 * 展示三个分类（OOM / ANR / Native Crash）的场景列表
 */
@Composable
fun HomeScreen(
    onScenarioClick: (String) -> Unit
) {
    val categorizedScenarios = ScenarioRegistry.getScenariosByCategory()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 标题
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🛡️ Android 稳定性问题实战",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "OOM · ANR · Native Crash",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击任意场景进入 → 触发问题 → 学习排查",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // 按分类渲染
        categorizedScenarios.forEach { (category, scenarios) ->
            item {
                CategoryHeader(category = category)
            }

            items(scenarios) { scenario ->
                ScenarioCard(
                    scenario = scenario,
                    onClick = { onScenarioClick(scenario.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 底部说明
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 使用提示",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Debug 构建下，LeakCanary 会自动检测 Activity 泄漏并通知\n" +
                                "• ANR/Native Crash 触发后请查看 logcat 和 traces.txt\n" +
                                "• 每个场景都有\"修复版本\"开关，可对比正确实现",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

/**
 * 分类标题
 */
@Composable
private fun CategoryHeader(category: ScenarioCategory) {
    val (icon, bgColor, textColor) = when (category) {
        ScenarioCategory.OOM -> Triple("💥", OomRedBackground, OomRed)
        ScenarioCategory.ANR -> Triple("⏳", AnrOrangeBackground, AnrOrange)
        ScenarioCategory.NATIVE_CRASH -> Triple("⚡", NativePurpleBackground, NativePurple)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "$icon ${category.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            val count = when (category) {
                ScenarioCategory.OOM -> 5
                ScenarioCategory.ANR -> 4
                ScenarioCategory.NATIVE_CRASH -> 3
            }
            Text(
                text = when (category) {
                    ScenarioCategory.OOM -> "$count 个场景 - 内存泄漏导致 OOM"
                    ScenarioCategory.ANR -> "$count 个场景 - 主线程阻塞导致无响应"
                    ScenarioCategory.NATIVE_CRASH -> "$count 个场景 - C++ 层崩溃"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 场景卡片
 */
@Composable
private fun ScenarioCard(
    scenario: Scenario,
    onClick: () -> Unit
) {
    val categoryColor = scenario.category.color

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scenario.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 危险等级小圆点
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (scenario.dangerLevel) {
                                    5 -> Color(0xFFD32F2F)
                                    4 -> Color(0xFFD84315)
                                    3 -> Color(0xFFF57C00)
                                    else -> Color(0xFF388E3C)
                                }
                            )
                    )
                }
                Text(
                    text = scenario.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 右侧：箭头
            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium,
                color = categoryColor.copy(alpha = 0.5f)
            )
        }
    }
}
