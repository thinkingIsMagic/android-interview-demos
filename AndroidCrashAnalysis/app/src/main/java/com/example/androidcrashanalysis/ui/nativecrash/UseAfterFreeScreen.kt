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
 * 场景：Native Use-After-Free
 *
 * 【问题】
 * free() 后继续使用指针，触发 SIGSEGV
 * fault addr = 非零地址（与空指针的区别）
 */
@Composable
fun UseAfterFreeScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("native_uaf") !! }
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
int* ptr = (int*)malloc(sizeof(int));
*ptr = 42;

free(ptr);      // 释放内存

*ptr = 100;     // ❌ UAF → SIGSEGV
                // fault addr = 某个非零地址（不是 0x0）
                    """.trimIndent()
                )

                if (fixEnabled) {
                    CodeDisplayCard(
                        title = "修复代码 (C++)",
                        code = """
// 修复1: free 后置空
free(ptr);
ptr = nullptr;  // 置空，后续检查会触发明确的空指针异常

// 修复2: 使用智能指针
auto ptr = std::make_unique<int>(42);
// 作用域结束自动释放，无需手动 free
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
                        text = "💥 触发 Crash（SIGSEGV - UAF）",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "⚠️ Use-After-Free：fault addr ≠ 0x0（与空指针不同）",
                    style = androidx.compose.ui.text.TextStyle(
                        color = NativePurple.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )

                Text(
                    text = "SIGSEGV vs 空指针: fault addr 非零 → UAF",
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
                    "Use-After-Free 是内存安全漏洞。\n\n" +
                    "崩溃后请在 logcat 中查看:\n" +
                    "• FATAL SIGNAL 11 (SIGSEGV)\n" +
                    "• fault addr: 非零地址（这是和空指针的关键区别！）"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        NativeCrashBridge.triggerUseAfterFree()
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
