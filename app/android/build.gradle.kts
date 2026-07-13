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
    // 不使用 useLegacyPackaging，让 AGP 新打包管道自动处理 16KB 对齐
    packaging {
        jniLibs {
            // libc++_shared.so 下载损坏（空 HTML 文件），且没有 native 库依赖它，排除掉
            excludes.add("lib/**/libc++_shared.so")
        }
        resources {
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "**/*.kotlin_module",
                "DebugProbesKt.bin"
            )
        }
    }
    
    buildFeatures {
        buildConfig = true
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("${System.getProperty("user.home")}/Desktop/xmvisio-release.keystore")
            storePassword = "***REDACTED***"
            keyAlias = "xmvisio"
            keyPassword = "***REDACTED***"
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
                "../shared/proguard-rules.pro",
                "../shared/kotlinx-serialization.pro",
                "../shared/kotlinx-coroutines.pro"
            )
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
    
    // Media3 (ExoPlayer) - 视频播放
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.session)
    
    // 网络请求 - OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON 序列化
    implementation(libs.kotlinx.serialization.json)
    
    // Lottie 动画
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    
    // YoutubeDL Android (Seal's fork) - 视频/音频下载
    implementation(libs.youtubedl.android.library)
}
