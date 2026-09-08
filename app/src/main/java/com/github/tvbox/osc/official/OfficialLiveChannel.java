package com.github.tvbox.osc.official;

public final class OfficialLiveChannel {
    private final String id;
    private final String name;
    private final String pageUrl;
    private final String directUrl;

    public OfficialLiveChannel(String id, String name, String pageUrl, String directUrl) {
        this.id = value(id);
        this.name = value(name);
        this.pageUrl = value(pageUrl);
        this.directUrl = value(directUrl);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getDirectUrl() {
        return directUrl;
    }

    public OfficialLivePlaybackMode preferredMode() {
        if (!directUrl.isEmpty()) return OfficialLivePlaybackMode.DIRECT;
        return OfficialLiveUrlPolicy.isAllowedPageUrl(pageUrl)
                ? OfficialLivePlaybackMode.WEB : OfficialLivePlaybackMode.NONE;
    }

    private static String value(String value) {
        return value == null ? "" : value.trim();
    }
}
