package com.github.tvbox.osc.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DistributionEndpointsTest {
    @Test public void preservesUserLiveUrl() {
        assertEquals("https://home.example/live.txt", DistributionEndpoints.resolveLiveUrl(
                "https://home.example/live.txt", "https://tv.example/live/live.txt"));
    }

    @Test public void usesBuiltInHttpsUrlOnlyWhenUserValueIsEmpty() {
        assertEquals("https://tv.example/live/live.txt", DistributionEndpoints.resolveLiveUrl(
                "", "https://tv.example/live/live.txt"));
        assertEquals("", DistributionEndpoints.resolveLiveUrl("", "http://tv.example/live.txt"));
    }

    @Test public void rejectsCredentialsQueryAndFragments() {
        assertFalse(DistributionEndpoints.isSafeHttps("https://user:pass@tv.example/live.txt"));
        assertFalse(DistributionEndpoints.isSafeHttps("https://tv.example/live.txt?token=x"));
        assertFalse(DistributionEndpoints.isSafeHttps("https://tv.example/live.txt#x"));
        assertTrue(DistributionEndpoints.isSafeHttps("https://tv.example/live/live.txt"));
    }
}
