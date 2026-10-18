# Project-specific release shrinker rules.
# Add keep rules here only when a dependency or reflected API requires them.

# Retrofit and Kakao SDK inspect generic service method signatures at runtime.
# Without these attributes, release builds can crash while creating Retrofit call adapters.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-keep class com.kakao.sdk.** { *; }
-dontwarn com.kakao.sdk.**
