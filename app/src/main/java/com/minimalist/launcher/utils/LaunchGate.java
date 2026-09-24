package com.minimalist.launcher.utils;

import android.app.Activity;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.repository.AppRepository;

/**
 * Single entry point for opening an app from anywhere in the launcher.
 * Counts the launch and, when enabled, asks "Do you really need this?"
 * once the daily threshold has been reached.
 */
public final class LaunchGate {

    public interface OnLaunched {
        void onLaunched();
    }

    private LaunchGate() {
    }

    public static void launch(Activity activity, AppRepository repository, String packageName,
            OnLaunched onLaunched) {
        if (!Prefs.warningsEnabled(activity)) {
            launchNow(activity, repository, packageName, onLaunched);
            return;
        }

        int threshold = Prefs.warningThreshold(activity);
        repository.getTodayLaunchCount(packageName, count -> {
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            if (count < threshold) {
                launchNow(activity, repository, packageName, onLaunched);
                return;
            }
            new MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.launch_warning_title)
                    .setMessage(activity.getString(R.string.launch_warning_message, count))
                    .setPositiveButton(R.string.launch_anyway,
                            (dialog, which) -> launchNow(activity, repository, packageName, onLaunched))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
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
