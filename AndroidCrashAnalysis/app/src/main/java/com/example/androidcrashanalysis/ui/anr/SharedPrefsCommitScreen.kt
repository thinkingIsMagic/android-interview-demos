package com.example.androidcrashanalysis.ui.anr

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.AnrOrange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 场景：SharedPreferences commit 卡顿
 *
 * 【问题原理】
 * SharedPreferences.commit() 是同步写入，每次调用都会等待磁盘 IO 完成。
 * 循环 1000 次 commit() 可能导致 5-10 秒的阻塞 → ANR。
 *
 * 【修复方案】
 * 99% 场景使用 apply()（异步写入），只有必须知道写入结果时才用 commit()。
 */
@Composable
fun SharedPrefsCommitScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("anr_sp_commit") !! }
    var fixEnabled by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    val context = LocalContext.current

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
                        isRunning = true
                        progress = 0
                        status = "执行中..."

                        if (fixEnabled) {
                            // 修复版本：使用 apply（异步）
                            CoroutineScope(Dispatchers.IO).launch {
                                val sp = context.getSharedPreferences("test_sp", Context.MODE_PRIVATE)
                                repeat(1000) { i ->
                                    sp.edit()
                                        .putString("key_$i", "value_$i")
                                        .apply() // ✅ 异步，不阻塞
                                    progress = i + 1
                                    if (i % 100 == 0) delay(10) // 每100条让 UI 有机会刷新
                                }
                                status = "✅ 完成: 1000 次 apply()（异步，无 ANR）"
                                isRunning = false
                            }
                        } else {
                            // 问题版本：使用 commit（同步）
                            // 注意：这个会在主线程执行 1000 次同步写入
                            // 会明显卡顿，在低端设备上触发 ANR
                            try {
                                val sp = context.getSharedPreferences("test_sp", Context.MODE_PRIVATE)
                                repeat(1000) { i ->
                                    sp.edit()
                                        .putString("key_$i", "value_$i")
                                        .commit() // ❌ 同步等待写入完成
                                    progress = i + 1
                                }
                                status = "完成: 1000 次 commit()"
                            } catch (e: Exception) {
                                status = "异常: ${e.message}"
                            }
                            isRunning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnrOrange,
                        disabledContainerColor = AnrOrange.copy(alpha = 0.3f)
                    ),
                    enabled = !isRunning
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本 - apply() 异步写入" else "⚠️ 触发卡顿（1000 次 commit）",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isRunning) {
                    LinearProgressIndicator(
                        progress = { progress / 1000f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "进度: $progress / 1000",
                        style = androidx.compose.ui.text.TextStyle(
                            color = AnrOrange,
                            fontSize = 12.sp
                        )
                    )
                }

                if (status.isNotBlank()) {
                    Text(
                        text = status,
                        style = androidx.compose.ui.text.TextStyle(
                            color = when {
                                status.startsWith("✅") -> Color(0xFF388E3C)
                                status.startsWith("完成") -> Color(0xFF1565C0)
                                else -> AnrOrange
                            },
                            fontSize = 12.sp
                        )
                    )
                }

                if (!fixEnabled) {
                    Text(
                        text = "⚠️ 1000 次同步写入约需 3-10 秒，可能触发 ANR",
                        style = androidx.compose.ui.text.TextStyle(
                            color = AnrOrange.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ apply() 立即返回，后台线程写入，无阻塞",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF388E3C),
                            fontSize = 12.sp
                        )
                    )
                }

                Text(
                    text = "commit vs apply: 两者都保证写入成功，区别是同步/异步",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
            }
        }
    )
}
