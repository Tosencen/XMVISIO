import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
}

kotlin {
    jvm {
        withJava()
    }
    
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.app.shared)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.xmvisio.app.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "XMVISIO"
            packageVersion = "1.0.0"
            description = "XMVISIO - 多功能媒体播放应用"
            vendor = "XMVISIO"
            
            macOS {
                dockName = "XMVISIO"
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "com.xmvisio.app"
                setDockNameSameAsPackageName = false
            }
            windows {
                iconFile.set(project.file("icons/a_1024x1024_rounded.ico"))
            }
            linux {
                iconFile.set(project.file("appResources/linux-x64/icon.png"))
            }
        }
    }
}
