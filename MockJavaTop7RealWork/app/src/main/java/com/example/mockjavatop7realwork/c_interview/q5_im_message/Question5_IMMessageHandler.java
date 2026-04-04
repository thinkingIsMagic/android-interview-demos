package com.example.mockjavatop7realwork.c_interview.q5_im_message;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试题5：IM即时通讯 - 消息补偿与乱序处理
 *
 * 题目描述：
 * 在高并发聊天场景下，如何保证消息不丢失、不重复、不乱序？
 *
 * 核心概念：
 * - Seq ID：服务端生成的单调递增序列号
 * - ACK机制：客户端收到消息后向服务端确认
 * - 去重：基于msg_id的本地去重
 *
 * 候选人需要实现：
 * 1. 消息顺序校验 - 检测乱序和空洞
 * 2. 消息补偿 - 拉取缺失的消息
 * 3. 去重机制 - 基于msg_id
 * 4. ACK确认 - 收到消息后发送确认
 */
public class Question5_IMMessageHandler {

    // 本地记录的最后一个连续消息的ID
    private int lastSeqId = 0;

    // 消息缓存（用于暂存乱序消息）
    private Map<Integer, Message> messageCache = new ConcurrentHashMap<>();

    // 已确认的消息ID集合（用于去重）
    private Map<String, Boolean> acknowledgedMsgIds = new HashMap<>();

    // ACK重发定时器
    private static final long ACK_TIMEOUT = 5000; // 5秒

    /**
     * 消息实体类
     */
    public static class Message {
        public String id;         // 消息唯一ID（用于去重）
        public int seqId;         // 序列号（用于排序）
        public String content;    // 消息内容
        public long timestamp;   // 时间戳

        public Message(String id, int seqId, String content) {
            this.id = id;
            this.seqId = seqId;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "Message{id='" + id + "', seqId=" + seqId + ", content='" + content + "'}";
        }
    }

    /**
     * 候选人请实现：消息接收处理
     *
     * 核心逻辑：
     * 1. 如果 seqId == lastSeqId + 1，正常顺序，直接上屏
     * 2. 如果 seqId > lastSeqId + 1，发现空洞，缓存消息并拉取缺失
     * 3. 如果 seqId <= lastSeqId，收到重复/旧消息，去重处理
     *
     * @param msg 接收到的消息
     */
    public void onReceive(Message msg) {
        // TODO: 实现消息处理逻辑
        // 提示：
        // case 1: msg.seqId == lastSeqId + 1
        //   - 直接展示消息
        //   - lastSeqId = msg.seqId
        //   - 发送ACK
        //   - 检查缓存中是否有后续消息
        //
        // case 2: msg.seqId > lastSeqId + 1
        //   - 缓存当前消息到 messageCache
        //   - 调用 pullMsgFromServer 拉取缺失消息
        //
        // case 3: msg.seqId <= lastSeqId
        //   - 检查 msg.id 是否已处理过
        //   - 如果已处理，直接发送ACK
        //   - 如果未处理，可能是网络延迟导致的乱序，按seqId处理
    }

    /**
     * 候选人请实现：处理连续消息
     * 当收到正确顺序的消息时调用
     *
     * @param msg 消息
     */
    private void processInOrder(Message msg) {
        // TODO: 实现顺序消息处理
        // 提示：
        // 1. 展示消息
        // 2. 更新 lastSeqId
        // 3. 发送 ACK
        // 4. 检查缓存中是否有可以展示的后续消息
    }

    /**
     * 候选人请实现：处理乱序消息（发现空洞）
     *
     * @param msg 乱序消息
     */
    private void processOutOfOrder(Message msg) {
        // TODO: 实现乱序消息处理
        // 提示：
        // 1. 将消息存入缓存 messageCache
        // 2. 触发拉取缺失消息
    }

    /**
     * 候选人请实现：消息去重检查
     *
     * @param msgId 消息ID
     * @return 是否已处理过
     */
    private boolean isDuplicate(String msgId) {
        // TODO: 检查消息是否已处理
        return false;
    }

    /**
     * 候选人请实现：标记消息已处理
     *
     * @param msgId 消息ID
     */
    private void markAsProcessed(String msgId) {
        // TODO: 将消息ID加入已处理集合
    }

    /**
     * 候选人请实现：从服务器拉取缺失的消息
     *
     * @param startSeqId 起始序列号
     * @param endSeqId 结束序列号
     */
    private void pullMsgFromServer(int startSeqId, int endSeqId) {
        // TODO: 实现消息拉取
        // 提示：
        // 1. 向服务器发送拉取请求
        // 2. 服务器返回 [startSeqId, endSeqId] 范围内的消息
        // 3. 逐个处理拉取到的消息（会触发 onReceive 递归）
    }

    /**
     * 候选人请实现：发送ACK确认
     *
     * @param msgId 消息ID
     */
    private void sendAck(String msgId) {
        // TODO: 发送ACK给服务器
        // 服务器未收到ACK会重发
    }

    /**
     * 候选人请实现：检查并处理缓存中的消息
     * 当收到一条连续消息后，检查缓存中是否有可以展示的后续消息
     */
    private void checkCacheAndProcess() {
        // TODO: 检查缓存
        // 循环检查 messageCache.get(lastSeqId + 1) 是否存在
        // 如果存在，调用 processInOrder 处理，然后继续检查
    }

    /**
     * 显示消息到界面
     */
    private void showMessage(Message msg) {
        System.out.println("📱 展示消息: " + msg);
    }

    // ==================== 测试代码 ====================
    public static void main(String[] args) {
        System.out.println("=== 面试题5：IM消息补偿与乱序处理 ===\n");

        Question5_IMMessageHandler handler = new Question5_IMMessageHandler();

        // 测试1：正常顺序消息
        System.out.println("--- 测试1: 正常顺序 ---");
        handler.onReceive(new Message("msg-1", 1, "你好"));
        handler.onReceive(new Message("msg-2", 2, "在吗"));

        // 测试2：乱序消息
        System.out.println("\n--- 测试2: 乱序消息（先收3再收2）---");
        handler.onReceive(new Message("msg-3", 3, "晚上出来"));
        handler.onReceive(new Message("msg-2", 2, "在吗")); // 缺失1，这里只是测试2

        // 测试3：重复消息
        System.out.println("\n--- 测试3: 重复消息 ---");
        handler.onReceive(new Message("msg-1", 1, "你好"));
    }
}
