package com.mathwake.android.ui.settings;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mathwake.android.ui.ThemeUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public abstract class TextContentActivity extends AppCompatActivity {
    private static final int MAX_CONTENT_WIDTH_DP = 480;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildLayout();
    }

    protected abstract String screenTitle();

    protected abstract String bodyText();

    private void buildLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(color("#F7F8FC"));

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

        root.addView(text(screenTitle(), 28, color("#2D2D44"), Typeface.BOLD));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(ThemeUtils.getSurfaceColor(this)));
        card.setElevation(dp(2));
        TextView body = text(bodyText(), 15, color("#2D2D44"), Typeface.NORMAL);
        body.setLineSpacing(dp(3), 1.0f);
        card.addView(body);
        root.addView(card, marginTop(20));

        Button done = styledButton("Done", color("#6C63FF"), ThemeUtils.getOnPrimaryColor(this));
        done.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams doneParams = marginTop(20);
        doneParams.height = dp(52);
        root.addView(done, doneParams);

        setContentView(scrollView);
    }

    protected TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    protected Button styledButton(String label, int bgColor, int textColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextSize(14);
        button.setPadding(dp(16), dp(10), dp(16), dp(10));
        button.setBackground(cardBackground(bgColor));
        button.setStateListAnimator(null);
        return button;
    }

    protected GradientDrawable cardBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    protected LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(dp);
        return params;
    }

    protected int constrainedWidth() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidthDp = (int) (dm.widthPixels / dm.density);
        if (screenWidthDp > MAX_CONTENT_WIDTH_DP + 40) {
            return dp(MAX_CONTENT_WIDTH_DP);
        }
        return ViewGroup.LayoutParams.MATCH_PARENT;
    }

    protected int color(String hex) {
        return ThemeUtils.getColor(this, hex);
    }

    protected int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
