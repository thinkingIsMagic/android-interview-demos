package com.example.androidcrashanalysis.jni

/**
 * JNI 桥接层
 * 封装 native 代码的调用入口
 *
 * 【native 方法签名说明】
 * System.loadLibrary() 会加载 lib<name>.so 文件
 * extern "C" JNIEXPORT void JNICALL
 * Java_包名_类名_方法名(JNIEnv*, jobject)
 */
object NativeCrashBridge {

    init {
        // 加载 native 库（libnative-crash-lib.so）
        // 必须在使用任何 native 方法前加载
        System.loadLibrary("native-crash-lib")
    }

    /**
     * 触发空指针解引用
     * 内部调用 C++ 代码，对 nullptr 解引用
     * 结果: SIGSEGV (signal 11), fault addr = 0x0
     */
    external fun triggerNullPointerDereference()

    /**
     * 触发缓冲区溢出
     * 内部调用 C++ 代码，memcpy 到超出边界的缓冲区
     * 结果: SIGABRT 或 SIGSEGV（栈被破坏）
     */
    external fun triggerBufferOverflow()

    /**
     * 触发 Use-After-Free
     * 内部调用 C++ 代码，在 free() 后继续使用指针
     * 结果: SIGSEGV (signal 11), fault addr = 某个非零地址
     */
    external fun triggerUseAfterFree()
}
