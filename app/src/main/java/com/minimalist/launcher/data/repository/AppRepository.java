package com.minimalist.launcher.data.repository;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.AppLaunchCounterDao;
import com.minimalist.launcher.data.database.entities.AppLaunchCounter;
import com.minimalist.launcher.data.model.AppInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for installed applications and their daily launch counters
 */
public class AppRepository {

    private static final String TAG = "AppRepository";

    // Shared so every screen serialises counter updates on one thread
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final Context context;
    private final PackageManager packageManager;
    private final AppLaunchCounterDao launchCounterDao;

    public AppRepository(Context context) {
        this.context = context.getApplicationContext();
        this.packageManager = this.context.getPackageManager();
        this.launchCounterDao = AppDatabase.getInstance(context).appLaunchCounterDao();
    }

    /**
     * Get all launchable apps, sorted alphabetically.
     *
     * @param excludedPackages packages to leave out (e.g. hidden apps)
     * @param loadIcons        icons are only loaded when they will be shown
     */
    @WorkerThread
    public List<AppInfo> getLaunchableApps(Collection<String> excludedPackages, boolean loadIcons) {
        List<AppInfo> appList = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);

        List<String> added = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;

            if (packageName.equals(context.getPackageName())
                    || excludedPackages.contains(packageName)
                    || added.contains(packageName)) {
                continue;
            }

            ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
            boolean isSystemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

            AppInfo app = new AppInfo(
                    resolveInfo.loadLabel(packageManager).toString(),
                    packageName,
                    loadIcons ? resolveInfo.loadIcon(packageManager) : null,
                    isSystemApp);
            app.setInstallTime(getInstallTime(packageName));
            appList.add(app);
            added.add(packageName);
        }

        Collections.sort(appList);
        return appList;
    }

    /**
     * Load a single app by package name, or null if it is not installed / launchable
     */
    @WorkerThread
    public AppInfo getApp(String packageName, boolean loadIcon) {
        if (packageManager.getLaunchIntentForPackage(packageName) == null) {
            return null;
        }
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            return new AppInfo(
                    packageManager.getApplicationLabel(appInfo).toString(),
                    packageName,
                    loadIcon ? packageManager.getApplicationIcon(appInfo) : null,
                    isSystemApp);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public boolean isLaunchable(String packageName) {
        return packageManager.getLaunchIntentForPackage(packageName) != null;
    }

    /**
     * Launch an app and count the launch.
     *
     * @return true if the app was started
     */
    public boolean launchApp(String packageName) {
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            return false;
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            context.startActivity(launchIntent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Unable to launch " + packageName, e);
            return false;
        }
        incrementLaunchCounter(packageName);
        return true;
    }

    private void incrementLaunchCounter(String packageName) {
        EXECUTOR.execute(() -> {
            String today = getTodayDate();
            AppLaunchCounter counter = launchCounterDao.getCounter(packageName, today);

            if (counter == null) {
                String appName = packageName;
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                    appName = packageManager.getApplicationLabel(appInfo).toString();
                } catch (PackageManager.NameNotFoundException ignored) {
                    // Fall back to the package name
                }
                // REPLACE also overwrites yesterday's row for this package (packageName is the key)
                counter = new AppLaunchCounter(packageName, appName, today);
                counter.incrementLaunchCount();
                launchCounterDao.insert(counter);
            } else {
                counter.incrementLaunchCount();
                launchCounterDao.update(counter);
            }
        });
    }

    /**
     * Today's launch count for one app
     */
    @WorkerThread
    public int getTodayLaunchCountSync(String packageName) {
        AppLaunchCounter counter = launchCounterDao.getCounter(packageName, getTodayDate());
        return counter != null ? counter.getLaunchCount() : 0;
    }

    /**
     * Get all launch counters for today
     */
    public LiveData<List<AppLaunchCounter>> getAllLaunchCounters() {
        return launchCounterDao.getAllCounters(getTodayDate());
    }

    /**
     * Remove counters from previous days (called by the daily worker)
     */
    @WorkerThread
    public void resetDailyCountersSync() {
        launchCounterDao.deleteOldCounters(getTodayDate());
    }

    private long getInstallTime(String packageName) {
        try {
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            return info.firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    /**
     * Today's date in yyyy-MM-dd (fixed locale so stored keys never change with language)
     */
    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
