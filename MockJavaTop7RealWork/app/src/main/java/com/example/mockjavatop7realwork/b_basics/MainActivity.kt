package com.example.mockjavatop7realwork.b_basics

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.mockjavatop7realwork.R
import com.example.mockjavatop7realwork.b_basics.q1_multi_type.MultiTypeActivity
import com.example.mockjavatop7realwork.b_basics.q2_handler.HandlerActivity
import com.example.mockjavatop7realwork.b_basics.q3_network_popup.NetworkPopupActivity
import com.example.mockjavatop7realwork.b_basics.q4_circle_view.CircleViewActivity
import com.example.mockjavatop7realwork.b_basics.q5_async_task.AsyncTaskActivity
import com.example.mockjavatop7realwork.b_basics.q6_draggable_view.DraggableViewActivity
import com.example.mockjavatop7realwork.b_basics.q7_retrofit.RetrofitActivity

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val titles = listOf(
        "1. RecyclerView 多类型 Item",
        "2. Handler 延时任务 & UI线程",
        "3. 网络状态触发弹窗",
        "4. 自定义 View 测量 & 绘制",
        "5. 异步任务 & UI 更新",
        "6. 自定义 View 触摸交互",
        "7. Retrofit 网络请求"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val intent = when (position) {
                0 -> Intent(this, MultiTypeActivity::class.java)
                1 -> Intent(this, HandlerActivity::class.java)
                2 -> Intent(this, NetworkPopupActivity::class.java)
                3 -> Intent(this, CircleViewActivity::class.java)
                4 -> Intent(this, AsyncTaskActivity::class.java)
                5 -> Intent(this, DraggableViewActivity::class.java)
                6 -> Intent(this, RetrofitActivity::class.java)
                else -> return@OnItemClickListener
            }
            startActivity(intent)
        }
    }
}
