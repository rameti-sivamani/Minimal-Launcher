package com.minimalist.launcher.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.StringRes;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minimalist.launcher.BuildConfig;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.ui.focus.FocusModeActivity;
import com.minimalist.launcher.ui.usage.UsageActivity;
import com.minimalist.launcher.utils.AppLimitsManager;
import com.minimalist.launcher.utils.FavoritesHelper;
import com.minimalist.launcher.utils.HiddenAppsManager;
import com.minimalist.launcher.utils.PermissionHelper;
import com.minimalist.launcher.utils.Prefs;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for appearance, usage awareness, apps and system integration.
 * Every change is saved immediately; other screens pick it up in onResume.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int[] THRESHOLD_OPTIONS = { 3, 5, 10, 15, 20, 30 };

    private SharedPreferences prefs;
    private ThemeManager themeManager;
    private HiddenAppsManager hiddenAppsManager;
    private AppLimitsManager limitsManager;
    private ActivityResultLauncher<Intent> defaultLauncherRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = Prefs.get(this);
        themeManager = new ThemeManager(this);
        hiddenAppsManager = new HiddenAppsManager(this);
        limitsManager = new AppLimitsManager(this);
        defaultLauncherRequest = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> refreshSummaries());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        SystemBars.padForInsets(findViewById(R.id.app_bar_layout), true, false);
        SystemBars.padForInsets(findViewById(R.id.settings_scroll_view), false, true);

        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        refreshSummaries();
    }

    private void setupClickListeners() {
        // Appearance
        onClick(R.id.theme_setting, this::showThemeDialog);
        onClick(R.id.font_size_setting, this::showFontSizeDialog);
        onClick(R.id.show_icons_setting, () -> toggle(Prefs.KEY_SHOW_ICONS, false));
        onClick(R.id.show_day_toggle, () -> toggle(Prefs.KEY_SHOW_DAY_OF_WEEK, true));
        onClick(R.id.quick_info_toggle, () -> toggle(Prefs.KEY_SHOW_QUICK_INFO, false));

        // Usage awareness
        onClick(R.id.show_screen_time_setting, () -> toggle(Prefs.KEY_SHOW_SCREEN_TIME, true));
        onClick(R.id.usage_report_setting,
                () -> startActivity(new Intent(this, UsageActivity.class)));
        onClick(R.id.usage_access_setting, this::showUsageAccessDialog);
        onClick(R.id.app_limits_setting, this::showAppLimitsDialog);
        onClick(R.id.warnings_setting, () -> toggle(Prefs.KEY_WARNINGS_ENABLED, true));
        onClick(R.id.threshold_setting, this::showThresholdDialog);
        onClick(R.id.focus_modes_setting,
                () -> startActivity(new Intent(this, FocusModeActivity.class)));

        // Apps
        onClick(R.id.app_sorting_setting, this::showSortingDialog);
        onClick(R.id.hidden_apps_setting, this::showHiddenAppsDialog);
        onClick(R.id.reset_favorites_setting, this::showResetFavoritesDialog);

        // System
        onClick(R.id.set_default_launcher, this::setAsDefaultLauncher);
        onClick(R.id.privacy_policy_setting, this::openPrivacyPolicy);
    }

    private void onClick(int viewId, Runnable action) {
        findViewById(viewId).setOnClickListener(v -> action.run());
    }

    private void toggle(String key, boolean defaultValue) {
        prefs.edit().putBoolean(key, !prefs.getBoolean(key, defaultValue)).apply();
        refreshSummaries();
    }

    // ---------------------------------------------------------------------
    // Summaries
    // ---------------------------------------------------------------------

    private void refreshSummaries() {
        setSummary(R.id.theme_summary, themeManager.getThemeName(themeManager.getCurrentTheme()));
        setSummary(R.id.font_size_summary, getResources().getStringArray(R.array.font_sizes)[Prefs.fontSize(this)]);
        setSummary(R.id.show_icons_summary, onOff(Prefs.showIcons(this)));
        setSummary(R.id.show_day_status, onOff(Prefs.showDayOfWeek(this)));
        setSummary(R.id.quick_info_status, onOff(Prefs.showQuickInfo(this)));

        setSummary(R.id.show_screen_time_summary, onOff(Prefs.showScreenTime(this)));
        setSummary(R.id.usage_access_summary, getString(PermissionHelper.hasUsageStatsPermission(this)
                ? R.string.permission_granted : R.string.permission_not_granted));
        setSummary(R.id.warnings_summary, onOff(Prefs.warningsEnabled(this)));
        setSummary(R.id.threshold_summary,
                getString(R.string.launch_warning_threshold_summary, Prefs.warningThreshold(this)));
        findViewById(R.id.threshold_setting).setEnabled(Prefs.warningsEnabled(this));
        findViewById(R.id.threshold_setting).setAlpha(Prefs.warningsEnabled(this) ? 1f : 0.5f);
        setSummary(R.id.focus_modes_summary, getString(R.string.focus_modes_summary));
        setSummary(R.id.usage_report_summary, getString(R.string.usage_report_summary));
        int limited = limitsManager.getLimitedAppCount();
        setSummary(R.id.app_limits_summary, limited == 0 ? getString(R.string.app_limits_none)
                : getResources().getQuantityString(R.plurals.apps_limited, limited, limited));

        setSummary(R.id.app_sorting_summary, getResources().getStringArray(R.array.sort_orders)[Prefs.sortOrder(this)]);
        int hidden = hiddenAppsManager.getHiddenCount();
        setSummary(R.id.hidden_apps_summary, getResources().getQuantityString(R.plurals.apps_hidden, hidden, hidden));
        setSummary(R.id.reset_favorites_summary, getString(R.string.reset_favorites_summary));

        setSummary(R.id.default_launcher_summary, getString(PermissionHelper.isDefaultLauncher(this)
                ? R.string.already_default : R.string.set_as_default_desc));
        setSummary(R.id.privacy_policy_summary, getString(R.string.privacy_policy_summary));
        setSummary(R.id.version_summary, BuildConfig.VERSION_NAME);
    }

    private void setSummary(int viewId, String text) {
        ((TextView) findViewById(viewId)).setText(text);
    }

    private String onOff(boolean value) {
        return getString(value ? R.string.on : R.string.off);
    }

    // ---------------------------------------------------------------------
    // Dialogs
    // ---------------------------------------------------------------------

    private void showSingleChoice(@StringRes int title, String[] options, int checked, OnChoice onChoice) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    onChoice.onChoice(which);
                    refreshSummaries();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private interface OnChoice {
        void onChoice(int which);
    }

    private void showThemeDialog() {
        showSingleChoice(R.string.select_theme, getResources().getStringArray(R.array.themes),
                themeManager.getCurrentTheme(), which -> {
                    themeManager.setTheme(which);
                    applyTheme();
                });
    }

    private void showFontSizeDialog() {
        showSingleChoice(R.string.select_font_size, getResources().getStringArray(R.array.font_sizes),
                Prefs.fontSize(this), which -> prefs.edit().putInt(Prefs.KEY_FONT_SIZE, which).apply());
    }

    private void showSortingDialog() {
        showSingleChoice(R.string.app_sorting, getResources().getStringArray(R.array.sort_orders),
                Prefs.sortOrder(this), which -> prefs.edit().putInt(Prefs.KEY_APP_SORT_ORDER, which).apply());
    }

    private void showThresholdDialog() {
        String[] labels = new String[THRESHOLD_OPTIONS.length];
        int checked = -1;
        int current = Prefs.warningThreshold(this);
        for (int i = 0; i < THRESHOLD_OPTIONS.length; i++) {
            labels[i] = getString(R.string.launch_warning_threshold_summary, THRESHOLD_OPTIONS[i]);
            if (THRESHOLD_OPTIONS[i] == current) {
                checked = i;
            }
        }
        showSingleChoice(R.string.launch_warning_threshold, labels, checked,
                which -> prefs.edit().putInt(Prefs.KEY_WARNING_THRESHOLD, THRESHOLD_OPTIONS[which]).apply());
    }

    private void showUsageAccessDialog() {
        boolean granted = PermissionHelper.hasUsageStatsPermission(this);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.usage_access_title)
                .setMessage(granted ? R.string.usage_access_revoke_info : R.string.usage_access_disclosure)
                .setPositiveButton(granted ? R.string.open_settings : R.string.grant_permission,
                        (dialog, which) -> PermissionHelper.requestUsageStatsPermission(this))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showHiddenAppsDialog() {
        PackageManager pm = getPackageManager();
        final List<String> appNames = new ArrayList<>();
        final List<String> packageNames = new ArrayList<>();

        for (String packageName : hiddenAppsManager.getHiddenApps()) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                appNames.add(pm.getApplicationLabel(appInfo).toString());
                packageNames.add(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // Uninstalled while hidden: forget it
                hiddenAppsManager.unhideApp(packageName);
            }
        }

        if (appNames.isEmpty()) {
            refreshSummaries();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.hidden_apps)
                    .setMessage(R.string.no_hidden_apps)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        boolean[] checked = new boolean[appNames.size()];
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.unhide_apps)
                .setMultiChoiceItems(appNames.toArray(new String[0]), checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.unhide_selected, (dialog, which) -> {
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            hiddenAppsManager.unhideApp(packageNames.get(i));
                        }
                    }
                    refreshSummaries();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAppLimitsDialog() {
        PackageManager pm = getPackageManager();
        final List<String> labels = new ArrayList<>();
        final List<String> packages = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : limitsManager.getAllLimits().entrySet()) {
            String label = entry.getKey();
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(entry.getKey(), 0)).toString();
            } catch (PackageManager.NameNotFoundException e) {
                // Uninstalled: drop its limit
                limitsManager.setLimitMinutes(entry.getKey(), 0);
                continue;
            }
            labels.add(label + " — " + ScreenTimeCalculator.format(entry.getValue() * 60_000L));
            packages.add(entry.getKey());
        }

        if (labels.isEmpty()) {
            refreshSummaries();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.app_time_limits)
                    .setMessage(R.string.app_limits_none)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        boolean[] checked = new boolean[labels.size()];
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_limits)
                .setMultiChoiceItems(labels.toArray(new String[0]), checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.remove_selected, (dialog, which) -> {
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            limitsManager.setLimitMinutes(packages.get(i), 0);
                        }
                    }
                    refreshSummaries();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showResetFavoritesDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_favorites)
                .setMessage(R.string.reset_favorites_confirm)
                .setPositiveButton(R.string.reset, (dialog, which) -> {
                    new FavoritesHelper(this).clearAll();
                    Toast.makeText(this, R.string.favorites_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setAsDefaultLauncher() {
        try {
            defaultLauncherRequest.launch(PermissionHelper.createDefaultLauncherIntent(this));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.set_default_manually, Toast.LENGTH_LONG).show();
        }
    }

    private void openPrivacyPolicy() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------------

    private void applyTheme() {
        int bgColor = themeManager.getBackgroundColor();
        int textColor = themeManager.getTextColor();
        int secondaryTextColor = themeManager.getSecondaryTextColor();

        SystemBars.apply(this, themeManager.isDarkTheme());
        getWindow().getDecorView().setBackgroundColor(bgColor);
        findViewById(R.id.settings_root).setBackgroundColor(bgColor);
        findViewById(R.id.app_bar_layout).setBackgroundColor(bgColor);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(bgColor);
        toolbar.setTitleTextColor(textColor);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(textColor);
        }

        tintTexts(findViewById(R.id.settings_content), textColor, secondaryTextColor);
    }

    /**
     * Color text by the tag set in the settings styles: title = primary, others = secondary
     */
    private void tintTexts(View view, int primary, int secondary) {
        if (view instanceof TextView) {
            ((TextView) view).setTextColor("title".equals(view.getTag()) ? primary : secondary);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintTexts(group.getChildAt(i), primary, secondary);
            }
        }
    }
}
