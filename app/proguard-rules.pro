# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to hide the original source file name.
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# Keep serializable classes and their serializers
-keep,includedescriptorclasses class com.app.shouze.data.local.**$$serializer { *; }
-keepclassmembers class com.app.shouze.data.local.** {
    *** Companion;
}
-keepclasseswithmembers class com.app.shouze.data.local.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep AniList API models
-keep,includedescriptorclasses class com.app.shouze.data.remote.**$$serializer { *; }
-keepclassmembers class com.app.shouze.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.app.shouze.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# --- General Compose / AndroidX ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
