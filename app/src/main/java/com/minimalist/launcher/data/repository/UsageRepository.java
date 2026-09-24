package com.minimalist.launcher.data.repository;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import androidx.annotation.WorkerThread;

import com.minimalist.launcher.data.database.AppDatabase;
import com.minimalist.launcher.data.database.dao.AppUsageDao;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
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
        if (usageStatsManager == null || !PermissionHelper.hasUsageStatsPermission(context)) {
            return null;
        }

        long start = startOfToday();
        long end = System.currentTimeMillis();

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

    private static long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
