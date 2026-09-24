# R8 rules for Minimalist Launcher
# AndroidX (Room, Lifecycle, WorkManager) ship their own consumer rules,
# so only app-specific rules live here.

# Room entities are read reflectively by generated code only through
# their fields; keep them intact so schema and column names stay stable.
-keep class com.minimalist.launcher.data.database.entities.** { *; }

# Strip verbose logging from release builds (keeps Log.w / Log.e)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep line numbers for readable crash reports in Play Console
# (upload the generated mapping.txt with each release)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
