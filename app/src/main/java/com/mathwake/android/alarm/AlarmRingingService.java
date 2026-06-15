package com.mathwake.android.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;

import com.mathwake.android.R;
import com.mathwake.android.model.AlarmModel;
import com.mathwake.android.ui.ring.AlarmRingActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class AlarmRingingService extends Service {
    private static final String ACTION_DISMISS = "com.mathwake.android.action.DISMISS_ALARM";
    private static final String EXTRA_ALARM_ID = "extra_alarm_id";
    private static final String CHANNEL_ID = "mathwake_alarm_channel";

    private static final float VOLUME_START = 0.1f;
    private static final float VOLUME_END = 1.0f;
    private static final long VOLUME_RAMP_DURATION_MS = 30000L;
    private static final long VOLUME_RAMP_STEP_MS = 500L;

    private MediaPlayer player;
    private Vibrator vibrator;
    private Integer currentAlarmId;
    private Handler volumeHandler;
    private Runnable volumeRunnable;
    private long volumeRampStartTime;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISMISS.equals(intent.getAction())) {
            int targetId = intent.getIntExtra(EXTRA_ALARM_ID, -1);
            if (currentAlarmId != null && targetId == currentAlarmId) {
                stopRinging();
                stopSelf();
            }
            return START_STICKY;
        }

        String json = intent == null ? null : intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_JSON);
        if (json == null) {
            return START_NOT_STICKY;
        }

        boolean preview = intent.getBooleanExtra(AlarmScheduler.EXTRA_PREVIEW, false);
        AlarmModel alarm;
        try {
            alarm = AlarmModel.fromJson(new JSONObject(json));
        } catch (JSONException exception) {
            return START_NOT_STICKY;
        }
        currentAlarmId = alarm.getId();
        Notification notification = buildNotification(alarm, json, preview);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(alarm.getId(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(alarm.getId(), notification);
        }
        startRinging(alarm);
        openRingScreen(json, preview);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopRinging();
        super.onDestroy();
    }

    private Notification buildNotification(AlarmModel alarm, String json, boolean preview) {
        ensureNotificationChannel(this);

        Intent ringIntent = new Intent(this, AlarmRingActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ALARM_JSON, json)
                .putExtra(AlarmScheduler.EXTRA_PREVIEW, preview)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                this,
                alarm.getId(),
                ringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm_notification)
                .setContentTitle("MathWake Alarm")
                .setContentText("Solve the math problem to stop " + alarm.getLabel() + ".")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenIntent, true)
                .setContentIntent(fullScreenIntent)
                .build();
    }

    private void openRingScreen(String json, boolean preview) {
        Intent intent = new Intent(this, AlarmRingActivity.class)
                .putExtra(AlarmScheduler.EXTRA_ALARM_JSON, json)
                .putExtra(AlarmScheduler.EXTRA_PREVIEW, preview)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void startRinging(AlarmModel alarm) {
        // If a previous alarm is still ringing (a second alarm fired before the first was
        // dismissed), tear it down so the most recent alarm is the one that sounds.
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
        if (volumeHandler != null && volumeRunnable != null) {
            volumeHandler.removeCallbacks(volumeRunnable);
            volumeHandler = null;
            volumeRunnable = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }

        // --- Audio ---
        if (player == null) {
            try {
                Uri sound = null;

                // Try custom ringtone first
                String ringtoneUriString = alarm.getRingtoneUri();
                if (ringtoneUriString != null && !ringtoneUriString.isEmpty()) {
                    sound = Uri.parse(ringtoneUriString);
                }

                // Fall back to system defaults
                if (sound == null) {
                    sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                }
                if (sound == null) {
                    sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }

                player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                player.setDataSource(this, sound);
                player.setLooping(true);
                player.prepare();

                // Start with low volume for gradual ramp
                player.setVolume(VOLUME_START, VOLUME_START);
                player.start();

                // Gradual volume increase
                startVolumeRamp();
            } catch (Exception ignored) {
                player = null;
            }
        }

        // --- Vibration (only if enabled for this alarm) ---
        if (alarm.isVibrate()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                vibrator = manager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }

            long[] pattern = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void startVolumeRamp() {
        volumeHandler = new Handler(Looper.getMainLooper());
        volumeRampStartTime = System.currentTimeMillis();
        volumeRunnable = new Runnable() {
            @Override
            public void run() {
                if (player == null) {
                    return;
                }
                long elapsed = System.currentTimeMillis() - volumeRampStartTime;
                float progress = Math.min(1f, (float) elapsed / VOLUME_RAMP_DURATION_MS);
                float volume = VOLUME_START + (VOLUME_END - VOLUME_START) * progress;
                try {
                    player.setVolume(volume, volume);
                } catch (IllegalStateException ignored) {
                    return;
                }
                if (progress < 1f) {
                    volumeHandler.postDelayed(this, VOLUME_RAMP_STEP_MS);
                }
            }
        };
        volumeHandler.postDelayed(volumeRunnable, VOLUME_RAMP_STEP_MS);
    }

    private void stopRinging() {
        if (volumeHandler != null && volumeRunnable != null) {
            volumeHandler.removeCallbacks(volumeRunnable);
            volumeHandler = null;
            volumeRunnable = null;
        }
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    public static void start(Context context, String alarmJson, boolean preview) {
        Intent intent = new Intent(context, AlarmRingingService.class)
                .putExtra(AlarmScheduler.EXTRA_ALARM_JSON, alarmJson)
                .putExtra(AlarmScheduler.EXTRA_PREVIEW, preview);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void dismiss(Context context, int alarmId) {
        Intent intent = new Intent(context, AlarmRingingService.class)
                .setAction(ACTION_DISMISS)
                .putExtra(EXTRA_ALARM_ID, alarmId);
        context.startService(intent);
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.channel_alarm_description));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }
}
