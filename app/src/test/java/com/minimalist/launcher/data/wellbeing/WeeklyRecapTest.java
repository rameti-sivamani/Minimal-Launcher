package com.minimalist.launcher.data.wellbeing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class WeeklyRecapTest {

    private static final long H = 3_600_000L;

    @Test
    public void comparesFullWeeks() {
        WeeklyRecap recap = WeeklyRecap.of(
                Arrays.asList(2 * H, 2 * H, 2 * H, 2 * H, 2 * H, 2 * H, 2 * H),
                Arrays.asList(3 * H, 3 * H, 3 * H, 3 * H, 3 * H, 3 * H, 3 * H));
        assertTrue(recap.comparable);
        assertEquals(14 * H, recap.thisWeekTotal);
        assertEquals(2 * H, recap.dailyAverage);
        assertEquals(7 * H, recap.savedVsLastWeek);
    }

    @Test
    public void usedMoreGivesNegativeSaving() {
        WeeklyRecap recap = WeeklyRecap.of(
                Arrays.asList(4 * H, 4 * H, 4 * H, 4 * H),
                Arrays.asList(2 * H, 2 * H, 2 * H, 2 * H));
        assertEquals(-14 * H, recap.savedVsLastWeek);
    }

    @Test
    public void missingDaysAreScaledNotCountedAsZero() {
        WeeklyRecap recap = WeeklyRecap.of(
                Arrays.asList(2 * H, null, 2 * H, 2 * H, 2 * H, null, null),
                Arrays.asList(2 * H, 2 * H, 2 * H, 2 * H, 2 * H, 2 * H, 2 * H));
        assertEquals(4, recap.thisWeekDays);
        assertEquals(0, recap.savedVsLastWeek);
    }

    @Test
    public void notComparableWithTooLittleHistory() {
        WeeklyRecap recap = WeeklyRecap.of(
                Arrays.asList(H, H, H, H, H, H, H),
                Arrays.asList(H, null, null, null, null, null, null));
        assertFalse(recap.comparable);
        assertEquals(0, recap.savedVsLastWeek);
        assertEquals(H, recap.dailyAverage);
    }

    @Test
    public void emptyWeek() {
        WeeklyRecap recap = WeeklyRecap.of(Collections.emptyList(), Collections.emptyList());
        assertEquals(0, recap.thisWeekTotal);
        assertEquals(0, recap.dailyAverage);
        assertFalse(recap.comparable);
    }
}
