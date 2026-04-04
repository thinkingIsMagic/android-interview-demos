# Retrofit 网络请求

## 题目描述
使用 Retrofit 请求 JSONPlaceholder 的 /todos/1 接口并展示结果。

## 关键知识点
- Retrofit 创建（baseUrl、addConverterFactory）
- 接口定义（@GET、Call）
- 异步调用（enqueue）
- 回调处理（onResponse、onFailure）

## 参考答案

### Retrofit 创建与请求
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.q7_retrofit)

    tvResult = findViewById(R.id.tvResult)

    // 创建 Retrofit
    val retrofit = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 创建服务
    val service = retrofit.create(ApiService::class.java)

    // 发起请求
    service.getTodo().enqueue(object : Callback<Todo> {
        override fun onResponse(call: Call<Todo>, response: Response<Todo>) {
            if (response.isSuccessful) {
                val todo = response.body()
                tvResult.text = "id: ${todo?.id}\ntitle: ${todo?.title}\ncompleted: ${todo?.completed}"
            } else {
                tvResult.text = "请求失败: ${response.code()}"
            }
        }

        override fun onFailure(call: Call<Todo>, t: Throwable) {
            tvResult.text = "网络错误: ${t.message}"
        }
    })
}
```

### 依赖添加（build.gradle.kts）
```kotlin
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
```

## 注意事项
- 需要添加网络权限 `<uses-permission android:name="android.permission.INTERNET"/>`
- 异步请求不会阻塞主线程
- 注意处理网络错误情况
