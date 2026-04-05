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
import com.example.androidcrashanalysis.ui.common.ExplanationCard
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.NativePurple

/**
 * 场景：Native 空指针解引用
 *
 * 【问题】
 * C++ 中对 nullptr 解引用，触发 SIGSEGV (Signal 11)
 *
 * 【修复方案】
 * 防御性编程：使用前检查指针是否为 nullptr
 *
 * 【触发核心逻辑 — JNI 调用】
 * NativeCrashBridge.triggerNullPointerDereference() → JNI → C++ 代码：
 *     int* ptr = nullptr;
 *     *ptr = 42;  // ← 解引用 0x0 → SIGSEGV
 *
 * 【日志特征】
 * - FATAL SIGNAL 11 (SIGSEGV)
 * - fault addr: 0x0  ← 空指针的特征地址
 * - 与 UAF 的区别：UAF 的 fault addr 是非零地址
 */
@Composable
fun NullPointerScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("native_nullptr") !! }
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
                // 问题代码
                CodeDisplayCard(
                    title = "问题代码 (C++)",
                    code = """
// 问题版本：直接解引用空指针
void triggerBuggy() {
    int* ptr = nullptr;
    *ptr = 42;  // ← SIGSEGV, fault addr = 0x0
}
                    """.trimIndent()
                )

                // 修复代码
                if (fixEnabled) {
                    CodeDisplayCard(
                        title = "修复代码 (C++)",
                        code = """
// 修复版本：防御性检查
void triggerFixed() {
    int* ptr = nullptr;

    if (ptr != nullptr) {
        *ptr = 42;
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Native",
            "Null pointer detected!");
    }
}
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
                        text = "💥 触发 Crash（SIGSEGV）",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "⚠️ 触发后 app 会崩溃，请查看 logcat 中的 tombstone 信息",
                    style = androidx.compose.ui.text.TextStyle(
                        color = NativePurple.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )

                Text(
                    text = "logcat 命令: adb logcat | grep -A 30 \"SIGSEGV\"",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    )

    // 确认对话框（Native crash 会杀死进程，所以要提前确认）
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("确认触发 Native Crash？") },
            text = {
                Text(
                    "这将导致应用崩溃（SIGSEGV）。\n\n" +
                    "崩溃后请在 logcat 中查看:\n" +
                    "• FATAL SIGNAL 11 (SIGSEGV)\n" +
                    "• fault addr: 0x0\n" +
                    "• tombstone 文件"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        // 触发 native crash → 进程被杀死
                        NativeCrashBridge.triggerNullPointerDereference()
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
