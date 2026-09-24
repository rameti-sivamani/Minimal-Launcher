package com.minimalist.launcher.ui.focus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.minimalist.launcher.R;
import com.minimalist.launcher.data.database.entities.FocusMode;
import com.minimalist.launcher.data.focus.FocusSchedule;
import com.minimalist.launcher.utils.AppFilterHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Adapter for displaying focus modes in RecyclerView
 */
public class FocusModeAdapter extends RecyclerView.Adapter<FocusModeAdapter.FocusModeViewHolder> {
    
    private List<FocusMode> focusModes = new ArrayList<>();
    private OnFocusModeActionListener activateListener;
    private OnFocusModeActionListener deleteListener;
    private OnFocusModeActionListener editListener;
    
    public interface OnFocusModeActionListener {
        void onAction(FocusMode focusMode);
    }
    
    public FocusModeAdapter(OnFocusModeActionListener activateListener,
                           OnFocusModeActionListener deleteListener,
                           OnFocusModeActionListener editListener) {
        this.activateListener = activateListener;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }
    
    @NonNull
    @Override
    public FocusModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_focus_mode, parent, false);
        return new FocusModeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull FocusModeViewHolder holder, int position) {
        FocusMode focusMode = focusModes.get(position);
        holder.bind(focusMode);
    }
    
    @Override
    public int getItemCount() {
        return focusModes.size();
    }
    
    public void setFocusModes(List<FocusMode> focusModes) {
        this.focusModes = focusModes != null ? focusModes : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * e.g. "09:00–17:00 · Mon, Tue, Wed"
     */
    static String describeSchedule(android.content.Context context, FocusMode mode) {
        String[] shortDays = context.getResources().getStringArray(R.array.weekdays_short);
        List<String> names = new ArrayList<>();
        for (int day : FocusSchedule.parseDays(mode.getActiveDays())) {
            names.add(shortDays[day - 1]);
        }
        return mode.getStartTime() + "–" + mode.getEndTime() + " · " + String.join(", ", names);
    }

    class FocusModeViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView nameText;
        private final TextView statusText;
        private final Button activateButton;
        private final Button deleteButton;
        
        public FocusModeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.focus_mode_name);
            statusText = itemView.findViewById(R.id.focus_mode_status);
            activateButton = itemView.findViewById(R.id.activate_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
        
        public void bind(FocusMode focusMode) {
            nameText.setText(focusMode.getName());
            
            android.content.Context context = itemView.getContext();
            boolean scheduledNow = AppFilterHelper.isScheduledNow(focusMode, Calendar.getInstance());
            String status;
            if (focusMode.isActive()) {
                status = context.getString(R.string.active);
            } else if (scheduledNow) {
                status = context.getString(R.string.active_by_schedule);
            } else {
                status = context.getString(R.string.inactive);
            }
            if (focusMode.isHasSchedule()) {
                status = context.getString(R.string.status_with_schedule, status, describeSchedule(context, focusMode));
            }
            statusText.setText(status);
            statusText.setTextColor(context.getColor(
                    focusMode.isActive() || scheduledNow ? R.color.focus_active : R.color.gray_500));
            activateButton.setText(focusMode.isActive() ? R.string.deactivate : R.string.activate);
            
            activateButton.setOnClickListener(v -> {
                if (activateListener != null) {
                    activateListener.onAction(focusMode);
                }
            });
            
            // Tap the card to edit name and allowed apps
            itemView.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onAction(focusMode);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onAction(focusMode);
                }
            });
        }
    }
}
