package com.mall.perflab.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mall.perflab.R
import com.mall.perflab.data.model.MarketingFloor
import com.mall.perflab.data.model.CouponFloor
import com.mall.perflab.data.model.GridFloor
import com.mall.perflab.data.model.BannerFloor

/**
 * 营销楼层Adapter
 *
 * 支持多种楼层类型：
 * - BannerFloor: 轮播图
 * - CouponFloor: 优惠券
 * - GridFloor: 商品网格
 *
 * 使用DiffUtil优化列表更新
 */
class MarketingFloorAdapter : ListAdapter<MarketingFloor, RecyclerView.ViewHolder>(FloorDiffCallback()) {

    companion object {
        private const val TYPE_BANNER = 0
        private const val TYPE_COUPON = 1
        private const val TYPE_GRID = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is BannerFloor -> TYPE_BANNER
            is CouponFloor -> TYPE_COUPON
            is GridFloor -> TYPE_GRID
            else -> TYPE_BANNER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_BANNER -> {
                val view = inflater.inflate(R.layout.item_banner_floor, parent, false)
                BannerViewHolder(view)
            }
            TYPE_COUPON -> {
                val view = inflater.inflate(R.layout.item_coupon_floor, parent, false)
                CouponViewHolder(view)
            }
            TYPE_GRID -> {
                val view = inflater.inflate(R.layout.item_grid_floor, parent, false)
                GridViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val floor = getItem(position)) {
            is BannerFloor -> (holder as BannerViewHolder).bind(floor)
            is CouponFloor -> (holder as CouponViewHolder).bind(floor)
            is GridFloor -> (holder as GridViewHolder).bind(floor)
        }
    }

    // ==================== ViewHolder ====================

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvBannerTitle)
        private val viewPager: androidx.viewpager.widget.ViewPager = itemView.findViewById(R.id.vpBanner)

        fun bind(floor: BannerFloor) {
            title.text = "Banner楼层 (${floor.banners.size}张)"
            // Banner实现简化：这里不展开ViewPager内容
        }
    }

    class CouponViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvCouponTitle)
        private val rvCoupon: RecyclerView = itemView.findViewById(R.id.rvCoupon)

        fun bind(floor: CouponFloor) {
            title.text = "优惠券 (${floor.coupons.size}张)"
            // Coupon实现简化
        }
    }

    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvGridTitle)
        private val rvGrid: RecyclerView = itemView.findViewById(R.id.rvGrid)

        fun bind(floor: GridFloor) {
            title.text = floor.title
            // Grid实现简化
        }
    }

    // ==================== DiffUtil ====================

    class FloorDiffCallback : DiffUtil.ItemCallback<MarketingFloor>() {
        override fun areItemsTheSame(oldItem: MarketingFloor, newItem: MarketingFloor): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MarketingFloor, newItem: MarketingFloor): Boolean {
            return oldItem == newItem
        }
    }
}
