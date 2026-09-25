package com.minimalist.launcher.ui.applist;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;
import com.minimalist.launcher.utils.AppLimitsManager;
import com.minimalist.launcher.utils.FavoritesHelper;
import com.minimalist.launcher.utils.FontScale;
import com.minimalist.launcher.utils.LaunchGate;
import com.minimalist.launcher.utils.PermissionHelper;
import com.minimalist.launcher.utils.Prefs;
import com.minimalist.launcher.utils.SwipeDetector;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;
import com.minimalist.launcher.utils.Transitions;

import java.util.ArrayList;
import java.util.List;

/**
 * Searchable list of all apps. Long-press an app for favorites, info, uninstall and hide.
 * Swipe left or press Back to return home.
 */
public class AppListActivity extends AppCompatActivity {

    private AppListViewModel viewModel;
    private AppAdapter adapter;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private TextView emptyStateText;
    private FavoritesHelper favoritesHelper;
    private SwipeDetector swipeDetector;
    private AppLimitsManager limitsManager;
    private BroadcastReceiver packageChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        viewModel = new ViewModelProvider(this).get(AppListViewModel.class);
        favoritesHelper = new FavoritesHelper(this);
        limitsManager = new AppLimitsManager(this);

        recyclerView = findViewById(R.id.apps_recycler_view);
        searchView = findViewById(R.id.search_view);
        emptyStateText = findViewById(R.id.empty_state_text);

        SystemBars.padForInsets(findViewById(R.id.app_list_root), true, false);
        SystemBars.padForInsets(recyclerView, false, true);

        adapter = new AppAdapter(this::onAppClick, this::onAppLongClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Enter opens the top result - fast type-to-launch
                List<AppInfo> apps = viewModel.getFilteredApps().getValue();
                if (apps != null && !apps.isEmpty()) {
                    onAppClick(apps.get(0));
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });

        viewModel.getFilteredApps().observe(this, apps -> {
            adapter.setApps(apps);
            emptyStateText.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getLaunchCounters().observe(this, viewModel::setLaunchCounters);

        setupSwipeGesture();
        registerPackageChangeReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        ThemeManager themeManager = new ThemeManager(this);
        adapter.setAppearance(Prefs.showIcons(this), themeManager.getTextColor(),
                themeManager.getSecondaryTextColor(), FontScale.appName(this), FontScale.secondary(this));
        TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTextSize(TypedValue.COMPLEX_UNIT_SP, FontScale.appName(this));
        }
        viewModel.loadApps();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (packageChangeReceiver != null) {
            unregisterReceiver(packageChangeReceiver);
        }
    }

    // ---------------------------------------------------------------------
    // Launching
    // ---------------------------------------------------------------------

    private void onAppClick(AppInfo app) {
        LaunchGate.launch(this, viewModel.getAppRepository(), app.getPackageName(), this::finish);
    }

    // ---------------------------------------------------------------------
    // Long-press menu
    // ---------------------------------------------------------------------

    private void onAppLongClick(AppInfo app) {
        final boolean isFavorite = favoritesHelper.isFavorite(app.getPackageName());
        final List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();

        if (isFavorite) {
            labels.add(getString(R.string.remove_from_favorites));
            actions.add(() -> {
                favoritesHelper.removeFavorite(app.getPackageName());
                Toast.makeText(this, R.string.removed_from_favorites, Toast.LENGTH_SHORT).show();
            });
        } else {
            labels.add(getString(R.string.add_to_favorites));
            actions.add(() -> addToFavorites(app));
        }
        labels.add(getString(R.string.daily_time_limit));
        actions.add(() -> showLimitDialog(app));
        labels.add(getString(R.string.app_info));
        actions.add(() -> openAppInfo(app));
        labels.add(getString(R.string.uninstall));
        actions.add(() -> uninstallApp(app));
        labels.add(getString(R.string.hide_app));
        actions.add(() -> hideApp(app));

        new MaterialAlertDialogBuilder(this)
                .setTitle(app.getAppName())
                .setItems(labels.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
                .show();
    }

    private void showLimitDialog(AppInfo app) {
        int[] options = AppLimitsManager.LIMIT_OPTIONS_MINUTES;
        String[] labels = new String[options.length];
        int current = limitsManager.getLimitMinutes(app.getPackageName());
        int checked = 0;
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i] == 0 ? getString(R.string.no_limit)
                    : ScreenTimeCalculator.format(options[i] * 60_000L);
            if (options[i] == current) {
                checked = i;
            }
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.daily_time_limit_for, app.getAppName()))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    limitsManager.setLimitMinutes(app.getPackageName(), options[which]);
                    viewModel.loadApps();
                    dialog.dismiss();
                    if (options[which] > 0 && !PermissionHelper.hasUsageStatsPermission(this)) {
                        Toast.makeText(this, R.string.limit_needs_usage_access, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void addToFavorites(AppInfo app) {
        if (favoritesHelper.addFavorite(app.getPackageName())) {
            Toast.makeText(this, getString(R.string.added_to_favorites, app.getAppName()),
                    Toast.LENGTH_SHORT).show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.favorites_full_title)
                    .setMessage(getString(R.string.favorites_full_message, favoritesHelper.getMaxFavorites()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void openAppInfo(AppInfo app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", app.getPackageName(), null));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.unable_to_open_app_info, Toast.LENGTH_SHORT).show();
        }
    }

    private void uninstallApp(AppInfo app) {
        if (app.isSystemApp()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.system_app_warning)
                    .setMessage(getString(R.string.system_app_message, app.getAppName()))
                    .setPositiveButton(R.string.app_info, (dialog, which) -> openAppInfo(app))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        // The system shows its own confirmation dialog
        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.getPackageName(), null));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.unable_to_uninstall, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideApp(AppInfo app) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hide_app)
                .setMessage(getString(R.string.hide_app_message, app.getAppName()))
                .setPositiveButton(R.string.hide, (dialog, which) -> {
                    viewModel.hideApp(app.getPackageName());
                    favoritesHelper.removeFavorite(app.getPackageName());
                    Toast.makeText(this, getString(R.string.app_hidden, app.getAppName()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------------------------------------------------------------------
    // Package changes, gestures, theme
    // ---------------------------------------------------------------------

    private void registerPackageChangeReceiver() {
        packageChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                viewModel.forceReloadApps();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        // System broadcasts are still delivered to non-exported receivers
        ContextCompat.registerReceiver(this, packageChangeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void setupSwipeGesture() {
        swipeDetector = new SwipeDetector(this, getWindow().getDecorView(), direction -> {
            // Close with the gesture that mirrors how the list was opened
            if (direction == SwipeDetector.Direction.LEFT) {
                Transitions.finish(this, Transitions.Slide.FROM_LEFT);
                return true;
            }
            if (direction == SwipeDetector.Direction.DOWN && !recyclerView.canScrollVertically(-1)) {
                Transitions.finish(this, Transitions.Slide.FROM_BOTTOM);
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (swipeDetector != null && swipeDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void applyTheme() {
        ThemeManager themeManager = new ThemeManager(this);
        int bgColor = themeManager.getBackgroundColor();
        int textColor = themeManager.getTextColor();
        int secondaryTextColor = themeManager.getSecondaryTextColor();

        SystemBars.apply(this, themeManager.isDarkTheme());
        getWindow().getDecorView().setBackgroundColor(bgColor);
        findViewById(R.id.app_list_root).setBackgroundColor(bgColor);
        emptyStateText.setTextColor(secondaryTextColor);

        searchView.setBackgroundColor(bgColor);
        TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (searchText != null) {
            searchText.setTextColor(textColor);
            searchText.setHintTextColor(secondaryTextColor);
        }
        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        if (searchIcon != null) {
            searchIcon.setColorFilter(textColor);
        }
        ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeIcon != null) {
            closeIcon.setColorFilter(textColor);
        }
    }
}
