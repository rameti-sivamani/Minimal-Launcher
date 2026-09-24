package com.minimalist.launcher.ui.launcher;

import android.app.Application;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.repository.UsageRepository;
import com.minimalist.launcher.utils.FavoritesHelper;
import com.minimalist.launcher.utils.Prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the Launcher home screen
 * Manages clock, date, screen time, favorites and the active focus mode
 */
public class LauncherViewModel extends AndroidViewModel {

    // First-run favorites: the first installed package of each group (phone, messages, camera, settings)
    private static final List<List<String>> DEFAULT_FAVORITE_GROUPS = Arrays.asList(
            Arrays.asList("com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer"),
            Arrays.asList("com.google.android.apps.messaging", "com.samsung.android.messaging",
                    "com.android.mms", "com.android.messaging"),
            Arrays.asList("com.google.android.GoogleCamera", "com.sec.android.app.camera", "com.android.camera"),
            Arrays.asList("com.android.settings"));

    private final UsageRepository usageRepository;
    private final AppRepository appRepository;
    private final FavoritesHelper favoritesHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> currentTime = new MutableLiveData<>();
    private final MutableLiveData<String> currentDate = new MutableLiveData<>();
    private final MutableLiveData<Long> screenTimeMillis = new MutableLiveData<>();
    private final MutableLiveData<List<AppInfo>> favorites = new MutableLiveData<>(new ArrayList<>());
    private final LiveData<List<FocusMode>> allFocusModes;

    public LauncherViewModel(@NonNull Application application) {
        super(application);

        this.usageRepository = new UsageRepository(application);
        this.appRepository = new AppRepository(application);
        this.favoritesHelper = new FavoritesHelper(application);
        this.allFocusModes = AppDatabase.getInstance(application).focusModeDao().getAllFocusModes();

        updateTimeAndDate();
    }

    /**
     * Update clock and date text. Follows the system 12/24-hour setting and locale.
     */
    public void updateTimeAndDate() {
        Application app = getApplication();
        Locale locale = Locale.getDefault();
        Date now = new Date();

        String timeSkeleton = DateFormat.is24HourFormat(app) ? "Hm" : "hm";
        currentTime.setValue(DateFormat.format(
                DateFormat.getBestDateTimePattern(locale, timeSkeleton), now).toString());

        String dateSkeleton = Prefs.showDayOfWeek(app) ? "EEEEMMMd" : "MMMd";
        currentDate.setValue(DateFormat.format(
                DateFormat.getBestDateTimePattern(locale, dateSkeleton), now).toString());
    }

    /**
     * Recompute today's screen time in the background (-1 = no usage access)
     */
    public void refreshScreenTime() {
        executor.execute(() -> screenTimeMillis.postValue(usageRepository.getTodayScreenTimeMillis()));
    }

    /**
     * Reload favorite apps in their saved order
     */
    public void loadFavorites() {
        final boolean loadIcons = Prefs.showIcons(getApplication());
        executor.execute(() -> {
            List<String> installedDefaults = new ArrayList<>();
            for (List<String> group : DEFAULT_FAVORITE_GROUPS) {
                for (String pkg : group) {
                    if (appRepository.isLaunchable(pkg)) {
                        installedDefaults.add(pkg);
                        break;
                    }
                }
            }
            favoritesHelper.applyDefaultsOnce(installedDefaults);

            List<AppInfo> apps = new ArrayList<>();
            for (String packageName : favoritesHelper.getFavorites()) {
                AppInfo app = appRepository.getApp(packageName, loadIcons);
                if (app != null) {
                    apps.add(app);
                } else {
                    // Uninstalled since it was added
                    favoritesHelper.removeFavorite(packageName);
                }
            }
            favorites.postValue(apps);
        });
    }

    public void removeFavorite(String packageName) {
        executor.execute(() -> favoritesHelper.removeFavorite(packageName));
        loadFavorites();
    }

    public void moveFavorite(String packageName, int direction) {
        executor.execute(() -> favoritesHelper.move(packageName, direction));
        loadFavorites();
    }

    public AppRepository getAppRepository() {
        return appRepository;
    }

    public LiveData<String> getCurrentTime() {
        return currentTime;
    }

    public LiveData<String> getCurrentDate() {
        return currentDate;
    }

    public LiveData<Long> getScreenTimeMillis() {
        return screenTimeMillis;
    }

    public LiveData<List<AppInfo>> getFavorites() {
        return favorites;
    }

    public LiveData<List<FocusMode>> getAllFocusModes() {
        return allFocusModes;
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
    }
}
