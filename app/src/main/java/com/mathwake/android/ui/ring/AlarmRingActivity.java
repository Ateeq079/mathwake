package com.mathwake.android.ui.ring;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import com.mathwake.android.ui.ThemeUtils;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.mathwake.android.alarm.AlarmRingingService;
import com.mathwake.android.alarm.AlarmScheduler;
import com.mathwake.android.data.AppSettingsRepository;
import com.mathwake.android.math.MathGenerator;
import com.mathwake.android.math.MathProblem;
import com.mathwake.android.model.AlarmModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalTime;

public class AlarmRingActivity extends AppCompatActivity {
    private static final int MAX_CONTENT_WIDTH_DP = 480;

    private AlarmModel alarm;
    private MathProblem problem;
    private AppSettingsRepository appSettings;
    private int attempts = 0;
    private int snoozeCount = 0;
    private TextView problemText;
    private EditText inputField;
    private TextView feedbackText;
    private TextView attemptsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This screen is a fixed vibrant gradient with white text; keep it light-themed
        // regardless of the app/system dark-mode setting so it always renders correctly.
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        showOverLockScreen();
        appSettings = new AppSettingsRepository(this);

        String json = getIntent().getStringExtra(AlarmScheduler.EXTRA_ALARM_JSON);
        if (json == null) {
            finish();
            return;
        }
        try {
            alarm = AlarmModel.fromJson(new JSONObject(json));
        } catch (JSONException exception) {
            finish();
            return;
        }
        snoozeCount = getIntent().getIntExtra(AlarmScheduler.EXTRA_SNOOZE_COUNT, 0);
        problem = MathGenerator.generate(alarm.getDifficulty());
        buildLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();
        focusAndShowKeyboard();
    }

    @Override
    public void onBackPressed() {
        // The alarm can only be dismissed by solving the math problem.
    }

    private void showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Ensure keyboard displays over lock screen
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void buildLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{color("#2D2D44"), color("#6C63FF"), color("#FF6584")}
        ));
        scrollView.addView(wrapper, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));

        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                constrainedWidth(), ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rootParams.gravity = Gravity.CENTER_HORIZONTAL;
        wrapper.addView(root, rootParams);

        // Top chips
        LinearLayout chips = new LinearLayout(this);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip(alarm.getLabel()), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        chips.addView(chip("RINGING"));
        root.addView(chips, matchWrap());

        // Clock
        TextView clock = text(appSettings.formatTime(LocalTime.now()), 48, Color.WHITE, Typeface.BOLD);
        clock.setGravity(Gravity.CENTER);
        root.addView(clock, marginTop(28));

        // Problem card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(18), dp(20), dp(18), dp(20));
        card.setBackground(cardBackground(color("#33FFFFFF")));
        root.addView(card, marginTop(24));

        TextView prompt = text("Solve to dismiss - " + alarm.getDifficulty().getDisplay().toUpperCase(), 12, Color.WHITE, Typeface.BOLD);
        prompt.setAlpha(0.8f);
        card.addView(prompt);

        problemText = text("", 34, Color.WHITE, Typeface.BOLD);
        problemText.setGravity(Gravity.CENTER);
        card.addView(problemText, marginTop(12));

        feedbackText = text("", 15, color("#43E97B"), Typeface.BOLD);
        feedbackText.setGravity(Gravity.CENTER);
        card.addView(feedbackText, marginTop(10));

        attemptsText = text("", 13, Color.WHITE, Typeface.NORMAL);
        attemptsText.setAlpha(0.65f);
        attemptsText.setGravity(Gravity.CENTER);
        card.addView(attemptsText, marginTop(4));

        Button newProblem = styledButton("New Problem", color("#24FFFFFF"), Color.WHITE);
        newProblem.setOnClickListener(v -> {
            problem = MathGenerator.generate(alarm.getDifficulty());
            inputField.setText("");
            feedbackText.setText("");
            updateProblemViews();
        });
        card.addView(newProblem, marginTop(12));

        // System input field (EditText)
        inputField = new EditText(this);
        inputField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        inputField.setTextColor(Color.WHITE);
        inputField.setTextSize(30);
        inputField.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        inputField.setGravity(Gravity.CENTER);
        inputField.setPadding(dp(36), dp(14), dp(36), dp(14));
        inputField.setBackground(cardBackground(color("#24FFFFFF")));
        inputField.setSingleLine(true);
        inputField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        inputField.setHint("Answer");
        inputField.setHintTextColor(color("#A0FFFFFF"));
        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAnswer();
                return true;
            }
            return false;
        });
        root.addView(inputField, marginTop(18));

        // Action buttons row
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER);
        root.addView(actionRow, marginTop(14));

        // Snooze button (only while snooze is enabled and the per-ring snooze cap is not reached).
        int snoozesLeft = AlarmScheduler.MAX_SNOOZES - snoozeCount;
        boolean showSnooze = alarm.getSnoozeMinutes() > 0 && snoozesLeft > 0;
        if (showSnooze) {
            Button snooze = styledButton("Snooze " + alarm.snoozeText() + " · " + snoozesLeft + " left",
                    color("#FFBE3B"), color("#2D2D44"));
            snooze.setOnClickListener(v -> snoozeAlarm());
            LinearLayout.LayoutParams snoozeParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
            snoozeParams.rightMargin = dp(6);
            actionRow.addView(snooze, snoozeParams);
        }

        // Dismiss button
        Button dismiss = styledButton("Dismiss Alarm", color("#43E97B"), color("#2D2D44"));
        dismiss.setOnClickListener(v -> checkAnswer());
        LinearLayout.LayoutParams dismissParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        if (showSnooze) {
            dismissParams.leftMargin = dp(6);
        }
        actionRow.addView(dismiss, dismissParams);

        setContentView(scrollView);
        updateProblemViews();
    }

    private void focusAndShowKeyboard() {
        if (inputField != null) {
            inputField.requestFocus();
            inputField.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
        }
    }

    private void snoozeAlarm() {
        AlarmRingingService.dismiss(this, alarm.getId());
        if (AlarmScheduler.canScheduleExact(this)) {
            AlarmScheduler.scheduleSnooze(this, alarm, snoozeCount + 1);
        }
        finish();
    }

    private void checkAnswer() {
        String input = inputField.getText().toString().trim();
        int answer;
        try {
            answer = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            feedbackText.setText("Enter a number");
            return;
        }

        if (answer == problem.getAnswer()) {
            AlarmRingingService.dismiss(this, alarm.getId());
            Intent intent = new Intent(this, AlarmSuccessActivity.class)
                    .putExtra(AlarmSuccessActivity.EXTRA_LABEL, alarm.getLabel())
                    .putExtra(AlarmSuccessActivity.EXTRA_ATTEMPTS, attempts + 1)
                    .putExtra(AlarmSuccessActivity.EXTRA_DIFFICULTY, alarm.getDifficulty().getDisplay());
            startActivity(intent);
            finish();
            return;
        }

        attempts += 1;
        String[] messages = {"Not quite. Try again.", "Keep going.", "Wake up and solve it.", "One more try."};
        feedbackText.setText(messages[attempts % messages.length]);
        inputField.setText("");
        updateProblemViews();
    }

    private void updateProblemViews() {
        problemText.setText(problem.getExpression());
        attemptsText.setText(attempts == 0 ? "" : attempts + " wrong attempt" + (attempts == 1 ? "" : "s"));
    }

    // ─── UI helpers ──────────────────────────────────────────────

    private TextView chip(String value) {
        TextView chip = text(value, 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(12), dp(8), dp(12), dp(8));
        chip.setBackground(cardBackground(color("#24FFFFFF")));
        return chip;
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
        button.setTextSize(14);
        button.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(14));
        button.setBackground(bg);
        button.setStateListAnimator(null);
        return button;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
