/*
 * KMP + Compose Multiplatform Template
 */

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    kotlin("jvm") apply false
    kotlin("plugin.serialization") version libs.versions.kotlin apply false
    kotlin("plugin.compose") apply false
    id("org.jetbrains.compose") apply false
    id("com.android.library") apply false
    id("com.android.application") apply false
    id("org.jetbrains.kotlinx.atomicfu") apply false
    id("de.mannodermaus.android-junit5") version "1.11.2.0" apply false
    idea
}

allprojects {
    group = "com.template.kmp"
    version = properties["version.name"].toString()

    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

subprojects {
    afterEvaluate {
        configureKotlinOptIns()
        configureKotlinTestSettings()
        configureEncoding()
        configureJvmTarget()
    }
}

idea {
    module {
        excludeDirs.add(file(".kotlin"))
        excludeDirs.add(file("build"))
    }
}
