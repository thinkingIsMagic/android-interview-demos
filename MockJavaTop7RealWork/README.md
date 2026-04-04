# Android 面试题 Demo 项目

本项目包含7个常见的Android开发场景面试题，代码中已留出候选人需要实现的部分。

## 项目结构

```
app/src/main/java/com/example/mockjavatop7realwork/
├── MainActivity.kt
├── c_interview/            # 面试题（7道）
│   ├── q1_emoji_panel/      # 表情面板设计
│   ├── q2_weak_network/     # 弱网检测
│   ├── q3_massive_data/     # 海量数据处理
│   ├── q4_download_manager/ # 离线下载管理器
│   ├── q5_im_message/       # IM消息处理
│   ├── q6_large_image/      # 超长图查看器
│   └── q7_apm_monitor/      # APM性能监控
└── b_basics/                # 基础题（7道）
    ├── MainActivity.kt
    ├── q1_multi_type/       # RecyclerView多类型Item
    ├── q2_handler/          # Handler延时任务
    ├── q3_network_popup/    # 网络状态触发弹窗
    ├── q4_circle_view/      # 自定义View测量绘制
    ├── q5_async_task/      # 异步任务与UI更新
    ├── q6_draggable_view/   # 自定义View触摸交互
    └── q7_retrofit/        # Retrofit网络请求

docs/
├── ANSWERS.md              # 汇总答案
└── answers/
    ├── interview/          # 面试题答案
    │   ├── q1_emoji_panel.md
    │   ├── q2_weak_network.md
    │   ├── q3_massive_data.md
    │   ├── q4_download_manager.md
    │   ├── q5_im_message.md
    │   ├── q6_large_image.md
    │   └── q7_apm_monitor.md
    └── basics/              # 基础题答案
        ├── q1_multi_type.md
        ├── q2_handler.md
        ├── q3_network_popup.md
        ├── q4_circle_view.md
        ├── q5_async_task.md
        ├── q6_draggable_view.md
        └── q7_retrofit.md
```

## 面试题列表

### 1. 字节跳动：表情面板设计 【已遇到】

**考察点：**
- 自定义 RecyclerView.LayoutManager（流式布局）
- LRU 缓存（LinkedHashMap）
- 图片内存优化（inSampleSize、BitmapFactory.Options）
- 点击防抖与预加载

**候选人需要实现：**
- `FlowLayoutManager` - 流式布局核心逻辑
- `LruCacheUtils` - LRU缓存工具类
- `EmojiBitmapOptimizer` - 图片加载优化

---

### 2. 字节跳动：弱网检测 【已遇到】

**考察点：**
- 滑动窗口统计
- 队列数据结构
- 冷却期逻辑

**候选人需要实现：**
- `onNetworkChanged` - 网络状态处理
- `cleanOld` - 清理过期数据
- `check3MinBadCount` - 3分钟BAD次数检查
- `check30sBadRatio` - 30秒BAD占比检查
- `isInCoolDown` - 冷却期判断

---

### 3. 海量数据题

**考察点：**
- 分治思想
- Hash 散列
- 布隆过滤器

**候选人需要实现：**
- `splitFileByHash` - Hash分治拆分文件
- `findCommonUrls` - 逐个对比找相同URL
- `BloomFilter` - 布隆过滤器（可选）

---

### 4. 离线下载管理器（断点续传 & 并发控制）

**考察点：**
- HTTP Range 请求头
- RandomAccessFile 断点续写
- 线程池与优先级队列
- 进度回调防抖

**候选人需要实现：**
- `DownloadTask.run` - 下载任务核心逻辑
- `setupRangeConnection` - 设置Range请求头
- `reportProgress` - 进度回调防抖

---

### 5. IM即时通讯：消息补偿与乱序处理

**考察点：**
- Seq ID 序列号
- ACK 确认机制
- 消息去重
- 消息缓存与拉取

**候选人需要实现：**
- `onReceive` - 消息接收处理
- `processInOrder` - 顺序消息处理
- `isDuplicate` - 去重检查
- `pullMsgFromServer` - 拉取缺失消息

---

### 6. 超长图/巨型图片查看器

**考察点：**
- BitmapRegionDecoder 区域解码
- RGB_565 内存优化
- 手势处理（滑动、缩放）

**候选人需要实现：**
- `init` - 初始化解码器
- `onDraw` - 区域解码绘制
- `onScroll` - 滑动处理
- `onScale` - 缩放处理

---

### 7. APM性能监控：零侵入的OOM/ANR监控

**考察点：**
- LeakCanary 内存泄漏检测原理
- WeakReference + ReferenceQueue
- Watchdog ANR监控机制
- 堆转储（dumpHprofData）

**候选人需要实现：**
- `ActivityLeakMonitor.watch` - 监听Activity销毁
- `OOMMonitor.getMemoryUsage` - 内存使用率计算
- `ANRWatchdog.run` - ANR监控循环
- `collectANRInfo` - 收集ANR信息

---

## 使用方式

1. 每个面试题在单独文件夹中，方便单独练习
2. 候选人阅读题目描述和注释
3. 实现标记为 `TODO` 的方法
4. 参考 `docs/answers/` 下的答案文件验证正确性
