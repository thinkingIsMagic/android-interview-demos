# Insta360LearningCustomView

一个 **Android 练手项目**,目前包含两个练习:

1. **自定义时间轴控件** —— 手写一个简化版「视频剪辑时间轴」(类似剪映底部的轨道),学 **绘制流程** 和 **事件分发体系**。
2. **Glide 图片库** —— 在一个示例页里练 Glide 的常见用法(加载、占位/失败图、变换、列表加载)。

> 技术栈:Android + Kotlin,传统 View 体系(非 Compose),单 module。

## 这个仓库的玩法

脚手架已经搭好(工程配置、Activity、布局、假数据、类骨架),**核心逻辑留空给你自己写**;
写完跟我说一声,我**只审核、点问题,不替你写实现**。目前工程可直接编译运行。

入口:主界面是时间轴练习,点「打开 Glide 练习」按钮进 Glide 练习页。

---

## 练习一:自定义时间轴控件

把 [TimelineView.kt](app/src/main/java/com/example/insta360learningcustomview/widget/TimelineView.kt)
里四个方法的 TODO 补全。运行时控件区域会显示占位文字 `TODO: 我来实现绘制`。

### 目标示意稿

没有正式 UI 设计图,下面这版 ASCII 示意稿就是 `onDraw` 的绘制目标参考(控件高 120dp,全宽):

```
 ◀── scrollX 横向滚动 ───────────────────────────────────▶
┌──────────────────────────────────────────────────────────┐
│ 00:00    00:05    00:10    00:15    00:20    00:25         │ ← ① 刻度区(顶部 ~24dp)
│   |    |    |    |    |    |    |    |    |    |    |   |    │   时间文字 + 刻度竖线
│ ┌────┐┌────┐┌────┐┌────┐┌────┐┌────┐┌────┐┌────┐┌────┐    │
│▐│ 0  ││ 1  ││ 2  ││ 3  ││ 4  ││ 5  ││ 6  ││ 7  ││ 8  │▐   │ ← ② 缩略图轨道(中部)
│▐└────┘└────┘└────┘└────┘└────┘└────┘└────┘└────┘└────┘▐   │   纯色块 + 序号
│▐                                                      ▐   │
└─▲────────────────────────────────────────────────────▲───┘
  └ 左 trim 手柄                          右 trim 手柄 ┘   ← ③ 手柄(贯穿高度)
   (trimStartMs)                          (trimEndMs)
```

分区(对应 onDraw 的几层):

| 区域 | 大概位置 | 画什么 | 用哪个 Paint |
| --- | --- | --- | --- |
| ① 刻度区 | 顶部 ~0–24dp | 每隔固定秒画竖线 + `mm:ss` 文字 | `rulerPaint` / `textPaint` |
| ② 缩略图轨道 | 中部 ~24–120dp | 每张占位色块 + 中央序号 | `thumbPaint` / `textPaint` |
| ③ trim 手柄 | 选区左右边缘,贯穿高度 | 左右两个手柄;选区外压暗 | `handlePaint` |

关键映射关系(实现时绕不开):

- **时间 → x 像素**:`x = timeMs / 1000f * pixelsPerSecond * scale - scrollX`(`pixelsPerSecond` 自定基准,如每秒 60px)
- **x → 时间**:上式反解,用于触摸命中手柄 / 定位
- **内容总宽**:`durationSec * pixelsPerSecond * scale`(可能 > 屏幕宽,所以才需要 `scrollX`)

## 关键文件入口

| 文件 | 作用 |
| --- | --- |
| [MainActivity.kt](app/src/main/java/com/example/insta360learningcustomview/MainActivity.kt) | 入口 Activity,生成假数据并 `setData` 给控件 |
| [activity_main.xml](app/src/main/res/layout/activity_main.xml) | 布局:全宽、高 120dp、浅灰背景的 TimelineView |
| [TimelineData.kt](app/src/main/java/com/example/insta360learningcustomview/data/TimelineData.kt) | 假数据:`VideoClip` / `ThumbnailStub` 数据类 + `TimelineDataFactory`(默认 60 秒,每秒一张纯色块+序号占位,无视频解码) |
| [TimelineView.kt](app/src/main/java/com/example/insta360learningcustomview/widget/TimelineView.kt) | **你要写的地方**:三个构造函数 / Paint / `scale` / `scrollX` / `clip` 已备好,四个核心方法只留 TODO |

## 四个 TODO 练什么 & 涉及的 API

只列 API 名,具体实现自己查、自己写。

### `onMeasure` — 测量流程 / MeasureSpec
解析父容器约束 → 算内容理想宽度(缩略图数 × 单宽 × `scale`)→ 按约束收敛 → `setMeasuredDimension`。
- `MeasureSpec.getMode` / `getSize` / `EXACTLY` / `AT_MOST` / `UNSPECIFIED`
- `setMeasuredDimension`、`resolveSize`、`getPaddingLeft/Right`

### `onDraw` — Canvas 绘制 / 坐标系 / 平移裁剪
分层画:缩略图轨道 → 刻度尺竖线 → 时间文字(mm:ss)→ 左右 trim 手柄 + 区间高亮。
- `Canvas.drawRect` / `drawLine` / `drawText` / `drawRoundRect`
- `Canvas.save` / `restore` / `translate` / `clipRect`
- `Paint.measureText` / `getTextBounds` / `Paint.FontMetrics` / `setColor` / `setTextAlign`

### `onInterceptTouchEvent` — 事件分发 / 滑动冲突(外部拦截法)
DOWN 不拦截;MOVE 时比较 dx / dy,判定为水平滑动才拦截给自己。
- `MotionEvent.getActionMasked` / `getX` / `getY`
- `ViewConfiguration.get(context).scaledTouchSlop`、`Math.abs`

### `onTouchEvent` — 事件处理 / 手势 / 与父容器协作
命中手柄则拖动改 trim 区间;拖空白改 `scrollX`(clamp);消费手势时阻止父容器拦截;抬手做 fling。
- `getParent()?.requestDisallowInterceptTouchEvent(boolean)`
- `invalidate()` / `postInvalidateOnAnimation()`
- `VelocityTracker`、`OverScroller`、`GestureDetector` / `ScaleGestureDetector`
- `ViewConfiguration` 的 `scaledMinimumFlingVelocity` / `scaledMaximumFlingVelocity`

## 已知约定(踩坑提醒)

- **为什么继承 `FrameLayout` 而不是 `View`**:`onInterceptTouchEvent` 是 `ViewGroup` 的方法,普通 `View` 没有。
  本控件没有子 View,内容全靠自己在 `onDraw` 里用 Canvas 画,所以 `init()` 里调了 `setWillNotDraw(false)` 强制走 `onDraw`。
- **占位文字**:`onDraw` 里那行 `canvas.drawText("TODO: 我来实现绘制", ...)` 是脚手架占位,等你开始画真实内容后删掉。
- **临时 super 兜底**:`onMeasure` / `onTouchEvent` / `onInterceptTouchEvent` 当前直接调 `super`,只是为了能跑;实现时替换成你自己的逻辑。

---

## 练习二:Glide 图片库

补全 Glide 练习页里的加载调用。运行后所有 ImageView 是空的浅灰格子,把 Glide 代码写上就能看到图。

> 加载的是 picsum.photos 在线图,**需要联网**(Manifest 已加 `INTERNET` 权限)。

### 关键文件入口

| 文件 | 作用 |
| --- | --- |
| [GlideDemoActivity.kt](app/src/main/java/com/example/insta360learningcustomview/glide/GlideDemoActivity.kt) | **你要写的地方**:5 个场景的加载方法,方法签名 + TODO,空着 |
| [GlideImageAdapter.kt](app/src/main/java/com/example/insta360learningcustomview/glide/GlideImageAdapter.kt) | **你要写的地方**:`onBindViewHolder` 里的列表项加载留空 |
| [GlideSampleData.kt](app/src/main/java/com/example/insta360learningcustomview/glide/GlideSampleData.kt) | 示例图片地址:正常图 / 故意写坏的地址(测 error)/ 列表地址 |
| [activity_glide_demo.xml](app/src/main/res/layout/activity_glide_demo.xml) | 5 个场景的界面;`placeholder_gray` / `error_red` 两个 drawable 供占位/失败图引用 |

### 五个 TODO 练什么 & 涉及的 API

只列 API 名,具体实现自己查、自己写。

| 场景 | 练什么 | API |
| --- | --- | --- |
| ① 基础加载 | Glide 三段式 | `Glide.with(...).load(url).into(imageView)` |
| ② 占位图 + 失败图 | 加载前/失败的兜底图(用坏地址触发) | `.placeholder(R.drawable.placeholder_gray)` / `.error(R.drawable.error_red)` |
| ③ 圆形裁剪 | 内置变换 | `.circleCrop()`(或 `.transform(CircleCrop())`) |
| ④ 圆角 | RoundedCorners 变换 + dp→px | `.transform(RoundedCorners(radiusPx))` |
| ⑤ 列表加载 | 复用 + 缓存 + 生命周期 | `Glide.with(holder.itemView).load(url)...into(...)` |

> 想清楚:列表里为什么用 `holder.itemView` 作为 `with(...)` 的参数?快速滑动时为什么不会错图?

## 运行

```bash
./gradlew :app:assembleDebug      # 编译
./gradlew :app:installDebug       # 装到已连接的设备/模拟器
```
