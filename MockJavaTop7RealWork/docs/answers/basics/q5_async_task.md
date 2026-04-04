# 异步任务 & UI 更新

## 题目描述
使用 AsyncTask 实现一个模拟下载任务，展示进度条并更新 UI。

## 关键知识点
- `onPreExecute()` - 准备阶段，主线程
- `doInBackground()` - 后台执行，子线程
- `onPostExecute()` - 完成阶段，主线程
- `publishProgress()` - 进度更新（可选）

## 参考答案

### onPreExecute 实现
```kotlin
override fun onPreExecute() {
    progressBar.visibility = View.VISIBLE
    tvResult.text = "开始下载..."
}
```

### doInBackground 实现
```kotlin
override fun doInBackground(vararg voids: Void?): String? {
    // 模拟下载过程
    for (i in 1..10) {
        Thread.sleep(300)  // 模拟耗时操作
        publishProgress(i * 10)
    }
    return "下载完成"
}
```

### onPostExecute 实现
```kotlin
override fun onPostExecute(result: String?) {
    progressBar.visibility = View.GONE
    tvResult.text = result
}
```

## 注意事项
- AsyncTask 已在 Android R 废弃，仅作学习理解 Handler 原理
- 实际开发推荐使用 Kotlin 协程 + Flow
