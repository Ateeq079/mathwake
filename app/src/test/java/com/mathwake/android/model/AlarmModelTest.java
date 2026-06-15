package com.mathwake.android.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class AlarmModelTest {

    // Monday, 5 Jan 2026, 10:00.
    private static final LocalDateTime MON_10AM = LocalDateTime.of(2026, 1, 5, 10, 0);

    private AlarmModel alarm(int hour, int minute, Integer... repeatDays) {
        return new AlarmModel(
                1, hour, minute, "Test", true,
                new HashSet<>(Arrays.asList(repeatDays)), AlarmDifficulty.MEDIUM);
    }

    @Test
    public void oneTime_laterToday_firesToday() {
        LocalDateTime next = alarm(11, 0).nextOccurrence(MON_10AM);
        assertEquals(LocalDateTime.of(2026, 1, 5, 11, 0), next);
    }

    @Test
    public void oneTime_earlierToday_firesTomorrow() {
        LocalDateTime next = alarm(9, 0).nextOccurrence(MON_10AM);
        assertEquals(LocalDateTime.of(2026, 1, 6, 9, 0), next);
    }

    @Test
    public void oneTime_exactlyNow_firesTomorrow() {
        LocalDateTime next = alarm(10, 0).nextOccurrence(MON_10AM);
        assertEquals(LocalDateTime.of(2026, 1, 6, 10, 0), next);
    }

    @Test
    public void repeating_sameDayLater_firesToday() {
        LocalDateTime next = alarm(11, 0, 1 /* Mon */).nextOccurrence(MON_10AM);
        assertEquals(LocalDateTime.of(2026, 1, 5, 11, 0), next);
    }

    @Test
    public void repeating_sameDayPassed_firesNextWeek() {
        LocalDateTime next = alarm(9, 0, 1 /* Mon */).nextOccurrence(MON_10AM);
        assertEquals(LocalDateTime.of(2026, 1, 12, 9, 0), next);
    }

    @Test
    public void repeating_laterInWeek_firesThatDay() {
        LocalDateTime next = alarm(8, 0, 3 /* Wed */).nextOccurrence(MON_10AM);
        assertEquals(LocalDateTime.of(2026, 1, 7, 8, 0), next);
    }

    @Test
    public void repeatText_namedGroups() {
        assertEquals("Once", alarm(8, 0).repeatText());
        assertEquals("Weekdays", alarm(8, 0, 1, 2, 3, 4, 5).repeatText());
        assertEquals("Weekends", alarm(8, 0, 6, 7).repeatText());
        assertEquals("Every day", alarm(8, 0, 1, 2, 3, 4, 5, 6, 7).repeatText());
    }

    @Test
    public void isRepeating_reflectsRepeatDays() {
        assertFalse(alarm(8, 0).isRepeating());
        assertTrue(alarm(8, 0, 1).isRepeating());
    }

    @Test
    public void blankLabel_fallsBackToDefault() {
        AlarmModel a = new AlarmModel(1, 8, 0, "   ", true,
                Collections.emptySet(), AlarmDifficulty.EASY);
        assertEquals("Wake Up!", a.getLabel());
    }

    @Test
    public void negativeSnooze_isClampedToZero() {
        AlarmModel a = new AlarmModel(1, 8, 0, "x", true,
                Collections.emptySet(), AlarmDifficulty.EASY, -5, true, null);
        assertEquals(0, a.getSnoozeMinutes());
    }
}
