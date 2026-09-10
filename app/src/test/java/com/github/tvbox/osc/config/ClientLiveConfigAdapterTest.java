package com.github.tvbox.osc.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ClientLiveConfigAdapterTest {
    private static final String CONFIG = "{\"schemaVersion\":1,\"configVersion\":7," +
            "\"generatedAt\":\"2026-09-10T05:49:14Z\",\"channels\":[" +
            "{\"id\":\"cctv1\",\"name\":\"CCTV1\",\"group\":\"央视频道｜来源：iptv\"," +
            "\"logo\":\"https://logo.example/cctv1.png\",\"epgId\":\"CCTV-1\",\"sources\":[" +
            "{\"id\":\"source-a\",\"url\":\"https://live.example/a.m3u8\",\"priority\":1,\"protocol\":\"https\"}," +
            "{\"id\":\"source-b\",\"url\":\"https://live.example/b.m3u8?key=txiptv\",\"priority\":2,\"protocol\":\"https\"}]}]}";

    @Test
    public void preservesGroupsChannelsMetadataAndSourceOrder() {
        JsonArray groups = ClientLiveConfigAdapter.toTvBoxLiveGroups(CONFIG);

        assertEquals(1, groups.size());
        JsonObject group = groups.get(0).getAsJsonObject();
        assertEquals("央视频道｜来源：iptv", group.get("group").getAsString());
        JsonObject channel = group.getAsJsonArray("channels").get(0).getAsJsonObject();
        assertEquals("CCTV1", channel.get("name").getAsString());
        assertEquals("https://logo.example/cctv1.png", channel.get("logo").getAsString());
        assertEquals("CCTV-1", channel.get("tvg-id").getAsString());
        assertEquals("https://live.example/a.m3u8",
                channel.getAsJsonArray("urls").get(0).getAsString());
        assertEquals("https://live.example/b.m3u8?key=txiptv",
                channel.getAsJsonArray("urls").get(1).getAsString());
    }
}
