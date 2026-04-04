# 网络状态触发弹窗

## 题目描述
实现网络状态监听，当检测到弱网（3分钟内有3次BAD或30秒内BAD占比超过50%）时触发弹窗，有冷却期。

## 关键知识点
- 队列数据结构（滑动窗口）
- 时间窗口统计
- 冷却期逻辑

## 参考答案

### 常量定义
```kotlin
class NetworkPopupManager {
    companion object {
        private const val COOL_DOWN = 30_000L      // 冷却期30秒
        private const val THREE_MIN = 180_000L     // 3分钟窗口
        private const val THIRTY_SEC = 30_000L     // 30秒窗口
        private const val BAD_COUNT_THRESHOLD = 3 // 3分钟BAD次数阈值
        private const val BAD_RATIO_THRESHOLD = 0.5 // BAD占比阈值
    }

    private val badQueue = ArrayDeque<Long>()     // 记录BAD时间点
    private val allQueue = ArrayDeque<Long>()     // 记录所有状态时间点
    private var lastPopupTime = 0L
}
```

### onNetworkChanged 实现
```kotlin
fun onNetworkChanged(state: NetworkState) {
    val now = System.currentTimeMillis()

    // 加入队列
    allQueue.add(now)
    if (state == NetworkState.BAD) {
        badQueue.add(now)
    }

    // 清理超时数据
    while (badQueue.isNotEmpty() && now - badQueue.peek() > THREE_MIN) {
        badQueue.poll()
    }
    while (allQueue.isNotEmpty() && now - allQueue.peek() > THREE_MIN) {
        allQueue.poll()
    }

    // 检查冷却期
    if (now - lastPopupTime < COOL_DOWN) {
        return
    }

    // 检查触发条件
    if (check3MinBadCount() || check30sBadRatio()) {
        showPopup()
        lastPopupTime = now
    }
}

private fun check3MinBadCount(): Boolean {
    return badQueue.size >= BAD_COUNT_THRESHOLD
}

private fun check30sBadRatio(): Boolean {
    val now = System.currentTimeMillis()
    val recentBad = badQueue.count { now - it <= THIRTY_SEC }
    val recentAll = allQueue.count { now - it <= THIRTY_SEC }
    return recentAll > 0 && recentBad.toFloat() / recentAll >= BAD_RATIO_THRESHOLD
}
```
