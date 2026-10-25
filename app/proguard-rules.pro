# keep AI model code
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# keep button click methods
-keepclassmembers class com.example.soilmetrics.** {
    public void *(android.view.View);
}

# keep google sign in
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# keep pose AI classes
-keep class ai.onnxruntime.** { *; }
