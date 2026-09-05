package com.github.tvbox.osc.update;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UpdatePolicy {
    private static final Pattern SHA256 = Pattern.compile("^[a-f0-9]{64}$");

    private UpdatePolicy() {
    }

    public static UpdateDecision evaluate(
            int currentVersionCode,
            int sdkInt,
            String manifestUrl,
            UpdateManifest manifest
    ) {
        if (manifest == null || manifest.schemaVersion != 1 || manifest.apk == null
                || manifest.versionCode <= 0 || manifest.minimumSdk <= 0
                || manifest.versionName == null || manifest.versionName.trim().isEmpty()
                || manifest.apk.size <= 0 || manifest.apk.sha256 == null
                || !SHA256.matcher(manifest.apk.sha256.toLowerCase(Locale.US)).matches()
                || !DistributionEndpoints.isSafeHttps(manifestUrl)
                || !DistributionEndpoints.isSafeHttps(manifest.apk.url)
                || !sameOrigin(manifestUrl, manifest.apk.url)) {
            return UpdateDecision.INVALID;
        }
        if (manifest.versionCode <= currentVersionCode) return UpdateDecision.NO_UPDATE;
        if (manifest.minimumSdk > sdkInt) return UpdateDecision.UNSUPPORTED;
        return UpdateDecision.AVAILABLE;
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
