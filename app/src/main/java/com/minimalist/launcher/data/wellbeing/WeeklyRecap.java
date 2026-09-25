package com.minimalist.launcher.data.wellbeing;

import java.util.List;

/**
 * Numbers for the weekly recap card, from daily screen-time totals.
 * Pure Java so it can be unit tested.
 */
public final class WeeklyRecap {

    /** A week needs at least this many days of data to be compared */
    public static final int MIN_DAYS_TO_COMPARE = 4;

    public final long thisWeekTotal;
    public final int thisWeekDays;
    public final long dailyAverage;
    /** Positive = time taken back compared with last week; only valid when {@link #comparable} */
    public final long savedVsLastWeek;
    public final boolean comparable;

    private WeeklyRecap(long thisWeekTotal, int thisWeekDays, long dailyAverage, long savedVsLastWeek,
            boolean comparable) {
        this.thisWeekTotal = thisWeekTotal;
        this.thisWeekDays = thisWeekDays;
        this.dailyAverage = dailyAverage;
        this.savedVsLastWeek = savedVsLastWeek;
        this.comparable = comparable;
    }

    /**
     * @param thisWeek daily totals of the last 7 days (null = no data)
     * @param lastWeek daily totals of the 7 days before that (null = no data)
     */
    public static WeeklyRecap of(List<Long> thisWeek, List<Long> lastWeek) {
        long thisTotal = 0;
        int thisDays = 0;
        for (Long value : thisWeek) {
            if (value != null) {
                thisTotal += value;
                thisDays++;
            }
        }
        long lastTotal = 0;
        int lastDays = 0;
        for (Long value : lastWeek) {
            if (value != null) {
                lastTotal += value;
                lastDays++;
            }
        }
        long average = thisDays > 0 ? thisTotal / thisDays : 0;
        boolean comparable = thisDays >= MIN_DAYS_TO_COMPARE && lastDays >= MIN_DAYS_TO_COMPARE;
        // Compare daily averages scaled to a full week, so missing days don't skew it
        long saved = comparable ? (lastTotal / lastDays - average) * 7 : 0;
        return new WeeklyRecap(thisTotal, thisDays, average, saved, comparable);
    }
}
