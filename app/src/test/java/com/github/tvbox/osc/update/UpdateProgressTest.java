package com.github.tvbox.osc.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class UpdateProgressTest {
    @Test public void reportsBoundedPercentageForKnownDownloadSize() {
        assertEquals(0, UpdateProgress.percent(0, 100));
        assertEquals(25, UpdateProgress.percent(25, 100));
        assertEquals(100, UpdateProgress.percent(100, 100));
        assertEquals(100, UpdateProgress.percent(120, 100));
    }

    @Test public void handlesUnknownOrInvalidDownloadSize() {
        assertEquals(0, UpdateProgress.percent(50, 0));
        assertEquals(0, UpdateProgress.percent(50, -1));
        assertEquals(0, UpdateProgress.percent(-1, 100));
    }
}
