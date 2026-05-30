package com.example.insta360learningcustomview.glide

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.insta360learningcustomview.databinding.ItemGlideImageBinding

/**
 * 列表加载场景的 Adapter(脚手架)。
 *
 * ViewHolder / 复用机制已经写好,**只有 onBindViewHolder 里的图片加载留空**给你写。
 * 列表加载是 Glide 的重点:View 会被回收复用,要靠 Glide 的 with(生命周期)+ 缓存来正确处理。
 */
class GlideImageAdapter(
    private val urls: List<String>,
) : RecyclerView.Adapter<GlideImageAdapter.VH>() {

    class VH(val binding: ItemGlideImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGlideImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = urls[position]
        // TODO（知识点：列表中的 Glide / 复用 / 缓存）
        //   把 url 加载进 holder.binding.ivItem。
        //   API:Glide.with(holder.itemView).load(url).placeholder(R.drawable.placeholder_gray).into(holder.binding.ivItem)
        //   想一想:为什么用 holder.itemView 作为 with 的参数?快速滑动时为什么不会错图?(提示:Glide 会自动 clear 复用的 target)
    }

    override fun getItemCount(): Int = urls.size
}
