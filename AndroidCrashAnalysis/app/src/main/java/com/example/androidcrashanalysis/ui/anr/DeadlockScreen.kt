package com.example.androidcrashanalysis.ui.anr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidcrashanalysis.model.ScenarioRegistry
import com.example.androidcrashanalysis.ui.common.ScenarioScaffold
import com.example.androidcrashanalysis.ui.theme.AnrOrange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 场景：主线程死锁
 *
 * 【问题原理】
 * 两个线程各自持有对方需要的锁，形成循环等待 → 死锁。
 * - 主线程先获取锁 A，再等锁 B
 * - 子线程先获取锁 B，再等锁 A
 * → 互相等待，永不解锁 → ANR
 *
 * 【修复方案】
 * 统一加锁顺序：所有线程都先获取锁 A，再获取锁 B。
 */
@Composable
fun DeadlockScreen(onBack: () -> Unit) {
    val scenario = remember { ScenarioRegistry.getScenarioById("anr_deadlock") !! }
    var fixEnabled by remember { mutableStateOf(false) }
    var triggered by remember { mutableStateOf(false) }

    ScenarioScaffold(
        title = scenario.title,
        description = scenario.description,
        dangerLevel = scenario.dangerLevel,
        categoryColor = AnrOrange,
        explanationText = scenario.explanationText,
        investigationMethod = scenario.investigationMethod,
        fixDescription = scenario.fixDescription,
        fixEnabled = fixEnabled,
        onFixToggle = { fixEnabled = it },
        onBack = onBack,
        actionButtons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        triggered = true
                        if (fixEnabled) {
                            triggerFixedDeadlock()
                        } else {
                            triggerBuggyDeadlock()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnrOrange,
                        disabledContainerColor = AnrOrange.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (fixEnabled) "🔧 修复版本 - 统一加锁顺序" else "⚠️ 触发死锁（交叉等锁）",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!fixEnabled) {
                    Text(
                        text = "⚠️ 点击后约 5 秒出现 ANR（主线程和子线程互相等锁）",
                        style = androidx.compose.ui.text.TextStyle(
                            color = AnrOrange.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "✅ 统一 A→B 顺序，不会形成环路",
                        style = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF388E3C),
                            fontSize = 12.sp
                        )
                    )
                }

                Text(
                    text = "traces 分析: 找 \"main\" 和另一线程，看 BLOCKED + waiting to lock",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
            }
        }
    )
}

// ========== 问题版本：交叉加锁形成死锁 ==========
//
// 【触发原理 — 死锁四要素】
// 1. 互斥：锁 A 和锁 B 只能被一个线程持有
// 2. 持有并等待：主线程持有 A 还想要 B，子线程持有 B 还想要 A
// 3. 不可抢占：锁不能被强制释放
// 4. 循环等待：主线程等 B，子线程等 A → 形成环路
//
// 【时序】
// 1. 主线程: synchronized(LockA) → 获得锁 A
// 2. CoroutineScope: synchronized(LockB) → 获得锁 B
// 3. 主线程: 尝试 synchronized(LockB) → 等待锁 B 释放
// 4. 子线程: 尝试 synchronized(LockA) → 等待锁 A 释放
// 5. 互相等待，永不解锁 → 死锁 → ANR
//
// 【traces.txt 中的特征】
// "main" tid=1 Blocked  waiting to lock <A> held by Thread-1
// "Thread-1" tid=2 Blocked  waiting to lock <B> held by main
private fun triggerBuggyDeadlock() {
    // 在主线程执行（会触发 ANR）
    synchronized(LockA) {
        // 主线程获得锁 A，等待锁 B
        // 此时子线程正在持有锁 B，等待锁 A
        // 死锁形成！
        synchronized(LockB) {
            // 不会执行到这里
        }
    }
}

// ========== 修复版本：统一加锁顺序 ==========
//
// 【修复原理】
// 核心原则：所有线程按相同顺序获取多个锁。
// 统一为 A→B 顺序后，不存在"等对方释放"的环路：
// - 主线程: A → B → 完成 → 释放 A → 释放 B
// - 子线程: A（等主线程释放）→ B（等主线程释放）→ 完成
// 不会形成循环等待 → 无死锁
private fun triggerFixedDeadlock() {
    CoroutineScope(Dispatchers.Default).launch {
        // 子线程: A → B（统一顺序，与主线程一致）
        synchronized(LockA) {
            synchronized(LockB) {
                // 正常工作
            }
        }
    }

    // 主线程: A → B（同样顺序）
    synchronized(LockA) {
        synchronized(LockB) {
            // 正常工作
        }
    }
}

// 两个锁对象
private val LockA = Object()
private val LockB = Object()
