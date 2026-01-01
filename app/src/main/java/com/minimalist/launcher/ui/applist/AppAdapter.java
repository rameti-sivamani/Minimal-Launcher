package com.minimalist.launcher.ui.applist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minimalist.launcher.R;
import com.minimalist.launcher.data.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying apps in RecyclerView
 * Shows app name (text-based) with optional icon and launch counter
 */
public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private List<AppInfo> apps = new ArrayList<>();
    private final OnAppClickListener clickListener;
    private final OnAppLongClickListener longClickListener;
    private boolean showIcons = false;

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    public interface OnAppLongClickListener {
        void onAppLongClick(AppInfo app);
    }

    public AppAdapter(OnAppClickListener clickListener, OnAppLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    /**
     * Update app list
     */
    public void setApps(List<AppInfo> apps) {
        this.apps = apps != null ? apps : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Toggle app icon visibility
     */
    public void setShowIcons(boolean showIcons) {
        this.showIcons = showIcons;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for app items
     */
    class AppViewHolder extends RecyclerView.ViewHolder {

        private final TextView appName;
        private final ImageView appIcon;
        private final TextView launchCounter;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            appIcon = itemView.findViewById(R.id.app_icon);
            launchCounter = itemView.findViewById(R.id.launch_counter);
        }

        public void bind(AppInfo app) {
            appName.setText(app.getAppName());

            // Show/hide icon based on settings
            if (showIcons) {
                appIcon.setImageDrawable(app.getIcon());
                appIcon.setVisibility(View.VISIBLE);
            } else {
                appIcon.setVisibility(View.GONE);
            }

            // Show launch counter if > 0
            if (app.getLaunchCount() > 0) {
                launchCounter.setText(String.valueOf(app.getLaunchCount()));
                launchCounter.setVisibility(View.VISIBLE);
            } else {
                launchCounter.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAppClick(app);
                }
            });

            // Long-press listener for adding to favorites
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onAppLongClick(app);
                    return true;
                }
                return false;
            });
        }
    }
}
