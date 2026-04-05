package com.example.androidcrashanalysis.ui.oom

import android.os.Handler
import android.os.Looper
import android.os.Message
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
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.common.getMemorySnapshot
import com.example.androidcrashanalysis.ui.theme.OomRed
import java.lang.ref.WeakReference

// ========== 问题版本：匿名内部类 Handler 持有外部引用 ==========
//
// 【触发原理】
// 匿名内部类 Handler 会隐式持有外部类（Activity）的引用。
// sendEmptyMessageDelayed 将 Message 放入 MessageQueue，延迟 delayMs 毫秒。
// 引用链：MessageQueue → Message → Handler(匿名类) → Activity
// 在延迟消息被处理前，Activity 无法被 GC 回收。
//
// 【泄漏条件】
// DisposableEffect 的 onDispose 是空实现 → Message 永远留在队列中 → 永久泄漏。
// 退出页面后 GC，Activity 仍在内存中。
private object UnsafeLeakHandler {
    private var handler: Handler? = null
    fun sendDelayed(ctx: android.content.Context, delayMs: Long) {
        handler = object : Handler(Looper.getMainLooper()) {
            // 匿名内部类：隐式持有外部 Activity 的引用
            override fun handleMessage(msg: Message) { super.handleMessage(msg) }
        }
        handler?.sendEmptyMessageDelayed(1, delayMs)
        // ❌ DisposableEffect.onDispose 为空 → Message 永不清理
    }
    fun cleanup() { handler = null }
}

// ========== 修复版本：静态 Handler + WeakReference + 手动清理 ==========
//
// 【修复原理】
// 1. SafeHandler 改为 static 内部类 → 不持有外部 Activity 引用
// 2. 通过 WeakReference 持有 Activity（可选，仅当需要操作 UI 时）
// 3. onDispose 中调用 removeCallbacksAndMessages(null) 移除所有消息
//    → MessageQueue 中不再有 pending message → Activity 可被 GC 回收
private object SafeLeakHandler {
    private var handler: SafeHandler? = null
    fun sendDelayed(ctx: android.content.Context, delayMs: Long) {
        val activityRef = WeakReference(ctx as? android.app.Activity)
        handler = SafeHandler(Looper.getMainLooper(), activityRef)
        handler?.postDelayed({}, delayMs)
    }
    fun cleanup() {
        // ✅ 移除所有与该 Handler 关联的回调和消息
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }
    private class SafeHandler(
        looper: Looper,
        private val activityRef: WeakReference<android.app.Activity?>
    ) : Handler(looper) {
        // static 内部类：不持有外部 Activity 引用
    }
}

@Composable
fun HandlerLeakScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("oom_handler_leak")!! }
    var fixEnabled by remember { mutableStateOf(false) }
    var memorySnapshot by remember { mutableStateOf(getMemorySnapshot()) }
    var messageCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    DisposableEffect(fixEnabled) {
        onDispose {
            if (fixEnabled) SafeLeakHandler.cleanup() else UnsafeLeakHandler.cleanup()
        }
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
        onBack = {
            if (fixEnabled) SafeLeakHandler.cleanup() else UnsafeLeakHandler.cleanup()
            onBack()
        },
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
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (fixEnabled) {
                            SafeLeakHandler.sendDelayed(context, 30000)
                        } else {
                            UnsafeLeakHandler.sendDelayed(context, 30000)
                        }
                        messageCount++
                        memorySnapshot = getMemorySnapshot()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OomRed,
                        disabledContainerColor = OomRed.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本 - 安全 Handler" else "⚠️ 触发泄漏（发送 30s 延迟消息）",
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
                    text = "已发送延迟消息: $messageCount 条",
                    style = androidx.compose.ui.text.TextStyle(
                        color = if (fixEnabled) Color(0xFF388E3C) else OomRed.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    )
}
