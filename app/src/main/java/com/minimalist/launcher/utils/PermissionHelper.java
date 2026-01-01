package com.minimalist.launcher.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

/**
 * Helper class for managing permissions
 * Particularly for usage stats permission which requires special handling
 */
public class PermissionHelper {

    /**
     * Check if usage stats permission is granted
     */
    public static boolean hasUsageStatsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) {
                return false;
            }

            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.getPackageName());

            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return false;
    }

    /**
     * Open usage stats settings screen
     */
    public static void requestUsageStatsPermission(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        activity.startActivity(intent);
    }

    /**
     * Check if this app is set as default launcher
     */
    public static boolean isDefaultLauncher(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        String defaultLauncher = intent.resolveActivity(context.getPackageManager())
                .getPackageName();

        return context.getPackageName().equals(defaultLauncher);
    }

    /**
     * Open default apps settings to allow user to set as default launcher
     */
    public static void requestDefaultLauncher(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
        activity.startActivity(intent);
    }
}
