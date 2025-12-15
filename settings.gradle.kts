/*
 * KMP + Compose Multiplatform Template
 * 一套代码，多端运行
 */

rootProject.name = "kmp-template"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

// 应用模块
include(":app:shared")
include(":app:shared:ui-foundation")
include(":app:android")
include(":app:desktop")

// 核心工具模块
include(":core:utils:platform")
include(":core:utils:logging")
include(":core:utils:serialization")
include(":core:utils:coroutines")
include(":core:utils:testing")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
