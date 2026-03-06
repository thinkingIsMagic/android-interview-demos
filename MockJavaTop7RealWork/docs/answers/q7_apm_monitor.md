# 面试题7：APM性能监控 - 答案参考

## 1. 内存泄漏监控

```java
public void watch(Object activity) {
    WeakReference<Object> ref = new WeakReference<>(activity, referenceQueue);
    references.add(ref);

    // 延迟5秒后检查
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
        checkForLeaks();
    }, 5000);
}

private void checkForLeaks() {
    for (WeakReference<Object> ref : references) {
        if (ref.get() != null) {
            // 对象未被回收，疑似泄漏
            System.out.println("检测到疑似内存泄漏: " + ref.get());
            triggerHeapDump();
        }
    }
    // 清理已被回收的引用
    referenceQueue.clear();
}
```

## 2. OOM监控

```java
public void startMonitor() {
    isMonitoring = true;
    new Thread(() -> {
        while (isMonitoring) {
            float usage = getMemoryUsage();
            if (usage > MEMORY_THRESHOLD) {
                System.out.println("内存使用率超过阈值: " + (usage * 100) + "%");
                triggerHeapDump();
            }
            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                break;
            }
        }
    }).start();
}

private float getMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    return (float) (totalMemory - freeMemory) / maxMemory;
}
```

## 3. ANR监控

```java
@Override
public void run() {
    while (running) {
        int lastTick = tick;

        // 向主线程发送任务
        mainHandler.post(() -> tick = (tick + 1) % 100);

        try {
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException e) {
            return;
        }

        // 如果tick未变化，说明主线程未处理任务
        if (tick == lastTick) {
            collectANRInfo();
        }
    }
}

private void collectANRInfo() {
    Thread mainThread = Looper.getMainLooper().getThread();
    StackTraceElement[] traces = mainThread.getStackTrace();

    StringBuilder sb = new StringBuilder();
    sb.append("=== ANR 检测 ===\n");
    sb.append("主线程堆栈:\n");
    for (StackTraceElement trace : traces) {
        sb.append("    ").append(trace).append("\n");
    }
    System.out.println(sb.toString());

    // 可选：打印所有线程
    Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
    for (Map.Entry<Thread, StackTraceElement[]> entry : allThreads.entrySet()) {
        Thread t = entry.getKey();
        if (t.getState() == Thread.State.RUNNABLE) {
            System.out.println("RUNNABLE线程: " + t.getName());
        }
    }
}
```
