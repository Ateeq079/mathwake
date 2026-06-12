package com.mathwake.android.ui.settings;

public class PrivacyPolicyActivity extends TextContentActivity {
    @Override
    protected String screenTitle() {
        return "Privacy Policy";
    }

    @Override
    protected String bodyText() {
        return "MathWake stores alarm and app settings locally on your device using Android preferences.\n\n"
                + "The app does not require an account and does not transmit your alarm labels, schedules, ringtone choices, or settings to a server.\n\n"
                + "If you use the share action, Android opens your chosen sharing app and that app handles the shared message under its own privacy practices.";
    }
}
