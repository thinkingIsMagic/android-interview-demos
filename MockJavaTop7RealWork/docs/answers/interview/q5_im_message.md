# 面试题5：IM消息处理 - 答案参考

## 消息接收处理

```java
public void onReceive(Message msg) {
    // 去重检查
    if (isDuplicate(msg.id)) {
        sendAck(msg.id);
        return;
    }

    if (msg.seqId == lastSeqId + 1) {
        // 正常顺序
        processInOrder(msg);
    } else if (msg.seqId > lastSeqId + 1) {
        // 发现空洞
        processOutOfOrder(msg);
    } else {
        // 收到旧消息/重复
        sendAck(msg.id);
    }
}

private void processInOrder(Message msg) {
    // 显示消息
    showMessage(msg);

    // 更新lastSeqId
    lastSeqId = msg.seqId;

    // 标记已处理
    markAsProcessed(msg.id);

    // 发送ACK
    sendAck(msg.id);

    // 检查缓存中是否有后续消息
    while (messageCache.containsKey(lastSeqId + 1)) {
        Message next = messageCache.remove(lastSeqId + 1);
        processInOrder(next);
    }
}

private void processOutOfOrder(Message msg) {
    // 存入缓存
    messageCache.put(msg.seqId, msg);

    // 拉取缺失的消息
    pullMsgFromServer(lastSeqId + 1, msg.seqId - 1);
}

private boolean isDuplicate(String msgId) {
    return acknowledgedMsgIds.containsKey(msgId);
}

private void markAsProcessed(String msgId) {
    acknowledgedMsgIds.put(msgId, true);
}
