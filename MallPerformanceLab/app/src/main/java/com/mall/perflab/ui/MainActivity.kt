package com.mall.perflab.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mall.perflab.R
import com.mall.perflab.core.config.FeatureToggle
import com.mall.perflab.core.perf.PerformanceTracker
import com.mall.perflab.core.perf.TraceLogger
import com.mall.perflab.data.model.FeedItem
import com.mall.perflab.data.model.MallData
import com.mall.perflab.data.model.MarketingFloor
import com.mall.perflab.data.repository.MallRepository
import com.mall.perflab.ui.adapter.FeedAdapter
import com.mall.perflab.ui.adapter.MarketingFloorAdapter
import kotlinx.coroutines.*
import java.net.URL

/**
 * Mall Performance Lab - 商城首页
 *
 * 性能链路打点：
 * 1. onCreate -> 页面创建
 * 2. data_loaded -> 首屏数据到达
 * 3. content_ready -> 首屏渲染完成
 * 4. interactive -> 首屏可交互
 *
 * Baseline vs Optimized对比：
 * - Baseline: 直接请求，不使用缓存/预请求
 * - Optimized: 使用缓存、预请求、预创建
 */
class MainActivity : AppCompatActivity() {

    // ==================== 组件 ====================

    private lateinit var repository: MallRepository

    // Adapter
    private lateinit var floorAdapter: MarketingFloorAdapter
    private lateinit var feedAdapter: FeedAdapter

    // View引用
    private lateinit var rvMall: RecyclerView
    private lateinit var tvMode: TextView
    private lateinit var btnToggleMode: Button
    private lateinit var btnShowReport: Button
    private lateinit var btnClear: Button
    private lateinit var loadMorePanel: LinearLayout
    private lateinit var metricsPanel: LinearLayout

    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ==================== 生命周期 ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initAdapter()
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ==================== 初始化 ====================

    private fun initViews() {
        rvMall = findViewById(R.id.rvMall)
        tvMode = findViewById(R.id.tvMode)
        btnToggleMode = findViewById(R.id.btnToggleMode)
        btnShowReport = findViewById(R.id.btnShowReport)
        btnClear = findViewById(R.id.btnClear)
        loadMorePanel = findViewById(R.id.loadMorePanel)
        metricsPanel = findViewById(R.id.metricsPanel)

        // 初始化仓库
        repository = MallRepository(this)

        // 模式切换
        btnToggleMode.setOnClickListener {
            val newMode = FeatureToggle.toggleMode()
            updateModeUI(newMode)
            // 重新加载数据
            loadData()
        }

        // 显示报告
        btnShowReport.setOnClickListener {
            showPerformanceReport()
        }

        // 清空缓存
        btnClear.setOnClickListener {
            PerformanceTracker.clear()
            repository.clearCache()
            Toast.makeText(this, "缓存已清空", Toast.LENGTH_SHORT).show()
        }

        // RecyclerView滚动监听（加载更多）
        rvMall.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)

                // 滚动到底部时加载更多
                val layoutManager = rv.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                if (lastVisible >= totalItemCount - 3 && repository.hasMoreFeed()) {
                    loadMoreFeed()
                }
            }
        })
    }

    private fun initAdapter() {
        // 楼层Adapter
        floorAdapter = MarketingFloorAdapter()

        // Feed Adapter（注入图片加载逻辑）
        feedAdapter = FeedAdapter { imageView, url ->
            // 【优化点】图片加载
            // 这里简化处理，实际项目应使用Glide/Picasso
            // 优化策略：异步加载、缓存、采样
            loadImageAsync(imageView, url)
        }

        // 组合Adapter（使用RecyclerView的另外方式，这里简化为单一列表）
        // 实际实现中，楼层和Feed可以用一个列表或分开
        // 这里简化：楼层 + Feed = 完整列表
        val adapter = CombinedAdapter(floorAdapter, feedAdapter)
        rvMall.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = adapter
        }
    }

    // ==================== 数据加载 ====================

    /**
     * 加载首页数据
     */
    private fun loadData() {
        PerformanceTracker.begin("page_onCreate")

        // 【打点】开始请求首屏
        PerformanceTracker.begin("perf_mall_first_data")

        scope.launch {
            try {
                // 异步获取数据
                val (data, latency) = repository.getMallData()

                PerformanceTracker.end("perf_mall_first_data", "network")

                if (data != null) {
                    // 【打点】数据到达
                    val dataReadyTime = System.currentTimeMillis()

                    // 更新UI
                    updateMallData(data)

                    // 【打点】渲染完成
                    PerformanceTracker.begin("perf_mall_first_content")
                    rvMall.post {
                        PerformanceTracker.end("perf_mall_first_content", "render")

                        // 【打点】可交互
                        PerformanceTracker.begin("perf_mall_interactive")
                        rvMall.postDelayed({
                            PerformanceTracker.end("perf_mall_interactive", "interactive")
                            updateMetricsUI()
                        }, 100)
                    }
                }
            } catch (e: Exception) {
                TraceLogger.e("PAGE", "数据加载失败", e)
            }
        }

        PerformanceTracker.end("page_onCreate", "page_init")
    }

    /**
     * 加载更多Feed
     */
    private fun loadMoreFeed() {
        loadMorePanel.visibility = View.VISIBLE

        scope.launch {
            val (items, latency) = repository.loadMoreFeed()

            if (items.isNotEmpty()) {
                feedAdapter.submitList(feedAdapter.currentList + items)
            }

            loadMorePanel.visibility = View.GONE
        }
    }

    // ==================== UI更新 ====================

    private fun updateMallData(data: MallData) {
        // 更新楼层列表
        floorAdapter.submitList(data.marketingFloors)

        // 更新Feed列表
        feedAdapter.submitList(data.feedItems)
    }

    private fun updateModeUI(mode: FeatureToggle.Mode) {
        val modeText = if (mode == FeatureToggle.Mode.BASELINE) "[BASELINE]" else "[OPTIMIZED]"
        tvMode.text = modeText

        val colorRes = if (mode == FeatureToggle.Mode.BASELINE) {
            android.R.color.holo_red_light
        } else {
            android.R.color.holo_green_light
        }
        tvMode.setTextColor(getColor(colorRes))
    }

    private fun updateMetricsUI() {
        metricsPanel.removeAllViews()

        // 读取最近的打点记录
        val records = PerformanceTracker.getAllRecords()
        val keyMetrics = listOf(
            "perf_mall_first_data",
            "perf_mall_first_content",
            "perf_mall_interactive"
        )

        keyMetrics.forEach { metric ->
            val record = records.find { it.name == metric }
            if (record != null) {
                val duration = record.duration / 1_000_000

                val textView = TextView(this).apply {
                    text = "${metric.split("_").last()}: ${duration}ms"
                    setPadding(16, 8, 16, 8)
                    textSize = 12f
                }
                metricsPanel.addView(textView)
            }
        }
    }

    // ==================== 报告 ====================

    private fun showPerformanceReport() {
        val report = PerformanceTracker.dump()

        AlertDialog.Builder(this)
            .setTitle("性能报告")
            .setMessage(report)
            .setPositiveButton("确定", null)
            .setNeutralButton("复制") { _, _ ->
                // 复制到剪贴板
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("perf_report", report)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ==================== 图片加载（简化版） ====================

    /**
     * 异步加载图片
     * 【优化点】异步 + 线程池，避免阻塞主线程
     */
    private fun loadImageAsync(imageView: ImageView, url: String) {
        scope.launch(Dispatchers.IO) {
            try {
                // 模拟网络请求
                val bitmap = try {
                    BitmapFactory.decodeStream(URL(url).openStream())
                } catch (e: Exception) {
                    // 加载失败
                    null
                }

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                TraceLogger.e("IMAGE", "加载失败: $url", e)
            }
        }
    }
}

/**
 * 组合Adapter（楼层 + Feed）
 *
 * 简化实现：将楼层和Feed合并为一个列表渲染
 */
class CombinedAdapter(
    private val floorAdapter: MarketingFloorAdapter,
    private val feedAdapter: FeedAdapter
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Item类型
    private const val TYPE_FLOOR_HEADER = 100
    private const val TYPE_FEED = 200

    // 列表数据
    private val items = mutableListOf<Pair<Int, Any>>()

    fun setFloors(floors: List<MarketingFloor>) {
        items.clear()
        floors.forEach { items.add(TYPE_FLOOR_HEADER to it) }
        notifyDataSetChanged()
    }

    fun appendFeeds(feeds: List<FeedItem>) {
        val startPos = items.size
        feeds.forEach { items.add(TYPE_FEED to it) }
        notifyItemRangeInserted(startPos, feeds.size)
    }

    override fun getItemViewType(position: Int): Int = items[position].first

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FLOOR_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_banner_floor, parent, false)
                object : RecyclerView.ViewHolder(view) {}
            }
            TYPE_FEED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_feed_product, parent, false)
                object : RecyclerView.ViewHolder(view) {}
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position].second) {
            is MarketingFloor -> {
                // 简化渲染
            }
            is FeedItem -> {
                // Feed已在FeedAdapter中渲染
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
