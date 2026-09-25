package com.minimalist.launcher.ui.lockin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.ui.launcher.GoalRingView;
import com.minimalist.launcher.utils.FavoritesHelper;
import com.minimalist.launcher.utils.LockInManager;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;
import com.minimalist.launcher.utils.ThemeStyler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lock-in: a focus timer. While it runs, the launcher only shows and opens the apps
 * picked for the session. Ending early needs a 2-second press so it is never an accident.
 */
public class LockInActivity extends AppCompatActivity {

    private static final long HOLD_TO_END_MS = 2000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private LockInManager lockIn;
    private ThemeManager theme;
    private int selectedMinutes = 25;
    private final Set<String> allowedApps = new LinkedHashSet<>();
    private final List<TextView> durationChips = new ArrayList<>();

    private GoalRingView ring;
    private TextView endButton;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!lockIn.isActive()) {
                Toast.makeText(LockInActivity.this, R.string.lock_in_done, Toast.LENGTH_LONG).show();
                showState();
                return;
            }
            long remaining = lockIn.getRemainingMillis();
            float done = 1f - (float) remaining / lockIn.getTotalMillis();
            long seconds = (remaining + 999) / 1000;
            ring.setProgress(done, String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60));
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable endAfterHold = () -> {
        lockIn.stop();
        Toast.makeText(this, R.string.lock_in_ended, Toast.LENGTH_SHORT).show();
        showState();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_in);

        lockIn = new LockInManager(this);
        theme = new ThemeManager(this);
        ring = findViewById(R.id.session_ring);
        endButton = findViewById(R.id.end_button);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        SystemBars.padForInsets(findViewById(R.id.lock_root), true, true);

        // Default allowed apps: the home screen favorites
        allowedApps.addAll(new FavoritesHelper(this).getFavorites());

        buildDurationChips();
        findViewById(R.id.apps_summary).setOnClickListener(v -> pickApps());
        findViewById(R.id.start_button).setOnClickListener(v -> startSession());
        setupHoldToEnd();

        applyTheme();
        updateAppsSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(endAfterHold);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void showState() {
        boolean active = lockIn.isActive();
        findViewById(R.id.setup_container).setVisibility(active ? View.GONE : View.VISIBLE);
        findViewById(R.id.session_container).setVisibility(active ? View.VISIBLE : View.GONE);
        handler.removeCallbacks(tick);
        if (active) {
            String label = lockIn.getLabel();
            ((TextView) findViewById(R.id.session_label)).setText(
                    label.isEmpty() ? getString(R.string.lock_in_default_label) : label);
            ((TextView) findViewById(R.id.session_apps)).setText(getString(R.string.lock_in_only_these,
                    describeApps(lockIn.getAllowedApps())));
            handler.post(tick);
        }
    }

    // ---------------------------------------------------------------------
    // Set-up
    // ---------------------------------------------------------------------

    private void buildDurationChips() {
        LinearLayout row = findViewById(R.id.duration_row);
        float density = getResources().getDisplayMetrics().density;
        for (int minutes : LockInManager.DURATION_OPTIONS_MINUTES) {
            TextView chip = new TextView(this);
            chip.setText(getString(R.string.minutes_short, minutes));
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            chip.setMinHeight(Math.round(44 * density));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setContentDescription(getString(R.string.minutes_long, minutes));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginEnd(Math.round(8 * density));
            final int value = minutes;
            chip.setOnClickListener(v -> {
                selectedMinutes = value;
                styleChips();
            });
            row.addView(chip, params);
            durationChips.add(chip);
        }
    }

    private void styleChips() {
        for (int i = 0; i < durationChips.size(); i++) {
            TextView chip = durationChips.get(i);
            boolean selected = LockInManager.DURATION_OPTIONS_MINUTES[i] == selectedMinutes;
            if (selected) {
                ThemeStyler.accentButton(chip, theme);
            } else {
                ThemeStyler.outlineButton(chip, theme);
            }
            chip.setSelected(selected);
        }
    }

    private void pickApps() {
        AppRepository repository = new AppRepository(this);
        executor.execute(() -> {
            List<AppInfo> apps = repository.getLaunchableApps(Collections.emptyList(), false);
            handler.post(() -> {
                if (isDestroyed()) {
                    return;
                }
                String[] labels = new String[apps.size()];
                boolean[] checked = new boolean[apps.size()];
                for (int i = 0; i < apps.size(); i++) {
                    labels[i] = apps.get(i).getAppName();
                    checked[i] = allowedApps.contains(apps.get(i).getPackageName());
                }
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.lock_in_allowed_apps)
                        .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                        .setPositiveButton(R.string.save, (dialog, which) -> {
                            allowedApps.clear();
                            for (int i = 0; i < checked.length; i++) {
                                if (checked[i]) {
                                    allowedApps.add(apps.get(i).getPackageName());
                                }
                            }
                            updateAppsSummary();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        });
    }

    private void updateAppsSummary() {
        TextView summary = findViewById(R.id.apps_summary);
        summary.setText(allowedApps.isEmpty()
                ? getString(R.string.lock_in_no_apps)
                : getString(R.string.lock_in_apps_change, describeApps(allowedApps)));
    }

    private String describeApps(Set<String> packages) {
        AppRepository repository = new AppRepository(this);
        List<String> names = new ArrayList<>();
        for (String pkg : packages) {
            AppInfo app = repository.getApp(pkg, false);
            if (app != null) {
                names.add(app.getAppName());
            }
        }
        if (names.isEmpty()) {
            return getString(R.string.lock_in_nothing);
        }
        return String.join(", ", names);
    }

    private void startSession() {
        String label = ((EditText) findViewById(R.id.label_input)).getText().toString();
        lockIn.start(selectedMinutes, label, allowedApps);
        showState();
    }

    // ---------------------------------------------------------------------
    // Ending early: press and hold
    // ---------------------------------------------------------------------

    private void setupHoldToEnd() {
        endButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    endButton.setText(R.string.lock_in_keep_holding);
                    handler.postDelayed(endAfterHold, HOLD_TO_END_MS);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(endAfterHold);
                    endButton.setText(R.string.lock_in_hold_to_end);
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.performClick();
                    }
                    return true;
                default:
                    return false;
            }
        });
        // Accessibility services can't hold; a confirmation dialog stands in for them
        endButton.setOnClickListener(v -> {
            AccessibilityManager accessibility = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
            if (accessibility != null && accessibility.isTouchExplorationEnabled()) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.lock_in_end_confirm)
                        .setPositiveButton(R.string.confirm, (d, w) -> endAfterHold.run())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    // ---------------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------------

    private void applyTheme() {
        ThemeStyler.applyScreen(this, theme, findViewById(R.id.lock_root), findViewById(R.id.toolbar));
        int text = theme.getTextColor();
        int secondary = theme.getSecondaryTextColor();

        ThemeStyler.applyBodyTypeface(findViewById(R.id.setup_container), theme);
        ThemeStyler.applyBodyTypeface(findViewById(R.id.session_container), theme);

        TextView headline = findViewById(R.id.setup_headline);
        headline.setTextColor(text);
        headline.setTypeface(theme.getClockTypeface());
        for (int id : new int[] { R.id.setup_caption, R.id.duration_label, R.id.apps_label, R.id.session_apps }) {
            ((TextView) findViewById(id)).setTextColor(secondary);
        }
        ((TextView) findViewById(R.id.session_label)).setTextColor(text);

        EditText input = findViewById(R.id.label_input);
        input.setTextColor(text);
        input.setHintTextColor(secondary);
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(theme.getAccentColor()));

        TextView apps = findViewById(R.id.apps_summary);
        ThemeStyler.card(apps, theme, 16);
        apps.setTextColor(text);

        ThemeStyler.accentButton(findViewById(R.id.start_button), theme);
        ThemeStyler.outlineButton(endButton, theme);
        styleChips();

        ring.setColors(theme.getSurfaceColor(), theme.getAccentColor(), text);
        ring.setLabelStyle(52, 10, theme.getClockTypeface());
    }
}
