# 自定义 View 触摸交互

## 题目描述
实现一个可拖动的 View，跟随手指移动而移动。

## 关键知识点
- `onTouchEvent()` - 触摸事件处理
- `MotionEvent` - ACTION_DOWN、ACTION_MOVE
- `invalidate()` - 请求重绘

## 参考答案

### Paint 初始化
```kotlin
init {
    paint.apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
}
```

### onDraw 实现
```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawRect(offsetX, offsetY, offsetX + SIZE, offsetY + SIZE, paint)
}
```

### onTouchEvent 实现
```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            lastX = event.x
            lastY = event.y
            return true
        }
        MotionEvent.ACTION_MOVE -> {
            val dx = event.x - lastX
            val dy = event.y - lastY

            offsetX += dx
            offsetY += dy

            // 边界限制
            offsetX = offsetX.coerceIn(0f, (width - SIZE).toFloat())
            offsetY = offsetY.coerceIn(0f, (height - SIZE).toFloat())

            lastX = event.x
            lastY = event.y

            invalidate()
        }
    }
    return super.onTouchEvent(event)
}
```

## 注意事项
- ACTION_DOWN 必须返回 true 标记事件被处理
- 边界限制防止 View 拖出屏幕
