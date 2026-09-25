package com.minimalist.launcher.ui.pause;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.minimalist.launcher.R;
import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.wellbeing.WellbeingStore;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;

/**
 * A short breathing pause before a distracting app opens.
 * "I don't need it" is the main action; opening unlocks after the countdown.
 */
public class PauseActivity extends AppCompatActivity {

    private static final String EXTRA_PACKAGE = "package";
    private static final String EXTRA_APP_NAME = "app_name";
    private static final String EXTRA_MESSAGE = "message";
    private static final int COUNTDOWN_SECONDS = 4;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ValueAnimator breathing;
    private int secondsLeft = COUNTDOWN_SECONDS;
    private boolean canOpen = false;

    private TextView circle;
    private TextView headline;
    private TextView openButton;
    private String appName;

    public static void start(Context context, String packageName, String appName, String message) {
        Intent intent = new Intent(context, PauseActivity.class);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_APP_NAME, appName);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pause);

        final String packageName = getIntent().getStringExtra(EXTRA_PACKAGE);
        appName = getIntent().getStringExtra(EXTRA_APP_NAME);
        if (packageName == null) {
            finish();
            return;
        }

        circle = findViewById(R.id.pause_circle);
        headline = findViewById(R.id.pause_headline);
        openButton = findViewById(R.id.pause_open);
        ((TextView) findViewById(R.id.pause_opening)).setText(getString(R.string.pause_opening, appName));
        ((TextView) findViewById(R.id.pause_message)).setText(getIntent().getStringExtra(EXTRA_MESSAGE));

        WellbeingStore store = new WellbeingStore(this);
        ((TextView) findViewById(R.id.pause_footer)).setText(R.string.pause_footer);

        findViewById(R.id.pause_close).setOnClickListener(v -> {
            store.recordSaidNo();
            Toast.makeText(this, R.string.pause_good_choice, Toast.LENGTH_SHORT).show();
            finish();
        });
        openButton.setOnClickListener(v -> {
            if (!canOpen) {
                return;
            }
            if (!new AppRepository(this).launchApp(packageName)) {
                Toast.makeText(this, R.string.unable_to_open_app, Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        SystemBars.padForInsets(findViewById(R.id.pause_root), true, true);
        applyTheme();
        startCountdown();
    }

    private void startCountdown() {
        breathing = ValueAnimator.ofFloat(0.82f, 1.08f);
        breathing.setDuration(COUNTDOWN_SECONDS * 1000L / 2);
        breathing.setRepeatMode(ValueAnimator.REVERSE);
        breathing.setRepeatCount(ValueAnimator.INFINITE);
        breathing.setInterpolator(new AccelerateDecelerateInterpolator());
        breathing.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            circle.setScaleX(scale);
            circle.setScaleY(scale);
        });
        breathing.start();
        tick();
    }

    private void tick() {
        if (secondsLeft > 0) {
            circle.setText(String.valueOf(secondsLeft));
            headline.setText(secondsLeft > COUNTDOWN_SECONDS / 2 ? R.string.pause_breathe_in : R.string.pause_breathe_out);
            openButton.setText(getString(R.string.pause_open_in, secondsLeft));
            openButton.setAlpha(0.45f);
            secondsLeft--;
            handler.postDelayed(this::tick, 1000);
        } else {
            canOpen = true;
            circle.setText("");
            headline.setText(R.string.pause_still_want);
            openButton.setText(getString(R.string.pause_open, appName));
            openButton.setAlpha(1f);
        }
    }

    private void applyTheme() {
        ThemeManager theme = new ThemeManager(this);
        int bg = theme.getBackgroundColor();
        int text = theme.getTextColor();
        int secondary = theme.getSecondaryTextColor();
        int accent = theme.getAccentColor();
        float density = getResources().getDisplayMetrics().density;

        SystemBars.apply(this, theme.isDarkTheme());
        getWindow().getDecorView().setBackgroundColor(bg);
        findViewById(R.id.pause_root).setBackgroundColor(bg);

        ((TextView) findViewById(R.id.pause_opening)).setTextColor(secondary);
        headline.setTextColor(text);
        headline.setTypeface(theme.getClockTypeface());
        ((TextView) findViewById(R.id.pause_message)).setTextColor(secondary);
        ((TextView) findViewById(R.id.pause_footer)).setTextColor(secondary);

        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setStroke(Math.round(2 * density), accent);
        circle.setBackground(ring);
        circle.setTextColor(text);
        circle.setTypeface(theme.getClockTypeface());

        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        glow.setGradientRadius(120 * density);
        glow.setColors(new int[] { (accent & 0x00FFFFFF) | 0x40000000, (accent & 0x00FFFFFF) });
        findViewById(R.id.pause_glow).setBackground(glow);

        TextView close = findViewById(R.id.pause_close);
        close.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000), pill(accent, 0, density), null));
        close.setTextColor(theme.getOnAccentColor());

        openButton.setBackground(new RippleDrawable(ColorStateList.valueOf(secondary & 0x33FFFFFF),
                pill(bg, secondary, density), null));
        openButton.setTextColor(text);
    }

    private static GradientDrawable pill(int fill, int stroke, float density) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(28 * density);
        if (stroke != 0) {
            shape.setStroke(Math.round(density), stroke);
        }
        return shape;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (breathing != null) {
            breathing.cancel();
        }
    }
}
