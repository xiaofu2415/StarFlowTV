package com.github.tvbox.osc.official;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;

import java.util.List;

/**
 * Chooses a safe automatic playback target for the live screen.
 *
 * <p>The built-in official catalog is intentionally not an automatic target. It
 * is a WebView-backed fallback and must only be opened after an explicit user
 * selection. This keeps a failed IPTV refresh from turning WebView startup into
 * the app's startup path.</p>
 */
public final class LiveStartupPolicy {
    private LiveStartupPolicy() {
    }

    public static ChannelPosition findFirstRemoteChannel(List<LiveChannelGroup> groups) {
        if (groups == null) return null;
        for (int groupPosition = 0; groupPosition < groups.size(); groupPosition++) {
            LiveChannelGroup group = groups.get(groupPosition);
            if (group == null || group.getLiveChannels() == null) continue;
            for (LiveChannelItem item : group.getLiveChannels()) {
                if (!hasRemotePlayback(item)) continue;
                return new ChannelPosition(groupPosition, item.getChannelIndex());
            }
        }
        return null;
    }

    private static boolean hasRemotePlayback(LiveChannelItem item) {
        if (item == null || item.isOfficialLive() || item.getChannelUrls() == null) return false;
        for (String url : item.getChannelUrls()) {
            if (url != null && !url.trim().isEmpty()) return true;
        }
        return false;
    }

    public static final class ChannelPosition {
        public final int groupIndex;
        public final int channelIndex;

        public ChannelPosition(int groupIndex, int channelIndex) {
            this.groupIndex = groupIndex;
            this.channelIndex = channelIndex;
        }
    }
}
