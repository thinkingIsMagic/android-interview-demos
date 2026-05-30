# Insta360LearningCustomView

一个**练手项目**:手写一个简化版「视频剪辑时间轴控件」(类似剪映底部的时间轴轨道),
用来学习 Android 传统 View 体系的 **绘制流程** 和 **事件分发体系**。

> 技术栈:Android + Kotlin,传统 View 体系(非 Compose),单 module。

## 这个仓库的玩法

脚手架已经搭好(工程配置、Activity、布局、假数据、类骨架),**核心逻辑留空给你自己写**。
你要做的就是把 [TimelineView.kt](app/src/main/java/com/example/insta360learningcustomview/widget/TimelineView.kt)
里四个方法的 TODO 补全。目前工程可以直接编译运行,控件区域会显示一行占位文字 `TODO: 我来实现绘制`。

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

## 运行

```bash
./gradlew :app:assembleDebug      # 编译
./gradlew :app:installDebug       # 装到已连接的设备/模拟器
```
