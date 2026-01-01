package com.minimalist.launcher.data.model;

/**
 * Model class representing usage statistics summary
 * Used to display daily screen time on home screen
 */
public class UsageStats {

    private long totalScreenTime; // Total time in milliseconds
    private int totalAppLaunches;
    private int uniqueAppsUsed;
    private String date;

    public UsageStats(String date) {
        this.date = date;
        this.totalScreenTime = 0;
        this.totalAppLaunches = 0;
        this.uniqueAppsUsed = 0;
    }

    // Getters and Setters
    public long getTotalScreenTime() {
        return totalScreenTime;
    }

    public void setTotalScreenTime(long totalScreenTime) {
        this.totalScreenTime = totalScreenTime;
    }

    public int getTotalAppLaunches() {
        return totalAppLaunches;
    }

    public void setTotalAppLaunches(int totalAppLaunches) {
        this.totalAppLaunches = totalAppLaunches;
    }

    public int getUniqueAppsUsed() {
        return uniqueAppsUsed;
    }

    public void setUniqueAppsUsed(int uniqueAppsUsed) {
        this.uniqueAppsUsed = uniqueAppsUsed;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Format screen time as human-readable string
     * e.g., "2h 35m" or "45m"
     */
    public String getFormattedScreenTime() {
        long seconds = totalScreenTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
