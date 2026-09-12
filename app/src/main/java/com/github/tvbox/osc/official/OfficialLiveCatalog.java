package com.github.tvbox.osc.official;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OfficialLiveCatalog {
    private static final String CCTV_LIVE = "https://tv.cctv.com/live/";
    private static final String GROUP_NAME = "官方直播";
    private static final String OFFICIAL_PAGE_SOURCE_NAME = "央视官方页";
    private static final List<OfficialLiveChannel> BUILT_IN = build();

    private OfficialLiveCatalog() {
    }

    public static List<OfficialLiveChannel> builtIn() {
        return BUILT_IN;
    }

    /** This release trusts only the exact built-in WEB catalog, not a remote display name. */
    public static boolean isTrustedGroup(LiveChannelGroup group) {
        if (group == null || !GROUP_NAME.equals(group.getGroupName())
                || (group.getGroupPassword() != null && !group.getGroupPassword().isEmpty())) return false;
        List<LiveChannelItem> items = group.getLiveChannels();
        if (items == null || items.size() != BUILT_IN.size()) return false;
        for (int i = 0; i < BUILT_IN.size(); i++) {
            OfficialLiveChannel trusted = BUILT_IN.get(i);
            LiveChannelItem item = items.get(i);
            if (item == null || !trusted.getId().equals(item.getOfficialChannelId())
                    || !trusted.getName().equals(item.getChannelName())
                    || !trusted.getPageUrl().equals(item.getOfficialPageUrl())
                    || !OfficialLiveUrlPolicy.isAllowedPageUrl(item.getOfficialPageUrl())
                    || !item.getOfficialDirectUrl().isEmpty()
                    || item.getChannelUrls() == null || item.getChannelUrls().size() != 1
                    || !trusted.getPageUrl().equals(item.getChannelUrls().get(0))
                    || item.getSourcePlaybackMode() != OfficialLivePlaybackMode.WEB) return false;
        }
        return true;
    }

    public static void ensureTrustedGroup(List<LiveChannelGroup> groups) {
        if (groups == null) return;
        int nextGroupIndex = 0;
        int nextChannelNumber = 0;
        boolean hasTrusted = false;
        for (LiveChannelGroup group : groups) {
            if (group == null) continue;
            boolean trusted = isTrustedGroup(group);
            hasTrusted |= trusted;
            if (GROUP_NAME.equals(group.getGroupName()) && !trusted) {
                group.setGroupName(GROUP_NAME + "（IPTV）");
                if (group.getLiveChannels() != null) for (LiveChannelItem item : group.getLiveChannels()) {
                    if (item == null) continue;
                    item.setOfficialChannelId("");
                    item.setOfficialPageUrl("");
                    item.setOfficialDirectUrl("");
                    item.setChannelSourceModes(null);
                }
            }
            nextGroupIndex = Math.max(nextGroupIndex, group.getGroupIndex() + 1);
            if (group.getLiveChannels() != null) for (LiveChannelItem item : group.getLiveChannels()) {
                if (item != null) nextChannelNumber = Math.max(nextChannelNumber, item.getChannelNum());
            }
        }
        if (!hasTrusted) groups.add(toGroup(nextGroupIndex, nextChannelNumber));
    }

    /** A failed proxy is not a channel group; fallback is ready for direct UI application. */
    public static List<LiveChannelGroup> fallbackGroups(List<LiveChannelGroup> cached) {
        List<LiveChannelGroup> groups = new ArrayList<>();
        if (cached != null) for (LiveChannelGroup group : cached) {
            if (group != null && (group.getGroupName() == null
                    || !group.getGroupName().startsWith("http://127.0.0.1"))) groups.add(group);
        }
        ensureTrustedGroup(groups);
        return groups;
    }

    public static LiveChannelGroup toGroup(int groupIndex, int channelNumberStart) {
        LiveChannelGroup group = new LiveChannelGroup();
        group.setGroupIndex(groupIndex);
        group.setGroupName(GROUP_NAME);
        group.setGroupPassword("");
        ArrayList<LiveChannelItem> items = new ArrayList<>();
        for (int i = 0; i < BUILT_IN.size(); i++) {
            OfficialLiveChannel channel = BUILT_IN.get(i);
            LiveChannelItem item = new LiveChannelItem();
            item.setChannelIndex(i);
            item.setChannelNum(channelNumberStart + i + 1);
            item.setChannelName(channel.getName());
            item.setOfficialChannelId(channel.getId());
            item.setOfficialPageUrl(channel.getPageUrl());
            item.setOfficialDirectUrl(channel.getDirectUrl());
            ArrayList<String> sourceNames = new ArrayList<>();
            sourceNames.add(OFFICIAL_PAGE_SOURCE_NAME);
            item.setChannelSourceNames(sourceNames);
            ArrayList<String> sourceUrls = new ArrayList<>();
            sourceUrls.add(channel.getPageUrl());
            item.setChannelUrls(sourceUrls);
            ArrayList<OfficialLivePlaybackMode> sourceModes = new ArrayList<>();
            sourceModes.add(OfficialLivePlaybackMode.WEB);
            item.setChannelSourceModes(sourceModes);
            items.add(item);
        }
        group.setLiveChannels(items);
        return group;
    }

    private static List<OfficialLiveChannel> build() {
        List<OfficialLiveChannel> channels = new ArrayList<>();
        for (int number = 1; number <= 17; number++) {
            add(channels, "cctv-" + number, "CCTV-" + number, "cctv" + number + "/m/");
        }
        add(channels, "cctv-5plus", "CCTV-5+", "cctv5plus/m/");
        add(channels, "cctv-4-asia", "CCTV-4 Asia", "cctv4/m/");
        add(channels, "cctv-4-europe", "CCTV-4 Europe", "cctveurope/m/");
        add(channels, "cctv-4-america", "CCTV-4 America", "cctvamerica/m/");
        return Collections.unmodifiableList(channels);
    }

    private static void add(List<OfficialLiveChannel> channels, String id, String name, String pagePath) {
        channels.add(new OfficialLiveChannel(id, name, CCTV_LIVE + pagePath, ""));
    }
}
