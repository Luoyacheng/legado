# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ================================================
# ProGuard按照顺序执行，不可更改：压缩代码(Shrinking)→优化字节码(Optimization)→混淆(Obfuscation)
# 规则排序：全局控制→属性保留→代码设定→警告抑制
# ================================================

# ================================================
# 全局控制
# ================================================

# 禁用混淆
-dontobfuscate

# ================================================
# 保留字节码属性（调试信息、注解、泛型等）
# ================================================

-keepattributes *Annotation*, InnerClasses, Signature, LineNumberTable

# ================================================
# 移除对Log类方法的调用，删除调试日志
# ================================================
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ================================================
# 保护第三方库
# ================================================

# ---------- Hutool ----------
-keep class cn.hutool.crypto.** { *; }
-keep class !cn.hutool.core.util.RuntimeUtil,
            !cn.hutool.core.util.ClassLoaderUtil,
            !cn.hutool.core.util.ReflectUtil,
            !cn.hutool.core.util.SerializeUtil,
            !cn.hutool.core.util.ClassUtil,
            cn.hutool.core.codec.**,
            cn.hutool.core.util.** { *; }
-dontwarn cn.hutool.**

# OkHttp
-keep class okhttp3.* { *; }

# Okio
-keep class okio.* { *; }

# JsonPath
-keep class com.jayway.jsonpath.* { *; }

# ---------- Jsoup ----------
-keep class org.jsoup.** { *; }

# ---------- Sora Editor ----------
# TM4E（语法高亮）
-keep class org.eclipse.tm4e.** { *; }
# Joni（正则引擎）
-keep class org.joni.** { *; }

# GSYVideoPlayer
-keep class com.shuyu.gsyvideoplayer.** { *; }
-dontwarn com.shuyu.gsyvideoplayer.**

# Cronet
-keepclassmembers class org.chromium.net.X509Util {
    *** sDefaultTrustManager;
    *** sTestTrustManager;
}

# ================================================
# 保护特定类
# ================================================

# legado
-keep class * extends io.legado.app.help.JsExtensions { *; }
-keep class io.legado.app.api.ReturnData { *; }
-keep class **.data.entities.** { *; }
-keep class **.help.http.CookieStore { *; }
-keep class **.help.CacheManager { *; }
-keep class **.help.http.StrResponse { *; }
# 视图
-keep class androidx.appcompat.view.menu.MenuBuilder {
    *** setOptionalIconsVisible(...);
    *** getNonActionItems();
}
-keepclassmembers class androidx.appcompat.widget.Toolbar {
    *** mNavButtonView;
}
-keep public class * extends android.view.View {
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, java.lang.Boolean);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
# 文件提供器
-keep class androidx.documentfile.provider.TreeDocumentFile {
    <init>(...);
}

# LiveEventBus
-keepclassmembers class androidx.lifecycle.LiveData {
    *** mObservers;
    *** mActiveCount;
}
-keepclassmembers class androidx.arch.core.internal.SafeIterableMap {
    *** size();
    *** putIfAbsent(...);
}

# ================================================
# 忽略警告（避免因可选依赖导致构建中断）
# ================================================
# JSpecify 注解
-dontwarn org.jspecify.annotations.NullMarked

# Markdown 扩展（删除线）
-dontwarn org.commonmark.ext.gfm.**
