package com.mathwake.android.model;

public enum AlarmDifficulty {
    EASY("easy", "Easy"),
    MEDIUM("medium", "Medium"),
    HARD("hard", "Hard");

    private final String wire;
    private final String display;

    AlarmDifficulty(String wire, String display) {
        this.wire = wire;
        this.display = display;
    }

    public String getWire() {
        return wire;
    }

    public String getDisplay() {
        return display;
    }

    public static AlarmDifficulty fromWire(String value) {
        if (value != null) {
            for (AlarmDifficulty difficulty : values()) {
                if (difficulty.wire.equals(value)) {
                    return difficulty;
                }
            }
        }
        return MEDIUM;
    }
}
