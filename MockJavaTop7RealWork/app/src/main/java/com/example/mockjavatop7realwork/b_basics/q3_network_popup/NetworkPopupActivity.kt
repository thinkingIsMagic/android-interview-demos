package com.example.mockjavatop7realwork.b_basics.q3_network_popup

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mockjavatop7realwork.R

class NetworkPopupActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var manager: NetworkPopupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.q3_network_popup)

        tvStatus = findViewById(R.id.tvStatus)
        val btnGood = findViewById<Button>(R.id.btnGood)
        val btnNormal = findViewById<Button>(R.id.btnNormal)
        val btnBad = findViewById<Button>(R.id.btnBad)

        manager = NetworkPopupManager()

        btnGood.setOnClickListener {
            manager.onNetworkChanged(NetworkState.GOOD)
            tvStatus.text = "当前状态: GOOD"
        }

        btnNormal.setOnClickListener {
            manager.onNetworkChanged(NetworkState.NORMAL)
            tvStatus.text = "当前状态: NORMAL"
        }

        btnBad.setOnClickListener {
            manager.onNetworkChanged(NetworkState.BAD)
            tvStatus.text = "当前状态: BAD"
        }
    }
}

enum class NetworkState {
    GOOD, NORMAL, BAD
}

class NetworkPopupManager {
    // TODO: 定义常量 COOL_DOWN, THREE_MIN, THIRTY_SEC

    // TODO: 定义队列 badQueue, allQueue

    // TODO: 定义 lastPopupTime

    fun onNetworkChanged(state: NetworkState) {
        val now = System.currentTimeMillis()
        // TODO: 加入队列，清理超时数据，检查冷却期，判断条件，触发弹窗
    }

    private fun showPopup() {
        println("弹窗触发！")
    }
}
