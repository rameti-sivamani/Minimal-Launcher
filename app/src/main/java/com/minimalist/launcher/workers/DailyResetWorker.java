package com.minimalist.launcher.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.minimalist.launcher.data.repository.AppRepository;
import com.minimalist.launcher.data.repository.UsageRepository;

/**
 * Worker that runs daily at midnight to reset counters and clean old data
 */
public class DailyResetWorker extends Worker {
    
    public DailyResetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            // Reset launch counters
            AppRepository appRepository = new AppRepository(getApplicationContext());
            appRepository.resetDailyCounters();
            
            // Clean up old usage data (older than 30 days)
            UsageRepository usageRepository = new UsageRepository(getApplicationContext());
            usageRepository.cleanupOldData();
            
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
