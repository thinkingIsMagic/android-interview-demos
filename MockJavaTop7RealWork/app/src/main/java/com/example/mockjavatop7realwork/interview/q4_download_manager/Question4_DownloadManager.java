package com.example.mockjavatop7realwork.interview.q4_download_manager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

/**
 * 面试题4：离线下载管理器 - 断点续传 & 并发控制
 *
 * 题目描述：
 * 设计一个能支持100个文件并发下载的任务调度系统
 * 要求支持：断点续传、进度回调、优先级队列
 *
 * 候选人需要实现：
 * 1. DownloadTask - 下载任务类（实现Runnable/Callable）
 * 2. Priority - 优先级队列管理
 * 3. 断点续传 - Range请求头
 * 4. 进度回调防抖
 */
public class Question4_DownloadManager {

    // 线程池配置
    private static final int CORE_POOL_SIZE = 3;  // 核心线程数
    private static final int MAX_POOL_SIZE = 10;   // 最大线程数
    private static final long KEEP_ALIVE_TIME = 60;

    // 进度回调间隔（防抖）
    private static final long PROGRESS_CALLBACK_INTERVAL = 200; // 200ms

    // 线程池
    private ThreadPoolExecutor executor;

    /**
     * 候选人请实现：下载任务类
     *
     * 需求：
     * 1. 实现Runnable接口
     * 2. 支持断点续传（记录startPos）
     * 3. 支持优先级（实现Comparable或使用PriorityBlockingQueue）
     * 4. 进度回调防抖
     */
    public static class DownloadTask implements Runnable, Comparable<DownloadTask> {

        public String url;          // 下载地址
        public String savePath;      // 保存路径
        public long startPos;       // 起始位置（断点续传用）
        public long totalSize;      // 文件总大小
        public int priority;        // 优先级（数字越大优先级越高）

        private DownloadListener listener;

        public DownloadTask(String url, String savePath, int priority) {
            this.url = url;
            this.savePath = savePath;
            this.priority = priority;
            this.startPos = 0; // 从0开始
        }

        @Override
        public void run() {
            // TODO: 实现下载逻辑
            // 提示：
            // 1. 创建HttpURLConnection
            // 2. 设置Range请求头：Range: bytes=startPos-
            // 3. 检查响应码是否为206（部分内容）
            // 4. 使用RandomAccessFile从startPos位置开始写
            // 5. 循环读取并写入，累加进度
            // 6. 实现进度回调防抖
        }

        @Override
        public int compareTo(DownloadTask other) {
            // TODO: 优先级比较（数字大的优先）
            return 0;
        }

        /**
         * TODO: 断点续传 - 检查本地已下载长度
         *
         * @return 已下载的长度
         */
        public long getLocalFileLength() {
            // TODO: 检查本地文件是否存在，返回已下载长度
            return 0;
        }
    }

    /**
     * 候选人请实现：断点续传的核心逻辑
     *
     * @param conn HttpURLConnection
     * @param startPos 起始位置
     */
    private void setupRangeConnection(HttpURLConnection conn, long startPos) {
        // TODO: 设置Range请求头
        // 格式：Range: bytes=startPos-
        // 例如：Range: bytes=1024-
    }

    /**
     * 候选人请实现：从断点位置写入文件
     *
     * @param file 本地文件
     * @param startPos 起始位置
     * @return RandomAccessFile
     */
    private RandomAccessFile createRandomAccessFile(File file, long startPos) throws IOException {
        // TODO: 创建RandomAccessFile，设置读写模式
        // 使用 seek(startPos) 定位到断点位置
        return null;
    }

    /**
     * 候选人请实现：进度回调防抖
     *
     * @param current 已下载长度
     * @param total 总长度
     * @param lastCallbackTime 上次回调时间
     * @return 本次回调后的时间
     */
    private long reportProgress(long current, long total, long lastCallbackTime) {
        // TODO: 实现防抖逻辑
        // 判断当前时间 - lastCallbackTime > PROGRESS_CALLBACK_INTERVAL
        // 才触发真正的回调
        return 0;
    }

    /**
     * 候选人请实现：实现下载管理器
     *
     * @param task 下载任务
     */
    public void enqueueDownload(DownloadTask task) {
        // TODO: 将任务加入线程池执行
    }

    /**
     * 候选人请实现：取消下载
     *
     * @param task 任务
     */
    public void cancelDownload(DownloadTask task) {
        // TODO: 取消正在执行的下载任务
    }

    // ==================== 下载监听器 ====================
    public interface DownloadListener {
        void onProgress(long current, long total);
        void onSuccess(String filePath);
        void onError(Exception e);
    }

    // ==================== 测试代码 ====================
    public static void main(String[] args) {
        System.out.println("=== 面试题4：离线下载管理器 ===");
        System.out.println("需求：断点续传 + 并发控制 + 进度防抖");
        System.out.println();

        // 测试优先级比较
        DownloadTask task1 = new DownloadTask("http://example.com/1.mp4", "/sdcard/1.mp4", 10);
        DownloadTask task2 = new DownloadTask("http://example.com/2.mp4", "/sdcard/2.mp4", 5);

        System.out.println("task1 priority: " + task1.priority);
        System.out.println("task2 priority: " + task2.priority);
        System.out.println("task1.compareTo(task2) = " + task1.compareTo(task2));
    }
}
