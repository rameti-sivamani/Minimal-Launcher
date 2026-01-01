package com.minimalist.launcher.utils;

import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.model.AppInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for filtering apps based on focus mode
 */
public class AppFilterHelper {

    /**
     * Filter apps based on active focus mode
     * Returns only apps that are in the allowlist
     */
    public static List<AppInfo> filterByFocusMode(List<AppInfo> allApps, FocusMode focusMode) {
        if (focusMode == null || !focusMode.isActive()) {
            return allApps;
        }

        // Parse allowed apps from comma-separated string
        Set<String> allowedPackages = new HashSet<>();
        if (focusMode.getAllowedApps() != null && !focusMode.getAllowedApps().isEmpty()) {
            String[] packages = focusMode.getAllowedApps().split(",");
            allowedPackages.addAll(Arrays.asList(packages));
        }

        // Filter the app list
        List<AppInfo> filteredApps = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (allowedPackages.contains(app.getPackageName())) {
                filteredApps.add(app);
            }
        }

        return filteredApps;
    }

    /**
     * Get default "Deep Work" mode allowlist
     * Includes only essential communication and productivity apps
     */
    public static Set<String> getDeepWorkAllowlist() {
        Set<String> allowlist = new HashSet<>();

        // Phone and SMS
        allowlist.add("com.android.dialer");
        allowlist.add("com.google.android.dialer");
        allowlist.add("com.android.messaging");
        allowlist.add("com.google.android.apps.messaging");

        // Calendar
        allowlist.add("com.android.calendar");
        allowlist.add("com.google.android.calendar");

        // Contacts
        allowlist.add("com.android.contacts");
        allowlist.add("com.google.android.contacts");

        // Clock/Alarms
        allowlist.add("com.android.deskclock");
        allowlist.add("com.google.android.deskclock");

        // Settings
        allowlist.add("com.android.settings");

        return allowlist;
    }

    /**
     * Create comma-separated string from package set
     */
    public static String packageSetToString(Set<String> packages) {
        return String.join(",", packages);
    }

    /**
     * Create package set from comma-separated string
     */
    public static Set<String> stringToPackageSet(String packagesString) {
        Set<String> packages = new HashSet<>();
        if (packagesString != null && !packagesString.isEmpty()) {
            String[] packageArray = packagesString.split(",");
            packages.addAll(Arrays.asList(packageArray));
        }
        return packages;
    }
}
