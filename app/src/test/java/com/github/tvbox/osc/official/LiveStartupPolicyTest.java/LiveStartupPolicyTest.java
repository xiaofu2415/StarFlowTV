package com.github.tvbox.osc.official;

import com.github.tvbox.osc.bean.LiveChannelGroup;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class LiveStartupPolicyTest {
    @Test
    public void officialOnlyFallbackDoesNotAutoStartWebChannel() {
        List<LiveChannelGroup> groups = Arrays.asList(OfficialLiveCatalog.toGroup(0, 0));

        assertNull(LiveStartupPolicy.findFirstRemoteChannel(groups));
    }

    @Test
    public void remoteChannelRemainsTheDefaultAutoStartTarget() {
        LiveChannelGroup remote = new LiveChannelGroup();
        remote.setGroupIndex(3);
        remote.setLiveChannels(new ArrayList<>());
        remote.getLiveChannels().add(remoteChannel(4));
        List<LiveChannelGroup> groups = Arrays.asList(remote, OfficialLiveCatalog.toGroup(4, 1));

        LiveStartupPolicy.ChannelPosition position = LiveStartupPolicy.findFirstRemoteChannel(groups);

        assertEquals(0, position.groupIndex);
        assertEquals(4, position.channelIndex);
    }

    private com.github.tvbox.osc.bean.LiveChannelItem remoteChannel(int index) {
        com.github.tvbox.osc.bean.LiveChannelItem item = new com.github.tvbox.osc.bean.LiveChannelItem();
        item.setChannelIndex(index);
        item.setChannelName("远程频道");
        item.setChannelUrls(new ArrayList<>(Arrays.asList("https://example.com/live.m3u8")));
        return item;
    }
}
