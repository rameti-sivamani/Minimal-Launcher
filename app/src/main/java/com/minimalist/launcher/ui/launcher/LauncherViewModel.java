package com.minimalist.launcher.ui.launcher;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.FocusModeDao;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.repository.UsageRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * ViewModel for the Launcher home screen
 * Manages current time, date, and usage statistics display
 */
public class LauncherViewModel extends AndroidViewModel {
    
    private final UsageRepository usageRepository;
    private final FocusModeDao focusModeDao;
    
    private final MutableLiveData<String> currentTime;
    private final MutableLiveData<String> currentDate;
    private final LiveData<Long> totalScreenTime;
    private final LiveData<FocusMode> activeFocusMode;
    
    public LauncherViewModel(@NonNull Application application) {
        super(application);
        
        this.usageRepository = new UsageRepository(application);
        AppDatabase database = AppDatabase.getInstance(application);
        this.focusModeDao = database.focusModeDao();
        
        this.currentTime = new MutableLiveData<>();
        this.currentDate = new MutableLiveData<>();
        this.totalScreenTime = usageRepository.getTotalScreenTime();
        this.activeFocusMode = focusModeDao.getActiveFocusMode();
        
        updateTimeAndDate();
    }
    
    /**
     * Update current time and date
     * Should be called every minute
     */
    public void updateTimeAndDate() {
        Calendar calendar = Calendar.getInstance();
        
        // Format time as HH:mm
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        currentTime.setValue(timeFormat.format(calendar.getTime()));
        
        // Format date as "Day, Month Date" (e.g., "Monday, Dec 27")
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());
        currentDate.setValue(dateFormat.format(calendar.getTime()));
    }
    
    /**
     * Format screen time in milliseconds to human-readable string
     */
    public String formatScreenTime(Long milliseconds) {
        if (milliseconds == null || milliseconds == 0) {
            return "0m";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes % 60);
        } else {
            return String.format(Locale.getDefault(), "%dm", minutes);
        }
    }
    
    // Getters
    public LiveData<String> getCurrentTime() {
        return currentTime;
    }
    
    public LiveData<String> getCurrentDate() {
        return currentDate;
    }
    
    public LiveData<Long> getTotalScreenTime() {
        return totalScreenTime;
    }
    
    public LiveData<FocusMode> getActiveFocusMode() {
        return activeFocusMode;
    }
}
