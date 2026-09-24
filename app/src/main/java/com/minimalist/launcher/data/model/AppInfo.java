package com.minimalist.launcher.data.model;

import android.graphics.drawable.Drawable;

/**
 * Model class representing an installed application
 * Used to display apps in the launcher
 */
public class AppInfo implements Comparable<AppInfo> {

    private String appName;
    private String packageName;
    private Drawable icon;
    private boolean isSystemApp;
    private int launchCount; // Daily launch count
    private long installTime; // First install time, used for sorting
    private long usageMillis; // Foreground time today (0 without usage access)
    private int limitMinutes; // Daily time limit, 0 = none

    public AppInfo(String appName, String packageName, Drawable icon, boolean isSystemApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.isSystemApp = isSystemApp;
        this.launchCount = 0;
    }

    // Getters and Setters
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public void setSystemApp(boolean systemApp) {
        isSystemApp = systemApp;
    }

    public int getLaunchCount() {
        return launchCount;
    }

    public void setLaunchCount(int launchCount) {
        this.launchCount = launchCount;
    }

    public long getUsageMillis() {
        return usageMillis;
    }

    public void setUsageMillis(long usageMillis) {
        this.usageMillis = usageMillis;
    }

    public int getLimitMinutes() {
        return limitMinutes;
    }

    public void setLimitMinutes(int limitMinutes) {
        this.limitMinutes = limitMinutes;
    }

    public long getInstallTime() {
        return installTime;
    }

    public void setInstallTime(long installTime) {
        this.installTime = installTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        AppInfo appInfo = (AppInfo) obj;
        return packageName.equals(appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }

    @Override
    public int compareTo(AppInfo other) {
        // Sort alphabetically by app name (case-insensitive)
        return this.appName.compareToIgnoreCase(other.appName);
    }
}
