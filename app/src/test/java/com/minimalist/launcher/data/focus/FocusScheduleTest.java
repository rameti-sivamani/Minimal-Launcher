package com.minimalist.launcher.data.focus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

public class FocusScheduleTest {

    private static final String WEEKDAYS = "1,2,3,4,5";

    private static int at(int h, int m) {
        return h * 60 + m;
    }

    @Test
    public void sameDayWindow() {
        assertTrue(FocusSchedule.isActive("09:00", "17:00", WEEKDAYS, 1, at(9, 0)));
        assertTrue(FocusSchedule.isActive("09:00", "17:00", WEEKDAYS, 5, at(16, 59)));
        assertFalse(FocusSchedule.isActive("09:00", "17:00", WEEKDAYS, 1, at(17, 0)));
        assertFalse(FocusSchedule.isActive("09:00", "17:00", WEEKDAYS, 1, at(8, 59)));
        assertFalse(FocusSchedule.isActive("09:00", "17:00", WEEKDAYS, 6, at(12, 0)));
    }

    @Test
    public void overnightWindowBelongsToStartDay() {
        // Friday night 22:00 - 07:00
        assertTrue(FocusSchedule.isActive("22:00", "07:00", "5", 5, at(23, 30)));
        assertTrue(FocusSchedule.isActive("22:00", "07:00", "5", 6, at(6, 59)));
        assertFalse(FocusSchedule.isActive("22:00", "07:00", "5", 6, at(7, 0)));
        assertFalse(FocusSchedule.isActive("22:00", "07:00", "5", 5, at(6, 0)));
        // Sunday night wraps to Monday morning
        assertTrue(FocusSchedule.isActive("22:00", "07:00", "7", 1, at(1, 0)));
    }

    @Test
    public void invalidSchedulesAreNeverActive() {
        assertFalse(FocusSchedule.isActive(null, "07:00", WEEKDAYS, 1, at(6, 0)));
        assertFalse(FocusSchedule.isActive("25:00", "07:00", WEEKDAYS, 1, at(6, 0)));
        assertFalse(FocusSchedule.isActive("09:00", "09:00", WEEKDAYS, 1, at(9, 0)));
        assertFalse(FocusSchedule.isActive("09:00", "17:00", "", 1, at(10, 0)));
    }

    @Test
    public void parsesAndFormats() {
        assertEquals(at(7, 5), FocusSchedule.parseMinutes("07:05"));
        assertEquals(-1, FocusSchedule.parseMinutes("7"));
        assertEquals("07:05", FocusSchedule.formatMinutes(at(7, 5)));
        assertEquals("1,3,7", FocusSchedule.formatDays(Arrays.asList(7, 1, 3, 3)));
        assertEquals(3, FocusSchedule.parseDays("1, 3,x,9,7").size());
    }

    @Test
    public void convertsCalendarDays() {
        assertEquals(7, FocusSchedule.isoDayFromCalendar(Calendar.SUNDAY));
        assertEquals(1, FocusSchedule.isoDayFromCalendar(Calendar.MONDAY));
        assertEquals(6, FocusSchedule.isoDayFromCalendar(Calendar.SATURDAY));
    }
}
