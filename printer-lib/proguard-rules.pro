# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,SourceFile,LineNumberTable,*Annotation*
-keep class com.wind.printer_lib.aidl.IPrinter$** {
    public <fields>;
    public <methods>;
}

-keep class com.wind.printer_lib.aidl.IPrinter$Stub.** {
    public <fields>;
    public <methods>;
}

-keep interface com.wind.printer_lib.aidl.IPrinter$** {*;}


-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclasseswithmembers class com.wind.printer_lib.PrinterService {
    public static <fields>;
}
-keep class com.wind.printer_lib.aidl.**{*;}
-keep class com.wind.printer_lib.DeviceUtil{*;}
-keep class com.wind.printer_lib.DeviceState{*;}
-keep class com.wind.printer_lib.ErrorCode{*;}
-keep class com.wind.printer_lib.ConnectMode{*;}
-keep class com.wind.printer_lib.PrinterException{*;}

