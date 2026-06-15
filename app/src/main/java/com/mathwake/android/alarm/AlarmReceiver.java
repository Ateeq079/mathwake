package com.mathwake.android.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mathwake.android.data.AlarmRepository;
import com.mathwake.android.model.AlarmModel;

import org.json.JSONException;
import org.json.JSONObject;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String json = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_JSON);
        if (json == null) {
            return;
        }

        AlarmModel alarm;
        try {
            alarm = AlarmModel.fromJson(new JSONObject(json));
        } catch (JSONException exception) {
            return;
        }
        boolean preview = intent.getBooleanExtra(AlarmScheduler.EXTRA_PREVIEW, false);
        boolean isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false);
        int snoozeCount = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_COUNT, 0);
        AlarmRepository repository = new AlarmRepository(context);

        // Only reschedule or disable the alarm for non-preview, non-snooze firings.
        // Snooze rings are one-off follow-ups; the parent alarm was already handled.
        if (!preview && !isSnooze) {
            if (alarm.isRepeating()) {
                AlarmScheduler.schedule(context, alarm);
            } else {
                repository.disableOneTimeAlarm(alarm.getId());
            }
        }

        AlarmRingingService.start(context, json, preview, snoozeCount);
    }
}
