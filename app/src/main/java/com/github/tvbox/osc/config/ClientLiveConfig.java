package com.github.tvbox.osc.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class ClientLiveConfig {
    private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9._-]*$");
    private static final Set<String> ROOT = set("schemaVersion", "configVersion", "generatedAt", "channels");
    private static final Set<String> CHANNEL = set("id", "name", "group", "logo", "epgId", "sources");
    private static final Set<String> SOURCE = set("id", "url", "priority", "protocol");
    private static final Set<String> CREDENTIAL_QUERY_KEYS = set(
            "access_token", "apikey", "api_key", "auth", "authorization", "key",
            "password", "session", "sessionid", "sig", "signature", "token");

    public final boolean valid;
    public final int configVersion;

    private ClientLiveConfig(boolean valid, int configVersion) {
        this.valid = valid;
        this.configVersion = configVersion;
    }

    public static ClientLiveConfig parseAndValidate(String json) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (!only(root, ROOT) || integer(root, "schemaVersion") != 1) return invalid();
            int version = integer(root, "configVersion");
            if (version <= 0 || text(root, "generatedAt").isEmpty()) return invalid();
            JsonArray channels = root.getAsJsonArray("channels");
            if (channels == null) return invalid();
            Set<String> urls = new HashSet<>();
            for (JsonElement item : channels) {
                JsonObject channel = item.getAsJsonObject();
                if (!only(channel, CHANNEL) || !id(channel, "id") || text(channel, "name").isEmpty()
                        || text(channel, "group").isEmpty()) return invalid();
                JsonArray sources = channel.getAsJsonArray("sources");
                if (sources == null || sources.size() == 0) return invalid();
                for (JsonElement raw : sources) {
                    JsonObject source = raw.getAsJsonObject();
                    String url = text(source, "url");
                    String protocol = text(source, "protocol");
                    if (!only(source, SOURCE) || !id(source, "id") || integer(source, "priority") <= 0
                            || !safeStreamUrl(url, protocol) || !urls.add(url)) return invalid();
                }
            }
            return new ClientLiveConfig(true, version);
        } catch (Exception ignored) {
            return invalid();
        }
    }

    private static boolean safeStreamUrl(String value, String protocol) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            return set("http", "https", "rtsp", "rtp", "udp").contains(scheme)
                    && scheme.equals(protocol) && uri.getHost() != null && uri.getUserInfo() == null
                    && !hasCredentialQuery(uri.getRawQuery()) && uri.getRawFragment() == null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean hasCredentialQuery(String rawQuery) throws Exception {
        if (rawQuery == null || rawQuery.isEmpty()) return false;
        for (String parameter : rawQuery.split("&")) {
            String rawKey = parameter.split("=", 2)[0];
            String key = URLDecoder.decode(rawKey, "UTF-8").toLowerCase();
            if (CREDENTIAL_QUERY_KEYS.contains(key)) return true;
        }
        return false;
    }

    private static boolean only(JsonObject object, Set<String> allowed) {
        return object != null && allowed.containsAll(object.keySet());
    }

    private static boolean id(JsonObject object, String key) {
        return ID.matcher(text(object, key)).matches();
    }

    private static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() ? "" : value.getAsString().trim();
    }

    private static int integer(JsonObject object, String key) {
        try { return object.get(key).getAsInt(); } catch (Exception ignored) { return 0; }
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static ClientLiveConfig invalid() {
        return new ClientLiveConfig(false, 0);
    }
}
