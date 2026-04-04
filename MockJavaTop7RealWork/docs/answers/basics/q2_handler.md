# Handler 延时任务 & UI线程

## 题目描述
使用 Handler 实现一个延时任务，每秒更新一次 UI，计数到 10 后停止。

## 关键知识点
- Handler 创建（绑定 Looper.getMainLooper()）
- `postDelayed()` 延时执行
- `removeCallbacks()` 移除回调
- UI 线程更新

## 参考答案

### Handler 创建与启动
```kotlin
private val runnable = Runnable {
    count++
    tvTime.text = "倒计时: ${maxCount - count}"
    if (count < maxCount) {
        handler?.postDelayed(runnable, 1000)
    }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.q2_handler)
    tvTime = findViewById(R.id.tvTime)

    handler = Handler(Looper.getMainLooper())
    handler?.postDelayed(runnable, 1000)
}
```

### 移除回调防止内存泄漏
```kotlin
override fun onDestroy() {
    super.onDestroy()
    handler?.removeCallbacks(runnable)
}
```

## 注意事项
- Handler 必须绑定到 Looper.getMainLooper() 才能更新 UI
- onDestroy 时务必移除回调，避免内存泄漏
