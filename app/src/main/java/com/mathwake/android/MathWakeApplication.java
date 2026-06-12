package com.mathwake.android;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.mathwake.android.alarm.AlarmRingingService;
import com.mathwake.android.data.AppSettingsRepository;

public class MathWakeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppSettingsRepository settings = new AppSettingsRepository(this);
        AppCompatDelegate.setDefaultNightMode(AppSettingsRepository.getNightMode(settings.getDarkMode()));
        AlarmRingingService.ensureNotificationChannel(this);
    }
}
