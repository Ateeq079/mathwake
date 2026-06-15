package com.mathwake.android.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mathwake.android.MainActivity;
import com.mathwake.android.R;

/**
 * Fires when a countdown timer reaches zero. Scheduled through AlarmManager so the alert
 * arrives even if the app has been backgrounded or killed. The alerting (sound + vibration)
 * is carried by a high-importance notification channel, so no service is required for the
 * one-shot timer beep.
 */
public class TimerReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "mathwake_timer_channel";
    public static final int NOTIFICATION_ID = 90210;
    public static final int REQUEST_CODE = 77001;

    @Override
    public void onReceive(Context context, Intent intent) {
        ensureChannel(context);

        android.app.PendingIntent contentIntent = android.app.PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                new Intent(context, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm_notification)
                .setContentTitle("Timer Finished")
                .setContentText("Your countdown timer has ended.")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifies you when a countdown timer ends.");
        channel.enableVibration(true);
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) {
            sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        if (sound != null) {
            channel.setSound(sound, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
        manager.createNotificationChannel(channel);
    }
}
