package com.mathwake.android.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.mathwake.android.MainActivity;
import com.mathwake.android.model.AlarmModel;

import java.time.ZoneId;

public final class AlarmScheduler {
    public static final String EXTRA_ALARM_JSON = "extra_alarm_json";
    public static final String EXTRA_PREVIEW = "extra_preview";
    public static final String EXTRA_IS_SNOOZE = "extra_is_snooze";
    public static final String EXTRA_SNOOZE_COUNT = "extra_snooze_count";

    /** Maximum times a single alarm ring can be snoozed before the snooze option is withheld. */
    public static final int MAX_SNOOZES = 3;

    private static final int SNOOZE_REQUEST_CODE_OFFSET = 1000000;

    private AlarmScheduler() {
    }

    public static void schedule(Context context, AlarmModel alarm) {
        schedule(context, alarm, false);
    }

    public static void schedule(Context context, AlarmModel alarm, boolean preview) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtMillis = alarm.nextOccurrence()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        PendingIntent infoIntent = PendingIntent.getActivity(
                context,
                alarm.getId(),
                new Intent(context, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent receiverIntent = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ALARM_JSON, alarm.toJson().toString())
                .putExtra(EXTRA_PREVIEW, preview);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                alarm.getId(),
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, infoIntent), alarmIntent);
    }

    public static void scheduleSnooze(Context context, AlarmModel alarm, int snoozeCount) {
        if (alarm.getSnoozeMinutes() <= 0) {
            return;
        }

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtMillis = System.currentTimeMillis() + (alarm.getSnoozeMinutes() * 60L * 1000L);

        PendingIntent infoIntent = PendingIntent.getActivity(
                context,
                alarm.getId() + SNOOZE_REQUEST_CODE_OFFSET,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent receiverIntent = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ALARM_JSON, alarm.toJson().toString())
                .putExtra(EXTRA_IS_SNOOZE, true)
                .putExtra(EXTRA_SNOOZE_COUNT, snoozeCount);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                alarm.getId() + SNOOZE_REQUEST_CODE_OFFSET,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, infoIntent), alarmIntent);
    }

    public static void schedulePreview(Context context, AlarmModel alarm) {
        AlarmModel previewAlarm = AlarmModel.previewFrom(alarm);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtMillis = System.currentTimeMillis() + 3000L;

        PendingIntent infoIntent = PendingIntent.getActivity(
                context,
                previewAlarm.getId(),
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent receiverIntent = new Intent(context, AlarmReceiver.class)
                .putExtra(EXTRA_ALARM_JSON, previewAlarm.toJson().toString())
                .putExtra(EXTRA_PREVIEW, true);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(
                context,
                previewAlarm.getId(),
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, infoIntent), alarmIntent);
    }

    public static void cancel(Context context, int alarmId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = PendingIntent.getBroadcast(
                context,
                alarmId,
                new Intent(context, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        manager.cancel(intent);
    }

    public static void cancelSnooze(Context context, int alarmId) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = PendingIntent.getBroadcast(
                context,
                alarmId + SNOOZE_REQUEST_CODE_OFFSET,
                new Intent(context, AlarmReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        manager.cancel(intent);
    }

    public static boolean canScheduleExact(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return manager.canScheduleExactAlarms();
    }

    public static Intent exactAlarmSettingsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }
}
