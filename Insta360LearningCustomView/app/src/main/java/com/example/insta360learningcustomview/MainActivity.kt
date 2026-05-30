package com.example.insta360learningcustomview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.insta360learningcustomview.data.TimelineDataFactory
import com.example.insta360learningcustomview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 生成一段 60 秒视频的假数据，喂给自定义 View。
        // 真正怎么用这份数据（measure 出多宽、onDraw 怎么画）由 TimelineView 内部实现。
        val clip = TimelineDataFactory.createFakeClip(durationMs = 60_000L)
        binding.timelineView.setData(clip)
    }
}
