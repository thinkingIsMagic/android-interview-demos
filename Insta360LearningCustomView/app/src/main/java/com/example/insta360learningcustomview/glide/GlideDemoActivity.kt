package com.example.insta360learningcustomview.glide

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.insta360learningcustomview.databinding.ActivityGlideDemoBinding

/**
 * Glide 练习页(脚手架)。
 *
 * ⚠️ 界面、示例图片地址、RecyclerView/Adapter 的接线都搭好了,
 * 但每个场景里**真正的 Glide 加载调用留空给你写**(见各 TODO)。
 * 现在直接运行,所有 ImageView 都是空的浅灰格子;你把 Glide 代码补上后就能看到图。
 *
 * 可用资源:
 *   - 图片地址:[GlideSampleData]
 *   - 占位图:R.drawable.placeholder_gray   失败图:R.drawable.error_red
 */
class GlideDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGlideDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGlideDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadBasic(binding.ivBasic)
        loadWithPlaceholderAndError(binding.ivError)
        loadCircleCrop(binding.ivCircle)
        loadRounded(binding.ivRounded)
        setupList()
    }

    /** 场景①:最基础的加载。 */
    private fun loadBasic(target: ImageView) {
        // TODO（知识点：Glide 三段式 with → load → into）
        //   用 GlideSampleData.SINGLE_IMAGE_URL,把图片加载进 target。
        //   API:Glide.with(context).load(url).into(imageView)
    }

    /** 场景②:占位图 + 失败图。用故意写坏的地址,观察先显示 placeholder、最后落到 error。 */
    private fun loadWithPlaceholderAndError(target: ImageView) {
        // TODO（知识点：占位/失败兜底图）
        //   加载 GlideSampleData.BROKEN_IMAGE_URL,
        //   设置 .placeholder(R.drawable.placeholder_gray) 和 .error(R.drawable.error_red)。
        //   API:RequestBuilder.placeholder(...) / .error(...) / .into(...)
    }

    /** 场景③:圆形裁剪。 */
    private fun loadCircleCrop(target: ImageView) {
        // TODO（知识点：内置变换 transform）
        //   加载 SINGLE_IMAGE_URL 并做圆形裁剪。
        //   API:.circleCrop()（或 .transform(CircleCrop())）
    }

    /** 场景④:圆角。 */
    private fun loadRounded(target: ImageView) {
        // TODO（知识点：RoundedCorners 变换 / dp 转 px）
        //   加载 SINGLE_IMAGE_URL 并加圆角(比如 24dp)。
        //   API:.transform(RoundedCorners(radiusPx))，半径要把 dp 换算成 px。
    }

    /** 场景⑤:RecyclerView 列表。Adapter 已就位,但 item 的图片加载留空(见 GlideImageAdapter)。 */
    private fun setupList() {
        binding.rvList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvList.adapter = GlideImageAdapter(GlideSampleData.LIST_IMAGE_URLS)
        // 列表项的实际加载逻辑在 GlideImageAdapter.onBindViewHolder 的 TODO 里。
    }
}
