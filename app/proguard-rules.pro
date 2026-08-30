# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Keep the service registered in manifest
-keep class com.letmese.aikeyboard.AiKeyboardService { *; }

# Keep the activity
-keep class com.letmese.aikeyboard.MainActivity { *; }

# Keep the AI client (used from background thread)
-keep class com.letmese.aikeyboard.AiClient { *; }

# Keep the prefs helper (reflection-safe)
-keep class com.letmese.aikeyboard.Prefs { *; }