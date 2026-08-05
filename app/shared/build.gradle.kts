plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    
    `ani-mpp-lib-targets`
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
            
            // Coroutines
            api(libs.kotlinx.coroutines.core)
            
            // Serialization
            api(libs.kotlinx.serialization.json)
            
            // Navigation
            api(libs.compose.navigation.compose)
            api(libs.compose.navigation.runtime)
            
            // ViewModel & Lifecycle
            api(libs.compose.lifecycle.viewmodel.compose)
            api(libs.compose.lifecycle.runtime.compose)
            
            // Core utils
            api(projects.core.utils.platform)
            api(projects.core.utils.logging)
            api(projects.core.utils.serialization)
            api(projects.core.utils.coroutines)
            
            // Material Kolor - 动态主题
            api(libs.materialkolor)
            
            // Reorderable - 拖动排序
            api(libs.reorderable)
            
            // Material3 Adaptive Navigation Suite
            api(libs.compose.material3.adaptive.core)
            api(libs.compose.material3.adaptive.layout)
            api(libs.compose.material3.adaptive.navigation.suite)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            
            // Media support for notifications
            implementation(libs.androidx.media)
            
            // Media3 (ExoPlayer) - 视频播放
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.ui.compose)
            implementation(libs.androidx.media3.session)
            
            // DataStore for preferences
            implementation(libs.datastore.preferences)
            
            // OkHttp for network requests
            implementation(libs.okhttp)
            
            // JSON serialization
            implementation(libs.kotlinx.serialization.json)
            
            // Squiggly Slider for wave animation
            implementation(libs.squigglyslider)

            // Haze - 毛玻璃效果
            implementation(libs.haze.core)
            implementation(libs.haze.materials)

            // Lottie - 动画
            implementation(libs.lottie)

            // SQLite（取代 SharedPreferences JSON 列表存储，支持 schema 迁移）
            implementation(libs.sqlite.bundled)

            // ===== 下载功能已暂时禁用 =====
            // YoutubeDL Android (Seal's fork) - 视频/音频下载
            // implementation(libs.youtubedl.android.library)
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                // Compose UI 测试（instrumented，跑在模拟器/真机上）
                implementation(libs.androidx.compose.ui.test.junit4)
                // JUnit5 runner/API 由 de.mannodermaus.android-junit5 插件注入，这里不显式声明，
                // 避免与插件版本（1.11.2.0）的运行时不一致。
                // Vintage engine：让 AndroidJUnit5Builder 也能发现并运行 JUnit4 测试（ComposeTestRule 是 JUnit4 rule）
                implementation(libs.junit5.vintage.engine)
                // 强制升级 espresso（见 libs.versions.toml 注释）：旧版 InputManagerEventInjectionStrategy
                // 调用已移除的 InputManager.getInstance()，在 Android 17 (API 36) 上崩溃
                implementation(libs.androidx.test.espresso.core)
            }
        }
        
        iosMain.dependencies {
            // iOS specific dependencies
        }
    }
}

android {
    namespace = "com.xmvisio.app.shared"
    compileSdk = getIntProperty("android.compile.sdk")
    
    defaultConfig {
        minSdk = getIntProperty("android.min.sdk")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    packageOfResClass = "com.xmvisio.app"
    generateResClass = always
}
