package com.minimalist.launcher.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.minimalist.launcher.data.database.entities.AppUsage;

import java.util.List;

/**
 * Data Access Object for AppUsage entity
 * Provides methods to query and manipulate app usage data
 */
@Dao
public interface AppUsageDao {

    /**
     * Insert new app usage record
     */
    @Insert
    void insert(AppUsage appUsage);

    /**
     * Update existing app usage record
     */
    @Update
    void update(AppUsage appUsage);

    /**
     * Get all app usage records for today
     */
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY launchTime DESC")
    LiveData<List<AppUsage>> getTodayUsage(String date);

    /**
     * Get usage records for a specific app on a specific date
     */
    @Query("SELECT * FROM app_usage WHERE packageName = :packageName AND date = :date")
    LiveData<List<AppUsage>> getAppUsageByDate(String packageName, String date);

    /**
     * Get total screen time for today (sum of all durations)
     */
    @Query("SELECT SUM(duration) FROM app_usage WHERE date = :date")
    LiveData<Long> getTotalScreenTime(String date);

    /**
     * Delete all usage records older than specified date
     * Used to clean up old data
     */
    @Query("DELETE FROM app_usage WHERE date < :date")
    void deleteOldRecords(String date);

    /**
     * Get usage count by app for a specific date
     */
    @Query("SELECT packageName, appName, COUNT(*) as launchCount, SUM(duration) as totalTime " +
            "FROM app_usage WHERE date = :date GROUP BY packageName ORDER BY launchCount DESC")
    LiveData<List<AppUsageSummary>> getUsageSummary(String date);

    /**
     * Simple class to hold aggregated usage summary
     */
    class AppUsageSummary {
        public String packageName;
        public String appName;
        public int launchCount;
        public long totalTime;
    }
}
