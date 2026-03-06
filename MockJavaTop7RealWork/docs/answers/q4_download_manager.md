# 面试题4：离线下载管理器 - 答案参考

## 1. 断点续传核心实现

```java
@Override
public void run() {
    HttpURLConnection conn = null;
    RandomAccessFile raf = null;
    InputStream is = null;

    try {
        conn = (HttpURLConnection) new URL(url).openConnection();

        // 断点续传：设置Range请求头
        conn.setRequestProperty("Range", "bytes=" + startPos + "-");
        conn.connect();

        if (conn.getResponseCode() == 206) {
            raf = new RandomAccessFile(savePath, "rw");
            raf.seek(startPos);

            is = conn.getInputStream();
            byte[] buffer = new byte[8192];
            int len;
            long lastTime = 0;

            while ((len = is.read(buffer)) != -1) {
                raf.write(buffer, 0, len);
                startPos += len;

                // 进度回调防抖
                long now = System.currentTimeMillis();
                if (now - lastTime > 200) {
                    listener.onProgress(startPos, totalSize);
                    lastTime = now;
                }
            }

            listener.onSuccess(savePath);
        }
    } catch (Exception e) {
        listener.onError(e);
    } finally {
        try { if (is != null) is.close(); } catch (Exception e) {}
        try { if (raf != null) raf.close(); } catch (Exception e) {}
        if (conn != null) conn.disconnect();
    }
}
```

## 2. 优先级比较

```java
@Override
public int compareTo(DownloadTask other) {
    // 数字大的优先
    return Integer.compare(other.priority, this.priority);
}
```
