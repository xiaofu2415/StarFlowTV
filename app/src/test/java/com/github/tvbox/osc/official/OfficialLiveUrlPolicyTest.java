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

    @Test public void resourceUrlsRequireAllowlistedHostsAndNoAmbiguousAuthority() {
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://p2.img.cctvpic.com/image.jpg?size=large"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://live.cctv.com/player.js"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "http://live.cctv.com/player.js"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://example.com/player.js"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "http://example.com/player.js"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://live.cctv.com:8443/player.js"));
    }

    @Test public void officialPlayerAndMediaCdnUrlsAreAllowlisted() {
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("js.player.cntv.cn"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("api.live.cntv.cn"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("cbox.cntv.cn"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("js.data.cctv.com"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("vdnad.apps.cntv.cn"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("ldncctvwbcdali.v.myalicdn.com"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("ldncctvwbcdbd.a.bdydns.com"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("ldncctvwbcdcnc.v.wscdns.com"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("ldncctvwbndhwy.cntv.myhwcdn.cn"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("ldncctvwbcdks.v.kcdnvip.com"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceHost("ldncctvwbndtxy.liveplay.myqcloud.com"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://js.player.cntv.cn/creator/liveplayer.js"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "https://ldncctvwbndhwy.cntv.myhwcdn.cn/ldncctvwbnd/ldcctv1_2/index.m3u8"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "http://ldncctvwbcdks.v.kcdnvip.com/ldncctvwbcd/cdrmldcctv1_1/index.m3u8"));
        assertFalse(OfficialLiveUrlPolicy.isAllowedResourceHost("player.cntv.cn.evil.example"));
        assertTrue(OfficialLiveUrlPolicy.isAllowedResourceUrl(
                "http://js.player.cntv.cn/creator/liveplayer.js"));
    }
}
