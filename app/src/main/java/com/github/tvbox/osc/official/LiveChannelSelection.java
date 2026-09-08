package com.github.tvbox.osc.official;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import java.util.List;

/** Stable official identity takes precedence; legacy IPTV name restoration remains supported. */
public final class LiveChannelSelection {
    private LiveChannelSelection() {}

    public static LiveChannelItem find(List<LiveChannelGroup> groups, String officialId, String name) {
        LiveChannelItem byName = null;
        for (LiveChannelGroup group : groups) {
            if (group == null || group.getLiveChannels() == null) continue;
            for (LiveChannelItem item : group.getLiveChannels()) {
                if (item == null) continue;
                if (officialId != null && !officialId.isEmpty()
                        && officialId.equals(item.getOfficialChannelId()) && item.isOfficialLive()) return item;
                if (byName == null && item.getChannelName() != null && item.getChannelName().equals(name)) byName = item;
            }
        }
        return byName;
    }
}
