package com.minimalist.launcher.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.minimalist.launcher.R;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.repository.UsageRepository;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.data.wellbeing.WellbeingStore;
import com.minimalist.launcher.ui.pause.PauseActivity;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point for opening an app from anywhere in the launcher.
 * The app opens straight away unless one of these applies, in which case the
 * mindful pause screen is shown first (with the reason):
 * 1. its daily time limit is used up (needs usage access),
 * 2. it has been opened more than the launch-reminder threshold today,
 * 3. the user asked for a pause before this app.
 */
public final class LaunchGate {

    public interface OnLaunched {
        void onLaunched();
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private LaunchGate() {
    }

    public static void launch(Activity activity, AppRepository repository, String packageName,
            OnLaunched onLaunched) {
        final int limitMinutes = new AppLimitsManager(activity).getLimitMinutes(packageName);
        final boolean warningsEnabled = Prefs.warningsEnabled(activity);
        final int threshold = Prefs.warningThreshold(activity);
        final boolean pauseApp = new WellbeingStore(activity).isPauseApp(packageName);

        if (limitMinutes <= 0 && !warningsEnabled && !pauseApp) {
            launchNow(activity, repository, packageName, onLaunched);
            return;
        }

        final UsageRepository usageRepository = new UsageRepository(activity);
        EXECUTOR.execute(() -> {
            int count = repository.getTodayLaunchCountSync(packageName);
            long usedMillis = -1;
            if (limitMinutes > 0 || pauseApp) {
                Map<String, Long> usage = usageRepository.getTodayUsagePerApp();
                if (usage != null) {
                    Long used = usage.get(packageName);
                    usedMillis = used != null ? used : 0;
                }
            }
            AppInfo app = repository.getApp(packageName, false);
            final String appName = app != null ? app.getAppName() : packageName;
            final int launchCount = count;
            final long used = usedMillis;
            MAIN.post(() -> decide(activity, repository, packageName, appName, onLaunched,
                    limitMinutes, used, warningsEnabled, threshold, launchCount, pauseApp));
        });
    }

    private static void decide(Activity activity, AppRepository repository, String packageName, String appName,
            OnLaunched onLaunched, int limitMinutes, long usedMillis,
            boolean warningsEnabled, int threshold, int launchCount, boolean pauseApp) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        String message = null;
        if (limitMinutes > 0 && usedMillis >= limitMinutes * 60_000L) {
            message = activity.getString(R.string.limit_reached_message,
                    ScreenTimeCalculator.format(usedMillis),
                    ScreenTimeCalculator.format(limitMinutes * 60_000L));
        } else if (warningsEnabled && launchCount >= threshold) {
            message = activity.getString(R.string.launch_warning_message, launchCount);
        } else if (pauseApp) {
            message = describeToday(activity, launchCount, usedMillis, limitMinutes);
        }

        if (message == null) {
            launchNow(activity, repository, packageName, onLaunched);
            return;
        }
        PauseActivity.start(activity, packageName, appName, message);
        if (onLaunched != null) {
            onLaunched.onLaunched();
        }
    }

    /** e.g. "Opened 6 times today · 25m used · 5m left of your limit" */
    private static String describeToday(Activity activity, int launchCount, long usedMillis, int limitMinutes) {
        StringBuilder text = new StringBuilder(activity.getResources()
                .getQuantityString(R.plurals.pause_opened_times, launchCount, launchCount));
        if (usedMillis >= 60_000) {
            text.append(" · ").append(activity.getString(R.string.pause_used, ScreenTimeCalculator.format(usedMillis)));
        }
        if (limitMinutes > 0 && usedMillis >= 0) {
            long left = Math.max(0, limitMinutes * 60_000L - usedMillis);
            text.append(" · ").append(activity.getString(R.string.pause_left, ScreenTimeCalculator.format(left)));
        }
        return text.toString();
    }

    private static void launchNow(Activity activity, AppRepository repository, String packageName,
            OnLaunched onLaunched) {
        if (repository.launchApp(packageName)) {
            if (onLaunched != null) {
                onLaunched.onLaunched();
            }
        } else {
            Toast.makeText(activity, R.string.unable_to_open_app, Toast.LENGTH_SHORT).show();
        }
    }
}
