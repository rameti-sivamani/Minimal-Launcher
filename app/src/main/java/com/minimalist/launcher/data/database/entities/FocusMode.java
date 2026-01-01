package com.minimalist.launcher.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Entity representing a focus mode configuration
 * Allows users to create custom app allowlists for different contexts
 */
@Entity(tableName = "focus_mode")
public class FocusMode {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name; // e.g., "Deep Work", "Relax", "Study"

    @NonNull
    private String allowedApps; // Comma-separated package names

    private boolean isActive; // Whether this focus mode is currently active

    private boolean hasSchedule; // Whether this mode has scheduled periods

    private String startTime; // Format: HH:mm (e.g., "09:00")

    private String endTime; // Format: HH:mm (e.g., "17:00")

    private String activeDays; // Comma-separated day numbers: 1=Monday, 7=Sunday

    // Constructors
    public FocusMode() {
        this.isActive = false;
        this.hasSchedule = false;
        this.allowedApps = "";
        this.activeDays = "";
    }

    public FocusMode(@NonNull String name, @NonNull String allowedApps) {
        this.name = name;
        this.allowedApps = allowedApps;
        this.isActive = false;
        this.hasSchedule = false;
        this.activeDays = "";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getAllowedApps() {
        return allowedApps;
    }

    public void setAllowedApps(@NonNull String allowedApps) {
        this.allowedApps = allowedApps;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isHasSchedule() {
        return hasSchedule;
    }

    public void setHasSchedule(boolean hasSchedule) {
        this.hasSchedule = hasSchedule;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getActiveDays() {
        return activeDays;
    }

    public void setActiveDays(String activeDays) {
        this.activeDays = activeDays;
    }
}
