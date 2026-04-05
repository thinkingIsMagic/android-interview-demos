package com.example.androidcrashanalysis.ui.oom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.common.MemoryInfoBar
import com.example.androidcrashanalysis.ui.common.getMemorySnapshot
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.OomRed

/**
 * 场景：未反注册 Listener
 *
 * 【问题原理】
 * 注册 BroadcastReceiver/传感器监听后，必须在 Activity 销毁时反注册。
 *
 * 【修复方案】
 * 使用 DisposableEffect，在 onDispose 中调用 unregisterReceiver
 */
@Composable
fun UnregisteredListenerScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("oom_unregistered_listener") !! }
    var fixEnabled by remember { mutableStateOf(false) }
    var memorySnapshot by remember { mutableStateOf(getMemorySnapshot()) }
    var broadcastCount by remember { mutableIntStateOf(0) }

    val context = LocalContext.current

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                broadcastCount++
            }
        }
    }

    // 修复版本：使用 DisposableEffect 管理生命周期
    if (fixEnabled) {
        DisposableEffect(Unit) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)

            onDispose {
                // ✅ 退出时反注册
                context.unregisterReceiver(receiver)
            }
        }
    } else {
        DisposableEffect(Unit) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)

            // ❌ 缺失 onDispose 中的 unregisterReceiver → 泄漏
            onDispose { /* 空实现：泄漏 */ }
        }
    }

    DisposableEffect(Unit) {
        val i = Intent(Intent.ACTION_BATTERY_CHANGED)
        context.sendBroadcast(i)
        onDispose { }
    }

    ScenarioScaffold(
        title = scenario.title,
        description = scenario.description,
        dangerLevel = scenario.dangerLevel,
        categoryColor = OomRed,
        explanationText = scenario.explanationText,
        investigationMethod = scenario.investigationMethod,
        fixDescription = scenario.fixDescription,
        fixEnabled = fixEnabled,
        onFixToggle = { fixEnabled = it },
        onBack = onBack,
        memoryInfo = {
            MemoryInfoBar(
                javaUsed = memorySnapshot.javaUsed,
                javaMax = memorySnapshot.javaMax,
                javaFree = memorySnapshot.javaFree,
                nativeAllocated = memorySnapshot.nativeAllocated,
                nativeSize = memorySnapshot.nativeSize
            )
        },
        actionButtons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_BATTERY_CHANGED)
                        repeat(100) { context.sendBroadcast(intent) }
                        memorySnapshot = getMemorySnapshot()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OomRed,
                        disabledContainerColor = OomRed.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本 - 已正确注册/反注册" else "⚠️ 发送广播（累积引用）",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { memorySnapshot = getMemorySnapshot() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📊 查看当前内存")
                }

                OutlinedButton(
                    onClick = {
                        System.gc()
                        System.runFinalization()
                        memorySnapshot = getMemorySnapshot()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🗑️ 手动 GC")
                }

                Text(
                    text = "已接收广播次数: $broadcastCount",
                    style = androidx.compose.ui.text.TextStyle(
                        color = if (fixEnabled) Color(0xFF388E3C) else OomRed.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )

                if (!fixEnabled) {
                    Text(
                        text = "提示：退出页面后 GC，Activity 仍被 Receiver 引用 → 泄漏",
                        style = androidx.compose.ui.text.TextStyle(
                            color = OomRed.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ DisposableEffect.onDispose 中已调用 unregisterReceiver",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF388E3C),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    )
}
