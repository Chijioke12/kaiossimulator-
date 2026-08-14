# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep our application classes intact to prevent reflective or lifecycle issues
-keep class com.example.** { *; }

# Keep NanoHTTPD classes intact
-keep class org.nanohttpd.** { *; }

# Keep all WebResourceResponse / WebView methods
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}

# Preserve Line Numbers and annotations for debugging
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

