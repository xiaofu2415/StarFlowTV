package com.github.tvbox.osc.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class UpdatePolicyTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private UpdateManifest manifest(int versionCode, int minimumSdk, String apkUrl, String sha256) {
        UpdateManifest value = new UpdateManifest();
        value.schemaVersion = 1;
        value.versionCode = versionCode;
        value.versionName = "1.1.0";
        value.minimumSdk = minimumSdk;
        value.apk = new UpdateManifest.Apk();
        value.apk.url = apkUrl;
        value.apk.sha256 = sha256;
        value.apk.size = 1024;
        return value;
    }

    @Test public void acceptsNewCompatibleSameOriginRelease() {
        assertEquals(UpdateDecision.AVAILABLE, UpdatePolicy.evaluate(
                1, 23, "https://tv.example/app/update.json",
                manifest(2, 23, "https://tv.example/app/StarFlowTV-1.1.0.apk", HASH)));
    }

    @Test public void rejectsCrossOriginOrInvalidHash() {
        assertEquals(UpdateDecision.INVALID, UpdatePolicy.evaluate(
                1, 23, "https://tv.example/app/update.json",
                manifest(2, 23, "https://evil.example/app.apk", HASH)));
        assertEquals(UpdateDecision.INVALID, UpdatePolicy.evaluate(
                1, 23, "https://tv.example/app/update.json",
                manifest(2, 23, "https://tv.example/app.apk", "bad")));
    }

    @Test public void distinguishesCurrentAndUnsupportedVersions() {
        assertEquals(UpdateDecision.NO_UPDATE, UpdatePolicy.evaluate(
                2, 23, "https://tv.example/app/update.json",
                manifest(2, 23, "https://tv.example/app/app.apk", HASH)));
        assertEquals(UpdateDecision.UNSUPPORTED, UpdatePolicy.evaluate(
                1, 23, "https://tv.example/app/update.json",
                manifest(2, 29, "https://tv.example/app/app.apk", HASH)));
    }
}
