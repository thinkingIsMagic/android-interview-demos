package com.example.androidcrashanalysis.ui.oom

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.common.MemoryInfoBar
import com.example.androidcrashanalysis.ui.common.getMemorySnapshot
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.OomRed

/**
 * 场景：Bitmap 大图 OOM
 *
 * 【问题原理】
 * Bitmap 是 Android 中内存占用最大的对象之一。
 * 4096x4096 ARGB_8888 = 64MB。循环创建多张会导致 OOM。
 *
 * 【修复方案】
 * 1. 使用 inSampleSize 压缩图片尺寸
 * 2. 使用完成后及时 recycle()
 * 3. 加载前检查可用内存
 */
@Composable
fun BitmapOomScreen(onBack: () -> Unit) {
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
        categoryColor = OomRed,
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
        memoryInfo = { MemoryInfoBar(memorySnapshot.used, memorySnapshot.max, memorySnapshot.free) },
        actionButtons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (fixEnabled) {
                            val newMemory = getMemorySnapshot()
                            val freeMem = newMemory.free
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
                                val bitmap = Bitmap.createBitmap(4096, 4096, Bitmap.Config.ARGB_8888)
                                bitmapList.add(bitmap)
                                bitmapCount = bitmapList.size
                            } catch (e: OutOfMemoryError) {
                                bitmapCount = -1
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
                        text = if (fixEnabled) "🔧 修复版本 - 加载小图 (4MB)" else "⚠️ 触发 OOM（创建大图 64MB）",
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
                    "💥 OOM 已触发！"
                } else {
                    "已创建 Bitmap: $bitmapCount 张"
                }

                Text(
                    text = statusText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = if (bitmapCount < 0) Color.Red else if (fixEnabled) Color(0xFF388E3C) else OomRed.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                )

                if (!fixEnabled) {
                    Text(
                        text = "⚠️ 4-8 张即可触发 OOM，注意保存数据",
                        style = androidx.compose.ui.text.TextStyle(
                            color = OomRed.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ inSampleSize=4 + 内存检查，安全",
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
