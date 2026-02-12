package com.example.android_demo_ceiling_container_1.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_demo_ceiling_container_1.data.model.Component
import com.example.android_demo_ceiling_container_1.performance.PerformanceStats
import com.example.android_demo_ceiling_container_1.ui.bottom.BottomHost
import com.example.android_demo_ceiling_container_1.ui.theme.AndroidDemoCeilingContainer1Theme

class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidDemoCeilingContainer1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DemoScreen()
                }
            }
        }
    }
}

@Composable
fun DemoScreen(
    viewModel: DemoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val bottomState by viewModel.bottomState.collectAsState()

    var scrollOffset by remember { mutableFloatStateOf(0f) }
    var isAtTop by remember { mutableIntStateOf(1) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部演示内容区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column {
                // 标题
                DemoTitle()

                Spacer(modifier = Modifier.height(12.dp))

                // 配置状态卡片
                ConfigStatusCard(
                    state = state,
                    bottomState = bottomState
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 滚动控制卡片
                ScrollControlCard(
                    scrollOffset = scrollOffset,
                    isAtTop = isAtTop == 1,
                    onScrollChange = { offset, top ->
                        scrollOffset = offset
                        isAtTop = if (top) 1 else 0
                        viewModel.updateScroll(offset.toInt(), top)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 组件列表卡片
                ComponentListCard(
                    components = state.config?.components ?: emptyList(),
                    selectedId = bottomState.selectedComponent?.id,
                    onToggleVisibility = { componentId ->
                        viewModel.toggleComponentVisibility(componentId)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 性能统计卡片
                PerformanceCard(stats = viewModel.getPerformanceStats())

                Spacer(modifier = Modifier.height(12.dp))

                // 裁决结果卡片
                ExclusiveResultCard(result = bottomState.exclusiveResult)

                Spacer(modifier = Modifier.height(12.dp))

                // 开关控制卡片
                FeatureToggleCard(viewModel = viewModel)

                Spacer(modifier = Modifier.height(80.dp)) // 为底部 BottomHost 留空间
            }
        }

        // 底部 BottomHost
        BottomHost(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        )
    }
}

@Composable
fun DemoTitle() {
    Column {
        Text(
            text = "Config-Driven Bottom Host",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "面试级 Android 项目演示",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ConfigStatusCard(
    state: DemoState,
    bottomState: com.example.android_demo_ceiling_container_1.ui.bottom.BottomViewState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("配置状态", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = state.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("加载中...")
                }
            }

            if (state.errorMessage != null) {
                Text("错误: ${state.errorMessage}", color = Color.Red)
            }

            AnimatedVisibility(visible = !state.isLoading && state.errorMessage == null) {
                Column {
                    Text("版本: ${state.config?.version ?: "未加载"}")
                    Text("组件数: ${state.config?.components?.size ?: 0}")
                    Text("启用: ${state.config?.enable ?: false}")
                    Text("选中组件: ${bottomState.selectedComponent?.id ?: "无"}")
                    Text("组件类型: ${bottomState.selectedComponent?.type?.value ?: "-"}")
                }
            }
        }
    }
}

@Composable
fun ScrollControlCard(
    scrollOffset: Float,
    isAtTop: Boolean,
    onScrollChange: (Float, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("滚动控制", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text("滚动偏移: ${scrollOffset.toInt()} px")

            Slider(
                value = scrollOffset,
                onValueChange = { onScrollChange(it, it <= 0f) },
                valueRange = 0f..500f,
                steps = 9
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("是否在顶部: ")
                    Switch(
                        checked = isAtTop,
                        onCheckedChange = { onScrollChange(scrollOffset, it) }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onScrollChange((scrollOffset + 100f).coerceAtMost(500f), false) }) {
                        Text("+100")
                    }
                    Button(onClick = { onScrollChange(0f, true) }) {
                        Text("重置")
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentListCard(
    components: List<Component>,
    selectedId: String?,
    onToggleVisibility: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("组件列表", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (components.isEmpty()) {
                Text("暂无组件", color = Color.Gray)
            }

            components.forEach { component ->
                val isSelected = component.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = component.id,
                            color = if (isSelected) Color(0xFF2196F3) else Color.Black,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${component.type.value} | 优先级: ${component.priority} | 可见: ${component.visible}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Checkbox(
                        checked = component.visible,
                        onCheckedChange = { onToggleVisibility(component.id) }
                    )

                    if (isSelected) {
                        Text("已选中", color = Color(0xFF2196F3), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceCard(stats: PerformanceStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("性能统计", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Text("createViewCount: ${stats.createViewCount}")
            Text("composeCount: ${stats.composeCount}")
            Text("lastRenderTimeMs: ${stats.lastRenderTimeMs}")
            Text("totalRenderTimeMs: ${stats.totalRenderTimeMs}")
        }
    }
}

@Composable
fun ExclusiveResultCard(
    result: com.example.android_demo_ceiling_container_1.domain.model.ExclusiveResult?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("互斥裁决结果", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (result == null) {
                Text("无裁决结果", color = Color.Gray)
            } else {
                Text("选中: ${result.selectedComponent?.id ?: "无"}")
                Text("拒绝: ${result.rejectedComponents.size} 个")
                Text("原因: ${result.reason}", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun FeatureToggleCard(viewModel: DemoViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("功能开关", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("调试模式")
                Switch(
                    checked = true,
                    onCheckedChange = { /* 调试开关 */ }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.refreshConfig() }) {
                    Text("刷新配置")
                }
                Button(onClick = {
                    // 打印统计
                }) {
                    Text("打印统计")
                }
            }
        }
    }
}
