package com.example.androidcrashanalysis.model

import androidx.compose.ui.graphics.Color
import com.example.androidcrashanalysis.ui.theme.AnrOrange
import com.example.androidcrashanalysis.ui.theme.NativeMemoryBlue
import com.example.androidcrashanalysis.ui.theme.NativePurple
import com.example.androidcrashanalysis.ui.theme.OomRed

/**
 * 场景分类枚举
 * @param label 中文标签名称
 * @param color 主色调
 */
enum class ScenarioCategory(
    val label: String,
    val color: Color
) {
    /** 内存溢出（Out Of Memory）*/
    OOM("OOM", OomRed),
    /** 应用无响应（Application Not Responding）*/
    ANR("ANR", AnrOrange),
    /** Native 层崩溃（C++ 代码触发）*/
    NATIVE_CRASH("Native Crash", NativePurple),
    /** Native 内存耗尽（LMK 杀进程）*/
    NATIVE_MEMORY("Native 内存", NativeMemoryBlue)
}
