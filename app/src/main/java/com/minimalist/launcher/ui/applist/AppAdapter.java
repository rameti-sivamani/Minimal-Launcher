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
import com.minimalist.launcher.data.usage.ScreenTimeCalculator;

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
    private int textColor = 0xFFE8E8E8;
    private int secondaryTextColor = 0xFF9B9B9B;
    private float nameSizeSp = 19f;
    private float badgeSizeSp = 14f;
    private android.graphics.Typeface typeface = android.graphics.Typeface.DEFAULT;
    private boolean lowercase = false;

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
     * Update icon visibility and theme colors in one pass
     */
    public void setAppearance(boolean showIcons, int textColor, int secondaryTextColor,
            float nameSizeSp, float badgeSizeSp, android.graphics.Typeface typeface, boolean lowercase) {
        this.typeface = typeface;
        this.lowercase = lowercase;
        this.showIcons = showIcons;
        this.textColor = textColor;
        this.secondaryTextColor = secondaryTextColor;
        this.nameSizeSp = nameSizeSp;
        this.badgeSizeSp = badgeSizeSp;
        notifyDataSetChanged();
    }

    /**
     * e.g. "25m / 30m · 3×", "1h 5m", "2×"; empty when there is nothing to show
     */
    static String buildBadge(AppInfo app) {
        StringBuilder badge = new StringBuilder();
        if (app.getUsageMillis() >= 60_000 || app.getLimitMinutes() > 0) {
            badge.append(ScreenTimeCalculator.format(app.getUsageMillis()));
            if (app.getLimitMinutes() > 0) {
                badge.append(" / ").append(ScreenTimeCalculator.format(app.getLimitMinutes() * 60_000L));
            }
        }
        if (app.getLaunchCount() > 0) {
            if (badge.length() > 0) {
                badge.append(" · ");
            }
            badge.append(app.getLaunchCount()).append('×');
        }
        return badge.toString();
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
            appName.setText(lowercase ? app.getAppName().toLowerCase(java.util.Locale.getDefault()) : app.getAppName());
            appName.setTypeface(typeface);
            launchCounter.setTypeface(typeface);
            appName.setTextColor(textColor);
            appName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, nameSizeSp);
            launchCounter.setTextColor(secondaryTextColor);
            launchCounter.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, badgeSizeSp);

            // Show/hide icon based on settings
            if (showIcons && app.getIcon() != null) {
                appIcon.setImageDrawable(app.getIcon());
                appIcon.setVisibility(View.VISIBLE);
            } else {
                appIcon.setImageDrawable(null);
                appIcon.setVisibility(View.GONE);
            }

            // Badge: today's time (and limit), launch count
            String badge = buildBadge(app);
            if (badge.isEmpty()) {
                launchCounter.setVisibility(View.GONE);
            } else {
                launchCounter.setText(badge);
                launchCounter.setVisibility(View.VISIBLE);
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
