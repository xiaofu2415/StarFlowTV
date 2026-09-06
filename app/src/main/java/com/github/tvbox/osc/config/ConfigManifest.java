package com.github.tvbox.osc.config;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public final class ConfigManifest {
    public int schemaVersion;
    public int configVersion;
    public String publishedAt;
    public String signatureAlgorithm;
    public String keyId;
    public List<Entry> files = new ArrayList<>();

    public static final class Entry {
        public String name;
        public String url;
        public long size;
        public String sha256;
        public String mediaType;
    }

    public static ConfigManifest parse(String json) {
        return new Gson().fromJson(json, ConfigManifest.class);
    }

    public Entry file(String name) {
        if (files == null) return null;
        for (Entry entry : files) if (entry != null && name.equals(entry.name)) return entry;
        return null;
    }
}
