package com.example.mockjavatop7realwork.b_basics.q1_multi_type

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mockjavatop7realwork.R

class MultiTypeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val items = mutableListOf<MyModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.q1_multi_type)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        items.add(MyModel(1, "1-123456"))
        items.add(MyModel(2, "https://picsum.photos/200/200?random=1"))
        items.add(MyModel(1, "2-123456"))
        items.add(MyModel(2, "https://picsum.photos/200/200?random=2"))
        items.add(MyModel(1, "3-123456"))
        items.add(MyModel(2, "https://picsum.photos/200/200?random=3"))

        recyclerView.adapter = MultiAdapter(items)
    }
}

data class MyModel(val type: Int, val content: String)

class MultiAdapter(private val items: List<MyModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 1
        private const val TYPE_IMAGE = 2
    }

    override fun getItemViewType(position: Int): Int {
        // TODO: 判断是文字还是图片
        return items[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // TODO: 根据viewType创建不同布局
        // android.R.layout.simple_list_item_1
        // R.layout.q1_item_image
        val inflater = LayoutInflater.from(parent.context);
        val textView = inflater.inflate(android.R.layout.simple_list_item_1, parent);
        val imgView = inflater.inflate(R.layout.q1_item_image, parent);
        return when(viewType){
            TYPE_TEXT ->  TextHolder(textView);
            TYPE_IMAGE ->  ImageHolder(imgView);
            else -> throw Exception("unknown type");
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // TODO: 绑定数据
        when(holder){
            is TextHolder -> {
                holder.textView.text = items[position].content;
            }
            is ImageHolder -> {
                // 使用Glide图片库、否则需要自己写bitmap加载图片
                Glide.with(holder.imageView.context).load(items[position].content).into(holder.imageView)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class TextHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }

    class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}
