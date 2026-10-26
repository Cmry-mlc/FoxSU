# Protobuf
-shrinkunusedprotofields

# Commons-compress
-dontwarn com.github.luben.zstd.**

# ---- FoxSu: R8 keeps ----
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# kotlinx.serialization（导航路由、设置持久化都用它）
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class me.weishu.kernelsu.** {
    *** Companion;
}
-keepclasseswithmembers class me.weishu.kernelsu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WebView JS 桥
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Compose / ViewModel 反射入口
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
