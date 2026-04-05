package com.example.androidcrashanalysis.ui.common

import android.os.Debug

/**
 * 内存快照数据类
 * 同时记录 Java 堆和 Native 堆信息
 * Bitmap 实际分配在 Native 堆中，所以 Native 堆才是观察 Bitmap OOM 的关键
 */
data class MemorySnapshot(
    val javaUsed: Long,     // Java 堆已用
    val javaMax: Long,      // Java 堆最大
    val javaFree: Long,      // Java 堆空闲
    val nativeAllocated: Long, // Native 堆已分配（Bitmap 实际在此）
    val nativeSize: Long,     // Native 堆总大小
) {
    val totalNativeFree: Long get() = nativeSize - nativeAllocated
}

/** 获取当前内存信息（Java 堆 + Native 堆） */
fun getMemorySnapshot(): MemorySnapshot {
    val runtime = Runtime.getRuntime()
    return MemorySnapshot(
        javaUsed = runtime.totalMemory() - runtime.freeMemory(),
        javaMax = runtime.maxMemory(),
        javaFree = runtime.freeMemory(),
        nativeAllocated = Debug.getNativeHeapAllocatedSize(),
        nativeSize = Debug.getNativeHeapSize()
    )
}
