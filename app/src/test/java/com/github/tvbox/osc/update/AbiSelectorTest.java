package com.github.tvbox.osc.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class AbiSelectorTest {
    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private UpdateManifest manifest() {
        return UpdateManifest.parse("{\"schemaVersion\":1,\"versionName\":\"1.2.0\"," +
                "\"versionCode\":12,\"channel\":\"stable\",\"releaseNotes\":\"notes\"," +
                "\"publishedAt\":\"2026-09-06T03:30:00Z\",\"minimumSdk\":23," +
                "\"minSupportedVersionCode\":1,\"mandatory\":false," +
                "\"packageName\":\"tv.starflow.player\",\"signingStatus\":\"ready\"," +
                "\"forceUpdate\":false,\"signatureAlgorithm\":\"Ed25519\"," +
                "\"keyId\":\"starflow-production-2026-01\",\"packages\":[" +
                apk("armeabi-v7a") + "," + apk("arm64-v8a") + "," + apk("universal") + "]}");
    }

    private String apk(String abi) {
        return "{\"abi\":\"" + abi + "\",\"fileName\":\"app-" + abi + ".apk\"," +
                "\"downloadUrl\":\"https://update.example/app-" + abi + ".apk\",\"size\":100," +
                "\"sha256\":\"" + HASH + "\",\"certificateSha256\":\"" + HASH + "\"}";
    }

    @Test public void prefersExactDeviceAbiAndFallsBackToUniversal() {
        assertEquals("arm64-v8a", AbiSelector.select(manifest(),
                new String[]{"arm64-v8a", "armeabi-v7a"}).abi);
        assertEquals("armeabi-v7a", AbiSelector.select(manifest(),
                new String[]{"armeabi-v7a"}).abi);
        assertEquals("universal", AbiSelector.select(manifest(),
                new String[]{"x86"}).abi);
    }
}
