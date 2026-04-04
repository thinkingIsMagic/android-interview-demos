package com.example.mockjavatop7realwork.c_interview.q6_large_image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.BitmapRegionDecoder;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

/**
 * 面试题6：超长图/巨型图片查看器
 *
 * 题目描述：
 * 像微博、知乎那种几万像素高的"清明上河图"
 * 内存限制20MB，如何流畅显示且支持缩放？
 *
 * 核心API：
 * - BitmapRegionDecoder: 区域解码，只加载可见区域
 *
 * 候选人需要实现：
 * 1. 初始化 BitmapRegionDecoder
 * 2. 根据当前显示区域解码对应图片
 * 3. 手势处理（滑动、缩放）
 * 4. 动态计算显示区域
 */
public class Question6_LargeImageViewer extends View {

    // 图片解码器
    private BitmapRegionDecoder mDecoder;

    // 当前显示区域
    private Rect mRect = new Rect();

    // 图片尺寸
    private int mImageWidth;
    private int mImageHeight;

    // 当前缩放比例
    private float mScale = 1.0f;

    // 图片配置
    private BitmapFactory.Options mOptions = new BitmapFactory.Options();

    // 手势检测器
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    // 内部手势监听器（用于回调外部方法）
    private OnScrollListener scrollListener;
    private OnScaleListener scaleListener;

    public Question6_LargeImageViewer(Context context) {
        super(context);
        init();
    }

    public Question6_LargeImageViewer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 使用 RGB_565 格式，节省内存
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;

        // 初始化手势检测器
        mGestureDetector = new GestureDetector(getContext(), new GestureListener());
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    /**
     * 候选人请实现：初始化图片解码器
     *
     * @param is 输入流
     */
    public void init(InputStream is) throws IOException {
        // TODO: 初始化 BitmapRegionDecoder
        // 提示：BitmapRegionDecoder.newInstance(is, false)
    }

    /**
     * 候选人请实现：核心绘制方法
     *
     * 根据当前 mRect 指定的区域，只解码图片的一部分
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: 实现区域解码和绘制
        // 提示：
        // 1. 使用 mDecoder.decodeRegion(mRect, mOptions) 解码当前区域
        // 2. 将解码的 Bitmap 绘制到 Canvas
        // 3. 注意 Bitmap 回收（如果需要）
    }

    /**
     * 候选人请实现：计算显示区域
     *
     * 根据手势和缩放比例，计算当前应该显示的图片区域
     *
     * @param viewWidth View宽度
     * @param viewHeight View高度
     */
    private void calculateDisplayRect(int viewWidth, int viewHeight) {
        // TODO: 计算显示区域
        // 提示：
        // 1. 根据缩放比例计算缩放后的图片尺寸
        // 2. 计算居中显示时的 left, top
        // 3. 确保不超出图片边界
        // 4. 更新 mRect
    }

    /**
     * 候选人请实现：处理滑动
     *
     * @param dx X方向滑动距离
     * @param dy Y方向滑动距离
     */
    private void onScroll(float dx, float dy) {
        // TODO: 处理滑动，更新 mRect
        // 提示：
        // 1. 根据滑动方向调整 mRect
        // 2. 边界检查，不能滑出图片范围
        // 3. 调用 invalidate() 触发重绘
    }

    /**
     * 候选人请实现：处理缩放
     *
     * @param scaleFactor 缩放因子
     * @param focusX 缩放中心X
     * @param focusY 缩放中心Y
     */
    private void onScale(float scaleFactor, float focusX, float focusY) {
        // TODO: 处理缩放
        // 提示：
        // 1. 更新 mScale
        // 2. 边界检查（最大/最小缩放比例）
        // 3. 计算新的显示区域（以焦点为中心）
        // 4. 调用 invalidate() 触发重绘
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO: 分发给手势检测器
        // 提示：依次调用 mScaleGestureDetector.onTouchEvent 和 mGestureDetector.onTouchEvent
        boolean scaleResult = mScaleGestureDetector.onTouchEvent(event);
        boolean gestureResult = mGestureDetector.onTouchEvent(event);
        return scaleResult || gestureResult || super.onTouchEvent(event);
    }

    // ==================== 手势监听器 ====================

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            // 内部调用 onScroll(float, float)
            Question6_LargeImageViewer.this.onScroll(distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // TODO: 双击放大/缩小
            // 提示：切换缩放比例（如 1x -> 2x -> 1x）
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // 内部调用 onScale(float, float, float)
            Question6_LargeImageViewer.this.onScale(
                detector.getScaleFactor(),
                detector.getFocusX(),
                detector.getFocusY()
            );
            return true;
        }
    }

    // ==================== 回调接口 ====================

    public interface OnScrollListener {
        void onScroll(float dx, float dy);
    }

    public interface OnScaleListener {
        void onScale(float scaleFactor, float focusX, float focusY);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        this.scrollListener = listener;
    }

    public void setOnScaleListener(OnScaleListener listener) {
        this.scaleListener = listener;
    }

    /**
     * 获取图片尺寸
     */
    public int getImageWidth() {
        return mImageWidth;
    }

    public int getImageHeight() {
        return mImageHeight;
    }

    // ==================== 测试代码 ====================
    public static void main(String[] args) {
        System.out.println("=== 面试题6：超长图查看器 ===");
        System.out.println("核心API: BitmapRegionDecoder");
        System.out.println("关键点：");
        System.out.println("  1. 只解码当前可见区域");
        System.out.println("  2. 使用 RGB_565 节省内存");
        System.out.println("  3. 配合手势检测实现缩放滑动");
    }
}
