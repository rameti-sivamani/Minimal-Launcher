package com.minimalist.launcher.ui.recap;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.minimalist.launcher.BuildConfig;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.repository.UsageRepository;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.data.wellbeing.WeeklyRecap;
import com.minimalist.launcher.data.wellbeing.WellbeingStore;
import com.minimalist.launcher.utils.LockInManager;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;
import com.minimalist.launcher.utils.ThemeStyler;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Weekly recap of the last 7 finished days, styled like the current theme, with a
 * "Share to story" button that exports the card as an image. Nothing leaves the phone
 * unless the user shares it.
 */
public class RecapActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private ThemeManager theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recap);
        theme = new ThemeManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        SystemBars.padForInsets(findViewById(R.id.recap_root), true, true);
        findViewById(R.id.share_button).setOnClickListener(v -> share());

        applyTheme();
        load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void load() {
        WellbeingStore store = new WellbeingStore(this);
        UsageRepository usage = new UsageRepository(this);
        LockInManager lockIn = new LockInManager(this);
        executor.execute(() -> {
            usage.backfillDailyTotals(store);
            List<Long> thisWeek = new ArrayList<>();
            List<Long> lastWeek = new ArrayList<>();
            int saidNo = 0;
            int focusMinutes = 0;
            for (int daysAgo = 7; daysAgo >= 1; daysAgo--) {
                thisWeek.add(store.getDailyTotal(daysAgo));
                saidNo += store.getSaidNo(daysAgo);
                focusMinutes += lockIn.getFocusMinutes(daysAgo);
            }
            for (int daysAgo = 14; daysAgo >= 8; daysAgo--) {
                lastWeek.add(store.getDailyTotal(daysAgo));
            }
            WeeklyRecap recap = WeeklyRecap.of(thisWeek, lastWeek);
            int streak = store.getPastStreak();
            final int no = saidNo;
            final int focus = focusMinutes;
            main.post(() -> {
                if (!isDestroyed()) {
                    render(recap, thisWeek, streak, no, focus);
                }
            });
        });
    }

    private void render(WeeklyRecap recap, List<Long> week, int streak, int saidNo, int focusMinutes) {
        SimpleDateFormat dayMonth = new SimpleDateFormat("d MMM", Locale.getDefault());
        ((TextView) findViewById(R.id.recap_range)).setText(getString(R.string.recap_range,
                dayMonth.format(new Date(UsageRepository.startOfDay(7))),
                dayMonth.format(new Date(UsageRepository.startOfDay(1)))));

        TextView kicker = findViewById(R.id.recap_kicker);
        TextView big = findViewById(R.id.recap_big);
        TextView sentence = findViewById(R.id.recap_sentence);
        if (recap.thisWeekDays == 0) {
            kicker.setText(R.string.recap_no_data_kicker);
            big.setText("—");
            sentence.setText(R.string.recap_no_data);
        } else if (recap.comparable && recap.savedVsLastWeek >= 60_000) {
            kicker.setText(R.string.recap_took_back);
            big.setText(ScreenTimeCalculator.format(recap.savedVsLastWeek));
            sentence.setText(R.string.recap_vs_last_week);
        } else if (recap.comparable && recap.savedVsLastWeek <= -60_000) {
            kicker.setText(R.string.recap_this_week);
            big.setText(ScreenTimeCalculator.format(recap.thisWeekTotal));
            sentence.setText(getString(R.string.recap_more_than_last,
                    ScreenTimeCalculator.format(-recap.savedVsLastWeek)));
        } else {
            kicker.setText(R.string.recap_this_week);
            big.setText(ScreenTimeCalculator.format(recap.thisWeekTotal));
            sentence.setText(getResources().getQuantityString(R.plurals.recap_days_tracked,
                    recap.thisWeekDays, recap.thisWeekDays));
        }

        ((TextView) findViewById(R.id.tile_streak_value)).setText(String.valueOf(streak));
        ((TextView) findViewById(R.id.tile_no_value)).setText(String.valueOf(saidNo));
        ((TextView) findViewById(R.id.tile_focus_value)).setText(ScreenTimeCalculator.format(focusMinutes * 60_000L));
        ((TextView) findViewById(R.id.tile_avg_value)).setText(ScreenTimeCalculator.format(recap.dailyAverage));

        drawBars(week);
    }

    /** Seven bars (oldest to newest) drawn on the accent card in the card's text colour */
    private void drawBars(List<Long> week) {
        LinearLayout bars = findViewById(R.id.recap_bars);
        bars.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        long max = 1;
        for (Long value : week) {
            if (value != null) {
                max = Math.max(max, value);
            }
        }
        int barColor = theme.getOnAccentColor();
        SimpleDateFormat dayLetter = new SimpleDateFormat("EEEEE", Locale.getDefault());
        int maxBarPx = Math.round(110 * density);
        for (int i = 0; i < week.size(); i++) {
            int daysAgo = week.size() - i;
            Long value = week.get(i);

            LinearLayout column = new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            columnParams.setMarginStart(Math.round(4 * density));
            columnParams.setMarginEnd(Math.round(4 * density));

            View bar = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(barColor);
            shape.setCornerRadius(8 * density);
            bar.setBackground(shape);
            bar.setAlpha(value == null ? 0.25f : 1f);
            int height = value == null ? Math.round(6 * density)
                    : Math.max(Math.round(6 * density), Math.round(maxBarPx * (float) value / max));
            column.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));

            TextView label = new TextView(this);
            label.setText(dayLetter.format(new Date(UsageRepository.startOfDay(daysAgo))));
            label.setTextColor(barColor);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(theme.getBodyTypeface());
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = Math.round(6 * density);
            column.addView(label, labelParams);

            column.setContentDescription(label.getText() + ": "
                    + (value == null ? getString(R.string.recap_no_day_data) : ScreenTimeCalculator.format(value)));
            bars.addView(column, columnParams);
        }
    }

    // ---------------------------------------------------------------------
    // Share
    // ---------------------------------------------------------------------

    private void share() {
        View area = findViewById(R.id.share_area);
        if (area.getWidth() == 0 || area.getHeight() == 0) {
            return;
        }
        Bitmap bitmap = Bitmap.createBitmap(area.getWidth(), area.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(theme.getBackgroundColor());
        area.draw(canvas);

        executor.execute(() -> {
            try {
                File dir = new File(getCacheDir(), "shared");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File file = new File(dir, "weekly-recap.png");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
                main.post(() -> {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("image/png");
                    send.putExtra(Intent.EXTRA_STREAM, uri);
                    send.putExtra(Intent.EXTRA_TEXT, getString(R.string.recap_share_text));
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(Intent.createChooser(send, getString(R.string.recap_share)));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, R.string.recap_share_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                main.post(() -> Toast.makeText(this, R.string.recap_share_failed, Toast.LENGTH_SHORT).show());
            } finally {
                bitmap.recycle();
            }
        });
    }

    // ---------------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------------

    private void applyTheme() {
        ThemeStyler.applyScreen(this, theme, findViewById(R.id.recap_root), findViewById(R.id.toolbar));
        ThemeStyler.applyBodyTypeface(findViewById(R.id.share_area), theme);
        float density = getResources().getDisplayMetrics().density;
        int text = theme.getTextColor();
        int secondary = theme.getSecondaryTextColor();
        int onAccent = theme.getOnAccentColor();

        findViewById(R.id.recap_card).setBackground(ThemeStyler.rounded(theme.getAccentColor(), 28 * density));
        for (int id : new int[] { R.id.recap_kicker, R.id.recap_big, R.id.recap_sentence }) {
            ((TextView) findViewById(id)).setTextColor(onAccent);
        }
        TextView big = findViewById(R.id.recap_big);
        big.setTypeface(theme.getClockTypeface(), android.graphics.Typeface.BOLD);

        for (int id : new int[] { R.id.tile_streak, R.id.tile_no, R.id.tile_focus, R.id.tile_avg }) {
            ThemeStyler.card(findViewById(id), theme, 20);
        }
        for (int id : new int[] { R.id.tile_streak_value, R.id.tile_no_value, R.id.tile_focus_value, R.id.tile_avg_value }) {
            TextView value = findViewById(id);
            value.setTextColor(text);
            value.setTypeface(theme.getClockTypeface(), android.graphics.Typeface.BOLD);
        }
        for (int id : new int[] { R.id.tile_streak_label, R.id.tile_no_label, R.id.tile_focus_label,
                R.id.tile_avg_label, R.id.recap_range, R.id.recap_brand }) {
            ((TextView) findViewById(id)).setTextColor(secondary);
        }
        ThemeStyler.accentButton(findViewById(R.id.share_button), theme);
    }
}
