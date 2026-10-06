-dontshrink
-dontoptimize
-adaptclassstrings
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions
-keep public class com.secureguard.SecureGuard {
    public <init>();
    public void onInitialize();
}
-keep class com.secureguard.mixin.** {
    *;
}