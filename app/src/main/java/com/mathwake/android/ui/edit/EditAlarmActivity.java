package com.mathwake.android.ui.edit;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import com.mathwake.android.ui.ThemeUtils;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mathwake.android.R;
import com.mathwake.android.alarm.AlarmScheduler;
import com.mathwake.android.data.AppSettingsRepository;
import com.mathwake.android.data.AlarmRepository;
import com.mathwake.android.model.AlarmDifficulty;
import com.mathwake.android.model.AlarmModel;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

public class EditAlarmActivity extends AppCompatActivity {
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";
    private static final int REQUEST_RINGTONE = 20;
    private static final int MAX_CONTENT_WIDTH_DP = 480;

    private AlarmRepository repository;
    private AppSettingsRepository appSettings;
    private AlarmModel initial;
    private int hour;
    private int minute;
    private EditText labelInput;
    private MaterialTextView[] dayButtons = new MaterialTextView[7];
    private final boolean[] daySelected = new boolean[7];
    private RadioGroup difficultyGroup;
    private TimePicker timePicker;
    private MaterialSwitch vibrateSwitch;
    private MaterialTextView ringtoneLabel;
    private Spinner snoozeSpinner;
    private String selectedRingtoneUri;

    private static final int[] SNOOZE_VALUES = {0, 1, 5, 10, 15, 20, 30};
    private static final String[] SNOOZE_LABELS = {"Off", "1 min", "5 min", "10 min", "15 min", "20 min", "30 min"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(
                AppSettingsRepository.getNightMode(new AppSettingsRepository(this).getDarkMode()));
        super.onCreate(savedInstanceState);
        repository = new AlarmRepository(this);
        appSettings = new AppSettingsRepository(this);

        int alarmId = getIntent().getIntExtra(EXTRA_ALARM_ID, -1);
        AlarmModel existing = alarmId == -1 ? null : repository.getAlarm(alarmId);
        LocalTime fallback = LocalTime.now().plusMinutes(1);
        initial = existing == null
                ? new AlarmModel(
                        AlarmModel.newId(),
                        fallback.getHour(),
                        fallback.getMinute(),
                        "Wake Up!",
                        true,
                        new HashSet<>(),
                        appSettings.getDefaultDifficulty(),
                        appSettings.getDefaultSnoozeMinutes(),
                        appSettings.isDefaultVibrate(),
                        null
                )
                : existing;
        hour = initial.getHour();
        minute = initial.getMinute();
        selectedRingtoneUri = initial.getRingtoneUri();

        buildLayout(existing != null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RINGTONE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                selectedRingtoneUri = uri.toString();
                String title = RingtoneManager.getRingtone(this, uri).getTitle(this);
                ringtoneLabel.setText(title != null ? title : "Custom");
            } else {
                selectedRingtoneUri = null;
                ringtoneLabel.setText("Default");
            }
        }
    }

    private void buildLayout(boolean editing) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(color("#F7F8FC"));
        scrollView.setFillViewport(true);

        FrameLayout wrapper = new FrameLayout(this);
        scrollView.addView(wrapper, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));

        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                constrainedWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.CENTER_HORIZONTAL;
        wrapper.addView(root, rootParams);

        // Title
        MaterialTextView title = text(editing ? "Edit Alarm" : "New Alarm", 28, color("#2D2D44"), Typeface.BOLD);
        root.addView(title);

        // Time Picker card (inline slot/spinner mode)
        timePicker = new TimePicker(new android.view.ContextThemeWrapper(this, R.style.SpinnerTimePicker), null, 0);
        timePicker.setIs24HourView(appSettings.is24HourTime());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }

        LinearLayout timePickerCard = new LinearLayout(this);
        timePickerCard.setOrientation(LinearLayout.VERTICAL);
        timePickerCard.setGravity(Gravity.CENTER);
        timePickerCard.setPadding(dp(10), dp(10), dp(10), dp(10));
        timePickerCard.setBackground(cardBackground(color("#FFFFFF")));
        timePickerCard.setElevation(dp(2));
        timePickerCard.addView(timePicker);
        root.addView(timePickerCard, marginTop(20));

        // Label input
        root.addView(sectionTitle("Label"), marginTop(20));
        labelInput = new EditText(this);
        labelInput.setHint("Label");
        labelInput.setSingleLine(true);
        labelInput.setText(initial.getLabel());
        labelInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        labelInput.setBackground(cardBackground(color("#FFFFFF")));
        root.addView(labelInput, marginTop(8));

        // Repeat days - circular selectors
        root.addView(sectionTitle("Repeat"), marginTop(20));
        LinearLayout repeatRow = new LinearLayout(this);
        repeatRow.setOrientation(LinearLayout.HORIZONTAL);
        repeatRow.setGravity(Gravity.CENTER);
        String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        Set<Integer> repeatDays = initial.getRepeatDays();
        for (int index = 0; index < days.length; index++) {
            final int dayIndex = index;
            daySelected[index] = repeatDays.contains(index + 1);

            MaterialTextView dayBtn = new MaterialTextView(this);
            dayBtn.setText(days[index]);
            dayBtn.setTextSize(14);
            dayBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            dayBtn.setGravity(Gravity.CENTER);
            // Disambiguate the single-letter labels for screen readers (M/T/W/T/F/S/S repeat).
            dayBtn.setContentDescription(dayNames[index]);
            // 48dp keeps the tap target at the accessibility minimum.
            int size = dp(48);
            LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(size, size);
            dayParams.setMargins(dp(3), 0, dp(3), 0);
            dayBtn.setLayoutParams(dayParams);
            updateDayButtonStyle(dayBtn, daySelected[index]);

            dayBtn.setOnClickListener(v -> {
                daySelected[dayIndex] = !daySelected[dayIndex];
                updateDayButtonStyle(dayBtn, daySelected[dayIndex]);
            });

            dayButtons[index] = dayBtn;
            repeatRow.addView(dayBtn);
        }
        root.addView(repeatRow, marginTop(8));

        // Math Difficulty
        root.addView(sectionTitle("Math Difficulty"), marginTop(20));
        difficultyGroup = new RadioGroup(this);
        difficultyGroup.setOrientation(RadioGroup.HORIZONTAL);
        difficultyGroup.setGravity(Gravity.CENTER);
        addDifficulty(AlarmDifficulty.EASY);
        addDifficulty(AlarmDifficulty.MEDIUM);
        addDifficulty(AlarmDifficulty.HARD);
        difficultyGroup.check(initial.getDifficulty().ordinal() + 100);
        root.addView(difficultyGroup, marginTop(8));

        // ─── Settings Card ──────────────────────────────
        LinearLayout settingsCard = verticalCard(color("#FFFFFF"));
        settingsCard.setElevation(dp(2));
        root.addView(settingsCard, marginTop(20));

        // Vibrate toggle
        LinearLayout vibrateRow = settingRow("Vibrate", "Phone vibrates when alarm rings");
        vibrateSwitch = new MaterialSwitch(this);
        vibrateSwitch.setChecked(initial.isVibrate());
        vibrateSwitch.setContentDescription("Vibrate when this alarm rings");
        vibrateRow.addView(vibrateSwitch);
        settingsCard.addView(vibrateRow);

        // Divider
        settingsCard.addView(divider());

        // Snooze duration
        LinearLayout snoozeRow = new LinearLayout(this);
        snoozeRow.setOrientation(LinearLayout.HORIZONTAL);
        snoozeRow.setGravity(Gravity.CENTER_VERTICAL);
        snoozeRow.setPadding(0, dp(10), 0, dp(10));

        LinearLayout snoozeInfo = new LinearLayout(this);
        snoozeInfo.setOrientation(LinearLayout.VERTICAL);
        snoozeRow.addView(snoozeInfo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        snoozeInfo.addView(text("Snooze", 16, color("#2D2D44"), Typeface.BOLD));
        snoozeInfo.addView(text("Delay alarm by selected duration", 12, color("#6D6A80"), Typeface.NORMAL));

        snoozeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SNOOZE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snoozeSpinner.setAdapter(adapter);

        // Set initial selection
        int snoozeIndex = 0;
        for (int i = 0; i < SNOOZE_VALUES.length; i++) {
            if (SNOOZE_VALUES[i] == initial.getSnoozeMinutes()) {
                snoozeIndex = i;
                break;
            }
        }
        snoozeSpinner.setSelection(snoozeIndex);
        snoozeRow.addView(snoozeSpinner);
        settingsCard.addView(snoozeRow);

        // Divider
        settingsCard.addView(divider());

        // Ringtone picker
        LinearLayout ringtoneRow = new LinearLayout(this);
        ringtoneRow.setOrientation(LinearLayout.HORIZONTAL);
        ringtoneRow.setGravity(Gravity.CENTER_VERTICAL);
        ringtoneRow.setPadding(0, dp(10), 0, dp(10));

        LinearLayout ringtoneInfo = new LinearLayout(this);
        ringtoneInfo.setOrientation(LinearLayout.VERTICAL);
        ringtoneRow.addView(ringtoneInfo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ringtoneInfo.addView(text("Ringtone", 16, color("#2D2D44"), Typeface.BOLD));

        ringtoneLabel = text(getRingtoneName(), 12, color("#6C63FF"), Typeface.NORMAL);
        ringtoneInfo.addView(ringtoneLabel);

        MaterialButton ringtoneBtn = styledButton("Change", color("#6C63FF"), Color.WHITE);
        ringtoneBtn.setOnClickListener(v -> openRingtonePicker());
        ringtoneRow.addView(ringtoneBtn);
        settingsCard.addView(ringtoneRow);

        // ─── Action buttons ──────────────────────────────

        MaterialButton save = styledButton(editing ? "Update Alarm" : "Set Alarm", color("#6C63FF"), Color.WHITE);
        save.setOnClickListener(v -> saveAlarm());
        LinearLayout.LayoutParams saveParams = marginTop(28);
        saveParams.height = dp(52);
        root.addView(save, saveParams);

        MaterialButton cancel = styledButton("Cancel", color("#F1F1F6"), color("#2D2D44"));
        cancel.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams cancelParams = marginTop(8);
        cancelParams.height = dp(52);
        root.addView(cancel, cancelParams);

        if (editing) {
            MaterialButton delete = styledButton("Delete Alarm", color("#FF6584"), Color.WHITE);
            delete.setOnClickListener(v -> confirmDelete());
            LinearLayout.LayoutParams deleteParams = marginTop(8);
            deleteParams.height = dp(52);
            root.addView(delete, deleteParams);
        }

        setContentView(scrollView);
    }

    private void addDifficulty(AlarmDifficulty difficulty) {
        RadioButton button = new RadioButton(this);
        button.setId(difficulty.ordinal() + 100);
        button.setText(difficulty.getDisplay());
        button.setTextSize(15);
        button.setPadding(dp(4), dp(8), dp(12), dp(8));
        difficultyGroup.addView(button);
    }

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        if (selectedRingtoneUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedRingtoneUri));
        } else {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        }
        startActivityForResult(intent, REQUEST_RINGTONE);
    }

    private String getRingtoneName() {
        if (selectedRingtoneUri == null || selectedRingtoneUri.isEmpty()) {
            return "Default";
        }
        try {
            android.media.Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(selectedRingtoneUri));
            if (ringtone != null) {
                String title = ringtone.getTitle(this);
                return title != null ? title : "Custom";
            }
        } catch (Exception ignored) {
        }
        return "Custom";
    }

    private void saveAlarm() {
        Set<Integer> repeat = new HashSet<>();
        for (int index = 0; index < daySelected.length; index++) {
            if (daySelected[index]) {
                repeat.add(index + 1);
            }
        }

        AlarmDifficulty difficulty = AlarmDifficulty.MEDIUM;
        int checked = difficultyGroup.getCheckedRadioButtonId();
        for (AlarmDifficulty candidate : AlarmDifficulty.values()) {
            if (candidate.ordinal() + 100 == checked) {
                difficulty = candidate;
                break;
            }
        }

        int selectedHour;
        int selectedMinute;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            selectedHour = timePicker.getHour();
            selectedMinute = timePicker.getMinute();
        } else {
            selectedHour = timePicker.getCurrentHour();
            selectedMinute = timePicker.getCurrentMinute();
        }

        int snoozeMinutes = SNOOZE_VALUES[snoozeSpinner.getSelectedItemPosition()];
        boolean vibrate = vibrateSwitch.isChecked();

        AlarmModel updated = initial.withDetails(
                selectedHour,
                selectedMinute,
                labelInput.getText().toString(),
                repeat,
                difficulty,
                snoozeMinutes,
                vibrate,
                selectedRingtoneUri
        );

        repository.upsert(updated);
        if (updated.isEnabled() && AlarmScheduler.canScheduleExact(this)) {
            AlarmScheduler.schedule(this, updated);
        } else {
            AlarmScheduler.cancel(this, updated.getId());
        }

        setResult(RESULT_OK);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete alarm?")
                .setMessage("This alarm will be removed from the schedule.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AlarmScheduler.cancel(this, initial.getId());
                    AlarmScheduler.cancelSnooze(this, initial.getId());
                    repository.delete(initial.getId());
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── UI helpers ──────────────────────────────────────────────

    private void updateDayButtonStyle(MaterialTextView btn, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (selected) {
            bg.setColor(color("#6C63FF"));
            btn.setTextColor(Color.WHITE);
        } else {
            bg.setColor(color("#F1F1F6"));
            btn.setTextColor(color("#6D6A80"));
        }
        btn.setBackground(bg);
    }

    private LinearLayout settingRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        info.addView(text(title, 16, color("#2D2D44"), Typeface.BOLD));
        info.addView(text(subtitle, 12, color("#6D6A80"), Typeface.NORMAL));

        return row;
    }

    private android.view.View divider() {
        android.view.View divider = new android.view.View(this);
        divider.setBackgroundColor(color("#E8E8EE"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.topMargin = dp(4);
        params.bottomMargin = dp(4);
        divider.setLayoutParams(params);
        return divider;
    }

    private LinearLayout verticalCard(int backgroundColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(backgroundColor));
        return card;
    }

    private MaterialTextView sectionTitle(String value) {
        return text(value, 16, color("#2D2D44"), Typeface.BOLD);
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
        button.setTextSize(14);
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setStateListAnimator(null);
        return button;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(dp);
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
