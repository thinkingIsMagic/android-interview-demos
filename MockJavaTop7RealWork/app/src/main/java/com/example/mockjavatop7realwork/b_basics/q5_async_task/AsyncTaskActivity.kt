package com.example.mockjavatop7realwork.b_basics.q5_async_task

import android.os.AsyncTask
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mockjavatop7realwork.R

class AsyncTaskActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.q5_async_task)

        tvResult = findViewById(R.id.tvResult)
        progressBar = findViewById(R.id.progressBar)

        DownloadTask(tvResult, progressBar).execute()
    }
}

@Deprecated("AsyncTask已废弃，仅作练习使用")
class DownloadTask(
    private val tvResult: TextView,
    private val progressBar: ProgressBar
) : AsyncTask<Void, Void, String>() {

    override fun onPreExecute() {
        // TODO: 显示进度条
    }

    override fun doInBackground(vararg voids: Void?): String? {
        // TODO: 模拟3秒下载
        Thread.sleep(3000)
        return "下载完成"
    }

    override fun onPostExecute(result: String?) {
        // TODO: 更新UI
    }
}
