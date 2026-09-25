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
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.data.wellbeing.StreakCalculator;
import com.minimalist.launcher.ui.applist.AppListActivity;
import com.minimalist.launcher.ui.settings.SettingsActivity;
import com.minimalist.launcher.ui.usage.UsageActivity;
import com.minimalist.launcher.utils.AppFilterHelper;
import com.minimalist.launcher.utils.FontScale;
import com.minimalist.launcher.utils.LaunchGate;
import com.minimalist.launcher.utils.PermissionHelper;
import com.minimalist.launcher.utils.Prefs;
import com.minimalist.launcher.utils.SwipeDetector;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;
import com.minimalist.launcher.utils.Transitions;

import java.util.Calendar;
import java.util.List;

/**
 * Main launcher activity - the home screen.
 * Shows the clock, date, today's screen time and favorite apps.
 * Swipe right or up (or tap the clock) for all apps, swipe left for the camera.
 */
public class LauncherActivity extends AppCompatActivity {

    private static final String ONBOARDING_PREFS = "launcher_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_WAS_DEFAULT = "was_default_launcher";
    private static final long CLOCK_TICK_MS = 15_000;
    private static final long DEFERRED_REFRESH_MS = 400;

    private String appliedAppearance = null;

    private LauncherViewModel viewModel;
    private TextView timeText;
    private TextView dateText;
    private TextView screenTimeText;
    private TextView streakPill;
    private View goalCard;
    private GoalRingView goalRing;
    private TextView goalTitle;
    private TextView intentionText;
    private TextView allAppsButton;
    private TextView focusModeText;
    private TextView batteryText;
    private TextView networkText;
    private View quickInfoContainer;
    private RecyclerView favoritesRecyclerView;
    private FavoriteAppsAdapter favoritesAdapter;
    private SwipeDetector swipeDetector;
    private List<FocusMode> focusModes;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            viewModel.updateTimeAndDate();
            updateFocusIndicator();
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
        streakPill = findViewById(R.id.streak_pill);
        goalCard = findViewById(R.id.goal_card);
        goalRing = findViewById(R.id.goal_ring);
        goalTitle = findViewById(R.id.goal_title);
        intentionText = findViewById(R.id.intention_text);
        allAppsButton = findViewById(R.id.all_apps_button);
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
        timeText.setOnClickListener(v -> openAppList(Transitions.Slide.FROM_BOTTOM));
        goalCard.setOnClickListener(v -> {
            if (PermissionHelper.hasUsageStatsPermission(this)) {
                startActivity(new Intent(this, UsageActivity.class));
            } else {
                showUsageAccessDisclosure(null);
            }
        });
        streakPill.setOnClickListener(v -> goalCard.performClick());
        intentionText.setOnClickListener(v -> editIntention());
        allAppsButton.setOnClickListener(v -> openAppList(Transitions.Slide.FROM_BOTTOM));

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
        // Only cheap work here: this runs every time the user returns home, during the
        // system's return animation. Anything heavier is deferred until it has finished.
        applyAppearanceIfChanged();

        viewModel.updateTimeAndDate();
        updateFocusIndicator();
        renderIntention();
        clockHandler.removeCallbacks(clockTick);
        clockHandler.postDelayed(clockTick, CLOCK_TICK_MS);

        clockHandler.removeCallbacks(deferredRefresh);
        clockHandler.postDelayed(deferredRefresh, DEFERRED_REFRESH_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
        clockHandler.removeCallbacks(deferredRefresh);
    }

    private final Runnable deferredRefresh = () -> {
        viewModel.loadFavorites();
        updateScreenTimeVisibility();
        updateQuickInfo();
        checkStillDefaultLauncher();
    };

    /**
     * Re-apply theme, font size and icon setting only when one of them changed
     */
    private void applyAppearanceIfChanged() {
        ThemeManager themeManager = new ThemeManager(this);
        String signature = themeManager.getSignature() + "/" + Prefs.fontSize(this) + "/" + Prefs.showIcons(this);
        if (signature.equals(appliedAppearance)) {
            return;
        }
        appliedAppearance = signature;
        applyTheme();
        applyFontSize();
    }

    // ---------------------------------------------------------------------
    // Default home app
    // ---------------------------------------------------------------------

    /**
     * Some phones (and battery savers that keep killing the launcher) silently switch the
     * home screen back to the built-in launcher. Notice it and offer a fix once per loss.
     */
    private void checkStillDefaultLauncher() {
        SharedPreferences prefs = getSharedPreferences(ONBOARDING_PREFS, MODE_PRIVATE);
        boolean isDefault = PermissionHelper.isDefaultLauncher(this);
        boolean wasDefault = prefs.getBoolean(KEY_WAS_DEFAULT, false);
        if (isDefault != wasDefault) {
            prefs.edit().putBoolean(KEY_WAS_DEFAULT, isDefault).apply();
        }
        if (!isDefault && wasDefault && prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.default_lost_title)
                    .setMessage(R.string.default_lost_message)
                    .setPositiveButton(R.string.set_as_default, (dialog, which) -> promptSetAsDefaultLauncher())
                    .setNeutralButton(R.string.battery_settings, (dialog, which) -> PermissionHelper.openBatterySettings(this))
                    .setNegativeButton(R.string.not_now, null)
                    .show();
        }
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

        viewModel.getWellbeing().observe(this, this::renderWellbeing);

        viewModel.getAllFocusModes().observe(this, modes -> {
            focusModes = modes;
            updateFocusIndicator();
        });
    }

    /**
     * Show the focus mode in effect now (manual or scheduled). Re-run on every clock tick
     * so scheduled modes appear and disappear on time.
     */
    private void updateFocusIndicator() {
        FocusMode focusMode = AppFilterHelper.resolveActive(focusModes, Calendar.getInstance());
        if (focusMode == null) {
            focusModeText.setVisibility(View.GONE);
            return;
        }
        focusModeText.setText(getString(focusMode.isActive()
                ? R.string.focus_mode_active : R.string.focus_mode_scheduled, focusMode.getName()));
        focusModeText.setVisibility(View.VISIBLE);
    }

    private void updateScreenTimeVisibility() {
        boolean show = Prefs.showScreenTime(this);
        goalCard.setVisibility(show ? View.VISIBLE : View.GONE);
        streakPill.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        if (show) {
            long goal = viewModel.getWellbeingStore().getGoalMillis();
            LauncherViewModel.Wellbeing current = viewModel.getWellbeing().getValue();
            // A changed goal needs a fresh streak right away
            viewModel.refreshWellbeing(current != null && current.goalMillis != goal);
        }
    }

    /**
     * Goal card ("1h 12m of 2h", ring) and streak pill
     */
    private void renderWellbeing(LauncherViewModel.Wellbeing state) {
        if (state == null) {
            return;
        }
        if (state.todayMillis < 0) {
            goalRing.setProgress(0f, "");
            goalTitle.setText(R.string.screen_time_tap_to_enable);
            screenTimeText.setText(R.string.goal_needs_access);
            streakPill.setText(R.string.streak_start);
            return;
        }
        float progress = StreakCalculator.goalProgress(state.todayMillis, state.goalMillis);
        goalRing.setProgress(progress, Math.round(progress * 100) + "%");
        goalTitle.setText(getString(R.string.goal_title,
                ScreenTimeCalculator.format(state.todayMillis), ScreenTimeCalculator.format(state.goalMillis)));
        long left = state.goalMillis - state.todayMillis;
        screenTimeText.setText(left >= 0
                ? getString(R.string.goal_left, ScreenTimeCalculator.format(left))
                : getString(R.string.goal_over, ScreenTimeCalculator.format(-left)));
        streakPill.setText(state.streak > 0
                ? getResources().getQuantityString(R.plurals.streak_days, state.streak, state.streak)
                : getString(R.string.streak_start));
    }

    // ---------------------------------------------------------------------
    // Daily intention
    // ---------------------------------------------------------------------

    private void renderIntention() {
        String intention = viewModel.getWellbeingStore().getTodayIntention();
        ThemeManager themeManager = new ThemeManager(this);
        if (intention == null) {
            intentionText.setText(R.string.intention_prompt);
            intentionText.setTextColor(themeManager.getSecondaryTextColor());
        } else {
            intentionText.setText(intention);
            intentionText.setTextColor(themeManager.getTextColor());
        }
    }

    private void editIntention() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(R.string.intention_hint);
        input.setSingleLine(false);
        input.setMaxLines(3);
        input.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(120) });
        String current = viewModel.getWellbeingStore().getTodayIntention();
        if (current != null) {
            input.setText(current);
            input.setSelection(current.length());
        }
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        int padding = getResources().getDimensionPixelSize(R.dimen.screen_padding_horizontal);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.intention_title)
                .setView(container)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    viewModel.getWellbeingStore().setTodayIntention(input.getText().toString());
                    renderIntention();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------------------------------------------------------------------
    // Gestures and navigation
    // ---------------------------------------------------------------------

    private void setupGestures() {
        swipeDetector = new SwipeDetector(this, getWindow().getDecorView(), direction -> {
            switch (direction) {
                case UP:
                    openAppList(Transitions.Slide.FROM_BOTTOM);
                    return true;
                case RIGHT:
                    openAppList(Transitions.Slide.FROM_LEFT);
                    return true;
                case LEFT:
                    openCamera();
                    return true;
                default:
                    return false;
            }
        });
    }

    /**
     * Let the gesture detector see touches before child views so swipes work anywhere
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (swipeDetector != null && swipeDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void openAppList(Transitions.Slide slide) {
        Transitions.start(this, new Intent(this, AppListActivity.class), slide);
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

        int accent = themeManager.getAccentColor();
        int surface = themeManager.getSurfaceColor();
        float density = getResources().getDisplayMetrics().density;
        android.graphics.Typeface body = themeManager.getBodyTypeface();

        timeText.setTextColor(textColor);
        timeText.setTypeface(themeManager.getClockTypeface());
        timeText.setLetterSpacing(themeManager.getClockLetterSpacing());
        for (TextView view : new TextView[] { dateText, screenTimeText, focusModeText, batteryText, networkText }) {
            view.setTextColor(secondaryTextColor);
            view.setTypeface(body);
        }
        goalTitle.setTextColor(textColor);
        goalTitle.setTypeface(body);
        intentionText.setTypeface(themeManager.getEffectiveTheme() == ThemeManager.STYLE_AURA
                ? body : android.graphics.Typeface.create("serif", android.graphics.Typeface.ITALIC));

        // Streak pill: surface background, accent flame
        streakPill.setBackground(rounded(surface, 18 * density));
        streakPill.setTextColor(textColor);
        streakPill.setTypeface(body);
        android.graphics.drawable.Drawable flame = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_flame);
        if (flame != null) {
            flame = flame.mutate();
            flame.setTint(accent);
            streakPill.setCompoundDrawablesRelativeWithIntrinsicBounds(flame, null, null, null);
        }

        goalCard.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(secondaryTextColor & 0x33FFFFFF),
                rounded(surface, 24 * density), null));
        goalRing.setColors(themeManager.isDarkTheme() ? 0xFF2A2A2D : 0xFFD8CFC1, accent, textColor);

        // Accent-filled pill button
        allAppsButton.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33000000), rounded(accent, 26 * density), null));
        allAppsButton.setTextColor(themeManager.getOnAccentColor());
        allAppsButton.setTypeface(body);

        ((android.widget.ImageButton) findViewById(R.id.settings_button)).setColorFilter(secondaryTextColor);
        renderIntention();
    }

    private static android.graphics.drawable.GradientDrawable rounded(int color, float radius) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        return shape;
    }

    private void applyFontSize() {
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, FontScale.clock(this));
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, FontScale.date(this));
        float secondary = FontScale.secondary(this);
        screenTimeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondary);
        focusModeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondary);
        batteryText.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondary);
        networkText.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondary);

        ThemeManager themeManager = new ThemeManager(this);
        favoritesAdapter.setAppearance(Prefs.showIcons(this), themeManager.getTextColor(),
                FontScale.appName(this) * 1.35f, themeManager.getBodyTypeface(), themeManager.useLowercaseNames());
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
