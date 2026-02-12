plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

/**
 * Observability 模块配置
 *
 * 面试亮点：
 * - 独立模块设计，可打包为 AAR 供其他项目复用
 * - 最小化依赖，保持轻量
 * - 不使用 Compose，专注于核心逻辑
 */
android {
    namespace = "com.trackapi.observability"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        consumerProguardFiles("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

}

/**
 * 依赖配置策略：
 * - 只引入必要的 AndroidX 核心库
 * - 不引入 Compose，保持轻量
 */
dependencies {
    implementation(libs.androidx.core.ktx)

    // 单元测试
    testImplementation(libs.junit)
}
