package com.minimalist.launcher.ui.applist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.FocusModeDao;
import com.minimalist.launcher.data.database.entities.AppLaunchCounter;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.repository.UsageRepository;
import com.minimalist.launcher.utils.AppFilterHelper;
import com.minimalist.launcher.utils.AppLimitsManager;
import com.minimalist.launcher.utils.HiddenAppsManager;
import com.minimalist.launcher.utils.LockInManager;
import com.minimalist.launcher.utils.Prefs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the app list screen: loading, search, focus-mode filtering and sorting.
 * All list state is owned by the single background executor, so loads and filters
 * can never race each other.
 */
public class AppListViewModel extends AndroidViewModel {

    private static final long CACHE_DURATION_MS = 5 * 60 * 1000;

    private final AppRepository appRepository;
    private final FocusModeDao focusModeDao;
    private final HiddenAppsManager hiddenAppsManager;
    private final UsageRepository usageRepository;
    private final AppLimitsManager limitsManager;
    private final LockInManager lockIn;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<AppInfo>> filteredApps = new MutableLiveData<>();
    private final LiveData<List<AppLaunchCounter>> launchCounters;

    // Only touched on the executor thread
    private List<AppInfo> allApps = new ArrayList<>();
    private boolean iconsLoaded = false;
    private long lastLoadTime = 0;
    private List<String> loadedHiddenApps = new ArrayList<>();
    private Map<String, Integer> launchCounts = new HashMap<>();
    private Map<String, Long> usageToday = new HashMap<>();

    // Written on the main thread, read on the executor
    private volatile String searchQuery = "";
    private volatile boolean showIcons;
    private volatile int sortOrder;

    public AppListViewModel(@NonNull Application application) {
        super(application);
        this.appRepository = new AppRepository(application);
        this.focusModeDao = AppDatabase.getInstance(application).focusModeDao();
        this.hiddenAppsManager = new HiddenAppsManager(application);
        this.usageRepository = new UsageRepository(application);
        this.limitsManager = new AppLimitsManager(application);
        this.lockIn = new LockInManager(application);
        this.launchCounters = appRepository.getAllLaunchCounters();
    }

    /**
     * Load apps, reusing the cached list if it is recent and settings haven't changed
     */
    public void loadApps() {
        readSettings();
        executor.execute(() -> {
            boolean expired = System.currentTimeMillis() - lastLoadTime > CACHE_DURATION_MS;
            boolean needsIcons = showIcons && !iconsLoaded;
            boolean hiddenChanged = !hiddenAppsManager.getHiddenApps().equals(loadedHiddenApps);
            if (allApps.isEmpty() || expired || needsIcons || hiddenChanged) {
                reloadOnExecutor();
            }
            refreshUsageOnExecutor();
            filterOnExecutor();
        });
    }

    /**
     * Today's time per app for the badges (empty without usage access)
     */
    @WorkerThread
    private void refreshUsageOnExecutor() {
        Map<String, Long> usage = usageRepository.getTodayUsagePerApp();
        usageToday = usage != null ? usage : new HashMap<>();
    }

    /**
     * Reload from PackageManager (after install/uninstall/unhide)
     */
    public void forceReloadApps() {
        readSettings();
        executor.execute(() -> {
            reloadOnExecutor();
            filterOnExecutor();
        });
    }

    private void readSettings() {
        showIcons = Prefs.showIcons(getApplication());
        sortOrder = Prefs.sortOrder(getApplication());
    }

    @WorkerThread
    private void reloadOnExecutor() {
        loadedHiddenApps = hiddenAppsManager.getHiddenApps();
        allApps = appRepository.getLaunchableApps(loadedHiddenApps, showIcons);
        iconsLoaded = showIcons;
        lastLoadTime = System.currentTimeMillis();
    }

    @WorkerThread
    private void filterOnExecutor() {
        List<AppInfo> result = new ArrayList<>(allApps);

        FocusMode focusMode = AppFilterHelper.resolveActive(
                focusModeDao.getAllFocusModesSync(), Calendar.getInstance());
        if (focusMode != null) {
            result = AppFilterHelper.filterByFocusMode(result, focusMode);
        }

        // A running lock-in session narrows the list to the apps chosen for it
        if (lockIn.isActive()) {
            java.util.Set<String> allowed = lockIn.getAllowedApps();
            List<AppInfo> locked = new ArrayList<>();
            for (AppInfo app : result) {
                if (allowed.contains(app.getPackageName())) {
                    locked.add(app);
                }
            }
            result = locked;
        }

        String query = searchQuery.trim().toLowerCase(Locale.getDefault());
        if (!query.isEmpty()) {
            List<AppInfo> matches = new ArrayList<>();
            for (AppInfo app : result) {
                if (app.getAppName().toLowerCase(Locale.getDefault()).contains(query)) {
                    matches.add(app);
                }
            }
            result = matches;
        }

        for (AppInfo app : result) {
            Integer count = launchCounts.get(app.getPackageName());
            app.setLaunchCount(count != null ? count : 0);
            Long usage = usageToday.get(app.getPackageName());
            app.setUsageMillis(usage != null ? usage : 0);
            app.setLimitMinutes(limitsManager.getLimitMinutes(app.getPackageName()));
        }

        sort(result, sortOrder);
        filteredApps.postValue(result);
    }

    private static void sort(List<AppInfo> apps, int sortOrder) {
        if (sortOrder == Prefs.SORT_MOST_USED) {
            // Screen time first (when usage access is on), then launches
            Comparator<AppInfo> byLaunches = Comparator.comparingInt(AppInfo::getLaunchCount).reversed();
            Collections.sort(apps, Comparator
                    .comparingLong(AppInfo::getUsageMillis).reversed()
                    .thenComparing(byLaunches)
                    .thenComparing(Comparator.naturalOrder()));
        } else if (sortOrder == Prefs.SORT_RECENTLY_INSTALLED) {
            Collections.sort(apps, Comparator
                    .comparingLong(AppInfo::getInstallTime).reversed()
                    .thenComparing(Comparator.naturalOrder()));
        } else {
            Collections.sort(apps);
        }
    }

    public void setSearchQuery(String query) {
        searchQuery = query != null ? query : "";
        executor.execute(this::filterOnExecutor);
    }

    /**
     * Update the per-app launch counts shown as badges
     */
    public void setLaunchCounters(List<AppLaunchCounter> counters) {
        Map<String, Integer> counts = new HashMap<>();
        if (counters != null) {
            for (AppLaunchCounter counter : counters) {
                counts.put(counter.getPackageName(), counter.getLaunchCount());
            }
        }
        executor.execute(() -> {
            launchCounts = counts;
            filterOnExecutor();
        });
    }

    /**
     * Hide an app and drop it from the list immediately
     */
    public void hideApp(String packageName) {
        executor.execute(() -> {
            hiddenAppsManager.hideApp(packageName);
            loadedHiddenApps = hiddenAppsManager.getHiddenApps();
            List<AppInfo> updated = new ArrayList<>();
            for (AppInfo app : allApps) {
                if (!app.getPackageName().equals(packageName)) {
                    updated.add(app);
                }
            }
            allApps = updated;
            filterOnExecutor();
        });
    }

    public AppRepository getAppRepository() {
        return appRepository;
    }

    public LiveData<List<AppInfo>> getFilteredApps() {
        return filteredApps;
    }

    public LiveData<List<AppLaunchCounter>> getLaunchCounters() {
        return launchCounters;
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
    }
}
