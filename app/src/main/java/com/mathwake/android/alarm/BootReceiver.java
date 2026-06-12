package com.mathwake.android.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mathwake.android.data.AlarmRepository;
import com.mathwake.android.model.AlarmModel;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        AlarmRepository repository = new AlarmRepository(context);
        for (AlarmModel alarm : repository.getAlarms()) {
            if (alarm.isEnabled()) {
                AlarmScheduler.schedule(context, alarm);
            }
        }
    }
}
