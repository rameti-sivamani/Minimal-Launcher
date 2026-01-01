package com.minimalist.launcher.ui.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.minimalist.launcher.R;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.ui.applist.AppListActivity;
import com.minimalist.launcher.ui.settings.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Main launcher activity - the home screen
 * Displays time, date, and daily screen time in a minimal interface
 */
public class LauncherActivity extends AppCompatActivity {

    private LauncherViewModel viewModel;
    private TextView timeText;
    private TextView dateText;
    private TextView focusModeText;
    private RecyclerView favoritesRecyclerView;
    private FavoriteAppsAdapter favoritesAdapter;
    private com.minimalist.launcher.utils.FavoritesHelper favoritesHelper;

    private boolean isThemeReceiverRegistered = false;

    // Broadcast receiver for theme changes
    private BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                applyTheme();
            } catch (Exception e) {
                android.util.Log.e("LauncherActivity", "Error applying theme", e);
            }
        }
    };

    // Broadcast receiver for quick info changes
    private BroadcastReceiver quickInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean showQuickInfo = intent.getBooleanExtra("show_quick_info", false);
            android.util.Log.d("LauncherActivity", "Quick info update received: " + showQuickInfo);
            updateQuickInfoVisibility(showQuickInfo);
        }
    };

    private Handler timeUpdateHandler;
    private Runnable timeUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        // Initialize ViewModel and Helpers
        viewModel = new ViewModelProvider(this).get(LauncherViewModel.class);
        favoritesHelper = new com.minimalist.launcher.utils.FavoritesHelper(this);

        // Bind views
        timeText = findViewById(R.id.time_text);
        dateText = findViewById(R.id.date_text);
        focusModeText = findViewById(R.id.focus_mode_text);
        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view);

        TextView batteryText = findViewById(R.id.battery_text);
        TextView networkText = findViewById(R.id.network_text);
        View quickInfoContainer = findViewById(R.id.quick_info_container);

        View settingsButton = findViewById(R.id.settings_button);

        // Apply theme and font size AFTER views are initialized
        applyTheme();
        applyFontSize();

        // Set up quick info (hide for now, can be enabled in settings)
        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        boolean showQuickInfo = prefs.getBoolean("show_quick_info", false);

        quickInfoContainer.setVisibility(showQuickInfo ? View.VISIBLE : View.GONE);

        if (showQuickInfo) {
            updateBatteryInfo(batteryText);
            updateNetworkInfo(networkText);
        }

        // Set up favorite apps
        setupFavoriteApps();

        // Set up observers
        observeViewModel();

        // Update time every minute
        setupTimeUpdater();

        // Settings button
        settingsButton.setOnClickListener(v -> openSettings());

        // Enable edge-to-edge display and handle window insets
        setupEdgeToEdge();
        setupWindowInsets();

        // Set up swipe gesture to open app list
        setupSwipeGesture();

        // TEMPORARY: Also add tap listener for debugging
        findViewById(R.id.time_text).setOnClickListener(v -> {
            android.util.Log.d("LauncherActivity", "Time text clicked - opening app list");
            openAppList();
        });

        // Register quick info receiver (keeps working when navigating to Settings)
        try {
            IntentFilter quickInfoFilter = new IntentFilter("com.minimalist.launcher.UPDATE_QUICK_INFO");
            // Android 13+ requires RECEIVER_NOT_EXPORTED for internal broadcasts
            androidx.core.content.ContextCompat.registerReceiver(
                    this,
                    quickInfoReceiver,
                    quickInfoFilter,
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
            android.util.Log.d("LauncherActivity", "Quick info receiver registered in onCreate");
        } catch (Exception e) {
            android.util.Log.e("LauncherActivity", "Error registering quick info receiver", e);
        }

        // Setup persistent immersive mode (hide status bar)
        setupPersistentImmersiveMode();

        // Check and request permissions on first launch
        checkAndRequestPermissions();
    }

    /**
     * Enable persistent immersive fullscreen mode
     */
    private void setupPersistentImmersiveMode() {
        View decorView = getWindow().getDecorView();
        // Hide status bar but keep navigation bar visible for recent apps gesture
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Re-apply when visibility changes
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decorView.setSystemUiVisibility(uiOptions);
            }
        });
    }

    // Gesture detector for swipe detection
    private android.view.GestureDetector gestureDetector;

    /**
     * Set up swipe right gesture to open app list
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

                        android.util.Log.d("LauncherActivity",
                                "Fling detected: diffX=" + diffX + ", velocityX=" + velocityX);

                        // Swipe right (left-to-right) to open app list
                        if (Math.abs(diffX) > Math.abs(diffY) &&
                                diffX > SWIPE_THRESHOLD &&
                                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            android.util.Log.d("LauncherActivity", "Swipe right detected - opening app list");
                            openAppList();
                            return true;
                        }

                        // Swipe left (right-to-left) to open camera
                        if (Math.abs(diffX) > Math.abs(diffY) &&
                                diffX < -SWIPE_THRESHOLD &&
                                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            android.util.Log.d("LauncherActivity", "Swipe left detected - opening camera");
                            openCamera();
                            return true;
                        }

                        return false;
                    }
                });
    }

    /**
     * Set up favorite apps list
     */
    private void setupFavoriteApps() {
        favoritesAdapter = new FavoriteAppsAdapter(new FavoriteAppsAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(com.minimalist.launcher.data.model.AppInfo app) {
                launchFavoriteApp(app);
            }

            @Override
            public void onAppLongClick(com.minimalist.launcher.data.model.AppInfo app) {
                showRemoveFavoriteDialog(app);
            }
        });
        favoritesRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        favoritesRecyclerView.setAdapter(favoritesAdapter);

        // Load favorite apps
        loadFavoriteApps();
    }

    /**
     * Load favorite apps from SharedPreferences
     */
    private void loadFavoriteApps() {
        new Thread(() -> {
            // Set default favorites if none exist
            List<String> defaultPackages = java.util.Arrays.asList(
                    "com.android.dialer", "com.google.android.dialer",
                    "com.android.mms", "com.google.android.apps.messaging",
                    "com.android.camera", "com.google.android.GoogleCamera",
                    "com.android.settings");
            favoritesHelper.setDefaultFavorites(defaultPackages);

            // Load saved favorites
            List<String> favoritePackages = favoritesHelper.getFavorites();
            android.content.pm.PackageManager pm = getPackageManager();
            List<com.minimalist.launcher.data.model.AppInfo> favorites = new ArrayList<>();

            for (String packageName : favoritePackages) {
                try {
                    android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    String label = pm.getApplicationLabel(appInfo).toString();
                    android.graphics.drawable.Drawable icon = pm.getApplicationIcon(appInfo);

                    com.minimalist.launcher.data.model.AppInfo app = new com.minimalist.launcher.data.model.AppInfo(
                            label, packageName, icon, false);
                    favorites.add(app);
                } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                    // App not found, remove from favorites
                    favoritesHelper.removeFavorite(packageName);
                }
            }

            // Update UI on main thread
            runOnUiThread(() -> {
                if (!favorites.isEmpty()) {
                    favoritesAdapter.setFavoriteApps(favorites);
                    favoritesRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    favoritesRecyclerView.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    /**
     * Show dialog to remove app from favorites
     */
    private void showRemoveFavoriteDialog(com.minimalist.launcher.data.model.AppInfo app) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Remove from Favorites")
                .setMessage("Remove " + app.getAppName() + " from favorites?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    favoritesHelper.removeFavorite(app.getPackageName());
                    loadFavoriteApps(); // Reload list
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Launch a favorite app
     */
    private void launchFavoriteApp(com.minimalist.launcher.data.model.AppInfo app) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (intent != null) {
                startActivity(intent);
            }
        } catch (Exception e) {
            android.util.Log.e("LauncherActivity", "Error launching favorite app", e);
        }
    }

    /**
     * Override dispatchTouchEvent to capture gestures before child views
     * This ensures swipe gestures work even when settings button is present
     */
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        // Let gesture detector try to handle it first
        if (gestureDetector != null && gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        // If gesture detector didn't consume it, let normal touch handling proceed
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Check and request necessary permissions on startup
     */
    private void checkAndRequestPermissions() {
        // Check if this is first launch or permissions not granted
        android.content.SharedPreferences prefs = getSharedPreferences("launcher_prefs", MODE_PRIVATE);
        boolean hasRequestedPermissions = prefs.getBoolean("permissions_requested", false);

        if (!hasRequestedPermissions) {
            // Show welcome dialog
            showWelcomeDialog();
        } else {
            // Check if permissions are still missing
            checkMissingPermissions();
        }
    }

    /**
     * Show welcome dialog on first launch
     */
    private void showWelcomeDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Welcome to Minimalist Launcher")
                .setMessage("This launcher needs two permissions to work properly:\n\n" +
                        "1. Usage Stats - to track screen time\n" +
                        "2. Set as Default Launcher - to replace your home screen\n\n" +
                        "Let's set these up now!")
                .setPositiveButton("Continue", (dialog, which) -> {
                    requestUsageStatsPermission();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Request Usage Stats permission
     */
    private void requestUsageStatsPermission() {
        if (!com.minimalist.launcher.utils.PermissionHelper.hasUsageStatsPermission(this)) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Usage Stats Permission")
                    .setMessage("Tap 'Grant Permission' to allow this app to access usage statistics. " +
                            "This is needed to track your screen time.\n\n" +
                            "Find 'Minimalist Launcher' in the list and enable the permission.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        com.minimalist.launcher.utils.PermissionHelper.requestUsageStatsPermission(this);
                        // Mark as requested
                        getSharedPreferences("launcher_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("permissions_requested", true)
                                .apply();
                    })
                    .setNegativeButton("Skip", (dialog, which) -> {
                        promptSetAsDefaultLauncher();
                    })
                    .show();
        } else {
            promptSetAsDefaultLauncher();
        }
    }

    /**
     * Prompt user to set as default launcher
     */
    private void promptSetAsDefaultLauncher() {
        if (!com.minimalist.launcher.utils.PermissionHelper.isDefaultLauncher(this)) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Set as Default Launcher")
                    .setMessage(
                            "To use this as your home screen, press the Home button and select 'Minimalist Launcher', then tap 'Always'.\n\n"
                                    +
                                    "Or you can open launcher settings now.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        com.minimalist.launcher.utils.PermissionHelper.requestDefaultLauncher(this);
                    })
                    .setNegativeButton("I'll Do Later", null)
                    .show();
        }
    }

    /**
     * Check for missing permissions (for subsequent launches)
     */
    private void checkMissingPermissions() {
        // Silently check - don't annoy user every time
        // They can go to settings if needed
    }

    /**
     * Enable edge-to-edge display (modern replacement for immersive mode)
     */
    private void setupEdgeToEdge() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Hide system bars (status bar and navigation bar)
        getWindow().getInsetsController().hide(
                android.view.WindowInsets.Type.systemBars());
    }

    /**
     * Setup window insets handling for proper padding
     */
    private void setupWindowInsets() {
        View mainContainer = findViewById(R.id.main_container);
        if (mainContainer == null)
            return;

        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Get original padding from dimensions
            int horizontalPadding = getResources().getDimensionPixelSize(R.dimen.screen_padding_horizontal);
            int topPadding = getResources().getDimensionPixelSize(R.dimen.screen_padding_top);
            int bottomPadding = getResources().getDimensionPixelSize(R.dimen.screen_padding_bottom);

            // Apply padding with insets
            v.setPadding(
                    horizontalPadding,
                    insets.top + topPadding,
                    horizontalPadding,
                    insets.bottom + bottomPadding);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Observe ViewModel LiveData
     */
    private void observeViewModel() {
        // Current time
        viewModel.getCurrentTime().observe(this, time -> {
            if (time != null) {
                timeText.setText(time);
            }
        });

        // Current date
        viewModel.getCurrentDate().observe(this, date -> {
            if (date != null) {
                dateText.setText(date);
            }
        });

        // Active focus mode
        viewModel.getActiveFocusMode().observe(this, focusMode -> {
            if (focusMode != null && focusMode.isActive()) {
                focusModeText.setText(focusMode.getName() + " (Active)");
                focusModeText.setVisibility(View.VISIBLE);
            } else {
                focusModeText.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Set up time updater that runs every minute
     */
    private void setupTimeUpdater() {
        timeUpdateHandler = new Handler(Looper.getMainLooper());
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                viewModel.updateTimeAndDate();
                timeUpdateHandler.postDelayed(this, 60000); // Update every minute
            }
        };
    }

    /**
     * Open app list activity
     */
    private void openAppList() {
        android.util.Log.d("LauncherActivity", "openAppList() called");
        try {
            Intent intent = new Intent(this, AppListActivity.class);
            android.util.Log.d("LauncherActivity", "Starting AppListActivity");
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("LauncherActivity", "Error opening app list", e);
        }
    }

    /**
     * Open settings activity
     */
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Apply selected theme to activity (enhanced for immediate updates)
     */
    private void applyTheme() {
        com.minimalist.launcher.utils.ThemeManager themeManager = new com.minimalist.launcher.utils.ThemeManager(this);

        // Get theme colors
        int backgroundColor = themeManager.getBackgroundColor();
        int textColor = themeManager.getTextColor();
        int secondaryTextColor = themeManager.getSecondaryTextColor();

        // Apply to status bar (make it same color as background)
        getWindow().setStatusBarColor(backgroundColor);

        // Make status bar icons light/dark based on theme
        View decorView = getWindow().getDecorView();
        int systemUiVisibility = decorView.getSystemUiVisibility();
        if (themeManager.isDarkTheme()) {
            // Dark theme - use light status bar icons
            systemUiVisibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            // Light theme - use dark status bar icons
            systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decorView.setSystemUiVisibility(systemUiVisibility);

        // Apply background color to window and root layouts
        decorView.setBackgroundColor(backgroundColor);

        View rootView = decorView.findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setBackgroundColor(backgroundColor);
        }

        // Apply to window background
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Apply to launcher root ConstraintLayout
        View launcherRoot = findViewById(R.id.launcher_root);
        if (launcherRoot != null) {
            launcherRoot.setBackgroundColor(backgroundColor);
        }

        // Apply to main container
        View mainContainer = findViewById(R.id.main_container);
        if (mainContainer != null) {
            mainContainer.setBackgroundColor(backgroundColor);
        }

        // Update ALL text colors
        if (timeText != null) {
            timeText.setTextColor(textColor);
        }
        if (dateText != null) {
            dateText.setTextColor(secondaryTextColor);
        }
        if (focusModeText != null) {
            focusModeText.setTextColor(secondaryTextColor);
        }

        // Update quick info text colors
        TextView batteryText = findViewById(R.id.battery_text);
        if (batteryText != null) {
            batteryText.setTextColor(secondaryTextColor);
        }

        TextView networkText = findViewById(R.id.network_text);
        if (networkText != null) {
            networkText.setTextColor(secondaryTextColor);
        }

        // Refresh favorites adapter to update item colors
        if (favoritesAdapter != null) {
            favoritesAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Apply font size from settings
     */
    private void applyFontSize() {
        if (timeText == null || dateText == null) {
            return; // Views not initialized yet
        }

        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        int fontSize = prefs.getInt("font_size", 1); // 0=small, 1=medium, 2=large

        float timeSize, dateSize;
        switch (fontSize) {
            case 0: // Small
                timeSize = 56f;
                dateSize = 14f;
                break;
            case 2: // Large
                timeSize = 72f;
                dateSize = 18f;
                break;
            default: // Medium
                timeSize = 64f;
                dateSize = 16f;
                break;
        }

        timeText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, timeSize);
        dateText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, dateSize);
    }

    /**
     * Open camera app
     */
    private void openCamera() {
        android.util.Log.d("LauncherActivity", "openCamera() called");
        try {
            // Use INTENT_STILL_IMAGE_CAMERA to open the default camera app
            Intent intent = new Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to generic camera intent if the above doesn't work
                Intent fallbackIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                if (fallbackIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(fallbackIntent);
                } else {
                    android.widget.Toast.makeText(this, "No camera app found", android.widget.Toast.LENGTH_SHORT)
                            .show();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("LauncherActivity", "Error opening camera", e);
            android.widget.Toast.makeText(this, "Unable to open camera", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update quick info visibility instantly without app restart
     */
    private void updateQuickInfoVisibility(boolean show) {
        View quickInfoContainer = findViewById(R.id.quick_info_container);
        TextView batteryText = findViewById(R.id.battery_text);
        TextView networkText = findViewById(R.id.network_text);

        if (quickInfoContainer != null) {
            quickInfoContainer.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show && batteryText != null && networkText != null) {
                updateBatteryInfo(batteryText);
                updateNetworkInfo(networkText);
            }
        }
    }

    /**
     * Update battery info
     */
    private void updateBatteryInfo(TextView batteryText) {
        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(
                    android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level * 100 / (float) scale;
                batteryText.setText("🔋 " + Math.round(batteryPct) + "%");
            }
        } catch (Exception e) {
            batteryText.setText("🔋 --");
        }
    }

    /**
     * Update network info
     */
    private void updateNetworkInfo(TextView networkText) {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnected()) {
                if (activeNetwork.getType() == android.net.ConnectivityManager.TYPE_WIFI) {
                    networkText.setText("📶 WiFi");
                } else if (activeNetwork.getType() == android.net.ConnectivityManager.TYPE_MOBILE) {
                    networkText.setText("📶 Mobile");
                } else {
                    networkText.setText("📶 Connected");
                }
            } else {
                networkText.setText("📶 No Network");
            }
        } catch (Exception e) {
            networkText.setText("📶 --");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register theme change receiver (only if not already registered)
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
                android.util.Log.e("LauncherActivity", "Error registering theme receiver", e);
            }
        }

        // Re-apply theme and font size in case settings changed
        applyTheme();
        applyFontSize();

        // Start time updates
        viewModel.updateTimeAndDate();
        timeUpdateHandler.post(timeUpdateRunnable);

        // Re-apply edge-to-edge mode
        setupEdgeToEdge();

        // Reload favorites in case they were changed in app list
        loadFavoriteApps();

        // Refresh quick info visibility from preferences (in case changed in Settings)
        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        boolean showQuickInfo = prefs.getBoolean("show_quick_info", false);
        View quickInfoContainer = findViewById(R.id.quick_info_container);
        if (quickInfoContainer != null) {
            quickInfoContainer.setVisibility(showQuickInfo ? View.VISIBLE : View.GONE);
            android.util.Log.d("LauncherActivity", "Quick info visibility refreshed in onResume: " + showQuickInfo);
        }
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
                android.util.Log.e("LauncherActivity", "Error unregistering theme receiver", e);
            }
        }

        // Stop time updates
        if (timeUpdateHandler != null && timeUpdateRunnable != null) {
            timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
        }
    }

    /**
     * Disable back button - launcher should not close
     */
    @Override
    public void onBackPressed() {
        // Do nothing - prevent exiting launcher
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister quick info receiver
        try {
            unregisterReceiver(quickInfoReceiver);
            android.util.Log.d("LauncherActivity", "Quick info receiver unregistered");
        } catch (Exception e) {
            android.util.Log.e("LauncherActivity", "Error unregistering quick info receiver", e);
        }
    }
}
