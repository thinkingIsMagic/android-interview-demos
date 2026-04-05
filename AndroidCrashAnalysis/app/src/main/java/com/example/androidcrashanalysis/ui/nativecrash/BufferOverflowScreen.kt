package com.example.androidcrashanalysis.ui.nativecrash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.jni.NativeCrashBridge
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.common.CodeDisplayCard
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.NativePurple

/**
 * 场景：Native 缓冲区溢出
 *
 * 【问题】
 * memcpy 写入超出栈缓冲区边界，触发 SIGABRT/SIGSEGV
 *
 * 【触发核心逻辑 — JNI 调用】
 * NativeCrashBridge.triggerBufferOverflow() → JNI → C++ 代码：
 *     char buffer[10];                        // 栈上 10 字节
 *     memcpy(buffer, src, strlen(src));        // src > 10 字节 → 栈溢出
 *
 * 【后果分析】
 * - 覆盖 buffer 之后的栈内存：saved PC、registers 等
 * - 破坏栈帧 → return address 被覆盖 → SIGABRT（Stack Canary 检测到）
 * - 或 SIGSEGV（访问到非法地址）
 *
 * 【日志特征】
 * - FATAL SIGNAL 6 (SIGABRT) 或 11 (SIGSEGV)
 * - fault addr 在栈地址范围内（0x7fff_xxxx）
 * - backtrace 中能看到 memcpy 调用
 */
@Composable
fun BufferOverflowScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("native_buffer_overflow") !! }
    var fixEnabled by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    ScenarioScaffold(
        title = scenario.title,
        description = scenario.description,
        dangerLevel = scenario.dangerLevel,
        categoryColor = NativePurple,
        explanationText = scenario.explanationText,
        investigationMethod = scenario.investigationMethod,
        fixDescription = scenario.fixDescription,
        fixEnabled = fixEnabled,
        onFixToggle = { fixEnabled = it },
        onBack = onBack,
        codeDisplay = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CodeDisplayCard(
                    title = "问题代码 (C++)",
                    code = """
// 栈上 10 字节缓冲区
char buffer[10];

// 超过 48 字节的源数据
const char* src = "This string is much longer...";

// ❌ 写入超出边界 → 破坏栈帧 → SIGABRT
memcpy(buffer, src, strlen(src));
                    """.trimIndent()
                )

                if (fixEnabled) {
                    CodeDisplayCard(
                        title = "修复代码 (C++)",
                        code = """
// 修复1: 使用 strncpy 限制长度
strncpy(buffer, src, sizeof(buffer) - 1);
buffer[sizeof(buffer) - 1] = '\\0';

// 修复2: 使用 std::string 自动管理
std::string buffer = src;  // 自动扩容
                        """.trimIndent()
                    )
                }
            }
        },
        actionButtons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NativePurple,
                        disabledContainerColor = NativePurple.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "💥 触发 Crash（SIGABRT/SIGSEGV）",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "⚠️ 栈缓冲区溢出，破坏 return address，可能触发 Stack Canary",
                    style = androidx.compose.ui.text.TextStyle(
                        color = NativePurple.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )

                Text(
                    text = "logcat: 查找 \"FATAL SIGNAL 6 (SIGABRT)\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    )

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认触发 Native Crash？") },
            text = {
                Text(
                    "栈缓冲区溢出将破坏栈帧，可能触发 Stack Canary 检测。\n\n" +
                    "崩溃后请在 logcat 中查看:\n" +
                    "• FATAL SIGNAL 6 (SIGABRT) 或 11 (SIGSEGV)\n" +
                    "• backtrace 中的 memcpy 调用"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        NativeCrashBridge.triggerBufferOverflow()
                    }
                ) {
                    Text("确认触发", color = NativePurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
