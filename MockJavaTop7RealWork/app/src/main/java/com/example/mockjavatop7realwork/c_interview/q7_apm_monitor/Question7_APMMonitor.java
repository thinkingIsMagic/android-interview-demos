package com.example.mockjavatop7realwork.c_interview.q7_apm_monitor;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * 面试题7：APM性能监控 - 零侵入的OOM/ANR监控
 *
 * 题目描述：
 * 线上环境如何监控内存泄漏？
 * 如果发生OOM，如何获取堆转储文件且不影响用户体验？
 *
 * 候选人需要实现：
 * 1. 内存泄漏监控（LeakCanary原理）
     * 2. OOM监控与堆转储
     * 3. ANR监控（Watchdog机制）
 */
public class Question7_APMMonitor {

    // ============================================================
    // 候选人请实现：内存泄漏监控
    // ============================================================

    /**
     * 候选人请实现：Activity泄漏监控
     *
     * 思路（参考LeakCanary）：
     * 1. 在Activity销毁后，创建WeakReference关联Activity
     * 2. 等待一段时间后（GC后）检查是否被回收
     * 3. 如果未被回收，说明疑似泄漏
     * 4. 触发堆转储分析
     */
    public static class ActivityLeakMonitor {

        private ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
        private Set<WeakReference<Object>> references = new HashSet<>();

        /**
         * TODO: 监听Activity销毁
         *
         * @param activity 要监听的Activity
         */
        public void watch(Object activity) {
            // TODO: 实现监控逻辑
            // 提示：
            // 1. 创建 WeakReference，关联 referenceQueue
            // 2. 存入 references 集合
            // 3. 延迟一段时间后检查是否被回收
            // 4. 未被回收则输出泄漏警告
        }

        /**
         * TODO: 检查是否发生泄漏
         */
        private void checkForLeaks() {
            // TODO: 遍历references检查泄漏
            // 提示：
            // 1. 遍历references集合
            // 2. 使用 referenceQueue.poll() 检查是否被回收
            // 3. 未被回收的触发dumpHprofData
        }
    }

    // ============================================================
    // 候选人请实现：OOM监控
    // ============================================================

    /**
     * 候选人请实现：OOM监控器
     *
     * 需求：
     * 1. 监控JVM内存使用率达到阈值（如80%）
     * 2. 主动触发堆转储 dumpHprofData
     * 3. 在子进程dump，避免影响主进程
     */
    public static class OOMMonitor {

        // 内存阈值（80%）
        private static final float MEMORY_THRESHOLD = 0.8f;

        // 监控间隔
        private static final long CHECK_INTERVAL = 5000; // 5秒

        // 是否正在监控
        private volatile boolean isMonitoring = false;

        /**
         * TODO: 启动OOM监控
         */
        public void startMonitor() {
            // TODO: 启动监控线程
            // 提示：
            // 1. 创建后台线程定期检查内存
            // 2. 使用 Runtime.getRuntime() 获取内存信息
            // 3. 计算已使用内存 / 最大内存
            // 4. 超过阈值时触发 dumpHprofData
        }

        /**
         * TODO: 主动触发堆转储
         */
        private void triggerHeapDump() {
            // TODO: 触发堆转储
            // 提示：
            // 1. 创建新进程执行 dump
            // 2. 使用 Debug.dumpHprofData()
            // 3. 或者使用 Runtime.exec() 执行 am dumpheap
            // 4. 上传后进行后续分析
        }

        /**
         * 获取内存使用率
         */
        private float getMemoryUsage() {
            // TODO: 计算内存使用率
            // 提示：
            // long maxMemory = Runtime.getRuntime().maxMemory();
            // long totalMemory = Runtime.getRuntime().totalMemory();
            // long freeMemory = Runtime.getRuntime().freeMemory();
            // return (float)(totalMemory - freeMemory) / maxMemory;
            return 0;
        }

        /**
         * 停止监控
         */
        public void stopMonitor() {
            isMonitoring = false;
        }
    }

    // ============================================================
    // 候选人请实现：ANR监控（Watchdog机制）
    // ============================================================

    /**
     * 候选人请实现：ANR监控器
     *
     * 思路：
     * 1. 启动后台线程定期向主线程发送消息
     * 2. 如果消息在规定时间内未被处理，说明主线程卡住
     * 3. 收集主线程堆栈信息，判定为ANR
     */
    public static class ANRWatchdog extends Thread {

        // ANR判定时间（5秒）
        private static final int TIMEOUT = 5000;

        private Handler mainHandler = new Handler(Looper.getMainLooper());

        // 标记，用于检测主线程是否处理了消息
        private volatile int tick = 0;

        // 是否运行
        private volatile boolean running = true;

        @Override
        public void run() {
            // TODO: 实现ANR监控循环
            // 提示：
            // 1. 循环执行，直到 running 为 false
            // 2. 记录当前 tick 值
            // 3. 向主线程 post 一个增加 tick 的任务
            // 4. 等待 TIMEOUT 时间
            // 5. 如果 tick 未变化，说明主线程未处理，判定为 ANR
            // 6. 收集主线程堆栈信息
        }

        /**
         * TODO: 收集ANR信息
         */
        private void collectANRInfo() {
            // TODO: 收集ANR信息
            // 提示：
            // 1. 获取主线程：Looper.getMainLooper().getThread()
            // 2. 获取堆栈：getStackTrace()
            // 3. 打印或保存堆栈信息
            // 4. 可选：dump当前所有线程的状态
        }

        /**
         * 停止监控
         */
        public void stopWatchdog() {
            running = false;
            interrupt();
        }
    }

    // ==================== 面试题说明 ====================

    /**
     * 内存泄漏监控原理图
     *
     * Activity.onDestroy() {
     *     // 1. 创建WeakReference关联Activity
     *     ref = new WeakReference(activity, queue);
     *
     *     // 2. 延迟检查
     *     handler.postDelayed({
     *         // 3. GC后检查是否被回收
     *         if (ref.get() == null) {
     *             // 已被回收，无泄漏
     *         } else {
     *             // 4. 未被回收，疑似泄漏
     *             // 触发堆转储分析
     *             dumpHprofData();
     *         }
     *     }, 5000);
     * }
     */

    /**
     * ANR监控原理图
     *
     * new Thread() {
     *     while(running) {
     *         lastTick = tick;
     *         mainHandler.post(() => tick++); // 发送任务到主线程
     *         sleep(TIMEOUT);                  // 等待TIMEOUT
     *         if (tick == lastTick) {          // 主线程未处理
     *             // ANR发生！
     *             dumpStackTrace();
     *         }
     *     }
     * }.start();
     */

    // ==================== 测试代码 ====================
    public static void main(String[] args) {
        System.out.println("=== 面试题7：APM性能监控 ===\n");

        // 测试ANR监控
        System.out.println("--- 启动ANR监控 ---");
        ANRWatchdog watchdog = new ANRWatchdog();
        watchdog.start();

        // 模拟主线程阻塞
        try {
            System.out.println("模拟主线程阻塞...");
            Thread.sleep(6000); // 模拟阻塞6秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        watchdog.stopWatchdog();
        System.out.println("ANR监控已停止");
    }
}
