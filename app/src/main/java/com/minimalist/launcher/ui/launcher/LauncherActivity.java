package com.minimalist.launcher.ui.launcher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.ui.applist.AppListActivity;
import com.minimalist.launcher.ui.settings.SettingsActivity;
import com.minimalist.launcher.utils.LaunchGate;
import com.minimalist.launcher.utils.PermissionHelper;
import com.minimalist.launcher.utils.Prefs;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;

/**
 * Main launcher activity - the home screen.
 * Shows the clock, date, today's screen time and favorite apps.
 * Swipe right or up (or tap the clock) for all apps, swipe left for the camera.
 */
public class LauncherActivity extends AppCompatActivity {

    private static final String ONBOARDING_PREFS = "launcher_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final long CLOCK_TICK_MS = 15_000;

    private LauncherViewModel viewModel;
    private TextView timeText;
    private TextView dateText;
    private TextView screenTimeText;
    private TextView focusModeText;
    private TextView batteryText;
    private TextView networkText;
    private View quickInfoContainer;
    private RecyclerView favoritesRecyclerView;
    private FavoriteAppsAdapter favoritesAdapter;
    private GestureDetector gestureDetector;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            viewModel.updateTimeAndDate();
            clockHandler.postDelayed(this, CLOCK_TICK_MS);
        }
    };

    private ActivityResultLauncher<Intent> defaultLauncherRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        viewModel = new ViewModelProvider(this).get(LauncherViewModel.class);

        timeText = findViewById(R.id.time_text);
        dateText = findViewById(R.id.date_text);
        screenTimeText = findViewById(R.id.screen_time_text);
        focusModeText = findViewById(R.id.focus_mode_text);
        batteryText = findViewById(R.id.battery_text);
        networkText = findViewById(R.id.network_text);
        quickInfoContainer = findViewById(R.id.quick_info_container);
        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view);

        defaultLauncherRequest = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    // Nothing to do: the system dialog applies the choice itself
                });

        SystemBars.padForInsets(findViewById(R.id.launcher_root), true, true);

        setupFavoriteApps();
        observeViewModel();
        setupGestures();

        findViewById(R.id.settings_button).setOnClickListener(v -> openSettings());
        timeText.setOnClickListener(v -> openAppList());
        screenTimeText.setOnClickListener(v -> {
            if (!PermissionHelper.hasUsageStatsPermission(this)) {
                showUsageAccessDisclosure(null);
            }
        });

        // The home screen is the root: Back does nothing here
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Intentionally empty
            }
        });

        if (savedInstanceState == null) {
            runOnboardingIfNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        applyFontSize();

        viewModel.updateTimeAndDate();
        clockHandler.removeCallbacks(clockTick);
        clockHandler.postDelayed(clockTick, CLOCK_TICK_MS);

        viewModel.loadFavorites();
        updateScreenTimeVisibility();
        updateQuickInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
    }

    // ---------------------------------------------------------------------
    // Favorites
    // ---------------------------------------------------------------------

    private void setupFavoriteApps() {
        favoritesAdapter = new FavoriteAppsAdapter(new FavoriteAppsAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(AppInfo app) {
                LaunchGate.launch(LauncherActivity.this, viewModel.getAppRepository(),
                        app.getPackageName(), null);
            }

            @Override
            public void onAppLongClick(AppInfo app) {
                showFavoriteOptions(app);
            }
        });
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecyclerView.setAdapter(favoritesAdapter);
    }

    private void showFavoriteOptions(AppInfo app) {
        String[] options = {
                getString(R.string.move_up),
                getString(R.string.move_down),
                getString(R.string.remove_from_favorites)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(app.getAppName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.moveFavorite(app.getPackageName(), -1);
                    } else if (which == 1) {
                        viewModel.moveFavorite(app.getPackageName(), 1);
                    } else {
                        viewModel.removeFavorite(app.getPackageName());
                    }
                })
                .show();
    }

    // ---------------------------------------------------------------------
    // Observers
    // ---------------------------------------------------------------------

    private void observeViewModel() {
        viewModel.getCurrentTime().observe(this, time -> timeText.setText(time));
        viewModel.getCurrentDate().observe(this, date -> dateText.setText(date));

        viewModel.getFavorites().observe(this, favorites -> {
            favoritesAdapter.setFavoriteApps(favorites);
            favoritesRecyclerView.setVisibility(favorites.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getScreenTimeMillis().observe(this, millis -> {
            if (millis == null) {
                return;
            }
            if (millis < 0) {
                screenTimeText.setText(R.string.screen_time_tap_to_enable);
            } else {
                screenTimeText.setText(getString(R.string.screen_time_today,
                        ScreenTimeCalculator.format(millis)));
            }
        });

        viewModel.getActiveFocusMode().observe(this, focusMode -> {
            if (focusMode != null && focusMode.isActive()) {
                focusModeText.setText(getString(R.string.focus_mode_active, focusMode.getName()));
                focusModeText.setVisibility(View.VISIBLE);
            } else {
                focusModeText.setVisibility(View.GONE);
            }
        });
    }

    private void updateScreenTimeVisibility() {
        if (Prefs.showScreenTime(this)) {
            screenTimeText.setVisibility(View.VISIBLE);
            viewModel.refreshScreenTime();
        } else {
            screenTimeText.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------------
    // Gestures and navigation
    // ---------------------------------------------------------------------

    private void setupGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                    float velocityX, float velocityY) {
                if (e1 == null) {
                    return false;
                }
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) < SWIPE_THRESHOLD || Math.abs(velocityX) < SWIPE_VELOCITY_THRESHOLD) {
                        return false;
                    }
                    if (diffX > 0) {
                        openAppList();
                    } else {
                        openCamera();
                    }
                    return true;
                }

                // Swipe up opens the app list
                if (diffY < -SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    openAppList();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Let the gesture detector see touches before child views so swipes work anywhere
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void openAppList() {
        startActivity(new Intent(this, AppListActivity.class));
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, R.string.no_camera_app, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------------------------------------------------------------------
    // Onboarding (prominent disclosure before any permission request)
    // ---------------------------------------------------------------------

    private void runOnboardingIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(ONBOARDING_PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.welcome_title)
                .setMessage(R.string.welcome_message)
                .setCancelable(false)
                .setPositiveButton(R.string.get_started, (dialog, which) -> {
                    prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();
                    showUsageAccessDisclosure(this::promptSetAsDefaultLauncher);
                })
                .show();
    }

    /**
     * Explain exactly what usage access is used for before sending the user to grant it
     */
    private void showUsageAccessDisclosure(Runnable onDone) {
        if (PermissionHelper.hasUsageStatsPermission(this)) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.usage_access_title)
                .setMessage(R.string.usage_access_disclosure)
                .setPositiveButton(R.string.grant_permission,
                        (dialog, which) -> PermissionHelper.requestUsageStatsPermission(this))
                .setNegativeButton(R.string.not_now, (dialog, which) -> {
                    if (onDone != null) {
                        onDone.run();
                    }
                })
                .show();
    }

    private void promptSetAsDefaultLauncher() {
        if (PermissionHelper.isDefaultLauncher(this)) {
            return;
        }
        try {
            defaultLauncherRequest.launch(PermissionHelper.createDefaultLauncherIntent(this));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.set_default_manually, Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------------
    // Appearance
    // ---------------------------------------------------------------------

    private void applyTheme() {
        ThemeManager themeManager = new ThemeManager(this);
        int backgroundColor = themeManager.getBackgroundColor();
        int textColor = themeManager.getTextColor();
        int secondaryTextColor = themeManager.getSecondaryTextColor();

        SystemBars.apply(this, themeManager.isDarkTheme());
        getWindow().getDecorView().setBackgroundColor(backgroundColor);
        findViewById(R.id.launcher_root).setBackgroundColor(backgroundColor);

        timeText.setTextColor(textColor);
        dateText.setTextColor(secondaryTextColor);
        screenTimeText.setTextColor(secondaryTextColor);
        focusModeText.setTextColor(secondaryTextColor);
        batteryText.setTextColor(secondaryTextColor);
        networkText.setTextColor(secondaryTextColor);
    }

    private void applyFontSize() {
        float timeSize;
        float dateSize;
        float favoriteSize;
        switch (Prefs.fontSize(this)) {
            case 0: // Small
                timeSize = 56f;
                dateSize = 14f;
                favoriteSize = 16f;
                break;
            case 2: // Large
                timeSize = 72f;
                dateSize = 18f;
                favoriteSize = 22f;
                break;
            default: // Medium
                timeSize = 64f;
                dateSize = 16f;
                favoriteSize = 18f;
                break;
        }
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, timeSize);
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize);

        ThemeManager themeManager = new ThemeManager(this);
        favoritesAdapter.setAppearance(Prefs.showIcons(this), themeManager.getTextColor(), favoriteSize);
    }

    // ---------------------------------------------------------------------
    // Quick info (battery / network)
    // ---------------------------------------------------------------------

    private void updateQuickInfo() {
        boolean show = Prefs.showQuickInfo(this);
        quickInfoContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }

        Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                batteryText.setText(getString(R.string.battery_level, Math.round(level * 100f / scale)));
            } else {
                batteryText.setText(R.string.battery_unknown);
            }
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm != null ? cm.getActiveNetwork() : null;
        NetworkCapabilities caps = network != null ? cm.getNetworkCapabilities(network) : null;
        if (caps == null) {
            networkText.setText(R.string.network_none);
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            networkText.setText(R.string.network_wifi);
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            networkText.setText(R.string.network_mobile);
        } else {
            networkText.setText(R.string.network_connected);
        }
    }
}
