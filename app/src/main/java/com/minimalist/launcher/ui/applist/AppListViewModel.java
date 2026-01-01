package com.minimalist.launcher.ui.applist;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.AppLaunchCounterDao;
import com.minimalist.launcher.data.database.dao.FocusModeDao;
import com.minimalist.launcher.data.database.entities.AppLaunchCounter;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.utils.AppFilterHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ViewModel for the app list screen
 * Manages app loading, filtering, and launch operations
 */
public class AppListViewModel extends AndroidViewModel {

    private final AppRepository appRepository;
    private final AppLaunchCounterDao launchCounterDao;
    private final FocusModeDao focusModeDao;
    private final Executor executor;

    private final MutableLiveData<List<AppInfo>> allApps;
    private final MutableLiveData<List<AppInfo>> filteredApps;
    private final MutableLiveData<String> searchQuery;
    private final LiveData<List<AppLaunchCounter>> launchCounters;
    private final LiveData<FocusMode> activeFocusMode;

    private boolean includeSystemApps = false;
    private boolean isCacheValid = false; // Cache validity flag
    private long lastLoadTime = 0; // Timestamp of last load
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes cache

    public AppListViewModel(@NonNull Application application) {
        super(application);

        this.appRepository = new AppRepository(application);
        AppDatabase database = AppDatabase.getInstance(application);
        this.launchCounterDao = database.appLaunchCounterDao();
        this.focusModeDao = database.focusModeDao();
        this.executor = Executors.newSingleThreadExecutor();

        this.allApps = new MutableLiveData<>(new ArrayList<>());
        this.filteredApps = new MutableLiveData<>(new ArrayList<>());
        this.searchQuery = new MutableLiveData<>("");
        this.launchCounters = appRepository.getAllLaunchCounters();
        this.activeFocusMode = focusModeDao.getActiveFocusMode();

        // DON'T load apps in constructor - let Activity call loadApps()
    }

    /**
     * Load all installed apps (with smart caching)
     */
    public void loadApps() {
        // Check if we already have apps loaded (cache)
        List<AppInfo> cachedApps = allApps.getValue();
        if (cachedApps != null && !cachedApps.isEmpty() && isCacheValid) {
            // Use cached data and check if still valid
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastLoadTime) < CACHE_DURATION) {
                android.util.Log.d("AppListViewModel", "Using cached app list (" + cachedApps.size() + " apps)");
                filterApps();
                return;
            }
        }

        // First load or cache expired - load fresh data
        android.util.Log.d("AppListViewModel", "Loading fresh app list from PackageManager");
        executor.execute(() -> {
            List<AppInfo> apps = loadAllLaunchableApps();
            allApps.postValue(apps);
            isCacheValid = true;
            lastLoadTime = System.currentTimeMillis();
            filterApps();
        });
    }

    /**
     * Force reload apps (ignoring cache)
     */
    public void forceReloadApps() {
        isCacheValid = false;
        loadApps();
    }

    /**
     * Load all launchable apps including hidden ones check
     */
    private List<AppInfo> loadAllLaunchableApps() {
        List<AppInfo> appList = new ArrayList<>();
        PackageManager pm = getApplication().getPackageManager();

        // Get hidden apps
        com.minimalist.launcher.utils.HiddenAppsManager hiddenAppsManager = new com.minimalist.launcher.utils.HiddenAppsManager(
                getApplication());
        List<String> hiddenApps = hiddenAppsManager.getHiddenApps();

        // Query for all apps with MAIN action and LAUNCHER category
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(mainIntent,
                PackageManager.MATCH_ALL);

        for (ResolveInfo resolveInfo : resolveInfoList) {
            try {
                String packageName = resolveInfo.activityInfo.packageName;

                // Skip hidden apps
                if (hiddenApps.contains(packageName)) {
                    continue;
                }

                // Skip this launcher itself
                if (packageName.equals(getApplication().getPackageName())) {
                    continue;
                }

                String appName = resolveInfo.loadLabel(pm).toString();
                Drawable icon = resolveInfo.loadIcon(pm);

                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                AppInfo app = new AppInfo(appName, packageName, icon, isSystemApp);
                appList.add(app);
            } catch (PackageManager.NameNotFoundException e) {
                // Skip if app not found
                android.util.Log.e("AppListViewModel", "App not found", e);
            }
        }

        // Sort alphabetically
        Collections.sort(appList);

        return appList;
    }

    /**
     * Filter apps based on search query and focus mode
     */
    private void filterApps() {
        executor.execute(() -> {
            List<AppInfo> apps = allApps.getValue();
            if (apps == null)
                return;

            String query = searchQuery.getValue();
            FocusMode focusMode = focusModeDao.getActiveFocusModeSync();

            List<AppInfo> filtered = new ArrayList<>(apps);

            // Apply focus mode filter
            if (focusMode != null && focusMode.isActive()) {
                filtered = AppFilterHelper.filterByFocusMode(filtered, focusMode);
            }

            // Apply search filter
            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase();
                List<AppInfo> searchFiltered = new ArrayList<>();
                for (AppInfo app : filtered) {
                    if (app.getAppName().toLowerCase().contains(lowerQuery)) {
                        searchFiltered.add(app);
                    }
                }
                filtered = searchFiltered;
            }

            filteredApps.postValue(filtered);
        });
    }

    /**
     * Update search query and re-filter apps
     */
    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
        filterApps();
    }

    /**
     * Get search query LiveData
     */
    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

    /**
     * Toggle system apps visibility
     */
    public void toggleSystemApps() {
        includeSystemApps = !includeSystemApps;
        loadApps();
    }

    /**
     * Launch an app
     */
    public void launchApp(String packageName) {
        appRepository.launchApp(packageName);
    }

    /**
     * Immediately remove app from current list (for hide action)
     */
    public void removeAppFromList(String packageName) {
        List<AppInfo> currentApps = allApps.getValue();
        if (currentApps != null) {
            List<AppInfo> updatedApps = new ArrayList<>();
            for (AppInfo app : currentApps) {
                if (!app.getPackageName().equals(packageName)) {
                    updatedApps.add(app);
                }
            }
            allApps.setValue(updatedApps);
            filterApps();
        }

        // Also invalidate cache so next load is fresh
        isCacheValid = false;
    }

    /**
     * Immediately add app to current list (for unhide action)
     */
    public void addAppToList(AppInfo app) {
        List<AppInfo> currentApps = allApps.getValue();
        if (currentApps != null) {
            List<AppInfo> updatedApps = new ArrayList<>(currentApps);
            updatedApps.add(app);
            Collections.sort(updatedApps); // Keep alphabetical order
            allApps.setValue(updatedApps);
            filterApps();
        }

        // Also invalidate cache so next load is fresh
        isCacheValid = false;
    }

    /**
     * Get launch counter for a specific app
     */
    public LiveData<AppLaunchCounter> getLaunchCounter(String packageName) {
        return appRepository.getLaunchCounter(packageName);
    }

    // Getters
    public LiveData<List<AppInfo>> getFilteredApps() {
        return filteredApps;
    }

    public LiveData<List<AppLaunchCounter>> getLaunchCounters() {
        return launchCounters;
    }

    public LiveData<FocusMode> getActiveFocusMode() {
        return activeFocusMode;
    }

    public boolean isIncludeSystemApps() {
        return includeSystemApps;
    }
}
