package com.minimalist.launcher.ui.usage;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.repository.UsageRepository;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.ui.recap.RecapActivity;
import com.minimalist.launcher.utils.AppLimitsManager;
import com.minimalist.launcher.utils.PermissionHelper;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;
import com.minimalist.launcher.utils.ThemeStyler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Screen time report: today's total, the last 7 days, and today's time per app.
 * Everything is computed on the device from Android's usage access.
 */
public class UsageActivity extends AppCompatActivity {

    private static final int DAYS = 7;
    private static final int MAX_APPS = 15;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ThemeManager themeManager;
    private UsageRepository usageRepository;
    private AppRepository appRepository;
    private AppLimitsManager limitsManager;

    /** Row data prepared off the main thread */
    private static final class AppUsageRow {
        final String label;
        final long millis;
        final int limitMinutes;

        AppUsageRow(String label, long millis, int limitMinutes) {
            this.label = label;
            this.millis = millis;
            this.limitMinutes = limitMinutes;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage);

        themeManager = new ThemeManager(this);
        usageRepository = new UsageRepository(this);
        appRepository = new AppRepository(this);
        limitsManager = new AppLimitsManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.usage_recap_button).setOnClickListener(
                v -> startActivity(new android.content.Intent(this, RecapActivity.class)));
        findViewById(R.id.usage_grant_button).setOnClickListener(
                v -> PermissionHelper.requestUsageStatsPermission(this));

        SystemBars.padForInsets(toolbar, true, false);
        SystemBars.padForInsets(findViewById(R.id.usage_scroll), false, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void load() {
        boolean granted = PermissionHelper.hasUsageStatsPermission(this);
        findViewById(R.id.usage_permission_text).setVisibility(granted ? View.GONE : View.VISIBLE);
        findViewById(R.id.usage_grant_button).setVisibility(granted ? View.GONE : View.VISIBLE);
        int dataVisibility = granted ? View.VISIBLE : View.GONE;
        for (int id : new int[] { R.id.usage_today_total, R.id.usage_today_caption, R.id.usage_week_header, R.id.usage_recap_button,
                R.id.usage_week_container, R.id.usage_week_average, R.id.usage_apps_header,
                R.id.usage_apps_container }) {
            findViewById(id).setVisibility(dataVisibility);
        }
        if (!granted) {
            return;
        }

        executor.execute(() -> {
            long[] week = usageRepository.getDailyTotals(DAYS);
            Map<String, Long> today = usageRepository.getTodayUsagePerApp();

            List<AppUsageRow> rows = new ArrayList<>();
            if (today != null) {
                List<Map.Entry<String, Long>> entries = new ArrayList<>(today.entrySet());
                Collections.sort(entries, (a, b) -> Long.compare(b.getValue(), a.getValue()));
                for (Map.Entry<String, Long> entry : entries) {
                    if (rows.size() >= MAX_APPS || entry.getValue() < 60_000) {
                        break;
                    }
                    AppInfo app = appRepository.getApp(entry.getKey(), false);
                    if (app != null) {
                        rows.add(new AppUsageRow(app.getAppName(), entry.getValue(),
                                limitsManager.getLimitMinutes(entry.getKey())));
                    }
                }
            }
            main.post(() -> {
                if (!isDestroyed()) {
                    render(week, rows);
                }
            });
        });
    }

    private void render(long[] week, List<AppUsageRow> rows) {
        int text = themeManager.getTextColor();
        int secondary = themeManager.getSecondaryTextColor();

        long todayTotal = week != null ? week[week.length - 1] : 0;
        ((TextView) findViewById(R.id.usage_today_total)).setText(ScreenTimeCalculator.format(todayTotal));

        // Last 7 days
        LinearLayout weekContainer = findViewById(R.id.usage_week_container);
        weekContainer.removeAllViews();
        long max = 1;
        long sum = 0;
        if (week != null) {
            for (long value : week) {
                max = Math.max(max, value);
                sum += value;
            }
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            for (int i = 0; i < week.length; i++) {
                int daysAgo = week.length - 1 - i;
                String label = daysAgo == 0 ? getString(R.string.today)
                        : dayFormat.format(new Date(UsageRepository.startOfDay(daysAgo)));
                weekContainer.addView(barRow(label, ScreenTimeCalculator.format(week[i]),
                        week[i], max, daysAgo == 0 ? text : secondary,
                        daysAgo == 0 ? themeManager.getAccentColor() : secondary));
            }
        }
        ((TextView) findViewById(R.id.usage_week_average)).setText(getString(R.string.usage_daily_average,
                ScreenTimeCalculator.format(week != null ? sum / week.length : 0)));
        ((TextView) findViewById(R.id.usage_week_average)).setTextColor(secondary);

        // Today by app
        LinearLayout appsContainer = findViewById(R.id.usage_apps_container);
        appsContainer.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.usage_no_apps_yet);
            empty.setTextColor(secondary);
            appsContainer.addView(empty);
        }
        long appMax = rows.isEmpty() ? 1 : Math.max(1, rows.get(0).millis);
        for (AppUsageRow row : rows) {
            String value = ScreenTimeCalculator.format(row.millis);
            if (row.limitMinutes > 0) {
                value = getString(R.string.usage_of_limit, value,
                        ScreenTimeCalculator.format(row.limitMinutes * 60_000L));
            }
            appsContainer.addView(barRow(row.label, value, row.millis, appMax, text, themeManager.getAccentColor()));
        }
    }

    /**
     * One labelled horizontal bar: [label] [=====      ] [value]
     */
    private View barRow(String label, String value, long amount, long max, int labelColor, int barColor) {
        int padding = dp(6);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, padding, 0, padding);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(labelColor);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        labelView.setTypeface(themeManager.getBodyTypeface());
        labelView.setMaxLines(1);
        labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(labelView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f));

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(0, dp(8), 5f);
        trackParams.setMarginStart(dp(8));
        trackParams.setMarginEnd(dp(8));
        row.addView(track, trackParams);

        float fraction = max > 0 ? Math.min(1f, (float) amount / max) : 0f;
        View bar = new View(this);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(4));
        shape.setColor(barColor);
        bar.setBackground(shape);
        bar.setAlpha(0.8f);
        track.addView(bar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(fraction, amount > 0 ? 0.02f : 0f)));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                1f - Math.max(fraction, amount > 0 ? 0.02f : 0f)));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(themeManager.getSecondaryTextColor());
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        valueView.setTypeface(themeManager.getBodyTypeface());
        valueView.setGravity(Gravity.END);
        row.addView(valueView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        return row;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void applyTheme() {
        int text = themeManager.getTextColor();
        int secondary = themeManager.getSecondaryTextColor();
        ThemeStyler.applyScreen(this, themeManager, findViewById(R.id.usage_root), findViewById(R.id.toolbar));
        ThemeStyler.applyBodyTypeface(findViewById(R.id.usage_content), themeManager);
        ((TextView) findViewById(R.id.usage_today_total)).setTypeface(themeManager.getClockTypeface());
        ThemeStyler.accentButton(findViewById(R.id.usage_recap_button), themeManager);
        ThemeStyler.outlineButton(findViewById(R.id.usage_grant_button), themeManager);
        ((TextView) findViewById(R.id.usage_today_total)).setTextColor(text);
        ((TextView) findViewById(R.id.usage_today_caption)).setTextColor(secondary);
        ((TextView) findViewById(R.id.usage_permission_text)).setTextColor(text);
        ((TextView) findViewById(R.id.usage_week_header)).setTextColor(secondary);
        ((TextView) findViewById(R.id.usage_apps_header)).setTextColor(secondary);
    }
}
