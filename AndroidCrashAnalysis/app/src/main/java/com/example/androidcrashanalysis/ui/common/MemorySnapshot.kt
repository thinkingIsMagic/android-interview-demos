package com.example.androidcrashanalysis.ui.common

/**
 * 内存快照数据类
 * 用于 OOM 场景的内存状态展示
 */
data class MemorySnapshot(
    val used: Long,
    val max: Long,
    val free: Long
)

/** 获取当前内存信息 */
fun getMemorySnapshot(): MemorySnapshot {
    val runtime = Runtime.getRuntime()
    return MemorySnapshot(
        used = runtime.totalMemory() - runtime.freeMemory(),
        max = runtime.maxMemory(),
        free = runtime.freeMemory()
    )
}
