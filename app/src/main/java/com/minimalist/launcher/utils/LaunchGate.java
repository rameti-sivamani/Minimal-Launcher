package com.minimalist.launcher.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.repository.UsageRepository;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point for opening an app from anywhere in the launcher.
 * Before launching it checks, in order:
 * 1. the app's daily time limit (needs usage access), then
 * 2. the "Do you really need this?" launch-count reminder.
 * At most one dialog is shown per launch.
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

        if (limitMinutes <= 0 && !warningsEnabled) {
            launchNow(activity, repository, packageName, onLaunched);
            return;
        }

        final UsageRepository usageRepository = new UsageRepository(activity);
        EXECUTOR.execute(() -> {
            int count = warningsEnabled ? repository.getTodayLaunchCountSync(packageName) : 0;
            long usedMillis = -1;
            if (limitMinutes > 0) {
                Map<String, Long> usage = usageRepository.getTodayUsagePerApp();
                if (usage != null) {
                    Long used = usage.get(packageName);
                    usedMillis = used != null ? used : 0;
                }
            }
            final int launchCount = count;
            final long used = usedMillis;
            MAIN.post(() -> decide(activity, repository, packageName, onLaunched,
                    limitMinutes, used, warningsEnabled, threshold, launchCount));
        });
    }

    private static void decide(Activity activity, AppRepository repository, String packageName,
            OnLaunched onLaunched, int limitMinutes, long usedMillis,
            boolean warningsEnabled, int threshold, int launchCount) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (limitMinutes > 0 && usedMillis >= limitMinutes * 60_000L) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.limit_reached_title)
                    .setMessage(activity.getString(R.string.limit_reached_message,
                            ScreenTimeCalculator.format(usedMillis),
                            ScreenTimeCalculator.format(limitMinutes * 60_000L)))
                    .setPositiveButton(R.string.open_anyway_once,
                            (dialog, which) -> launchNow(activity, repository, packageName, onLaunched))
                    .setNegativeButton(R.string.not_now, null)
                    .show();
            return;
        }

        if (warningsEnabled && launchCount >= threshold) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.launch_warning_title)
                    .setMessage(activity.getString(R.string.launch_warning_message, launchCount))
                    .setPositiveButton(R.string.launch_anyway,
                            (dialog, which) -> launchNow(activity, repository, packageName, onLaunched))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        launchNow(activity, repository, packageName, onLaunched);
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
