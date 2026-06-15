package com.mathwake.android.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.mathwake.android.model.AlarmModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AlarmRepository {
    private static final String STORE_NAME = "mathwake_store";
    private static final String KEY_ALARMS = "alarms";

    private final SharedPreferences prefs;

    public AlarmRepository(Context context) {
        prefs = context.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE);
    }

    public List<AlarmModel> getAlarms() {
        String raw = prefs.getString(KEY_ALARMS, null);
        if (raw == null || raw.trim().isEmpty()) {
            // Fresh install: start with an empty list so the app never rings an
            // alarm the user did not create. The empty-state UI guides them to add one.
            List<AlarmModel> empty = new ArrayList<>();
            saveAlarms(empty);
            return empty;
        }

        try {
            JSONArray json = new JSONArray(raw);
            List<AlarmModel> alarms = new ArrayList<>();
            for (int index = 0; index < json.length(); index++) {
                alarms.add(AlarmModel.fromJson(json.getJSONObject(index)));
            }
            return alarms;
        } catch (JSONException exception) {
            // Corrupt store: reset to empty rather than resurrecting demo alarms.
            List<AlarmModel> empty = new ArrayList<>();
            saveAlarms(empty);
            return empty;
        }
    }

    public AlarmModel getAlarm(int id) {
        for (AlarmModel alarm : getAlarms()) {
            if (alarm.getId() == id) {
                return alarm;
            }
        }
        return null;
    }

    public void saveAlarms(List<AlarmModel> alarms) {
        JSONArray payload = new JSONArray();
        for (AlarmModel alarm : alarms) {
            payload.put(alarm.toJson());
        }
        prefs.edit().putString(KEY_ALARMS, payload.toString()).apply();
    }

    public void upsert(AlarmModel alarm) {
        List<AlarmModel> updated = new ArrayList<>(getAlarms());
        int targetIndex = -1;
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).getId() == alarm.getId()) {
                targetIndex = index;
                break;
            }
        }

        if (targetIndex == -1) {
            updated.add(alarm);
        } else {
            updated.set(targetIndex, alarm);
        }
        saveAlarms(updated);
    }

    public void delete(int id) {
        List<AlarmModel> updated = new ArrayList<>();
        for (AlarmModel alarm : getAlarms()) {
            if (alarm.getId() != id) {
                updated.add(alarm);
            }
        }
        saveAlarms(updated);
    }

    public void disableOneTimeAlarm(int id) {
        List<AlarmModel> updated = new ArrayList<>();
        for (AlarmModel alarm : getAlarms()) {
            if (alarm.getId() == id && !alarm.isRepeating()) {
                updated.add(alarm.withEnabled(false));
            } else {
                updated.add(alarm);
            }
        }
        saveAlarms(updated);
    }
}
