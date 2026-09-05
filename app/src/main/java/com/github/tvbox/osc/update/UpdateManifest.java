package com.github.tvbox.osc.update;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public final class UpdateManifest {
    @SerializedName("schema_version") public int schemaVersion;
    @SerializedName("version_code") public int versionCode;
    @SerializedName("version_name") public String versionName;
    @SerializedName("minimum_sdk") public int minimumSdk;
    @SerializedName("release_notes") public String releaseNotes;
    public Apk apk;

    public static final class Apk {
        public String url;
        public String sha256;
        public long size;
    }

    public static UpdateManifest parse(String json) {
        return new Gson().fromJson(json, UpdateManifest.class);
    }
}
