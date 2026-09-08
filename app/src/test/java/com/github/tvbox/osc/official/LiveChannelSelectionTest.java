package com.github.tvbox.osc.official;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiveChannelSelectionTest {
    @Test public void officialIdWinsOverEarlierIptvChannelWithSameName() {
        LiveChannelItem iptv = new LiveChannelItem();
        iptv.setChannelName("CCTV-1");
        LiveChannelGroup remote = new LiveChannelGroup();
        remote.setLiveChannels(new ArrayList<>(Arrays.asList(iptv)));
        LiveChannelGroup official = OfficialLiveCatalog.toGroup(1, 1);
        List<LiveChannelGroup> groups = Arrays.asList(remote, official);
        assertSame(official.getLiveChannels().get(0), LiveChannelSelection.find(groups, "cctv-1", "CCTV-1"));
        assertSame(iptv, LiveChannelSelection.find(groups, "", "CCTV-1"));
        assertSame(iptv, LiveChannelSelection.find(groups, "missing", "CCTV-1"));
    }
}
