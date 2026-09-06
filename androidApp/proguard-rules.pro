# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   https://developer.android.com/build/shrink-code

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers and source file attribute so release stack traces can be
# de-obfuscated with the mapping file from build/outputs/mapping/release/.
-keepattributes SourceFile,LineNumberTable

# Replace the original source file names with a placeholder, so the attribute
# above does not leak them into the shipped APK.
-renamesourcefileattribute SourceFile
