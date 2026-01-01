package com.minimalist.launcher.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.minimalist.launcher.data.database.entities.AppLaunchCounter;

import java.util.List;

/**
 * Data Access Object for AppLaunchCounter entity
 * Manages daily app launch counts
 */
@Dao
public interface AppLaunchCounterDao {

    /**
     * Insert or replace app launch counter
     * Used when initializing counter for a new app
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppLaunchCounter counter);

    /**
     * Update existing counter
     */
    @Update
    void update(AppLaunchCounter counter);

    /**
     * Get counter for a specific app on current date
     */
    @Query("SELECT * FROM app_launch_counter WHERE packageName = :packageName AND date = :date")
    AppLaunchCounter getCounter(String packageName, String date);

    /**
     * Get counter as LiveData for observing changes
     */
    @Query("SELECT * FROM app_launch_counter WHERE packageName = :packageName AND date = :date")
    LiveData<AppLaunchCounter> getCounterLive(String packageName, String date);

    /**
     * Get all counters for today
     */
    @Query("SELECT * FROM app_launch_counter WHERE date = :date ORDER BY launchCount DESC")
    LiveData<List<AppLaunchCounter>> getAllCounters(String date);

    /**
     * Get top N most launched apps today
     */
    @Query("SELECT * FROM app_launch_counter WHERE date = :date ORDER BY launchCount DESC LIMIT :limit")
    LiveData<List<AppLaunchCounter>> getTopApps(String date, int limit);

    /**
     * Delete all counters (used for daily reset)
     */
    @Query("DELETE FROM app_launch_counter")
    void deleteAll();

    /**
     * Delete counters from previous dates
     */
    @Query("DELETE FROM app_launch_counter WHERE date != :currentDate")
    void deleteOldCounters(String currentDate);

    /**
     * Check if counter exists for a package
     */
    @Query("SELECT COUNT(*) FROM app_launch_counter WHERE packageName = :packageName AND date = :date")
    int counterExists(String packageName, String date);
}
