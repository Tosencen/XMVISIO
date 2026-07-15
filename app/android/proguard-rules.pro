# Project-specific app rules.
# Shared module/library rules are added from app/shared/*.pro in app/android/build.gradle.kts.

# ===== 下载功能已暂时禁用 =====
# # Keep downloader/runtime APIs that may be accessed by JNI/reflection.
# -keep class com.yausername.youtubedl_android.** { *; }

# Keep app entry and core initialization paths stable in release.
-keep class com.xmvisio.app.XmvisioApplication { *; }
# ===== 下载功能已暂时禁用 =====
# -keep class com.xmvisio.app.download.** { *; }
