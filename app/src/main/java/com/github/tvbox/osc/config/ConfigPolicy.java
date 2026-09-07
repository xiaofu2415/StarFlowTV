package com.github.tvbox.osc.config;

import com.github.tvbox.osc.update.DistributionEndpoints;

import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ConfigPolicy {
    private static final Pattern SHA256 = Pattern.compile("^[a-f0-9]{64}$");
    private static final Set<String> ALLOWED_FILES = new HashSet<>();

    static {
        ALLOWED_FILES.add("live.json");
        ALLOWED_FILES.add("live.m3u");
        ALLOWED_FILES.add("live.txt");
        ALLOWED_FILES.add("tvbox-live.json");
        ALLOWED_FILES.add("epg.xml");
        ALLOWED_FILES.add("checksums.sha256");
    }

    private ConfigPolicy() {
    }

    public static ConfigDecision evaluate(int currentVersion, String manifestUrl,
                                          String trustedKeyId, ConfigManifest manifest) {
        if (manifest == null || manifest.schemaVersion != 1 || manifest.configVersion <= 0
                || !"Ed25519".equals(manifest.signatureAlgorithm)
                || trustedKeyId == null || !trustedKeyId.equals(manifest.keyId)
                || manifest.publishedAt == null || manifest.publishedAt.trim().isEmpty()
                || manifest.files == null || !DistributionEndpoints.isSafeHttps(manifestUrl)) {
            return ConfigDecision.INVALID;
        }
        Set<String> names = new HashSet<>();
        for (ConfigManifest.Entry entry : manifest.files) {
            if (entry == null || entry.name == null || !ALLOWED_FILES.contains(entry.name)
                    || !names.add(entry.name) || entry.mediaType == null || entry.mediaType.trim().isEmpty()
                    || entry.size <= 0 || entry.sha256 == null
                    || !SHA256.matcher(entry.sha256.toLowerCase(Locale.US)).matches()
                    || !DistributionEndpoints.isSafeHttps(entry.url)
                    || !sameOrigin(manifestUrl, entry.url)) return ConfigDecision.INVALID;
        }
        if (!names.contains("live.json") || !names.contains("live.txt")) return ConfigDecision.INVALID;
        return manifest.configVersion <= currentVersion ? ConfigDecision.NO_UPDATE : ConfigDecision.AVAILABLE;
    }

    private static boolean sameOrigin(String left, String right) {
        try {
            URI a = new URI(left);
            URI b = new URI(right);
            return a.getScheme().equalsIgnoreCase(b.getScheme())
                    && a.getHost().equalsIgnoreCase(b.getHost())
                    && port(a) == port(b);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int port(URI uri) {
        return uri.getPort() == -1 ? 443 : uri.getPort();
    }
}
