package com.mathwake.android;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.RingtoneManager;

import com.mathwake.android.ui.ThemeUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.widget.Button;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import com.mathwake.android.alarm.AlarmScheduler;
import com.mathwake.android.alarm.TimerReceiver;
import com.mathwake.android.data.AppSettingsRepository;
import com.mathwake.android.data.AlarmRepository;
import com.mathwake.android.model.AlarmModel;
import com.mathwake.android.ui.edit.EditAlarmActivity;
import com.mathwake.android.ui.onboarding.OnboardingActivity;
import com.mathwake.android.ui.settings.SettingsActivity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EDIT_ALARM = 10;
    private static final int REQUEST_NOTIFICATIONS = 11;
    private static final int MAX_CONTENT_WIDTH_DP = 480;

    private AlarmRepository repository;
    private AppSettingsRepository appSettings;
    private LinearLayout list;
    private LinearLayout bottomNav;
    private com.google.android.material.floatingactionbutton.FloatingActionButton addAlarmFab;
    private boolean notificationsGranted = true;
    private boolean exactAlarmGranted = true;
    private boolean overlayGranted = true;
    private boolean batteryGranted = true;
    private boolean startupPermissionsRequested = false;

    private enum Tab {
        ALARMS,
        STOPWATCH,
        TIMER
    }
    private Tab activeTab = Tab.ALARMS;

    // Stopwatch fields
    private long stopwatchStartTime = 0L;
    private long stopwatchElapsedTime = 0L;
    private boolean stopwatchRunning = false;
    private final Handler stopwatchHandler = new Handler(Looper.getMainLooper());
    private Runnable stopwatchRunnable;
    private final List<String> stopwatchLaps = new ArrayList<>();
    private TextView stopwatchTimeView;
    private LinearLayout stopwatchLapsContainer;

    // Timer fields
    private long timerDurationMs = 0L;
    private long timerTimeLeftMs = 0L;
    private long timerEndTimeMs = 0L;
    private boolean timerRunning = false;
    private boolean timerActive = false;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private NumberPicker timerHourPicker;
    private NumberPicker timerMinPicker;
    private NumberPicker timerSecPicker;
    private TextView timerCountdownView;
    private android.media.Ringtone timerRingtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the saved Dark Mode preference to this activity directly, before anything is
        // inflated. Relying only on the Application-level default can lag on warm starts, which
        // left the screen rendering in the light theme (and the OS force-darkening it, turning
        // cards white). Setting the local night mode here keeps the app's own dark theme
        // authoritative every time the screen opens.
        getDelegate().setLocalNightMode(
                AppSettingsRepository.getNightMode(new AppSettingsRepository(this).getDarkMode()));
        super.onCreate(savedInstanceState);
        repository = new AlarmRepository(this);
        appSettings = new AppSettingsRepository(this);

        stopwatchRunnable = new Runnable() {
            @Override
            public void run() {
                if (!stopwatchRunning) return;
                long total = stopwatchElapsedTime + (System.currentTimeMillis() - stopwatchStartTime);
                updateStopwatchDisplay(total);
                stopwatchHandler.postDelayed(this, 30);
            }
        };

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!timerRunning) return;
                timerTimeLeftMs = Math.max(0L, timerEndTimeMs - System.currentTimeMillis());
                if (timerTimeLeftMs <= 0) {
                    timerTimeLeftMs = 0;
                    timerRunning = false;
                    timerActive = false;
                    updateTimerDisplay(timerTimeLeftMs);
                    onTimerFinished();
                } else {
                    updateTimerDisplay(timerTimeLeftMs);
                    timerHandler.postDelayed(this, Math.min(1000L, timerTimeLeftMs));
                }
            }
        };

        buildLayout();
        refreshState();
        // First launch: walk the user through granting permissions. Afterwards, just make sure
        // the notification permission has been asked for once.
        if (!appSettings.hasCompletedOnboarding()) {
            startActivity(new Intent(this, OnboardingActivity.class));
        } else {
            maybeRequestStartupPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    @Override
    protected void onDestroy() {
        stopwatchHandler.removeCallbacks(stopwatchRunnable);
        timerHandler.removeCallbacks(timerRunnable);
        if (timerRingtone != null && timerRingtone.isPlaying()) {
            timerRingtone.stop();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_ALARM && resultCode == RESULT_OK) {
            refreshState();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            refreshState();
        }
    }

    private void buildLayout() {
        // Let the system reserve space for the status and navigation bars (non edge-to-edge,
        // matching the theme's windowOptOutEdgeToEdgeEnforcement). This keeps the bottom nav
        // sitting above the device's navigation bar / gesture pill on every device.
        boolean lightBars = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                != android.content.res.Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat insetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(lightBars);
        insetsController.setAppearanceLightNavigationBars(lightBars);

        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(color("#F7F8FC"));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color("#F7F8FC"));

        // Responsive wrapper: center content with max width on large screens
        FrameLayout wrapper = new FrameLayout(this);
        scrollView.addView(wrapper, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        // Extra bottom padding so the last item can scroll clear of the floating add button.
        list.setPadding(dp(20), dp(24), dp(20), dp(96));

        FrameLayout.LayoutParams listParams = new FrameLayout.LayoutParams(
                constrainedWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        listParams.gravity = Gravity.CENTER_HORIZONTAL;
        wrapper.addView(list, listParams);

        // Content area (weight 1) holds the scrolling list with an "add alarm" FAB floating
        // over its bottom-right corner, just above the nav bar.
        FrameLayout content = new FrameLayout(this);
        content.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        addAlarmFab = new com.google.android.material.floatingactionbutton.FloatingActionButton(this);
        addAlarmFab.setImageResource(R.drawable.ic_add);
        addAlarmFab.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        addAlarmFab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color("#6C63FF")));
        addAlarmFab.setContentDescription("Add alarm");
        addAlarmFab.setOnClickListener(v -> openEditor(-1));
        FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        fabParams.gravity = Gravity.BOTTOM | Gravity.END;
        fabParams.setMargins(0, 0, dp(20), dp(20));
        content.addView(addAlarmFab, fabParams);

        screen.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(12), dp(8), dp(12), dp(8));
        bottomNav.setBackgroundColor(ThemeUtils.getSurfaceColor(this));
        screen.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(screen);
    }

    private void refreshState() {
        if (activeTab == Tab.ALARMS) {
            refreshPermissions();
            List<AlarmModel> alarms = repository.getAlarms();
            Collections.sort(alarms, Comparator.comparing(AlarmModel::nextOccurrence));

            if (exactAlarmGranted) {
                for (AlarmModel alarm : alarms) {
                    if (alarm.isEnabled()) {
                        AlarmScheduler.schedule(this, alarm);
                    } else {
                        AlarmScheduler.cancel(this, alarm.getId());
                    }
                }
            }

            render(alarms);
        } else if (activeTab == Tab.STOPWATCH) {
            renderStopwatch();
        } else if (activeTab == Tab.TIMER) {
            renderTimer();
        }
        if (addAlarmFab != null) {
            addAlarmFab.setVisibility(activeTab == Tab.ALARMS ? View.VISIBLE : View.GONE);
        }
        renderBottomNavigation();
    }

    private void refreshPermissions() {
        notificationsGranted = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        exactAlarmGranted = AlarmScheduler.canScheduleExact(this);
        overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        batteryGranted = batteryOptimizationIgnored();
    }

    private void renderBottomNavigation() {
        if (bottomNav == null) {
            return;
        }
        bottomNav.removeAllViews();
        addBottomNavButton("Alarms", activeTab == Tab.ALARMS, R.drawable.ic_alarm, v -> {
            activeTab = Tab.ALARMS;
            refreshState();
        });
        addBottomNavButton("Stopwatch", activeTab == Tab.STOPWATCH, R.drawable.ic_stopwatch, v -> {
            activeTab = Tab.STOPWATCH;
            refreshState();
        });
        addBottomNavButton("Timer", activeTab == Tab.TIMER, R.drawable.ic_timer, v -> {
            activeTab = Tab.TIMER;
            refreshState();
        });
        addBottomNavButton("Settings", false, R.drawable.ic_settings, v -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void addBottomNavButton(String label, boolean active, int iconRes, View.OnClickListener listener) {
        Button button = styledNavButton(label, active, iconRes);
        button.setOnClickListener(listener);
        // WRAP_CONTENT height so the icon + label always fit without clipping the label.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        bottomNav.addView(button, params);
    }

    private Button styledNavButton(String label, boolean active, int iconRes) {
        Button button = new Button(this);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setElevation(0);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(4), dp(6), dp(4), dp(6));
        
        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(this, iconRes);
        if (icon != null) {
            icon.setTintList(android.content.res.ColorStateList.valueOf(active ? color("#6C63FF") : color("#6D6A80")));
            button.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
        }

        if (active) {
            button.setTextColor(color("#6C63FF"));
        } else {
            button.setTextColor(color("#6D6A80"));
        }
        button.setStateListAnimator(null);
        return button;
    }


    private void render(List<AlarmModel> alarms) {
        list.removeAllViews();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        list.addView(header, matchWrap());

        TextView title = title("MathWake", 30);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        TextView subtitle = text("Wake up smarter every day", 14, color("#6D6A80"), Typeface.NORMAL);
        list.addView(subtitle, marginTop(4));

        AlarmModel next = null;
        for (AlarmModel alarm : alarms) {
            if (alarm.isEnabled() && (next == null || alarm.nextOccurrence().isBefore(next.nextOccurrence()))) {
                next = alarm;
            }
        }
        if (next != null) {
            list.addView(nextAlarmCard(next), marginTop(18));
        }

        if (!notificationsGranted || !exactAlarmGranted || !overlayGranted || !batteryGranted) {
            list.addView(permissionCard(), marginTop(14));
        }

        if (alarms.isEmpty()) {
            TextView empty = text("No alarms yet. Tap + Add Alarm to create your first math alarm.", 16, color("#6D6A80"), Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(48), dp(20), dp(48));
            empty.setBackground(cardBackground(color("#FFFFFF")));
            list.addView(empty, marginTop(18));
            return;
        }

        for (AlarmModel alarm : alarms) {
            list.addView(alarmCard(alarm), marginTop(14));
        }
    }

    // ─── Stopwatch tab rendering ──────────────────────────────────

    private void renderStopwatch() {
        list.removeAllViews();

        TextView title = title("Stopwatch", 30);
        list.addView(title);
        list.addView(text("Track time and laps accurately", 14, color("#6D6A80"), Typeface.NORMAL), marginTop(4));

        // Time card
        LinearLayout card = verticalCard(color("#FFFFFF"));
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(32), dp(24), dp(32));
        card.setElevation(dp(2));

        stopwatchTimeView = text(formatTime(stopwatchElapsedTime), 44, color("#2D2D44"), Typeface.BOLD);
        stopwatchTimeView.setGravity(Gravity.CENTER);
        card.addView(stopwatchTimeView);
        list.addView(card, marginTop(20));

        // Controls
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button startPause = styledButton(stopwatchRunning ? "Pause" : "Start", stopwatchRunning ? color("#FFBE3B") : color("#6C63FF"), stopwatchRunning ? color("#2D2D44") : Color.WHITE);
        android.graphics.drawable.Drawable spIcon = ContextCompat.getDrawable(this, stopwatchRunning ? R.drawable.ic_pause : R.drawable.ic_play);
        if (spIcon != null) {
            spIcon.setTint(stopwatchRunning ? color("#2D2D44") : Color.WHITE);
            startPause.setCompoundDrawablesWithIntrinsicBounds(spIcon, null, null, null);
        }
        startPause.setOnClickListener(v -> {
            if (stopwatchRunning) {
                stopwatchElapsedTime += System.currentTimeMillis() - stopwatchStartTime;
                stopwatchRunning = false;
                stopwatchHandler.removeCallbacks(stopwatchRunnable);
            } else {
                stopwatchStartTime = System.currentTimeMillis();
                stopwatchRunning = true;
                stopwatchHandler.post(stopwatchRunnable);
            }
            refreshState();
        });

        Button lap = styledButton("Lap", color("#43E97B"), color("#2D2D44"));
        lap.setOnClickListener(v -> {
            if (stopwatchRunning || stopwatchElapsedTime > 0) {
                long total = stopwatchElapsedTime + (stopwatchRunning ? (System.currentTimeMillis() - stopwatchStartTime) : 0);
                String lapTime = formatTime(total);
                stopwatchLaps.add(0, "Lap " + (stopwatchLaps.size() + 1) + ": " + lapTime);
                renderLaps();
            }
        });

        Button reset = styledButton("Reset", color("#FF6584"), Color.WHITE);
        android.graphics.drawable.Drawable resetIcon = ContextCompat.getDrawable(this, R.drawable.ic_refresh);
        if (resetIcon != null) {
            resetIcon.setTint(Color.WHITE);
            reset.setCompoundDrawablesWithIntrinsicBounds(resetIcon, null, null, null);
        }
        reset.setOnClickListener(v -> {
            stopwatchRunning = false;
            stopwatchHandler.removeCallbacks(stopwatchRunnable);
            stopwatchStartTime = 0L;
            stopwatchElapsedTime = 0L;
            stopwatchLaps.clear();
            refreshState();
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnParams.setMargins(dp(4), 0, dp(4), 0);
        controls.addView(startPause, btnParams);
        controls.addView(lap, btnParams);
        controls.addView(reset, btnParams);

        list.addView(controls, marginTop(18));

        stopwatchLapsContainer = new LinearLayout(this);
        stopwatchLapsContainer.setOrientation(LinearLayout.VERTICAL);
        list.addView(stopwatchLapsContainer, marginTop(18));
        renderLaps();
    }

    private void renderLaps() {
        if (stopwatchLapsContainer == null) return;
        stopwatchLapsContainer.removeAllViews();
        if (!stopwatchLaps.isEmpty()) {
            TextView header = text("Laps", 16, color("#2D2D44"), Typeface.BOLD);
            stopwatchLapsContainer.addView(header);

            LinearLayout card = verticalCard(color("#FFFFFF"));
            card.setElevation(dp(1));
            for (int i = 0; i < stopwatchLaps.size(); i++) {
                if (i > 0) {
                    card.addView(divider());
                }
                card.addView(text(stopwatchLaps.get(i), 15, color("#2D2D44"), Typeface.NORMAL), marginTop(6));
            }
            stopwatchLapsContainer.addView(card, marginTop(8));
        }
    }

    private void updateStopwatchDisplay(long total) {
        if (activeTab == Tab.STOPWATCH && stopwatchTimeView != null) {
            stopwatchTimeView.setText(formatTime(total));
        }
    }

    private String formatTime(long ms) {
        long minutes = (ms / 60000) % 60;
        long seconds = (ms / 1000) % 60;
        long hundredths = (ms / 10) % 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    // ─── Timer tab rendering ──────────────────────────────────────

    private void renderTimer() {
        list.removeAllViews();

        TextView title = title("Timer", 30);
        list.addView(title);
        list.addView(text("Set a countdown for tasks", 14, color("#6D6A80"), Typeface.NORMAL), marginTop(4));

        if (!timerActive) {
            // Setup screen
            LinearLayout pickersCard = verticalCard(color("#FFFFFF"));
            pickersCard.setElevation(dp(2));

            LinearLayout pickersRow = new LinearLayout(this);
            pickersRow.setOrientation(LinearLayout.HORIZONTAL);
            pickersRow.setGravity(Gravity.CENTER);

            timerHourPicker = createTimerPicker(0, 23);
            timerMinPicker = createTimerPicker(0, 59);
            timerSecPicker = createTimerPicker(0, 59);

            LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            pickerParams.setMargins(dp(6), 0, dp(6), 0);

            pickersRow.addView(timerPickerLayout(timerHourPicker, "Hours"), pickerParams);
            pickersRow.addView(timerPickerLayout(timerMinPicker, "Minutes"), pickerParams);
            pickersRow.addView(timerPickerLayout(timerSecPicker, "Seconds"), pickerParams);

            pickersCard.addView(pickersRow);
            list.addView(pickersCard, marginTop(20));

            Button startBtn = styledButton("Start Timer", color("#6C63FF"), Color.WHITE);
            android.graphics.drawable.Drawable stIcon = ContextCompat.getDrawable(this, R.drawable.ic_play);
            if (stIcon != null) {
                stIcon.setTint(Color.WHITE);
                startBtn.setCompoundDrawablesWithIntrinsicBounds(stIcon, null, null, null);
            }
            startBtn.setOnClickListener(v -> {
                int h = timerHourPicker.getValue();
                int m = timerMinPicker.getValue();
                int s = timerSecPicker.getValue();
                long totalMs = (h * 3600L + m * 60L + s) * 1000L;
                if (totalMs > 0) {
                    timerDurationMs = totalMs;
                    timerTimeLeftMs = totalMs;
                    timerEndTimeMs = System.currentTimeMillis() + timerTimeLeftMs;
                    timerRunning = true;
                    timerActive = true;
                    timerHandler.post(timerRunnable);
                    scheduleTimerAlarm(timerEndTimeMs);
                    refreshState();
                }
            });
            LinearLayout.LayoutParams startParams = marginTop(20);
            startParams.height = dp(52);
            list.addView(startBtn, startParams);
        } else {
            // Countdown screen
            LinearLayout countdownCard = verticalCard(color("#FFFFFF"));
            countdownCard.setGravity(Gravity.CENTER);
            countdownCard.setPadding(dp(24), dp(32), dp(24), dp(32));
            countdownCard.setElevation(dp(2));

            timerCountdownView = text(formatTimerTime(timerTimeLeftMs), 44, color("#2D2D44"), Typeface.BOLD);
            timerCountdownView.setGravity(Gravity.CENTER);
            countdownCard.addView(timerCountdownView);
            list.addView(countdownCard, marginTop(20));

            // Controls
            LinearLayout controls = new LinearLayout(this);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            controls.setGravity(Gravity.CENTER);

            Button pauseResume = styledButton(timerRunning ? "Pause" : "Resume", timerRunning ? color("#FFBE3B") : color("#6C63FF"), timerRunning ? color("#2D2D44") : Color.WHITE);
            android.graphics.drawable.Drawable prIcon = ContextCompat.getDrawable(this, timerRunning ? R.drawable.ic_pause : R.drawable.ic_play);
            if (prIcon != null) {
                prIcon.setTint(timerRunning ? color("#2D2D44") : Color.WHITE);
                pauseResume.setCompoundDrawablesWithIntrinsicBounds(prIcon, null, null, null);
            }
            pauseResume.setOnClickListener(v -> {
                if (timerRunning) {
                    timerTimeLeftMs = Math.max(0L, timerEndTimeMs - System.currentTimeMillis());
                    timerRunning = false;
                    timerHandler.removeCallbacks(timerRunnable);
                    cancelTimerAlarm();
                } else {
                    timerEndTimeMs = System.currentTimeMillis() + timerTimeLeftMs;
                    timerRunning = true;
                    timerHandler.post(timerRunnable);
                    scheduleTimerAlarm(timerEndTimeMs);
                }
                refreshState();
            });

            Button cancelBtn = styledButton("Cancel", color("#FF6584"), Color.WHITE);
            android.graphics.drawable.Drawable cnIcon = ContextCompat.getDrawable(this, R.drawable.ic_stop);
            if (cnIcon != null) {
                cnIcon.setTint(Color.WHITE);
                cancelBtn.setCompoundDrawablesWithIntrinsicBounds(cnIcon, null, null, null);
            }
            cancelBtn.setOnClickListener(v -> {
                timerRunning = false;
                timerActive = false;
                timerHandler.removeCallbacks(timerRunnable);
                timerTimeLeftMs = 0L;
                timerEndTimeMs = 0L;
                cancelTimerAlarm();
                refreshState();
            });

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            btnParams.setMargins(dp(6), 0, dp(6), 0);
            controls.addView(pauseResume, btnParams);
            controls.addView(cancelBtn, btnParams);
            list.addView(controls, marginTop(18));
        }
    }

    // Back the countdown with an AlarmManager alarm so the "timer finished" alert still fires
    // if the user leaves the app or it gets killed before the countdown ends.
    private void scheduleTimerAlarm(long triggerAtMs) {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent pendingIntent = timerPendingIntent();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent);
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent);
            }
        } catch (SecurityException exception) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent);
        }
    }

    private void cancelTimerAlarm() {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (manager != null) {
            manager.cancel(timerPendingIntent());
        }
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(TimerReceiver.NOTIFICATION_ID);
        }
    }

    private PendingIntent timerPendingIntent() {
        Intent intent = new Intent(this, TimerReceiver.class);
        return PendingIntent.getBroadcast(
                this,
                TimerReceiver.REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private NumberPicker createTimerPicker(int min, int max) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        return picker;
    }

    private LinearLayout timerPickerLayout(NumberPicker picker, String title) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.addView(text(title, 12, color("#6D6A80"), Typeface.NORMAL));
        container.addView(picker, marginTop(6));
        return container;
    }

    private String formatTimerTime(long ms) {
        long hours = ms / 3600000;
        long minutes = (ms / 60000) % 60;
        long seconds = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateTimerDisplay(long ms) {
        if (activeTab == Tab.TIMER && timerCountdownView != null) {
            timerCountdownView.setText(formatTimerTime(ms));
        }
    }

    private void onTimerFinished() {
        // The app is in the foreground, so alert with the in-app dialog/ringtone and cancel the
        // background notification we scheduled as a fallback.
        cancelTimerAlarm();
        try {
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (sound == null) {
                sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
            if (sound != null) {
                timerRingtone = RingtoneManager.getRingtone(MainActivity.this, sound);
                if (timerRingtone != null) {
                    timerRingtone.play();
                }
            }
        } catch (Exception ignored) {}

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Timer Finished!")
                .setMessage("Your timer has ended.")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (timerRingtone != null && timerRingtone.isPlaying()) {
                        timerRingtone.stop();
                    }
                    refreshState();
                })
                .setCancelable(false)
                .show();
    }

    // ─── Alarm card rendering ─────────────────────────────────────

    private View nextAlarmCard(AlarmModel alarm) {
        LinearLayout card = verticalCard(color("#6C63FF"));
        TextView label = text("Next Alarm", 12, Color.WHITE, Typeface.BOLD);
        label.setAlpha(0.8f);
        card.addView(label);
        card.addView(text(formatAlarmTime(alarm), 26, Color.WHITE, Typeface.BOLD));

        String meta = alarm.getLabel() + " · " + alarm.getDifficulty().getDisplay();
        if (alarm.getSnoozeMinutes() > 0) {
            meta += " · Snooze " + alarm.snoozeText();
        }
        card.addView(text(meta, 14, color("#D6FBE4"), Typeface.NORMAL));
        return card;
    }

    private View permissionCard() {
        LinearLayout card = verticalCard(color("#FFF6E5"));
        card.addView(text("Permissions needed", 17, color("#2D2D44"), Typeface.BOLD));
        card.addView(text("Enable notifications, exact alarms, overlay access, and battery background access for reliable alarms.", 14, color("#6D6A80"), Typeface.NORMAL));
        Button grant = styledButton("Review permissions", color("#FFBE3B"), color("#2D2D44"));
        android.graphics.drawable.Drawable grIcon = ContextCompat.getDrawable(this, R.drawable.ic_settings);
        if (grIcon != null) {
            grIcon.setTint(color("#2D2D44"));
            grant.setCompoundDrawablesWithIntrinsicBounds(grIcon, null, null, null);
        }
        grant.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        card.addView(grant, marginTop(10));
        return card;
    }

    private View alarmCard(AlarmModel alarm) {
        LinearLayout card = verticalCard(alarm.isEnabled() ? color("#FFFFFF") : color("#F1F1F6"));
        card.setOnClickListener(v -> openEditor(alarm.getId()));
        card.setOnLongClickListener(v -> {
            confirmDelete(alarm);
            return true;
        });

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(top, matchWrap());

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        top.addView(details, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int primaryText = alarm.isEnabled() ? color("#2D2D44") : color("#A5A2B5");
        details.addView(text(formatAlarmTime(alarm), 34, primaryText, Typeface.BOLD));
        details.addView(text(alarm.getLabel(), 15, color("#6D6A80"), Typeface.NORMAL));

        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setChecked(alarm.isEnabled());
        toggle.setContentDescription("Enable alarm " + alarm.getLabel() + " at " + formatAlarmTime(alarm));
        toggle.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> toggleAlarm(alarm, isChecked));
        top.addView(toggle);

        // Meta row: repeat | difficulty | snooze | vibrate
        StringBuilder metaBuilder = new StringBuilder();
        metaBuilder.append(alarm.repeatText());
        metaBuilder.append("  |  ").append(alarm.getDifficulty().getDisplay());
        if (alarm.getSnoozeMinutes() > 0) {
            metaBuilder.append("  |  ⏱ ").append(alarm.snoozeText());
        }
        if (alarm.isVibrate()) {
            metaBuilder.append("  |  📳");
        }
        TextView meta = text(metaBuilder.toString(), 13, color("#6D6A80"), Typeface.NORMAL);
        card.addView(meta, marginTop(10));

        // Tap the card to edit (and delete from within the editor); long-press for a quick delete.
        return card;
    }

    // Proactively ask for the notification permission once on first launch. Without it the
    // alarm's foreground-service notification and full-screen intent cannot post, so the
    // math/dismiss screen may never appear. Exact-alarm/overlay/battery still live in Settings.
    private void maybeRequestStartupPermissions() {
        if (startupPermissionsRequested) {
            return;
        }
        startupPermissionsRequested = true;
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private boolean batteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        return manager != null && manager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void openEditor(int alarmId) {
        Intent intent = new Intent(this, EditAlarmActivity.class);
        if (alarmId != -1) {
            intent.putExtra(EditAlarmActivity.EXTRA_ALARM_ID, alarmId);
        }
        startActivityForResult(intent, REQUEST_EDIT_ALARM);
    }

    private void toggleAlarm(AlarmModel alarm, boolean enabled) {
        AlarmModel updated = alarm.withEnabled(enabled);
        repository.upsert(updated);
        if (enabled && exactAlarmGranted) {
            AlarmScheduler.schedule(this, updated);
        } else {
            AlarmScheduler.cancel(this, alarm.getId());
            AlarmScheduler.cancelSnooze(this, alarm.getId());
        }
        refreshState();
    }

    private void confirmDelete(AlarmModel alarm) {
        new AlertDialog.Builder(this)
                .setTitle("Delete alarm?")
                .setMessage("This alarm will be removed from the schedule.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AlarmScheduler.cancel(this, alarm.getId());
                    AlarmScheduler.cancelSnooze(this, alarm.getId());
                    repository.delete(alarm.getId());
                    refreshState();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatAlarmTime(AlarmModel alarm) {
        return appSettings.formatTime(LocalTime.of(alarm.getHour(), alarm.getMinute()));
    }

    // ─── UI helpers ──────────────────────────────────────────────

    private LinearLayout verticalCard(int backgroundColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(backgroundColor));
        card.setElevation(dp(2));
        return card;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private TextView title(String value, int sp) {
        return text(value, sp, color("#2D2D44"), Typeface.BOLD);
    }

    private MaterialTextView text(String value, int sp, int color, int style) {
        MaterialTextView textView = new MaterialTextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    private MaterialButton styledButton(String label, int bgColor, int textColor) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        button.setCornerRadius(dp(12));
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextSize(13);
        // Padding is handled by MaterialButton internally but can be tweaked
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setStateListAnimator(null);
        return button;
    }

    private android.view.View divider() {
        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(color("#E8E8EE"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.topMargin = dp(6);
        params.bottomMargin = dp(6);
        divider.setLayoutParams(params);
        return divider;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(dp);
        return params;
    }

    private LinearLayout.LayoutParams marginLeft(int dp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = dp(dp);
        return params;
    }

    private int constrainedWidth() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidthDp = (int) (dm.widthPixels / dm.density);
        if (screenWidthDp > MAX_CONTENT_WIDTH_DP + 40) {
            return dp(MAX_CONTENT_WIDTH_DP);
        }
        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    private int color(String hex) {
        return ThemeUtils.getColor(this, hex);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
