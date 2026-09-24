package com.minimalist.launcher.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.minimalist.launcher.data.database.dao.AppLaunchCounterDao;
import com.minimalist.launcher.data.database.dao.AppUsageDao;
import com.minimalist.launcher.data.database.dao.FocusModeDao;
import com.minimalist.launcher.data.database.entities.AppLaunchCounter;
import com.minimalist.launcher.data.database.entities.AppUsage;
import com.minimalist.launcher.data.database.entities.FocusMode;

/**
 * Room database for the Minimalist Launcher
 * Stores app usage data, launch counters, and focus mode configurations
 */
@Database(entities = { AppUsage.class, AppLaunchCounter.class, FocusMode.class }, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // DAOs
    public abstract AppUsageDao appUsageDao();

    public abstract AppLaunchCounterDao appLaunchCounterDao();

    public abstract FocusModeDao focusModeDao();

    /**
     * Singleton pattern to get database instance
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "minimalist_launcher_db")
                            // Bumping the version requires a Migration so users keep their data
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
