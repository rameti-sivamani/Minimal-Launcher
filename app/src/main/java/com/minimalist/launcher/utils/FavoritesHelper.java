package com.minimalist.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to manage favorite apps using SharedPreferences
 */
public class FavoritesHelper {

    private static final String PREFS_NAME = "favorites_prefs";
    private static final String KEY_FAVORITES = "favorite_packages";
    private static final int MAX_FAVORITES = 4;

    private final SharedPreferences prefs;

    public FavoritesHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get list of favorite package names
     */
    public List<String> getFavorites() {
        Set<String> favSet = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
        return new ArrayList<>(favSet);
    }

    /**
     * Add a package to favorites
     */
    public boolean addFavorite(String packageName) {
        Set<String> favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));

        if (favorites.size() >= MAX_FAVORITES) {
            return false; // Max favorites reached
        }

        favorites.add(packageName);
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
        return true;
    }

    /**
     * Remove a package from favorites
     */
    public void removeFavorite(String packageName) {
        Set<String> favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        favorites.remove(packageName);
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    /**
     * Check if a package is in favorites
     */
    public boolean isFavorite(String packageName) {
        Set<String> favorites = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
        return favorites.contains(packageName);
    }

    /**
     * Set default favorites if none exist
     */
    public void setDefaultFavorites(List<String> defaultPackages) {
        if (getFavorites().isEmpty()) {
            Set<String> favorites = new HashSet<>();
            int count = 0;
            for (String pkg : defaultPackages) {
                if (count >= MAX_FAVORITES)
                    break;
                favorites.add(pkg);
                count++;
            }
            prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
        }
    }

    /**
     * Get max number of favorites allowed
     */
    public int getMaxFavorites() {
        return MAX_FAVORITES;
    }

    /**
     * Clear all favorites
     */
    public void clearAll() {
        prefs.edit().remove(KEY_FAVORITES).apply();
    }
}
