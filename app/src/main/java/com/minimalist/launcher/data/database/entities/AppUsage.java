package com.minimalist.launcher.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Entity representing app usage statistics
 * Tracks when apps are launched and for how long they're used
 */
@Entity(tableName = "app_usage")
public class AppUsage {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String packageName;

    @NonNull
    private String appName;

    private long launchTime; // Timestamp when app was launched

    private long duration; // Duration in milliseconds

    private String date; // Date in format yyyy-MM-dd

    // Constructors
    public AppUsage() {
    }

    public AppUsage(@NonNull String packageName, @NonNull String appName, long launchTime, long duration, String date) {
        this.packageName = packageName;
        this.appName = appName;
        this.launchTime = launchTime;
        this.duration = duration;
        this.date = date;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(@NonNull String packageName) {
        this.packageName = packageName;
    }

    @NonNull
    public String getAppName() {
        return appName;
    }

    public void setAppName(@NonNull String appName) {
        this.appName = appName;
    }

    public long getLaunchTime() {
        return launchTime;
    }

    public void setLaunchTime(long launchTime) {
        this.launchTime = launchTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
