rootProject.name = "KotlinCoreLearningDemo"

// 包含 app 和 core 两个子模块
// 为什么用多模块：便于职责分离，core 专注业务逻辑，app 专注运行入口
include("app", "core")
