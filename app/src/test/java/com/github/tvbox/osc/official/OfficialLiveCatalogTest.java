package com.github.tvbox.osc.official;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class OfficialLiveCatalogTest {

    @Test public void allEntrancesPreserveExactIdentityAndWebOnlyGroupMetadata() {
        String[][] expected = {
            {"cctv-1", "CCTV-1", "cctv1/"}, {"cctv-2", "CCTV-2", "cctv2/"},
            {"cctv-3", "CCTV-3", "cctv3/"}, {"cctv-4", "CCTV-4", "cctv4/"},
            {"cctv-5", "CCTV-5", "cctv5/"}, {"cctv-6", "CCTV-6", "cctv6/"},
            {"cctv-7", "CCTV-7", "cctv7/"}, {"cctv-8", "CCTV-8", "cctv8/"},
            {"cctv-9", "CCTV-9", "cctv9/"}, {"cctv-10", "CCTV-10", "cctv10/"},
            {"cctv-11", "CCTV-11", "cctv11/"}, {"cctv-12", "CCTV-12", "cctv12/"},
            {"cctv-13", "CCTV-13", "cctv13/"}, {"cctv-14", "CCTV-14", "cctv14/"},
            {"cctv-15", "CCTV-15", "cctv15/"}, {"cctv-16", "CCTV-16", "cctv16/"},
            {"cctv-17", "CCTV-17", "cctv17/"}, {"cctv-5plus", "CCTV-5+", "cctv5plus/"},
            {"cctv-4-asia", "CCTV-4 Asia", "cctv4/"},
            {"cctv-4-europe", "CCTV-4 Europe", "cctveurope/index.shtml"},
            {"cctv-4-america", "CCTV-4 America", "cctvamerica/"}
        };
        LiveChannelGroup group = OfficialLiveCatalog.toGroup(4, 12);
        assertEquals("官方直播", group.getGroupName());
        assertEquals(4, group.getGroupIndex());
        assertEquals("", group.getGroupPassword());
        assertEquals(21, group.getLiveChannels().size());
        for (int i = 0; i < expected.length; i++) {
            OfficialLiveChannel channel = OfficialLiveCatalog.builtIn().get(i);
            LiveChannelItem item = group.getLiveChannels().get(i);
            String url = "https://tv.cctv.com/live/" + expected[i][2];
            assertEquals(expected[i][0], channel.getId());
            assertEquals(expected[i][1], channel.getName());
            assertEquals(url, channel.getPageUrl());
            assertEquals("", channel.getDirectUrl());
            assertEquals(expected[i][0], item.getOfficialChannelId());
            assertEquals(expected[i][1], item.getChannelName());
            assertEquals(url, item.getOfficialPageUrl());
            assertEquals("", item.getOfficialDirectUrl());
            assertEquals(url, item.getUrl());
            assertEquals(i, item.getChannelIndex());
            assertEquals(i + 13, item.getChannelNum());
            assertEquals(1, item.getSourceNum());
            assertEquals("央视官方页", item.getSourceName());
            assertEquals(OfficialLivePlaybackMode.WEB, item.getSourcePlaybackMode());
        }
    }

    @Test public void forgedAndEmptySameNameGroupsCannotSuppressTrustedCatalog() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        LiveChannelGroup forged = OfficialLiveCatalog.toGroup(0, 0);
        forged.getLiveChannels().get(0).getChannelUrls().set(0, "https://example.com/live.m3u8");
        LiveChannelGroup empty = new LiveChannelGroup();
        empty.setGroupName("官方直播");
        empty.setGroupIndex(1);
        groups.add(forged);
        groups.add(empty);
        OfficialLiveCatalog.ensureTrustedGroup(groups);
        OfficialLiveCatalog.ensureTrustedGroup(groups);
        assertEquals(3, groups.size());
        assertEquals("官方直播（IPTV）", forged.getGroupName());
        assertEquals("官方直播（IPTV）", empty.getGroupName());
        assertEquals("", forged.getLiveChannels().get(0).getOfficialChannelId());
        assertEquals("https://example.com/live.m3u8", forged.getLiveChannels().get(0).getUrl());
        assertEquals(OfficialLivePlaybackMode.NONE, forged.getLiveChannels().get(0).getSourcePlaybackMode());
        assertTrue(OfficialLiveCatalog.isTrustedGroup(groups.get(2)));
        assertEquals(2, groups.get(2).getGroupIndex());
    }

    @Test public void offlineFallbackReplacesUnresolvedProxyAndCanBeAppliedWithoutAnotherLoad() {
        List<LiveChannelGroup> failed = new ArrayList<>();
        LiveChannelGroup proxy = new LiveChannelGroup();
        proxy.setGroupName("http://127.0.0.1:9978/proxy?do=live");
        failed.add(proxy);
        List<LiveChannelGroup> fallback = OfficialLiveCatalog.fallbackGroups(failed);
        assertEquals(1, fallback.size());
        assertTrue(OfficialLiveCatalog.isTrustedGroup(fallback.get(0)));
        assertEquals(0, fallback.get(0).getGroupIndex());
        assertEquals(21, fallback.get(0).getLiveChannels().size());
        assertEquals(1, failed.size()); // Doesn't clear the shared config while preparing fallback.
        assertEquals(1, OfficialLiveCatalog.fallbackGroups(fallback).size());
    }

    @Test public void invalidIdentityOrMetadataRequiresWholeCatalogFallback() {
        for (int mutation = 0; mutation < 4; mutation++) {
            LiveChannelGroup group = OfficialLiveCatalog.toGroup(0, 0);
            LiveChannelItem item = group.getLiveChannels().get(1);
            if (mutation == 0) item.setOfficialChannelId("cctv-1");
            if (mutation == 1) item.setChannelName("");
            if (mutation == 2) item.setOfficialPageUrl("https://example.com/live/cctv2/");
            if (mutation == 3) item.setOfficialDirectUrl("https://example.com/live.m3u8");
            List<LiveChannelGroup> groups = new ArrayList<>();
            groups.add(group);
            OfficialLiveCatalog.ensureTrustedGroup(groups);
            assertEquals(2, groups.size());
            assertTrue(OfficialLiveCatalog.isTrustedGroup(groups.get(1)));
        }
    }

    @Test public void builtInOfficialItemsStartInWebModeAndExposeOneSource() {
        LiveChannelItem item = OfficialLiveCatalog.toGroup(0, 0).getLiveChannels().get(0);
        assertEquals(1, item.getSourceNum());
        assertEquals(OfficialLivePlaybackMode.WEB, item.getSourcePlaybackMode());
        assertTrue(item.getUrl().startsWith("https://tv.cctv.com/live/"));
    }

    @Test public void builtInCatalogContainsTheTwentyOneCctvEntrances() {
        List<OfficialLiveChannel> channels = OfficialLiveCatalog.builtIn();

        assertEquals(21, channels.size());
        assertEquals("cctv-1", channels.get(0).getId());
        assertEquals("cctv-5plus", channels.get(17).getId());
        assertEquals("cctv-4-america", channels.get(20).getId());
        assertEquals(21, new HashSet<String>(ids(channels)).size());
        for (OfficialLiveChannel channel : channels) {
            assertTrue(OfficialLiveUrlPolicy.isAllowedPageUrl(channel.getPageUrl()));
        }
    }

    @Test public void emptyDirectUrlUsesOfficialWebPage() {
        OfficialLiveChannel channel = OfficialLiveCatalog.builtIn().get(0);

        assertEquals("", channel.getDirectUrl());
        assertEquals(OfficialLivePlaybackMode.WEB, channel.preferredMode());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void builtInCatalogCannotBeModified() {
        OfficialLiveCatalog.builtIn().clear();
    }

    @Test public void catalogGroupProvidesNavigableLiveItems() {
        LiveChannelGroup group = OfficialLiveCatalog.toGroup(4, 12);

        assertEquals("官方直播", group.getGroupName());
        assertEquals(4, group.getGroupIndex());
        assertEquals(21, group.getLiveChannels().size());
        LiveChannelItem first = group.getLiveChannels().get(0);
        assertEquals(0, first.getChannelIndex());
        assertEquals(13, first.getChannelNum());
        assertTrue(first.isOfficialLive());
        assertEquals(OfficialLivePlaybackMode.WEB, first.getSourcePlaybackMode());
        assertEquals("央视官方页", first.getSourceName());
    }

    private List<String> ids(List<OfficialLiveChannel> channels) {
        List<String> ids = new ArrayList<>();
        for (OfficialLiveChannel channel : channels) ids.add(channel.getId());
        return ids;
    }
}
