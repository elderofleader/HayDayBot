# OpenCV
-keep class org.opencv.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep bot classes
-keep class com.haydaybot.** { *; }
