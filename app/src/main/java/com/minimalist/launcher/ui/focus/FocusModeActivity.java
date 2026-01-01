package com.minimalist.launcher.ui.focus;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minimalist.launcher.R;
import com.minimalist.launcher.data.database.entities.FocusMode;

/**
 * Activity for managing focus modes
 * Allows creating, editing, and activating focus modes
 */
public class FocusModeActivity extends AppCompatActivity {

    private FocusModeViewModel viewModel;
    private RecyclerView recyclerView;
    private FocusModeAdapter adapter;
    private FloatingActionButton createFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_mode);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(FocusModeViewModel.class);

        // Initialize views
        recyclerView = findViewById(R.id.focus_modes_recycler_view);
        createFab = findViewById(R.id.create_fab);

        // Set up RecyclerView
        setupRecyclerView();

        // Set up FAB
        createFab.setOnClickListener(v -> showCreateDialog());

        // Observe focus modes
        observeViewModel();

        // Enable fullscreen mode
        enableImmersiveMode();
    }

    /**
     * Enable immersive fullscreen mode
     */
    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Set up RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new FocusModeAdapter(
                this::onActivateClick,
                this::onDeleteClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Observe ViewModel LiveData
     */
    private void observeViewModel() {
        viewModel.getAllFocusModes().observe(this, focusModes -> {
            if (focusModes != null) {
                adapter.setFocusModes(focusModes);
            }
        });
    }

    /**
     * Show dialog to create new focus mode
     */
    private void showCreateDialog() {
        CharSequence[] options = {
                "Deep Work (Essential apps only)",
                "Custom Focus Mode"
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_focus_mode)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Create Deep Work mode
                        viewModel.createDeepWorkMode();
                    } else {
                        // Create custom mode (to be implemented)
                        // For now, create a simple example
                        FocusMode custom = new FocusMode("Custom Mode", "");
                        viewModel.createFocusMode(custom);
                    }
                })
                .show();
    }

    /**
     * Handle activate/deactivate click
     */
    private void onActivateClick(FocusMode focusMode) {
        if (focusMode.isActive()) {
            // Deactivate
            viewModel.deactivateAll();
        } else {
            // Activate this mode
            viewModel.activateFocusMode(focusMode.getId());
        }
    }

    /**
     * Handle delete click
     */
    private void onDeleteClick(FocusMode focusMode) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_focus_mode)
                .setMessage("Delete " + focusMode.getName() + "?")
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    viewModel.deleteFocusMode(focusMode);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
