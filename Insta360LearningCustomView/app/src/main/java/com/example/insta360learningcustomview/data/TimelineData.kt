package com.example.insta360learningcustomview.data

import android.graphics.Color

/**
 * 一张缩略图的占位数据。
 *
 * 练手项目不做真正的视频解码，所以这里不存 Bitmap，而是用「纯色块 + 序号」来代替一帧缩略图。
 * onDraw 时按 [color] 画一个矩形，再把 [index] 画成文字盖在上面即可。
 *
 * @param index      第几张缩略图（从 0 开始），用于在色块上显示序号。
 * @param timeMs     这张缩略图对应视频里的时间点（毫秒），方便和刻度尺对齐。
 * @param color      色块颜色（ARGB）。
 */
data class ThumbnailStub(
    val index: Int,
    val timeMs: Long,
    val color: Int,
)

/**
 * 一段视频剪辑的数据模型（时间轴控件的数据源）。
 *
 * @param durationMs  视频总时长（毫秒）。
 * @param thumbnails  按时间顺序排列的缩略图占位列表。
 * @param trimStartMs trim（裁剪）区间的起点，默认 0。左手柄拖动时改这个值。
 * @param trimEndMs   trim（裁剪）区间的终点，默认等于总时长。右手柄拖动时改这个值。
 */
data class VideoClip(
    val durationMs: Long,
    val thumbnails: List<ThumbnailStub>,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
)

/**
 * 假数据生成器。
 *
 * 模拟一段视频：按固定间隔切出若干「缩略图」占位，每张用不同颜色 + 序号区分，
 * 这样在 TimelineView 里横向铺开后，能直观看到一帧一帧的轨道效果。
 */
object TimelineDataFactory {

    /** 默认每 1 秒生成一张缩略图占位。 */
    private const val DEFAULT_THUMB_INTERVAL_MS = 1_000L

    /**
     * 生成一段假视频数据。
     *
     * @param durationMs        视频总时长（毫秒），默认 60 秒。
     * @param thumbIntervalMs   每隔多久切一张缩略图占位（毫秒）。
     */
    fun createFakeClip(
        durationMs: Long = 60_000L,
        thumbIntervalMs: Long = DEFAULT_THUMB_INTERVAL_MS,
    ): VideoClip {
        val thumbnails = mutableListOf<ThumbnailStub>()
        var index = 0
        var timeMs = 0L
        while (timeMs < durationMs) {
            thumbnails.add(
                ThumbnailStub(
                    index = index,
                    timeMs = timeMs,
                    color = colorForIndex(index),
                )
            )
            index++
            timeMs += thumbIntervalMs
        }
        return VideoClip(durationMs = durationMs, thumbnails = thumbnails)
    }

    /**
     * 给每张缩略图算一个区分度高的颜色：沿色相环均匀取色，序号越大色相越偏移。
     */
    private fun colorForIndex(index: Int): Int {
        val hue = (index * 37f) % 360f
        return Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.85f))
    }
}
