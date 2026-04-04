# 自定义 View 测量 & 绘制

## 题目描述
实现一个自定义圆形 View，支持 wrap_content 自适应尺寸。

## 关键知识点
- `onMeasure()` - 测量模式（EXACTLY、AT_MOST、UNSPECIFIED）
- `setMeasuredDimension()` - 设置测量尺寸
- `onDraw()` - Canvas 绑定图形

## 参考答案

### Paint 初始化
```kotlin
init {
    paint.apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
}
```

### onMeasure 实现
```kotlin
override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)

    // 取宽高的最小值，确保是正方形
    val size = minOf(width, height)

    // 处理 wrap_content (AT_MOST)
    val mode = MeasureSpec.getMode(widthMeasureSpec)
    val resultSize = if (mode == MeasureSpec.AT_MOST) {
        minOf(size, 200.dpToPx()) // 默认200px
    } else {
        size
    }

    setMeasuredDimension(resultSize, resultSize)
}

private fun Int.dpToPx(): Int {
    return (this * resources.displayMetrics.density).toInt()
}
```

### onDraw 实现
```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val centerX = width / 2f
    val centerY = height / 2f
    val radius = minOf(centerX, centerY)

    canvas.drawCircle(centerX, centerY, radius, paint)
}
```
