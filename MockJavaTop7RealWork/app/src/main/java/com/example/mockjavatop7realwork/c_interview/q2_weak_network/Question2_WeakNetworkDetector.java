package com.example.mockjavatop7realwork.c_interview.q2_weak_network;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 面试题2：字节跳动 - 弱网检测
 *
 * 题目描述：
 * 实现一个弱网检测器，监控网络状态变化
 *
 * 网络状态：
 * - GOOD: 良好
 * - NORMAL: 一般
 * - BAD: 差
 *
 * 弹窗规则：
 * 1. 3分钟内 BAD >= 4 次
 * 2. 30秒内 BAD 占比 > 70%
 *
 * 约束：
 * - 弹窗后进入2分钟冷却期
 * - 冷却期内不再触发弹窗
 *
 * 候选人需要实现：
 * 1. 使用滑动窗口统计网络状态
 * 2. 判断是否触发弹窗条件
 * 3. 冷却期逻辑
 */
public class Question2_WeakNetworkDetector {

    // 网络状态枚举
    public enum NetworkState {
        GOOD,    // 良好
        NORMAL,  // 一般
        BAD      // 差
    }

    // 队列存储网络状态记录
    private Queue<NetworkStatus> queue = new LinkedList<>();

    // 时间窗口定义
    private static final long WINDOW_30S = 30_000;   // 30秒
    private static final long WINDOW_3MIN = 180_000; // 3分钟
    private static final long COOL_DOWN = 120_000;  // 2分钟冷却期

    // 弹窗触发阈值
    private static final int BAD_COUNT_THRESHOLD = 4;    // 3分钟内BAD次数阈值
    private static final double BAD_RATIO_THRESHOLD = 0.7; // 30秒内BAD占比阈值

    // 冷却期相关
    private long lastPopupTime = 0;
    private boolean inCoolDown = false;

    /**
     * 网络状态记录
     */
    public static class NetworkStatus {
        long timestamp;
        NetworkState state;

        public NetworkStatus(long timestamp, NetworkState state) {
            this.timestamp = timestamp;
            this.state = state;
        }
    }

    /**
     * TODO: 实现网络状态回调处理
     *
     * 需求：
     * 1. 将新的网络状态加入队列
     * 2. 清理超过3分钟窗口的旧数据
     * 3. 检查是否触发弹窗条件
     * 4. 如果触发且不在冷却期内，显示弹窗并开始冷却
     *
     * @param state 当前网络状态
     */
    public void onNetworkChanged(NetworkState state) {
        // TODO: 实现网络状态处理逻辑
        // 提示：
        // 1. 当前时间戳
        // 2. 将状态加入队列
        // 3. 清理过期数据 (超过3分钟的数据)
        // 4. 检查是否在冷却期
        // 5. 判断触发条件
    }

    /**
     * TODO: 清理超过时间窗口的旧数据
     *
     * @param now 当前时间戳
     */
    private void cleanOld(long now) {
        // TODO: 清理超过WINDOW_3MIN的数据
        // 提示：遍历队列，移除 timestamp < now - WINDOW_3MIN 的元素
    }

    /**
     * TODO: 检查3分钟内BAD次数是否>=4
     *
     * @param now 当前时间戳
     * @return 是否触发条件
     */
    private boolean check3MinBadCount(long now) {
        // TODO: 统计3分钟内的BAD次数
        // 返回 badCount >= BAD_COUNT_THRESHOLD
        return false;
    }

    /**
     * TODO: 检查30秒内BAD占比是否>70%
     *
     * @param now 当前时间戳
     * @return 是否触发条件
     */
    private boolean check30sBadRatio(long now) {
        // TODO: 统计30秒内的总次数和BAD次数
        // 返回 (double)badCount / total > BAD_RATIO_THRESHOLD
        return false;
    }

    /**
     * TODO: 开始冷却期
     */
    private void startCoolDown() {
        // TODO: 记录当前时间作为冷却开始时间
    }

    /**
     * TODO: 检查是否在冷却期内
     *
     * @param now 当前时间戳
     * @return 是否在冷却期
     */
    private boolean isInCoolDown(long now) {
        // TODO: 判断 now - lastPopupTime < COOL_DOWN
        return false;
    }

    /**
     * 显示弱网提示弹窗
     */
    private void showWeakNetworkPopup() {
        System.out.println("⚠️ 弹出弱网提示弹窗！");
    }

    // ==================== 测试代码 ====================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 面试题2：弱网检测 ===");

        Question2_WeakNetworkDetector detector = new Question2_WeakNetworkDetector();

        // 模拟连续发送BAD网络状态
        System.out.println("\n--- 测试1: 3分钟内4次BAD ---");
        for (int i = 0; i < 4; i++) {
            detector.onNetworkChanged(NetworkState.BAD);
            Thread.sleep(100); // 模拟100ms间隔
        }

        // 模拟30秒内连续BAD
        System.out.println("\n--- 测试2: 30秒内BAD占比>70% ---");
        Thread.sleep(1000);
        for (int i = 0; i < 10; i++) {
            // 70% BAD, 30% GOOD
            NetworkState state = (i % 10 < 7) ? NetworkState.BAD : NetworkState.GOOD;
            detector.onNetworkChanged(state);
            Thread.sleep(100);
        }

        System.out.println("\n测试完成");
    }
}
