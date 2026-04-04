package com.example.androidcrashanalysis.ui.anr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.AnrOrange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 场景：主线程 Sleep
 *
 * 【问题原理】
 * Thread.sleep() 会阻塞当前线程。
 * 在主线程调用 sleep(15000) 会阻塞 UI 线程 15 秒。
 * 系统检测到主线程无响应超过 5 秒，弹出 ANR 对话框。
 *
 * 【修复方案】
 * 使用 Dispatchers.IO 在子线程执行 sleep。
 * 注意：Handler.postDelayed 是安全的（不占用 CPU），但 Thread.sleep 不安全。
 */
@Composable
fun MainThreadSleepScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("anr_main_thread_sleep")!! }
    var fixEnabled by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }

    ScenarioScaffold(
        title = scenario.title,
        description = scenario.description,
        dangerLevel = scenario.dangerLevel,
        categoryColor = AnrOrange,
        explanationText = scenario.explanationText,
        investigationMethod = scenario.investigationMethod,
        fixDescription = scenario.fixDescription,
        fixEnabled = fixEnabled,
        onFixToggle = { fixEnabled = it },
        onBack = onBack,
        actionButtons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        running = true
                        if (fixEnabled) {
                            // 修复版本：在子线程执行 sleep，不阻塞主线程
                            CoroutineScope(Dispatchers.IO).launch {
                                // delay 是协程的 sleep，不会阻塞任何线程
                                delay(15000)
                                running = false
                            }
                        } else {
                            // 问题版本：在主线程执行 sleep → ANR
                            try {
                                Thread.sleep(15000) // ← 阻塞主线程 15 秒
                            } catch (e: InterruptedException) {
                                // ignore
                            }
                            running = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnrOrange,
                        disabledContainerColor = AnrOrange.copy(alpha = 0.3f)
                    ),
                    enabled = !running
                ) {
                    Text(
                        text = if (fixEnabled) {
                            "🔧 修复版本 - 子线程执行（安全）"
                        } else {
                            "⚠️ 触发 ANR（主线程 sleep 15s）"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                if (running) {
                    Text(
                        text = "⏳ 正在执行...",
                        style = androidx.compose.ui.text.TextStyle(
                            color = AnrOrange,
                            fontSize = 12.sp
                        )
                    )
                }

                if (!fixEnabled) {
                    Text(
                        text = "⚠️ 点击后约 5 秒出现 ANR 弹窗，期间请勿操作手机",
                        style = androidx.compose.ui.text.TextStyle(
                            color = AnrOrange.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ Dispatchers.IO 子线程执行，主线程完全不受影响",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF388E3C),
                            fontSize = 12.sp
                        )
                    )
                }

                // traces.txt 路径说明
                Text(
                    text = "排查命令: adb pull /data/anr/traces.txt .",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
            }
        }
    )
}
