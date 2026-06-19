package com.mathwake.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class AppCrashTest {
    @Test
    public void testMainActivityLaunches() {
        try {
            Robolectric.buildActivity(MainActivity.class).create().start().resume().visible();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
