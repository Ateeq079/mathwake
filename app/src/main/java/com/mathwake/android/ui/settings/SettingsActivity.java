package com.mathwake.android.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;

import com.mathwake.android.ui.ThemeUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.mathwake.android.alarm.AlarmScheduler;
import com.mathwake.android.data.AppSettingsRepository;
import com.mathwake.android.model.AlarmDifficulty;

public class SettingsActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATIONS = 30;
    private static final int MAX_CONTENT_WIDTH_DP = 480;
    private static final int[] SNOOZE_VALUES = {0, 1, 5, 10, 15, 20, 30};
    private static final String[] SNOOZE_LABELS = {"Off", "1 min", "5 min", "10 min", "15 min", "20 min", "30 min"};

    private AppSettingsRepository settings;
    private LinearLayout root;
    private RadioGroup timeFormatGroup;
    private RadioGroup darkModeGroup;
    private RadioGroup difficultyGroup;
    private Spinner snoozeSpinner;
    private Switch vibrateSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(
                AppSettingsRepository.getNightMode(new AppSettingsRepository(this).getDarkMode()));
        super.onCreate(savedInstanceState);
        settings = new AppSettingsRepository(this);
        buildLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildLayout();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            buildLayout();
        }
    }

    private void buildLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color("#F7F8FC"));

        FrameLayout wrapper = new FrameLayout(this);
        scrollView.addView(wrapper, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));

        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                constrainedWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.CENTER_HORIZONTAL;
        wrapper.addView(root, rootParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, matchWrap());

        TextView title = text("Settings", 30, color("#2D2D44"), Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Changes save automatically as they are made, so there is no Done button.
        root.addView(text("Changes save automatically", 14, color("#6D6A80"), Typeface.NORMAL), marginTop(4));

        buildAppSection();
        buildPermissionSection();
        buildAlarmSection();
        buildDarkModeSection();
        buildSupportSection();
        buildAboutSection();

        setContentView(scrollView);
    }

    private void buildAppSection() {
        LinearLayout card = sectionCard("App");
        card.addView(text("Time Format", 16, color("#2D2D44"), Typeface.BOLD));
        timeFormatGroup = new RadioGroup(this);
        timeFormatGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton twelve = option("12-hour", 100);
        RadioButton twentyFour = option("24-hour", 101);
        timeFormatGroup.addView(twelve);
        timeFormatGroup.addView(twentyFour);
        timeFormatGroup.check(settings.is24HourTime() ? 101 : 100);
        timeFormatGroup.setOnCheckedChangeListener((group, checkedId) -> settings.set24HourTime(checkedId == 101));
        card.addView(timeFormatGroup, marginTop(8));
    }

    private void buildPermissionSection() {
        LinearLayout card = sectionCard("App Permissions");
        card.addView(permissionRow(
                "Notifications",
                notificationsGranted() ? "Granted" : "Needed for alarm alerts",
                notificationsGranted(),
                v -> requestNotifications()
        ));
        card.addView(divider());
        card.addView(permissionRow(
                "Exact Alarms",
                AlarmScheduler.canScheduleExact(this) ? "Granted" : "Needed for on-time alarms",
                AlarmScheduler.canScheduleExact(this),
                v -> startActivity(AlarmScheduler.exactAlarmSettingsIntent(this))
        ));
        card.addView(divider());
        card.addView(permissionRow(
                "Display Over Other Apps",
                overlayGranted() ? "Granted" : "Helps alarms appear above apps",
                overlayGranted(),
                v -> requestOverlayPermission()
        ));
        card.addView(divider());
        card.addView(permissionRow(
                "Battery Background",
                batteryOptimizationIgnored() ? "Allowed" : "Allow reliable background alarms",
                batteryOptimizationIgnored(),
                v -> requestBatteryPermission()
        ));
    }

    private void buildAlarmSection() {
        LinearLayout card = sectionCard("Alarm Settings");

        LinearLayout vibrateRow = rowShell("Default Vibrate", "Used when creating a new alarm");
        vibrateSwitch = new Switch(this);
        vibrateSwitch.setChecked(settings.isDefaultVibrate());
        vibrateSwitch.setContentDescription("Default vibrate for new alarms");
        vibrateSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setDefaultVibrate(isChecked));
        vibrateRow.addView(vibrateSwitch);
        card.addView(vibrateRow);
        card.addView(divider());

        LinearLayout snoozeRow = rowShell("Default Snooze", "Used when creating a new alarm");
        snoozeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SNOOZE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snoozeSpinner.setAdapter(adapter);
        snoozeSpinner.setSelection(snoozeIndex(settings.getDefaultSnoozeMinutes()));
        snoozeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                settings.setDefaultSnoozeMinutes(SNOOZE_VALUES[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        snoozeRow.addView(snoozeSpinner);
        card.addView(snoozeRow);
        card.addView(divider());

        card.addView(text("Default Difficulty", 16, color("#2D2D44"), Typeface.BOLD));
        difficultyGroup = new RadioGroup(this);
        difficultyGroup.setOrientation(RadioGroup.HORIZONTAL);
        addDifficultyOption(AlarmDifficulty.EASY);
        addDifficultyOption(AlarmDifficulty.MEDIUM);
        addDifficultyOption(AlarmDifficulty.HARD);
        difficultyGroup.check(settings.getDefaultDifficulty().ordinal() + 200);
        difficultyGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (AlarmDifficulty difficulty : AlarmDifficulty.values()) {
                if (difficulty.ordinal() + 200 == checkedId) {
                    settings.setDefaultDifficulty(difficulty);
                    break;
                }
            }
        });
        card.addView(difficultyGroup, marginTop(8));
    }

    private void buildDarkModeSection() {
        LinearLayout card = sectionCard("Dark Mode");
        darkModeGroup = new RadioGroup(this);
        darkModeGroup.setOrientation(RadioGroup.HORIZONTAL);
        darkModeGroup.addView(option("System", 300));
        darkModeGroup.addView(option("Light", 301));
        darkModeGroup.addView(option("Dark", 302));

        String mode = settings.getDarkMode();
        int checked = 300;
        if (AppSettingsRepository.DARK_MODE_LIGHT.equals(mode)) {
            checked = 301;
        } else if (AppSettingsRepository.DARK_MODE_DARK.equals(mode)) {
            checked = 302;
        }
        darkModeGroup.check(checked);
        darkModeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedMode;
            if (checkedId == 301) {
                selectedMode = AppSettingsRepository.DARK_MODE_LIGHT;
            } else if (checkedId == 302) {
                selectedMode = AppSettingsRepository.DARK_MODE_DARK;
            } else {
                selectedMode = AppSettingsRepository.DARK_MODE_SYSTEM;
            }
            settings.setDarkMode(selectedMode);
            AppCompatDelegate.setDefaultNightMode(AppSettingsRepository.getNightMode(selectedMode));
            recreate();
        });
        card.addView(darkModeGroup);
    }

    private void buildSupportSection() {
        LinearLayout card = sectionCard("Support");
        card.addView(actionRow("Share with Friend", "Send MathWake to someone", v -> shareApp()));
        card.addView(divider());
        card.addView(actionRow("FAQ", "Common permission and alarm questions", v -> startActivity(new Intent(this, FaqActivity.class))));
    }

    private void buildAboutSection() {
        LinearLayout card = sectionCard("About");
        card.addView(actionRow("Terms of Service", "Usage terms for MathWake", v -> startActivity(new Intent(this, TermsActivity.class))));
        card.addView(divider());
        card.addView(actionRow("Privacy Policy", "How app data is handled", v -> startActivity(new Intent(this, PrivacyPolicyActivity.class))));
    }

    private LinearLayout permissionRow(String title, String subtitle, boolean granted, android.view.View.OnClickListener listener) {
        LinearLayout row = rowShell(title, subtitle);
        Button button = styledButton(granted ? "Granted" : "Open", granted ? color("#43E97B") : color("#6C63FF"), granted ? color("#2D2D44") : Color.WHITE);
        button.setEnabled(!granted);
        button.setOnClickListener(listener);
        row.addView(button);
        return row;
    }

    private LinearLayout actionRow(String title, String subtitle, android.view.View.OnClickListener listener) {
        LinearLayout row = rowShell(title, subtitle);
        Button button = styledButton("Open", color("#6C63FF"), Color.WHITE);
        button.setOnClickListener(listener);
        row.addView(button);
        return row;
    }

    private LinearLayout rowShell(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(text(title, 16, color("#2D2D44"), Typeface.BOLD));
        copy.addView(text(subtitle, 12, color("#6D6A80"), Typeface.NORMAL));
        return row;
    }

    private LinearLayout sectionCard(String title) {
        root.addView(text(title, 18, color("#2D2D44"), Typeface.BOLD), marginTop(22));
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(cardBackground(color("#FFFFFF")));
        card.setElevation(dp(2));
        root.addView(card, marginTop(8));
        return card;
    }

    private RadioButton option(String label, int id) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(label);
        button.setTextSize(14);
        button.setPadding(dp(2), dp(6), dp(10), dp(6));
        return button;
    }

    private void addDifficultyOption(AlarmDifficulty difficulty) {
        RadioButton button = option(difficulty.getDisplay(), difficulty.ordinal() + 200);
        difficultyGroup.addView(button);
    }

    private int snoozeIndex(int minutes) {
        for (int i = 0; i < SNOOZE_VALUES.length; i++) {
            if (SNOOZE_VALUES[i] == minutes) {
                return i;
            }
        }
        return 3;
    }

    private boolean notificationsGranted() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean overlayGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private boolean batteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        return manager != null && manager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !notificationsGranted()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        }
    }

    private void requestBatteryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !batteryOptimizationIgnored()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void shareApp() {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "Try MathWake, an alarm app that makes you solve math to wake up.");
        startActivity(Intent.createChooser(intent, "Share MathWake"));
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    private Button styledButton(String label, int bgColor, int textColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextSize(13);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        button.setBackground(cardBackground(bgColor));
        button.setStateListAnimator(null);
        return button;
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

    private GradientDrawable cardBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams params = matchWrap();
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
