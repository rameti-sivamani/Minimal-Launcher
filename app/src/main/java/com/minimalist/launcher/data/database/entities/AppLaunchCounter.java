package com.minimalist.launcher.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Entity representing daily app launch counters
 * Resets at midnight to track daily usage
 */
@Entity(tableName = "app_launch_counter")
public class AppLaunchCounter {

    @PrimaryKey
    @NonNull
    private String packageName;

    @NonNull
    private String appName;

    private int launchCount; // Number of times launched today

    private String date; // Current date in format yyyy-MM-dd

    private boolean showWarning; // Whether to show "Do you really need this?" dialog

    private int warningThreshold; // Launch count threshold for warning (default: 10)

    // Constructors
    public AppLaunchCounter() {
        this.launchCount = 0;
        this.showWarning = false;
        this.warningThreshold = 10;
    }

    public AppLaunchCounter(@NonNull String packageName, @NonNull String appName, String date) {
        this.packageName = packageName;
        this.appName = appName;
        this.date = date;
        this.launchCount = 0;
        this.showWarning = false;
        this.warningThreshold = 10;
    }

    // Getters and Setters
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

    public int getLaunchCount() {
        return launchCount;
    }

    public void setLaunchCount(int launchCount) {
        this.launchCount = launchCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isShowWarning() {
        return showWarning;
    }

    public void setShowWarning(boolean showWarning) {
        this.showWarning = showWarning;
    }

    public int getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(int warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    /**
     * Increment launch count and update warning flag if threshold exceeded
     */
    public void incrementLaunchCount() {
        this.launchCount++;
        if (this.launchCount >= this.warningThreshold) {
            this.showWarning = true;
        }
    }
}
