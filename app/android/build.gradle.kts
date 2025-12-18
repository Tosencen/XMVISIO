plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.xmvisio.app"
    compileSdk = getIntProperty("android.compile.sdk")
    
    defaultConfig {
        applicationId = "com.xmvisio.app"
        minSdk = getIntProperty("android.min.sdk")
        targetSdk = getIntProperty("android.compile.sdk")
        versionCode = getIntProperty("android.version.code")
        versionName = project.version.toString()
        
        // GitHub Token (可选，从 local.properties 读取)
        val githubToken = project.findProperty("github.token") as String? ?: ""
        buildConfigField("String", "GITHUB_TOKEN", "\"$githubToken\"")
        
        // 只支持 arm64-v8a 架构（减小 APK 体积）
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }
    
    // 确保 youtubedl-android 的 native 库被正确打包
    packaging {
        jniLibs {
            useLegacyPackaging = true
            // 保留所有 native 库，包括 libc++_shared.so
            pickFirsts.add("lib/*/libc++_shared.so")
        }
    }
    
    buildFeatures {
        buildConfig = true
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("${System.getProperty("user.home")}/Desktop/xmvisio-release.keystore")
            storePassword = "xmvisio2024"
            keyAlias = "xmvisio"
            keyPassword = "xmvisio2024"
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(projects.app.shared)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    
    // 媒体支持
    implementation(libs.androidx.media)
    
    // 网络请求 - OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON 序列化
    implementation(libs.kotlinx.serialization.json)
    
    // Lottie 动画
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    
    // YoutubeDL Android (Seal's fork) - 视频/音频下载
    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)
    implementation(libs.youtubedl.android.aria2c)
}
