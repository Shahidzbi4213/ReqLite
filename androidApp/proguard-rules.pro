# ==============================================================================
# ReqLite Production R8 / ProGuard Keep Rules
# ==============================================================================

# 1. Attributes for Annotations, Coroutines, and Crash Stack Traces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# 2. Kotlin Standard Library & Coroutines
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# 3. Kotlinx Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer {
    <fields>;
    <methods>;
}
-dontwarn kotlinx.serialization.**

# 4. AndroidX Room 3 Multiplatform
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room3.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room3.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room3.Dao class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room3.Entity class * { *; }
-keep class * implements androidx.room.RoomDatabaseConstructor { *; }
-keep class * implements androidx.room3.RoomDatabaseConstructor { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep class * extends androidx.room3.migration.Migration { *; }
-keep class **.*_Impl { *; }
-keep class com.learn.reqlite.data.local.database.** { *; }
-dontwarn androidx.room.**
-dontwarn androidx.room3.**

# 5. Koin Dependency Injection
-keep class org.koin.** { *; }
-keep class io.insert_koin.** { *; }
-keep class com.learn.reqlite.di.** { *; }
-dontwarn org.koin.**

# 6. Ktor HTTP Client & OkHttp Engine
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# 7. Coil 3 Image Loading
-keep class coil3.** { *; }
-dontwarn coil3.**

# 8. ZXing Barcode Scanner
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# 9. AndroidX DataStore & Crypto
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# 10. ReqLite Domain Models & Database Entities
-keep class com.learn.reqlite.domain.model.** { *; }
-keep class com.learn.reqlite.domain.export.** { *; }
-keep class com.learn.reqlite.data.local.entity.** { *; }
