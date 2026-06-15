package com.mathwake.android.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlarmDifficultyTest {

    @Test
    public void fromWire_knownValues() {
        assertEquals(AlarmDifficulty.EASY, AlarmDifficulty.fromWire("easy"));
        assertEquals(AlarmDifficulty.MEDIUM, AlarmDifficulty.fromWire("medium"));
        assertEquals(AlarmDifficulty.HARD, AlarmDifficulty.fromWire("hard"));
    }

    @Test
    public void fromWire_unknownOrNull_defaultsToMedium() {
        assertEquals(AlarmDifficulty.MEDIUM, AlarmDifficulty.fromWire(null));
        assertEquals(AlarmDifficulty.MEDIUM, AlarmDifficulty.fromWire("nonsense"));
        assertEquals(AlarmDifficulty.MEDIUM, AlarmDifficulty.fromWire(""));
    }

    @Test
    public void wire_roundTripsForEveryValue() {
        for (AlarmDifficulty difficulty : AlarmDifficulty.values()) {
            assertEquals(difficulty, AlarmDifficulty.fromWire(difficulty.getWire()));
        }
    }
}
