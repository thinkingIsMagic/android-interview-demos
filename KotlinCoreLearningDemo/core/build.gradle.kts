plugins {
    kotlin("jvm")
}

dependencies {
    // Kotlin 标准库
    implementation(kotlin("stdlib"))

    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 日志
    implementation("org.slf4j:slf4j-api:2.0.9")
}
