package com.example.androidcrashanalysis.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 内存信息条
 * 同时显示 Java 堆和 Native 堆信息
 * 重点：Bitmap 分配在 Native 堆，观察 Native 堆才能看到 Bitmap 的内存占用
 */
@Composable
fun MemoryInfoBar(
    javaUsed: Long,
    javaMax: Long,
    javaFree: Long,
    nativeAllocated: Long,
    nativeSize: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 当前内存状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // === Java 堆 ===
            SectionHeader("☕ Java 堆")
            MemoryBar(
                used = javaUsed,
                max = javaMax,
                label = "Java"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("已用: ${formatBytes(javaUsed)}", style = MaterialTheme.typography.labelSmall)
                Text("最大: ${formatBytes(javaMax)}", style = MaterialTheme.typography.labelSmall)
                Text("空闲: ${formatBytes(javaFree)}", style = MaterialTheme.typography.labelSmall)
            }

            // === Native 堆（Bitmap 实际在此）===
            SectionHeader("🖼️ Native 堆（Bitmap 所在）")
            MemoryBar(
                used = nativeAllocated,
                max = nativeSize,
                label = "Native"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("已分配: ${formatBytes(nativeAllocated)}", style = MaterialTheme.typography.labelSmall)
                Text("总大小: ${formatBytes(nativeSize)}", style = MaterialTheme.typography.labelSmall)
                Text("空闲: ${formatBytes(nativeSize - nativeAllocated)}", style = MaterialTheme.typography.labelSmall)
            }

            // 提示
            Text(
                text = "💡 提示：Bitmap 分配在 Native 堆，点击\"触发 OOM\"后观察上方 Native 堆增长",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 单条内存使用进度条 */
@Composable
private fun MemoryBar(used: Long, max: Long, label: String) {
    val ratio = if (max > 0) (used.toFloat() / max).coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "bar")

    val barColor = when {
        ratio > 0.9f -> Color(0xFFD32F2F)
        ratio > 0.7f -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(barColor.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedRatio)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
