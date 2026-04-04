package com.example.mockjavatop7realwork.b_basics.q6_draggable_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class DraggableViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(DraggableView(this))
    }
}

class DraggableView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var offsetX = 100f
    private var offsetY = 100f
    private var lastX = 0f
    private var lastY = 0f

    companion object {
        private const val SIZE = 200
    }

    init {
        // TODO: 初始化Paint
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // TODO: 绘制红色方块
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // TODO: 处理ACTION_DOWN和ACTION_MOVE，更新坐标，invalidate()
        return super.onTouchEvent(event)
    }
}
