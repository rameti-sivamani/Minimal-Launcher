package com.minimalist.launcher.data.wellbeing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class StreakCalculatorTest {

    private static final long HOUR = 3_600_000L;
    private static final long GOAL = 2 * HOUR;

    @Test
    public void countsConsecutiveDaysUnderGoal() {
        assertEquals(3, StreakCalculator.currentStreak(Arrays.asList(HOUR, 2 * HOUR, HOUR / 2, 5 * HOUR, HOUR), GOAL));
    }

    @Test
    public void overGoalYesterdayMeansNoStreak() {
        assertEquals(0, StreakCalculator.currentStreak(Arrays.asList(3 * HOUR, HOUR), GOAL));
    }

    @Test
    public void missingDataEndsStreak() {
        assertEquals(1, StreakCalculator.currentStreak(Arrays.asList(HOUR, null, HOUR), GOAL));
        assertEquals(0, StreakCalculator.currentStreak(Collections.emptyList(), GOAL));
    }

    @Test
    public void todayExtendsStreakOnlyWhileUnderGoal() {
        assertEquals(4, StreakCalculator.displayStreak(3, HOUR, GOAL));
        assertEquals(3, StreakCalculator.displayStreak(3, 3 * HOUR, GOAL));
        assertEquals(3, StreakCalculator.displayStreak(3, -1, GOAL)); // no usage access
    }

    @Test
    public void goalProgressIsClamped() {
        assertEquals(0.5f, StreakCalculator.goalProgress(HOUR, GOAL), 0.001f);
        assertEquals(1f, StreakCalculator.goalProgress(5 * HOUR, GOAL), 0.001f);
        assertEquals(0f, StreakCalculator.goalProgress(-1, GOAL), 0.001f);
    }
}
