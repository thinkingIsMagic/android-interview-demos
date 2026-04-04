package com.example.androidcrashanalysis.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
 * 显示 Runtime.getRuntime() 的 used/max/free memory
 * @param usedBytes 已用内存（bytes）
 * @param maxBytes 最大可用内存（bytes）
 * @param freeBytes 空闲内存（bytes）
 */
@Composable
fun MemoryInfoBar(
    usedBytes: Long,
    maxBytes: Long,
    freeBytes: Long,
    modifier: Modifier = Modifier,
    usedColor: Color = MaterialTheme.colorScheme.error,
    freeColor: Color = MaterialTheme.colorScheme.primary
) {
    val usageRatio = if (maxBytes > 0) (usedBytes.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(targetValue = usageRatio, label = "memory")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📊 当前内存状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 内存条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(freeColor.copy(alpha = 0.3f))
            ) {
                // 已用部分（从右往左增长）
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                usageRatio > 0.9f -> Color(0xFFD32F2F)
                                usageRatio > 0.7f -> Color(0xFFFF9800)
                                else -> usedColor
                            }
                        )
                )
            }

            // 数值显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "已用 (Used)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(usedBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "空闲 (Free)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(freeBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "最大 (Max)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(maxBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 使用百分比
            Text(
                text = "使用率: ${(usageRatio * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 将字节数格式化为可读字符串
 */
private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        String.format("%.1f GB", mb / 1024)
    } else {
        String.format("%.1f MB", mb)
    }
}
