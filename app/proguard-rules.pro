# The widget provider and the activity are named in AndroidManifest.xml, which
# R8 already reads, so no keep rules are needed for them. Nothing else in this
# app is reached reflectively.

# Strip Log.d/Log.v from the shipped build.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
