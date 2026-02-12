plugins {
    kotlin("jvm")
}

dependencies {
    // Kotlin 标准库（所有模块都需要）
    implementation(kotlin("stdlib"))

    // 协程支持（面试必考）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 测试依赖
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // 日志（简单门面，实际用 SLF4J 或 Kotlin-logging）
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

tasks.test {
    useJUnitPlatform()
}
