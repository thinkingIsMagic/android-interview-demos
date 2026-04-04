#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#define TAG "NativeCrash"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/**
 * 场景 1: 空指针解引用
 *
 * 【问题】
 * 对 nullptr 解引用，触发 SIGSEGV (Signal 11)
 *
 * 【JNI 命名规则】
 * Java_包名类名_方法名
 * 包名: com_example_androidcrashanalysis
 * 类名: jni_NativeCrashBridge
 * 方法: triggerNullPointerDereference
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidcrashanalysis_jni_NativeCrashBridge_triggerNullPointerDereference(
        JNIEnv *env,
        jobject /* this */) {

    LOGI("Native: 触发空指针解引用...");

    // ❌ 解引用空指针，触发 SIGSEGV
    int *ptr = nullptr;
    *ptr = 42;  // ← 罪魁祸首：写入地址 0x0

    // 不会执行到这里
    LOGI("Native: 永远不会打印这行");
}

/**
 * 场景 2: 数组越界写入
 *
 * 【问题】
 * memcpy 写入超出栈缓冲区边界，触发 SIGABRT 或 SIGSEGV
 *
 * 【原理】
 * - 栈上分配了 10 字节的 buffer
 * - 写入超过 10 字节的数据，覆盖了栈帧中的 return address
 * - 触发 Stack Canary 检查失败 → SIGABRT
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidcrashanalysis_jni_NativeCrashBridge_triggerBufferOverflow(
        JNIEnv *env,
        jobject /* this */) {

    LOGI("Native: 触发缓冲区溢出...");

    // 栈上分配 10 字节缓冲区
    char buffer[10];

    // 准备一个超过 10 字节的数据源
    const char *src = "This string is much longer than 10 characters!!!";

    // ❌ 复制到超出 buffer 容量的目标
    // 会覆盖 buffer 之后的栈内存（return address、saved registers 等）
    memcpy(buffer, src, strlen(src));  // ← 写入 48 字节到 10 字节空间

    // 不会执行到这里
    LOGI("Native: 永远不会打印这行");
}

/**
 * 场景 3: Use-After-Free (UAF)
 *
 * 【问题】
 * free() 释放内存后继续使用指针，触发 SIGSEGV
 *
 * 【原理】
 * - malloc 分配内存，ptr 指向该内存
 * - free(ptr) 释放内存，但 ptr 仍指向原地址
 * - 继续解引用 ptr → 访问已释放的内存 → SIGSEGV
 *
 * 【与空指针的区别】
 * - 空指针：fault addr = 0x0（地址为空）
 * - UAF：fault addr = 某个有效地址（但已被释放）
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_example_androidcrashanalysis_jni_NativeCrashBridge_triggerUseAfterFree(
        JNIEnv *env,
        jobject /* this */) {

    LOGI("Native: 触发 Use-After-Free...");

    // 分配内存
    int *ptr = (int *) malloc(sizeof(int));

    // 写入数据
    *ptr = 42;
    LOGI("Native: 写入值 42 到堆内存 %p", ptr);

    // ✅ 释放内存
    free(ptr);
    LOGI("Native: 释放了内存 %p", ptr);

    // ❌ free 后继续使用指针
    // ptr 现在是 dangling pointer（悬空指针）
    // 写入已释放的内存 → SIGSEGV
    *ptr = 100;  // ← 罪魁祸首：访问已释放的内存

    // 不会执行到这里
    LOGI("Native: 永远不会打印这行");
}
