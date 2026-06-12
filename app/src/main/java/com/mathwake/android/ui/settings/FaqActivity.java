package com.mathwake.android.ui.settings;

public class FaqActivity extends TextContentActivity {
    @Override
    protected String screenTitle() {
        return "FAQ";
    }

    @Override
    protected String bodyText() {
        return "Why does MathWake need exact alarms?\n"
                + "Exact alarms let Android ring at the selected minute instead of batching the alarm later.\n\n"
                + "Why request overlay permission?\n"
                + "Overlay access helps the alarm screen appear promptly above other apps on devices that restrict full-screen launches.\n\n"
                + "Why request battery permission?\n"
                + "Some devices delay alarms when battery optimization is active. Allowing MathWake to ignore optimization improves reliability.\n\n"
                + "How do I stop an alarm?\n"
                + "Solve the math problem shown on the ringing screen, then tap Dismiss Alarm.";
    }
}
