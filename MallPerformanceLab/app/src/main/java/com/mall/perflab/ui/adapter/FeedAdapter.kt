package com.mall.perflab.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mall.perflab.R
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.data.model.FeedItem
import com.mall.perflab.data.model.ProductFeedItem

/**
 * Feed流Adapter
 *
 * 支持：
 * - 商品Feed
 * - 广告Banner Feed
 *
 * 优化点：
 * 1. ViewHolder复用
 * 2. DiffUtil增量更新
 * 3. 图片占位策略（useImagePlaceholder）
 */
class FeedAdapter(
    /**
     * 图片加载回调（由外部注入，支持不同实现）
     */
    private val imageLoader: (ImageView, String) -> Unit
) : ListAdapter<FeedItem, FeedAdapter.FeedViewHolder>(FeedDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProductFeedItem -> TYPE_PRODUCT
            else -> TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_feed_product, parent, false)
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ==================== ViewHolder ====================

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 布局元素
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvImagePlaceholder: TextView = itemView.findViewById(R.id.tvImagePlaceholder)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvOriginalPrice: TextView = itemView.findViewById(R.id.tvOriginalPrice)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)

        fun bind(item: FeedItem) {
            when (item) {
                is ProductFeedItem -> bindProduct(item)
                else -> { /* 其他类型 */ }
            }
        }

        private fun bindProduct(item: ProductFeedItem) {
            // 商品名称
            tvProductName.text = item.product.name

            // 价格
            tvPrice.text = String.format("¥%.2f", item.product.price)
            tvOriginalPrice.text = String.format("¥%.2f", item.product.originalPrice)
            tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            // 点赞评论
            tvLikeCount.text = item.likeCount.toString()
            tvCommentCount.text = item.commentCount.toString()

            // 图片加载
            val imageUrl = item.product.imageUrl

            // 优化点：图片占位策略
            if (FeatureToggle.useImagePlaceholder()) {
                // 显示占位UI（骨架屏/Loading）
                tvImagePlaceholder.visibility = View.VISIBLE
                ivProduct.setImageDrawable(null)

                // 异步加载图片
                imageLoader(ivProduct, imageUrl)
            } else {
                // 直接加载
                tvImagePlaceholder.visibility = View.GONE
                imageLoader(ivProduct, imageUrl)
            }
        }
    }

    // ==================== DiffUtil ====================

    companion object {
        private const val TYPE_PRODUCT = 0
    }

    class FeedDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            return oldItem == newItem
        }
    }
}
