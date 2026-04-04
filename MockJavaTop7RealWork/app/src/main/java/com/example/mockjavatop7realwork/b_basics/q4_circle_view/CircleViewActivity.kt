package com.example.mockjavatop7realwork.b_basics.q4_circle_view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class CircleViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(CircleView(this))
    }
}

class CircleView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // TODO: 初始化Paint
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // TODO: 获取尺寸，取最小值，setMeasuredDimension
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // TODO: 绘制圆形
    }
}
