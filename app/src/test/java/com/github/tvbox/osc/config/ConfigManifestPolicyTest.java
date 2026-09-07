package com.github.tvbox.osc.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ConfigManifestPolicyTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private ConfigManifest valid() {
        return ConfigManifest.parse("{\"schemaVersion\":1,\"configVersion\":7," +
                "\"publishedAt\":\"2026-09-06T03:30:00Z\",\"signatureAlgorithm\":\"Ed25519\"," +
                "\"keyId\":\"starflow-production-1\",\"files\":[" +
                file("live.json", "application/json") + "," +
                file("live.txt", "text/plain") + "]}");
    }

    private String file(String name, String mediaType) {
        return "{\"name\":\"" + name + "\",\"url\":\"https://config.example/starflow/config/" +
                name + "\",\"size\":100,\"sha256\":\"" + HASH + "\",\"mediaType\":\"" + mediaType + "\"}";
    }

    @Test public void acceptsNewSignedSameOriginConfig() {
        assertEquals(ConfigDecision.AVAILABLE, ConfigPolicy.evaluate(6,
                "https://config.example/starflow/config/manifest.json",
                "starflow-production-1", valid()));
    }

    @Test public void preservesCurrentVersionAndRejectsWrongKeyOrMissingPlaybackFile() {
        assertEquals(ConfigDecision.NO_UPDATE, ConfigPolicy.evaluate(7,
                "https://config.example/starflow/config/manifest.json",
                "starflow-production-1", valid()));
        assertEquals(ConfigDecision.INVALID, ConfigPolicy.evaluate(6,
                "https://config.example/starflow/config/manifest.json", "other-key", valid()));
        ConfigManifest missing = valid();
        missing.files.remove(1);
        assertEquals(ConfigDecision.INVALID, ConfigPolicy.evaluate(6,
                "https://config.example/starflow/config/manifest.json",
                "starflow-production-1", missing));
    }
}
