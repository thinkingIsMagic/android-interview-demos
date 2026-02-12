plugin {
    kotlin("jvm") version "1.9.22"
}

group = "com.learn.kotlin"
version = "1.0.0"

// 统一 Kotlin 版本，便于管理
// 为什么：避免子模块版本不一致导致的兼容性问题
extra["kotlinVersion"] = "1.9.22"
