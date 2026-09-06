package com.github.tvbox.osc.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ClientLiveConfigTest {
    private static final String VALID = "{\"schemaVersion\":1,\"configVersion\":3," +
            "\"generatedAt\":\"2026-09-06T03:30:00Z\",\"channels\":[{" +
            "\"id\":\"cctv-plus-1\",\"name\":\"CCTV+ 1\",\"group\":\"央视\"," +
            "\"sources\":[{\"id\":\"official\",\"url\":\"https://live.example/a.m3u8\"," +
            "\"priority\":1,\"protocol\":\"https\"}]}]}";

    @Test public void validatesMinimalClientSchema() {
        ClientLiveConfig result = ClientLiveConfig.parseAndValidate(VALID);
        assertTrue(result.valid);
        assertEquals(3, result.configVersion);
    }

    @Test public void rejectsUnknownFieldsCredentialsAndDuplicateUrls() {
        assertFalse(ClientLiveConfig.parseAndValidate(VALID.replace("\"channels\"", "\"unknown\":1,\"channels\"")).valid);
        assertFalse(ClientLiveConfig.parseAndValidate(VALID.replace("a.m3u8", "a.m3u8?token=secret")).valid);
        String duplicate = VALID.replace(
                "{\"id\":\"official\",\"url\":\"https://live.example/a.m3u8\",\"priority\":1,\"protocol\":\"https\"}",
                "{\"id\":\"official\",\"url\":\"https://live.example/a.m3u8\",\"priority\":1,\"protocol\":\"https\"}," +
                        "{\"id\":\"b\",\"url\":\"https://live.example/a.m3u8\",\"priority\":2,\"protocol\":\"https\"}");
        assertFalse(ClientLiveConfig.parseAndValidate(duplicate).valid);
    }
}
