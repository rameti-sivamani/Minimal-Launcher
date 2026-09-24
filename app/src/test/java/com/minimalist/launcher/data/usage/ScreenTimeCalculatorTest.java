package com.minimalist.launcher.data.usage;

import static com.minimalist.launcher.data.usage.ScreenTimeCalculator.PAUSED;
import static com.minimalist.launcher.data.usage.ScreenTimeCalculator.RESUMED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.minimalist.launcher.data.usage.ScreenTimeCalculator.Event;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScreenTimeCalculatorTest {

    private static final long MIN = 60_000;
    private static final Set<String> NONE = Collections.emptySet();

    @Test
    public void sumsSimpleSessions() {
        List<Event> events = Arrays.asList(
                new Event("a", RESUMED, 0),
                new Event("a", PAUSED, 10 * MIN),
                new Event("b", RESUMED, 20 * MIN),
                new Event("b", PAUSED, 25 * MIN),
                new Event("a", RESUMED, 30 * MIN),
                new Event("a", PAUSED, 35 * MIN));

        Map<String, Long> perApp = ScreenTimeCalculator.perApp(events, 0, 60 * MIN, NONE);

        assertEquals(15 * MIN, (long) perApp.get("a"));
        assertEquals(5 * MIN, (long) perApp.get("b"));
        assertEquals(20 * MIN, ScreenTimeCalculator.total(events, 0, 60 * MIN, NONE));
    }

    @Test
    public void countsSessionStartedBeforeWindow() {
        // App was already open at midnight; first event we see is its pause
        List<Event> events = Collections.singletonList(new Event("a", PAUSED, 100 + 5 * MIN));
        assertEquals(5 * MIN, ScreenTimeCalculator.total(events, 100, 100 + 60 * MIN, NONE));
    }

    @Test
    public void countsAppStillOpenAtWindowEnd() {
        List<Event> events = Collections.singletonList(new Event("a", RESUMED, 50 * MIN));
        assertEquals(10 * MIN, ScreenTimeCalculator.total(events, 0, 60 * MIN, NONE));
    }

    @Test
    public void activitySwitchWithinOneAppIsNotDoubleCounted() {
        // Activity A resumes, activity B of the same app resumes before A pauses
        List<Event> events = Arrays.asList(
                new Event("a", RESUMED, 0),
                new Event("a", RESUMED, 2 * MIN),
                new Event("a", PAUSED, 3 * MIN),
                new Event("a", PAUSED, 8 * MIN));
        // Counted 0..3; the trailing pause has no open session and is ignored
        assertEquals(3 * MIN, ScreenTimeCalculator.total(events, 0, 60 * MIN, NONE));
    }

    @Test
    public void strayPauseAfterKnownActivityIsIgnored() {
        List<Event> events = Arrays.asList(
                new Event("a", RESUMED, 0),
                new Event("a", PAUSED, MIN),
                new Event("a", PAUSED, 30 * MIN));
        assertEquals(MIN, ScreenTimeCalculator.total(events, 0, 60 * MIN, NONE));
    }

    @Test
    public void excludesPackages() {
        List<Event> events = Arrays.asList(
                new Event("launcher", RESUMED, 0),
                new Event("launcher", PAUSED, 30 * MIN),
                new Event("a", RESUMED, 30 * MIN),
                new Event("a", PAUSED, 40 * MIN));
        Map<String, Long> perApp = ScreenTimeCalculator.perApp(
                events, 0, 60 * MIN, Collections.singleton("launcher"));
        assertFalse(perApp.containsKey("launcher"));
        assertEquals(10 * MIN, (long) perApp.get("a"));
    }

    @Test
    public void clampsEventsOutsideWindow() {
        List<Event> events = Arrays.asList(
                new Event("a", RESUMED, 50 * MIN),
                new Event("a", PAUSED, 90 * MIN));
        assertEquals(10 * MIN, ScreenTimeCalculator.total(events, 0, 60 * MIN, NONE));
    }

    @Test
    public void formatsDurations() {
        assertEquals("0m", ScreenTimeCalculator.format(0));
        assertEquals("45m", ScreenTimeCalculator.format(45 * MIN + 30_000));
        assertEquals("2h 5m", ScreenTimeCalculator.format(125 * MIN));
        assertEquals("0m", ScreenTimeCalculator.format(-5));
    }
}
