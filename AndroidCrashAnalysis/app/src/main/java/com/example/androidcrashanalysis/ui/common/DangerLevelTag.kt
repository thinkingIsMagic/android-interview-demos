package com.example.androidcrashanalysis.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.ui.theme.DangerCritical
import com.example.androidcrashanalysis.ui.theme.DangerHigh
import com.example.androidcrashanalysis.ui.theme.DangerLow
import com.example.androidcrashanalysis.ui.theme.DangerMedium

/**
 * 危险等级标签
 * @param level 危险等级 1-5
 * @param color 背景色
 */
@Composable
fun DangerLevelTag(
    level: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val (label, bgColor) = when (level) {
        5 -> "极度危险" to DangerCritical
        4 -> "高危" to DangerHigh
        3 -> "中危" to DangerMedium
        else -> "低危" to DangerLow
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "⚠ $label (L$level)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
