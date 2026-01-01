package com.minimalist.launcher.ui.launcher;

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
 * Adapter for displaying favorite apps on the home screen
 */
public class FavoriteAppsAdapter extends RecyclerView.Adapter<FavoriteAppsAdapter.ViewHolder> {

    private List<AppInfo> favoriteApps = new ArrayList<>();
    private final OnAppClickListener clickListener;
    private boolean showIcons = false;

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);

        void onAppLongClick(AppInfo app); // For removing from favorites
    }

    public FavoriteAppsAdapter(OnAppClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setFavoriteApps(List<AppInfo> apps) {
        this.favoriteApps = apps != null ? apps : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setShowIcons(boolean show) {
        this.showIcons = show;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = favoriteApps.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return favoriteApps.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView appName;
        private final ImageView appIcon;

        ViewHolder(View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.fav_app_name);
            appIcon = itemView.findViewById(R.id.fav_app_icon);
        }

        void bind(AppInfo app) {
            appName.setText(app.getAppName());

            // Show/hide icon based on settings
            if (showIcons && app.getIcon() != null) {
                appIcon.setImageDrawable(app.getIcon());
                appIcon.setVisibility(View.VISIBLE);
            } else {
                appIcon.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAppClick(app);
                }
            });

            // Long-press to remove from favorites
            itemView.setOnLongClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAppLongClick(app);
                    return true;
                }
                return false;
            });
        }
    }
}
