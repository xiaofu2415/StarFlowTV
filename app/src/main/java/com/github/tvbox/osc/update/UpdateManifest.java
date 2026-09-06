package com.github.tvbox.osc.update;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public final class UpdateManifest {
    public int schemaVersion;
    public String versionName;
    public int versionCode;
    public String channel;
    public String releaseNotes;
    public String publishedAt;
    public int minimumSdk;
    public boolean forceUpdate;
    public String signatureAlgorithm;
    public String keyId;
    public List<Apk> packages = new ArrayList<>();

    public static final class Apk {
        public String abi;
        public String fileName;
        public String downloadUrl;
        public long size;
        public String sha256;
        public String certificateSha256;
    }

    public static UpdateManifest parse(String json) {
        return new Gson().fromJson(json, UpdateManifest.class);
    }
}
