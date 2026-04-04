# RecyclerView 多类型 Item

## 题目描述
实现一个支持多类型 Item 的 RecyclerView，包含文字和图片两种类型。

## 关键知识点
- `getItemViewType()` - 根据位置返回不同的类型
- `onCreateViewHolder()` - 根据 viewType 创建不同的 ViewHolder
- `onBindViewHolder()` - 绑定数据到对应的 ViewHolder

## 参考答案

### getItemViewType 实现
```kotlin
override fun getItemViewType(position: Int): Int {
    return items[position].type
}
```

### onCreateViewHolder 实现
```kotlin
override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when(viewType) {
        TYPE_TEXT -> {
            val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            TextHolder(view)
        }
        TYPE_IMAGE -> {
            val view = inflater.inflate(R.layout.q1_item_image, parent, false)
            ImageHolder(view)
        }
        else -> throw Exception("unknown type")
    }
}
```

### onBindViewHolder 实现
```kotlin
override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when(holder) {
        is TextHolder -> {
            holder.textView.text = items[position].content
        }
        is ImageHolder -> {
            Glide.with(holder.imageView.context)
                .load(items[position].content)
                .into(holder.imageView)
        }
    }
}
```
