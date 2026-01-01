package com.minimalist.launcher.data.repository;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.lifecycle.LiveData;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.AppLaunchCounterDao;
import com.minimalist.launcher.data.database.entities.AppLaunchCounter;
import com.minimalist.launcher.data.model.AppInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for managing installed applications
 * Handles app list retrieval, filtering, and launch operations
 */
public class AppRepository {

    private final Context context;
    private final PackageManager packageManager;
    private final AppLaunchCounterDao launchCounterDao;
    private final Executor executor;

    public AppRepository(Context context) {
        this.context = context.getApplicationContext();
        this.packageManager = context.getPackageManager();
        AppDatabase database = AppDatabase.getInstance(context);
        this.launchCounterDao = database.appLaunchCounterDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Get all installed apps (excluding system apps by default)
     */
    public List<AppInfo> getAllApps(boolean includeSystemApps) {
        List<AppInfo> appList = new ArrayList<>();

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(intent, 0);

        for (ResolveInfo resolveInfo : resolveInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;

            // Skip our own launcher
            if (packageName.equals(context.getPackageName())) {
                continue;
            }

            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                // Filter system apps if requested
                if (!includeSystemApps && isSystemApp) {
                    continue;
                }

                String appName = resolveInfo.loadLabel(packageManager).toString();

                AppInfo app = new AppInfo(
                        appName,
                        packageName,
                        resolveInfo.loadIcon(packageManager),
                        isSystemApp);

                appList.add(app);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Sort alphabetically
        Collections.sort(appList);

        return appList;
    }

    /**
     * Launch an app by package name
     */
    public void launchApp(String packageName) {
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);

            // Increment launch counter in background
            incrementLaunchCounter(packageName);
        }
    }

    /**
     * Increment launch counter for an app
     */
    private void incrementLaunchCounter(String packageName) {
        executor.execute(() -> {
            String today = getTodayDate();
            AppLaunchCounter counter = launchCounterDao.getCounter(packageName, today);

            if (counter == null) {
                // Create new counter
                try {
                    ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                    String appName = packageManager.getApplicationLabel(appInfo).toString();
                    counter = new AppLaunchCounter(packageName, appName, today);
                    counter.incrementLaunchCount();
                    launchCounterDao.insert(counter);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                // Update existing counter
                counter.incrementLaunchCount();
                launchCounterDao.update(counter);
            }
        });
    }

    /**
     * Get launch counter for a specific app
     */
    public LiveData<AppLaunchCounter> getLaunchCounter(String packageName) {
        return launchCounterDao.getCounterLive(packageName, getTodayDate());
    }

    /**
     * Get all launch counters for today
     */
    public LiveData<List<AppLaunchCounter>> getAllLaunchCounters() {
        return launchCounterDao.getAllCounters(getTodayDate());
    }

    /**
     * Reset daily counters (called at midnight)
     */
    public void resetDailyCounters() {
        executor.execute(() -> {
            launchCounterDao.deleteOldCounters(getTodayDate());
        });
    }

    /**
     * Get today's date in yyyy-MM-dd format
     */
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
