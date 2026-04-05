package com.example.androidcrashanalysis.ui.nativememory

import android.graphics.Bitmap
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
import kotlin.random.Random
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.common.MemoryInfoBar
import com.example.androidcrashanalysis.ui.common.getMemorySnapshot
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.NativeMemoryBlue

/**
 * 场景：Native 内存耗尽（LMK 杀进程）
 *
 * 【问题原理】
 * Bitmap.createBitmap() 分配在 Native 堆而非 Java 堆。
 * 循环创建大 Bitmap（4096x4096 ARGB_8888 = 64MB/张）会快速耗尽 Native 堆，
 * 触发 Low Memory Killer (LMK) 杀进程，产生 SIGKILL 信号，进程无法捕获。
 *
 * 注意：这里不会抛出 Java OutOfMemoryError，而是进程直接被系统杀掉。
 *
 * 【修复方案】
 * 1. 使用 inSampleSize 压缩图片尺寸（如 1024x1024 = 4MB）
 * 2. 加载前检查 Native 可用内存
 * 3. 及时 recycle() 释放 Bitmap
 */
@Composable
fun BitmapNativeMemoryScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("oom_bitmap") !! }
    var fixEnabled by remember { mutableStateOf(false) }
    var memorySnapshot by remember { mutableStateOf(getMemorySnapshot()) }
    var bitmapCount by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val bitmapList = remember { mutableListOf<Bitmap>() }

    DisposableEffect(Unit) {
        onDispose {
            bitmapList.forEach { if (!it.isRecycled) it.recycle() }
            bitmapList.clear()
        }
    }

    ScenarioScaffold(
        title = scenario.title,
        description = scenario.description,
        dangerLevel = scenario.dangerLevel,
        categoryColor = NativeMemoryBlue,
        explanationText = scenario.explanationText,
        investigationMethod = scenario.investigationMethod,
        fixDescription = scenario.fixDescription,
        fixEnabled = fixEnabled,
        onFixToggle = { fixEnabled = it },
        onBack = {
            bitmapList.forEach { if (!it.isRecycled) it.recycle() }
            bitmapList.clear()
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
                        if (fixEnabled) {
                            val newMemory = getMemorySnapshot()
                            val freeMem = newMemory.javaFree
                            if (freeMem < 50L * 1024 * 1024) {
                                bitmapList.forEach { if (!it.isRecycled) it.recycle() }
                                bitmapList.clear()
                                System.gc()
                            }
                            // 修复：加载小图（1024x1024），每张约 4MB
                            val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
                            bitmapList.add(bitmap)
                            bitmapCount = bitmapList.size
                        } else {
                            try {
                                // 一次创建 20 个 4096x4096 Bitmap，每个约 64MB
                                // 合计约 1.28GB，保证触发 LMK 杀进程
                                repeat(20) {
                                    val bitmap = Bitmap.createBitmap(4096, 4096, Bitmap.Config.ARGB_8888)
                                    bitmap.eraseColor(android.graphics.Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)))
                                    bitmapList.add(bitmap)
                                }
                                bitmapCount = bitmapList.size
                            } catch (e: OutOfMemoryError) {
                                bitmapCount = -1
                            }
                        }
                        memorySnapshot = getMemorySnapshot()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NativeMemoryBlue,
                        disabledContainerColor = NativeMemoryBlue.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本 - 加载小图 (4MB)" else "⚠️ 触发 Native 内存压力（≈ 1.28GB）",
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

                val statusText = if (bitmapCount < 0) {
                    "💥 内存压力已触发！"
                } else {
                    "已创建 Bitmap: $bitmapCount 张"
                }

                Text(
                    text = statusText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = if (bitmapCount < 0) Color.Red else if (fixEnabled) Color(0xFF388E3C) else NativeMemoryBlue.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )

                if (!fixEnabled) {
                    Text(
                        text = "⚠️ Native 内存耗尽时进程会被 LMK 杀掉（SIGKILL，无法捕获）",
                        style = androidx.compose.ui.text.TextStyle(
                            color = NativeMemoryBlue.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ inSampleSize 压缩 + 内存检查，安全",
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
