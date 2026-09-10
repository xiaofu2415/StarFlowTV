package com.github.tvbox.osc.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Converts the signed StarFlow live schema to the player's legacy live format. */
public final class ClientLiveConfigAdapter {
    private ClientLiveConfigAdapter() {
    }

    public static JsonArray toTvBoxLiveGroups(String json) {
        JsonArray result = new JsonArray();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray channels = root.getAsJsonArray("channels");
            if (channels == null) return result;

            LinkedHashMap<String, JsonObject> groups = new LinkedHashMap<>();
            for (JsonElement rawChannel : channels) {
                if (rawChannel == null || !rawChannel.isJsonObject()) continue;
                JsonObject sourceChannel = rawChannel.getAsJsonObject();
                String groupName = text(sourceChannel, "group");
                String channelName = text(sourceChannel, "name");
                JsonArray sources = sourceChannel.getAsJsonArray("sources");
                if (groupName.isEmpty() || channelName.isEmpty() || sources == null || sources.size() == 0) {
                    continue;
                }

                JsonObject group = groups.get(groupName);
                if (group == null) {
                    group = new JsonObject();
                    group.addProperty("group", groupName);
                    group.add("channels", new JsonArray());
                    groups.put(groupName, group);
                    result.add(group);
                }

                JsonArray targetChannels = group.getAsJsonArray("channels");
                JsonObject targetChannel = findChannel(targetChannels, channelName);
                if (targetChannel == null) {
                    targetChannel = new JsonObject();
                    targetChannel.addProperty("name", channelName);
                    copyText(sourceChannel, targetChannel, "logo");
                    String epgId = text(sourceChannel, "epgId");
                    if (!epgId.isEmpty()) targetChannel.addProperty("tvg-id", epgId);
                    targetChannels.add(targetChannel);
                }

                JsonArray urls = targetChannel.has("urls")
                        ? targetChannel.getAsJsonArray("urls") : new JsonArray();
                for (JsonElement rawSource : orderedSources(sources)) {
                    if (rawSource == null || !rawSource.isJsonObject()) continue;
                    String url = text(rawSource.getAsJsonObject(), "url");
                    if (!url.isEmpty() && !contains(urls, url)) urls.add(url);
                }
                targetChannel.add("urls", urls);
            }
            return result;
        } catch (Throwable ignored) {
            return new JsonArray();
        }
    }

    private static List<JsonElement> orderedSources(JsonArray sources) {
        List<JsonElement> ordered = new ArrayList<>();
        for (JsonElement source : sources) ordered.add(source);
        ordered.sort((left, right) -> Integer.compare(priority(left), priority(right)));
        return ordered;
    }

    private static int priority(JsonElement source) {
        try {
            return source.getAsJsonObject().get("priority").getAsInt();
        } catch (Throwable ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static JsonObject findChannel(JsonArray channels, String name) {
        for (JsonElement rawChannel : channels) {
            if (!rawChannel.isJsonObject()) continue;
            JsonObject channel = rawChannel.getAsJsonObject();
            if (name.equals(text(channel, "name"))) return channel;
        }
        return null;
    }

    private static boolean contains(JsonArray values, String value) {
        for (JsonElement element : values) {
            if (element != null && value.equals(element.getAsString())) return true;
        }
        return false;
    }

    private static void copyText(JsonObject source, JsonObject target, String key) {
        String value = text(source, key);
        if (!value.isEmpty()) target.addProperty(key, value);
    }

    private static String text(JsonObject object, String key) {
        try {
            JsonElement value = object.get(key);
            return value == null || !value.isJsonPrimitive() ? "" : value.getAsString().trim();
        } catch (Throwable ignored) {
            return "";
        }
    }
}
