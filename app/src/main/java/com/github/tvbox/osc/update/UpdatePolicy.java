package com.github.tvbox.osc.update;

import com.github.tvbox.osc.BuildConfig;

import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class UpdatePolicy {
    private static final Pattern SHA256 = Pattern.compile("^[a-f0-9]{64}$");
    private static final Set<String> REQUIRED_ABIS = new HashSet<>();

    static {
        REQUIRED_ABIS.add("armeabi-v7a");
        REQUIRED_ABIS.add("arm64-v8a");
        REQUIRED_ABIS.add("universal");
    }

    private UpdatePolicy() {
    }

    public static UpdateDecision evaluate(
            int currentVersionCode, int sdkInt, String selectedChannel, String trustedKeyId,
            String manifestUrl, UpdateManifest manifest
    ) {
        if (manifest == null || manifest.schemaVersion != 1 || manifest.versionCode <= 0
                || manifest.minimumSdk < 23 || blank(manifest.versionName) || blank(manifest.publishedAt)
                || manifest.minSupportedVersionCode <= 0
                || !BuildConfig.APPLICATION_ID.equals(manifest.packageName)
                || !"ready".equals(manifest.signingStatus)
                || !"Ed25519".equals(manifest.signatureAlgorithm)
                || trustedKeyId == null || !trustedKeyId.equals(manifest.keyId)
                || !DistributionEndpoints.isSafeHttps(manifestUrl) || manifest.packages == null) {
            return UpdateDecision.INVALID;
        }
        if (!"stable".equals(selectedChannel) && !"beta".equals(selectedChannel)) return UpdateDecision.INVALID;
        if (!selectedChannel.equals(manifest.channel)) return UpdateDecision.NO_UPDATE;
        Set<String> abis = new HashSet<>();
        for (UpdateManifest.Apk apk : manifest.packages) {
            if (apk == null || !REQUIRED_ABIS.contains(apk.abi) || !abis.add(apk.abi)
                    || blank(apk.fileName) || apk.fileName.contains("/") || apk.size <= 0
                    || !hash(apk.sha256) || !hash(apk.certificateSha256)
                    || !DistributionEndpoints.isSafeHttps(apk.downloadUrl)
                    || !sameOrigin(manifestUrl, apk.downloadUrl)) return UpdateDecision.INVALID;
        }
        if (!abis.equals(REQUIRED_ABIS)) return UpdateDecision.INVALID;
        if (manifest.versionCode <= currentVersionCode) return UpdateDecision.NO_UPDATE;
        if (manifest.minimumSdk > sdkInt) return UpdateDecision.UNSUPPORTED;
        return UpdateDecision.AVAILABLE;
    }

    private static boolean hash(String value) {
        return value != null && SHA256.matcher(value.toLowerCase(Locale.US)).matches();
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean sameOrigin(String left, String right) {
        try {
            URI a = new URI(left);
            URI b = new URI(right);
            return a.getScheme().equalsIgnoreCase(b.getScheme())
                    && a.getHost().equalsIgnoreCase(b.getHost())
                    && effectivePort(a) == effectivePort(b);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() == -1 ? 443 : uri.getPort();
    }
}
