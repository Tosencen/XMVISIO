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
            
            // DataStore for preferences
            implementation(libs.datastore.preferences)
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
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
