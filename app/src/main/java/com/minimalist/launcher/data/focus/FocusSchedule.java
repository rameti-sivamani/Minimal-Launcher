package com.minimalist.launcher.data.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pure schedule logic for focus modes (no Android types, unit tested).
 * Times are "HH:mm"; days are ISO numbers 1 = Monday ... 7 = Sunday.
 * A window whose end is before its start runs overnight (e.g. 22:00-07:00).
 */
public final class FocusSchedule {

    private FocusSchedule() {
    }

    /** Parse "HH:mm" into minutes after midnight, or -1 if invalid */
    public static int parseMinutes(String time) {
        if (time == null) {
            return -1;
        }
        String[] parts = time.trim().split(":");
        if (parts.length != 2) {
            return -1;
        }
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                return -1;
            }
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatMinutes(int minutes) {
        return String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    public static Set<Integer> parseDays(String days) {
        Set<Integer> result = new TreeSet<>();
        if (days == null || days.trim().isEmpty()) {
            return result;
        }
        for (String part : days.split(",")) {
            try {
                int day = Integer.parseInt(part.trim());
                if (day >= 1 && day <= 7) {
                    result.add(day);
                }
            } catch (NumberFormatException ignored) {
                // Skip malformed entries
            }
        }
        return result;
    }

    public static String formatDays(Collection<Integer> days) {
        List<String> parts = new ArrayList<>();
        for (int day : new TreeSet<>(days)) {
            parts.add(String.valueOf(day));
        }
        return String.join(",", parts);
    }

    /**
     * @param dayOfWeek   ISO day (1 = Monday ... 7 = Sunday) of the moment being checked
     * @param minuteOfDay minutes after midnight of the moment being checked
     */
    public static boolean isActive(String start, String end, String activeDays, int dayOfWeek, int minuteOfDay) {
        int startMin = parseMinutes(start);
        int endMin = parseMinutes(end);
        Set<Integer> days = parseDays(activeDays);
        if (startMin < 0 || endMin < 0 || startMin == endMin || days.isEmpty()) {
            return false;
        }

        if (startMin < endMin) {
            // Same-day window
            return days.contains(dayOfWeek) && minuteOfDay >= startMin && minuteOfDay < endMin;
        }

        // Overnight window: the part after midnight belongs to the previous day's schedule
        if (minuteOfDay >= startMin) {
            return days.contains(dayOfWeek);
        }
        if (minuteOfDay < endMin) {
            int previousDay = dayOfWeek == 1 ? 7 : dayOfWeek - 1;
            return days.contains(previousDay);
        }
        return false;
    }

    /** Convert java.util.Calendar.DAY_OF_WEEK (1 = Sunday) to ISO (1 = Monday) */
    public static int isoDayFromCalendar(int calendarDayOfWeek) {
        return calendarDayOfWeek == 1 ? 7 : calendarDayOfWeek - 1;
    }
}
