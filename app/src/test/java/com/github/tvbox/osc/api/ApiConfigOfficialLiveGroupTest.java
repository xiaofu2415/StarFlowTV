package com.github.tvbox.osc.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.github.tvbox.osc.official.OfficialLiveCatalog;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class ApiConfigOfficialLiveGroupTest {

    @Test public void onlyBuiltInOfficialGroupDoesNotRepresentRemoteLiveConfigSuccess() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        groups.add(OfficialLiveCatalog.toGroup(0, 0));

        assertFalse(ApiConfig.hasRemoteLiveConfigResult(groups));
    }

    @Test public void normalRemoteGroupAndOfficialGroupRepresentRemoteLiveConfigSuccess() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        groups.add(remoteGroupWithChannel());
        ApiConfig.ensureOfficialLiveGroup(groups);

        assertTrue(ApiConfig.hasRemoteLiveConfigResult(groups));
        assertEquals(2, groups.size());
    }

    @Test public void emptyRemoteGroupDoesNotRepresentRemoteLiveConfigSuccess() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        LiveChannelGroup emptyRemoteGroup = new LiveChannelGroup();
        emptyRemoteGroup.setGroupName("远程空组");
        emptyRemoteGroup.setLiveChannels(new ArrayList<LiveChannelItem>());
        groups.add(emptyRemoteGroup);
        ApiConfig.ensureOfficialLiveGroup(groups);

        assertFalse(ApiConfig.hasRemoteLiveConfigResult(groups));
    }

    @Test public void remoteChannelWithoutPlaybackSourceDoesNotRepresentRemoteLiveConfigSuccess() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        LiveChannelItem channelWithoutSource = new LiveChannelItem();
        channelWithoutSource.setChannelName("无源远程频道");
        ArrayList<LiveChannelItem> channels = new ArrayList<>();
        channels.add(channelWithoutSource);
        LiveChannelGroup group = new LiveChannelGroup();
        group.setGroupName("远程直播");
        group.setLiveChannels(channels);
        groups.add(group);
        ApiConfig.ensureOfficialLiveGroup(groups);

        assertFalse(ApiConfig.hasRemoteLiveConfigResult(groups));
    }

    @Test public void remoteChannelWithOnlyEmptyBlankOrNullUrlsDoesNotRepresentRemoteLiveConfigSuccess() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        groups.add(remoteGroupWithSourceUrls("", " \t", null));
        ApiConfig.ensureOfficialLiveGroup(groups);

        assertFalse(ApiConfig.hasRemoteLiveConfigResult(groups));
    }

    @Test public void remoteChannelWithOneTrimmedNonEmptyUrlRepresentsRemoteLiveConfigSuccess() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        groups.add(remoteGroupWithSourceUrls("", null, " https://example.com/live.m3u8 "));
        ApiConfig.ensureOfficialLiveGroup(groups);

        assertTrue(ApiConfig.hasRemoteLiveConfigResult(groups));
    }

    @Test public void untrustedSameNameGroupIsPreservedAsIptvAndCannotHideBuiltInCatalog() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        LiveChannelGroup remoteOfficialGroup = remoteGroupWithChannel();
        remoteOfficialGroup.setGroupName("官方直播");
        groups.add(remoteOfficialGroup);

        ApiConfig.ensureOfficialLiveGroup(groups);
        ApiConfig.ensureOfficialLiveGroup(groups);

        assertEquals(1, countOfficialGroups(groups));
        assertEquals(2, groups.size());
        assertEquals("官方直播（IPTV）", remoteOfficialGroup.getGroupName());
        assertEquals("https://example.com/live.m3u8", remoteOfficialGroup.getLiveChannels().get(0).getUrl());
        assertEquals(21, groups.get(1).getLiveChannels().size());
    }

    @Test public void emptySameNameGroupCannotHideBuiltInCatalog() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        LiveChannelGroup empty = new LiveChannelGroup();
        empty.setGroupName("官方直播");
        empty.setLiveChannels(new ArrayList<LiveChannelItem>());
        groups.add(empty);
        ApiConfig.ensureOfficialLiveGroup(groups);
        assertEquals(2, groups.size());
        assertEquals("官方直播（IPTV）", empty.getGroupName());
        assertEquals(21, groups.get(1).getLiveChannels().size());
    }

    @Test public void forgedMetadataOrPlaybackUrlCannotHideBuiltInCatalog() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        LiveChannelGroup forged = OfficialLiveCatalog.toGroup(0, 0);
        forged.getLiveChannels().get(0).getChannelUrls().set(0, "https://example.com/live.m3u8");
        groups.add(forged);
        ApiConfig.ensureOfficialLiveGroup(groups);
        assertEquals(2, groups.size());
        assertEquals("官方直播（IPTV）", forged.getGroupName());
        assertFalse(forged.getLiveChannels().get(0).isOfficialLive());
    }

    @Test public void proxyPlaceholderRemainsAloneUntilItLoadsRemoteChannels() {
        List<LiveChannelGroup> groups = new ArrayList<>();
        LiveChannelGroup proxyPlaceholder = new LiveChannelGroup();
        proxyPlaceholder.setGroupName("http://127.0.0.1:9978/proxy?do=live");
        groups.add(proxyPlaceholder);

        ApiConfig.ensureOfficialLiveGroup(groups);

        assertEquals(1, groups.size());
    }

    private LiveChannelGroup remoteGroupWithChannel() {
        return remoteGroupWithSourceUrls("https://example.com/live.m3u8");
    }

    private LiveChannelGroup remoteGroupWithSourceUrls(String... urls) {
        LiveChannelItem channel = new LiveChannelItem();
        channel.setChannelName("远程频道");
        ArrayList<String> sourceUrls = new ArrayList<>();
        for (String url : urls) sourceUrls.add(url);
        channel.setChannelUrls(sourceUrls);
        ArrayList<LiveChannelItem> channels = new ArrayList<>();
        channels.add(channel);
        LiveChannelGroup group = new LiveChannelGroup();
        group.setGroupName("远程直播");
        group.setLiveChannels(channels);
        return group;
    }

    private int countOfficialGroups(List<LiveChannelGroup> groups) {
        int count = 0;
        for (LiveChannelGroup group : groups) {
            if (group != null && "官方直播".equals(group.getGroupName())) count++;
        }
        return count;
    }
}
