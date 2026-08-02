# ============================================================
# Arrow Maze — ProGuard / R8 rules
# ============================================================

# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.zenox.arrowmaze.**$$serializer { *; }
-keepclassmembers class com.zenox.arrowmaze.** {
    *** Companion;
}
-keepclasseswithmembers class com.zenox.arrowmaze.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Hilt ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ---- Retrofit / OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# ---- Firebase ----
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---- Compose ----
-dontwarn androidx.compose.**

# ---- Coroutines ----
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ---- Model classes (keep for reflection / serialization) ----
-keep class com.zenox.arrowmaze.core.data.dto.** { *; }
-keep class com.zenox.arrowmaze.core.domain.model.** { *; }

# ---- Lottie ----
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ---- Coil ----
-dontwarn coil.**

# ---- Billing ----
-keep class com.android.billingclient.** { *; }

# ---- Ads ----
-keep public class com.google.android.gms.ads.** { public *; }
-dontwarn com.google.android.gms.ads.**

# ---- Keep enums ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Keep generic signatures ----
-keepattributes Signature
-keepattributes EnclosingMethod

# ---- Timber ----
-dontwarn org.jetbrains.annotations.**
