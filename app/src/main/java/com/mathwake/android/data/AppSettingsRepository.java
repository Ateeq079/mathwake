package com.mathwake.android.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.mathwake.android.model.AlarmDifficulty;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AppSettingsRepository {
    public static final String DARK_MODE_SYSTEM = "system";
    public static final String DARK_MODE_LIGHT = "light";
    public static final String DARK_MODE_DARK = "dark";

    private static final String STORE_NAME = "mathwake_app_settings";
    private static final String KEY_TIME_24_HOUR = "time_24_hour";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_DEFAULT_SNOOZE = "default_snooze";
    private static final String KEY_DEFAULT_VIBRATE = "default_vibrate";
    private static final String KEY_DEFAULT_DIFFICULTY = "default_difficulty";
    private static final String KEY_ONBOARDED = "onboarding_completed";

    private static final DateTimeFormatter TIME_12 = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");

    private final SharedPreferences prefs;

    public AppSettingsRepository(Context context) {
        prefs = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasCompletedOnboarding() {
        return prefs.getBoolean(KEY_ONBOARDED, false);
    }

    public void setOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply();
    }

    public boolean is24HourTime() {
        return prefs.getBoolean(KEY_TIME_24_HOUR, false);
    }

    public void set24HourTime(boolean value) {
        prefs.edit().putBoolean(KEY_TIME_24_HOUR, value).apply();
    }

    public String formatTime(LocalTime time) {
        return time.format(is24HourTime() ? TIME_24 : TIME_12);
    }

    public String getDarkMode() {
        return prefs.getString(KEY_DARK_MODE, DARK_MODE_SYSTEM);
    }

    public void setDarkMode(String value) {
        if (!DARK_MODE_LIGHT.equals(value) && !DARK_MODE_DARK.equals(value)) {
            value = DARK_MODE_SYSTEM;
        }
        prefs.edit().putString(KEY_DARK_MODE, value).apply();
    }

    public static int getNightMode(String darkMode) {
        if (DARK_MODE_DARK.equals(darkMode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        if (DARK_MODE_LIGHT.equals(darkMode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public int getDefaultSnoozeMinutes() {
        return prefs.getInt(KEY_DEFAULT_SNOOZE, 10);
    }

    public void setDefaultSnoozeMinutes(int value) {
        prefs.edit().putInt(KEY_DEFAULT_SNOOZE, Math.max(0, value)).apply();
    }

    public boolean isDefaultVibrate() {
        return prefs.getBoolean(KEY_DEFAULT_VIBRATE, true);
    }

    public void setDefaultVibrate(boolean value) {
        prefs.edit().putBoolean(KEY_DEFAULT_VIBRATE, value).apply();
    }

    public AlarmDifficulty getDefaultDifficulty() {
        return AlarmDifficulty.fromWire(prefs.getString(KEY_DEFAULT_DIFFICULTY, AlarmDifficulty.MEDIUM.getWire()));
    }

    public void setDefaultDifficulty(AlarmDifficulty difficulty) {
        prefs.edit().putString(KEY_DEFAULT_DIFFICULTY, difficulty.getWire()).apply();
    }
}
