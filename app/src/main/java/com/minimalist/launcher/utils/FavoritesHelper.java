package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages favorite apps (shown on the home screen) in their user-chosen order
 */
public class FavoritesHelper {

    private static final String PREFS_NAME = "favorites_prefs";
    // Ordered, comma-separated list
    private static final String KEY_FAVORITES_ORDERED = "favorite_packages_ordered";
    // Legacy unordered StringSet from v1 builds, migrated on first read
    private static final String KEY_FAVORITES_LEGACY = "favorite_packages";
    private static final String KEY_DEFAULTS_APPLIED = "defaults_applied";
    private static final int MAX_FAVORITES = 4;

    private final SharedPreferences prefs;

    public FavoritesHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get favorite package names in display order
     */
    public synchronized List<String> getFavorites() {
        if (!prefs.contains(KEY_FAVORITES_ORDERED) && prefs.contains(KEY_FAVORITES_LEGACY)) {
            Set<String> legacy = prefs.getStringSet(KEY_FAVORITES_LEGACY, new HashSet<>());
            List<String> migrated = new ArrayList<>(legacy);
            prefs.edit()
                    .putString(KEY_FAVORITES_ORDERED, PackageListCodec.encode(migrated))
                    .remove(KEY_FAVORITES_LEGACY)
                    .putBoolean(KEY_DEFAULTS_APPLIED, true)
                    .apply();
            return migrated;
        }
        return PackageListCodec.decode(prefs.getString(KEY_FAVORITES_ORDERED, ""));
    }

    private void save(List<String> favorites) {
        prefs.edit().putString(KEY_FAVORITES_ORDERED, PackageListCodec.encode(favorites)).apply();
    }

    /**
     * Add a package to the end of the favorites list
     *
     * @return false if the list is already full
     */
    public synchronized boolean addFavorite(String packageName) {
        List<String> favorites = getFavorites();
        if (favorites.contains(packageName)) {
            return true;
        }
        if (favorites.size() >= MAX_FAVORITES) {
            return false;
        }
        favorites.add(packageName);
        save(favorites);
        return true;
    }

    public synchronized void removeFavorite(String packageName) {
        List<String> favorites = getFavorites();
        if (favorites.remove(packageName)) {
            save(favorites);
        }
    }

    /**
     * Move a favorite one position up (-1) or down (+1)
     */
    public synchronized void move(String packageName, int direction) {
        List<String> favorites = getFavorites();
        int from = favorites.indexOf(packageName);
        int to = from + direction;
        if (from < 0 || to < 0 || to >= favorites.size()) {
            return;
        }
        favorites.remove(from);
        favorites.add(to, packageName);
        save(favorites);
    }

    public boolean isFavorite(String packageName) {
        return getFavorites().contains(packageName);
    }

    /**
     * Fill favorites the first time the launcher runs.
     * Only installed candidates are used, and this never runs again once applied
     * (so a user who clears all favorites keeps an empty home screen).
     */
    public synchronized void applyDefaultsOnce(List<String> installedCandidates) {
        if (prefs.getBoolean(KEY_DEFAULTS_APPLIED, false)) {
            return;
        }
        if (getFavorites().isEmpty()) {
            List<String> defaults = new ArrayList<>();
            for (String pkg : installedCandidates) {
                if (defaults.size() >= MAX_FAVORITES) {
                    break;
                }
                if (!defaults.contains(pkg)) {
                    defaults.add(pkg);
                }
            }
            save(defaults);
        }
        prefs.edit().putBoolean(KEY_DEFAULTS_APPLIED, true).apply();
    }

    public int getMaxFavorites() {
        return MAX_FAVORITES;
    }

    public synchronized void clearAll() {
        prefs.edit()
                .putString(KEY_FAVORITES_ORDERED, "")
                .remove(KEY_FAVORITES_LEGACY)
                .apply();
    }
}
