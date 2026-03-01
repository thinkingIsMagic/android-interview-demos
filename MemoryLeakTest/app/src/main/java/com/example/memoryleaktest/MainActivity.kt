package com.example.memoryleaktest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.memoryleaktest.leak.LeakDemoActivity
import com.example.memoryleaktest.leak.LeakFixedDemoActivity
import com.example.memoryleaktest.performance.FlameGraphDemoActivity
import com.example.memoryleaktest.ui.theme.MemoryLeakTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MemoryLeakTestTheme {
                HomeScreen(
                    onStartLeakyDemo = {
                        startActivity(Intent(this, LeakDemoActivity::class.java))
                    },
                    onStartFixedDemo = {
                        startActivity(Intent(this, LeakFixedDemoActivity::class.java))
                    },
                    onStartFlameGraphDemo = {
                        startActivity(Intent(this, FlameGraphDemoActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartLeakyDemo: () -> Unit,
    onStartFixedDemo: () -> Unit,
    onStartFlameGraphDemo: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android 性能分析实践") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Android 性能分析实践",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 内存泄漏演示入口
            DemoCard(
                title = "Handler 内存泄漏",
                description = "非静态 Handler + 延迟消息\n点击后立即退出，观察泄漏",
                buttonText = "进入泄漏演示",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                buttonColor = MaterialTheme.colorScheme.error,
                onClick = onStartLeakyDemo
            )

            // 修复演示入口
            DemoCard(
                title = "Handler 泄漏修复",
                description = "静态 Handler + WeakReference\n在 onDestroy 中清理消息",
                buttonText = "进入修复演示",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                buttonColor = MaterialTheme.colorScheme.tertiary,
                onClick = onStartFixedDemo
            )

            // 火焰图演示入口
            DemoCard(
                title = "火焰图分析",
                description = "主线程卡顿分析\n使用 CPU Profiler 查看火焰图",
                buttonText = "进入火焰图演示",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                buttonColor = MaterialTheme.colorScheme.primary,
                onClick = onStartFlameGraphDemo
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 说明文字
            Text(
                text = "提示：配合 LeakCanary 和 Android Profiler 使用效果更佳\n参考文档: docs/Handler内存泄漏分析指南.md",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DemoCard(
    title: String,
    description: String,
    buttonText: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    buttonColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text(buttonText)
            }
        }
    }
}
