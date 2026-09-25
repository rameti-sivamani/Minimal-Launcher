package com.minimalist.launcher.utils;

import android.content.Context;
import android.os.Build;

import com.minimalist.launcher.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Keeps the most recent crash on the device (never uploaded) so the user can choose
 * to share it from Settings. Android switches the home screen back to the built-in
 * launcher when a launcher keeps crashing, so these reports matter.
 */
public final class CrashLog {

    private static final String FILE_NAME = "last_crash.txt";

    private CrashLog() {
    }

    public static void install(Context context) {
        final Context appContext = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                write(appContext, thread, error);
            } catch (Throwable ignored) {
                // Never let crash logging hide the original crash
            }
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
    }

    private static void write(Context context, Thread thread, Throwable error) throws Exception {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        String report = "Minimalist Launcher " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE
                + " (API " + Build.VERSION.SDK_INT + ")\n"
                + "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "\n"
                + "Thread: " + thread.getName() + "\n\n"
                + trace;
        try (FileOutputStream out = new FileOutputStream(new File(context.getFilesDir(), FILE_NAME))) {
            out.write(report.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** The last crash report, or null if none has been recorded */
    public static String read(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear(Context context) {
        //noinspection ResultOfMethodCallIgnored
        new File(context.getFilesDir(), FILE_NAME).delete();
    }
}
