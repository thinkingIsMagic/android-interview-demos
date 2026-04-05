# Android Crash Analysis - 稳定性问题实战练习

Android 稳定性问题（OOM / ANR / Native Crash）的**制造 → 复现 → 排查 → 修复**全流程实战练习项目。

## 项目简介

本项目是一个 Android Demo 应用，包含 12 个精心设计的稳定性问题场景。通过"问题版本"和"修复版本"开关，帮助开发者：

- **理解原理**：每个场景都有详细的中文原理说明
- **亲手复现**：点击按钮即可触发对应问题
- **掌握排查**：每个场景都有对应的命令行排查步骤
- **学会修复**：开关切换到修复版本，对比正确实现

## 功能概览

### OOM 模块（5 个场景）

| 场景 | 危险等级 | 问题类型 |
|------|---------|---------|
| 静态引用泄漏 | 高危 | static 变量持有 Activity 引用 |
| Handler 泄漏 | 高危 | 延迟消息持有 Handler 引用 |
| 未反注册 Listener | 中危 | BroadcastReceiver 不反注册 |
| Bitmap 大图 OOM | 极度危险 | 大尺寸 Bitmap 循环创建不释放 |
| 集合泄漏 | 中危 | 单例 List 只增不减 |

### ANR 模块（4 个场景）

| 场景 | 危险等级 | 问题类型 |
|------|---------|---------|
| 主线程 Sleep | 高危 | Thread.sleep 阻塞主线程 |
| 主线程死锁 | 极度危险 | 交叉等锁形成环路 |
| 大文件 IO | 中危 | 主线程同步读写大文件 |
| SP commit 卡顿 | 低危 | 大量 commit 同步写入 |

### Native Crash 模块（3 个场景）

| 场景 | 危险等级 | 问题类型 |
|------|---------|---------|
| 空指针解引用 | 极度危险 | C++ nullptr 解引用 → SIGSEGV |
| 缓冲区溢出 | 极度危险 | memcpy 超出边界 → SIGABRT |
| Use-After-Free | 极度危险 | free 后继续使用 → SIGSEGV |

## 技术栈

- **Language**: Kotlin + JNI (C++)
- **UI**: Jetpack Compose + Material Design 3
- **Native**: CMake + NDK
- **内存检测**: LeakCanary 2.14
- **Min SDK**: 24 | **Target SDK**: 34 | **Compile SDK**: 36

## 环境要求

- JDK 11+
- Android SDK (API 36)
- Android NDK (r29+)
- Android Studio Hedgehog 或更高版本

## 快速开始

### 编译

```bash
# Debug 构建
./gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 使用

1. 打开 App，进入首页
2. 选择一个分类（OOM / ANR / Native Crash）
3. 点击具体场景进入详情页
4. 点击**触发**按钮复现问题
5. 点击**手动 GC** 或观察内存变化
6. 打开**修复版本**开关，对比正确实现

## 项目结构

```
app/src/main/
├── java/com/example/androidcrashanalysis/
│   ├── MainActivity.kt              # 入口
│   ├── navigation/                  # 导航
│   ├── model/                      # 数据模型 + 场景注册表
│   ├── ui/
│   │   ├── common/                 # 通用组件
│   │   │   └── ScenarioScaffold.kt # 场景页面骨架
│   │   ├── home/                  # 首页
│   │   ├── oom/                   # OOM 场景（5个）
│   │   ├── anr/                   # ANR 场景（4个）
│   │   └── nativecrash/           # Native Crash 场景（3个）
│   └── jni/
│       └── NativeCrashBridge.kt    # JNI 桥接
└── cpp/
    ├── CMakeLists.txt
    └── native_crash.cpp            # C++ Native Crash 实现
```

## 排查工具速查

| 场景 | 排查命令 |
|------|---------|
| OOM 泄漏 | LeakCanary 自动通知 / `adb shell dumpsys meminfo` |
| ANR | `adb pull /data/anr/traces.txt` |
| Native Crash | `adb logcat \| grep SIGSEGV` |
| hprof 分析 | `hprof-conv dump.hprof converted.hprof` + MAT |
| Native 堆栈 | `addr2line -e libnative-crash-lib.so <offset>` |

## 相关文档

- [PRACTICE_GUIDE.md](PRACTICE_GUIDE.md) — 完整排查 SOP / MAT 操作步骤 / traces.txt 阅读指南 / 面试话术模板
