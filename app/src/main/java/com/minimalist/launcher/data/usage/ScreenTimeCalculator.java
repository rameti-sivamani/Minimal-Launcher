package com.minimalist.launcher.data.usage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes foreground time per app from a time-ordered stream of
 * resume/pause events (as reported by UsageStatsManager.queryEvents).
 * Kept free of Android types so it can be unit tested on the JVM.
 */
public final class ScreenTimeCalculator {

    public static final int RESUMED = 1;
    public static final int PAUSED = 2;

    /** A single foreground/background transition */
    public static final class Event {
        final String packageName;
        final int type;
        final long timestamp;

        public Event(String packageName, int type, long timestamp) {
            this.packageName = packageName;
            this.type = type;
            this.timestamp = timestamp;
        }
    }

    private ScreenTimeCalculator() {
    }

    /**
     * @param events       events ordered by timestamp, all at or after {@code windowStart}
     * @param windowStart  start of the measured window (e.g. local midnight)
     * @param windowEnd    end of the measured window (usually now)
     * @param excluded     packages not counted (e.g. this launcher)
     * @return foreground milliseconds per package
     */
    public static Map<String, Long> perApp(List<Event> events, long windowStart, long windowEnd,
            Set<String> excluded) {
        Map<String, Long> totals = new HashMap<>();
        Map<String, Long> openSince = new HashMap<>();
        Map<String, Boolean> seen = new HashMap<>();

        for (Event event : events) {
            String pkg = event.packageName;
            if (pkg == null || excluded.contains(pkg)) {
                continue;
            }
            long ts = Math.max(windowStart, Math.min(event.timestamp, windowEnd));

            if (event.type == RESUMED) {
                if (!openSince.containsKey(pkg)) {
                    openSince.put(pkg, ts);
                }
            } else if (event.type == PAUSED) {
                Long start = openSince.remove(pkg);
                if (start == null && !seen.containsKey(pkg)) {
                    // App was already in the foreground when the window started
                    start = windowStart;
                }
                if (start != null && ts > start) {
                    totals.merge(pkg, ts - start, Long::sum);
                }
            }
            seen.put(pkg, Boolean.TRUE);
        }

        // Apps still in the foreground at the end of the window
        for (Map.Entry<String, Long> open : openSince.entrySet()) {
            if (windowEnd > open.getValue()) {
                totals.merge(open.getKey(), windowEnd - open.getValue(), Long::sum);
            }
        }
        return totals;
    }

    public static long total(List<Event> events, long windowStart, long windowEnd, Set<String> excluded) {
        long sum = 0;
        for (long value : perApp(events, windowStart, windowEnd, excluded).values()) {
            sum += value;
        }
        return sum;
    }

    /**
     * Format milliseconds as "2h 35m" or "45m"
     */
    public static String format(long millis) {
        long minutes = Math.max(0, millis) / 60_000;
        long hours = minutes / 60;
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        }
        return minutes + "m";
    }
}
