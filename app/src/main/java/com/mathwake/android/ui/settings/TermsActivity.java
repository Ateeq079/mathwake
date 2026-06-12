package com.mathwake.android.ui.settings;

public class TermsActivity extends TextContentActivity {
    @Override
    protected String screenTitle() {
        return "Terms of Service";
    }

    @Override
    protected String bodyText() {
        return "MathWake is provided as an alarm and productivity tool. You are responsible for confirming that alarms are configured correctly and that your device settings allow alarms to ring.\n\n"
                + "The app should not be relied on as the only alert for safety-critical, medical, legal, travel, or emergency events.\n\n"
                + "By using the app, you agree to use it lawfully and to keep device permissions, volume, battery, and notification settings suitable for your needs.";
    }
}
