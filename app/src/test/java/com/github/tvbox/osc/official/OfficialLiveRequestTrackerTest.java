package com.github.tvbox.osc.official;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OfficialLiveRequestTrackerTest {
    @Test public void redirectDoesNotNeedWebViewSourceGettersOrPriorAuthorization() {
        OfficialLiveRequestTracker tracker = new OfficialLiveRequestTracker();
        tracker.begin("https://tv.cctv.com/live/cctv1/");

        assertTrue(tracker.transition("https://tv.cctv.com/live/cctv1/index.shtml"));
        assertTrue(tracker.matches("https://tv.cctv.com/live/cctv1/index.shtml"));
        assertFalse(tracker.matches("https://tv.cctv.com/live/cctv1/"));
    }

    @Test public void repeatedRedirectAndSiteNavigationAdvanceCurrentCallbacks() {
        OfficialLiveRequestTracker tracker = new OfficialLiveRequestTracker();
        tracker.begin("https://tv.cctv.com/live/cctv1/");

        assertTrue(tracker.transition("https://tv.cctv.com/live/cctv1/index.shtml"));
        assertTrue(tracker.transition("https://tv.cctv.com/live/cctv2/"));
        assertTrue(tracker.transition("https://tv.cctv.com/live/cctv2/index.shtml"));
        assertTrue(tracker.matches("https://tv.cctv.com/live/cctv2/index.shtml"));
        assertFalse(tracker.matches("https://tv.cctv.com/live/cctv1/index.shtml"));
        assertFalse(tracker.matches("https://tv.cctv.com/live/cctv2/"));
    }

    @Test public void disallowedRedirectDoesNotChangeCurrentUrl() {
        OfficialLiveRequestTracker tracker = new OfficialLiveRequestTracker();
        tracker.begin("https://tv.cctv.com/live/cctv1/");

        assertFalse(tracker.transition("https://example.com/live/cctv1/"));
        assertFalse(tracker.transition("https://tv.cctv.com/program/"));
        assertFalse(tracker.transition("about:blank"));
        assertFalse(tracker.transition(null));
        assertTrue(tracker.matches("https://tv.cctv.com/live/cctv1/"));
    }

    @Test public void replacingRequestTracksOnlyNewUrl() {
        OfficialLiveRequestTracker tracker = new OfficialLiveRequestTracker();
        tracker.begin("https://tv.cctv.com/live/cctv1/");
        tracker.begin("https://tv.cctv.com/live/cctv2/");

        assertFalse(tracker.matches("https://tv.cctv.com/live/cctv1/"));
        assertTrue(tracker.matches("https://tv.cctv.com/live/cctv2/"));
    }

    @Test public void stopInvalidatesCurrentCallbacksAndIsIdempotent() {
        OfficialLiveRequestTracker tracker = new OfficialLiveRequestTracker();
        tracker.begin("https://tv.cctv.com/live/cctv1/");

        assertTrue(tracker.stop());
        assertFalse(tracker.stop());
        assertFalse(tracker.matches("https://tv.cctv.com/live/cctv1/"));
        assertFalse(tracker.transition("https://tv.cctv.com/live/cctv2/"));
        assertFalse(tracker.isActive());
    }
}
