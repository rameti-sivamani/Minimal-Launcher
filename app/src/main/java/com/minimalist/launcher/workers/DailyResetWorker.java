package com.minimalist.launcher.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.repository.UsageRepository;
import com.minimalist.launcher.data.wellbeing.WellbeingStore;

/**
 * Worker that runs daily at midnight to reset counters and clean old data.
 * Work runs synchronously so it finishes before WorkManager marks it done.
 */
public class DailyResetWorker extends Worker {

    public DailyResetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            new AppRepository(getApplicationContext()).resetDailyCountersSync();
            UsageRepository usageRepository = new UsageRepository(getApplicationContext());
            usageRepository.cleanupOldDataSync();
            // Save yesterday's screen time for streaks before Android forgets it
            usageRepository.backfillDailyTotals(new WellbeingStore(getApplicationContext()));
            return Result.success();
        } catch (Exception e) {
            Log.e("DailyResetWorker", "Daily reset failed", e);
            return Result.retry();
        }
    }
}
