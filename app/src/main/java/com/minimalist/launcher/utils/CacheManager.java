package com.minimalist.launcher.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

/**
 * Icon cache manager using LRU cache for memory-efficient icon storage
 */
public class CacheManager {

    private static CacheManager instance;
    private final LruCache<String, Bitmap> iconCache;

    private CacheManager() {
        // Get max available memory
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of available memory for cache
        final int cacheSize = maxMemory / 8;

        iconCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // Size in kilobytes
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    /**
     * Put icon in cache
     */
    public void putIcon(String packageName, Drawable icon) {
        if (icon instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) icon).getBitmap();
            iconCache.put(packageName, bitmap);
        }
    }

    /**
     * Get icon from cache
     */
    public Bitmap getIcon(String packageName) {
        return iconCache.get(packageName);
    }

    /**
     * Check if icon is cached
     */
    public boolean hasIcon(String packageName) {
        return iconCache.get(packageName) != null;
    }

    /**
     * Clear all cached icons
     */
    public void clearCache() {
        iconCache.evictAll();
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return iconCache.size();
    }

    /**
     * Remove specific icon from cache
     */
    public void removeIcon(String packageName) {
        iconCache.remove(packageName);
    }
}
