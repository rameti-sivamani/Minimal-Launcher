# ProGuard rules for Minimalist Launcher
# Optimized for release builds

# Keep Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep interface * extends androidx.room.Dao
-dontwarn androidx.room.paging.**

# Keep ViewModel and LiveData
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}
-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.MutableLiveData { *; }

# Keep all data models and entities
-keep class com.minimalist.launcher.data.** { *; }
-keep class com.minimalist.launcher.data.model.** { *; }
-keep class com.minimalist.launcher.data.database.entities.** { *; }

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep RecyclerView adapters
-keep class * extends androidx.recyclerview.widget.RecyclerView$Adapter {
    <init>(...);
}
-keep class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    <init>(...);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep utility classes
-keep class com.minimalist.launcher.utils.** { *; }

# Keep Activities, Services, BroadcastReceivers
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
