package com.minimalist.launcher.ui.applist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.minimalist.launcher.R;
import com.minimalist.launcher.data.database.entities.AppLaunchCounter;
import com.minimalist.launcher.data.model.AppInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity displaying all installed apps in a searchable list
 * Implements intentional app access with friction features
 */
public class AppListActivity extends AppCompatActivity {

    private AppListViewModel viewModel;
    private AppAdapter adapter;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> filteredApps = new ArrayList<>();

    private boolean isThemeReceiverRegistered = false;

    // Broadcast receiver for theme changes
    private BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                android.util.Log.d("AppListActivity", "Theme change broadcast received!");
                applyTheme();
            } catch (Exception e) {
                android.util.Log.e("AppListActivity", "Error applying theme", e);
            }
        }
    };
    private TextView emptyStateText;
    private SharedPreferences preferences;
    private com.minimalist.launcher.utils.FavoritesHelper favoritesHelper;

    private Map<String, Integer> launchCountMap = new HashMap<>();

    private BroadcastReceiver packageChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        // Initialize ViewModel (uses cached data when available)
        viewModel = new ViewModelProvider(this).get(AppListViewModel.class);

        // Initialize preferences and favorites helper
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        favoritesHelper = new com.minimalist.launcher.utils.FavoritesHelper(this);

        setupViews();
        observeViewModel();
        enableImmersiveMode();
        setupSwipeGesture();
        setupPackageChangeReceiver();

        // Initial load
        viewModel.loadApps();

        // Apply theme
        applyTheme();
    }

    /**
     * Apply current theme to activity
     */
    private void applyTheme() {
        try {
            com.minimalist.launcher.utils.ThemeManager themeManager = new com.minimalist.launcher.utils.ThemeManager(
                    this);

            int bgColor = themeManager.getBackgroundColor();
            int textColor = themeManager.getTextColor();
            int secondaryTextColor = themeManager.getSecondaryTextColor();

            // Status bar same color as background
            getWindow().setStatusBarColor(bgColor);
            View decorView = getWindow().getDecorView();
            int systemUiVisibility = decorView.getSystemUiVisibility();
            if (themeManager.isDarkTheme()) {
                systemUiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(systemUiVisibility);

            // Apply backgrounds
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.setBackgroundColor(bgColor);
            }

            getWindow().getDecorView().setBackgroundColor(bgColor);

            View appListRoot = findViewById(R.id.app_list_root);
            if (appListRoot != null) {
                appListRoot.setBackgroundColor(bgColor);
            }

            // Apply text colors
            TextView emptyStateText = findViewById(R.id.empty_state_text);
            if (emptyStateText != null) {
                emptyStateText.setTextColor(secondaryTextColor);
            }

            // Theme search bar
            androidx.appcompat.widget.SearchView searchView = findViewById(R.id.search_view);
            if (searchView != null) {
                // Set search bar background
                searchView.setBackgroundColor(bgColor);

                // Get search text view and hint
                int searchTextId = searchView.getContext().getResources()
                        .getIdentifier("android:id/search_src_text", null, null);
                TextView searchText = searchView.findViewById(searchTextId);
                if (searchText != null) {
                    searchText.setTextColor(textColor);
                    searchText.setHintTextColor(secondaryTextColor);
                }

                // Tint search icons
                try {
                    int searchIconId = searchView.getContext().getResources()
                            .getIdentifier("android:id/search_mag_icon", null, null);
                    android.widget.ImageView searchIcon = searchView.findViewById(searchIconId);
                    if (searchIcon != null) {
                        searchIcon.setColorFilter(textColor);
                    }

                    int closeIconId = searchView.getContext().getResources()
                            .getIdentifier("android:id/search_close_btn", null, null);
                    android.widget.ImageView closeIcon = searchView.findViewById(closeIconId);
                    if (closeIcon != null) {
                        closeIcon.setColorFilter(textColor);
                    }
                } catch (Exception e) {
                    android.util.Log.e("AppListActivity", "Error theming search icons", e);
                }
            }

        } catch (Exception e) {
            android.util.Log.e("AppListActivity", "Error in applyTheme()", e);
        }
    }

    /**
     * Listen for app installs/uninstalls to refresh list
     */
    private void setupPackageChangeReceiver() {
        packageChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // App installed or uninstalled - invalidate cache
                Log.d("AppListActivity", "Package changed - refreshing list");
                viewModel.forceReloadApps();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        // Android 13+ requires RECEIVER_NOT_EXPORTED
        androidx.core.content.ContextCompat.registerReceiver(
                this,
                packageChangeReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    /**
     * Initialize views and set up RecyclerView and SearchView
     */
    private void setupViews() {
        recyclerView = findViewById(R.id.apps_recycler_view);
        searchView = findViewById(R.id.search_view);
        emptyStateText = findViewById(R.id.empty_state_text);

        // Set up RecyclerView
        adapter = new AppAdapter(this::onAppClick, this::onAppLongClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Check settings for icon visibility
        boolean showIcons = preferences.getBoolean("show_icons", false);
        adapter.setShowIcons(showIcons);

        // SearchView configuration
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
    }

    /**
     * Observe ViewModel data and update UI
     */
    private void observeViewModel() {
        viewModel.getFilteredApps().observe(this, apps -> {
            if (apps != null) {
                adapter.setApps(apps);
                emptyStateText.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getLaunchCounters().observe(this, counters -> {
            if (counters != null) {
                launchCountMap.clear();
                for (AppLaunchCounter counter : counters) {
                    launchCountMap.put(counter.getPackageName(), counter.getLaunchCount());
                }
            }
        });
    }

    /**
     * Enable immersive fullscreen mode
     */
    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Set up swipe gesture to close
        setupSwipeGesture();
    }

    // Gesture detector for swipe detection
    private android.view.GestureDetector gestureDetector;

    /**
     * Set up swipe left gesture to close app list
     */
    private void setupSwipeGesture() {
        gestureDetector = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {

                    private static final int SWIPE_THRESHOLD = 100;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2,
                            float velocityX, float velocityY) {
                        if (e1 == null || e2 == null)
                            return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        // Swipe left to close
                        if (Math.abs(diffX) > Math.abs(diffY) &&
                                diffX < -SWIPE_THRESHOLD &&
                                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            finish();
                            return true;
                        }
                        return false;
                    }
                });
    }

    /**
     * Override dispatchTouchEvent to capture gestures before child views
     */
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        // Return to home screen
        Intent intent = new Intent(this, com.minimalist.launcher.ui.launcher.LauncherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * Set up RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new AppAdapter(this::onAppClick, this::onAppLongClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Check settings for icon visibility
        boolean showIcons = preferences.getBoolean("show_icons", false);
        adapter.setShowIcons(showIcons);
    }

    /**
     * Handle app long-click - show options menu
     */
    private void onAppLongClick(AppInfo app) {
        String[] options;
        if (favoritesHelper.isFavorite(app.getPackageName())) {
            options = new String[] { "App Info", "Uninstall", "Hide App", "Remove from Favorites" };
        } else {
            options = new String[] { "Add to Favorites", "App Info", "Uninstall", "Hide App" };
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle(app.getAppName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // First option varies
                            if (favoritesHelper.isFavorite(app.getPackageName())) {
                                // App Info
                                openAppInfo(app);
                            } else {
                                // Add to Favorites
                                addToFavorites(app);
                            }
                            break;
                        case 1: // Second option varies
                            if (favoritesHelper.isFavorite(app.getPackageName())) {
                                // Uninstall
                                uninstallApp(app);
                            } else {
                                // App Info
                                openAppInfo(app);
                            }
                            break;
                        case 2: // Third option varies
                            if (favoritesHelper.isFavorite(app.getPackageName())) {
                                // Hide App
                                hideApp(app);
                            } else {
                                // Uninstall
                                uninstallApp(app);
                            }
                            break;
                        case 3: // Fourth option (only when already favorite)
                            if (favoritesHelper.isFavorite(app.getPackageName())) {
                                // Remove from Favorites
                                favoritesHelper.removeFavorite(app.getPackageName());
                                android.widget.Toast
                                        .makeText(this, "Removed from favorites", android.widget.Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                // Hide App
                                hideApp(app);
                            }
                            break;
                    }
                })
                .show();
    }

    /**
     * Add app to favorites
     */
    private void addToFavorites(AppInfo app) {
        if (favoritesHelper.addFavorite(app.getPackageName())) {
            android.widget.Toast
                    .makeText(this, app.getAppName() + " added to favorites", android.widget.Toast.LENGTH_SHORT).show();
        } else {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Favorites Full")
                    .setMessage("You can only have " + favoritesHelper.getMaxFavorites()
                            + " favorite apps. Remove one first.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    /**
     * Open app info in system settings
     */
    private void openAppInfo(AppInfo app) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.fromParts("package", app.getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("AppListActivity", "Error opening app info", e);
        }
    }

    /**
     * Uninstall app
     */
    private void uninstallApp(AppInfo app) {
        if (app.isSystemApp()) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("System App")
                    .setMessage(app.getAppName() + " is a system app and cannot be uninstalled without root access.")
                    .setPositiveButton("App Info", (dialog, which) -> openAppInfo(app))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Uninstall App")
                    .setMessage("Uninstall " + app.getAppName() + "?")
                    .setPositiveButton("Uninstall", (dialog, which) -> {
                        try {
                            Log.d("AppListActivity", "Attempting to uninstall: " + app.getPackageName());

                            Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                            intent.setData(android.net.Uri.parse("package:" + app.getPackageName()));
                            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);

                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                                Log.d("AppListActivity", "Uninstall intent started for: " + app.getPackageName());
                            } else {
                                Log.e("AppListActivity", "No activity found to handle uninstall intent");
                                android.widget.Toast.makeText(this,
                                        "Unable to uninstall this app",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("AppListActivity", "Error uninstalling app: " + app.getPackageName(), e);
                            android.widget.Toast.makeText(this,
                                    "Error: " + e.getMessage(),
                                    android.widget.Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    /**
     * Hide app from launcher
     */
    private void hideApp(AppInfo appInfo) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Hide App")
                .setMessage("Hide " + appInfo.getAppName() + " from launcher?\n\nYou can unhide it from Settings.")
                .setPositiveButton("Hide", (dialog, which) -> {
                    com.minimalist.launcher.utils.HiddenAppsManager hiddenAppsManager = new com.minimalist.launcher.utils.HiddenAppsManager(
                            this);
                    hiddenAppsManager.hideApp(appInfo.getPackageName());

                    // Immediately remove from UI - no waiting for reload!
                    viewModel.removeAppFromList(appInfo.getPackageName());

                    android.widget.Toast.makeText(this,
                            appInfo.getAppName() + " hidden",
                            android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Set up SearchView for filtering apps
     */
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
    }

    /**
     * Shows a settings menu (placeholder for future settings)
     */
    private void showSettingsMenu() {
        // Placeholder for future settings menu
    }

    /**
     * Handle app click - show warning dialog if needed, then launch
     */
    private void onAppClick(AppInfo app) {
        boolean warningsEnabled = preferences.getBoolean("warnings_enabled", true);
        Integer launchCount = launchCountMap.get(app.getPackageName());
        int threshold = preferences.getInt("warning_threshold", 10);

        // Show warning dialog if launch count exceeds threshold
        if (warningsEnabled && launchCount != null && launchCount >= threshold) {
            showLaunchWarningDialog(app);
        } else {
            launchApp(app);
        }
    }

    /**
     * Show "Do you really need this?" warning dialog
     */
    private void showLaunchWarningDialog(AppInfo app) {
        Integer count = launchCountMap.get(app.getPackageName());
        String message = getString(R.string.launch_warning_message, count != null ? count : 0);

        new AlertDialog.Builder(this)
                .setTitle(R.string.launch_warning_title)
                .setMessage(message)
                .setPositiveButton(R.string.launch_anyway, (dialog, which) -> {
                    launchApp(app);
                    finish(); // Close app list after launch
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Launch the app
     */
    private void launchApp(AppInfo app) {
        viewModel.launchApp(app.getPackageName());
        finish(); // Return to home screen after launching app
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register theme change receiver
        if (!isThemeReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter(com.minimalist.launcher.utils.ThemeManager.ACTION_THEME_CHANGED);
                // Android 13+ requires RECEIVER_NOT_EXPORTED
                androidx.core.content.ContextCompat.registerReceiver(
                        this,
                        themeChangeReceiver,
                        filter,
                        androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
                isThemeReceiverRegistered = true;
            } catch (Exception e) {
                android.util.Log.e("AppListActivity", "Error registering theme receiver", e);
            }
        }

        enableImmersiveMode();

        // Always call loadApps - it handles caching internally
        viewModel.loadApps();

        // Check if we should force refresh (came back from settings with unhidden apps)
        if (getIntent().getBooleanExtra("refresh_apps", false)) {
            viewModel.forceReloadApps();
            getIntent().removeExtra("refresh_apps");
        }

        // Apply theme
        applyTheme();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister theme change receiver
        if (isThemeReceiverRegistered) {
            try {
                unregisterReceiver(themeChangeReceiver);
                isThemeReceiverRegistered = false;
            } catch (Exception e) {
                android.util.Log.e("AppListActivity", "Error unregistering theme receiver", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister theme change receiver if still registered
        if (isThemeReceiverRegistered) {
            try {
                unregisterReceiver(themeChangeReceiver);
                isThemeReceiverRegistered = false;
            } catch (Exception e) {
                // Already unregistered
            }
        }

        // Unregister package change receiver
        if (packageChangeReceiver != null) {
            try {
                unregisterReceiver(packageChangeReceiver);
            } catch (IllegalArgumentException e) {
                // Already unregistered
            }
        }
    }
}
