package com.github.tvbox.osc.update;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ApkDownloadProgressTest {
    @Test public void reportsWholePercentFromDownloadedBytes() {
        assertEquals(0, ApkDownloadProgress.percent(0, 100));
        assertEquals(1, ApkDownloadProgress.percent(1, 100));
        assertEquals(42, ApkDownloadProgress.percent(42, 100));
        assertEquals(99, ApkDownloadProgress.percent(99, 100));
        assertEquals(100, ApkDownloadProgress.percent(100, 100));
    }

    @Test public void boundsProgressAndHandlesInvalidTotals() {
        assertEquals(100, ApkDownloadProgress.percent(120, 100));
        assertEquals(0, ApkDownloadProgress.percent(-1, 100));
        assertEquals(0, ApkDownloadProgress.percent(12, 0));
        assertEquals(0, ApkDownloadProgress.percent(12, -1));
    }
}
