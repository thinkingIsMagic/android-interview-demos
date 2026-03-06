package com.example.mockjavatop7realwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mockjavatop7realwork.ui.theme.MockJavaTop7RealWorkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MockJavaTop7RealWorkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    InterviewQuestionsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

data class InterviewQuestion(
    val id: String,
    val title: String,
    val company: String,
    val description: String
)

val questions = listOf(
    InterviewQuestion(
        id = "Q1",
        title = "1. 表情面板设计",
        company = "字节跳动",
        description = "流式布局 + LRU缓存 + 图片内存优化 + 点击防抖"
    ),
    InterviewQuestion(
        id = "Q2",
        title = "2. 弱网检测",
        company = "字节跳动",
        description = "滑动窗口统计 + 冷却期逻辑"
    ),
    InterviewQuestion(
        id = "Q3",
        title = "3. 海量数据处理",
        company = "通用",
        description = "Hash分治 + 布隆过滤器"
    ),
    InterviewQuestion(
        id = "Q4",
        title = "4. 离线下载管理器",
        company = "通用",
        description = "断点续传 + 并发控制 + 进度防抖"
    ),
    InterviewQuestion(
        id = "Q5",
        title = "5. IM消息处理",
        company = "通用",
        description = "SeqID顺序校验 + ACK确认 + 去重 + 消息拉取"
    ),
    InterviewQuestion(
        id = "Q6",
        title = "6. 超长图查看器",
        company = "通用",
        description = "BitmapRegionDecoder区域解码 + 手势缩放"
    ),
    InterviewQuestion(
        id = "Q7",
        title = "7. APM性能监控",
        company = "通用",
        description = "内存泄漏 + OOM监控 + ANR Watchdog"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewQuestionsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Android 面试题 Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "共 7 道面试题，代码中已留空候选人需实现的部分",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(questions) { question ->
                QuestionCard(question = question)
            }
        }
    }
}

@Composable
fun QuestionCard(question: InterviewQuestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 点击查看详情 */ },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = question.company,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = question.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
