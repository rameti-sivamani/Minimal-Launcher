package com.minimalist.launcher.ui.focus;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.FocusModeDao;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.utils.AppFilterHelper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel for Focus Mode management
 * Handles CRUD operations for focus modes
 */
public class FocusModeViewModel extends AndroidViewModel {
    
    private final FocusModeDao focusModeDao;
    private final LiveData<List<FocusMode>> allFocusModes;
    private final LiveData<FocusMode> activeFocusMode;
    private final Executor executor;
    private final AppRepository appRepository;
    
    public FocusModeViewModel(@NonNull Application application) {
        super(application);
        
        AppDatabase database = AppDatabase.getInstance(application);
        this.focusModeDao = database.focusModeDao();
        this.allFocusModes = focusModeDao.getAllFocusModes();
        this.activeFocusMode = focusModeDao.getActiveFocusMode();
        this.executor = Executors.newSingleThreadExecutor();
        this.appRepository = new AppRepository(application);
    }

    /**
     * Load all launchable apps (including hidden ones) for the allowed-apps picker
     */
    public void loadSelectableApps(Consumer<List<AppInfo>> callback) {
        Handler main = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<AppInfo> apps = appRepository.getLaunchableApps(Collections.emptyList(), false);
            main.post(() -> callback.accept(apps));
        });
    }
    
    /**
     * Create a new focus mode
     */
    public void createFocusMode(FocusMode focusMode) {
        executor.execute(() -> {
            focusModeDao.insert(focusMode);
        });
    }
    
    /**
     * Update existing focus mode
     */
    public void updateFocusMode(FocusMode focusMode) {
        executor.execute(() -> {
            focusModeDao.update(focusMode);
        });
    }
    
    /**
     * Delete focus mode
     */
    public void deleteFocusMode(FocusMode focusMode) {
        executor.execute(() -> {
            focusModeDao.delete(focusMode);
        });
    }
    
    /**
     * Activate a focus mode (deactivates all others first)
     */
    public void activateFocusMode(int focusModeId) {
        executor.execute(() -> {
            focusModeDao.deactivateAll();
            focusModeDao.activate(focusModeId);
        });
    }
    
    /**
     * Deactivate all focus modes
     */
    public void deactivateAll() {
        executor.execute(() -> {
            focusModeDao.deactivateAll();
        });
    }
    
    /**
     * Create default "Deep Work" focus mode
     */
    public void createDeepWorkMode() {
        String allowedApps = AppFilterHelper.packageSetToString(
            AppFilterHelper.getDeepWorkAllowlist()
        );
        
        FocusMode deepWork = new FocusMode("Deep Work", allowedApps);
        createFocusMode(deepWork);
    }
    
    // Getters
    public LiveData<List<FocusMode>> getAllFocusModes() {
        return allFocusModes;
    }
    
    public LiveData<FocusMode> getActiveFocusMode() {
        return activeFocusMode;
    }
}
