# 面试题6：超长图查看器 - 答案参考

## 1. 初始化解码器

```java
public void init(InputStream is) throws IOException {
    mDecoder = BitmapRegionDecoder.newInstance(is, false);
    mImageWidth = mDecoder.getWidth();
    mImageHeight = mDecoder.getHeight();

    // 初始显示区域
    mRect.set(0, 0,
        Math.min(mImageWidth, getWidth()),
        Math.min(mImageHeight, getHeight()));
}
```

## 2. 区域解码绘制

```java
@Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    if (mDecoder == null) return;

    // 只解码当前可见区域
    Bitmap bitmap = mDecoder.decodeRegion(mRect, mOptions);
    if (bitmap != null) {
        canvas.drawBitmap(bitmap, 0, 0, null);
        bitmap.recycle();
    }
}
```

## 3. 滑动处理

```java
private void onScroll(float dx, float dy) {
    mRect.offset((int) -dx, (int) -dy);

    // 边界检查
    if (mRect.left < 0) {
        mRect.left = 0;
        mRect.right = Math.min(mImageWidth, mRect.width());
    }
    if (mRect.top < 0) {
        mRect.top = 0;
        mRect.bottom = Math.min(mImageHeight, mRect.height());
    }
    if (mRect.right > mImageWidth) {
        mRect.right = mImageWidth;
        mRect.left = Math.max(0, mImageWidth - mRect.width());
    }
    if (mRect.bottom > mImageHeight) {
        mRect.bottom = mImageHeight;
        mRect.top = Math.max(0, mImageHeight - mRect.height());
    }

    invalidate();
}
```

## 4. 缩放处理

```java
private static final float MIN_SCALE = 1.0f;
private static final float MAX_SCALE = 4.0f;

private void onScale(float scaleFactor, float focusX, float focusY) {
    // 更新缩放比例
    float newScale = mScale * scaleFactor;
    newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));

    if (newScale != mScale) {
        // 计算缩放后的区域（以焦点为中心）
        float scaleChange = newScale / mScale;
        int newWidth = (int) (mRect.width() * scaleChange);
        int newHeight = (int) (mRect.height() * scaleChange);

        int newLeft = (int) (focusX - (focusX - mRect.left) * scaleChange);
        int newTop = (int) (focusY - (focusY - mRect.top) * scaleChange);

        mRect.set(newLeft, newTop, newLeft + newWidth, newTop + newHeight);

        // 边界检查
        if (mRect.left < 0) mRect.offset(-mRect.left, 0);
        if (mRect.top < 0) mRect.offset(0, -mRect.top);
        if (mRect.right > mImageWidth) mRect.offset(mImageWidth - mRect.right, 0);
        if (mRect.bottom > mImageHeight) mRect.offset(0, mImageHeight - mRect.bottom);

        mScale = newScale;
        invalidate();
    }
}
```
