package com.mathwake.android.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlarmModel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private final int id;
    private final int hour;
    private final int minute;
    private final String label;
    private final boolean enabled;
    private final Set<Integer> repeatDays;
    private final AlarmDifficulty difficulty;
    private final int snoozeMinutes;
    private final boolean vibrate;
    private final String ringtoneUri;

    public AlarmModel(
            int id,
            int hour,
            int minute,
            String label,
            boolean enabled,
            Set<Integer> repeatDays,
            AlarmDifficulty difficulty
    ) {
        this(id, hour, minute, label, enabled, repeatDays, difficulty, 10, true, null);
    }

    public AlarmModel(
            int id,
            int hour,
            int minute,
            String label,
            boolean enabled,
            Set<Integer> repeatDays,
            AlarmDifficulty difficulty,
            int snoozeMinutes,
            boolean vibrate,
            String ringtoneUri
    ) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.label = label == null || label.trim().isEmpty() ? "Wake Up!" : label.trim();
        this.enabled = enabled;
        this.repeatDays = new HashSet<>(repeatDays == null ? Collections.emptySet() : repeatDays);
        this.difficulty = difficulty == null ? AlarmDifficulty.MEDIUM : difficulty;
        this.snoozeMinutes = Math.max(0, snoozeMinutes);
        this.vibrate = vibrate;
        this.ringtoneUri = ringtoneUri;
    }

    public int getId() {
        return id;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<Integer> getRepeatDays() {
        return new HashSet<>(repeatDays);
    }

    public AlarmDifficulty getDifficulty() {
        return difficulty;
    }

    public int getSnoozeMinutes() {
        return snoozeMinutes;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public String getRingtoneUri() {
        return ringtoneUri;
    }

    public String timeText() {
        return LocalTime.of(hour, minute).format(TIME_FORMATTER);
    }

    public String repeatText() {
        List<Integer> ordered = new ArrayList<>(repeatDays);
        Collections.sort(ordered);
        if (ordered.isEmpty()) {
            return "Once";
        }
        if (ordered.size() == 7) {
            return "Every day";
        }
        if (ordered.equals(listOf(1, 2, 3, 4, 5))) {
            return "Weekdays";
        }
        if (ordered.equals(listOf(6, 7))) {
            return "Weekends";
        }

        String[] labels = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
        List<String> parts = new ArrayList<>();
        for (int day : ordered) {
            if (day >= 1 && day <= 7) {
                parts.add(labels[day - 1]);
            }
        }
        return String.join(" - ", parts);
    }

    public String snoozeText() {
        if (snoozeMinutes <= 0) {
            return "Off";
        }
        return snoozeMinutes + " min";
    }

    public boolean isRepeating() {
        return !repeatDays.isEmpty();
    }

    public LocalDateTime nextOccurrence() {
        return nextOccurrence(LocalDateTime.now());
    }

    public LocalDateTime nextOccurrence(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalTime candidateTime = LocalTime.of(hour, minute);

        if (repeatDays.isEmpty()) {
            LocalDateTime candidate = LocalDateTime.of(today, candidateTime);
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1);
            }
            return candidate;
        }

        for (int offset = 0; offset <= 7; offset++) {
            LocalDate date = today.plusDays(offset);
            if (!repeatDays.contains(date.getDayOfWeek().getValue())) {
                continue;
            }
            LocalDateTime candidate = LocalDateTime.of(date, candidateTime);
            if (candidate.isAfter(now)) {
                return candidate;
            }
        }

        return LocalDateTime.of(today.plusDays(1), candidateTime);
    }

    public JSONObject toJson() {
        try {
            JSONArray repeat = new JSONArray();
            List<Integer> ordered = new ArrayList<>(repeatDays);
            Collections.sort(ordered);
            for (int day : ordered) {
                repeat.put(day);
            }

            JSONObject json = new JSONObject()
                    .put("id", id)
                    .put("hour", hour)
                    .put("minute", minute)
                    .put("label", label)
                    .put("enabled", enabled)
                    .put("difficulty", difficulty.getWire())
                    .put("repeatDays", repeat)
                    .put("snoozeMinutes", snoozeMinutes)
                    .put("vibrate", vibrate);
            if (ringtoneUri != null) {
                json.put("ringtoneUri", ringtoneUri);
            }
            return json;
        } catch (JSONException exception) {
            throw new IllegalStateException("Unable to serialize alarm", exception);
        }
    }

    public AlarmModel withEnabled(boolean value) {
        return new AlarmModel(id, hour, minute, label, value, repeatDays, difficulty, snoozeMinutes, vibrate, ringtoneUri);
    }

    public AlarmModel withDetails(int newHour, int newMinute, String newLabel, Set<Integer> newRepeatDays,
                                  AlarmDifficulty newDifficulty, int newSnoozeMinutes, boolean newVibrate, String newRingtoneUri) {
        return new AlarmModel(id, newHour, newMinute, newLabel, enabled, newRepeatDays, newDifficulty, newSnoozeMinutes, newVibrate, newRingtoneUri);
    }

    public static AlarmModel fromJson(JSONObject json) {
        try {
            Set<Integer> repeat = new HashSet<>();
            JSONArray repeatArray = json.optJSONArray("repeatDays");
            if (repeatArray != null) {
                for (int index = 0; index < repeatArray.length(); index++) {
                    repeat.add(repeatArray.getInt(index));
                }
            }

            return new AlarmModel(
                    json.getInt("id"),
                    json.getInt("hour"),
                    json.getInt("minute"),
                    json.optString("label", "Wake Up!"),
                    json.optBoolean("enabled", true),
                    repeat,
                    AlarmDifficulty.fromWire(json.optString("difficulty")),
                    json.optInt("snoozeMinutes", 10),
                    json.optBoolean("vibrate", true),
                    json.optString("ringtoneUri", null)
            );
        } catch (JSONException exception) {
            throw new IllegalArgumentException("Unable to parse alarm", exception);
        }
    }

    public static int newId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    public static List<AlarmModel> defaultSeed() {
        List<AlarmModel> alarms = new ArrayList<>();
        alarms.add(new AlarmModel(
                1,
                6,
                30,
                "Morning Rise",
                true,
                new HashSet<>(listOf(1, 2, 3, 4, 5)),
                AlarmDifficulty.MEDIUM
        ));
        alarms.add(new AlarmModel(
                2,
                8,
                0,
                "Weekend Lazy",
                false,
                new HashSet<>(listOf(6, 7)),
                AlarmDifficulty.EASY
        ));
        return alarms;
    }

    public static AlarmModel previewFrom(AlarmModel base) {
        return new AlarmModel(
                newId(),
                base.hour,
                base.minute,
                base.label + " (Test)",
                true,
                Collections.emptySet(),
                base.difficulty,
                base.snoozeMinutes,
                base.vibrate,
                base.ringtoneUri
        );
    }

    private static List<Integer> listOf(Integer... values) {
        List<Integer> list = new ArrayList<>();
        Collections.addAll(list, values);
        return list;
    }
}
