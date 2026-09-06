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
    public int minSupportedVersionCode;
    public boolean forceUpdate;
    public boolean mandatory;
    public String packageName;
    public String signingStatus;
    public String signingKeyId;
    public String signatureAlgorithm;
    public String keyId;
    public List<Apk> packages = new ArrayList<>();
    public List<Apk> apkVariants = new ArrayList<>();

    public static final class Apk {
        public String abi;
        public String fileName;
        public String url;
        public String downloadUrl;
        public long size;
        public String sha256;
        public String signingCertificateDigest;
        public String certificateSha256;
        public String status;
        public boolean installable;
    }

    public static UpdateManifest parse(String json) {
        return new Gson().fromJson(json, UpdateManifest.class);
    }
}
