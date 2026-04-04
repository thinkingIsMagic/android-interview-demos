# 面试题2：弱网检测 - 答案参考

## 核心实现

```java
public void onNetworkChanged(NetworkState state) {
    long now = System.currentTimeMillis();
    queue.add(new NetworkStatus(now, state));
    cleanOld(now);

    if (!isInCoolDown(now)) {
        if (check3MinBadCount(now) || check30sBadRatio(now)) {
            showWeakNetworkPopup();
            startCoolDown();
        }
    }
}

private void cleanOld(long now) {
    while (!queue.isEmpty() && now - queue.peek().timestamp > WINDOW_3MIN) {
        queue.poll();
    }
}

private boolean check3MinBadCount(long now) {
    int badCount = 0;
    for (NetworkStatus ns : queue) {
        if (now - ns.timestamp <= WINDOW_3MIN && ns.state == NetworkState.BAD) {
            badCount++;
        }
    }
    return badCount >= BAD_COUNT_THRESHOLD;
}

private boolean check30sBadRatio(long now) {
    int badCount = 0;
    int total = 0;
    for (NetworkStatus ns : queue) {
        if (now - ns.timestamp <= WINDOW_30S) {
            total++;
            if (ns.state == NetworkState.BAD) {
                badCount++;
            }
        }
    }
    return total > 0 && ((double) badCount / total) > BAD_RATIO_THRESHOLD;
}

private boolean isInCoolDown(long now) {
    return now - lastPopupTime < COOL_DOWN;
}

private void startCoolDown() {
    lastPopupTime = System.currentTimeMillis();
}
```
