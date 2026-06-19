package com.mathwake.android;

import androidx.appcompat.app.AppCompatActivity;

import com.mathwake.android.data.AppSettingsRepository;
import com.mathwake.android.ui.ThemeUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/**
 * Reproduces the "card goes white" report: the user forces dark mode while the device (Robolectric
 * default qualifiers) is in light/day mode. We then check whether the launched activity actually
 * resolves the card surface to the dark night colour.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class ForcedDarkModeTest {
    @Test
    public void forcedDarkModeAppliesToActivityColours() {
        // User picked "Dark" in-app while the device (Robolectric default qualifiers) is light.
        new AppSettingsRepository(RuntimeEnvironment.getApplication())
                .setDarkMode(AppSettingsRepository.DARK_MODE_DARK);

        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).create().start().resume();
        AppCompatActivity activity = controller.get();

        int surfaceVariant = ThemeUtils.getColor(activity, "#F1F1F6");
        System.out.println("FORCED-DARK activity surfaceVariant=" + String.format("#%08X", surfaceVariant));
        // Dark night value is #232634; light value is #EDEFF5.
        org.junit.Assert.assertEquals(0xFF232634, surfaceVariant);
    }
}
