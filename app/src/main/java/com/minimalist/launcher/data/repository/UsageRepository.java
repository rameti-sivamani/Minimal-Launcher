package com.minimalist.launcher.data.repository;

import android.app.usage.UsageStatsManager;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.AppUsageDao;
import com.minimalist.launcher.data.database.entities.AppUsage;
import com.minimalist.launcher.data.model.UsageStats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for managing app usage statistics
 * Integrates with Android's UsageStatsManager and local database
 */
public class UsageRepository {

    private final Context context;
    private final AppUsageDao appUsageDao;
    private final UsageStatsManager usageStatsManager;
    private final Executor executor;

    public UsageRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        this.appUsageDao = database.appUsageDao();
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Get total screen time for today from database
     */
    public LiveData<Long> getTotalScreenTime() {
        return appUsageDao.getTotalScreenTime(getTodayDate());
    }

    /**
     * Get today's usage records
     */
    public LiveData<List<AppUsage>> getTodayUsage() {
        return appUsageDao.getTodayUsage(getTodayDate());
    }

    /**
     * Record app usage
     * Called when an app is launched
     */
    public void recordAppUsage(String packageName, String appName, long launchTime, long duration) {
        executor.execute(() -> {
            AppUsage usage = new AppUsage(
                    packageName,
                    appName,
                    launchTime,
                    duration,
                    getTodayDate());
            appUsageDao.insert(usage);
        });
    }

    /**
     * Get usage summary for today
     * Aggregates data from database
     */
    public LiveData<List<AppUsageDao.AppUsageSummary>> getUsageSummary() {
        return appUsageDao.getUsageSummary(getTodayDate());
    }

    /**
     * Clean up old usage data (older than 30 days)
     */
    public void cleanupOldData() {
        executor.execute(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            String cutoffDate = sdf.format(new Date(thirtyDaysAgo));
            appUsageDao.deleteOldRecords(cutoffDate);
        });
    }

    /**
     * Get today's date in yyyy-MM-dd format
     */
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Check if usage stats permission is granted
     */
    public boolean hasUsageStatsPermission() {
        if (usageStatsManager == null) {
            return false;
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - 1000 * 60; // Check last minute

        android.app.usage.UsageStats stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime).stream().findFirst().orElse(null);

        return stats != null;
    }
}
