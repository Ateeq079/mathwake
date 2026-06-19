package com.mathwake.android.ui.onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mathwake.android.MainActivity;
import com.mathwake.android.alarm.AlarmScheduler;
import com.mathwake.android.data.AppSettingsRepository;
import com.mathwake.android.ui.ThemeUtils;

/**
 * First-run screen that walks the user through granting the permissions MathWake needs for
 * reliable alarms. Shown only until the user finishes it once (see
 * {@link AppSettingsRepository#hasCompletedOnboarding()}); afterwards permissions can still be
 * reviewed from Settings.
 */
public class OnboardingActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATIONS = 40;
    private static final int MAX_CONTENT_WIDTH_DP = 480;

    private AppSettingsRepository settings;
    private LinearLayout root;

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
        // Special-access permissions (exact alarm, overlay, battery) are granted in system
        // screens, so refresh the statuses every time we come back into view.
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
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(color("#F7F8FC"));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        FrameLayout wrapper = new FrameLayout(this);
        scrollView.addView(wrapper, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(24));

        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                constrainedWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.CENTER_HORIZONTAL;
        wrapper.addView(root, rootParams);

        root.addView(text("Welcome to MathWake", 30, color("#2D2D44"), Typeface.BOLD));
        root.addView(text(
                "To make sure your alarms always wake you up, MathWake needs a few permissions. "
                        + "Grant the ones you'd like — you can change them later in Settings.",
                15, color("#6D6A80"), Typeface.NORMAL), marginTop(8));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(6), dp(18), dp(6));
        card.setBackground(cardBackground(color("#FFFFFF")));
        card.setElevation(dp(2));
        root.addView(card, marginTop(24));

        card.addView(permissionRow(
                "Notifications",
                "Show alarm alerts and the wake-up screen.",
                notificationsGranted(),
                v -> requestNotifications()
        ));
        card.addView(divider());
        card.addView(permissionRow(
                "Exact Alarms",
                "Ring exactly at the time you set.",
                AlarmScheduler.canScheduleExact(this),
                v -> startActivity(AlarmScheduler.exactAlarmSettingsIntent(this))
        ));
        card.addView(divider());
        card.addView(permissionRow(
                "Display Over Other Apps",
                "Let the alarm appear above whatever you're doing.",
                overlayGranted(),
                v -> requestOverlayPermission()
        ));
        card.addView(divider());
        card.addView(permissionRow(
                "Battery Background",
                "Keep alarms reliable when the app is in the background.",
                batteryOptimizationIgnored(),
                v -> requestBatteryPermission()
        ));

        boolean allGranted = notificationsGranted()
                && AlarmScheduler.canScheduleExact(this)
                && overlayGranted()
                && batteryOptimizationIgnored();

        Button getStarted = styledButton(
                allGranted ? "Get Started" : "Continue",
                color("#6C63FF"), Color.WHITE);
        getStarted.setTextSize(16);
        getStarted.setOnClickListener(v -> finishOnboarding());
        LinearLayout.LayoutParams startParams = marginTop(28);
        startParams.height = dp(54);
        root.addView(getStarted, startParams);

        TextView skip = text(
                allGranted ? "All set!" : "Skip for now",
                14, color("#6D6A80"), Typeface.BOLD);
        skip.setGravity(Gravity.CENTER);
        skip.setPadding(dp(8), dp(14), dp(8), dp(8));
        if (!allGranted) {
            skip.setOnClickListener(v -> finishOnboarding());
        }
        root.addView(skip, marginTop(4));

        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(screen);
    }

    private void finishOnboarding() {
        settings.setOnboardingCompleted();
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private LinearLayout permissionRow(String title, String subtitle, boolean granted, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.addView(text(title, 16, color("#2D2D44"), Typeface.BOLD));
        copy.addView(text(subtitle, 12, color("#6D6A80"), Typeface.NORMAL));

        Button button = styledButton(
                granted ? "Granted" : "Allow",
                granted ? color("#43E97B") : color("#6C63FF"),
                granted ? color("#2D2D44") : Color.WHITE);
        button.setEnabled(!granted);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.setMarginStart(dp(12));
        row.addView(button, buttonParams);
        return row;
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
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setBackground(cardBackground(bgColor));
        button.setStateListAnimator(null);
        return button;
    }

    private View divider() {
        View divider = new View(this);
        divider.setBackgroundColor(color("#E8E8EE"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divider.setLayoutParams(params);
        return divider;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
