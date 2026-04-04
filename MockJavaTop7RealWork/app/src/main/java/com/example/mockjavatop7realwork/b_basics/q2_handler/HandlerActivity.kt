package com.example.mockjavatop7realwork.b_basics.q2_handler

import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mockjavatop7realwork.R

class HandlerActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private var handler: Handler? = null
    private var count = 0
    private val maxCount = 10

    private val runnable = Runnable {
        // TODO: 更新UI，count++，判断是否继续延时
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.q2_handler)
        tvTime = findViewById(R.id.tvTime)

        // TODO: 创建Handler，启动定时任务
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: 移除回调
        handler?.removeCallbacks(runnable)
    }
}
