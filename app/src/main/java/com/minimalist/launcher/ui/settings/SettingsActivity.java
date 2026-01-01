package com.minimalist.launcher.ui.settings;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.appbar.MaterialToolbar;

import com.minimalist.launcher.R;
import com.minimalist.launcher.utils.FavoritesHelper;
import com.minimalist.launcher.utils.HiddenAppsManager;
import com.minimalist.launcher.utils.ThemeManager;

/**
 * Settings activity for configuring launcher behavior
 * Manages permissions, appearance, and usage awareness settings
 */
public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private Button usageStatsButton;

    private SharedPreferences prefs;
    private ThemeManager themeManager;
    private HiddenAppsManager hiddenAppsManager;

    private boolean isThemeReceiverRegistered = false;

    // Broadcast receiver for theme changes
    private BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                android.util.Log.d("SettingsActivity", "Theme change broadcast received!");
                applyTheme();
                updateThemeSummary();
            } catch (Exception e) {
                android.util.Log.e("SettingsActivity", "Error applying theme in broadcast receiver", e);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        themeManager = new ThemeManager(this);
        hiddenAppsManager = new HiddenAppsManager(this);

        setupEdgeToEdge();
        setupToolbar();
        applyTheme();
        setupSettings();
    }

    /**
     * Apply current theme to activity (comprehensive - all UI elements)
     */
    private void applyTheme() {
        try {
            android.util.Log.d("SettingsActivity", "applyTheme() called");

            // Get theme colors
            int bgColor = themeManager.getBackgroundColor();
            int textColor = themeManager.getTextColor();
            int secondaryTextColor = themeManager.getSecondaryTextColor();

            android.util.Log.d("SettingsActivity", "Theme colors - bg: " + bgColor + ", text: " + textColor);

            // === STATUS BAR ===
            getWindow().setStatusBarColor(bgColor);

            // Set status bar icons color based on theme
            View decorView = getWindow().getDecorView();
            int systemUiVisibility = decorView.getSystemUiVisibility();
            if (themeManager.isDarkTheme()) {
                systemUiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(systemUiVisibility);

            // === BACKGROUNDS ===
            getWindow().getDecorView().setBackgroundColor(bgColor);

            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.setBackgroundColor(bgColor);
            }

            // Settings root CoordinatorLayout
            View settingsRoot = findViewById(R.id.settings_root);
            if (settingsRoot != null) {
                settingsRoot.setBackgroundColor(bgColor);
            }

            View scrollView = findViewById(R.id.settings_scroll_view);
            if (scrollView != null) {
                scrollView.setBackgroundColor(bgColor);
            }

            View contentLayout = findViewById(R.id.settings_content);
            if (contentLayout != null) {
                contentLayout.setBackgroundColor(bgColor);
            }

            // === TOOLBAR ===
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setBackgroundColor(bgColor);
                toolbar.setTitleTextColor(textColor);
                if (toolbar.getNavigationIcon() != null) {
                    toolbar.getNavigationIcon().setTint(textColor);
                }
            }

            com.google.android.material.appbar.AppBarLayout appBar = findViewById(R.id.app_bar_layout);
            if (appBar != null) {
                appBar.setBackgroundColor(bgColor);
            }

            // === TEXT COLORS ===
            // Section headers (secondary color)
            setTextViewColor(R.id.appearance_header, secondaryTextColor);
            setTextViewColor(R.id.apps_header, secondaryTextColor);
            setTextViewColor(R.id.system_header, secondaryTextColor);

            // Setting titles (primary color)
            setTextViewColor(R.id.theme_title, textColor);
            setTextViewColor(R.id.font_size_title, textColor);
            setTextViewColor(R.id.quick_info_title, textColor);
            setTextViewColor(R.id.show_day_title, textColor);
            setTextViewColor(R.id.app_sorting_title, textColor);
            setTextViewColor(R.id.hidden_apps_title, textColor);

            // Summaries/Status (secondary color)
            setTextViewColor(R.id.theme_summary, secondaryTextColor);
            setTextViewColor(R.id.font_size_summary, secondaryTextColor);
            setTextViewColor(R.id.quick_info_status, secondaryTextColor);
            setTextViewColor(R.id.show_day_status, secondaryTextColor);
            setTextViewColor(R.id.app_sorting_summary, secondaryTextColor);
            setTextViewColor(R.id.hidden_apps_summary, secondaryTextColor);

            android.util.Log.d("SettingsActivity", "applyTheme() completed successfully");

        } catch (Exception e) {
            android.util.Log.e("SettingsActivity", "Error in applyTheme()", e);
        }
    }

    /**
     * Helper to safely set TextView color
     */
    private void setTextViewColor(int viewId, int color) {
        try {
            View view = findViewById(viewId);
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(color);
            }
        } catch (Exception e) {
            android.util.Log.e("SettingsActivity", "Error setting text color for view " + viewId, e);
        }
    }

    /**
     * Enable edge-to-edge display and hide status bar
     */
    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide status bar but keep navigation bar for gestures
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Re-apply when window regains focus
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decorView.setSystemUiVisibility(uiOptions);
            }
        });
    }

    /**
     * Setup Material Toolbar with back button
     */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> navigateToHome());
        }
    }

    /**
     * Navigate back to home screen
     */
    private void navigateToHome() {
        Intent intent = new Intent(this, com.minimalist.launcher.ui.launcher.LauncherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        navigateToHome();
    }

    private void setupSettings() {
        // Theme setting
        findViewById(R.id.theme_setting).setOnClickListener(v -> showThemeDialog());
        updateThemeSummary();

        // Font size setting
        findViewById(R.id.font_size_setting).setOnClickListener(v -> showFontSizeDialog());
        updateFontSizeSummary();

        // Quick info toggle
        View quickInfoToggle = findViewById(R.id.quick_info_toggle);
        TextView quickInfoStatus = findViewById(R.id.quick_info_status);
        boolean showQuickInfo = prefs.getBoolean("show_quick_info", false);
        quickInfoStatus.setText(showQuickInfo ? "Shown" : "Hidden");
        quickInfoToggle.setOnClickListener(v -> {
            boolean current = prefs.getBoolean("show_quick_info", false);
            prefs.edit().putBoolean("show_quick_info", !current).apply();
            quickInfoStatus.setText(!current ? "Shown" : "Hidden");

            // Broadcast to launcher to update immediately
            Intent updateIntent = new Intent("com.minimalist.launcher.UPDATE_QUICK_INFO");
            updateIntent.putExtra("show_quick_info", !current);
            sendBroadcast(updateIntent);
        });

        // Show day of week toggle
        View dayToggle = findViewById(R.id.show_day_toggle);
        TextView dayStatus = findViewById(R.id.show_day_status);
        boolean showDay = prefs.getBoolean("show_day_of_week", true);
        dayStatus.setText(showDay ? "Yes" : "No");
        dayToggle.setOnClickListener(v -> {
            boolean current = prefs.getBoolean("show_day_of_week", true);
            prefs.edit().putBoolean("show_day_of_week", !current).apply();
            dayStatus.setText(!current ? "Yes" : "No");
        });

        // Hidden apps management
        View hiddenAppsOption = findViewById(R.id.hidden_apps_setting);
        hiddenAppsOption.setOnClickListener(v -> showHiddenAppsDialog());
        updateHiddenAppsCount();

        // Set as default launcher
        View setDefaultOption = findViewById(R.id.set_default_launcher);
        setDefaultOption.setOnClickListener(v -> setAsDefaultLauncher());

        // App sorting
        View appSortingOption = findViewById(R.id.app_sorting_setting);
        appSortingOption.setOnClickListener(v -> showSortingDialog());
        updateSortingSummary();

        // Reset Favorites
        View resetFavoritesOption = findViewById(R.id.reset_favorites_setting);
        if (resetFavoritesOption != null) {
            resetFavoritesOption.setOnClickListener(v -> showResetFavoritesDialog());
        }
    }

    private void showThemeDialog() {
        String[] themes = { "OLED Black", "Dark Gray", "Light", "Auto" };
        int currentTheme = themeManager.getCurrentTheme();

        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
                    android.util.Log.d("SettingsActivity", "Theme selected: " + which);
                    themeManager.setTheme(which);

                    // Apply theme immediately to this activity
                    applyTheme();
                    updateThemeSummary();

                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateThemeSummary() {
        TextView summary = findViewById(R.id.theme_summary);
        summary.setText(themeManager.getThemeName(themeManager.getCurrentTheme()));
    }

    private void showFontSizeDialog() {
        String[] sizes = { "Small", "Medium", "Large" };
        int currentSize = prefs.getInt("font_size", 1);

        new AlertDialog.Builder(this)
                .setTitle("Select Font Size")
                .setSingleChoiceItems(sizes, currentSize, (dialog, which) -> {
                    prefs.edit().putInt("font_size", which).apply();
                    updateFontSizeSummary();

                    // Signal immediate refresh
                    Intent refreshIntent = new Intent();
                    refreshIntent.putExtra("settings_changed", true);
                    setResult(RESULT_OK, refreshIntent);

                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateFontSizeSummary() {
        TextView summary = findViewById(R.id.font_size_summary);
        String[] sizes = { "Small", "Medium", "Large" };
        int currentSize = prefs.getInt("font_size", 1);
        summary.setText(sizes[currentSize]);
    }

    private void showHiddenAppsDialog() {
        java.util.List<String> hiddenPackages = hiddenAppsManager.getHiddenApps();

        if (hiddenPackages.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Hidden Apps")
                    .setMessage("No apps are currently hidden")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Get actual app names from package names
        android.content.pm.PackageManager pm = getPackageManager();
        final java.util.List<String> appNames = new java.util.ArrayList<>();
        final java.util.List<String> packageNames = new java.util.ArrayList<>();

        for (String packageName : hiddenPackages) {
            try {
                android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                String appName = pm.getApplicationLabel(appInfo).toString();
                appNames.add(appName);
                packageNames.add(packageName);
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                // App uninstalled but still in hidden list, skip it
            }
        }

        if (appNames.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Hidden Apps")
                    .setMessage("No hidden apps found (they may have been uninstalled)")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] appNameArray = appNames.toArray(new String[0]);
        boolean[] checked = new boolean[appNameArray.length];

        new AlertDialog.Builder(this)
                .setTitle("Unhide Apps")
                .setMultiChoiceItems(appNameArray, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Unhide Selected", (dialog, which) -> {
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            hiddenAppsManager.unhideApp(packageNames.get(i));
                        }
                    }
                    updateHiddenAppsCount();

                    // Force refresh app list immediately
                    Intent refreshIntent = new Intent();
                    refreshIntent.putExtra("refresh_apps", true);
                    setResult(RESULT_OK, refreshIntent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateHiddenAppsCount() {
        TextView summary = findViewById(R.id.hidden_apps_summary);
        int count = hiddenAppsManager.getHiddenCount();
        summary.setText(count + " app" + (count == 1 ? "" : "s") + " hidden");
    }

    private void showSortingDialog() {
        String[] options = { "Alphabetical", "Most Used", "Recently Installed" };
        int current = prefs.getInt("app_sort_order", 0);

        new AlertDialog.Builder(this)
                .setTitle("App List Sorting")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    prefs.edit().putInt("app_sort_order", which).apply();
                    updateSortingSummary();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSortingSummary() {
        TextView summary = findViewById(R.id.app_sorting_summary);
        String[] options = { "Alphabetical", "Most Used", "Recently Installed" };
        int current = prefs.getInt("app_sort_order", 0);
        summary.setText(options[current]);
    }

    /**
     * Open Android's default launcher settings
     */
    private void setAsDefaultLauncher() {
        try {
            // Method 1: Open default apps settings (works on most Android versions)
            Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
            startActivity(intent);

            android.widget.Toast.makeText(this,
                    "Select Minimalist Launcher as your default",
                    android.widget.Toast.LENGTH_LONG).show();

        } catch (android.content.ActivityNotFoundException e) {
            try {
                // Method 2: Trigger home button to show launcher chooser
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                android.widget.Toast.makeText(this,
                        "Select this app and choose 'Always'",
                        android.widget.Toast.LENGTH_LONG).show();

            } catch (Exception ex) {
                android.widget.Toast.makeText(this,
                        "Please set default launcher in Settings > Apps > Default Apps",
                        android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showResetFavoritesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Favorites")
                .setMessage("Remove all favorite apps?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    FavoritesHelper favoritesHelper = new FavoritesHelper(this);
                    favoritesHelper.clearAll();
                    android.widget.Toast.makeText(this, "Favorites cleared", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register theme change receiver (only if not already registered)
        if (!isThemeReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter(ThemeManager.ACTION_THEME_CHANGED);
                // Android 13+ requires RECEIVER_NOT_EXPORTED for internal broadcasts
                androidx.core.content.ContextCompat.registerReceiver(
                    this,
                    themeChangeReceiver,
                    filter,
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                );
                isThemeReceiverRegistered = true;
            } catch (Exception e) {
                android.util.Log.e("SettingsActivity", "Error registering theme receiver", e);
            }
        }

        setupEdgeToEdge();

        // Refresh all summaries to reflect any changes
        updateThemeSummary();
        updateFontSizeSummary();
        updateHiddenAppsCount();
        updateSortingSummary();

        // Update toggle states
        TextView quickInfoStatus = findViewById(R.id.quick_info_status);
        boolean showQuickInfo = prefs.getBoolean("show_quick_info", false);
        quickInfoStatus.setText(showQuickInfo ? "Shown" : "Hidden");

        TextView dayStatus = findViewById(R.id.show_day_status);
        boolean showDay = prefs.getBoolean("show_day_of_week", true);
        dayStatus.setText(showDay ? "Yes" : "No");
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister theme change receiver (only if registered)
        if (isThemeReceiverRegistered) {
            try {
                unregisterReceiver(themeChangeReceiver);
                isThemeReceiverRegistered = false;
            } catch (Exception e) {
                android.util.Log.e("SettingsActivity", "Error unregistering theme receiver", e);
            }
        }
    }
}
