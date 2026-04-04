package com.example.mockjavatop7realwork.b_basics.q7_retrofit

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mockjavatop7realwork.R
import retrofit2.Call
import retrofit2.http.GET

class RetrofitActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.q7_retrofit)

        tvResult = findViewById(R.id.tvResult)

        // TODO: 创建Retrofit、ApiService，调用接口
    }
}

interface ApiService {
    @GET("todos/1")
    fun getTodo(): Call<Todo>
}

data class Todo(
    val userId: Int = 0,
    val id: Int = 0,
    val title: String = "",
    val completed: Boolean = false
)
