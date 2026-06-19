package com.mathwake.android.ui.ring;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.mathwake.android.ui.ThemeUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import com.mathwake.android.MainActivity;

public class AlarmSuccessActivity extends AppCompatActivity {
    public static final String EXTRA_LABEL = "extra_label";
    public static final String EXTRA_ATTEMPTS = "extra_attempts";
    public static final String EXTRA_DIFFICULTY = "extra_difficulty";
    private static final int MAX_CONTENT_WIDTH_DP = 480;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Fixed vibrant gradient with white text; keep it light-themed regardless of
        // the app/system dark-mode setting so it always renders correctly.
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        String label = getIntent().getStringExtra(EXTRA_LABEL);
        int attempts = getIntent().getIntExtra(EXTRA_ATTEMPTS, 1);
        String difficulty = getIntent().getStringExtra(EXTRA_DIFFICULTY);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{color("#43E97B"), color("#38F9D7")}
        ));
        scrollView.addView(wrapper, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(32), dp(32), dp(32), dp(32));

        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                constrainedWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.CENTER;
        wrapper.addView(root, rootParams);

        MaterialTextView title = text("Alarm Dismissed", 32, Color.WHITE, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        MaterialTextView message = text("You solved " + safe(label) + " in " + attempts + " attempt" + (attempts == 1 ? "." : "s."), 16, Color.WHITE, Typeface.NORMAL);
        message.setGravity(Gravity.CENTER);
        root.addView(message, marginTop(12));

        MaterialTextView stats = text("Difficulty: " + safe(difficulty) + "\nAttempts: " + attempts, 18, Color.WHITE, Typeface.BOLD);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(dp(20), dp(20), dp(20), dp(20));
        stats.setBackground(cardBackground(color("#33FFFFFF")));
        root.addView(stats, marginTop(28));

        MaterialButton done = styledButton("Start My Day", Color.WHITE, color("#2D2D44"));
        done.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        root.addView(done, marginTop(28));

        setContentView(scrollView);
    }

    private String safe(String value) {
        return value == null || value.isEmpty() ? "your alarm" : value;
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
        button.setCornerRadius(dp(14));
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextSize(16);
        button.setPadding(dp(24), dp(12), dp(24), dp(12));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
