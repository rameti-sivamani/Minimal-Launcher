package com.minimalist.launcher.ui.focus;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.focus.FocusSchedule;
import com.minimalist.launcher.data.model.AppInfo;
import com.minimalist.launcher.utils.AppFilterHelper;
import com.minimalist.launcher.utils.SystemBars;
import com.minimalist.launcher.utils.ThemeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Create, edit, activate and delete focus modes.
 * While a mode is active, the app list only shows that mode's allowed apps.
 */
public class FocusModeActivity extends AppCompatActivity {

    private FocusModeViewModel viewModel;
    private FocusModeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_mode);

        viewModel = new ViewModelProvider(this).get(FocusModeViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.focus_modes_recycler_view);
        adapter = new FocusModeAdapter(this::onActivateClick, this::onDeleteClick, this::showEditor);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        FloatingActionButton createFab = findViewById(R.id.create_fab);
        createFab.setOnClickListener(v -> showCreateDialog());

        SystemBars.padForInsets(findViewById(R.id.focus_app_bar), true, false);
        SystemBars.padForInsets(recyclerView, false, true);
        SystemBars.padForInsets(createFab, false, true);

        viewModel.getAllFocusModes().observe(this, adapter::setFocusModes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeManager themeManager = new ThemeManager(this);
        SystemBars.apply(this, themeManager.isDarkTheme());
        int bg = themeManager.getBackgroundColor();
        getWindow().getDecorView().setBackgroundColor(bg);
        findViewById(R.id.focus_mode_root).setBackgroundColor(bg);
        findViewById(R.id.focus_app_bar).setBackgroundColor(bg);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(themeManager.getTextColor());
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(themeManager.getTextColor());
        }
    }

    private void showCreateDialog() {
        CharSequence[] options = {
                getString(R.string.deep_work_option),
                getString(R.string.custom_focus_mode_option)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.create_focus_mode)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.createDeepWorkMode();
                    } else {
                        showEditor(null);
                    }
                })
                .show();
    }

    /**
     * Name + allowed-apps editor. {@code existing == null} creates a new mode.
     */
    private void showEditor(FocusMode existing) {
        EditText nameInput = new EditText(this);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setHint(R.string.focus_mode_name_hint);
        nameInput.setSingleLine(true);
        if (existing != null) {
            nameInput.setText(existing.getName());
            nameInput.setSelection(nameInput.getText().length());
        }

        FrameLayout container = new FrameLayout(this);
        int padding = getResources().getDimensionPixelSize(R.dimen.screen_padding_horizontal);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(nameInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.create_focus_mode : R.string.edit_focus_mode)
                .setView(container)
                .setPositiveButton(R.string.select_apps, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.focus_mode_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.loadSelectableApps(apps -> showAppPicker(existing, name, apps));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAppPicker(FocusMode existing, String name, List<AppInfo> apps) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Set<String> selected = AppFilterHelper.stringToPackageSet(
                existing != null ? existing.getAllowedApps() : "");

        String[] labels = new String[apps.size()];
        boolean[] checked = new boolean[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i).getAppName();
            checked[i] = selected.contains(apps.get(i).getPackageName());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.allowed_apps_for, name))
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    List<String> allowed = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            allowed.add(apps.get(i).getPackageName());
                        }
                    }
                    if (allowed.isEmpty()) {
                        Toast.makeText(this, R.string.focus_mode_needs_apps, Toast.LENGTH_LONG).show();
                        return;
                    }
                    FocusMode mode = existing != null ? existing : new FocusMode(name, "");
                    mode.setName(name);
                    mode.setAllowedApps(String.join(",", allowed));
                    showScheduleChoice(mode, existing == null);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ---------------------------------------------------------------------
    // Schedule: optional start time, end time and days of the week
    // ---------------------------------------------------------------------

    private void showScheduleChoice(FocusMode mode, boolean isNew) {
        String[] options = {
                getString(R.string.schedule_none),
                getString(R.string.schedule_set)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.schedule_title)
                .setCancelable(false)
                .setSingleChoiceItems(options, mode.isHasSchedule() ? 1 : 0, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) {
                        mode.setHasSchedule(false);
                        save(mode, isNew);
                    } else {
                        pickStartTime(mode, isNew);
                    }
                })
                .show();
    }

    private void pickStartTime(FocusMode mode, boolean isNew) {
        int start = FocusSchedule.parseMinutes(mode.getStartTime());
        if (start < 0) {
            start = 9 * 60;
        }
        showTimePicker(R.string.schedule_start, start, minutes -> {
            mode.setStartTime(FocusSchedule.formatMinutes(minutes));
            pickEndTime(mode, isNew);
        });
    }

    private void pickEndTime(FocusMode mode, boolean isNew) {
        int end = FocusSchedule.parseMinutes(mode.getEndTime());
        if (end < 0) {
            end = 17 * 60;
        }
        showTimePicker(R.string.schedule_end, end, minutes -> {
            if (FocusSchedule.formatMinutes(minutes).equals(mode.getStartTime())) {
                Toast.makeText(this, R.string.schedule_same_times, Toast.LENGTH_LONG).show();
                pickEndTime(mode, isNew);
                return;
            }
            mode.setEndTime(FocusSchedule.formatMinutes(minutes));
            pickDays(mode, isNew);
        });
    }

    private void pickDays(FocusMode mode, boolean isNew) {
        String[] dayNames = getResources().getStringArray(R.array.weekdays); // Monday first
        Set<Integer> current = FocusSchedule.parseDays(mode.getActiveDays());
        if (current.isEmpty()) {
            current = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        }
        boolean[] checked = new boolean[7];
        for (int i = 0; i < 7; i++) {
            checked[i] = current.contains(i + 1);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.schedule_days)
                .setCancelable(false)
                .setMultiChoiceItems(dayNames, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    List<Integer> days = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        if (checked[i]) {
                            days.add(i + 1);
                        }
                    }
                    if (days.isEmpty()) {
                        Toast.makeText(this, R.string.schedule_needs_days, Toast.LENGTH_LONG).show();
                        pickDays(mode, isNew);
                        return;
                    }
                    mode.setActiveDays(FocusSchedule.formatDays(days));
                    mode.setHasSchedule(true);
                    save(mode, isNew);
                })
                .show();
    }

    private interface OnTimePicked {
        void onPicked(int minutesOfDay);
    }

    private void showTimePicker(int titleRes, int minutesOfDay, OnTimePicked onPicked) {
        TimePickerDialog dialog = new TimePickerDialog(this,
                (view, hour, minute) -> onPicked.onPicked(hour * 60 + minute),
                minutesOfDay / 60, minutesOfDay % 60,
                android.text.format.DateFormat.is24HourFormat(this));
        dialog.setTitle(titleRes);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void save(FocusMode mode, boolean isNew) {
        if (isNew) {
            viewModel.createFocusMode(mode);
        } else {
            viewModel.updateFocusMode(mode);
        }
    }

    private void onActivateClick(FocusMode focusMode) {
        if (focusMode.isActive()) {
            viewModel.deactivateAll();
        } else {
            viewModel.activateFocusMode(focusMode.getId());
        }
    }

    private void onDeleteClick(FocusMode focusMode) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_focus_mode)
                .setMessage(getString(R.string.delete_focus_mode_confirm, focusMode.getName()))
                .setPositiveButton(R.string.confirm, (dialog, which) -> viewModel.deleteFocusMode(focusMode))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
