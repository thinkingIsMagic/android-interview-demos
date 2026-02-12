package com.example.android_demo_ceiling_container_1.ui.bottom.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_demo_ceiling_container_1.data.model.ComponentConfig

/**
 * Banner 组件 UI
 */
@Composable
fun BannerComponent(
    config: ComponentConfig.BannerConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadius = config.cornerRadius.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(config.height.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFEEEEEE))
            .clickable { onClick() }
    ) {
        // 这里简化处理，实际应该使用 AsyncImage 加载图片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = config.title.ifBlank { "Banner" },
                color = Color(0xFF333333),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "点击跳转活动页",
                color = Color(0xFF999999),
                fontSize = 12.sp
            )
        }
    }
}
