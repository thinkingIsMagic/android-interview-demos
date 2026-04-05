package com.example.androidcrashanalysis.ui.oom

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.common.MemoryInfoBar
import com.example.androidcrashanalysis.ui.common.getMemorySnapshot
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.OomRed

/**
 * 场景：集合泄漏
 *
 * 【问题原理】
 * 全局单例中的集合持续 add 大对象，但从不清理，导致内存持续增长最终 OOM。
 *
 * 【修复方案】
 * 1. 设置集合容量上限（MAX_SIZE）
 * 2. 超出容量时移除最老的元素（FIFO）
 */
@Composable
fun CollectionLeakScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("oom_collection") !! }
    var fixEnabled by remember { mutableStateOf(false) }
    var memorySnapshot by remember { mutableStateOf(getMemorySnapshot()) }
    var storedCount by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            if (fixEnabled) {
                SafeLeakStore.clearAll()
            } else {
                UnsafeLeakStore.clearAll()
            }
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
            if (fixEnabled) SafeLeakStore.clearAll() else UnsafeLeakStore.clearAll()
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val data = ByteArray(1024 * 1024)
                        if (fixEnabled) {
                            SafeLeakStore.store(data)
                            storedCount = SafeLeakStore.size()
                        } else {
                            UnsafeLeakStore.store(data)
                            storedCount = UnsafeLeakStore.size()
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
                        text = if (fixEnabled) "🔧 修复版本 - 安全存储 (容量上限100)" else "⚠️ 存储 1MB 数据（持续增长）",
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        repeat(10) {
                            val data = ByteArray(1024 * 1024)
                            if (fixEnabled) {
                                SafeLeakStore.store(data)
                                storedCount = SafeLeakStore.size()
                            } else {
                                UnsafeLeakStore.store(data)
                                storedCount = UnsafeLeakStore.size()
                            }
                        }
                        memorySnapshot = getMemorySnapshot()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OomRed.copy(alpha = 0.8f),
                        disabledContainerColor = OomRed.copy(alpha = 0.2f)
                    )
                ) {
                    Text(text = "📦 批量存储 10MB")
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
                    text = "集合大小: $storedCount 项 ≈ ${storedCount}MB",
                    style = androidx.compose.ui.text.TextStyle(
                        color = if (fixEnabled) Color(0xFF388E3C) else OomRed.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )

                if (!fixEnabled) {
                    Text(
                        text = "提示：集合只增不减，退出页面后 GC 内存不下降",
                        style = androidx.compose.ui.text.TextStyle(
                            color = OomRed.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ 容量上限100，超出自动清理最旧数据",
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

// ========== 问题版本：无容量限制 ==========
private object UnsafeLeakStore {
    private val leakList = mutableListOf<ByteArray>()

    fun store(data: ByteArray) {
        leakList.add(data)
    }

    fun size(): Int = leakList.size

    fun clearAll() {
        leakList.clear()
    }
}

// ========== 修复版本：容量上限 ==========
private object SafeLeakStore {
    private val MAX_SIZE = 100
    private val safeList = mutableListOf<ByteArray>()

    fun store(data: ByteArray) {
        if (safeList.size >= MAX_SIZE) {
            safeList.removeAt(0)
        }
        safeList.add(data)
    }

    fun size(): Int = safeList.size

    fun clearAll() {
        safeList.clear()
    }
}
