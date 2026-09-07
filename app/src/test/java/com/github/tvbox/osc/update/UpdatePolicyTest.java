package com.github.tvbox.osc.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class UpdatePolicyTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String CERT = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    private UpdateManifest manifest(int versionCode, int minimumSdk, String apkUrl, String sha256) {
        UpdateManifest value = new UpdateManifest();
        value.schemaVersion = 1;
        value.versionCode = versionCode;
        value.versionName = "1.1.0";
        value.minimumSdk = minimumSdk;
        value.minSupportedVersionCode = 1;
        value.packageName = "tv.starflow.player";
        value.mandatory = false;
        value.signingStatus = "ready";
        value.channel = "stable";
        value.publishedAt = "2026-09-06T03:30:00Z";
        value.signatureAlgorithm = "Ed25519";
        value.keyId = "starflow-production-2026-01";
        value.packages = new java.util.ArrayList<>();
        for (String abi : new String[]{"armeabi-v7a", "arm64-v8a", "universal"}) {
            UpdateManifest.Apk apk = new UpdateManifest.Apk();
            apk.abi = abi;
            apk.fileName = "StarFlowTV-1.1.0-" + abi + ".apk";
            apk.downloadUrl = apkUrl.replace("app.apk", apk.fileName);
            apk.sha256 = sha256;
            apk.certificateSha256 = CERT;
            apk.size = 1024;
            value.packages.add(apk);
        }
        return value;
    }

    @Test public void acceptsNewCompatibleSameOriginRelease() {
        assertEquals(UpdateDecision.AVAILABLE, UpdatePolicy.evaluate(
                1, 23, "stable", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json",
                manifest(2, 23, "https://update.example/starflow/update/app.apk", HASH)));
    }

    @Test public void rejectsCrossOriginOrInvalidHash() {
        assertEquals(UpdateDecision.INVALID, UpdatePolicy.evaluate(
                1, 23, "stable", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json",
                manifest(2, 23, "https://evil.example/app.apk", HASH)));
        assertEquals(UpdateDecision.INVALID, UpdatePolicy.evaluate(
                1, 23, "stable", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json",
                manifest(2, 23, "https://update.example/starflow/update/app.apk", "bad")));
    }

    @Test public void distinguishesCurrentAndUnsupportedVersions() {
        assertEquals(UpdateDecision.NO_UPDATE, UpdatePolicy.evaluate(
                2, 23, "stable", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json",
                manifest(2, 23, "https://update.example/starflow/update/app.apk", HASH)));
        assertEquals(UpdateDecision.UNSUPPORTED, UpdatePolicy.evaluate(
                1, 23, "stable", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json",
                manifest(2, 29, "https://update.example/starflow/update/app.apk", HASH)));
    }

    @Test public void enforcesSelectedReleaseChannel() {
        assertEquals(UpdateDecision.NO_UPDATE, UpdatePolicy.evaluate(
                1, 23, "beta", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json",
                manifest(2, 23, "https://update.example/starflow/update/app.apk", HASH)));
    }

    @Test public void rejectsPendingSigningAndWrongPackageMetadata() {
        UpdateManifest pending = manifest(2, 23,
                "https://update.example/starflow/update/app.apk", HASH);
        pending.signingStatus = "production-signing-pending";
        assertEquals(UpdateDecision.INVALID, UpdatePolicy.evaluate(
                1, 23, "stable", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json", pending));

        UpdateManifest wrongPackage = manifest(2, 23,
                "https://update.example/starflow/update/app.apk", HASH);
        wrongPackage.packageName = "com.example.other";
        assertEquals(UpdateDecision.INVALID, UpdatePolicy.evaluate(
                1, 23, "stable", "starflow-production-2026-01",
                "https://update.example/starflow/update/latest.json", wrongPackage));
    }
}
