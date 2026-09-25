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
import com.minimalist.launcher.data.wellbeing.StreakCalculator;
import com.minimalist.launcher.data.wellbeing.WellbeingStore;
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
    private final WellbeingStore wellbeingStore;
    private String lastBackfillDay = null;
    private static final long SCREEN_TIME_REFRESH_MS = 60_000;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long lastScreenTimeRefresh = 0;
    // Package names + icon setting of the favorites last shown, to skip identical reloads
    private String lastFavoritesKey = null;

    private final MutableLiveData<String> currentTime = new MutableLiveData<>();
    private final MutableLiveData<String> currentDate = new MutableLiveData<>();
    private final MutableLiveData<Wellbeing> wellbeing = new MutableLiveData<>();
    private final MutableLiveData<List<AppInfo>> favorites = new MutableLiveData<>(new ArrayList<>());
    private final LiveData<List<FocusMode>> allFocusModes;

    public LauncherViewModel(@NonNull Application application) {
        super(application);

        this.usageRepository = new UsageRepository(application);
        this.appRepository = new AppRepository(application);
        this.favoritesHelper = new FavoritesHelper(application);
        this.wellbeingStore = new WellbeingStore(application);
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

    /** Today's screen time against the goal, plus the streak */
    public static final class Wellbeing {
        /** -1 without usage access */
        public final long todayMillis;
        public final long goalMillis;
        public final int streak;

        Wellbeing(long todayMillis, long goalMillis, int streak) {
            this.todayMillis = todayMillis;
            this.goalMillis = goalMillis;
            this.streak = streak;
        }
    }

    /**
     * Recompute screen time and streak in the background.
     * Reading usage events is expensive, so this runs at most once a minute unless forced
     * (e.g. after the goal changed).
     */
    public void refreshWellbeing(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && wellbeing.getValue() != null && now - lastScreenTimeRefresh < SCREEN_TIME_REFRESH_MS) {
            return;
        }
        lastScreenTimeRefresh = now;
        executor.execute(() -> {
            String today = WellbeingStore.dateKey(0);
            if (!today.equals(lastBackfillDay)) {
                usageRepository.backfillDailyTotals(wellbeingStore);
                lastBackfillDay = today;
            }
            long todayMillis = usageRepository.getTodayScreenTimeMillis();
            long goal = wellbeingStore.getGoalMillis();
            int streak = StreakCalculator.displayStreak(wellbeingStore.getPastStreak(), todayMillis, goal);
            wellbeing.postValue(new Wellbeing(todayMillis, goal, streak));
        });
    }

    public WellbeingStore getWellbeingStore() {
        return wellbeingStore;
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
            StringBuilder key = new StringBuilder(loadIcons ? "i:" : "t:");
            for (AppInfo app : apps) {
                key.append(app.getPackageName()).append('|').append(app.getAppName()).append(',');
            }
            if (!key.toString().equals(lastFavoritesKey)) {
                lastFavoritesKey = key.toString();
                favorites.postValue(apps);
            }
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

    public LiveData<Wellbeing> getWellbeing() {
        return wellbeing;
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
