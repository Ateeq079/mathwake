package com.mathwake.android;

import static org.junit.Assert.assertEquals;

import android.graphics.Color;

import com.mathwake.android.ui.ThemeUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28}, qualifiers = "night")
public class NightColorTest {
    @Test
    public void surfaceColorsResolveToDarkInNightMode() {
        android.content.Context context = RuntimeEnvironment.getApplication();
        int surface = ThemeUtils.getColor(context, "#FFFFFF");
        int surfaceVariant = ThemeUtils.getColor(context, "#F1F1F6");
        int background = ThemeUtils.getColor(context, "#F7F8FC");
        System.out.println("NIGHT surface=" + hex(surface)
                + " surfaceVariant=" + hex(surfaceVariant)
                + " background=" + hex(background));
        assertEquals(0xFF191B24, surface);
        assertEquals(0xFF232634, surfaceVariant);
        assertEquals(0xFF0E1016, background);
    }

    private static String hex(int color) {
        return String.format("#%08X", color);
    }
}
