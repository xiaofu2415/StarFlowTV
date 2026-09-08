package com.github.tvbox.osc.official;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OfficialLiveUrlPolicyTest {

    @Test public void rejectsNonOfficialPageUrls() {
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("http://tv.cctv.com/live/cctv1/"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("https://evil.example/live/cctv1/"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/program/"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/live/cctv1/?token=secret"));
    }

    @Test public void acceptsCctvLivePages() {
        assertTrue(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/live/cctv1/"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedPageUrl("https://tv.cctv.com/live/cctveurope/index.shtml"));
    }

    @Test public void rejectsAmbiguousLivePagePaths() {
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl(
                "https://tv.cctv.com/live/../program/"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl(
                "https://tv.cctv.com/live/%2e%2e/program/"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl(
                "https://tv.cctv.com/live/%2E%2E/program/"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedPageUrl(
                "https://tv.cctv.com/live/..\\program/"));
    }

    @Test public void officialResourceHostsRemainAllowlisted() {
        assertTrue(OfficialLiveUrlPolicy.isAllowedNavigationUrl("https://tv.cctv.com/live/cctv1/"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("p2.img.cctvpic.com"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("live.cctv.com"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedNavigationUrl("https://example.com/redirect"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceHost("p2.img.cctvpic.com.evil.example"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceHost("example.com"));
    }

    @Test public void resourceUrlsRequireAllowlistedHttpsHosts() {
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://p2.img.cctvpic.com/image.jpg?size=large"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://live.cctv.com/player.js"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "http://live.cctv.com/player.js"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://example.com/player.js"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://live.cctv.com:8443/player.js"));
    }
}
