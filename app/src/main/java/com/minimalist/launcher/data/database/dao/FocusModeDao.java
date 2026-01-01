package com.minimalist.launcher.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.minimalist.launcher.data.database.entities.FocusMode;

import java.util.List;

/**
 * Data Access Object for FocusMode entity
 * Manages focus mode configurations
 */
@Dao
public interface FocusModeDao {

    /**
     * Insert new focus mode
     */
    @Insert
    long insert(FocusMode focusMode);

    /**
     * Update existing focus mode
     */
    @Update
    void update(FocusMode focusMode);

    /**
     * Delete focus mode
     */
    @Delete
    void delete(FocusMode focusMode);

    /**
     * Get all focus modes
     */
    @Query("SELECT * FROM focus_mode ORDER BY name ASC")
    LiveData<List<FocusMode>> getAllFocusModes();

    /**
     * Get currently active focus mode
     */
    @Query("SELECT * FROM focus_mode WHERE isActive = 1 LIMIT 1")
    LiveData<FocusMode> getActiveFocusMode();

    /**
     * Get currently active focus mode (non-live)
     */
    @Query("SELECT * FROM focus_mode WHERE isActive = 1 LIMIT 1")
    FocusMode getActiveFocusModeSync();

    /**
     * Get focus mode by ID
     */
    @Query("SELECT * FROM focus_mode WHERE id = :id")
    LiveData<FocusMode> getFocusModeById(int id);

    /**
     * Deactivate all focus modes
     * Used before activating a new mode
     */
    @Query("UPDATE focus_mode SET isActive = 0")
    void deactivateAll();

    /**
     * Activate a specific focus mode
     */
    @Query("UPDATE focus_mode SET isActive = 1 WHERE id = :id")
    void activate(int id);

    /**
     * Get focus modes with schedules
     */
    @Query("SELECT * FROM focus_mode WHERE hasSchedule = 1")
    List<FocusMode> getScheduledFocusModes();
}
