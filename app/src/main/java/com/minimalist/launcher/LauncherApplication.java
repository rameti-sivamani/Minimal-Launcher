package com.minimalist.launcher;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.minimalist.launcher.workers.DailyResetWorker;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Application class for Minimalist Launcher
 * Initializes WorkManager for scheduled tasks
 */
public class LauncherApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Schedule daily reset at midnight
        scheduleDailyReset();
    }
    
    /**
     * Schedule daily reset of launch counters at midnight
     * Uses WorkManager for reliable background execution
     */
    private void scheduleDailyReset() {
        // Calculate delay until midnight
        Calendar now = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        
        long initialDelay = midnight.getTimeInMillis() - now.getTimeInMillis();
        
        // Create periodic work request (daily)
        PeriodicWorkRequest dailyResetWork = new PeriodicWorkRequest.Builder(
            DailyResetWorker.class,
            1,
            TimeUnit.DAYS
        )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build();
        
        // KEEP: don't restart the schedule every time the process starts
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reset",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyResetWork
        );
    }
}
