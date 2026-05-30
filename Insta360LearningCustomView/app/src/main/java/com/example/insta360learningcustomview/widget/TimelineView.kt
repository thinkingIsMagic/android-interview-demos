package com.example.insta360learningcustomview.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.example.insta360learningcustomview.data.VideoClip

/**
 * 简化版视频剪辑时间轴控件（类似剪映底部的时间轴轨道）。
 *
 * ⚠️ 这是一个「练手脚手架」：成员字段、构造函数、setData 已经搭好，
 * 但 onMeasure / onDraw / onTouchEvent / onInterceptTouchEvent 四个核心方法只留了 TODO，
 * 需要你自己实现，用来练习 Android 的「View 绘制流程」和「事件分发体系」。
 *
 * 为什么继承 FrameLayout（而不是 View）？
 *   因为要练习 onInterceptTouchEvent —— 它是 ViewGroup 的方法，普通 View 没有。
 *   本控件没有子 View，所有内容（轨道/刻度/手柄）都由自己在 onDraw 里用 Canvas 画出来，
 *   所以构造时调用了 setWillNotDraw(false)，强制让 ViewGroup 也走 onDraw。
 */
class TimelineView : FrameLayout {

    // ---------------------------------------------------------------------------------------------
    // 三个构造函数：Kotlin 自定义 View 的经典写法，用 this(...) 逐级委托，最终汇聚到带 defStyleAttr 的那个。
    // 代码里 new 出来走第一个；XML 里声明走第二个（系统传入 attrs）。
    // ---------------------------------------------------------------------------------------------
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init()
    }

    // ---------------------------------------------------------------------------------------------
    // 成员字段：已帮你声明并初始化好，实现时直接用即可。
    // ---------------------------------------------------------------------------------------------

    /** 画刻度尺竖线用的画笔。 */
    private val rulerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
    }

    /** 画时间文字（如 "00:05"）用的画笔。 */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
    }

    /** 画缩略图占位色块用的画笔（onDraw 时按每个 ThumbnailStub 的颜色改 color）。 */
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /** 画左右 trim 手柄用的画笔。 */
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCC00")
        style = Paint.Style.FILL
    }

    /** 占位提示文字的画笔（脚手架阶段用，等你实现 onDraw 后可以删掉）。 */
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textSize = sp(14f)
        textAlign = Paint.Align.CENTER
    }

    /** 缩放比：1f 表示原始比例，> 1 放大（时间轴拉长，每秒占更多像素），< 1 缩小。双指捏合时改它。 */
    private var scale: Float = 1f

    /** 横向偏移：内容向左滚动了多少像素。手指水平拖动 / fling 时改它。 */
    private var scrollX: Float = 0f

    /** 数据源：要展示的那段视频。由外部通过 setData(...) 注入。 */
    private var clip: VideoClip? = null

    /** 屏幕密度，dp/sp 换算用。 */
    private val density: Float = resources.displayMetrics.density

    private fun init() {
        // 本控件是 ViewGroup 但没有子 View，靠自己画。强制开启 onDraw 回调。
        setWillNotDraw(false)
    }

    /**
     * 注入数据源。改了数据后内容宽度可能变化，所以同时触发重新测量 + 重绘。
     */
    fun setData(clip: VideoClip) {
        this.clip = clip
        requestLayout()
        invalidate()
    }

    // =============================================================================================
    // 下面四个方法是你要练的核心，目前都只有 TODO + 占位实现，请自行补全。
    // =============================================================================================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // TODO（知识点：View 绘制三大流程之 measure / MeasureSpec）
        //   1. 用 MeasureSpec.getMode / getSize 解析父容器给的宽高约束（EXACTLY / AT_MOST / UNSPECIFIED）。
        //   2. 计算「内容理想宽度」：缩略图总数 × 每张宽度 × scale（即整条时间轴铺开后有多宽，可能远大于屏幕）。
        //   3. 宽度按约束收敛：EXACTLY 用父给的尺寸；AT_MOST 取 min(理想宽度, 父给上限)；
        //      高度本例固定 120dp，按 XML 的 EXACTLY 处理即可。
        //   4. 调用 setMeasuredDimension(...) 把最终测量结果交回去。
        //   ——这里先临时用 super，保证脚手架能跑；实现时请替换成你自己的测量逻辑。
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // TODO（知识点：Canvas 绘制 / 坐标系 / 平移与裁剪）
        //   分图层从下往上画：
        //   1. 缩略图轨道：遍历 clip.thumbnails，按 timeMs 和 scale 算出每个色块的 x 区间，
        //      thumbPaint.color 设为它的颜色画矩形，再用 textPaint 把 index 序号写在色块中央。
        //   2. 刻度尺：每隔固定秒数画一根竖线（rulerPaint）。
        //   3. 时间文字：在刻度处用 textPaint 画 "mm:ss"（注意 textAlign 已设为 CENTER）。
        //   4. trim 手柄：在 trimStartMs / trimEndMs 对应的 x 处画左右两个手柄（handlePaint），
        //      并把选中区间高亮、区间外压暗。
        //   提示：横向滚动用 canvas.translate(-scrollX, 0f)；只画可见区间可配合 canvas.clipRect 提升性能。

        // —— 脚手架占位：等你开始实现 onDraw 后可删除下面这行 ——
        canvas.drawText(
            "TODO: 我来实现绘制",
            width / 2f,
            height / 2f,
            placeholderPaint,
        )
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // TODO（知识点：事件分发 / 滑动冲突 —— 外部拦截法）
        //   场景：本控件需要水平滑动，但它可能放在一个可垂直滚动的父容器里，二者会抢手势。
        //   思路：DOWN 时不拦截（return false），让子级/自己先拿到；
        //        MOVE 时比较 dx 与 dy，判定为「水平滑动」时返回 true 拦截给自己处理，否则放行。
        //   注意：DOWN 一定不要拦截，否则后续事件序列拿不到。
        //   ——目前直接交给父类默认行为。
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // TODO（知识点：事件处理 / 手势 / 与父容器协作）
        //   1. DOWN：记录按下坐标，判断是否点中了左/右 trim 手柄（命中测试）；
        //      若开始消费手势，调用 parent?.requestDisallowInterceptTouchEvent(true) 阻止父容器拦截。
        //   2. MOVE：
        //        - 拖手柄 → 把 dx 换算成时间，更新 clip 的 trimStart/trimEnd（注意边界与最小区间），invalidate()。
        //        - 拖空白 → 更新 scrollX（注意 clamp 到 [0, 最大可滚动距离]），invalidate()。
        //   3. UP / CANCEL：结束拖拽，必要时做 fling（可配合 VelocityTracker + OverScroller / GestureDetector）。
        //   4. 消费了事件就 return true。
        //   ——目前直接交给父类默认行为。
        return super.onTouchEvent(event)
    }

    // ---------------------------------------------------------------------------------------------
    // 小工具：dp / sp 转 px，字段初始化和实现时都会用到。
    // ---------------------------------------------------------------------------------------------
    private fun dp(value: Float): Float = value * density

    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
}
