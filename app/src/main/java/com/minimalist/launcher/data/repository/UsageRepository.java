package com.minimalist.launcher.data.repository;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import androidx.annotation.WorkerThread;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.AppUsageDao;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.data.wellbeing.WellbeingStore;
import com.minimalist.launcher.utils.PermissionHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repository for app usage statistics.
 * Screen time comes from Android's UsageStatsManager and never leaves the device.
 */
public class UsageRepository {

    private final Context context;
    private final AppUsageDao appUsageDao;
    private final UsageStatsManager usageStatsManager;

    public UsageRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        this.appUsageDao = database.appUsageDao();
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    /**
     * Total foreground time of all apps (except this launcher) since local midnight.
     *
     * @return milliseconds, or -1 if usage access has not been granted
     */
    @WorkerThread
    public long getTodayScreenTimeMillis() {
        Map<String, Long> perApp = getTodayUsagePerApp();
        if (perApp == null) {
            return -1;
        }
        long total = 0;
        for (long value : perApp.values()) {
            total += value;
        }
        return total;
    }

    /**
     * Foreground time per package since local midnight, or null without usage access
     */
    @WorkerThread
    public Map<String, Long> getTodayUsagePerApp() {
        return getUsagePerApp(startOfDay(0), System.currentTimeMillis());
    }

    /**
     * Total screen time for each of the last {@code days} days, oldest first
     * (the last entry is today so far). Null without usage access.
     */
    @WorkerThread
    public long[] getDailyTotals(int days) {
        if (!PermissionHelper.hasUsageStatsPermission(context)) {
            return null;
        }
        long[] totals = new long[days];
        long now = System.currentTimeMillis();
        for (int i = 0; i < days; i++) {
            int daysAgo = days - 1 - i;
            long start = startOfDay(daysAgo);
            long end = daysAgo == 0 ? now : startOfDay(daysAgo - 1);
            Map<String, Long> perApp = getUsagePerApp(start, end);
            long sum = 0;
            if (perApp != null) {
                for (long value : perApp.values()) {
                    sum += value;
                }
            }
            totals[i] = sum;
        }
        return totals;
    }

    /**
     * Save the screen time of finished days (up to a week back, which is roughly how long
     * Android keeps usage events) so streaks survive. Days before this app was installed
     * are skipped so a streak only counts days spent with the launcher.
     */
    @WorkerThread
    public void backfillDailyTotals(WellbeingStore store) {
        if (!PermissionHelper.hasUsageStatsPermission(context)) {
            return;
        }
        long installedAt = 0;
        try {
            installedAt = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime;
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            // Own package is always present
        }
        for (int daysAgo = 1; daysAgo <= 7; daysAgo++) {
            long start = startOfDay(daysAgo);
            if (store.hasDailyTotal(daysAgo) || startOfDay(daysAgo - 1) <= installedAt) {
                continue;
            }
            Map<String, Long> perApp = getUsagePerApp(start, startOfDay(daysAgo - 1));
            if (perApp == null) {
                return;
            }
            long sum = 0;
            for (long value : perApp.values()) {
                sum += value;
            }
            store.saveDailyTotal(daysAgo, sum);
        }
        store.pruneHistory();
    }

    /**
     * Foreground time per package in [start, end), or null without usage access
     */
    @WorkerThread
    public Map<String, Long> getUsagePerApp(long start, long end) {
        if (usageStatsManager == null || !PermissionHelper.hasUsageStatsPermission(context)) {
            return null;
        }

        List<ScreenTimeCalculator.Event> events = new ArrayList<>();
        UsageEvents usageEvents = usageStatsManager.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();
        while (usageEvents != null && usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            int type = event.getEventType();
            if (type == UsageEvents.Event.ACTIVITY_RESUMED) {
                events.add(new ScreenTimeCalculator.Event(
                        event.getPackageName(), ScreenTimeCalculator.RESUMED, event.getTimeStamp()));
            } else if (type == UsageEvents.Event.ACTIVITY_PAUSED) {
                events.add(new ScreenTimeCalculator.Event(
                        event.getPackageName(), ScreenTimeCalculator.PAUSED, event.getTimeStamp()));
            }
        }

        return ScreenTimeCalculator.perApp(events, start, end,
                Collections.singleton(context.getPackageName()));
    }

    /**
     * Delete locally stored usage records older than 30 days
     */
    @WorkerThread
    public void cleanupOldDataSync() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        appUsageDao.deleteOldRecords(sdf.format(new Date(thirtyDaysAgo)));
    }

    /**
     * Local midnight {@code daysAgo} days before today (0 = today, -1 = tomorrow)
     */
    public static long startOfDay(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return calendar.getTimeInMillis();
    }
}
