/**
 * 项目主入口 - 一键运行所有 Kotlin 示例
 * 
 * 本项目结构：
 * - app/: 可运行的示例和测试
 * - core/: 真实工程分层（domain/data/common）
 * - docs/: Kotlin 核心知识与面试问答
 * 
 * 运行方式：
 * - 运行全部示例: ./gradlew run
 * - 运行单个 Demo: 修改 Main.kt 中 demo.run() 调用
 * - 运行测试: ./gradlew test
 */
fun main() {
    println("=" .repeat(60))
    println("  Kotlin 核心学习项目 - 电商活动引擎")
    println("=".repeat(60))
    println()
    
    // M1 验证：基础结构
    println("【M1】项目结构验证通过 ✓")
    println()
    
    // M2 验证：OOP 特性
    println("【M2】运行面向对象与函数特性示例...")
    Demo01_Basics.run()
    Demo02_Functions.run()
    Demo03_OOP.run()
    println("【M2】完成 ✓")
    println()
    
    // M3 验证：协程与并发
    println("【M3】运行集合与协程示例...")
    Demo04_Collections.run()
    Demo05_Coroutines.run()
    println("【M3】完成 ✓")
    
    println()
    println("=".repeat(60))
    println("  所有示例运行完成！")
    println("=".repeat(60))
    println()
    println("下一步：阅读 docs/ 目录下的文档，深入理解 Kotlin 核心原理")
}
