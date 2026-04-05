package com.example.androidcrashanalysis.ui.anr

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 场景：主线程大文件 IO
 *
 * 【问题原理】
 * 在主线程同步读写大文件会阻塞 UI 线程。
 * 100MB 文件读取可能需要 1-10 秒 → 触发 ANR。
 *
 * 【修复方案】
 * 使用 Dispatchers.IO 在子线程执行文件操作。
 *
 * 【触发核心逻辑】
 * 问题版本: File("xxx.bin").readBytes()
 *   → 在当前线程（主线程）执行同步 IO
 *   → 50MB 文件在低端设备上可能需要 3-10 秒
 *   → 阻塞期间主线程无法响应 → ANR
 *
 * 修复版本: CoroutineScope(Dispatchers.IO).launch {
 *              val data = withContext(Dispatchers.IO) { file.readBytes() }
 *            }
 *   → IO 操作在 Dispatchers.IO 线程池执行
 *   → 主线程继续处理 UI 事件，完全无感知
 *
 * 【为什么 Dispatchers.IO 适合 IO】
 * IO 操作特点是等待时间长、CPU 占用低。
 * Dispatchers.IO 底层使用共享线程池，并发数 ≈ CPU核心数 × 2，
 * 适合处理大量并发的网络/磁盘 IO。
 */
@Composable
fun LargeFileIoScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("anr_large_file_io") !! }
    var fixEnabled by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0) }

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
                // 创建测试文件
                OutlinedButton(
                    onClick = {
                        createTestFile(context)
                        status = "✅ 测试文件已创建（50MB）"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📄 创建测试文件（50MB）")
                }

                Button(
                    onClick = {
                        if (fixEnabled) {
                            status = "读取中..."
                            // 修复版本：子线程 IO
                            CoroutineScope(Dispatchers.IO).launch {
                                val file = File(context.filesDir, "test_file.bin")
                                if (file.exists()) {
                                    val data = withContext(Dispatchers.IO) {
                                        file.readBytes()
                                    }
                                    status = "✅ 读取完成: ${data.size / 1024 / 1024}MB"
                                } else {
                                    status = "⚠️ 请先创建测试文件"
                                }
                            }
                        } else {
                            // 问题版本：主线程 IO → ANR
                            status = "读取中..."
                            val file = File(context.filesDir, "test_file.bin")
                            if (file.exists()) {
                                try {
                                    val data = file.readBytes() // ← 阻塞主线程
                                    status = "读取完成: ${data.size / 1024 / 1024}MB"
                                } catch (e: Exception) {
                                    status = "读取失败: ${e.message}"
                                }
                            } else {
                                status = "⚠️ 请先创建测试文件"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnrOrange,
                        disabledContainerColor = AnrOrange.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本 - 子线程 IO（安全）" else "⚠️ 触发 ANR（主线程读大文件）",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (status.isNotBlank()) {
                    Text(
                        text = status,
                        style = androidx.compose.ui.text.TextStyle(
                            color = if (status.startsWith("✅")) Color(0xFF388E3C) else AnrOrange,
                            fontSize = 12.sp
                        )
                    )
                }

                if (!fixEnabled) {
                    Text(
                        text = "⚠️ 50MB 文件在低端设备上可能触发 ANR",
                        style = androidx.compose.ui.text.TextStyle(
                            color = AnrOrange.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ Dispatchers.IO 子线程执行，主线程完全无感知",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF388E3C),
                            fontSize = 12.sp
                        )
                    )
                }

                Text(
                    text = "建议: 在旋转屏幕后立即读取大文件更容易复现",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
            }
        }
    )
}

/** 创建 50MB 测试文件 */
private fun createTestFile(context: Context) {
    val file = File(context.filesDir, "test_file.bin")
    if (!file.exists()) {
        // 快速创建稀疏文件（只占用逻辑大小）
        file.writeBytes(ByteArray(50 * 1024 * 1024))
    }
}
