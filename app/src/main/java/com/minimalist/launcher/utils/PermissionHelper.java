package com.minimalist.launcher.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.provider.Settings;

/**
 * Helper class for usage access and default-launcher (home role) handling
 */
public class PermissionHelper {

    /**
     * Check if usage access has been granted in system settings
     */
    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        int mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Open the usage access screen, pointing at this app where the system supports it
     */
    public static void requestUsageStatsPermission(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Some devices don't accept the package URI
            activity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    /**
     * Open this app's system settings page, where the battery option lives
     * ("Unrestricted" / "Don't optimise" keeps the launcher from being closed).
     */
    public static void openBatterySettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null));
        try {
            activity.startActivity(intent);
            android.widget.Toast.makeText(activity, com.minimalist.launcher.R.string.battery_settings_hint,
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    /**
     * Check if this app currently holds the home (default launcher) role
     */
    public static boolean isDefaultLauncher(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        return roleManager != null
                && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)
                && roleManager.isRoleHeld(RoleManager.ROLE_HOME);
    }

    /**
     * Intent for the one-tap system "set default home app" dialog, or the home settings
     * screen as a fallback. Start it with an activity result launcher.
     */
    public static Intent createDefaultLauncherIntent(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
        }
        return new Intent(Settings.ACTION_HOME_SETTINGS);
    }
}
