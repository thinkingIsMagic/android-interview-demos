package com.example.insta360learningcustomview.glide

/**
 * Glide 练习用的示例图片地址。
 *
 * 用的是 picsum.photos 在线占位图服务,无需自己准备图片。
 * 注意:加载这些需要联网(Manifest 已加 INTERNET 权限)。
 */
object GlideSampleData {

    /** 单张正常图片(用于基础加载、占位图、圆形裁剪、圆角等单图场景)。 */
    const val SINGLE_IMAGE_URL = "https://picsum.photos/seed/glide/600/600"

    /** 一个故意写错的地址,用来验证「错误占位图(error)」是否生效。 */
    const val BROKEN_IMAGE_URL = "https://picsum.photos/this-path-does-not-exist.jpg"

    /** 列表场景:给 RecyclerView 用的一组图片地址(每个 seed 不同,图也不同)。 */
    val LIST_IMAGE_URLS: List<String> = (1..30).map { i ->
        "https://picsum.photos/seed/item$i/300/300"
    }
}
