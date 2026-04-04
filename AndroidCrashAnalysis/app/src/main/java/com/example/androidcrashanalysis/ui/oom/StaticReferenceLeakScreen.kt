package com.example.androidcrashanalysis.ui.oom

import android.app.Activity
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
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.common.getMemorySnapshot
import com.example.androidcrashanalysis.ui.theme.OomRed
import java.lang.ref.WeakReference

// ========== 问题版本：强引用持有 Activity ==========
private object UnsafeActivityHolder {
    var heldActivity: Activity? = null
}

// ========== 修复版本：弱引用持有 Activity ==========
private object SafeActivityHolder {
    var heldRef: WeakReference<Activity>? = null
}

@Composable
fun StaticReferenceLeakScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("oom_static_ref")!! }
    var fixEnabled by remember { mutableStateOf(false) }
    var memorySnapshot by remember { mutableStateOf(getMemorySnapshot()) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        if (context is Activity) {
            if (fixEnabled) {
                SafeActivityHolder.heldRef = WeakReference(context)
            } else {
                UnsafeActivityHolder.heldActivity = context
            }
        }
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
        memoryInfo = { MemoryInfoBar(memorySnapshot.used, memorySnapshot.max, memorySnapshot.free) },
        actionButtons = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (context is Activity) {
                            if (fixEnabled) {
                                SafeActivityHolder.heldRef = WeakReference(context)
                            } else {
                                UnsafeActivityHolder.heldActivity = context
                            }
                        }
                        memorySnapshot = getMemorySnapshot()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OomRed,
                        disabledContainerColor = OomRed.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本 - WeakReference 安全" else "⚠️ 触发泄漏（static 强引用）",
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = { memorySnapshot = getMemorySnapshot() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📊 查看当前内存") }
                OutlinedButton(
                    onClick = {
                        System.gc()
                        System.runFinalization()
                        memorySnapshot = getMemorySnapshot()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗑️ 手动 GC") }
                Text(
                    text = if (!fixEnabled) "提示：旋转屏幕后再 GC，观察内存是否下降"
                           else "WeakReference 不阻碍 GC，Activity 可正常回收",
                    style = androidx.compose.ui.text.TextStyle(
                        color = if (fixEnabled) Color(0xFF388E3C) else OomRed.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    )
}
